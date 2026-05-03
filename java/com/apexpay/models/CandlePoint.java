package com.apexpay.models;

public class CandlePoint {
    public float open;
    public float close;
    public float high;
    public float low;
    public long  timestamp;

    public CandlePoint(float open, float close, float high, float low, long timestamp) {
        this.open      = open;
        this.close     = close;
        this.high      = high;
        this.low       = low;
        this.timestamp = timestamp;
    }
}