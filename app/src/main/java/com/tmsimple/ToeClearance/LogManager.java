package com.tmsimple.ToeClearance;

import android.view.View;
import android.widget.TextView;
import android.content.Context;

import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.utils.DotLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class LogManager {

    private TextView logContents;
    private Context context;
    private File logFile;
    public String Version = "v1.2";
    public int SAMPLE_RATE = 60; //Hz
    public ArrayList<File> loggerFilePaths = new ArrayList<>();
    public ArrayList<String> loggerFileNames = new ArrayList<>();
    private ImuManager imuManager;

    public LogManager(Context context, TextView logContents, File logFile) {
        this.context = context;
        this.logContents = logContents;
        this.logFile = logFile;
    }
    public void setLogContents(TextView logContents) {
        this.logContents = logContents;
    }
    public void setImuManager(ImuManager imuManager) {
        this.imuManager = imuManager;
    }

    // You can update the log file later if needed
    public void setLogFile(File logFile, int subjectNumber) {
        this.logFile = logFile;
        log("Subject number set: " + subjectNumber);
        log("Log File Created");
    }
    public void setLogVisible(boolean visible) {
        if (logContents != null) {
            logContents.post(() -> {
                logContents.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            });
        }
    }

    public void log(String message) {
        String logMessage = java.text.DateFormat.getDateTimeInstance().format(new Date()) + ": " + message + "\n";

        // Append to UI on UI thread
        if (logContents != null) {
            logContents.post(() -> {
                try {
                    logContents.append("\n" + message);
                    int scrollAmount = logContents.getLayout().getLineTop(logContents.getLineCount()) - logContents.getHeight();
                    logContents.scrollTo(0, Math.max(scrollAmount, 0));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Write to file
        if (logFile != null) {
            try (FileOutputStream stream = new FileOutputStream(logFile, true)) {
                stream.write(logMessage.getBytes());
            } catch (IOException | NullPointerException e) {
                if (logContents != null) {
                    logContents.post(() -> logContents.append("\nError: Log file not found"));
                }
            }
        }
    }
    public DotLogger createDataLog(DotDevice device, String subjectTitle, int subjectNumber,  ImuManager imuManager) {

        try {
            File loggerFileFolder;
            String loggerFileName;
            loggerFileFolder = context.getApplicationContext().getExternalFilesDir(subjectTitle + "/" + device.getTag());
            loggerFileName = device.getTag() + "_" + java.text.DateFormat.getDateTimeInstance().format(new Date()) + ", Subject " + subjectNumber + ".csv";
            String path = loggerFileFolder.getPath() + "/" + loggerFileName;
            File loggerFile = new File(path);

            loggerFilePaths.add(loggerFile);
            loggerFileNames.add(loggerFileName);
            log(loggerFileName + " created");

            DotLogger logger = new DotLogger(context.getApplicationContext(), 1, imuManager.getMeasurementMode(), path, device.getTag(),
                    device.getFirmwareVersion(), true, SAMPLE_RATE, null, Version, 0);
            return logger;


        } catch (NullPointerException e) {
            log("Error with creation of logger with" + device.getName());
            DotLogger logger = new DotLogger(context.getApplicationContext(), 1, imuManager.getMeasurementMode(), "", device.getTag(),
                    device.getFirmwareVersion(), true, SAMPLE_RATE, null,
                    Version, 0);
            return logger;
        }
    }



}
