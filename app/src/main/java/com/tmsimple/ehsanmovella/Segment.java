package com.tmsimple.ehsanmovella;

import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.utils.DotLogger;

public class Segment {
    public DotDevice xsDevice;
    public String[] dataOutput = {"No Data", "NA", "NA", "NA", "NA"};

    public double[] initEuler = {20.0,0.0,0.0};
    public int sampleCounter = 1;
    public String MAC;
    public String Name;
    public boolean isScanned = false;
    public boolean isConnected = false;
    public boolean isReady = false;

    public DotLogger normalDataLogger;


    public double[][] initEulerSamples;

    public Segment(String Name, String MAC){
        this.MAC = MAC;
        this.Name = Name;
    }

}
