package com.tmsimple.ToeClearance;

import android.view.View;
import android.widget.TextView;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class LogManager {

    private TextView logContents;
    private Context context;
    private File logFile;

    public LogManager(Context context, TextView logContents, File logFile) {
        this.context = context;
        this.logContents = logContents;
        this.logFile = logFile;
    }
    public void setLogContents(TextView logContents) {
        this.logContents = logContents;
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
}
