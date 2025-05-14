package com.example.snaprec;

public class NativeScreenCapture {
    static {
//        System.loadLibrary("ScreenCapture"); // 載入名為 ScreenCapture.dll 的本地庫
//            System.load("C:/Users/t0910/OneDrive/codes/JavaFinal/SnapRec/src/main/java/com/example/snaprec/ScreenCapture.dll");
            String basePath = System.getProperty("user.dir"); // 取得目前工作目錄
            String dllPath = basePath + "/src/main/java/com/example/snaprec/ScreenCapture.dll";
            System.load(dllPath);
//        "C:/Users/t0910/OneDrive/codes/JavaFinal/SnapRec/src/main/java/com/example/snaprec/ScreenCapture.dll");
    }

    public native byte[] captureFrame();
}
