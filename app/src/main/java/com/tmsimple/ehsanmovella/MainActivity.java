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
import java.util.concurrent.TimeUnit;

import android.widget.Switch;
import android.util.Log;


import org.tensorflow.lite.Interpreter;


public class MainActivity extends AppCompatActivity implements DotDeviceCallback, DotScannerCallback, DotRecordingCallback, DotSyncCallback, DotMeasurementCallback {

    public String Version = "v1.2";
    public static int UNREACHABLE_VALUE = 9999;
    public static int windowTime = 60;
    public static int strideWindow = 15;
    private Segment thigh, foot;
    private DotScanner mXsScanner;
    public  String thighMAC = "D4:22:CD:00:63:8B";
    public String footMAC = "D4:22:CD:00:63:A4";

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
    public boolean isLoggingData = false;
    public int dataLogButtonIndex = 0;
    public int[][] confusionMat = new int[5][5];
    public int[] peakPacketCounter = {0, 0};

    Button scanButton, syncButton, measureButton, disconnectButton, stopButton, uploadButton, dataLogButton,
            activity0Button, activity1Button, activity2Button, activity3Button, activity4Button, activity5Button, homeButton ;
    Switch logSwitch, ImuSwitch;
    private ArrayList<DotDevice> mDeviceLst;
    TextView thighScanStatus, footScanStatus, logContents;
    TextView ValueF1, ValueF2, ValueF3, ValueF4, ValueF5, ValueF6, ValueT1, ValueT2, ValueT3, ValueT4, ValueT5, ValueT6, valueResult;
    EditText enterSubjectNumber;
    DecimalFormat threePlaces = new DecimalFormat("##.#");
    public int packetCounterCofficient = 0;
    private Interpreter tflite;

    public int MeasurementMode;
    StorageReference storageReference;
    //DatabaseReference databaseReference;
    public int SAMPLE_RATE = 60;
    public int windowSize = 70; // It shows that window size is 1.5 s
    public String estimationResult;

    private static final int BLUETOOTH_PERMISSION_CODE = 100; //Bluetooth Permission variable
    private static final int BLUETOOTH_SCAN_PERMISSION_CODE = 101; //Bluetooth Permission variable
    public boolean conditionFindingPeak = false;

