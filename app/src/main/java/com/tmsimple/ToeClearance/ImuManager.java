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
        DotMeasurementCallback,
        ZuptDetector.ZuptListener {

    private Context context;
    private ImuManagerListener listener;

    private DotScanner mScanner;
    private ArrayList<DotDevice> deviceList;
    private ZuptDetector zuptDetector;

    private Segment IMU1, IMU2;
    private boolean isLoggingData = false;
    private int packetCounterOffset = 0;

    private LogManager logManager;
    int measurementMode;


    DecimalFormat decimalFormat = new DecimalFormat("##.##");

    public ImuManager(Context context, ImuManagerListener listener, LogManager logManager) {
        this.context = context;
        this.listener = listener;
        this.logManager = logManager;

        // Initialize zuptDetector with UiManager reference (will be set later)
        this.zuptDetector = new ZuptDetector(this, logManager);

        DotSdk.setDebugEnabled(true);
        DotSdk.setReconnectEnabled(true);

        mScanner = new DotScanner(context, this);
        mScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);

        deviceList = new ArrayList<>();
    }


    @Override
    public void onZuptDetected(String imuId, int packetCounter) {
        // This gets called when ZUPT starts - you can leave it empty for now
        // or add any additional logic you want when ZUPT is detected
    }

    @Override
    public void onZuptEnded(String imuId, int packetCounter) {
        // This gets called when ZUPT ends - you can leave it empty for now
        // or add any additional logic you want when ZUPT ends
    }

    @Override
    public void onZuptDataUpdated(String imuId, double gyroMag, double linearAccelMag) {

        // This forwards the magnitude data to MainActivity for UI updates
        if (listener != null) {
            listener.onZuptDataUpdated(imuId, gyroMag, linearAccelMag);
        } else {
            logManager.log("ERROR: MainActivity listener is null!");
        }
    }

    public void setSegments(Segment IMU1, Segment IMU2) {
        this.IMU1 = IMU1;
        this.IMU2 = IMU2;
    }
    public int getMeasurementMode() {
        return measurementMode;
    }

    public boolean startScan() {
        return mScanner.startScan();
    }

    public void setPacketCounterOffset(int packetCounterOffset) {
        this.packetCounterOffset = packetCounterOffset;
    }

    @Override
    public void onDotScanned(android.bluetooth.BluetoothDevice bluetoothDevice, int rssi) {

        if (IMU1 == null || IMU2 == null) {
            logManager.log("Error: Segments not initialized before scanning!");
            return;
        }
        String address = bluetoothDevice.getAddress();

        if (address.equals(IMU1.MAC) && !IMU1.isScanned) {
            IMU1.isScanned = true;
            IMU1.xsDevice = new DotDevice(context, bluetoothDevice, this);
            IMU1.xsDevice.connect();
            IMU1.isConnected = true;
            deviceList.add(IMU1.xsDevice);

            listener.onImuScanned(IMU1.Name);
            logManager.log(IMU1.Name + " is scanned and logger is created");
        }
        else if (address.equals(IMU2.MAC) && !IMU2.isScanned) {
            IMU2.isScanned = true;
            IMU2.xsDevice = new DotDevice(context, bluetoothDevice, this);
            IMU2.xsDevice.connect();
            deviceList.add(IMU2.xsDevice);
            IMU2.isConnected = true;

            listener.onImuScanned(IMU2.Name);
            logManager.log(IMU2.Name + " is scanned and logger is created");
        }

        if (IMU1.isScanned && IMU2.isScanned) {
            mScanner.stopScan();
            logManager.log("Both Devices are Scanned");
        }
    }
    @Override
    public void onDotInitDone(String address) {
        if (address.equals(IMU1.MAC)) {
            IMU1.isReady = true;
            IMU1.xsDevice.setOutputRate(60);
            listener.onImuReady(IMU1.Name);
            logManager.log("IMU1 IMU sample rate is : " + String.valueOf(IMU1.xsDevice.getCurrentOutputRate()));
        }
        else if (address.equals(IMU2.MAC)) {
            IMU2.isReady = true;
            IMU2.xsDevice.setOutputRate(60);
            listener.onImuReady(IMU2.Name);
            logManager.log("IMU2 IMU sample rate is : " + String.valueOf(IMU2.xsDevice.getCurrentOutputRate()));
        }
    }

    public void startSync() {

        DotSyncManager.getInstance(this).stopSyncing();

        logManager.log("Start Sync clicked");
        logManager.log("Device List size: " + deviceList.size());

        if (!IMU1.isReady || !IMU2.isReady) {
            logManager.log("Error: Devices not ready for syncing. IMU1 ready: " + IMU1.isReady + ", IMU2 ready: " + IMU2.isReady);
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
        IMU1.xsDevice.setMeasurementMode(measurementMode);
        IMU2.xsDevice.setMeasurementMode(measurementMode);

        // Optional: store if you need
        // this.measurementMode = measurementMode;

        // 2️⃣ Log detailed mode info
        logManager.log("Measurement Mode set: "
                + IMU1.xsDevice.getMeasurementMode()
                + " / "
                + IMU2.xsDevice.getMeasurementMode());

        // 3️⃣ Log syncing result
        logManager.log("---------- Syncing is done! ----------");

        // 4️⃣ Notify UI through listener
        listener.onSyncingDone();
    }

    public void startMeasurement() {

        if (IMU1.xsDevice.startMeasuring()) {logManager.log("Left IMU1 IMU is measuring");}
        if (IMU2.xsDevice.startMeasuring()) {logManager.log("Left IMU2 IMU is measuring");}
    }

    public void stopMeasurement() {
        IMU1.xsDevice.stopMeasuring();
        IMU1.normalDataLogger.stop();
        IMU2.xsDevice.stopMeasuring();
        IMU2.normalDataLogger.stop();
    }

    public void disconnectAll() {
        IMU1.xsDevice.disconnect();
        IMU2.xsDevice.disconnect();
    }


    // Callbacks will go here in the next step

    @Override
    public void onDotConnectionChanged(String address, int state) {


        boolean isConnected = (state == DotDevice.CONN_STATE_CONNECTED);

        if (state == DotDevice.CONN_STATE_CONNECTED){
            if(address.equals(IMU1.MAC)){
                if(address.equals(IMU1.MAC)){
                    IMU1.isConnected = true;
                    logManager.log("IMU1 IMU is connected!");
                    listener.onImuConnectionChanged(IMU1.Name, isConnected);
                }
            }
            else if(address.equals(IMU2.MAC)){
                if(address.equals(IMU2.MAC)){
                    IMU2.isConnected = true;
                    logManager.log("IMU2 IMU is connected!");
                    listener.onImuConnectionChanged(IMU2.Name, isConnected);
                }
            }

        }
        else if (state == DotDevice.CONN_STATE_DISCONNECTED){
            if(address.equals(IMU1.MAC)){
                if(address.equals(IMU1.MAC)){
                    IMU1.isConnected = false;
                    logManager.log("IMU1 IMU is disconnected!");
                    listener.onImuConnectionChanged(IMU1.Name, isConnected);
                }
            }
            else if(address.equals(IMU2.MAC)){
                if(address.equals(IMU2.MAC)){
                    IMU2.isConnected = false;
                    logManager.log("IMU2 IMU is disconnected!");
                    listener.onImuConnectionChanged(IMU2.Name, isConnected);
                }
            }
        }



    }

    @Override
    public void onDotDataChanged(String address, com.xsens.dot.android.sdk.events.DotData dotData) {
        final float[] quats = dotData.getQuat();
        final double[] eulerAngles = DotParser.quaternion2Euler(quats);
        final double[] gyroData = dotData.getGyr();
        final double[] accelData = dotData.getAcc();

        if (address.equals(IMU1.MAC)) {
            dotData.setPacketCounter(dotData.getPacketCounter() + packetCounterOffset);
            // Calculate initial values during standing
            calculateInitialValues(IMU1, dotData, eulerAngles, gyroData, accelData);

            // Apply calibration
            double[] calibrated = applyCalibratedData(IMU1, eulerAngles, gyroData, accelData);
            eulerAngles[0] = calibrated[0]; // Use calibrated roll

        } else if (address.equals(IMU2.MAC)) {
            dotData.setPacketCounter(dotData.getPacketCounter() + packetCounterOffset);
            // Same for IMU2
            calculateInitialValues(IMU2, dotData, eulerAngles, gyroData, accelData);

            double[] calibrated = applyCalibratedData(IMU2, eulerAngles, gyroData, accelData);
            eulerAngles[0] = calibrated[0];
        }

        if (listener != null) {
            listener.onDataUpdated(address, eulerAngles);
        }


        // ZUPT PROCESSING - happens always (outside logging condition)
        if (address.equals(IMU1.MAC)) {
            // Use calibrated data for ZUPT
            double[] calibrated = applyCalibratedData(IMU1, eulerAngles, gyroData, accelData);
            double calibratedGyro = calibrated[1];
            double calibratedAccel = calibrated[2];
            zuptDetector.processNewImuData("IMU1", calibratedGyro, calibratedAccel, eulerAngles[0], dotData.getPacketCounter());

        } else if (address.equals(IMU2.MAC)) {
            // Use calibrated data for ZUPT
            double[] calibrated = applyCalibratedData(IMU2, eulerAngles, gyroData, accelData);
            double calibratedGyro = calibrated[1];
            double calibratedAccel = calibrated[2];
            zuptDetector.processNewImuData("IMU2", calibratedGyro, calibratedAccel, eulerAngles[0], dotData.getPacketCounter());
        }


        // PRESERVE THE ORIGINAL LOGGING SECTION
        if (isLoggingData) {
            if (address.equals(IMU1.MAC)) {

                IMU1.normalDataLogger.update(dotData);
                IMU1.sampleCounter++;
                IMU1.dataOutput[3] = decimalFormat.format(dotData.getPacketCounter());

            } else if (address.equals(IMU2.MAC)) {

                IMU2.normalDataLogger.update(dotData);
                IMU2.sampleCounter++;
                IMU2.dataOutput[3] = decimalFormat.format(dotData.getPacketCounter());
            }
        }


    }

    public void setLoggingData(boolean logging) {
        this.isLoggingData = logging;
    }

    public void calculateInitialValues(Segment segment, com.xsens.dot.android.sdk.events.DotData dotData, double[] eulerAngles, double[] gyroData, double[] accelData) {

        if (dotData.getPacketCounter() > 1000000 && dotData.getPacketCounter() < 2000000) { // Standing mode

            // Accumulate all sensor values
            segment.sumOfInitialRoll += eulerAngles[0];
            segment.sumOfInitialGyro += Math.sqrt(
                                            gyroData[0] * gyroData[0] +
                                            gyroData[1] * gyroData[1] +
                                            gyroData[2] * gyroData[2]);
            segment.sumOfInitialAccel += Math.sqrt(
                                            accelData[0] * accelData[0] +
                                            accelData[1] * accelData[1] +
                                            accelData[2] * accelData[2]);

            // Calculate running averages
            segment.initRollValue = segment.sumOfInitialRoll / segment.initializationCounter;
            segment.initGyroValue = segment.sumOfInitialGyro / segment.initializationCounter;
            segment.initAccelValue = segment.sumOfInitialAccel / segment.initializationCounter;

            // Log every 2 seconds (120 samples at 60Hz)
            if (segment.initializationCounter % 120 == 119) {
                logManager.log("Initial values for " + segment.Name + ":");
                logManager.log("  Roll: " + decimalFormat.format(segment.initRollValue));

                logManager.log("  Gyro: " +
                                decimalFormat.format(segment.initGyroValue));

                logManager.log("  Accel: " +
                        decimalFormat.format(segment.initAccelValue));
            }

            segment.initializationCounter++;
        }
    }

    // Apply calibration offsets
    private double[] applyCalibratedData(Segment segment, double[] eulerAngles, double[] gyroData, double[] accelData) {
        double[] calibrated = new double[7];

        // Subtract initial values from current measurements
        calibrated[0] = eulerAngles[0] - segment.initRollValue;    // Roll

        calibrated[1] = Math.sqrt(gyroData[0]*gyroData[0] + gyroData[1]*gyroData[1] + gyroData[2]*gyroData[2]) -
                        segment.initGyroValue;

        calibrated[2] = Math.sqrt(accelData[0]*accelData[0] + accelData[1]*accelData[1] + accelData[2]*accelData[2]) -
                        segment.initAccelValue;

        return calibrated;
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
