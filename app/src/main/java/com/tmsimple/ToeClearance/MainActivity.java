package com.tmsimple.ToeClearance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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
import com.xsens.dot.android.sdk.utils.DotScanner;
import android.text.method.ScrollingMovementMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.widget.Switch;
import android.util.Log;


import org.tensorflow.lite.Interpreter;


public class MainActivity extends AppCompatActivity implements ImuManagerListener {

    public String Version = "v1.2";

    private Segment thigh, foot;
    private DotScanner mXsScanner;
    public String thighMAC = "D4:22:CD:00:63:8B"; //V2-17
    public String footMAC = "D4:22:CD:00:63:D6"; //V2-16

    //RT: "D4:22:CD:00:63:71"
    //RF: "D4:22:CD:00:63:D6"
    //LT: "D4:22:CD:00:63:8B";
    //LF: "D4:22:CD:00:63:A4";
    //LA: "D4:CA:6E:F1:77:9B";
    //RA: "D4:22:CD:00:04:D2";
    //CE-LA : "D4:CA:6E:F1:72:BF";
    public File logFile;
    public FileOutputStream stream = null;
    public ArrayList<File> loggerFilePaths = new ArrayList<>();
    public ArrayList<String> loggerFileNames = new ArrayList<>();
    public int subjectNumber = 0;
    public String subjectTitle;
    public String logFileName;
    public File logFilePath;
    public String subjectDateAndTime;
    public int dataLogButtonIndex = 0;

    private ImuManager imuManager;
    private LogManager logManager;

    Button scanButton, syncButton, measureButton, disconnectButton, stopButton, uploadButton, dataLogButton,
            activity0Button, activity1Button, activity2Button, activity3Button, activity4Button, activity5Button, homeButton,
            activity6Button, activity7Button, activity8Button, activity9Button;
    Switch logSwitch, ImuSwitch;
    private ArrayList<DotDevice> mDeviceLst;
    TextView thighScanStatus, footScanStatus, logContents;
    TextView ValueF1, ValueF2, ValueF3, ValueF4, ValueT1, ValueT2, ValueT3, ValueT4;
    EditText enterSubjectNumber;
    DecimalFormat threePlaces = new DecimalFormat("##.#");
    public int packetCounterCofficient = 0;

    public int MeasurementMode;
    StorageReference storageReference;
    //DatabaseReference databaseReference;
    public int SAMPLE_RATE = 60;
    public String estimationResult;

    private static final int BLUETOOTH_PERMISSION_CODE = 100; //Bluetooth Permission variable
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 101; //Bluetooth Permission variable
    public boolean conditionFindingPeak = false;
    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);

        storageReference = FirebaseStorage.getInstance().getReference();
        checkPermission(android.Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_PERMISSION_CODE);
        checkPermission(android.Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_PERMISSION_CODE);
        checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, BLUETOOTH_PERMISSION_CODE);
        checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, BLUETOOTH_PERMISSION_CODE);

        // ✅ Only create LogManager with null contents for now
        logManager = new LogManager(this, null, null);

        // ✅ Only create ImuManager ONCE here
        imuManager = new ImuManager(this, this, logManager);
    }

    public void LabelingData(View view) {
        setContentView(R.layout.labeling_data);

        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);

        // NOW that logContents exists in this layout
        logManager.setLogContents(logContents);


        scanButton = findViewById(R.id.scanButton);
        syncButton = findViewById(R.id.syncButton);
        measureButton = findViewById(R.id.measureButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        stopButton = findViewById(R.id.stopButton);
        uploadButton = findViewById(R.id.uploadButton);
        dataLogButton = findViewById(R.id.dataLogButton);
        enterSubjectNumber = findViewById(R.id.enterSubjectNumber);
        homeButton = findViewById(R.id.homeButton);

        thighScanStatus = findViewById(R.id.thighStatusView);
        footScanStatus = findViewById(R.id.footStatusView);

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
        ImuSwitch = findViewById(R.id.ImuSwitch);
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
        dataLogButton.setEnabled(false);


        //Log Show UI
        logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                logManager.setLogVisible(isChecked);
            }
        });
        ImuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    thighMAC = "D4:22:CD:00:A1:76";
                    footMAC = "D4:22:CD:00:9F:95";
                    logManager.log("IMU switch is checked for left side");
                    logManager.log(thighMAC);
                    logManager.log(footMAC);

                } else {
                    thighMAC = "D4:22:CD:00:63:8B";
                    footMAC = "D4:22:CD:00:63:A4";
                    logManager.log("IMU switch is checked for right side");
                    logManager.log(thighMAC);
                    logManager.log(footMAC);
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
            public void afterTextChanged(Editable string) {
                if (!string.toString().isEmpty()) {
                    try {
                        subjectNumber = Integer.parseInt(enterSubjectNumber.getText().toString());
                        subjectTitle = "Subject " + subjectNumber;
                        subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                        logFileName = subjectTitle + " " + subjectDateAndTime + ".txt";
                        logFile = new File(logFilePath, logFileName);
                        logManager.setLogFile(logFile);

                        logManager.log("Subject number set: " + subjectNumber);
                        logManager.log("Log File Created");
                        scanButton.setEnabled(true);
                        //enterSubjectNumber.setEnabled(false);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        logManager.log("Subject Number is invalid");
                    }
                }
            }
        });

        dataLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataLogButtonIndex++;
                if (dataLogButtonIndex % 2 == 1) {
                    imuManager.setLoggingData(true);
                    dataLogButton.setBackgroundColor(Color.parseColor("#05edbb"));
                    dataLogButton.setText("Data Logging ...");
                    logManager.log(" ---- Data is Logging -----");
                } else if (dataLogButtonIndex % 2 == 0 && dataLogButtonIndex > 1) {
                    imuManager.setLoggingData(false);
                    dataLogButton.setBackgroundColor(Color.parseColor("#4DBDDF"));
                    dataLogButton.setText("Data Logging Stopped");
                    logManager.log("---- Data Logging Stopped -----");
                }
            }
        });
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.first_page);
            }
        });
    }