    @SuppressLint({"MissingInflatedId", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first_page);

        storageReference = FirebaseStorage.getInstance().getReference();


        // Initialize TensorFlow Lite interpreter
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @SuppressLint({"ClickableViewAccessibility", "MissingInflatedId"})
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
        ImuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){


                    thighMAC = "D4:CA:6E:F1:77:9B";
                    footMAC = "D4:CA:6E:F1:78:4E";
                    writeToLogs("IMU switch is checked for left side");
                    writeToLogs(thighMAC);
                    writeToLogs(footMAC);

                }
                else{
                    thighMAC = "D4:22:CD:00:63:8B";
                    footMAC = "D4:22:CD:00:63:A4";
                    writeToLogs("IMU switch is checked for right side");
                    writeToLogs(thighMAC);
                    writeToLogs(footMAC);
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
    @SuppressLint("ClickableViewAccessibility")
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
        activity0Button = findViewById(R.id.activity0Button);
        activity1Button = findViewById(R.id.activity1Button);
        activity2Button = findViewById(R.id.activity2Button);
        activity3Button = findViewById(R.id.activity3Button);
        activity4Button = findViewById(R.id.activity4Button);
        activity5Button = findViewById(R.id.activity5Button);
        homeButton = findViewById(R.id.homeButton);


        thighScanStatus = findViewById(R.id.thighStatusView);
        footScanStatus = findViewById(R.id.footStatusView);

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
        ImuSwitch = findViewById(R.id.ImuSwitch);

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
        ImuSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){


                    thighMAC = "D4:CA:6E:F1:77:9B";
                    footMAC = "D4:CA:6E:F1:78:4E";
                    writeToLogs("IMU switch is checked for right side");
                    writeToLogs(thighMAC);
                    writeToLogs(footMAC);
                }
                else{
                    thighMAC = "D4:22:CD:00:63:8B";
                    footMAC = "D4:22:CD:00:63:A4";
                    writeToLogs("IMU switch is checked for left side");
                    writeToLogs(thighMAC);
                    writeToLogs(footMAC);
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
        activity0Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 1000000;
                        activity0Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity0Button.setBackgroundColor(Color.parseColor("#008884"));
                        break;
                }
                return true;
            }
        });
        activity1Button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        packetCounterCofficient = 11000000;
                        activity1Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity1Button.setBackgroundColor(Color.parseColor("#B5398C"));
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
                        packetCounterCofficient = 22000000;
                        activity2Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity2Button.setBackgroundColor(Color.parseColor("#B5398C"));
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
                        packetCounterCofficient = 33000000;
                        activity3Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity3Button.setBackgroundColor(Color.parseColor("#B5398C"));
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
                        packetCounterCofficient = 44000000;
                        activity4Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity4Button.setBackgroundColor(Color.parseColor("#B5398C"));
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
                        packetCounterCofficient = 55000000;
                        activity5Button.setBackgroundColor(Color.parseColor("#05fff8"));
                        break;
                    case MotionEvent.ACTION_UP:
                        packetCounterCofficient = 0;
                        activity5Button.setBackgroundColor(Color.parseColor("#B5398C"));
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
//*//*////*//*////*//*////*//*////*//*////*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*// //*//*//




    /////////////////////////////////      Functions     /////////////////////////////

    public void processingValues(Segment segment, DotData dotData, double[] eulerAngles){

        segment.angleHistory.add(segment.angleValue);
        
        // Saving the stream data into array as the window
        for (int j = 1; j < segment.smallWindow.length; j++)
        {
            segment.smallWindow[j - 1] = segment.smallWindow[j];
        }
        segment.smallWindow[6] = segment.angleValue;
        
        if (segment == foot)
        {
            // Check three criteria for finding peak
            if (segment.smallWindow[3] < segment.smallWindow[0]     &&
                segment.smallWindow[3] < segment.smallWindow[1]     &&
                segment.smallWindow[3] < segment.smallWindow[2]     &&
                segment.smallWindow[3] < segment.smallWindow[4]     &&
                segment.smallWindow[3] < segment.smallWindow[5]     &&
                segment.smallWindow[3] < segment.smallWindow[6]     &&
                segment.smallWindow[3] < -20                             &&  // Condition 1: If is min
                Math.abs(thigh.angleValue - foot.angleValue) > 50               && // Condition 2: Distance with thigh angle in that time
                foot.sampleCounter - peakPacketCounter[1] > SAMPLE_RATE * 0.5    //  Condition 3: Distance time with the previous local minimum
                                                                            )
            {
                peakPacketCounter[0] = peakPacketCounter[1];
                peakPacketCounter[1] = foot.sampleCounter;
                conditionFindingPeak = true;
                writeToLogs("Toe-off :  " + threePlaces.format(segment.smallWindow[3])
                                        + "   +    " + threePlaces.format(thigh.angleValue - foot.angleValue)
                                        + "   +    " + String.valueOf(peakPacketCounter[1] - peakPacketCounter[0])
                                        + "   +    " + threePlaces.format(segment.angleHistory.size()));
            }
        }

        // Finding Max and Min value in that window
        for (Double angle : segment.angleHistory) {
            if (angle > segment.maxEulerAngle_temp)
                segment.maxEulerAngle_temp = angle;
            if (angle < segment.minEulerAngle_temp)
                segment.minEulerAngle_temp = angle;
        }
        segment.minEulerAngle = segment.minEulerAngle_temp;
        segment.maxEulerAngle = segment.maxEulerAngle_temp;
        segment.maxEulerAngle_temp = -UNREACHABLE_VALUE;
        segment.minEulerAngle_temp =  UNREACHABLE_VALUE;


        if (segment == foot && conditionFindingPeak) {
            writeToLogs(threePlaces.format(thigh.maxEulerAngle) + "  +  " + threePlaces.format(thigh.minEulerAngle) + "  +  " +
                    threePlaces.format(foot.maxEulerAngle) + "  +  " + threePlaces.format(foot.minEulerAngle));
            writeToLogs("-------------------------");
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF5.setText(threePlaces.format(foot.maxEulerAngle));
                    ValueF6.setText(threePlaces.format(foot.minEulerAngle));
                    ValueT5.setText(threePlaces.format(thigh.maxEulerAngle));
                    ValueT6.setText(threePlaces.format(thigh.minEulerAngle));
                }
            });
            estimationResult = classifierModel_CNN(); // Calling the Classifier Model when the time window is completed
            validationProcedure(estimationResult, dotData);
            conditionFindingPeak = false;
            foot.angleHistory.clear();
            thigh.angleHistory.clear();

        }
        ///////// Checking for Sitting and Standing mode //////////////////////////////////////
        // Saving the stream data into array as the window
        for (int j = 1; j < segment.bigWindow.length; j++)
        {
            segment.bigWindow[j - 1] = segment.bigWindow[j];
        }
        segment.bigWindow[10] = segment.angleValue;
        // Check for Standing mode ////////////////////////////////
        if(Math.abs(segment.bigWindow[0]) < 4 && Math.abs(segment.bigWindow[1]) < 4 &&
           Math.abs(segment.bigWindow[2]) < 4 && Math.abs(segment.bigWindow[3]) < 4 &&
           Math.abs(segment.bigWindow[4]) < 4 && Math.abs(segment.bigWindow[5]) < 4 &&
           Math.abs(segment.bigWindow[6]) < 4 && Math.abs(segment.bigWindow[7]) < 4 &&
           Math.abs(segment.bigWindow[8]) < 4 && Math.abs(segment.bigWindow[9]) < 4 &&
           Math.abs(segment.bigWindow[10]) < 4 &&
           Math.abs(segment.bigWindow[10] - segment.bigWindow[0])  < 2 ) {

           segment.isStanding = true;
           segment.maxEulerAngle = segment.bigWindow[5];
           segment.minEulerAngle = segment.bigWindow[5];
        }

        if(foot.isStanding && thigh.isStanding)
        {
            writeToLogs(threePlaces.format(thigh.maxEulerAngle) + "  +  " + threePlaces.format(thigh.minEulerAngle) + "  +  " +
                    threePlaces.format(foot.maxEulerAngle) + "  +  " + threePlaces.format(foot.minEulerAngle));
            writeToLogs("--------- Standing ----------");
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF5.setText(threePlaces.format(foot.maxEulerAngle));
                    ValueF6.setText(threePlaces.format(foot.minEulerAngle));
                    ValueT5.setText(threePlaces.format(thigh.maxEulerAngle));
                    ValueT6.setText(threePlaces.format(thigh.minEulerAngle));
                }
            });
            if (foot.sampleCounter % 10 == 0) {
                estimationResult = classifierModel_CNN(); // Calling the Classifier Model when the time window is completed
                validationProcedure(estimationResult, dotData);
            }
            foot.isStanding = false;
            thigh.isStanding = false;
        }


        // Checking for Sitting mode /////////////////
        if (segment == foot){
            if(Math.abs(segment.bigWindow[0]) < 4 && Math.abs(segment.bigWindow[1]) < 4 &&
                    Math.abs(segment.bigWindow[2]) < 4 && Math.abs(segment.bigWindow[3]) < 4 &&
                    Math.abs(segment.bigWindow[4]) < 4 && Math.abs(segment.bigWindow[5]) < 4 &&
                    Math.abs(segment.bigWindow[6]) < 4 && Math.abs(segment.bigWindow[7]) < 4 &&
                    Math.abs(segment.bigWindow[8]) < 4 && Math.abs(segment.bigWindow[9]) < 4 &&
                    Math.abs(segment.bigWindow[10]) < 4 &&
                    Math.abs(segment.bigWindow[10] - segment.bigWindow[0])  < 2 ) {

                segment.isSitting = true;
                segment.maxEulerAngle = segment.bigWindow[5];
                segment.minEulerAngle = segment.bigWindow[5];
            }
        }
        else if (segment == thigh) {
            if(Math.abs(segment.bigWindow[0])  > 60 && Math.abs(segment.bigWindow[1]) > 60 &&
                Math.abs(segment.bigWindow[2]) > 60 && Math.abs(segment.bigWindow[3]) > 60 &&
                Math.abs(segment.bigWindow[4]) > 60 && Math.abs(segment.bigWindow[5]) > 60 &&
                Math.abs(segment.bigWindow[6]) > 60 && Math.abs(segment.bigWindow[7]) > 60 &&
                Math.abs(segment.bigWindow[8]) > 60 && Math.abs(segment.bigWindow[9]) > 60 &&
                Math.abs(segment.bigWindow[10]) > 60 &&
                Math.abs(segment.bigWindow[10] - segment.bigWindow[0])  < 2 ) {

                segment.isSitting = true;
                segment.maxEulerAngle = segment.bigWindow[5];
                segment.minEulerAngle = segment.bigWindow[5];
            }

        }
        if(foot.isSitting && thigh.isSitting)
        {
            writeToLogs(threePlaces.format(thigh.maxEulerAngle) + "  +  " + threePlaces.format(thigh.minEulerAngle) + "  +  " +
                    threePlaces.format(foot.maxEulerAngle) + "  +  " + threePlaces.format(foot.minEulerAngle));
            writeToLogs("--------- Sitting ----------");
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF5.setText(threePlaces.format(foot.maxEulerAngle));
                    ValueF6.setText(threePlaces.format(foot.minEulerAngle));
                    ValueT5.setText(threePlaces.format(thigh.maxEulerAngle));
                    ValueT6.setText(threePlaces.format(thigh.minEulerAngle));
                }
            });

            if (foot.sampleCounter % 10 ==0) {
                estimationResult = classifierModel_CNN(); // Calling the Classifier Model when the time window is completed
                validationProcedure(estimationResult, dotData);
            }
            foot.isSitting = false;
            thigh.isSitting = false;
        }


    }
    public void validationProcedure(String estimationResult, DotData dotData){

        if (dotData.getPacketCounter() > 11000000 && dotData.getPacketCounter() < 22000000){
            if(estimationResult == "Standing")
                confusionMat[0][0] ++;
            else if (estimationResult == "Walking")
                confusionMat[0][1] ++;
            else if (estimationResult == "Upstairs")
                confusionMat[0][2] ++;
            else if (estimationResult == "Downstairs")
                confusionMat[0][3] ++;
            else if (estimationResult == "Sitting")
                confusionMat[0][4] ++;
            writeToLogs("Standing validation correct:" + confusionMat[0][0] + " Incorrect: " + (confusionMat[0][1] + confusionMat[0][2] + confusionMat[0][3] + confusionMat[0][4]));
        }
        else if (dotData.getPacketCounter() > 22000000 && dotData.getPacketCounter() < 33000000){
            if(estimationResult == "Standing")
                confusionMat[1][0] ++;
            else if (estimationResult == "Walking")
                confusionMat[1][1] ++;
            else if (estimationResult == "Upstairs")
                confusionMat[1][2] ++;
            else if (estimationResult == "Downstairs")
                confusionMat[1][3] ++;
            else if (estimationResult == "Sitting")
                confusionMat[1][4] ++;
            writeToLogs("Walking validation correct:" + confusionMat[1][1] + " Incorrect: " + (confusionMat[1][0] + confusionMat[1][2] + confusionMat[1][3] + confusionMat[1][4]));
        }
        else if (dotData.getPacketCounter() > 33000000 && dotData.getPacketCounter() < 44000000){
            if(estimationResult == "Standing")
                confusionMat[2][0] ++;
            else if (estimationResult == "Walking")
                confusionMat[2][1] ++;
            else if (estimationResult == "Upstairs")
                confusionMat[2][2] ++;
            else if (estimationResult == "Downstairs")
                confusionMat[2][3] ++;
            else if (estimationResult == "Sitting")
                confusionMat[2][4] ++;
            writeToLogs("Upstairs validation correct:" + confusionMat[2][2] + " Incorrect: " + (confusionMat[2][0] + confusionMat[2][1] + confusionMat[2][3] + confusionMat[2][4]));
        }
        else if (dotData.getPacketCounter() > 44000000 && dotData.getPacketCounter() < 55000000){
            if(estimationResult == "Standing")
                confusionMat[3][0] ++;
            else if (estimationResult == "Walking")
                confusionMat[3][1] ++;
            else if (estimationResult == "Upstairs")
                confusionMat[3][2] ++;
            else if (estimationResult == "Downstairs")
                confusionMat[3][3] ++;
            else if (estimationResult == "Sitting")
                confusionMat[3][4] ++;
            writeToLogs("Downstairs validation correct:" + confusionMat[3][3] + " Incorrect: " + (confusionMat[3][0] + confusionMat[3][1] + confusionMat[3][2] + confusionMat[3][4]));
        }
        else if (dotData.getPacketCounter() > 55000000 && dotData.getPacketCounter() < 66000000){
            if(estimationResult == "Standing")
                confusionMat[4][0] ++;
            else if (estimationResult == "Walking")
                confusionMat[4][1] ++;
            else if (estimationResult == "Upstairs")
                confusionMat[4][2] ++;
            else if (estimationResult == "Downstairs")
                confusionMat[4][3] ++;
            else if (estimationResult == "Sitting")
                confusionMat[4][4] ++;
            writeToLogs("Sitting validation correct:" + confusionMat[4][4] + " Incorrect: " + (confusionMat[4][0] + confusionMat[4][1] + confusionMat[4][2] + confusionMat[4][3]));
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

    public void fillFields(String address, DotData dotData, double[] eulerAngles){

        if (address.equals(thigh.MAC)) {
            thigh.angleValue = eulerAngles[0] - thigh.initAngleValue;
            thigh.dataOutput[0] = threePlaces.format(eulerAngles[0] - thigh.initAngleValue);
            thigh.dataOutput[1] = threePlaces.format(eulerAngles[1]);
            thigh.dataOutput[2] = threePlaces.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {

                    ValueT1.setText(thigh.dataOutput[0] + "   deg");
                    ValueT2.setText(thigh.dataOutput[1] + "   deg");
                    ValueT3.setText(String.valueOf(thigh.dataOutput[3]));
                    ValueT4.setText(thigh.xsDevice.getBatteryPercentage() + "   %");

                }
            });
        } else if (address.equals(foot.MAC)) {
            foot.angleValue = eulerAngles[0] - foot.initAngleValue;
            foot.dataOutput[0] = threePlaces.format(eulerAngles[0]- foot.initAngleValue);
            foot.dataOutput[1] = threePlaces.format(eulerAngles[1]);
            foot.dataOutput[2] = threePlaces.format(eulerAngles[2]);
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    ValueF1.setText(foot.dataOutput[0] + "   deg");
                    ValueF2.setText(foot.dataOutput[1] + "   deg");
                    ValueF3.setText(String.valueOf(foot.dataOutput[3]));
                    ValueF4.setText(foot.xsDevice.getBatteryPercentage() + "   %");
                }
            });
        }
    }
    public String classifierModel_CNN(){

        // Start timing
        long startTime = System.nanoTime();

        // Prepare input data for the model
        float[][] input = new float[1][4];

        input[0][0] = (float)  thigh.maxEulerAngle;
        input[0][1] = (float)  thigh.minEulerAngle;
        input[0][2] = (float)  foot.maxEulerAngle;
        input[0][3] = (float)  foot.minEulerAngle;

        // Array to hold model output
        float[][] outputVal = new float[1][5];

        // Run inference
        if (tflite != null) {
            tflite.run(input, outputVal);
        }
        // Interpret the output
        String[] classLabels = {"Downstairs", "Sitting", "Standing", "Upstairs", "Walking"};
        int maxIndex = 0;
        for (int i = 1; i < outputVal[0].length; i++) {
            if (outputVal[0][i] > outputVal[0][maxIndex]) {
                maxIndex = i;
            }
        }
        String result = classLabels[maxIndex];
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                valueResult.setText(result);
            }
        });

        // Log the duration
        long endTime = System.nanoTime();
        long durationInMs = (endTime - startTime) / 1000000;
        Log.d("Performance", "Execution time of classifierModel_CNN: " + durationInMs + " ms");

        return result;
    }

    //////////////////////////////////////  Sequence of syncing  ///////////////////////////////////////////////////////////////////

    public void scanButton_onClick(View view){


        thigh = new Segment("Thigh IMU", thighMAC);
        foot = new Segment("Foot IMU", footMAC);

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
    @Override
    public void onDotConnectionChanged(String address, int state) {
        if (state == DotDevice.CONN_STATE_CONNECTED){
            if(address.equals(thigh.MAC)){
                if(address.equals(thigh.MAC)){
                    thigh.isConnected = true;
                    writeToLogs("Thigh IMU is connected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {thighScanStatus.setText("Connected");}
                    });
                }
            }
            else if(address.equals(foot.MAC)){
                if(address.equals(foot.MAC)){
                    foot.isConnected = true;
                    writeToLogs("Foot IMU is connected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {footScanStatus.setText("Connected");}
                    });
                }
            }

        }
        else if (state == DotDevice.CONN_STATE_DISCONNECTED){
            if(address.equals(thigh.MAC)){
                if(address.equals(thigh.MAC)){
                    thigh.isConnected = false;
                    writeToLogs("Thigh IMU is disconnected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {thighScanStatus.setText("Disconnected");}
                    });
                }
            }
            else if(address.equals(foot.MAC)){
                if(address.equals(foot.MAC)){
                    foot.isConnected = false;
                    writeToLogs("Foot IMU is disconnected!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {footScanStatus.setText("Disconnected");}
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
    public void onDotScanned(BluetoothDevice bluetoothDevice, int i) {
        String address = bluetoothDevice.getAddress();

        if(address.equals(thigh.MAC) && !thigh.isScanned){
            thigh.isScanned = true;
            thigh.xsDevice = new DotDevice(this.getApplicationContext(), bluetoothDevice, MainActivity.this);
            thigh.xsDevice.connect();
            thigh.isConnected = true;

            writeToLogs(thigh.Name + " is scanned and logger is created");
            mDeviceLst.add(thigh.xsDevice);
        }
        else if(address.equals(foot.MAC) && !foot.isScanned){
            foot.isScanned = true;
            foot.xsDevice = new DotDevice(this.getApplicationContext(), bluetoothDevice, MainActivity.this);
            foot.xsDevice.connect();
            foot.isConnected = true;

            mDeviceLst.add(foot.xsDevice);
            writeToLogs(foot.Name + " is scanned and logger is created");
        }
        if(thigh.isScanned && foot.isScanned){
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run(){footScanStatus.setText("Scanned");}
            });
            if(mXsScanner.stopScan()){
                writeToLogs("Scan Stopped!");
                //pauseMillis(500);
            }
        }
    }
    @Override
    public void onDotInitDone(String address) { //onDotInitDone is the callback after the initialization is successful
        if (address.equals(thigh.MAC)) {
            thigh.isReady = true;
            thigh.xsDevice.setOutputRate(SAMPLE_RATE);
            writeToLogs("Thigh IMU sample rate is : " + String.valueOf(thigh.xsDevice.getCurrentOutputRate()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    thighScanStatus.setText("Ready");
                }
            });
        }
        else if (address.equals(foot.MAC)) {
            foot.isReady = true;
            foot.xsDevice.setOutputRate(SAMPLE_RATE);
            writeToLogs("Foot IMU sample rate is : " + String.valueOf(foot.xsDevice.getCurrentOutputRate()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    footScanStatus.setText("Ready");
                }
            });
        }
        if (thigh.isReady && foot.isReady){
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
    }
    public void syncButton_onClick(View view){
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

        // Stop Any Previous Syncing
        DotSyncManager.getInstance(this).stopSyncing();
        mDeviceLst.get(0).setRootDevice(true);
        DotSyncManager.getInstance(this).startSyncing(mDeviceLst, 100);
    }
    @Override
    public void onSyncingDone(HashMap<String, Boolean> hashMap, boolean b, int i) {

        MeasurementMode = DotPayload.PAYLOAD_TYPE_CUSTOM_MODE_1;
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
        thigh.xsDevice.setMeasurementMode(MeasurementMode);
        foot.xsDevice.setMeasurementMode(MeasurementMode);

        writeToLogs(String.valueOf("Measurement Mode: " + thigh.xsDevice.getMeasurementMode())
                + " / " + foot.xsDevice.getMeasurementMode());
        writeToLogs("\n ---------- Syncing is done! --------- \n");
    }

    @Override
    public void onDotDataChanged(String address, DotData dotData) {

        double[] eulerAngles = dotData.getEuler();

        /* Fill the fields with appropriate values */
        fillFields(address, dotData, eulerAngles);

        if (isLoggingData) {
            //int xxx = dotData.getPacketCounter();
            if (address.equals(thigh.MAC)) {
                dotData.setPacketCounter(packetCounterCofficient + thigh.sampleCounter);
                thigh.normalDataLogger.update(dotData);
                thigh.sampleCounter++;
                thigh.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            } else if (address.equals(foot.MAC)) {
                dotData.setPacketCounter(packetCounterCofficient + foot.sampleCounter);
                foot.normalDataLogger.update(dotData);
                foot.sampleCounter++;
                foot.dataOutput[3] = threePlaces.format(dotData.getPacketCounter());
            }
        }
        View view = findViewById(R.id.recognition_page);

        if (address.equals(thigh.MAC)) {
            // Initialization process
            calculateInitialValue(thigh, dotData, eulerAngles);
            if (findViewById(R.id.recognition_page) != null) // To check if we are using the recognition page
                processingValues(thigh, dotData, eulerAngles);
        } else if (address.equals(foot.MAC))
        {
            // Initialization process
            calculateInitialValue(foot, dotData, eulerAngles);
            if (findViewById(R.id.recognition_page) != null) // To check if we are using the recognition page
                processingValues(foot, dotData, eulerAngles);
        }


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


    public void measureButton_onClick(View view){

        stopButton.setEnabled(true); // After starting measuring the stop button will be activated
        dataLogButton.setEnabled(true);

        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {measureButton.setText("Measuring...");}
        });


        if (thigh.xsDevice != null)
            thigh.normalDataLogger = createDataLog(thigh.xsDevice);
        if (foot.xsDevice != null)
            foot.normalDataLogger = createDataLog(foot.xsDevice);

        if (thigh.xsDevice.startMeasuring()) {writeToLogs("Left Thigh IMU is measuring");}
        if (foot.xsDevice.startMeasuring()) {writeToLogs("Left Foot IMU is measuring");}

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
        thigh.xsDevice.disconnect();
        foot.xsDevice.disconnect();
    }
    public void stopButton_onClick(View view){ // After measuring, the dots should be stopped to for data logging

        stopButton.setEnabled(false);
        measureButton.setEnabled(false);
        dataLogButton.setEnabled(false);
        uploadButton.setEnabled(true);
        writeToLogs("Stopping");
        if(thigh.xsDevice != null){
            try{
                thigh.xsDevice.stopMeasuring();
                thigh.normalDataLogger.stop();
                writeToLogs(thigh.Name + " measuring stopped");
            }catch (NullPointerException e) {
                writeToLogs("Error: Not connected to " + thigh.Name);
                e.printStackTrace();
            }
        }
        if(foot.xsDevice != null){try{
            foot.xsDevice.stopMeasuring();
            foot.normalDataLogger.stop();
            writeToLogs(foot.Name + " measuring stopped");
        }catch (NullPointerException e) {
            writeToLogs("Error: Not connected to " + foot.Name);
            e.printStackTrace();
        }}
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                measureButton.setText("Measuring Stopped");
            }
        });
        if (findViewById(R.id.recognition_page) != null) // To check if we are using the recognition mode
        {
            writeToLogs("----------  Confusion Matrix --------------");
            for (int i = 0; i < 5; i++) {

                writeToLogs(confusionMat[i][0] + "  " + confusionMat[i][1]
                             + "  " + confusionMat[i][2] + "  " + confusionMat[i][3] + "  " + confusionMat[i][4]);

            }
        }
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
    /* For interpretation of the Model from Tensorflow light to Java code */
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("model_v4.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    public void pauseMillis(int milliseconds){
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            writeToLogs("Pause Failed");
            e.printStackTrace();
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