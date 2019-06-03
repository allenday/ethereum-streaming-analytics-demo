package com.google.allenday;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

@DefaultCoder(AvroCoder.class)
public class Stats {
    Stats() {
    }

    Stats(long min, long max, double mean) {
        this.min = min;
        this.max = max;
        this.mean = mean;
    }

    private long min;

    private long max;

    private double mean;

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(long mean) {
        this.mean = mean;
    }
}
