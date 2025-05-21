package com.example.snaprec;

public class ClipSegment {
    public double startTime;
    public double endTime;

    public ClipSegment(double startTime, double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ClipSegment copy() {
        return new ClipSegment(startTime, endTime);
    }
}
