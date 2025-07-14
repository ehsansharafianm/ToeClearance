package com.tmsimple.ToeClearance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.utils.DotLogger;
import android.text.method.ScrollingMovementMethod;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements ImuManagerListener {

    //public String Version = "v1.2";
    //public int SAMPLE_RATE = 60; //Hz


    private Segment thigh, foot;
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
    //public ArrayList<File> loggerFilePaths = new ArrayList<>();
    //public ArrayList<String> loggerFileNames = new ArrayList<>();
    public int subjectNumber = 0;
    public String subjectTitle;
    public String logFileName;
    public File logFilePath;
    public String subjectDateAndTime;
    private ImuManager imuManager;
    private LogManager logManager;
    private UiManager uiManager;
    private PermissionManager permissionManager;


    TextView logContents;
    StorageReference storageReference;


    private static final int BLUETOOTH_PERMISSION_CODE = 100; //Bluetooth Permission variable
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 101; //Bluetooth Permission variable
    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);

        storageReference = FirebaseStorage.getInstance().getReference();
        logManager = new LogManager(this, logContents, null);
        permissionManager = new PermissionManager(this, logManager);
        permissionManager.requestAllPermissions();

    }

    public void LabelingData(View view) {
        setContentView(R.layout.labeling_data);


        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);

        // NOW that logContents exists in this layout
        logManager.setLogContents(logContents);

        // Set the root
        uiManager = new UiManager();
        uiManager.bindLabelingDataViews(getWindow().getDecorView().getRootView());


        logFilePath = this.getApplicationContext().getExternalFilesDir("logs");


        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);

        imuManager = new ImuManager(this, this, logManager);
        logManager.setImuManager(imuManager);


        // Before scanning all should be deactive; after each step they will be enabled

        uiManager.setButton(uiManager.scanButton,null, null, false);
        uiManager.setButton(uiManager.syncButton, null, null, false);
        uiManager.setButton(uiManager.measureButton, null, null, false);
        uiManager.setButton(uiManager.stopButton, null, null, false);
        uiManager.setButton(uiManager.disconnectButton, null, null, false);
        uiManager.setButton(uiManager.uploadButton, null, null, false);
        uiManager.setButton(uiManager.dataLogButton, null, null, false);



        uiManager.setLogSwitchHandler(uiManager.logSwitch, logManager);
        uiManager.setImuSwitchHandler(uiManager.ImuSwitch, logManager, new UiManager.OnImuSwitchChangedListener() {
            @Override
            public void onLeftSideSelected() {
                thighMAC = "D4:22:CD:00:A1:76";
                footMAC = "D4:22:CD:00:9F:95";
                logManager.log(thighMAC);
                logManager.log(footMAC);
            }
            @Override
            public void onRightSideSelected() {
                thighMAC = "D4:22:CD:00:63:8B";
                footMAC = "D4:22:CD:00:63:A4";
                logManager.log(thighMAC);
                logManager.log(footMAC);
            }
        });

        uiManager.setEnterSubjectNumberHandler(uiManager.enterSubjectNumber, new UiManager.OnSubjectNumberEnteredListener() {
            @Override
            public void onSubjectNumberEntered(int subjcetNu) {

                subjectTitle = "Subject " + subjcetNu;
                subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                logFileName = subjectTitle + " " + subjectDateAndTime + ".txt";
                logFile = new File(logFilePath, logFileName);
                logManager.setLogFile(logFile, subjcetNu);
                subjectNumber = subjcetNu;
            }
        });

        // DataLogger Button
        uiManager.setDataLogButtonHandler(uiManager.dataLogButton, logManager, imuManager);

        // Go back to firt page Button
        uiManager.setHomeButtonHandler(uiManager.homeButton, () -> {setContentView(R.layout.first_page);});
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

            uiManager.setButton(uiManager.scanButton, "Scanning ...", "#FF9933", null);
        } else {
            logManager.log("Failed to start scan.");
        }

    }

    @Override
    public void onImuConnectionChanged(String deviceName, boolean connected) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                uiManager.setTextView(uiManager.thighScanStatus, connected ? "Connected" : "Disconnected", null, null);
            } else if (deviceName.equals("Foot IMU")) {
                uiManager.setTextView(uiManager.footScanStatus, connected ? "Connected" : "Disconnected", null, null);
            }

            if (!connected && !isSyncing) {
                // Only reset buttons on DISCONNECT
                uiManager.setButton(uiManager.scanButton, "Scan", null, null);
                uiManager.setButton(uiManager.measureButton, "Measure", null, null);
                uiManager.setButton(uiManager.syncButton, "Start Sync", null, null);
                uiManager.setButton(uiManager.disconnectButton, "Disconnect", null, null);


                uiManager.setButton(uiManager.scanButton, null, "#4CAF50", null);
                uiManager.setButton(uiManager.syncButton, null, "#4CAF50", null);
                uiManager.setButton(uiManager.disconnectButton, null, "#FD8888", null);
            }
        });
    }

    @Override
    public void onImuScanned(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                uiManager.setTextView(uiManager.thighScanStatus, "Scanned", null, null);
            } else if (deviceName.equals("Foot IMU")) {
                uiManager.setTextView(uiManager.footScanStatus, "Scanned", null, null);
            }
        });
    }

    @Override
    public void onImuReady(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("Thigh IMU")) {
                uiManager.setTextView(uiManager.thighScanStatus, "Ready", null, null);
            } else if (deviceName.equals("Foot IMU")) {
                uiManager.setTextView(uiManager.footScanStatus, "Ready", null, null);
            }
            if (thigh.isReady && foot.isReady) {
                uiManager.setButton(uiManager.syncButton, null, null, true);
                uiManager.setButton(uiManager.scanButton, "Scanned", "#008080", true);
            }
        });
    }


    public void syncButton_onClick(View view) {
        isSyncing = true;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {

                uiManager.setButton(uiManager.syncButton, "Syncing...", "#FF9933", null);
                uiManager.setTextView(uiManager.thighScanStatus, "Syncing", null, null);
                uiManager.setTextView(uiManager.footScanStatus, "Syncing", null, null);

            }
        });

        imuManager.startSync();

    }

    @Override
    public void onSyncingDone() {
        isSyncing = false;

        uiManager.setButton(uiManager.measureButton, null, null, true);
        uiManager.setButton(uiManager.disconnectButton, null, null, true);
        runOnUiThread(() -> {

            uiManager.setButton(uiManager.syncButton, "Sync Done", "#008080", null);

            uiManager.setButton(uiManager.measureButton, null, null, true);
            uiManager.setButton(uiManager.disconnectButton, null, null, true);
            logManager.log("(Main): --- Syncing is done! ---- ");
        });
    }

    public void measureButton_onClick(View view) {


        uiManager.setButton(uiManager.stopButton, null, null, true);
        uiManager.setButton(uiManager.dataLogButton, null, null, true);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                //measureButton.setText("Measuring...");
                uiManager.setButton(uiManager.measureButton, "Measuring...", null, null);
            }
        });

        if (thigh.xsDevice != null)
            thigh.normalDataLogger = logManager.createDataLog(thigh.xsDevice, subjectTitle, subjectNumber, imuManager);
        if (foot.xsDevice != null)
            foot.normalDataLogger = logManager.createDataLog(foot.xsDevice, subjectTitle, subjectNumber, imuManager);
        imuManager.startMeasurement();
    }
