import os


class Settings:
    PROJECT_NAME: str = "SmartNotify ML Inference Backend"
    VERSION: str = "1.0.0"
    API_PREFIX: str = ""
    
    # Path to Phase 3 serialized pipeline artifact
    BASE_DIR: str = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    PIPELINE_PATH: str = os.path.join(BASE_DIR, "models", "smartnotify_pipeline.joblib")
    METADATA_PATH: str = os.path.join(BASE_DIR, "models", "model_metadata.json")
    
    # Security & CORS Settings
    CORS_ORIGINS: list = ["*"]
    
    # Privacy & Logging Settings
    ENABLE_PRIVACY_LOGGING: bool = True  # Ensures raw title/message text is NEVER logged


settings = Settings()
