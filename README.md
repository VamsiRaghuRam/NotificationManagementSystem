# SmartNotify – ML-Based Intelligent Notification Management System

> **Academic Capstone Project**  
> **Final Release:** Version 1.0 (Phase 10 Complete Integration & Delivery)

---

## 📖 1. Project Overview & Problem Statement

Modern mobile users receive dozens of notifications daily—ranging from critical server alerts to routine promotional ads and social media noise. Unfiltered notifications break focus, reduce productivity, and cause notification fatigue.

**SmartNotify** solves this by classifying incoming Android notifications using machine learning into **`LOW`**, **`MEDIUM`**, or **`HIGH`** priority. During active **Focus Mode**, SmartNotify suppresses low-priority noise while allowing urgent alerts to reach the user. Additionally, SmartNotify incorporates an automated **Caller Urgency Triage** mechanism so callers can signal emergency callback needs without forcing constant interruptions.

---

## ✨ 2. Key Features

- **Real-Time Notification Interception:** Intercepts system notifications via Android's native `NotificationListenerService`.
- **On-Premise Machine Learning Inference:** Classifies notifications in real time using a calibrated **Support Vector Machine (LinearSVC)** backend model trained on 10,800 records.
- **Intelligent Decision Engine:**
  - **Focus Mode OFF:** Delivers standard native notification behavior.
  - **Focus Mode ON:**
    - `LOW` Priority $\rightarrow$ Suppressed silently in quiet history summary.
    - `MEDIUM` Priority $\rightarrow$ Soft alert banner on `smartnotify_soft` channel.
    - `HIGH` Priority $\rightarrow$ Urgent alert override on `smartnotify_important` channel (Vibration, Sound, Screen-On flags).
- **Automated Caller Urgency Triage:** Sends automated SMS triage options (`SMARTNOTIFY 1` for immediate callback, `SMARTNOTIFY 2` for callback when free) to incoming callers during Focus Mode.
- **Privacy-First Architecture:** Phone numbers, call logs, and notification contents are kept local; phone numbers are **NEVER** transmitted to external ML APIs or LLMs.
- **Analytics & History Dashboard:** Real-time stats on analyzed notifications, suppressed items, override alerts, and pending caller requests.

---

## 🛠️ 3. Technology Stack

- **Android Application:** Kotlin, Jetpack Compose Material 3, StateFlow, Coroutines, Android Telephony APIs.
- **ML Backend:** Python 3.13, FastAPI, Uvicorn, Scikit-learn, Joblib.
- **Machine Learning:** TF-IDF (Unigram + Bigram), Support Vector Machine (LinearSVC + Platt Scaling), Logistic Regression, Naive Bayes.
- **Datasets:** NotifAI Dataset, Smartphone Notifications Dataset (10,800 raw rows consolidated into `training_data.jsonl`).

---

## 📊 4. ML Model Benchmarks & Comparison

Three algorithms were trained and evaluated on 10,515 cleaned dataset records (80/20 train/test split):

| Model | Accuracy | Macro F1 | HIGH Priority Recall | HIGH Priority F1 | Selection Status |
|---|---|---|---|---|---|
| **Support Vector Machine (LinearSVC)** | **71.42%** | **0.6766** | **69.62%** | **0.6944** | **SELECTED MODEL** |
| **Logistic Regression** | 70.61% | 0.6768 | 71.14% | 0.6887 | Evaluated |
| **Multinomial Naive Bayes** | 70.04% | 0.6544 | 59.49% | 0.6456 | Evaluated |

---

## 🏗️ 5. System Architecture

```
[Android OS Notification] ──► [NotificationListenerService] ──► [FastAPI POST /predict]
                                                                          │
                                                                          ▼
[User Action / Alert] ◄── [Decision Engine] ◄── [LOW / MED / HIGH] ◄── [SVM Model]
```

---

## 🚀 6. Setup & Execution Instructions

### Step 1: Start FastAPI ML Backend
```bash
# In project root
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000
```
Verify health check:
- `GET http://localhost:8000/health` $\rightarrow$ `{"status": "healthy", "model_loaded": true}`

### Step 2: Launch Web Interactive Demo App
```bash
python -m http.server 8080
```
Access `http://localhost:8080/` in browser.

### Step 3: Run Automated Tests
```bash
$env:PYTHONPATH="."; pytest backend/tests/test_api.py
```

### Step 4: Build & Deploy Android Application
- Open `android/` directory in Android Studio.
- Build and run on Android Emulator or physical device (API Level 26+).
- Grant **Notification Access** and **Phone/SMS permissions** when prompted.

---

## 🔒 7. Honest Android OS Limitations

1. **No Kernel-Level Interception:** Standard Android apps cannot silently mute external third-party apps before the OS plays sound. SmartNotify uses custom notification channels (`smartnotify_soft`, `smartnotify_important`) for controlled alerting.
2. **No Caller Device Control:** SmartNotify cannot draw UI or buttons on another person's phone. Triage relies on standard SMS communication.
3. **User Channel Authority:** If a user disables notifications in Android System Settings, the OS overrides app settings.

---

## 🔮 8. Future Enhancements

- On-device TensorFlow Lite / ONNX ML inference for offline zero-latency classification.
- User feedback loop to personalize notification priority over time.
- Contextual signals (Calendar event integration, GPS location context).

---

## 📂 9. Project Structure

```
SmartNotify/
├── android/               # Native Android App (Kotlin + Compose)
├── backend/               # FastAPI ML Service & Pytest Suites
├── ml/                    # Data Preprocessing, Training & Benchmark Scripts
├── models/                # Serialized SVM Pipeline Artifacts
├── docs/                  # Technical Docs (Datasets, Pipeline, Architecture, Demo)
├── final_project/         # Complete Report Package & Deliverables
├── index.html & app.js    # Interactive Web Demonstration Platform
└── README.md              # Master Documentation
```

---

*SmartNotify — Intelligent Notification Management System*
