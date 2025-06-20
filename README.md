# SnapRec

## 安裝步驟

### 需求
- 需使用Intelij開啟專案
- 需使用JDK17

### 取得專案
    git clone https://github.com/Java-applications-ntou2025/SnapRec.git

### 開啟專案
    用Intelij開啟SnapRec選到src/main/java/com/example/snaprec/ScreenRecorderApp.java

### 啟動程式
    點右上角的run
![啟動程式圖片](src\ReadmeRepository\pic1.png)

## 功能介紹

### 首頁
    按下開始錄影之後會自動縮小視窗，錄完時點擊停止錄影，會自動跳轉至剪輯介面
![首頁圖片](src\ReadmeRepository\pic2.png)

#### 滑鼠圖示切換
![滑鼠圖片](src\ReadmeRepository\pic3.png)

#### 背景圖示切換
![滑鼠圖片](src\ReadmeRepository\pic4.png)

### 剪輯介面

![剪輯圖片](src\ReadmeRepository\pic5.png)

#### 操作說明

- 播放按鈕：撥放暫停影片
- 影片進度滑桿：拖動可以預覽相應的畫面
- 剪輯模式切換按鈕：可以選擇按下匯出剪輯區段時，保留的影片區段
- 剪輯滑桿：選取剪輯範圍
- undo、redo按鈕：在匯出剪輯區段後，可以選擇回復上一步、下一步
- 預覽剪輯區段按鈕：在剪輯模式為保留時，可以快速預覽保留畫面
- 匯出剪輯區段按鈕：按下後會自動撥放剪輯完的片段
- 重新命名並匯出影片按鈕：自行設定影片檔名與保留位置，匯出後自動關閉程式
