package com.example.snaprec;


import java.util.ArrayList;
import java.util.List;

public class CutSegmentAction implements EditAction {
    private double cutStart, cutEnd;
    private List<ClipSegment> removedSegments = new ArrayList<>();

    public CutSegmentAction(double start, double end) {
        this.cutStart = start;
        this.cutEnd = end;
    }

    @Override
    public void apply(VideoEditState state) {
        List<ClipSegment> newSegments = new ArrayList<>();
        for (ClipSegment seg : state.getSegments()) {
            if (seg.endTime <= cutStart || seg.startTime >= cutEnd) {
                // 完全不重疊，保留原段
                newSegments.add(seg);
            } else {
                // 重疊部分，做裁切
                if (seg.startTime < cutStart)
                    newSegments.add(new ClipSegment(seg.startTime, cutStart));
                if (seg.endTime > cutEnd)
                    newSegments.add(new ClipSegment(cutEnd, seg.endTime));

                // 紀錄被剪掉的部分（用來 undo）
                removedSegments.add(new ClipSegment(
                        Math.max(seg.startTime, cutStart),
                        Math.min(seg.endTime, cutEnd)
                ));
            }
        }
        state.setSegments(newSegments);
    }


    @Override
    public void undo(VideoEditState state) {
        state.getSegments().addAll(removedSegments);
    }
}