/////////////////// Callbacks ////////////////////////


    @Override
    public void onDataUpdated(String deviceAddress, double[] eulerAngles) {



        runOnUiThread(() -> {
            if (deviceAddress.equals(thigh.MAC)) {
                uiManager.setTextView(uiManager.ValueT1, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.ValueT2, String.format(Locale.US, "%.1f deg", eulerAngles[1]), null, null);
                uiManager.setTextView(uiManager.ValueT3, String.valueOf(thigh.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.ValueT4, thigh.xsDevice.getBatteryPercentage() + "%", null, null);

            } else if (deviceAddress.equals(foot.MAC)) {

                uiManager.setTextView(uiManager.ValueF1, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.ValueF2, String.format(Locale.US, "%.1f deg", eulerAngles[1]), null, null);
                uiManager.setTextView(uiManager.ValueF3, String.valueOf(foot.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.ValueF4, foot.xsDevice.getBatteryPercentage() + "%", null, null);
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



    /*
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
    */

    /*
    ///////////////////////////////////////////////////////         Buttons      //////////////////////
     */
    public void disconnectButton_onClick(View view) {
        // measureButton.setEnabled(false);
        uiManager.setButton(uiManager.measureButton, null, null, false);
        // dataLogButton.setEnabled(false);
        uiManager.setButton(uiManager.dataLogButton, null, null, false);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                // disconnectButton.setText("Disconnecting...");
                // disconnectButton.setBackgroundColor(Color.parseColor("#F60000"));
                uiManager.setButton(uiManager.disconnectButton, "Disconnecting...", "#F60000", false);
            }
        });
        imuManager.disconnectAll();
    }
    public void stopButton_onClick(View view) { // After measuring, the dots should be stopped to for data logging

        uiManager.setButton(uiManager.stopButton, null, null, false);
        uiManager.setButton(uiManager.dataLogButton, null, null, false);
        uiManager.setButton(uiManager.measureButton, "Measuring Stopped", null, false);
        uiManager.setButton(uiManager.uploadButton, null, null, true);

        logManager.log("Stopping");
        imuManager.stopMeasurement();
        //measureButton.setText("Measuring Stopped");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // measureButton.setText("Measuring Stopped");
                uiManager.setButton(uiManager.measureButton, "Measuring Stopped", null, null);
            }
        });

    }

    public void uploadButton_onClick(View view) {
        for (int i = 0; i < logManager.loggerFileNames.size(); i++) {
            logManager.log("Uploading data to cloud : " + logManager.loggerFileNames.get(i));
            uploadLogFileToCloud(Uri.fromFile(logManager.loggerFilePaths.get(i)), logManager.loggerFileNames.get(i));
        }

        uploadLogFileToCloud(Uri.fromFile(logFile), "log");
    }
    /*
    public void uploadButton_onClick(View view) {
        for (int i = 0; i < loggerFileNames.size(); i++) {
            logManager.log("Uploading data to cloud : " + loggerFileNames.get(i));
            uploadLogFileToCloud(Uri.fromFile(oggerFilePaths.get(i)), loggerFileNames.get(i));
        }

        uploadLogFileToCloud(Uri.fromFile(logFile), "log");
    }
     */
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
                        //uploadButton.setText("Uploading Failed");
                        //uploadButton.setBackgroundColor(Color.parseColor("#f63e00"));
                        uiManager.setButton(uiManager.uploadButton, "Uploading Failed", "#f63e00", null);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        runOnUiThread(new Runnable() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void run() {
                                // uploadButton.setText("Uploading Done");
                                // uploadButton.setBackgroundColor(Color.parseColor("#0af056"));
                                uiManager.setButton(uiManager.uploadButton, "Uploading Done", "#0af056", null);
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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissionManager != null) {
            permissionManager.handlePermissionResult(requestCode, grantResults);
        }
    }



}