package com.lam.lam_project.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lam.lam_project.R;
import com.lam.lam_project.database.ContextDB;
import com.lam.lam_project.databinding.ActivityMapsBinding;
import com.lam.lam_project.models.LocalizedRecord;
import com.lam.lam_project.models.Record;
import com.lam.lam_project.models.RecordType;
import com.lam.lam_project.records.AcousticNoiseRecorder;
import com.lam.lam_project.records.Recorder;
import com.lam.lam_project.records.SignalStrengthRecorder;
import com.lam.lam_project.records.WifiRecorder;
import com.lam.lam_project.services.RecorderService;
import com.lam.lam_project.utils.MGRS;
import com.lam.lam_project.utils.MapManager;

import java.util.ArrayList;
import java.util.List;

import mil.nga.mgrs.grid.GridType;


public class MapActivity extends MenuActivity implements OnMapReadyCallback {

    private FloatingActionButton btn_record;
    private Button btn_noise, btn_signal, btn_wifi;
    private Button btn_10m, btn_100m, btn_1000m;
    private static final int REQUEST_MICROPHONE = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final int REQUEST_ENABLE_GPS = 3;
    private static String SERVICE_CHANNEL_ID, BASIC_CHANNEL_ID;
    private FusedLocationProviderClient fusedLocationClient;
    private RecordType currentRecordType;
    private Recorder noiseRecorder, signalRecorder, wifiRecorder, currentRecorder;
    private GridType mapGridType;
    private ContextDB db;
    private MapManager mapManager;
    private SharedPreferences defaultSharedPreferences;
    private String defaultRecorderKey;
    private String defaultGranularityKey;
    private String defaultRecorder;
    private String defaultGranularity;
    private boolean backgroundRecordingOn;
    private boolean micPermissions, locationPermissions, GPSEnabled;
    private int pastToAverage, noiseSamplingTime;

    private void initializeRecorders() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        this.noiseRecorder = new AcousticNoiseRecorder(noiseSamplingTime);
        this.signalRecorder = new SignalStrengthRecorder(telephonyManager);
        this.wifiRecorder = new WifiRecorder(wifiManager);

        currentRecorder = wifiRecorder;
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        micPermissions = false;
        locationPermissions = false;
        GPSEnabled = false;

        ActivityMapsBinding activityMapsBinding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(activityMapsBinding.getRoot());
        SERVICE_CHANNEL_ID = getString(R.string.notification_channel_id_background_service);
        BASIC_CHANNEL_ID = "basic";

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        createNotificationChannel();

        btn_record = findViewById(R.id.btn_sample);
        btn_noise = findViewById(R.id.btn_noise);
        btn_signal = findViewById(R.id.btn_signal);
        btn_wifi = findViewById(R.id.btn_wifi);

        btn_10m = findViewById(R.id.btn_10m);
        btn_100m = findViewById(R.id.btn_100m);
        btn_1000m = findViewById(R.id.btn_1000m);

        db = new ContextDB(this);

        mapGridType = GridType.HUNDRED_METER;

