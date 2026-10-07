import time
import os
from typing import Optional
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, status
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

from backend.app.config import settings
from backend.app.schemas import (
    NotificationRequest, PredictionResponse,
    HealthResponse, ModelInfoResponse,
    CallRequestCreate, CallRequestUpdate, FeedbackRequest
)
from backend.app.predictor import predictor_instance
from backend.app.context_engine import context_engine_instance
from backend.app.utils import logger, log_prediction_privacy_safe
from backend.app import db


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Server Startup and Shutdown Event Handler."""
    logger.info("Initializing SmartNotify FastAPI Service & SQLite Database...")
    db.init_db()
    try:
        predictor_instance.load_artifacts()
    except Exception as e:
        logger.error(f"Startup warning: ML artifacts failed to load: {e}")
    yield
    logger.info("Shutting down SmartNotify FastAPI Service...")


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="ML-based notification priority classification service for SmartNotify mobile app.",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc"
)

# CORS Middleware Setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/", include_in_schema=False)
async def serve_index():
    return FileResponse(os.path.join(settings.BASE_DIR, "index.html"))


@app.get("/styles.css", include_in_schema=False)
async def serve_css():
    return FileResponse(os.path.join(settings.BASE_DIR, "styles.css"))


@app.get("/app.js", include_in_schema=False)
async def serve_js():
    return FileResponse(os.path.join(settings.BASE_DIR, "app.js"))


@app.get("/web_data.json", include_in_schema=False)
async def serve_web_data():
    return FileResponse(os.path.join(settings.BASE_DIR, "web_data.json"))


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint to verify backend and ML model readiness."""
    if not predictor_instance.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model pipeline is not loaded."
        )
    return HealthResponse(
        status="healthy",
        model_loaded=predictor_instance.is_loaded,
        version=settings.VERSION
    )


@app.get("/model-info", response_model=ModelInfoResponse, tags=["Model Info"])
async def get_model_info():
    """Returns non-sensitive Phase 3 evaluation metrics and model metadata."""
    if not predictor_instance.is_loaded:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Model metadata is unavailable."
        )
    meta = predictor_instance.metadata
    return ModelInfoResponse(
        model=meta.get("model_name", "Support Vector Machine"),
        classes=meta.get("classes", ["LOW", "MEDIUM", "HIGH"]),
        test_accuracy=meta.get("test_accuracy", 0.7142),
        test_macro_f1=meta.get("test_macro_f1", 0.6766),
        high_class_precision=meta.get("high_class_precision", 0.6926),
        high_class_recall=meta.get("high_class_recall", 0.6962),
        high_class_f1=meta.get("high_class_f1", 0.6944),
        training_samples=meta.get("training_samples", 8412),
        test_samples=meta.get("test_samples", 2103)
    )


