from typing import List, Optional
from pydantic import BaseModel, Field


class NotificationRequest(BaseModel):
    title: Optional[str] = Field(default="", description="Title of the incoming notification", json_schema_extra={"example": "Emergency Alert"})
    message: Optional[str] = Field(default="", description="Body/message content of the notification", json_schema_extra={"example": "Server PROD DOWN - Payment gateway offline!"})
    app: Optional[str] = Field(default="App", description="Application source or package name", json_schema_extra={"example": "WhatsApp"})
    sender: Optional[str] = Field(default="", description="Sender name or source person if available", json_schema_extra={"example": "Rahul"})
    category: Optional[str] = Field(default="General", description="System category/folder", json_schema_extra={"example": "Personal"})


class PredictionResponse(BaseModel):
    priority: str = Field(description="Predicted notification priority level (LOW, MEDIUM, HIGH)", json_schema_extra={"example": "HIGH"})
    confidence: float = Field(description="Calibrated probability/confidence score between 0.0 and 1.0", json_schema_extra={"example": 0.96})
    model: str = Field(description="Name of the deployed ML model", json_schema_extra={"example": "Support Vector Machine (LinearSVC)"})
    timestamp: str = Field(description="ISO timestamp of inference execution", json_schema_extra={"example": "2026-09-28T20:20:00Z"})


class HealthResponse(BaseModel):
    status: str = Field(json_schema_extra={"example": "healthy"})
    model_loaded: bool = Field(json_schema_extra={"example": True})
    version: str = Field(json_schema_extra={"example": "1.0.0"})


class ModelInfoResponse(BaseModel):
    model: str = Field(json_schema_extra={"example": "Support Vector Machine (LinearSVC with Probability Calibration)"})
    classes: List[str] = Field(json_schema_extra={"example": ["LOW", "MEDIUM", "HIGH"]})
    test_accuracy: float = Field(json_schema_extra={"example": 0.7142})
    test_macro_f1: float = Field(json_schema_extra={"example": 0.6766})
    high_class_precision: float = Field(json_schema_extra={"example": 0.6926})
    high_class_recall: float = Field(json_schema_extra={"example": 0.6962})
    high_class_f1: float = Field(json_schema_extra={"example": 0.6944})
    training_samples: int = Field(json_schema_extra={"example": 8412})
    test_samples: int = Field(json_schema_extra={"example": 2103})


class CallRequestCreate(BaseModel):
    caller_name: str = Field(description="Name or ID of incoming caller", json_schema_extra={"example": "John"})
    phone_number: str = Field(description="Caller phone number", json_schema_extra={"example": "+15550199"})
    request_type: str = Field(description="Triage option: CALL_IMMEDIATELY or CALL_WHEN_FREE", json_schema_extra={"example": "CALL_IMMEDIATELY"})


class CallRequestUpdate(BaseModel):
    status: str = Field(description="New status: COMPLETED or DISMISSED", json_schema_extra={"example": "COMPLETED"})


class FeedbackRequest(BaseModel):
    target_name: str = Field(description="Sender name or App name receiving feedback", json_schema_extra={"example": "Mother"})
    rating: str = Field(description="Rating string: IMPORTANT or NOT_IMPORTANT", json_schema_extra={"example": "NOT_IMPORTANT"})
    notification_id: Optional[int] = Field(default=None, description="Optional ID of notification rated")


