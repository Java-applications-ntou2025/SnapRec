package com.example.snaprec;

import java.awt.*;

public class ClickEffect {
    final Point location;
    final long timestamp; // 開始時間

    ClickEffect(Point location) {
        this.location = location;
        this.timestamp = System.nanoTime();
    }

    boolean isExpired() {
        return System.nanoTime() - timestamp > 500_000_000L; // 0.5 秒
    }

    double getProgress() {
        return (System.nanoTime() - timestamp) / 500_000_000.0;
    }
}
