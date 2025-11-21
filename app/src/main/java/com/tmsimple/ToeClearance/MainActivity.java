package com.tmsimple.ToeClearance;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.text.method.ScrollingMovementMethod;
import java.io.File;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements ImuManagerListener {


    private Segment IMU1, IMU2;
    public String IMU1MAC = "D4:22:CD:00:63:D6"; //V2-16
    public String IMU2MAC = "D4:22:CD:00:63:8B"; //V2-17


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
    private boolean isScanning = false;

    StorageReference storageReference;

    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        storageReference = FirebaseStorage.getInstance().getReference();
        logManager = new LogManager(this, null, null);
        permissionManager = new PermissionManager(this, logManager);
        permissionManager.requestAllPermissions();

        // Initialize main page directly
        initializeMainPage();

    }

    private  void initializeMainPage(){

        logFilePath = this.getApplicationContext().getExternalFilesDir("logs");


        imuManager = new ImuManager(this, this, logManager);
        logManager.setImuManager(imuManager);

        // Set the root
        View root = findViewById(R.id.labeling_data_root);
        uiManager = new UiManager(root, imuManager);
        uiManager.bindLabelingDataViews(getWindow().getDecorView().getRootView());
        uiManager.setupImuSpinners(this);

        imuManager.setUiManager(uiManager);

        // Before scanning all should be deactive; after each step they will be enabled

        uiManager.setButton(uiManager.scanButton,null, null, false);
        uiManager.setButton(uiManager.syncButton, null, null, false);
        uiManager.setButton(uiManager.measureButton, null, null, false);
        uiManager.setButton(uiManager.stopButton, null, null, false);
        uiManager.setButton(uiManager.disconnectButton, null, null, false);
        uiManager.setButton(uiManager.uploadButton, null, null, false);
        uiManager.setButton(uiManager.dataLogButton, null, null, false);


        uiManager.setEnterSubjectNumberHandler(uiManager.enterSubjectNumber, new UiManager.OnSubjectNumberEnteredListener() {
            @Override
            public void onSubjectNumberEntered(int subjcetNu) {

                subjectTitle = "Subject " + subjcetNu;
                subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                logFileName = "Logger " + subjectTitle + " " + subjectDateAndTime + ".txt";
                logFile = new File(logFilePath, logFileName);
                logManager.setLogFile(logFile, subjcetNu);
                subjectNumber = subjcetNu;
            }
        });

        // DataLogger Button
        uiManager.setDataLogButtonHandler(uiManager.dataLogButton, logManager, imuManager);


        //uiManager.bindLabelButtons();
        uiManager.setupLabelDialog(this);
        uiManager.setupLogDialog(this, logManager);
        uiManager.setupFeatureDialog(this);
        uiManager.setupImuDataDialog(this);

        // After uiManager.bindLabelingDataViews() call, add:
        if (uiManager.imu1Gyro == null) logManager.log("ERROR: imu1Gyro not bound!");
        if (uiManager.imu1Accel == null) logManager.log("ERROR: imu1Accel not bound!");
        if (uiManager.imu2Gyro == null) logManager.log("ERROR: imu2Gyro not bound!");
        if (uiManager.imu2Accel == null) logManager.log("ERROR: imu2Accel not bound!");


    }

