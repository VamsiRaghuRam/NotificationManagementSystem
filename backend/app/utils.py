import re
import logging
from datetime import datetime

# Setup Privacy-Conscious Server Logger
logger = logging.getLogger("SmartNotifyBackend")
logger.setLevel(logging.INFO)

if not logger.handlers:
    handler = logging.StreamHandler()
    formatter = logging.Formatter("[%(asctime)s] [%(levelname)s] [SmartNotify] %(message)s")
    handler.setFormatter(formatter)
    logger.addHandler(handler)


def clean_text(text: str) -> str:
    """Preprocesses input text identically to Phase 2/Phase 3 pipeline."""
    text = str(text or "")
    text = re.sub(r'\s+', ' ', text).strip()
    return text


def log_prediction_privacy_safe(app_name: str, priority: str, confidence: float, duration_ms: float):
    """
    Privacy Requirement (Section 15):
    Logs inference metadata WITHOUT recording raw notification titles or message content.
    """
    logger.info(
        f"INFERENCE SUCCESS | App: '{app_name}' | Result: {priority} | "
        f"Confidence: {confidence:.2f} | Latency: {duration_ms:.2f}ms"
    )