        btn_noise.setOnClickListener(e -> updateRecorderType(RecordType.Noise));
        btn_signal.setOnClickListener(e -> updateRecorderType(RecordType.Signal));
        btn_wifi.setOnClickListener(e -> updateRecorderType(RecordType.Wifi));
        btn_record.setOnClickListener(e -> createSample());
        btn_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentRecordType == RecordType.Noise) {
                    askForPermissions(Manifest.permission.RECORD_AUDIO, REQUEST_MICROPHONE);
                    if (!micPermissions) {
                        return;
                    }
                }
                if (GPSEnabled)
                    createSample();
            }
        });

        btn_10m.setOnClickListener(e -> changeGridType(GridType.TEN_METER));
        btn_100m.setOnClickListener(e -> changeGridType(GridType.HUNDRED_METER));
        btn_1000m.setOnClickListener(e -> changeGridType(GridType.KILOMETER));

        //if needed, ask for permissions
        setActivityTitle("Map");
        askForPermissions(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_LOCATION);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        defaultRecorderKey = getString(R.string.settings_samplers_key);
        defaultGranularityKey = getString(R.string.settings_granularity_key);
        String defaultBackgroundSamplingKey = getString(R.string.settings_background_key);

        defaultRecorder = defaultSharedPreferences.getString(defaultRecorderKey, "null");
        defaultGranularity = defaultSharedPreferences.getString(defaultGranularityKey, "null");
        backgroundRecordingOn = defaultSharedPreferences.getBoolean(defaultBackgroundSamplingKey, false);
        pastToAverage = Integer.parseInt(defaultSharedPreferences.getString("samples_past_to_average", "90"));
        noiseSamplingTime = Integer.parseInt(defaultSharedPreferences.getString("noise_sampling_time", "500"));


        if (locationPermissions) {
            enableGPS();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        }

        initializeRecorders();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        stopService();
        loadHeatMap();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (backgroundRecordingOn)
            startService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (backgroundRecordingOn)
            stopService();
    }

    private void createNotificationChannel(){
        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_CHANNEL_ID,
                "LAM background service",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationChannel basicNotification = new NotificationChannel(
                BASIC_CHANNEL_ID,
                "LAM notification",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(serviceChannel);
        nm.createNotificationChannel(basicNotification);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapManager = new MapManager(googleMap, this);

        //Disegna la griglia corrispondente
        mapManager.drawGrid(mapGridType);

        if (GPSEnabled) {
            mapManager.setLocationEnabled();
            mapManager.moveCameraToUserPosition(fusedLocationClient);
            btn_record.setEnabled(true);
        } else {
            LatLng coords = new LatLng(41.902782, 12.496366);
            mapManager.moveCameraTo(coords);
        }

        //Imposta la mappa del noise al primo avvio
        setRecorder(defaultRecorder);
        setGranularity(defaultGranularity);
    }
    private void setRecorder(String s){
        switch (s){
            case "noise":
                btn_noise.performClick();
                break;
            case "wifi":
                btn_wifi.performClick();
                break;
            case "signal":
                btn_signal.performClick();
                break;
            default:
                btn_noise.performClick();
                SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                editor.putString(defaultRecorderKey, "noise");
                editor.apply();
                break;
        }
    }

    private void setGranularity(String g){
        switch (g){
            case "10":
                btn_10m.performClick();
                break;
            case "100":
                btn_100m.performClick();
                break;
            case "1000":
                btn_1000m.performClick();
                break;
            default:
                btn_100m.performClick();
                SharedPreferences.Editor editor = defaultSharedPreferences.edit();
                editor.putString(defaultGranularityKey, "100");
                editor.apply();
                break;
        }
    }

    private void updateRecorderType(RecordType newType){
        currentRecordType = newType;

        if (currentRecordType == RecordType.Noise)
            currentRecorder = noiseRecorder;
        else if (currentRecordType == RecordType.Signal)
            currentRecorder = signalRecorder;
        else
            currentRecorder = wifiRecorder;

        loadHeatMap();
    }

    private void changeGridType(GridType newGridType){
        this.mapGridType = newGridType;
        mapManager.drawGrid(newGridType);
        loadHeatMap();
    }


    private void askForPermissions(String permissions, int requestCode){
        if (ActivityCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {permissions},
                    requestCode);
            return;
        }

        if (requestCode == REQUEST_MICROPHONE)
            micPermissions = true;
        else if (requestCode == REQUEST_LOCATION) {
            locationPermissions = true;
            GPSEnabled = true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                micPermissions = true;
                btn_noise.setEnabled(true);
                Toast.makeText(this, "Mic permissions given", Toast.LENGTH_SHORT).show();
            } else {
                micPermissions = false;
            }
        }

        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissions = true;
                enableGPS();
                mapManager.setLocationEnabled();
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                mapManager.moveCameraToUserPosition(fusedLocationClient);
                Toast.makeText(this, "Localization permissions given", Toast.LENGTH_SHORT).show();
            } else {
                locationPermissions = false;
                btn_record.setEnabled(false);
            }
        }

        if (requestCode == REQUEST_ENABLE_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GPSEnabled = true;
                btn_record.setEnabled(true);
                Toast.makeText(this, "GPS enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableGPS() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, REQUEST_ENABLE_GPS);
        }
        GPSEnabled = true;
    }


    private void loadHeatMap(){
        List<LocalizedRecord> list = getLocalRecords(currentRecordType.toString(), mapGridType.getAccuracy(), pastToAverage);
        drawHeatMap(list);
    }

    private List<LocalizedRecord> getLocalRecords(String sampleType, int accuracy, int dateLimit){
        List<LocalizedRecord> localizedRecordList = new ArrayList<>();

        Cursor samplesCursor = db.getAvgConditionByAccuracy(sampleType, accuracy, dateLimit);

        while (samplesCursor.moveToNext()){
            //prendo la coordinata
            String gridzone = samplesCursor.getString(0);
            String square = samplesCursor.getString(1);
            String easting = samplesCursor.getString(2);
            String northing = samplesCursor.getString(3);
            //prendo la condizione
            int avg_cond = Math.round(samplesCursor.getFloat(4));

            String coordString = gridzone + square + easting + northing;
            mil.nga.mgrs.MGRS MGRScoord = MGRS.fromStringToMGRS(coordString);
            Record s = new Record(RecordType.valueOf(sampleType), avg_cond, 0);
            LocalizedRecord ls = new LocalizedRecord(s, coordString, null);
            localizedRecordList.add(ls);
        }

        samplesCursor.close();

        return localizedRecordList;
    }

    private void drawHeatMap(List<LocalizedRecord> sampleList){
        if (currentRecordType == null) {
            Toast.makeText(this, "Something went wrong while maps' drawing", Toast.LENGTH_SHORT).show();
            return;
        }

        mapManager.deleteAllPolygons();

        for (LocalizedRecord ls : sampleList){
            mil.nga.mgrs.MGRS mgrsCoord = MGRS.fromStringToMGRS(ls.getMgrsCoords());
            int condition = ls.getBasicRecord().getCondition();
            mapManager.colorTile(mgrsCoord, getColorByValue(condition), mapGridType, currentRecordType);
        }
    }
    private int getColorByValue(int val){
        if (val == 0)
            return Color.rgb(255,0,0);
        else if (val == 1)
            return Color.rgb(255,255,0);
        else
            return Color.rgb(0, 255, 0);
    }

    @SuppressLint("MissingPermission")
    private void createSample() {
        if (noiseRecorder == null || signalRecorder == null || wifiRecorder == null) {
            Toast.makeText(this, "Recorder non inizializzato", Toast.LENGTH_SHORT).show();
            return;
        }

        Record newRecord = currentRecorder.getSample();

        if (newRecord == null){
            Toast.makeText(this, "Errore riscontrato durante il recording, riprovare", Toast.LENGTH_SHORT).show();
            return;
        }
        String msgToShow = "Value: " + newRecord.getValue() + " Condition: " + newRecord.getCondition();
        Toast.makeText(this,msgToShow, Toast.LENGTH_SHORT).show();
        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng cor = new LatLng(location.getLatitude(), location.getLongitude());
                        mil.nga.mgrs.MGRS currentLocation = mapManager.getMGRS(cor);

                        String zone = currentLocation.getZone() + "" + currentLocation.getBand();
                        String square = currentLocation.getColumnRowId();
                        String easting = MGRS.getMaxAccuracyEN(currentLocation.getEasting());
                        String northing = MGRS.getMaxAccuracyEN(currentLocation.getNorthing());

                        //System.out.println("Added coord: " + currentLocation.toString());
                        System.out.println("Added coord: "+ zone + " " + square + " " + easting + " " + northing);
                        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

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
                        loadHeatMap();
                    }
                });
    }

    private void startService(){
        Intent serviceIntent = new Intent(this, RecorderService.class);
        startService(serviceIntent);
    }

    private void stopService(){
        Intent serviceIntent = new Intent(this, RecorderService.class);
        stopService(serviceIntent);
    }

}