//*//*////*//*////*//*////*//*////*//*////*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*//


    /// ///////////////////////////////////  Sequence of syncing  ///////////////////////////////////////////////////////////////////

    public void scanButton_onClick(View view) {

        // Get selected MAC addresses
        IMU1MAC = uiManager.getSelectedIMU1Mac();
        IMU2MAC = uiManager.getSelectedIMU2Mac();

        // Get selected names
        String imu1Name = uiManager.getSelectedIMU1Name();
        String imu2Name = uiManager.getSelectedIMU2Name();

        logManager.log("IMU1: Name = " + imu1Name + ",MAC: " + IMU1MAC);
        logManager.log("IMU2: Name = " + imu2Name + ",MAC: " + IMU2MAC);

        // Check if the same IMU is selected for both
        if (IMU1MAC.equals(IMU2MAC)) {
            // Show error popup
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Selection Error")
                    .setMessage("Please choose different IMUs for IMU1 and IMU2!")
                    .setPositiveButton("OK", null)
                    .show();

            // Vibrate phone
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(500); // Vibrate for 500ms
            }

            logManager.log("Error: IMU1 and IMU2 cannot be the same!");
            return;
        }

        // Configure IMU segments
        IMU1 = new Segment("IMU1 IMU", IMU1MAC);
        IMU2 = new Segment("IMU2 IMU", IMU2MAC);
        imuManager.setSegments(IMU1, IMU2);

        if (imuManager.startScan()) {
            isScanning = true;
            logManager.log("Scan started!");

            uiManager.setButton(uiManager.scanButton, "Scanning ...", "#FF9933", null);
        } else {
            logManager.log("Failed to start scan.");
        }

    }
    // Added newly for discovery


    public void listImusButton_onClick(View view) {
        // Initialize segments to prevent null pointer exception

        if (IMU1 == null || IMU2 == null) {
            IMU1 = new Segment("IMU1 IMU", IMU1MAC);
            IMU2 = new Segment("IMU2 IMU", IMU2MAC);
            imuManager.setSegments(IMU1, IMU2);
        }

        // Start discovery scan
        if (imuManager.startDiscoveryScan()) {
            logManager.log("Discovery scan started - searching for 10 seconds...");

            // Update button to show scanning status
            runOnUiThread(() -> {
                Button listButton = findViewById(R.id.listImusButton);
                if (listButton != null) {
                    listButton.setText("Scanning...");
                    listButton.setBackgroundColor(Color.parseColor("#FF9933"));
                    listButton.setEnabled(false);
                }
            });

            // Reset button after 10 seconds
            new android.os.Handler().postDelayed(() -> {
                runOnUiThread(() -> {
                    Button listButton = findViewById(R.id.listImusButton);
                    if (listButton != null) {
                        listButton.setText("Available IMUs");
                        listButton.setBackgroundColor(Color.parseColor("#2196F3"));
                        listButton.setEnabled(true);
                    }
                });
            }, 10000);
        } else {
            logManager.log("Failed to start discovery scan.");
        }
    }

    // ---------------
    @Override
    public void onImuConnectionChanged(String deviceName, boolean connected) {
        runOnUiThread(() -> {
            String statusText = connected ? "Connected" : "Disconnected";

            if (deviceName.equals("IMU1 IMU")) {
                // Update main page
                uiManager.setTextView(uiManager.imu1Status, statusText, null, null);

                // Update dialog
                if (uiManager.dialogImu1Status != null) {
                    uiManager.dialogImu1Status.setText(statusText);
                }

            } else if (deviceName.equals("IMU2 IMU")) {
                // Update main page
                uiManager.setTextView(uiManager.imu2Status, statusText, null, null);

                // Update dialog
                if (uiManager.dialogImu2Status != null) {
                    uiManager.dialogImu2Status.setText(statusText);
                }
            }

            if (!connected && !isSyncing) {
                // Only reset buttons on DISCONNECT
                uiManager.setButton(uiManager.scanButton, "Scan", null, null);
                uiManager.setButton(uiManager.measureButton, "Measure", null, null);
                uiManager.setButton(uiManager.syncButton, "Start Sync", null, null);
                uiManager.setButton(uiManager.disconnectButton, "Disconnect", null, null);

                uiManager.setButton(uiManager.scanButton, null, "#2196F3", null);
                uiManager.setButton(uiManager.syncButton, null, "#2196F3", null);
                uiManager.setButton(uiManager.disconnectButton, null, "#AB2727", null);
            }
            updateAppBorderColor();
        });
    }

    @Override
    public void onImuScanned(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("IMU1 IMU")) {
                uiManager.setTextView(uiManager.imu1Status, "Scanned", null, null);
                // ADD THIS:
                if (uiManager.dialogImu1Status != null) {
                    uiManager.dialogImu1Status.setText("Scanned");
                }
            } else if (deviceName.equals("IMU2 IMU")) {
                uiManager.setTextView(uiManager.imu2Status, "Scanned", null, null);
                // ADD THIS:
                if (uiManager.dialogImu2Status != null) {
                    uiManager.dialogImu2Status.setText("Scanned");
                }
            }
            if (isScanning) {
                uiManager.setButton(uiManager.scanButton, "Scanning...", "#FF9933", null);
            }
            // UPDATE APP BORDER COLOR
            updateAppBorderColor();

        });
    }

    @Override
    public void onImuReady(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("IMU1 IMU")) {
                uiManager.setTextView(uiManager.imu1Status, "Ready", null, null);
                // ADD THIS:
                if (uiManager.dialogImu1Status != null) {
                    uiManager.dialogImu1Status.setText("Ready");
                }
            } else if (deviceName.equals("IMU2 IMU")) {
                uiManager.setTextView(uiManager.imu2Status, "Ready", null, null);
                // ADD THIS:
                if (uiManager.dialogImu2Status != null) {
                    uiManager.dialogImu2Status.setText("Ready");
                }
            }
            if (IMU1.isReady && IMU2.isReady) {
                uiManager.setButton(uiManager.syncButton, null, null, true);
                uiManager.setButton(uiManager.scanButton, "Scanned", "#008080", true);
            }
            // UPDATE APP BORDER COLOR
            updateAppBorderColor();
        });
    }


    public void syncButton_onClick(View view) {
        isSyncing = true;
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                uiManager.setButton(uiManager.syncButton, "Syncing...", "#FF9933", null);
                uiManager.setTextView(uiManager.imu1Status, "Syncing", null, null);
                uiManager.setTextView(uiManager.imu2Status, "Syncing", null, null);

                // ADD THIS:
                if (uiManager.dialogImu1Status != null) {
                    uiManager.dialogImu1Status.setText("Syncing");
                }
                if (uiManager.dialogImu2Status != null) {
                    uiManager.dialogImu2Status.setText("Syncing");
                }
                // UPDATE APP BORDER COLOR
                updateAppBorderColor();
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
            uiManager.setButton(uiManager.syncButton, "Synced", "#008080", null);
            uiManager.setButton(uiManager.measureButton, null, null, true);
            uiManager.setButton(uiManager.disconnectButton, null, null, true);

            // ADD THIS:
            uiManager.setTextView(uiManager.imu1Status, "Synced", null, null);
            uiManager.setTextView(uiManager.imu2Status, "Synced", null, null);
            if (uiManager.dialogImu1Status != null) {
                uiManager.dialogImu1Status.setText("Synced");
            }
            if (uiManager.dialogImu2Status != null) {
                uiManager.dialogImu2Status.setText("Synced");
            }

            logManager.log("(Main): --- Syncing is done! ---- ");
            // UPDATE APP BORDER COLOR
            updateAppBorderColor();
        });
    }

    public void measureButton_onClick(View view) {


        uiManager.setButton(uiManager.stopButton, null, null, true);
        uiManager.setButton(uiManager.dataLogButton, null, null, true);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                uiManager.setButton(uiManager.measureButton, "Measuring", null, null);
                // UPDATE APP BORDER COLOR to indicate measuring
                uiManager.setAppBorderColor("#2196F3"); // Light Blue for measuring
            }
        });

        if (IMU1.xsDevice != null)
            IMU1.normalDataLogger = logManager.createDataLog("IMU1", IMU1.xsDevice, subjectTitle, subjectNumber, imuManager);
        if (IMU2.xsDevice != null)
            IMU2.normalDataLogger = logManager.createDataLog("IMU2", IMU2.xsDevice, subjectTitle, subjectNumber, imuManager);

        logManager.initializeFeatureLogs(subjectTitle, subjectNumber);

        imuManager.startMeasurement();
    }
