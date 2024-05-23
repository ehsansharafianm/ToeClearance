package com.tmsimple.ehsanmovella;

import static com.tmsimple.ehsanmovella.MainActivity.UNREACHABLE_VALUE;

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
    public double maxEulerAngle = -UNREACHABLE_VALUE;
    public double maxEulerAngle_temp = -UNREACHABLE_VALUE;
    public double minEulerAngle = UNREACHABLE_VALUE;
    public double minEulerAngle_temp = UNREACHABLE_VALUE;
    public LinkedList<Double> angleHistory = new LinkedList<>();


    public double[][] initEulerSamples;

    public Segment(String Name, String MAC){
        this.MAC = MAC;
        this.Name = Name;
    }

}
