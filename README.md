# SmartNotify Native Android Application (Phase 8 Focus Mode & Intelligent Alerting)

Native Android mobile application for **SmartNotify – ML-Based Intelligent Notification Management System**. 
Phase 8 connects real-time ML priority predictions (`LOW`, `MEDIUM`, `HIGH`) to the **`NotificationDecisionEngine`** and official Android Notification Channels (`smartnotify_soft`, `smartnotify_important`) based on active **Focus Mode** status and user **Alert Preferences**.

---

## 📱 Application Architecture & Package Layout

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/smartnotify/app/
│   │   │   ├── MainActivity.kt               # Main ComponentActivity
│   │   │   ├── decision/
│   │   │   │   └── NotificationDecisionEngine.kt # Decision Matrix & Android Channel Alert Executor
│   │   │   ├── service/
│   │   │   │   └── SmartNotifyNotificationListenerService.kt # Triggers async classifier
│   │   │   ├── classifier/
│   │   │   │   └── NotificationClassifier.kt  # Calls FastAPI POST /predict & NotificationDecisionEngine
│   │   │   ├── data/
│   │   │   │   ├── api/
│   │   │   │   │   ├── ApiModels.kt           # PredictionApiRequest, PredictionApiResponse, HealthCheckResponse
│   │   │   │   │   ├── PredictionApi.kt       # Retrofit Interface
│   │   │   │   │   └── ApiClient.kt           # Retrofit Client (http://10.0.2.2:8000/)
│   │   │   │   ├── model/Models.kt            # ActionTaken (SUPPRESSED_BY_SMARTNOTIFY, SOFT_ALERT, IMPORTANT_ALERT, NORMAL_BEHAVIOR)
│   │   │   │   └── repository/Repositories.kt # NotificationRepository, FocusRepository (Timer Countdown), PreferencesRepository
│   │   │   ├── viewmodel/ViewModels.kt       # HomeViewModel, FocusViewModel, HistoryViewModel, StatisticsViewModel, SettingsViewModel
│   │   │   ├── navigation/
│   │   │   │   ├── Screen.kt                  # Navigation routes
│   │   │   │   └── SmartNotifyNavGraph.kt     # Bottom Navigation & NavHost
│   │   │   └── ui/
│   │   │       ├── theme/Color.kt, Theme.kt
│   │   │       ├── components/Components.kt   # ActionTakenText badges
│   │   │       ├── home/HomeScreen.kt         # Live Dashboard with Focus status
│   │   │       ├── focus/FocusScreen.kt       # Live HH:MM:SS timer countdown & duration presets
│   │   │       ├── history/HistoryScreen.kt   # Real-time action log (Suppressed / Soft Alert / Important Alert)
│   │   │       ├── statistics/StatisticsScreen.kt # Decision engine action analytics
│   │   │       └── settings/SettingsScreen.kt
│   │   └── AndroidManifest.xml                # Registered NotificationListenerService & permissions
│   └── build.gradle.kts                       # Retrofit, OkHttp, Gson dependencies
├── docs/
│   └── android_limitations.md                 # Technical document detailing AOSP OS boundaries & official capabilities
├── build.gradle.kts
└── settings.gradle.kts
```

---

## ⚙️ Phase 8 Decision Engine Matrix

| ML Priority Prediction | Focus Mode State | Action Evaluated & Executed | Supported Android Behavior |
|---|---|---|---|
| **Any (`LOW`, `MED`, `HIGH`)** | **`OFF`** | `NORMAL_BEHAVIOR` | Logged in history; standard native notification behavior |
| **`LOW`** | **`ACTIVE`** | `SUPPRESSED_BY_SMARTNOTIFY` | Quietly logged in history; **no extra SmartNotify alert banner created** |
| **`MEDIUM`** | **`ACTIVE`** | `SOFT_ALERT` | Low-intrusion silent notification banner issued on `smartnotify_soft` channel |
| **`HIGH`** | **`ACTIVE`** | `IMPORTANT_ALERT` | High-priority banner issued on `smartnotify_important` channel (Vibrate, Sound, ScreenOn per user preferences) |

---

## 🧪 Phase 8 End-to-End Acceptance Test Workflow

```
1. Ensure FastAPI ML Backend is Running (Port 8000)
   $ uvicorn backend.app.main:app --host 0.0.0.0 --port 8000

2. Launch SmartNotify App on Emulator / Device
   -> Home screen shows: "FastAPI Backend (http://10.0.2.2:8000): Connected (Healthy)"

3. Activate Focus Mode (Select 1 Hour Duration)
   -> Focus Mode status changes to ACTIVE. Live countdown displays remaining time (00:59:59).

4. Test Case 1: Incoming LOW Priority Notification (e.g. Shopping Sale Promo)
   a. Listener captures notification & sends to FastAPI POST /predict.
   b. FastAPI model predicts LOW.
   c. NotificationDecisionEngine evaluates Focus Mode = ACTIVE + Priority = LOW.
   d. Action executed: SUPPRESSED_BY_SMARTNOTIFY (No extra alert banner generated).
   e. History Screen records item: "LOW (92%) - SUPPRESSED BY SMARTNOTIFY (Focus Protected)".

5. Test Case 2: Incoming HIGH Priority Notification (e.g. "PROD DOWN Payment Failure")
   a. Listener captures notification & sends to FastAPI POST /predict.
   b. FastAPI model predicts HIGH.
   c. NotificationDecisionEngine evaluates Focus Mode = ACTIVE + Priority = HIGH.
   d. Action executed: IMPORTANT_ALERT.
   e. Android issue banner on smartnotify_important channel (Vibrate & Sound per settings).
   f. History Screen records item: "HIGH (83%) - IMPORTANT ALERT (Focus Mode Override Triggered)".

6. Test Case 3: Stop Focus Mode (or Timer Expires)
   a. Focus Mode automatically transitions to OFF.
   b. Future notifications are processed with NORMAL_BEHAVIOR.
```

---

## ⚠️ Platform Limitations Reference
For detailed documentation on AOSP OS notification muting restrictions, channel user authority, and full-screen intent policies, refer to [`docs/android_limitations.md`](file:///c:/Users/2005k/OneDrive/Documents/Vamsi/Temp/Notifications/docs/android_limitations.md).
