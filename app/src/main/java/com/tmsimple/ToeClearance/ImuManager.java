package com.tmsimple.ToeClearance;

import android.content.Context;

import com.xsens.dot.android.sdk.DotSdk;
import com.xsens.dot.android.sdk.interfaces.DotDeviceCallback;
import com.xsens.dot.android.sdk.interfaces.DotMeasurementCallback;
import com.xsens.dot.android.sdk.interfaces.DotRecordingCallback;
import com.xsens.dot.android.sdk.interfaces.DotScannerCallback;
import com.xsens.dot.android.sdk.interfaces.DotSyncCallback;
import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.models.DotPayload;
import com.xsens.dot.android.sdk.models.DotSyncManager;
import com.xsens.dot.android.sdk.utils.DotParser;
import com.xsens.dot.android.sdk.utils.DotScanner;
import android.bluetooth.le.ScanSettings;
import android.graphics.Color;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ImuManager implements
        DotDeviceCallback,
        DotScannerCallback,
        DotRecordingCallback,
        DotSyncCallback,
        DotMeasurementCallback {

    private Context context;
    private ImuManagerListener listener;

    private DotScanner mScanner;
    private ArrayList<DotDevice> deviceList;

    private Segment thigh;
    private Segment foot;
    private boolean isLoggingData = false;
    private int packetCounterCoefficient = 0;

    private LogManager logManager;
    int measurementMode;


    DecimalFormat threePlaces = new DecimalFormat("##.#");

    public ImuManager(Context context, ImuManagerListener listener, LogManager logManager) {
        this.context = context;
        this.listener = listener;
        this.logManager = logManager;

        DotSdk.setDebugEnabled(true);
        DotSdk.setReconnectEnabled(true);

        mScanner = new DotScanner(context, this);
        mScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);

        deviceList = new ArrayList<>();
    }

    public void setSegments(Segment thigh, Segment foot) {
        this.thigh = thigh;
        this.foot = foot;
    }
    public int getMeasurementMode() {
        return measurementMode;
    }

    public boolean startScan() {
        return mScanner.startScan();
    }

    @Override
    public void onDotScanned(android.bluetooth.BluetoothDevice bluetoothDevice, int rssi) {

        if (thigh == null || foot == null) {
            logManager.log("Error: Segments not initialized before scanning!");
            return;
        }
        String address = bluetoothDevice.getAddress();

        if (address.equals(thigh.MAC) && !thigh.isScanned) {
            thigh.isScanned = true;
            thigh.xsDevice = new DotDevice(context, bluetoothDevice, this);
            thigh.xsDevice.connect();
            thigh.isConnected = true;
            deviceList.add(thigh.xsDevice);

            listener.onImuScanned(thigh.Name);
            logManager.log(thigh.Name + " is scanned and logger is created");
        }
        else if (address.equals(foot.MAC) && !foot.isScanned) {
            foot.isScanned = true;
            foot.xsDevice = new DotDevice(context, bluetoothDevice, this);
            foot.xsDevice.connect();
            deviceList.add(foot.xsDevice);
            foot.isConnected = true;

            listener.onImuScanned(foot.Name);
            logManager.log(foot.Name + " is scanned and logger is created");
        }

        if (thigh.isScanned && foot.isScanned) {
            mScanner.stopScan();
            logManager.log("Both Devices are Scanned");
        }
    }
    @Override
    public void onDotInitDone(String address) {
        if (address.equals(thigh.MAC)) {
            thigh.isReady = true;
            thigh.xsDevice.setOutputRate(60);
            listener.onImuReady(thigh.Name);
            logManager.log("Thigh IMU sample rate is : " + String.valueOf(thigh.xsDevice.getCurrentOutputRate()));
        }
        else if (address.equals(foot.MAC)) {
            foot.isReady = true;
            foot.xsDevice.setOutputRate(60);
            listener.onImuReady(foot.Name);
            logManager.log("Foot IMU sample rate is : " + String.valueOf(foot.xsDevice.getCurrentOutputRate()));
        }
    }

    public void startSync() {

        DotSyncManager.getInstance(this).stopSyncing();

        logManager.log("Start Sync clicked");
        logManager.log("Device List size: " + deviceList.size());

        if (!thigh.isReady || !foot.isReady) {
            logManager.log("Error: Devices not ready for syncing. Thigh ready: " + thigh.isReady + ", Foot ready: " + foot.isReady);
            return;
        }

        deviceList.get(0).setRootDevice(true);
        logManager.log("Root device set: " + deviceList.get(0).getTag());
        DotSyncManager.getInstance(this).startSyncing(deviceList, 100);
        logManager.log("Sync requested.");
    }
    @Override
    public void onSyncingDone(HashMap<String, Boolean> results, boolean allSuccess, int errorCode) {
        // 1️⃣ Set measurement mode
        measurementMode = DotPayload.PAYLOAD_TYPE_CUSTOM_MODE_5;
        thigh.xsDevice.setMeasurementMode(measurementMode);
        foot.xsDevice.setMeasurementMode(measurementMode);

        // Optional: store if you need
        // this.measurementMode = measurementMode;

        // 2️⃣ Log detailed mode info
        logManager.log("Measurement Mode set: "
                + thigh.xsDevice.getMeasurementMode()
                + " / "
                + foot.xsDevice.getMeasurementMode());

        // 3️⃣ Log syncing result
        logManager.log("---------- Syncing is done! ----------");

        // 4️⃣ Notify UI through listener
        listener.onSyncingDone();
    }

    public void startMeasurement() {

        if (thigh.xsDevice.startMeasuring()) {logManager.log("Left Thigh IMU is measuring");}
        if (foot.xsDevice.startMeasuring()) {logManager.log("Left Foot IMU is measuring");}
    }

    public void stopMeasurement() {
        thigh.xsDevice.stopMeasuring();
        thigh.normalDataLogger.stop();
        foot.xsDevice.stopMeasuring();
        foot.normalDataLogger.stop();
    }

    public void disconnectAll() {
        thigh.xsDevice.disconnect();
        foot.xsDevice.disconnect();
    }


    // Callbacks will go here in the next step

    @Override
    public void onDotConnectionChanged(String address, int state) {


        boolean isConnected = (state == DotDevice.CONN_STATE_CONNECTED);

        if (state == DotDevice.CONN_STATE_CONNECTED){
            if(address.equals(thigh.MAC)){
                if(address.equals(thigh.MAC)){
                    thigh.isConnected = true;
                    logManager.log("Thigh IMU is connected!");
                    listener.onImuConnectionChanged(thigh.Name, isConnected);
                }
            }
            else if(address.equals(foot.MAC)){
                if(address.equals(foot.MAC)){
                    foot.isConnected = true;
                    logManager.log("Foot IMU is connected!");
                    listener.onImuConnectionChanged(foot.Name, isConnected);
                }
            }

        }
        else if (state == DotDevice.CONN_STATE_DISCONNECTED){
            if(address.equals(thigh.MAC)){
                if(address.equals(thigh.MAC)){
                    thigh.isConnected = false;
                    logManager.log("Thigh IMU is disconnected!");
                    listener.onImuConnectionChanged(thigh.Name, isConnected);
                }
            }
            else if(address.equals(foot.MAC)){
                if(address.equals(foot.MAC)){
                    foot.isConnected = false;
                    logManager.log("Foot IMU is disconnected!");
                    listener.onImuConnectionChanged(foot.Name, isConnected);
                }
            }
        }



    }




    @Override
    public void onDotDataChanged(String address, com.xsens.dot.android.sdk.events.DotData dotData) {

        final float[] quats = dotData.getQuat();
        final double[] eulerAngles = DotParser.quaternion2Euler(quats);

        /*logManager.log("onDotDataChanged: " + address + " euler: " +
                String.format(Locale.US, "[%.3f, %.3f, %.3f]",
                        eulerAngles[0], eulerAngles[1], eulerAngles[2]));*/

        if (address.equals(thigh.MAC)) {
            thigh.angleValue = eulerAngles[0];
        }
        else if (address.equals(foot.MAC)) {
            foot.angleValue = eulerAngles[0];
        }
        if (listener != null) {
            listener.onDataUpdated(address, eulerAngles);
        }

        if (isLoggingData) {
            if (address.equals(thigh.MAC)) {
                thigh.normalDataLogger.update(dotData);
                thigh.sampleCounter++;
                thigh.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            } else if (address.equals(foot.MAC)) {
                foot.normalDataLogger.update(dotData);
                foot.sampleCounter++;
                foot.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            }

        }
    }
    public void setLoggingData(boolean logging) {
        this.isLoggingData = logging;
    }

    @Override
    public void onDotButtonClicked(String s, long l) {}

    @Override
    public void onDotPowerSavingTriggered(String s) {}

    @Override
    public void onReadRemoteRssi(String s, int i) {}

    @Override
    public void onDotOutputRateUpdate(String s, int i) {}

    @Override
    public void onDotFilterProfileUpdate(String s, int i) {}

    @Override
    public void onDotGetFilterProfileInfo(String s, java.util.ArrayList<com.xsens.dot.android.sdk.models.FilterProfileInfo> arrayList) {}

    @Override
    public void onSyncStatusUpdate(String s, boolean b) {}

    @Override
    public void onDotRecordingNotification(String address, boolean isEnabled) {}

    @Override
    public void onDotEraseDone(String s, boolean b) {}

    @Override
    public void onDotRequestFlashInfoDone(String s, int i, int i1) {}

    @Override
    public void onDotRecordingAck(String s, int i, boolean b, com.xsens.dot.android.sdk.models.DotRecordingState dotRecordingState) {}

    @Override
    public void onDotGetRecordingTime(String s, int i, int i1, int i2) {}

    @Override
    public void onDotRequestFileInfoDone(String s, java.util.ArrayList<com.xsens.dot.android.sdk.models.DotRecordingFileInfo> arrayList, boolean b) {}

    @Override
    public void onDotDataExported(String s, com.xsens.dot.android.sdk.models.DotRecordingFileInfo dotRecordingFileInfo, com.xsens.dot.android.sdk.events.DotData dotData) {}

    @Override
    public void onDotDataExported(String s, com.xsens.dot.android.sdk.models.DotRecordingFileInfo dotRecordingFileInfo) {}

    @Override
    public void onDotAllDataExported(String s) {}

    @Override
    public void onDotStopExportingData(String s) {}

    @Override
    public void onSyncingStarted(String s, boolean b, int i) {}

    @Override
    public void onSyncingProgress(int i, int i1) {}

    @Override
    public void onSyncingResult(String s, boolean b, int i) {}

    @Override
    public void onSyncingStopped(String s, boolean b, int i) {}

    @Override
    public void onDotServicesDiscovered(String s, int i) {}

    @Override
    public void onDotFirmwareVersionRead(String s, String s1) {}

    @Override
    public void onDotTagChanged(String s, String s1) {}

    @Override
    public void onDotBatteryChanged(String s, int i, int i1) {}

    @Override
    public void onDotHeadingChanged(String s, int i, int i1) {}

    @Override
    public void onDotRotLocalRead(String s, float[] floats) {}




}
