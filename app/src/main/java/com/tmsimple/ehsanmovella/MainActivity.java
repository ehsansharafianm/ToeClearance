package com.tmsimple.ehsanmovella;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.xsens.dot.android.sdk.DotSdk;
import com.xsens.dot.android.sdk.events.DotData;
import com.xsens.dot.android.sdk.interfaces.DotDeviceCallback;
import com.xsens.dot.android.sdk.interfaces.DotMeasurementCallback;
import com.xsens.dot.android.sdk.interfaces.DotRecordingCallback;
import com.xsens.dot.android.sdk.interfaces.DotScannerCallback;
import com.xsens.dot.android.sdk.interfaces.DotSyncCallback;
import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.models.DotPayload;
import com.xsens.dot.android.sdk.models.DotRecordingFileInfo;
import com.xsens.dot.android.sdk.models.DotRecordingState;
import com.xsens.dot.android.sdk.models.DotSyncManager;
import com.xsens.dot.android.sdk.models.FilterProfileInfo;
import com.xsens.dot.android.sdk.utils.DotLogger;
import com.xsens.dot.android.sdk.utils.DotParser;
import com.xsens.dot.android.sdk.utils.DotScanner;
import android.text.method.ScrollingMovementMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity implements DotDeviceCallback, DotScannerCallback, DotRecordingCallback, DotSyncCallback, DotMeasurementCallback {


    private Segment leftThigh, leftFoot;
    private DotScanner mXsScanner;
    public  String leftThighMAC = "D4:22:CD:00:63:8B";
    //RT: "D4:22:CD:00:63:71"
    //LT: "D4:22:CD:00:63:8B"
    public String leftFootMAC = "D4:22:CD:00:63:A4";

    public File logFile;
    public FileOutputStream stream = null;
    public ArrayList<File> loggerFilePaths = new ArrayList<>();
    public ArrayList<String> loggerFileNames = new ArrayList<>();
    public int subjectNumber = 0;
    public String subjectTitle;
    public String logFileName;
    public File logFilePath;
    public String subjectDateAndTime;
    Button scanButton, syncButton, measureButton, disconnectButton, stopButton, uploadButton;
    Switch logSwitch;
    private ArrayList<DotDevice> mDeviceLst;
    TextView leftThighScanStatus, leftFootScanStatus, logContents;
    TextView ValueF1, ValueF2, ValueF3, ValueF4, ValueT1, ValueT2, ValueT3, ValueT4;
    EditText enterSubjectNumber;
    DecimalFormat threePlacesT = new DecimalFormat("##.#");
    DecimalFormat threePlacesF = new DecimalFormat("##.#");

    public int MeasurementMode;


    private static final int BLUETOOTH_PERMISSION_CODE = 100; //Bluetooth Permission variable
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 101; //Bluetooth Permission variable

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        leftThigh = new Segment("Left Thigh IMU", leftThighMAC);
        leftFoot = new Segment("Left Foot IMU", leftFootMAC);

        scanButton = findViewById(R.id.scanButton);
        syncButton = findViewById(R.id.syncButton);
        measureButton = findViewById(R.id.measureButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        stopButton = findViewById(R.id.stopButton);
        uploadButton = findViewById(R.id.uploadButton);
        enterSubjectNumber = findViewById(R.id.enterSubjectNumber);

        leftThighScanStatus = findViewById(R.id.leftThighStatusView);
        leftFootScanStatus = findViewById(R.id.leftFootStatusView);

        logFilePath = this.getApplicationContext().getExternalFilesDir("logs");

        ValueF1 = findViewById(R.id.valueF1);
        ValueF2 = findViewById(R.id.valueF2);
        ValueF3 = findViewById(R.id.valueF3);
        ValueF4 = findViewById(R.id.valueF4);
        ValueT1 = findViewById(R.id.valueT1);
        ValueT2 = findViewById(R.id.valueT2);
        ValueT3 = findViewById(R.id.valueT3);
        ValueT4 = findViewById(R.id.valueT4);

        logSwitch = findViewById(R.id.logSwitch);

        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);


        // Before scanning all should be deactive; after each step they will be enabled
        scanButton.setEnabled(false);
        syncButton.setEnabled(false);
        measureButton.setEnabled(false);
        stopButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        uploadButton.setEnabled(false);


        //Xsens Dot On Create Stuff
        DotSdk.setDebugEnabled(true);
        DotSdk.setReconnectEnabled(true);
        mXsScanner = new DotScanner(this.getApplicationContext(), this);
        mXsScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        mDeviceLst = new ArrayList<>();

        //Log Show UI
        logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    logContents.setVisibility(View.VISIBLE);
                }
                else{
                    logContents.setVisibility(View.INVISIBLE);
                }
            }
        });

        enterSubjectNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    subjectNumber = Integer.parseInt(enterSubjectNumber.getText().toString());
                    subjectTitle = "Subject " + subjectNumber;
                    subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                    logFileName = subjectTitle + " " + subjectDateAndTime + ".txt";
                    logFile = new File(logFilePath,logFileName);
                    writeToLogs("Subject number set: " + subjectNumber);
                    writeToLogs("Log File Created");
                    scanButton.setEnabled(true);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    @Override
    public void onDotConnectionChanged(String address, int state) {
        if (state == DotDevice.CONN_STATE_CONNECTED){
            if(address.equals(leftThigh.MAC)){
                if(address.equals(leftThigh.MAC)){
                    leftThigh.isConnected = true;
                    writeToLogs("Left Thigh IMU is connected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {leftThighScanStatus.setText("Connected");}
                    });
                }
            }
            else if(address.equals(leftFoot.MAC)){
                if(address.equals(leftFoot.MAC)){
                    leftFoot.isConnected = true;
                    writeToLogs("Left Foot IMU is connected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {leftFootScanStatus.setText("Connected");}
                    });
                }
            }
            writeToLogs(String.valueOf("Measurement Mode: " + leftThigh.xsDevice.getMeasurementMode())
                                                                 + " / " + leftFoot.xsDevice.getMeasurementMode());
        }
        else if (state == DotDevice.CONN_STATE_DISCONNECTED){
            if(address.equals(leftThigh.MAC)){
                if(address.equals(leftThigh.MAC)){
                    leftThigh.isConnected = false;
                    writeToLogs("Left Thigh IMU is disconnected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {leftThighScanStatus.setText("Disconnected");}
                    });
                }
            }
            else if(address.equals(leftFoot.MAC)){
                if(address.equals(leftFoot.MAC)){
                    leftFoot.isConnected = false;
                    writeToLogs("Left Foot IMU is disconnected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {leftFootScanStatus.setText("Disconnected");}
                    });
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    scanButton.setText("Scan");
                    measureButton.setText("Measure");
                    syncButton.setText("Start Sync");
                    disconnectButton.setText("Disconnect");
                    scanButton.setBackgroundColor(Color.parseColor("#4CAF50"));
                    syncButton.setBackgroundColor(Color.parseColor("#4CAF50"));
                    disconnectButton.setBackgroundColor(Color.parseColor("#FD8888"));
                }
            });
        }

    }

    @Override
    public void onDotDataChanged(String address, DotData dotData) {

        //double[] eulerAngles = dotData.getEuler();
        float[] eulerAngles = dotData.getQuat();
        //double[] eulerAngles = DotParser.quaternion2Euler(dotData.getQuat());

        if (address.equals(leftThigh.MAC)) {
            leftThigh.normalDataLogger.update(dotData);
            leftThigh.sampleCounter++;
            //double[] eulerAngles = DotParser.quaternion2Euler(dotData.getQuat());
            double[] eulerAnglesThigh = dotData.getEuler();

            leftThigh.dataOutput[0] = threePlacesT.format(eulerAngles[0]);
            leftThigh.dataOutput[1] = threePlacesT.format(eulerAngles[1]);
            leftThigh.dataOutput[2] = threePlacesT.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueT1.setText(String.valueOf(leftThigh.dataOutput[0]) + "   deg");
                    ValueT2.setText(String.valueOf(leftThigh.dataOutput[1]) + "   deg");
                    ValueT3.setText(String.valueOf(leftThigh.dataOutput[2]) + "   deg");
                    //ValueT4.setText(String.valueOf(leftThigh.xsDevice.getBatteryPercentage()) + "   %");
                    ValueT4.setText(String.valueOf(leftThigh.sampleCounter));
                }
            });
        }
        else if (address.equals(leftFoot.MAC)) {
            leftFoot.normalDataLogger.update(dotData);
            leftFoot.sampleCounter++;
            //double[] eulerAngles = DotParser.quaternion2Euler(dotData.getQuat());
            double[] eulerAnglesFoot = dotData.getEuler();

            leftFoot.dataOutput[0] = threePlacesF.format(eulerAngles[0]);
            leftFoot.dataOutput[1] = threePlacesF.format(eulerAngles[1]);
            leftFoot.dataOutput[2] = threePlacesF.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF1.setText(String.valueOf(leftFoot.dataOutput[0]) + "   deg");
                    ValueF2.setText(String.valueOf(leftFoot.dataOutput[1]) + "   deg");
                    ValueF3.setText(String.valueOf(leftFoot.dataOutput[2]) + "   deg");
                    ValueF4.setText(String.valueOf(leftFoot.xsDevice.getBatteryPercentage()) + "   %");
                }
            });
        }
    }

    @Override
    public void onDotInitDone(String address) { //onDotInitDone is the callback after the initialization is successful
        if (address.equals(leftThigh.MAC)) {
            leftThigh.isReady = true;
            leftThigh.xsDevice.setOutputRate(60);
            writeToLogs("Left Thigh IMU sample rate is : " + String.valueOf(leftThigh.xsDevice.getCurrentOutputRate()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leftThighScanStatus.setText("Ready");
                }
            });
        }
        else if (address.equals(leftFoot.MAC)) {
            leftFoot.isReady = true;
            leftFoot.xsDevice.setOutputRate(60);
            writeToLogs("Left Foot IMU sample rate is : " + String.valueOf(leftFoot.xsDevice.getCurrentOutputRate()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    leftFootScanStatus.setText("Ready");
                }
            });
        }
        if (leftThigh.isReady && leftFoot.isReady){
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    scanButton.setEnabled(true);
                    syncButton.setEnabled(true);
                    scanButton.setText("Scanned");
                    scanButton.setBackgroundColor(Color.parseColor("#008080"));
                }
            });
            if(mXsScanner.stopScan()){ writeToLogs("Scan Stopped!"); }
        }
    }
    @Override
    public void onDotScanned(BluetoothDevice bluetoothDevice, int i) {
        String address = bluetoothDevice.getAddress();

        MeasurementMode = DotPayload.PAYLOAD_TYPE_CUSTOM_MODE_1;

        if(address.equals(leftThigh.MAC) && !leftThigh.isScanned){
            leftThigh.xsDevice = new DotDevice(this.getApplicationContext(), bluetoothDevice, MainActivity.this);
            leftThigh.xsDevice.connect();
            leftThigh.isConnected = true;
            leftThigh.xsDevice.setMeasurementMode(MeasurementMode);
            writeToLogs(leftThigh.Name + " is scanned and logger is created");
            mDeviceLst.add(leftThigh.xsDevice);
        }
        else if(address.equals(leftFoot.MAC) && !leftFoot.isScanned){
            leftFoot.xsDevice = new DotDevice(this.getApplicationContext(), bluetoothDevice, MainActivity.this);
            leftFoot.xsDevice.connect();
            leftFoot.isConnected = true;
            leftFoot.xsDevice.setMeasurementMode(MeasurementMode);
            mDeviceLst.add(leftFoot.xsDevice);
            writeToLogs(leftFoot.Name + " is scanned and logger is created");
        }
        if(leftThigh.isScanned && leftFoot.isScanned){
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run(){leftFootScanStatus.setText("Scanned");}
            });
        }
    }
    @Override
    public void onSyncingDone(HashMap<String, Boolean> hashMap, boolean b, int i) {
        measureButton.setEnabled(true);
        disconnectButton.setEnabled(true);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                syncButton.setText("Sync Done");
                syncButton.setBackgroundColor(Color.parseColor("#008080"));
            }
        });
        writeToLogs("\n ---------- Syncing is done! --------- \n");
    }



    /*
    /////////////////////////////////////////////////////////      Functions     //////////////////////////////
     */
    // Write to Log view and file
    public void writeToLogs(String logMessage) {
        //Write to Log View UI
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logContents.append("\n" + logMessage);
                    // Scroll to the bottom to ensure the latest text is visible
                    final int scrollAmount = logContents.getLayout().getLineTop(logContents.getLineCount()) - logContents.getHeight();
                    // If there is no need to scroll, scrollAmount will be <=0
                    if (scrollAmount > 0)
                        logContents.scrollTo(0, scrollAmount);
                    else
                        logContents.scrollTo(0, 0);
                }
            });
        } catch (IndexOutOfBoundsException e){
            e.printStackTrace();
        }

        //Write To Log File:

        String logMessageWithDateTime = java.text.DateFormat.getDateTimeInstance().format(new Date()) + ": " + logMessage + "\n";
        try {
            stream = new FileOutputStream(logFile, true);
            stream.write(logMessageWithDateTime.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            logContents.append("\n" + "Error: Log file not found");
            //errorMessagePopUp("Error: Log file not found");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    private void checkPermission(String permission, int requestCode) {

        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
        } else {
            ////Toast.makeText(Page3.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(Page3.this, "Bluetooth Connect Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(Page3.this, "Bluetooth Connect Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == BLUETOOTH_SCAN_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(Page3.this, "Bluetooth Scan Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(Page3.this, "Bluetooth Scan Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public DotLogger createDataLog(DotDevice device){

        try {
            File loggerFileFolder;
            String loggerFileName;
            loggerFileFolder = this.getApplicationContext().getExternalFilesDir(subjectTitle + "/" + device.getTag());
            loggerFileName = device.getTag() + "_" + java.text.DateFormat.getDateTimeInstance().format(new Date()) + ".csv";
            String path = loggerFileFolder.getPath() + "/" + loggerFileName;
            File loggerFile = new File(path);

            loggerFilePaths.add(loggerFile);
            loggerFileNames.add(loggerFileName);
            writeToLogs(loggerFileName + " created");

            DotLogger logger = new DotLogger(getApplicationContext(), 1,MeasurementMode,path,device.getTag(),device.getFirmwareVersion(),true,60,null,"29",0);
            return logger;


        } catch (NullPointerException e) {
            writeToLogs("Error with creation of logger with" + device.getName());
            DotLogger logger = new DotLogger(getApplicationContext(), 1, MeasurementMode, "", device.getTag(),
                    device.getFirmwareVersion(), true, 60, null, "24", 0);
            return logger;
        }
    }
    /*
    ///////////////////////////////////////////////////////         Buttons      //////////////////////
     */
    public void scanButton_onClick(View view){
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                scanButton.setText("Scanning ...");
                scanButton.setBackgroundColor(Color.parseColor("#FF9933"));
            }
        });

        checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE);
        checkPermission(android.Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_PERMISSION_CODE);

        if(mXsScanner.startScan()) { writeToLogs("Scan started!"); }
    }

    public void syncButton_onClick(View view){
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                syncButton.setText("Syncing...");
                syncButton.setBackgroundColor(Color.parseColor("#FF9933"));
                leftThighScanStatus.setText("Syncing");
                leftFootScanStatus.setText("Syncing");
            }
        });
        mDeviceLst.get(0).setRootDevice(true);
        DotSyncManager.getInstance(this).startSyncing(mDeviceLst, 100);
    }
    public void measureButton_onClick(View view){

        stopButton.setEnabled(true); // After starting measuring the stop button will be activated
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {measureButton.setText("Measuring...");}
        });


        if (leftThigh.xsDevice != null)
            leftThigh.normalDataLogger = createDataLog(leftThigh.xsDevice);
        if (leftFoot.xsDevice != null)
            leftFoot.normalDataLogger = createDataLog(leftFoot.xsDevice);

        if (leftThigh.xsDevice.startMeasuring()) {writeToLogs("Left Thigh IMU is measuring");}
        if (leftFoot.xsDevice.startMeasuring()) {writeToLogs("Left Foot IMU is measuring");}

    }

    public void disconnectButton_onClick(View view){
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                disconnectButton.setText("Disconnecting...");
                disconnectButton.setBackgroundColor(Color.parseColor("#F60000"));
            }
        });
        leftThigh.xsDevice.disconnect();
        leftFoot.xsDevice.disconnect();
    }
    public void stopButton_onClick(View view){ // After measuring, the dots should be stopped to for data logging

        stopButton.setEnabled(false);
        uploadButton.setEnabled(true);
        writeToLogs("Stopping");
        if(leftThigh.xsDevice != null){
            try{
                leftThigh.xsDevice.stopMeasuring();
                leftThigh.normalDataLogger.stop();
                writeToLogs(leftThigh.Name + " measuring stopped");
            }catch (NullPointerException e) {
                writeToLogs("Error: Not connected to " + leftThigh.Name);
                e.printStackTrace();
            }
        }
        if(leftFoot.xsDevice != null){try{
            leftFoot.xsDevice.stopMeasuring();
            leftFoot.normalDataLogger.stop();
            writeToLogs(leftFoot.Name + " measuring stopped");
        }catch (NullPointerException e) {
            writeToLogs("Error: Not connected to " + leftFoot.Name);
            e.printStackTrace();
        }}
    }
    public void uploadButton_onClick (View view){
        for (int i = 0; i < loggerFileNames.size(); i++) {
            writeToLogs("Uploading data to cloud : " + loggerFileNames.get(i));
        }
    }

    /*
        ///////////////////////////////////////////////////////        Unused xsDevice functions      //////////////////////
         */
    @Override
    public void onDotButtonClicked(String s, long l) {

    }
    @Override
    public void onDotPowerSavingTriggered(String s) {

    }
    @Override
    public void onReadRemoteRssi(String s, int i) {

    }
    @Override
    public void onDotOutputRateUpdate(String s, int i) {

    }
    @Override
    public void onDotFilterProfileUpdate(String s, int i) {

    }
    @Override
    public void onDotGetFilterProfileInfo(String s, ArrayList<FilterProfileInfo> arrayList) {

    }
    @Override
    public void onSyncStatusUpdate(String s, boolean b) {

    }
    @Override
    public void onDotRecordingNotification(String address, boolean isEnabled) {

    }
    @Override
    public void onDotEraseDone(String s, boolean b) {

    }
    @Override
    public void onDotRequestFlashInfoDone(String s, int i, int i1) {

    }
    @Override
    public void onDotRecordingAck(String s, int i, boolean b, DotRecordingState dotRecordingState) {

    }
    @Override
    public void onDotGetRecordingTime(String s, int i, int i1, int i2) {

    }
    @Override
    public void onDotRequestFileInfoDone(String s, ArrayList<DotRecordingFileInfo> arrayList, boolean b) {

    }
    @Override
    public void onDotDataExported(String s, DotRecordingFileInfo dotRecordingFileInfo, DotData dotData) {

    }
    @Override
    public void onDotDataExported(String s, DotRecordingFileInfo dotRecordingFileInfo) {

    }
    @Override
    public void onDotAllDataExported(String s) {

    }
    @Override
    public void onDotStopExportingData(String s) {

    }
    @Override
    public void onSyncingStarted(String s, boolean b, int i) {

    }
    @Override
    public void onSyncingProgress(int i, int i1) {

    }
    @Override
    public void onSyncingResult(String s, boolean b, int i) {

    }
    @Override
    public void onSyncingStopped(String s, boolean b, int i) {

    }
    @Override
    public void onDotServicesDiscovered(String s, int i) {
    }
    @Override
    public void onDotFirmwareVersionRead(String s, String s1) {
    }
    @Override
    public void onDotTagChanged(String s, String s1) {

    }
    @Override
    public void onDotBatteryChanged(String address, int i, int i1) {
    }


    @Override
    public void onDotHeadingChanged(String s, int i, int i1) {

    }

    @Override
    public void onDotRotLocalRead(String s, float[] floats) {

    }
}//END OF THE CODE