package com.tmsimple.ToeClearance;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
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


    TextView logContents;
    StorageReference storageReference;

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


        Button labelButton1 = findViewById(R.id.labelButton1);


        logFilePath = this.getApplicationContext().getExternalFilesDir("logs");


        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);

        imuManager = new ImuManager(this, this, logManager);
        logManager.setImuManager(imuManager);

        // Set the root
        View root = findViewById(R.id.labeling_data_root);
        uiManager = new UiManager(root, imuManager);
        uiManager.bindLabelingDataViews(getWindow().getDecorView().getRootView());
        //uiManager.bindLabelingDataViews(findViewById(R.id.labeling_data_root));


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
                IMU1MAC = "D4:22:CD:00:A1:76";
                IMU2MAC = "D4:22:CD:00:9F:95";
                logManager.log(IMU1MAC);
                logManager.log(IMU2MAC);
            }
            @Override
            public void onRightSideSelected() {
                IMU1MAC = "D4:22:CD:00:63:8B";
                IMU2MAC = "D4:22:CD:00:63:A4";
                logManager.log(IMU1MAC);
                logManager.log(IMU2MAC);
            }
        });

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

        // Go back to first page Button
        uiManager.setHomeButtonHandler(uiManager.homeButton, () -> {setContentView(R.layout.first_page);});
        uiManager.bindLabelButtons();

        // After uiManager.bindLabelingDataViews() call, add:
        if (uiManager.imu1Gyro == null) logManager.log("ERROR: imu1Gyro not bound!");
        if (uiManager.imu1Accel == null) logManager.log("ERROR: imu1Accel not bound!");
        if (uiManager.imu2Gyro == null) logManager.log("ERROR: imu2Gyro not bound!");
        if (uiManager.imu2Accel == null) logManager.log("ERROR: imu2Accel not bound!");


    }

//*//*////*//*////*//*////*//*////*//*////*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*//


    /// ///////////////////////////////////  Sequence of syncing  ///////////////////////////////////////////////////////////////////

    public void scanButton_onClick(View view) {

        // Configure IMU segments
        IMU1 = new Segment("IMU1 IMU", IMU1MAC);
        IMU2 = new Segment("IMU2 IMU", IMU2MAC);
        imuManager.setSegments(IMU1, IMU2);

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
            if (deviceName.equals("IMU1 IMU")) {
                uiManager.setTextView(uiManager.imu1Status, connected ? "Connected" : "Disconnected", null, null);
            } else if (deviceName.equals("IMU2 IMU")) {
                uiManager.setTextView(uiManager.imu2Status, connected ? "Connected" : "Disconnected", null, null);
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
            if (deviceName.equals("IMU1 IMU")) {
                uiManager.setTextView(uiManager.imu1Status, "Scanned", null, null);
            } else if (deviceName.equals("IMU2 IMU")) {
                uiManager.setTextView(uiManager.imu2Status, "Scanned", null, null);
            }
        });
    }

    @Override
    public void onImuReady(String deviceName) {
        runOnUiThread(() -> {
            if (deviceName.equals("IMU1 IMU")) {
                uiManager.setTextView(uiManager.imu1Status, "Ready", null, null);
            } else if (deviceName.equals("IMU2 IMU")) {
                uiManager.setTextView(uiManager.imu2Status, "Ready", null, null);
            }
            if (IMU1.isReady && IMU2.isReady) {
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
                uiManager.setTextView(uiManager.imu1Status, "Syncing", null, null);
                uiManager.setTextView(uiManager.imu2Status, "Syncing", null, null);

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

        if (IMU1.xsDevice != null)
            IMU1.normalDataLogger = logManager.createDataLog(IMU1.xsDevice, subjectTitle, subjectNumber, imuManager);
        if (IMU2.xsDevice != null)
            IMU2.normalDataLogger = logManager.createDataLog(IMU2.xsDevice, subjectTitle, subjectNumber, imuManager);
        imuManager.startMeasurement();
    }
/////////////////// Callbacks ////////////////////////


    @Override
    public void onDataUpdated(String deviceAddress, double[] eulerAngles) {

        runOnUiThread(() -> {
            if (deviceAddress.equals(IMU1.MAC)) {
                uiManager.setTextView(uiManager.imu1Roll, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.imu1Index, String.valueOf(IMU1.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.imu1Battery, IMU1.xsDevice.getBatteryPercentage() + "%", null, null);
            } else if (deviceAddress.equals(IMU2.MAC)) {
                uiManager.setTextView(uiManager.imu2Roll, String.format(Locale.US, "%.1f deg", eulerAngles[0]), null, null);
                uiManager.setTextView(uiManager.imu2Index, String.valueOf(IMU2.dataOutput[3]), null, null);
                uiManager.setTextView(uiManager.imu2Battery, IMU2.xsDevice.getBatteryPercentage() + "%", null, null);
            }
        });
    }
    @Override
    public void onZuptDataUpdated(String deviceAddress, double gyroMag, double linearAccelMag) {
        runOnUiThread(() -> {

            if (deviceAddress.equals("IMU1")) {  // Changed from IMU1.MAC
                uiManager.setTextView(uiManager.imu1Gyro, String.format(Locale.US,"%.2f", gyroMag), null, null);
                uiManager.setTextView(uiManager.imu1Accel, String.format(Locale.US,"%.2f", linearAccelMag), null, null);

            } else if (deviceAddress.equals("IMU2")) {  // Changed from IMU2.MAC
                uiManager.setTextView(uiManager.imu2Gyro, String.format(Locale.US,"%.2f", gyroMag), null, null);
                uiManager.setTextView(uiManager.imu2Accel, String.format(Locale.US,"%.2f", linearAccelMag), null, null);
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