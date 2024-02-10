package com.lam.lam_project.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.lam.lam_project.R;
import com.lam.lam_project.activities.MapActivity;
import com.lam.lam_project.database.ContextDB;
import com.lam.lam_project.models.Record;
import com.lam.lam_project.records.AcousticNoiseRecorder;
import com.lam.lam_project.records.Recorder;
import com.lam.lam_project.records.SignalStrengthRecorder;
import com.lam.lam_project.records.WifiRecorder;
import com.lam.lam_project.utils.MGRS;

import mil.nga.mgrs.tile.MGRSTileProvider;

public class RecorderService extends Service {
    private ContextDB db;
    private AcousticNoiseRecorder noiseRecord;
    private SignalStrengthRecorder signalRecord;
    private WifiRecorder wifiRecord;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private MGRSTileProvider tp;

    int notification_id_counter = 0;

    public RecorderService() {
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat
                .Builder(this, getString(R.string.notification_channel_id_background_service))
                .setContentTitle("LAM")
                .setContentText("The app is sampling in background...")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        db = new ContextDB(this);
        noiseRecord = new AcousticNoiseRecorder(500);
        wifiRecord = new WifiRecorder(wifiManager);
        signalRecord = new SignalStrengthRecorder(telephonyManager);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tp = MGRSTileProvider.create(this);

        String backgroundAccuracyKey = getString(R.string.background_accuracy_key);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String samplingValue = defaultSharedPreferences.getString(backgroundAccuracyKey, "3");
        if (samplingValue.equals("100 meters")) {
            SharedPreferences.Editor editor = defaultSharedPreferences.edit();
            editor.putString(backgroundAccuracyKey, "100 meters");
            editor.apply();
            samplingValue = "3";
        }
        int backgroundSamplingAccuracy = Integer.parseInt(samplingValue);
        System.out.println("Record accuracy " + samplingValue);

        int distanceInMeters;

        if (backgroundSamplingAccuracy== 3)
            distanceInMeters = 60;
        else if (backgroundSamplingAccuracy == 4)
            distanceInMeters = 600;
        else
            distanceInMeters = 5;

        locationRequest = new LocationRequest
                .Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateDistanceMeters(distanceInMeters)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location lastLocation = locationResult.getLastLocation();

                if (lastLocation == null)
                    return;

                LatLng currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                mil.nga.mgrs.MGRS currentLocationMGRS = tp.getMGRS(currentLocation);
                //System.out.println(currentLocationMGRS);

                String zone = currentLocationMGRS.getZone() + "" + currentLocationMGRS.getBand();
                String square = currentLocationMGRS.getColumnRowId();
                String easting = MGRS.getMaxAccuracyEN(currentLocationMGRS.getEasting());
                String northing = MGRS.getMaxAccuracyEN(currentLocationMGRS.getNorthing());

                Cursor dbQuery = db.getMissingRecordTypes(backgroundSamplingAccuracy, zone, square, easting, northing);
                System.out.println("Position registered");
                while(dbQuery.moveToNext()){
                    String res = dbQuery.getString(0);
                    System.out.println("Need to sample: " + res);
                    Record newRecord = smartRecorder(res);

                    if (newRecord == null){
                        System.out.println("Error while sampling " + res);
                        continue;
                    }

                    db.addRecord(
                            newRecord.getType().toString(),
                            newRecord.getTimeStamp(),
                            newRecord.getValue(),
                            newRecord.getCondition(),
                            zone,
                            square,
                            easting,
                            northing
                    );

                    System.out.println("Correctly recorded " + res);

                    boolean notificationSettings = defaultSharedPreferences.getBoolean(getString(R.string.settings_notification_key), false);

                    if (notificationSettings) {
                        String notificationMsg = "Just recorded " + res + " with a value of " + newRecord.getValue();
                        sendNotification(notificationMsg);
                    }
                }
            }
        };

        System.out.println("Service started");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        return START_REDELIVER_INTENT;
    }

    private Record smartRecorder(String recordNeeded){
        Recorder recorder;

        if (recordNeeded.equals("Noise"))
            recorder = new AcousticNoiseRecorder(500);
        else if (recordNeeded.equals("Wifi"))
            recorder = wifiRecord;
        else
            recorder = signalRecord;
        System.out.println("Recording " + recordNeeded);
        return recorder.getSample();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop location callback
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @SuppressLint("MissingPermission")
    private void sendNotification(String message) {
        Intent notificationIntent = new Intent(this, MapActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat
                .Builder(this, "basic")
                .setContentTitle("The app have a new record!")
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(notification_id_counter++, notification);

    }
}