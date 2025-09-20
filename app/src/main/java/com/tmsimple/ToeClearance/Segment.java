package com.tmsimple.ToeClearance;

import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.utils.DotLogger;

public class Segment {
    public DotDevice xsDevice;
    public String[] dataOutput = {"No Data", "NA", "NA", "NA", "NA"};
    public double angleValue = 0.0;

    public int initializationCounter = 1;
    public int sampleCounter = 1;
    public String MAC;
    public String Name;
    public boolean isScanned = false;
    public boolean isConnected = false;
    public boolean isReady = false;

    public DotLogger normalDataLogger;

    // Calibration fields
    public double sumOfInitialRoll = 0.0;
    public double sumOfInitialGyroX = 0.0, sumOfInitialGyroY = 0.0, sumOfInitialGyroZ = 0.0;
    public double sumOfInitialAccelX = 0.0, sumOfInitialAccelY = 0.0, sumOfInitialAccelZ = 0.0;

    public double initRollValue = 0.0;
    public double initGyroXValue = 0.0, initGyroYValue = 0.0, initGyroZValue = 0.0;
    public double initAccelXValue = 0.0, initAccelYValue = 0.0, initAccelZValue = 0.0;




    public Segment(String Name, String MAC){
        this.MAC = MAC;
        this.Name = Name;
    }

}