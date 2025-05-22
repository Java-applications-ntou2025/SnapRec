package com.example.snaprec;

import java.awt.*;

public class ZoomEffect {
    public Point center;
    public int offsetX = 0;
    public int offsetY = 0;

    final long startTime;
    final long durationExpand = 600_000_000L; // 放大動畫 0.5 秒
    final long durationHold = 3_000_000_000L;  // 停留 3 秒
    final long durationShrink = 600_000_000L; // 縮回動畫 0.5 秒
    final long totalDuration = durationExpand + durationHold + durationShrink;
    final double zoomScale = 1.5;  // 假設這裡設置放大倍率為 1.5

    public ZoomEffect(Point center) {
        this.center = center;
        this.startTime = System.nanoTime();
    }

    // 平滑插值（Ease In and Out）
    private double easeInOut(double t) {
        return t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    public double getCurrentScale() {
        long elapsed = System.nanoTime() - startTime;

        if (elapsed <= durationExpand) {
            // 放大中：1.0 → zoomScale
            double progress = (double) elapsed / durationExpand;
            progress = easeInOut(progress);  // 加入平滑插值
            return 1.0 + (zoomScale - 1.0) * progress;
        } else if (elapsed <= durationExpand + durationHold) {
            // 保持最大倍率
            return zoomScale;
        } else if (elapsed <= totalDuration) {
            // 縮回中：zoomScale → 1.0
            double shrinkElapsed = elapsed - durationExpand - durationHold;
            double progress = shrinkElapsed / (double) durationShrink;
            progress = easeInOut(progress);  // 加入平滑插值
            return zoomScale - (zoomScale - 1.0) * progress;
        } else {
            // 已經結束
            return 1.0;
        }
    }

    public boolean isExpired() {
        return System.nanoTime() - startTime > totalDuration;
    }
}
