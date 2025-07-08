package com.tmsimple.ToeClearance;

public interface ImuManagerListener {
    void onImuConnectionChanged(String deviceName, boolean connected);
    void onImuScanned(String deviceName);
    void onImuReady(String deviceName);
    void onSyncingDone();
    void onDataUpdated(String deviceName, double[] eulerAngles);
    void onLogMessage(String message);
}
