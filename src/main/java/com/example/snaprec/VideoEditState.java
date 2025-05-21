package com.example.snaprec;

import java.util.ArrayList;
import java.util.List;

public class VideoEditState {
    private List<ClipSegment> segments;

    public VideoEditState() {
        this.segments = new ArrayList<>();
    }

    public VideoEditState(List<ClipSegment> original) {
        this.segments = new ArrayList<>();
        for (ClipSegment s : original) {
            this.segments.add(s.copy());
        }
    }

    public List<ClipSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<ClipSegment> newSegs) {
        segments = new ArrayList<>(newSegs);
    }

    public VideoEditState copy() {
        return new VideoEditState(segments);
    }
}

