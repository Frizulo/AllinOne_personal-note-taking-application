# 📘 AllinOne — Personal Productivity & Time Management System

AllinOne 是一套整合 **任務管理（Tasks）**、**行程排程（Schedule）** 與 **時間分析（Analysis）** 的跨平台個人生產力系統，支援 **Android App** 與 **Web 版本**，目標是協助使用者有效管理時間、提升專注度，並透過數據化分析了解自身的工作型態。


## 🚀 系統特色（Key Features）

### 📱 Android App

* **首頁儀表板（Home）**
  * 即時天氣顯示（可選縣市）
  * 今日待辦進度
  * 未完成任務統計
* **任務管理（Tasks）**
  * 新增 / 編輯 / 完成任務
  * 支援任務狀態（not yet / in progress / done）
* **行程排程（Schedule）**
  * 以時間區段方式安排每日行程
  * 支援週 / 月切換與可捲動時間軸
* **分析報表（Analysis）**
  * 總累計時長、Task 與純行程占比
  * 四時段分析（深夜 / 早 / 中 / 晚）
  * 可點擊柱狀圖查看該時段詳細數據
* **登入 / 登出機制**

  * 支援帳號登入
  * Token-based 身分管理


### 🌐 Web 版本

* 提供與 App 對應的功能（任務）
* 作為跨平台存取與資料同步的輔助介面

🔗 **Web 版 DEMO 網址**
👉 [http://210.240.160.82:9090/](http://210.240.160.82:9090/)


## ⛅ 天氣資料來源（Weather API）

本系統之天氣資訊來自 **Open-Meteo**（免費）：

* 官方網站：[https://open-meteo.com/](https://open-meteo.com/)
* 提供即時天氣、預報與天氣代碼（weather_code）
* Android App 會依據 weather_code 動態顯示對應天氣圖示


## 🧠 分析指標說明（Analysis Metrics）

* **總累計時長**：指定期間內所有行程與任務的總時間
* **Task 佔比（Focus Density）**
  = Task 時間 ÷ 總時間
  → 用於衡量使用者有多少時間投入在「可交付的任務」
* **四時段分析**

  * 深夜：00–06
  * 早：06–12
  * 中：12–18
  * 晚：18–24
    每個時段以堆疊柱狀圖呈現 Task / 純行程分布


## 🏗️ 系統架構概覽

```
[ Android App ]              [ Web UI ]
        │                        │
        └────── HTTP / API ──────┘
                     │
            [ Backend Server ]
                     │
               [ Database ]
```

* App 與 Web 共用後端 API
* 行程、任務與分析資料可跨平台同步


## 🛠️ Tech Stack

### Android
* Kotlin
* Jetpack Compose
* MVVM Architecture
* Room (Local Database)
* Flow / State Management

### Backend / Web
* RESTful API
* Web-based UI
* Centralized Database

### Third-Party
* Open-Meteo Weather API
