import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.predictor import predictor_instance

# Load artifacts for tests
predictor_instance.load_artifacts()


def test_health_endpoint():
    with TestClient(app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        assert data["model_loaded"] is True
        assert "version" in data


def test_model_info_endpoint():
    with TestClient(app) as client:
        response = client.get("/model-info")
        assert response.status_code == 200
        data = response.json()
        assert "model" in data
        assert "classes" in data
        assert data["classes"] == ["LOW", "MEDIUM", "HIGH"]
        assert "test_accuracy" in data
        assert "high_class_f1" in data


def test_predict_high_urgency():
    with TestClient(app) as client:
        payload = {
            "title": "PROD DOWN",
            "message": "Payment gateway offline! All transactions failing.",
            "app": "Slack",
            "category": "Work"
        }
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["priority"] in ["LOW", "MEDIUM", "HIGH"]
        assert 0.0 <= data["confidence"] <= 1.0
        assert "model" in data
        assert "timestamp" in data


def test_predict_low_urgency():
    with TestClient(app) as client:
        payload = {
            "title": "Exclusive Sale",
            "message": "Get 40% off your next purchase with code SALE40.",
            "app": "Target",
            "category": "Promotions"
        }
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["priority"] in ["LOW", "MEDIUM", "HIGH"]
        assert 0.0 <= data["confidence"] <= 1.0


def test_predict_empty_text():
    with TestClient(app) as client:
        payload = {
            "title": "",
            "message": "   ",
            "app": "WhatsApp",
            "category": "Personal"
        }
        response = client.post("/predict", json=payload)
        assert response.status_code == 400
        data = response.json()
        assert data["detail"] == "Notification text is empty"


def test_predict_missing_optional_fields():
    with TestClient(app) as client:
        payload = {
            "message": "Meeting in 10 minutes"
        }
        response = client.post("/predict", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["priority"] in ["LOW", "MEDIUM", "HIGH"]