/////////////////// Callbacks ////////////////////////


    @Override
    public void onDataUpdated(String deviceAddress, double[] eulerAngles) {

        runOnUiThread(() -> {
            if (deviceAddress.equals(IMU1.MAC)) {
                // Update main page
                uiManager.setTextView(uiManager.imu1Roll, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.imu1Index, String.valueOf(IMU1.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.imu1Battery, IMU1.xsDevice.getBatteryPercentage() + "%", null, null);

                // Update dialog
                if (uiManager.dialogImu1Roll != null) {
                    uiManager.dialogImu1Roll.setText(String.format(Locale.US, "%.1f deg", eulerAngles[0]));
                }
                if (uiManager.dialogImu1Index != null) {
                    uiManager.dialogImu1Index.setText(String.valueOf(IMU1.dataOutput[3]));
                }
                if (uiManager.dialogImu1Battery != null) {
                    uiManager.dialogImu1Battery.setText(IMU1.xsDevice.getBatteryPercentage() + "%");
                }

            } else if (deviceAddress.equals(IMU2.MAC)) {
                // Update main page
                uiManager.setTextView(uiManager.imu2Roll, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.imu2Index, String.valueOf(IMU2.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.imu2Battery, IMU2.xsDevice.getBatteryPercentage() + "%", null, null);

                // Update dialog
                if (uiManager.dialogImu2Roll != null) {
                    uiManager.dialogImu2Roll.setText(String.format(Locale.US, "%.1f deg", eulerAngles[0]));
                }
                if (uiManager.dialogImu2Index != null) {
                    uiManager.dialogImu2Index.setText(String.valueOf(IMU2.dataOutput[3]));
                }
                if (uiManager.dialogImu2Battery != null) {
                    uiManager.dialogImu2Battery.setText(IMU2.xsDevice.getBatteryPercentage() + "%");
                }
            }
        });
    }
    @Override
    public void onZuptDataUpdated(String deviceAddress, double gyroMag, double linearAccelMag) {
        runOnUiThread(() -> {

            if (deviceAddress.equals("IMU1")) {
                // Update main page
                uiManager.setTextView(uiManager.imu1Gyro, String.format(Locale.US,"%.2f", gyroMag), null, null);
                uiManager.setTextView(uiManager.imu1Accel, String.format(Locale.US,"%.2f", linearAccelMag), null, null);

                // Update dialog
                if (uiManager.dialogImu1Gyro != null) {
                    uiManager.dialogImu1Gyro.setText(String.format(Locale.US,"%.2f", gyroMag));
                }
                if (uiManager.dialogImu1Accel != null) {
                    uiManager.dialogImu1Accel.setText(String.format(Locale.US,"%.2f", linearAccelMag));
                }

            } else if (deviceAddress.equals("IMU2")) {
                // Update main page
                uiManager.setTextView(uiManager.imu2Gyro, String.format(Locale.US,"%.2f", gyroMag), null, null);
                uiManager.setTextView(uiManager.imu2Accel, String.format(Locale.US,"%.2f", linearAccelMag), null, null);

                // Update dialog
                if (uiManager.dialogImu2Gyro != null) {
                    uiManager.dialogImu2Gyro.setText(String.format(Locale.US,"%.2f", gyroMag));
                }
                if (uiManager.dialogImu2Accel != null) {
                    uiManager.dialogImu2Accel.setText(String.format(Locale.US,"%.2f", linearAccelMag));
                }
            }
        });
    }
    @Override
    public void onFeatureDetectionUpdate(int windowNum, String terrainType, double biasValue,
                                         double maxHeight, double maxStride) {
        runOnUiThread(() -> {
            uiManager.updateFeatureDisplay(windowNum, terrainType, biasValue,
                    maxHeight, maxStride);
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

        logManager.closeFeatureLogs();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                uiManager.setButton(uiManager.measureButton, "Measuring Stopped", null, null);
                // UPDATE APP BORDER COLOR to indicate stopped
                uiManager.setAppBorderColor("#AB2727"); // Red for stopped
            }
        });

    }

    public void uploadButton_onClick(View view) {
        for (int i = 0; i < logManager.loggerFileNames.size(); i++) {
            logManager.log("Uploading data to cloud : " + logManager.loggerFileNames.get(i));
            uploadLogFileToCloud(Uri.fromFile(logManager.loggerFilePaths.get(i)), logManager.loggerFileNames.get(i));
        }

        uploadLogFileToCloud(Uri.fromFile(logFile), logFileName);
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
                                uiManager.setButton(uiManager.uploadButton, "Uploaded", "#008080", null);
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

    private void updateAppBorderColor() {
        String imu1StatusText = uiManager.imu1Status.getText().toString();
        String imu2StatusText = uiManager.imu2Status.getText().toString();

        // Determine border color based on overall status
        String borderColor;

        if (imu1StatusText.equals("Synced") && imu2StatusText.equals("Synced")) {
            borderColor = "#052A64";
        } else if (imu1StatusText.equals("Ready") && imu2StatusText.equals("Ready")) {
            borderColor = "#052A64";
        } else if (imu1StatusText.equals("Syncing") || imu2StatusText.equals("Syncing")) {
            borderColor = "#FF9933"; // Amber - Currently syncing
        } else if ((imu1StatusText.equals("Connected") || imu1StatusText.equals("Scanned")) &&
                (imu2StatusText.equals("Connected") || imu2StatusText.equals("Scanned"))) {
            borderColor = "#052A64 ";
        } else if (imu1StatusText.equals("Disconnected") || imu2StatusText.equals("Disconnected") ||
                imu1StatusText.equals("-") || imu2StatusText.equals("-")) {
            borderColor = "#AB2727"; // Red - Disconnected or not started
        } else {
            borderColor = "#9E9E9E"; // Gray - Unknown/Initial state
        }

        // Update the border color
        uiManager.setAppBorderColor(borderColor);
    }

}