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
    public double sumOfInitialGyro = 0.0;
    public double sumOfInitialAccel = 0.0;

    public double initRollValue = 0.0;
    public double initGyroValue = 0.0;
    public double initAccelValue = 0.0;




    public Segment(String Name, String MAC){
        this.MAC = MAC;
        this.Name = Name;
    }

}