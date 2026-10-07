import os
import json
import joblib
from datetime import datetime, timezone
from backend.app.config import settings
from backend.app.utils import clean_text, logger


class SmartNotifyPredictor:
    """
    Inference Service for SmartNotify ML Pipeline.
    Loads Phase 3 model ONCE on server startup to maintain low-latency scoring.
    """
    def __init__(self):
        self.pipeline = None
        self.metadata = {}
        self.is_loaded = False
        self.model_name = "Support Vector Machine"
        
    def load_artifacts(self):
        """Loads serialized scikit-learn Pipeline and Metadata JSON."""
        try:
            pipeline_path = settings.PIPELINE_PATH
            metadata_path = settings.METADATA_PATH
            
            if not os.path.exists(pipeline_path):
                raise FileNotFoundError(f"Model pipeline artifact not found at: {pipeline_path}")
                
            logger.info(f"Loading Phase 3 ML Model Pipeline from {pipeline_path}...")
            self.pipeline = joblib.load(pipeline_path)
            
            if os.path.exists(metadata_path):
                with open(metadata_path, 'r', encoding='utf-8') as f:
                    self.metadata = json.load(f)
                    self.model_name = self.metadata.get('model_name', self.model_name)
                    
            self.is_loaded = True
            logger.info(f"Successfully loaded '{self.model_name}' pipeline into memory.")
        except Exception as e:
            self.is_loaded = False
            logger.error(f"Failed to load ML artifacts: {str(e)}")
            raise e

    def predict(self, title: str, message: str, app: str = "App", category: str = "General") -> dict:
        """
        Runs ML prediction using exact Phase 3 combined_text feature representation.
        """
        if not self.is_loaded or self.pipeline is None:
            raise RuntimeError("ML model pipeline is not loaded.")
            
        title_clean = clean_text(title)
        body_clean = clean_text(message)
        app_clean = clean_text(app)
        folder_clean = clean_text(category)
        
        # Combined text format matching Phase 2/Phase 3 training data
        combined_text = f"{app_clean} {title_clean} {body_clean} {folder_clean}".strip()
        
        # Model Prediction
        prediction_array = self.pipeline.predict([combined_text])
        priority_predicted = str(prediction_array[0])
        
        # Calculate Confidence Score (Probability estimate)
        confidence = 0.90
        if hasattr(self.pipeline, "predict_proba"):
            probs = self.pipeline.predict_proba([combined_text])[0]
            classes = list(self.pipeline.classes_)
            if priority_predicted in classes:
                idx = classes.index(priority_predicted)
                confidence = float(probs[idx])
                
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
        
        return {
            "priority": priority_predicted,
            "confidence": round(confidence, 4),
            "model": self.model_name,
            "timestamp": timestamp
        }


# Singleton Predictor Instance
predictor_instance = SmartNotifyPredictor()
