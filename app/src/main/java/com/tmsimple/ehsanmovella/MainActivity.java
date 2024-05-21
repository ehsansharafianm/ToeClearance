package com.tmsimple.ehsanmovella;

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
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
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
import com.xsens.dot.android.sdk.utils.DotParser;
import com.xsens.dot.android.sdk.utils.DotScanner;
import android.text.method.ScrollingMovementMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import android.widget.Switch;

import org.tensorflow.lite.Interpreter;

public class MainActivity extends AppCompatActivity implements DotDeviceCallback, DotScannerCallback, DotRecordingCallback, DotSyncCallback, DotMeasurementCallback {

    public String Version = "v1.2";
    public static int UNRECHABLE_VALUE = 9999;
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
    public boolean isLoggingData = false;
    public int dataLogButtonIndex = 0;
    Button scanButton, syncButton, measureButton, disconnectButton, stopButton, uploadButton, dataLogButton,
           activity1Button, activity2Button, activity3Button, activity4Button, activity5Button, homeButton, recognitionButton;
    Switch logSwitch;
    private ArrayList<DotDevice> mDeviceLst;
    TextView leftThighScanStatus, leftFootScanStatus, logContents;
    TextView ValueF1, ValueF2, ValueF3, ValueF4, ValueF5, ValueF6, ValueT1, ValueT2, ValueT3, ValueT4, ValueT5, ValueT6, valueResult;
    EditText enterSubjectNumber;
    DecimalFormat threePlaces = new DecimalFormat("##.#");
    public int packetCounterCofficient = 0;
    public boolean startRecognition = false;
    private Interpreter tflite;

    public int MeasurementMode;
    StorageReference storageReference;
    //DatabaseReference databaseReference;
    public int SAMPLE_RATE = 60;
    public int windowSize = 3 * SAMPLE_RATE;

