package com.lam.lam_project.records;

import android.annotation.SuppressLint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.lam.lam_project.models.Record;
import com.lam.lam_project.models.RecordType;
import com.lam.lam_project.models.SignalCondition;

import java.util.List;

public class WifiRecorder extends Recorder {
    WifiManager wifiManager;

    public WifiRecorder(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    @Override
    public Record getSample() {
        if (!wifiManager.isWifiEnabled())
            return null;

        int mean = 0;

        @SuppressLint("MissingPermission")
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for (ScanResult accessPoint: scanResults) {
            System.out.println(accessPoint);
            System.out.println(accessPoint.level);
            mean += accessPoint.level;
        }


        mean =  (mean / scanResults.size());

        int condition;

        //se non sono presenti wifi, wifi pessimo
        if (mean == 0) {
            mean = -127;
        }

        if (mean >= -50)
            condition = SignalCondition.EXCELLENT;
        else if (mean >= -65)
            condition = SignalCondition.GOOD;
        else
            condition = SignalCondition.POOR;

        return new Record(RecordType.Wifi, condition, mean);
    }
}