//*//*////*//*////*//*////*//*////*//*////*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*//


    /// ///////////////////////////////////  Sequence of syncing  ///////////////////////////////////////////////////////////////////

    public void scanButton_onClick(View view) {

        // Configure IMU segments
        thigh = new Segment("Thigh IMU", thighMAC);
        foot = new Segment("Foot IMU", footMAC);
        imuManager.setSegments(thigh, foot);

        if (imuManager.startScan()) {
            logManager.log("Scan started!");
            scanButton.setText("Scanning ...");
            scanButton.setBackgroundColor(Color.parseColor("#FF9933"));
        } else {
            logManager.log("Failed to start scan.");
        }

    }

    @Override
    public void onImuConnectionChanged(String deviceName, boolean connected) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                thighScanStatus.setText(connected ? "Connected" : "Disconnected");
            } else if (deviceName.equals("Foot IMU")) {
                footScanStatus.setText(connected ? "Connected" : "Disconnected");
            }

            if (!connected && !isSyncing) {
                // Only reset buttons on DISCONNECT
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

    @Override
    public void onImuScanned(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                thighScanStatus.setText("Scanned");
            } else if (deviceName.equals("Foot IMU")) {
                footScanStatus.setText("Scanned");
            }
        });
    }

    @Override
    public void onImuReady(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                thighScanStatus.setText("Ready");
            } else if (deviceName.equals("Foot IMU")) {
                footScanStatus.setText("Ready");
            }
            scanButton.setEnabled(true);
            syncButton.setEnabled(true);
            scanButton.setText("Scanned");
            scanButton.setBackgroundColor(Color.parseColor("#008080"));
        });
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
    }


    public void syncButton_onClick(View view) {
        isSyncing = true;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                syncButton.setText("Syncing...");
                syncButton.setBackgroundColor(Color.parseColor("#FF9933"));
                thighScanStatus.setText("Syncing");
                footScanStatus.setText("Syncing");
            }
        });

        imuManager.startSync();

    }

    @Override
    public void onSyncingDone() {
        isSyncing = false;
        measureButton.setEnabled(true);
        disconnectButton.setEnabled(true);
        runOnUiThread(() -> {
            syncButton.setText("Sync Done");
            syncButton.setBackgroundColor(Color.parseColor("#008080"));
            measureButton.setEnabled(true);
            disconnectButton.setEnabled(true);
            logManager.log("(Main): --- Syncing is done! ---- ");
        });
    }

    public void measureButton_onClick(View view) {

        stopButton.setEnabled(true); // After starting measuring the stop button will be activated
        dataLogButton.setEnabled(true);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                measureButton.setText("Measuring...");
            }
        });

        if (thigh.xsDevice != null)
            thigh.normalDataLogger = createDataLog(thigh.xsDevice);
        if (foot.xsDevice != null)
            foot.normalDataLogger = createDataLog(foot.xsDevice);
        imuManager.startMeasurement();
    }

    //  //////////////////////////////////////////////////// Callbacks ////////////////////////


    @Override
    public void onDataUpdated(String deviceAddress, double[] eulerAngles) {


        //logManager.log("onDataUpdated called for: " + deviceAddress + " euler: " + Arrays.toString(eulerAngles));

        runOnUiThread(() -> {
            if (deviceAddress.equals(thigh.MAC)) {
                ValueT1.setText(String.format(Locale.US, "%.1f deg", eulerAngles[0]));
                ValueT2.setText(String.format(Locale.US, "%.1f deg", eulerAngles[1]));
                ValueT3.setText(String.valueOf(thigh.dataOutput[3]));  // Or show packetCounter if you want
                ValueT4.setText(thigh.xsDevice.getBatteryPercentage() + "%");
            } else if (deviceAddress.equals(foot.MAC)) {
                ValueF1.setText(String.format(Locale.US, "%.1f deg", eulerAngles[0]));
                ValueF2.setText(String.format(Locale.US, "%.1f deg", eulerAngles[1]));
                ValueF3.setText(String.valueOf(foot.dataOutput[3]));
                ValueF4.setText(foot.xsDevice.getBatteryPercentage() + "%");
            }
        });
    }

    @Override
    public void onLogMessage(String message) {
        logManager.log(message);
    }

    /*
    /////////////////////////////////////////////////////////      Functions     //////////////////////////////
     */

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

    public DotLogger createDataLog(DotDevice device) {

        try {
            File loggerFileFolder;
            String loggerFileName;
            loggerFileFolder = this.getApplicationContext().getExternalFilesDir(subjectTitle + "/" + device.getTag());
            loggerFileName = device.getTag() + "_" + java.text.DateFormat.getDateTimeInstance().format(new Date()) + ", Subject " + subjectNumber + ".csv";
            String path = loggerFileFolder.getPath() + "/" + loggerFileName;
            File loggerFile = new File(path);

            loggerFilePaths.add(loggerFile);
            loggerFileNames.add(loggerFileName);
            logManager.log(loggerFileName + " created");

            DotLogger logger = new DotLogger(getApplicationContext(), 1, imuManager.getMeasurementMode(), path, device.getTag(),
                    device.getFirmwareVersion(), true, SAMPLE_RATE, null, Version, 0);
            return logger;


        } catch (NullPointerException e) {
            logManager.log("Error with creation of logger with" + device.getName());
            DotLogger logger = new DotLogger(getApplicationContext(), 1, imuManager.getMeasurementMode(), "", device.getTag(),
                    device.getFirmwareVersion(), true, SAMPLE_RATE, null,
                    Version, 0);
            return logger;
        }
    }
    /*
    ///////////////////////////////////////////////////////         Buttons      //////////////////////
     */


    public void disconnectButton_onClick(View view) {
        measureButton.setEnabled(false);
        dataLogButton.setEnabled(false);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                disconnectButton.setText("Disconnecting...");
                disconnectButton.setBackgroundColor(Color.parseColor("#F60000"));
            }
        });
        imuManager.disconnectAll();

    }

    public void stopButton_onClick(View view) { // After measuring, the dots should be stopped to for data logging

        stopButton.setEnabled(false);
        measureButton.setEnabled(false);
        dataLogButton.setEnabled(false);
        uploadButton.setEnabled(true);
        logManager.log("Stopping");
        imuManager.stopMeasurement();
        measureButton.setText("Measuring Stopped");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                measureButton.setText("Measuring Stopped");
            }
        });

    }

    public void uploadButton_onClick(View view) {
        for (int i = 0; i < loggerFileNames.size(); i++) {
            logManager.log("Uploading data to cloud : " + loggerFileNames.get(i));
            uploadLogFileToCloud(Uri.fromFile(loggerFilePaths.get(i)), loggerFileNames.get(i));
        }

        uploadLogFileToCloud(Uri.fromFile(logFile), "log");
    }

    private void uploadLogFileToCloud(Uri file, String fileName) {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("File is loading...");
        progressDialog.show();

        //This is where you can change the upload location and name
        StorageReference reference = storageReference.child("logs/Subject " + Integer.toString(subjectNumber) + "/" + fileName);

        reference.putFile(file).
                addOnFailureListener(new OnFailureListener() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        uploadButton.setText("Uploading Failed");
                        uploadButton.setBackgroundColor(Color.parseColor("#f63e00"));
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                uploadButton.setText("Uploading Done");
                                uploadButton.setBackgroundColor(Color.parseColor("#0af056"));
                            }
                        });
                        progressDialog.dismiss();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        progressDialog.setMessage("File Uploaded.." + (int) progress + "%");
                    }
                });

    }


}