    private static final int BLUETOOTH_PERMISSION_CODE = 100; //Bluetooth Permission variable
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 101; //Bluetooth Permission variable

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);


        storageReference = FirebaseStorage.getInstance().getReference();

        leftThigh = new Segment("Left Thigh IMU", leftThighMAC);
        leftFoot = new Segment("Left Foot IMU", leftFootMAC);

        // Initialize TensorFlow Lite interpreter
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void LabelingData(View view){
        setContentView((R.layout.labeling_data));

        scanButton = findViewById(R.id.scanButton);
        syncButton = findViewById(R.id.syncButton);
        measureButton = findViewById(R.id.measureButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        stopButton = findViewById(R.id.stopButton);
        uploadButton = findViewById(R.id.uploadButton);
        dataLogButton = findViewById(R.id.dataLogButton);
        enterSubjectNumber = findViewById(R.id.enterSubjectNumber);
        activity1Button = findViewById(R.id.activity1Button);
        activity2Button = findViewById(R.id.activity2Button);
        activity3Button = findViewById(R.id.activity3Button);
        activity4Button = findViewById(R.id.activity4Button);
        activity5Button = findViewById(R.id.activity5Button);
        homeButton = findViewById(R.id.homeButton);

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
        dataLogButton.setEnabled(false);



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
            public void afterTextChanged(Editable string) {
                if(!string.toString().isEmpty()){
                    try {
                        subjectNumber = Integer.parseInt(enterSubjectNumber.getText().toString());
                        subjectTitle = "Subject " + subjectNumber;
                        subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                        logFileName = subjectTitle + " " + subjectDateAndTime + ".txt";
                        logFile = new File(logFilePath,logFileName);
                        writeToLogs("Subject number set: " + subjectNumber);
                        writeToLogs("Log File Created");
                        scanButton.setEnabled(true);
                        //enterSubjectNumber.setEnabled(false);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        writeToLogs("Subject Number is invalid");
                    }
                }
            }
        });

        dataLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataLogButtonIndex++;
                if (dataLogButtonIndex%2 == 1){
                    isLoggingData = true;
                    dataLogButton.setBackgroundColor(Color.parseColor("#05edbb"));
                    dataLogButton.setText("Data Logging ...");
                    writeToLogs(" ---- Data is Logging -----");
                }
                else if (dataLogButtonIndex%2 == 0 && dataLogButtonIndex > 1) {
                    isLoggingData = false;
                    dataLogButton.setBackgroundColor(Color.parseColor("#4DBDDF"));
                    dataLogButton.setText("Data Logging Stopped");
                    writeToLogs("---- Data Logging Stopped -----");
                }
            }
        });
        activity1Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 1000000;
                        activity1Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity1Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        activity2Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 2000000;
                        activity2Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity2Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        activity3Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 3000000;
                        activity3Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity3Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        activity4Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 4000000;
                        activity4Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity4Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        activity5Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 5000000;
                        activity5Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity5Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.first_page);
            }
        });
    }
    public void activityRecognition(View view){
        setContentView(R.layout.recognition_page);

        scanButton = findViewById(R.id.scanButton);
        syncButton = findViewById(R.id.syncButton);
        measureButton = findViewById(R.id.measureButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        stopButton = findViewById(R.id.stopButton);
        uploadButton = findViewById(R.id.uploadButton);
        dataLogButton = findViewById(R.id.dataLogButton);
        enterSubjectNumber = findViewById(R.id.enterSubjectNumber);
        activity1Button = findViewById(R.id.activity1Button);
        homeButton = findViewById(R.id.homeButton);
        recognitionButton = findViewById(R.id.recognitionButton);

        leftThighScanStatus = findViewById(R.id.leftThighStatusView);
        leftFootScanStatus = findViewById(R.id.leftFootStatusView);

        logFilePath = this.getApplicationContext().getExternalFilesDir("logs");

        ValueF1 = findViewById(R.id.valueF1);
        ValueF2 = findViewById(R.id.valueF2);
        ValueF3 = findViewById(R.id.valueF3);
        ValueF4 = findViewById(R.id.valueF4);
        ValueF5 = findViewById(R.id.valueF5);
        ValueF6 = findViewById(R.id.valueF6);
        ValueT1 = findViewById(R.id.valueT1);
        ValueT2 = findViewById(R.id.valueT2);
        ValueT3 = findViewById(R.id.valueT3);
        ValueT4 = findViewById(R.id.valueT4);
        ValueT5 = findViewById(R.id.valueT5);
        ValueT6 = findViewById(R.id.valueT6);
        valueResult = findViewById(R.id.valueResult);

        logSwitch = findViewById(R.id.logSwitch);

        logContents = findViewById(R.id.logContents);
        logContents.setMovementMethod(new ScrollingMovementMethod());
        logContents.setVisibility(View.INVISIBLE);

        // Before scanning all should be deactivate; after each step they will be enabled
        scanButton.setEnabled(false);
        syncButton.setEnabled(false);
        measureButton.setEnabled(false);
        stopButton.setEnabled(false);
        disconnectButton.setEnabled(false);
        uploadButton.setEnabled(false);
        dataLogButton.setEnabled(false);

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
            public void afterTextChanged(Editable string) {
                if(!string.toString().isEmpty()){
                    try {
                        subjectNumber = Integer.parseInt(enterSubjectNumber.getText().toString());
                        subjectTitle = "Subject " + subjectNumber;
                        subjectDateAndTime = java.text.DateFormat.getDateTimeInstance().format(new Date());
                        logFileName = subjectTitle + " " + subjectDateAndTime + ".txt";
                        logFile = new File(logFilePath,logFileName);
                        writeToLogs("Subject number set: " + subjectNumber);
                        writeToLogs("Log File Created");
                        scanButton.setEnabled(true);
                        //enterSubjectNumber.setEnabled(false);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        writeToLogs("Subject Number is invalid");
                    }
                }
            }
        });

        dataLogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataLogButtonIndex++;
                if (dataLogButtonIndex%2 == 1){
                    isLoggingData = true;
                    dataLogButton.setBackgroundColor(Color.parseColor("#05edbb"));
                    dataLogButton.setText("Data Logging ...");
                    writeToLogs(" ---- Data is Logging -----");
                }
                else if (dataLogButtonIndex%2 == 0 && dataLogButtonIndex > 1) {
                    isLoggingData = false;
                    dataLogButton.setBackgroundColor(Color.parseColor("#4DBDDF"));
                    dataLogButton.setText("Data Logging Stopped");
                    writeToLogs("---- Data Logging Stopped -----");
                }
            }
        });
        activity1Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 1000000;
                        activity1Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity1Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.first_page);
            }
        });
        recognitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecognition = true;
                recognitionButton.setText("Recogniton...");
                recognitionButton.setBackgroundColor(Color.parseColor("#4DDFB8"));
            }
        });
    }
