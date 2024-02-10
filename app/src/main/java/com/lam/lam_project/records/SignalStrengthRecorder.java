package com.lam.lam_project.records;

import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;

import com.lam.lam_project.models.Record;
import com.lam.lam_project.models.RecordType;
import com.lam.lam_project.models.SignalCondition;

import java.util.List;

public class SignalStrengthRecorder extends Recorder {
    private TelephonyManager telephonyManager;

    public SignalStrengthRecorder(TelephonyManager telephonyManager) {
        this.telephonyManager = telephonyManager;
    }

    private int getLteCondition(double rssi){
        return getCondition(-65, -70, rssi);
    }

    private int getCdmaCondition(double rssi){
        return getCondition(-70, -95, rssi);
    }

    private int getGenericCondition(double rssi){
        return getCondition(-60, -80, rssi);
    }

    private int getCondition(int excellentBound, int goodBound, double rssi) {
        if (rssi >= excellentBound)
            return SignalCondition.EXCELLENT;
        if (rssi >= goodBound)
            return SignalCondition.GOOD;
        else
            return  SignalCondition.POOR;
    }


    @Override
    public Record getSample() {
        List<CellSignalStrength> cellSignals = telephonyManager.getSignalStrength().getCellSignalStrengths();

        //get the signal of the current SIM
        CellSignalStrength signalStrength = cellSignals.size() != 0 ? cellSignals.get(0)  : null;

        if (signalStrength == null)
            return null;

        double rssi;
        int condition;

        if (signalStrength instanceof CellSignalStrengthLte) {
            CellSignalStrengthLte lteSignal = (CellSignalStrengthLte) signalStrength;
            rssi = lteSignal.getRssi();
            condition = getLteCondition(rssi);
        } else if (signalStrength instanceof CellSignalStrengthCdma) {
            CellSignalStrengthCdma lteSignal = (CellSignalStrengthCdma) signalStrength;
            rssi = lteSignal.getCdmaDbm();
            condition = getCdmaCondition(rssi);
        } else {
            rssi = signalStrength.getDbm();
            condition = getGenericCondition(rssi);
        }

        //if rssi > 0 something went wrong
        if (rssi > 0)
                return  null;
        return new Record(RecordType.Signal, condition, rssi);
    }
}
