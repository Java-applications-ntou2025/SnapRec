package com.example.snaprec;

import org.jnativehook.GlobalScreen;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalMouseListener implements NativeMouseListener {

    private final Recorder recorder;

    public GlobalMouseListener(Recorder recorder) {
        this.recorder = recorder;
        try {
            // 關掉 jnativehook 的 debug 訊息
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            logger.setUseParentHandlers(false);

            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeMouseListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        int x = (int)(e.getX() * 0.8);
        int y = (int)(e.getY() * 0.8);
        System.out.println("全域滑鼠點擊：" + x + ", " + y);
        recorder.setZoomCenter(new Point(x, y));
        recorder.addClickEffect(new Point(x, y));
    }


    @Override public void nativeMousePressed(NativeMouseEvent e) {}
    @Override public void nativeMouseReleased(NativeMouseEvent e) {}

    public void stop() {
        try {
            GlobalScreen.removeNativeMouseListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