@app.get("/dataset-samples", tags=["Dataset"])
async def get_dataset_samples():
    """Returns sample records from the Smartphone Notifications Dataset."""
    import os, json
    dataset_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "web_data.json")
    if os.path.exists(dataset_path):
        with open(dataset_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            return data.get("samples", [])
    return []


@app.get("/notifications", tags=["Database"])
async def get_notifications(limit: int = 50):
    """Fetches real notification history stored in SQLite Database."""
    return db.get_all_notifications(limit=limit)


@app.delete("/notifications", tags=["Database"])
async def clear_notifications():
    """Clears all notification history from SQLite Database."""
    db.clear_all_notifications()
    return {"message": "All notifications cleared from database"}


@app.get("/calls", tags=["Call Triage"])
async def get_call_requests():
    """Fetches caller triage requests from SQLite Database."""
    return db.get_all_call_requests()


@app.post("/calls", tags=["Call Triage"])
async def create_call_request(req: CallRequestCreate):
    """Saves a new caller urgency triage request to SQLite Database."""
    item = db.add_call_request(
        caller_name=req.caller_name,
        phone_number=req.phone_number,
        request_type=req.request_type
    )
    return item


@app.patch("/calls/{req_id}", tags=["Call Triage"])
async def update_call_request(req_id: int, update: CallRequestUpdate):
    """Updates status of a caller urgency request in SQLite Database."""
    success = db.update_call_request_status(req_id, update.status)
    if not success:
        raise HTTPException(status_code=404, detail="Call request not found")
    return {"success": True, "status": update.status}


@app.get("/stats", tags=["Database"])
async def get_stats():
    """Returns live aggregate statistics directly from SQLite Database."""
    return db.get_db_stats()


@app.post("/sessions/start", tags=["Focus Session"])
async def start_focus_session():
    """Starts a NEW Focus Mode Session, completing any previous session."""
    session = db.create_new_focus_session()
    return session


@app.post("/sessions/stop", tags=["Focus Session"])
async def stop_focus_session():
    """Stops the active Focus Mode Session and returns session summary."""
    session = db.complete_active_focus_session()
    return session or {"status": "NO_ACTIVE_SESSION"}


@app.get("/sessions/active", tags=["Focus Session"])
async def get_active_session():
    """Returns details of the currently active Focus Session."""
    active = db.get_active_focus_session()
    return active or {"status": "INACTIVE"}


@app.get("/sessions/summary", tags=["Focus Session"])
async def get_session_summary(session_id: Optional[str] = None):
    """
    Returns the Focus Session Summary (messages, calls, high/medium/low priority breakdown,
    and actual stored communications) for the active or specified session.
    """
    if not session_id:
        active = db.get_active_focus_session()
        if active:
            session_id = active['session_id']
        else:
            # Fallback to most recent session in DB
            conn = db.get_connection()
            cursor = conn.cursor()
            cursor.execute("SELECT session_id FROM focus_sessions ORDER BY start_time DESC LIMIT 1")
            row = cursor.fetchone()
            conn.close()
            session_id = row['session_id'] if row else None

    if not session_id:
        return {
            "session_id": None,
            "stats": {"messages": 0, "calls": 0, "high": 0, "medium": 0, "low": 0},
            "notifications": [],
            "calls": []
        }

    stats = db.get_session_summary_stats(session_id)
    notifications = db.get_session_notifications(session_id)
    calls = db.get_session_call_requests(session_id)

    return {
        "session_id": session_id,
        "stats": stats,
        "notifications": notifications,
        "calls": calls
    }


@app.post("/feedback", tags=["Personalization"])
async def submit_user_feedback(req: FeedbackRequest):
    """
    Saves optional user feedback ('IMPORTANT' / 'NOT_IMPORTANT') for a sender/app
    to personalize future priority decisions without forcing manual labeling.
    """
    target = (req.target_name or "").strip()
    if not target:
        raise HTTPException(status_code=400, detail="Target name required for user feedback")
    item = db.add_user_feedback(
        target_name=target,
        rating=req.rating,
        notification_id=req.notification_id
    )
    return {"status": "SUCCESS", "feedback": item}


@app.post("/predict", response_model=PredictionResponse, tags=["Inference"])
async def predict_priority(request: NotificationRequest, focus_mode: bool = False):
    """
    Predicts notification priority using Two-Stage Architecture:
    Stage 1: ML Model predicts content-based priority.
    Stage 2: Context & Personalization Layer evaluates intent, user feedback, & safe fallback.
    """
    title_str = (request.title or "").strip()
    message_str = (request.message or "").strip()
    
    if not title_str and not message_str:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Notification text is empty"
        )
        
    start_time = time.time()
    
    try:
        # Stage 1: ML Pipeline Prediction
        res = predictor_instance.predict(
            title=request.title,
            message=request.message,
            app=request.app,
            category=request.category
        )
        duration_ms = (time.time() - start_time) * 1000
        
        # Stage 2: Context & Personalization Layer Evaluation
        stage2 = context_engine_instance.evaluate_final_decision(
            ml_priority=res["priority"],
            confidence=res["confidence"],
            app_name=request.app or "App",
            title=title_str,
            message=message_str,
            sender=request.sender or "",
            is_focus_mode_on=focus_mode
        )
        
        final_priority = stage2["final_priority"]
        action_taken = stage2["action_taken"]

        log_prediction_privacy_safe(
            app_name=request.app,
            priority=final_priority,
            confidence=res["confidence"],
            duration_ms=duration_ms
        )

        # Persist record into SQLite DB
        db.save_notification(
            app_name=request.app or "App",
            title=title_str or "No Title",
            message=message_str or "No Text",
            priority=final_priority,
            confidence=float(res["confidence"]),
            model=res["model"],
            action_taken=action_taken,
            sender=request.sender or ""
        )
        
        return PredictionResponse(
            priority=final_priority,
            confidence=res["confidence"],
            model=res["model"],
            timestamp=res["timestamp"]
        )
    except RuntimeError as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Prediction internal error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal prediction error occurred."
        )