//*//*////*//*////*//*////*//*////*//*//    //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*//


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
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onDotDataChanged(String address, DotData dotData) {

        double[] eulerAngles = dotData.getEuler();
        //double[] eulerAngles_Q = DotParser.quaternion2Euler(dotData.getQuat());
        if (address.equals(leftThigh.MAC)) {
            leftThigh.dataOutput[0] = threePlaces.format(eulerAngles[0] - leftThigh.initAngleValue);
            leftThigh.dataOutput[1] = threePlaces.format(eulerAngles[1]);
            leftThigh.dataOutput[2] = threePlaces.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {

                    ValueT1.setText(leftThigh.dataOutput[0] + "   deg");
                    ValueT2.setText(leftThigh.dataOutput[1] + "   deg");
                    ValueT3.setText(leftThigh.dataOutput[2] + "   deg");
                    ValueT4.setText(String.valueOf(leftThigh.dataOutput[3]));
                }
            });
        } else if (address.equals(leftFoot.MAC)) {
            leftFoot.dataOutput[0] = threePlaces.format(eulerAngles[0]- leftFoot.initAngleValue);
            leftFoot.dataOutput[1] = threePlaces.format(eulerAngles[1]);
            leftFoot.dataOutput[2] = threePlaces.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF1.setText(leftFoot.dataOutput[0] + "   deg");
                    ValueF2.setText(leftFoot.dataOutput[1] + "   deg");
                    ValueF3.setText(leftFoot.dataOutput[2] + "   deg");
                    ValueF4.setText(String.valueOf(leftFoot.dataOutput[3]));
                    //ValueF5.setText(threePlaces.format(leftFoot.minEulerAngle));
                    //ValueF6.setText(threePlaces.format(leftFoot.maxEulerAngle));
                }
            });
        }
        if (isLoggingData) {
            //int xxx = dotData.getPacketCounter();
            if (address.equals(leftThigh.MAC)) {
                dotData.setPacketCounter(packetCounterCofficient + leftThigh.sampleCounter);
                leftThigh.normalDataLogger.update(dotData);
                leftThigh.sampleCounter++;
                leftThigh.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            } else if (address.equals(leftFoot.MAC)) {
                dotData.setPacketCounter(packetCounterCofficient + leftFoot.sampleCounter);
                leftFoot.normalDataLogger.update(dotData);
                leftFoot.sampleCounter++;
                leftFoot.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            }
        }
        // Initialization process
        if (address.equals(leftThigh.MAC)) {
            // Initialization process
            calculateInitialValue(leftThigh, dotData, eulerAngles);
            // Calculation Max and Min values
            if (startRecognition)
                determineMaxMinValues(leftThigh, dotData, eulerAngles);
        } else if (address.equals(leftFoot.MAC))
        {
            // Initialization process
            calculateInitialValue(leftFoot, dotData, eulerAngles);
            // Calculation Max and Min values
            if (startRecognition)
                determineMaxMinValues(leftFoot, dotData, eulerAngles);
        }
    }

    public void classifierModel_CNN(){
        // Prepare input data for the model
        float[][] input = new float[1][4];

        input[0][0] = (float)  leftThigh.maxEulerAngle;
        input[0][1] = (float)  leftThigh.minEulerAngle;
        input[0][2] = (float)  leftFoot.maxEulerAngle;
        input[0][3] = (float)  leftFoot.minEulerAngle;

        // Array to hold model output
        float[][] outputVal = new float[1][3];

        // Run inference
        if (tflite != null) {
            tflite.run(input, outputVal);
        }
        // Interpret the output
        String[] classLabels = {"Downstairs", "Upstairs", "Walking"};
        int maxIndex = 0;
        for (int i = 1; i < outputVal[0].length; i++) {
            if (outputVal[0][i] > outputVal[0][maxIndex]) {
                maxIndex = i;
            }
        }
        String result = "Predicted Activity: " + classLabels[maxIndex];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                valueResult.setText(result);
            }
        });
    }
    public void determineMaxMinValues(Segment segment, DotData dotData, double[] eulerAngles){

        if (segment.windowCounter <= windowSize) {
            segment.windowCounter++;
        }else {
            segment.windowCounter = 1;
            segment.angleHistory.clear();
            segment.minEulerAngle = segment.minEulerAngle_temp;
            segment.maxEulerAngle = segment.maxEulerAngle_temp;
            segment.maxEulerAngle_temp = -UNRECHABLE_VALUE;
            segment.minEulerAngle_temp =  UNRECHABLE_VALUE;
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF5.setText(threePlaces.format(leftFoot.maxEulerAngle));
                    ValueF6.setText(threePlaces.format(leftFoot.minEulerAngle));
                    ValueT5.setText(threePlaces.format(leftThigh.maxEulerAngle));
                    ValueT6.setText(threePlaces.format(leftThigh.minEulerAngle));
                }
            });
            classifierModel_CNN();
        }
        segment.angleHistory.add(eulerAngles[0] -segment.initAngleValue);

        for (Double angle : segment.angleHistory){
            if (angle > segment.maxEulerAngle_temp)
                segment.maxEulerAngle_temp = angle;
            if (angle < segment.minEulerAngle_temp)
                segment.minEulerAngle_temp = angle;
        }
    }
    public void calculateInitialValue(Segment segment, DotData dotData, double[] eulerAngles){
        if (dotData.getPacketCounter() > 1000000 && dotData.getPacketCounter() < 2000000){ // If it is just in Standing mode

            segment.sumOfInitialValue += eulerAngles[0]; // Corresponding to x axis
            segment.initAngleValue = segment.sumOfInitialValue / segment.initializationCounter;
            if (segment.initializationCounter % 120 == 119)
                writeToLogs("Initial value of" + segment.Name + ":" + threePlaces.format(segment.initAngleValue));
            segment.initializationCounter++;
        }
    }

    @Override
    public void onDotInitDone(String address) { //onDotInitDone is the callback after the initialization is successful
        if (address.equals(leftThigh.MAC)) {
            leftThigh.isReady = true;
            leftThigh.xsDevice.setOutputRate(SAMPLE_RATE);
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
            leftFoot.xsDevice.setOutputRate(SAMPLE_RATE);
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
            loggerFileName = device.getTag() + "_" + java.text.DateFormat.getDateTimeInstance().format(new Date()) + ", Subject " + subjectNumber + ".csv";
            String path = loggerFileFolder.getPath() + "/" + loggerFileName;
            File loggerFile = new File(path);

            loggerFilePaths.add(loggerFile);
            loggerFileNames.add(loggerFileName);
            writeToLogs(loggerFileName + " created");

            DotLogger logger = new DotLogger(getApplicationContext(), 1,MeasurementMode,path,device.getTag(),
                    device.getFirmwareVersion(),true,SAMPLE_RATE,null, Version,0);
            return logger;


        } catch (NullPointerException e) {
            writeToLogs("Error with creation of logger with" + device.getName());
            DotLogger logger = new DotLogger(getApplicationContext(), 1, MeasurementMode, "", device.getTag(),
                    device.getFirmwareVersion(), true, SAMPLE_RATE, null,
                    Version, 0);
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
        dataLogButton.setEnabled(true);

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

        //if (leftThigh.xsDevice.resetHeading()) {writeToLogs("Left Thigh IMU heading reset");}
        //if (leftFoot.xsDevice.resetHeading()) {writeToLogs("Left Foot IMU heading reset");}

    }

    public void disconnectButton_onClick(View view){
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
        leftThigh.xsDevice.disconnect();
        leftFoot.xsDevice.disconnect();
    }
    public void stopButton_onClick(View view){ // After measuring, the dots should be stopped to for data logging

        stopButton.setEnabled(false);
        measureButton.setEnabled(false);
        dataLogButton.setEnabled(false);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                measureButton.setText("Measuring Stopped");
            }
        });
    }
    public void uploadButton_onClick (View view){
        for (int i = 0; i < loggerFileNames.size(); i++) {
            writeToLogs("Uploading data to cloud : " + loggerFileNames.get(i));
            uploadLogFileToCloud(Uri.fromFile(loggerFilePaths.get(i)),loggerFileNames.get(i));
        }

        uploadLogFileToCloud(Uri.fromFile(logFile), "log");
    }
    private void uploadLogFileToCloud(Uri file, String fileName){

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("File is loading...");
        progressDialog.show();

        //This is where you can change the upload location and name
        StorageReference reference = storageReference.child("logs/Subject " + Integer.toString(subjectNumber) + "/" + fileName);

        reference.putFile(file).
                addOnFailureListener(new OnFailureListener() {
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

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
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