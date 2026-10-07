# SmartNotify FastAPI ML Backend

FastAPI REST inference backend for **SmartNotify – ML-Based Intelligent Notification Management System**. 
This backend loads the Phase 3 trained **Support Vector Machine (LinearSVC with Probability Calibration)** model pipeline (`models/smartnotify_pipeline.joblib`) ONCE at server startup and provides low-latency notification priority classification (`LOW`, `MEDIUM`, `HIGH`) for Android mobile client integration.

---

## 🚀 Quick Start

### 1. Installation
Ensure dependencies are installed:
```bash
pip install -r backend/requirements.txt
```

### 2. Run the Development Server
Start the Uvicorn server:
```bash
uvicorn backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive API documentation will be available at:
* **Swagger UI:** `http://localhost:8000/docs`
* **ReDoc:** `http://localhost:8000/redoc`

---

## 📡 API Contract & Endpoints

### 1. Health Check
* **Method & Path:** `GET /health`
* **Response (200 OK):**
```json
{
  "status": "healthy",
  "model_loaded": true,
  "version": "1.0.0"
}
```

### 2. Model Metadata
* **Method & Path:** `GET /model-info`
* **Response (200 OK):**
```json
{
  "model": "Support Vector Machine (LinearSVC with Probability Calibration)",
  "classes": ["LOW", "MEDIUM", "HIGH"],
  "test_accuracy": 0.7142,
  "test_macro_f1": 0.6766,
  "high_class_precision": 0.6926,
  "high_class_recall": 0.6962,
  "high_class_f1": 0.6944,
  "training_samples": 8412,
  "test_samples": 2103
}
```

### 3. Notification Priority Prediction
* **Method & Path:** `POST /predict`
* **Request Header:** `Content-Type: application/json`
* **Request Body:**
```json
{
  "title": "Emergency",
  "message": "Please call me immediately",
  "app": "WhatsApp",
  "category": "Personal"
}
```
* **Response (200 OK):**
```json
{
  "priority": "HIGH",
  "confidence": 0.96,
  "model": "Support Vector Machine (LinearSVC with Probability Calibration)",
  "timestamp": "2026-09-28T20:20:00Z"
}
```
* **Error Response (400 Bad Request):** *(When title and message are empty)*
```json
{
  "detail": "Notification text is empty"
}
```

---

## 🛡️ Privacy & Security Design
1. **Zero Text Logging:** In compliance with Section 15 privacy guidelines, notification titles and body text are **never written to log files**. Server logs track metadata only (`timestamp`, `app`, `priority`, `confidence`, `latency`).
2. **On-Premise Execution:** Operates entirely locally without sending data to third-party AI APIs.

---

## 🧪 Running Automated Tests
Run unit tests with Pytest:
```bash
pytest backend/tests/test_api.py -v
```
