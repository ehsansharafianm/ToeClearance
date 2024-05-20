package com.tmsimple.ehsanmovella;

import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.utils.DotLogger;

import java.util.LinkedList;

public class Segment {
    public DotDevice xsDevice;
    public String[] dataOutput = {"No Data", "NA", "NA", "NA", "NA"};

    public double initAngleValue = 0.0;
    public double sumOfInitialValue = 0.0;
    public int initializationCounter = 1;
    public int sampleCounter = 1;
    public int windowCounter = 1;
    public String MAC;
    public String Name;
    public boolean isScanned = false;
    public boolean isConnected = false;
    public boolean isReady = false;

    public DotLogger normalDataLogger;
    public double maxEulerAngle = -9999;
    public double maxEulerAngle_temp = -9999;
    public double minEulerAngle = 9999;
    public double minEulerAngle_temp = 9999;
    public LinkedList<Double> angleHistory = new LinkedList<>();


    public double[][] initEulerSamples;

    public Segment(String Name, String MAC){
        this.MAC = MAC;
        this.Name = Name;
    }

}
