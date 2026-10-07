import re
from typing import Dict, Any, Optional
from backend.app import db
from backend.app.utils import logger

class ContextPersonalizationEngine:
    """
    SmartNotify Stage 2 Context & Personalization Decision Layer.
    
    Architecture:
    Stage 1: Trained ML Pipeline (Content Priority: LOW/MEDIUM/HIGH + Calibrated Probabilities)
    Stage 2: Context & Personalization Layer (Intent Analysis, User Feedback History, Uncertainty Fallback)
    Stage 3: Interruption Decision Engine (Final Action: EMERGENCY_OVERRIDE / MUTED / SUPPRESSED)
    """

    # Non-Urgent Intent Modifiers (Phrases indicating no immediate response requested)
    NON_URGENT_PATTERNS = [
        r"\bwhen\s+(you\s+are\s+)?free\b",
        r"\bwhen\s+you\s+have\s+time\b",
        r"\bwhen\s+you\s+can\b",
        r"\bwhen\s+convenient\b",
        r"\bno\s+rush\b",
        r"\bwhenever\b",
        r"\blater\b",
        r"\bcasual\b",
        r"\bjust\s+saying\s+hi\b",
        r"\bgood\s+morning\b",
        r"\bgood\s+night\b"
    ]

    # Emergency Intent Modifiers (Phrases indicating explicit urgency)
    EMERGENCY_PATTERNS = [
        r"\bimmediately\b",
        r"\bemergency\b",
        r"\bcritical\b",
        r"\burgent\b",
        r"\basap\b",
        r"\bserver\s+down\b",
        r"\botp\b",
        r"\bsecurity\s+alert\b",
        r"\bcode\s+red\b"
    ]

    def evaluate_final_decision(
        self,
        ml_priority: str,
        confidence: float,
        app_name: str,
        title: str,
        message: str,
        sender: str = "",
        is_focus_mode_on: bool = False
    ) -> Dict[str, Any]:
        """
        Evaluates Stage 1 ML prediction through Stage 2 Contextual & Personalization Rules.
        Returns final priority, final action taken, and contextual reasoning metadata.
        """
        combined_text = f"{title} {message}".lower()
        final_priority = ml_priority
        action_taken = "NORMAL_DELIVERY"
        context_modifier_applied = None

        # 1. Intent Context Analysis
        is_non_urgent = any(re.search(pat, combined_text) for pat in self.NON_URGENT_PATTERNS)
        is_explicit_emergency = any(re.search(pat, combined_text) for pat in self.EMERGENCY_PATTERNS)

        # 2. Intent Adjustment Logic
        if is_non_urgent and not is_explicit_emergency:
            # "Call me when free" -> Adjust from HIGH to LOW / NON-INTERRUPTIVE
            if final_priority == "HIGH":
                final_priority = "LOW"
                context_modifier_applied = "NON_URGENT_INTENT_DETECTED"
                logger.info(f"[Stage 2 Context Engine] Demoted HIGH to LOW for phrase 'when free/no rush' | Title: '{title}'")
        elif is_explicit_emergency:
            if final_priority == "LOW":
                final_priority = "HIGH"
                context_modifier_applied = "EXPLICIT_EMERGENCY_INTENT_DETECTED"

        # 3. User Personalization Signal Check (SQLite user_feedback table)
        user_feedback_signal = self._get_user_feedback_signal(sender, app_name)
        if user_feedback_signal == "NOT_IMPORTANT" and final_priority == "HIGH" and not is_explicit_emergency:
            final_priority = "MEDIUM"
            context_modifier_applied = "USER_FEEDBACK_PERSONALIZATION_DEMOTION"
            logger.info(f"[Stage 2 Context Engine] Demoted HIGH to MEDIUM based on user feedback history for sender/app: '{sender or app_name}'")

        # 4. Uncertainty & Safe Fallback Thresholding
        if confidence < 0.45 and final_priority == "HIGH" and not is_explicit_emergency:
            final_priority = "MEDIUM"
            context_modifier_applied = "LOW_CONFIDENCE_UNCERTAINTY_FALLBACK"

        # 5. Final Interruption Decision Mapping
        if is_focus_mode_on:
            if final_priority == "HIGH":
                action_taken = "EMERGENCY_OVERRIDE"
            elif final_priority == "MEDIUM":
                action_taken = "MUTED_MEDIUM"
            else:
                action_taken = "SUPPRESSED"
        else:
            action_taken = "NORMAL_DELIVERY"

        return {
            "ml_priority": ml_priority,
            "final_priority": final_priority,
            "confidence": confidence,
            "action_taken": action_taken,
            "context_modifier": context_modifier_applied,
            "is_non_urgent_intent": is_non_urgent,
            "is_explicit_emergency": is_explicit_emergency,
            "user_feedback_signal": user_feedback_signal
        }

    def _get_user_feedback_signal(self, sender: str, app_name: str) -> Optional[str]:
        """Queries user feedback history for sender or application."""
        try:
            conn = db.get_connection()
            cursor = conn.cursor()
            target = sender if sender else app_name
            cursor.execute("""
                SELECT rating, COUNT(*) as cnt FROM user_feedback 
                WHERE target_name = ? 
                GROUP BY rating ORDER BY cnt DESC LIMIT 1
            """, (target,))
            row = cursor.fetchone()
            conn.close()
            return row['rating'] if row else None
        except Exception:
            return None

# Singleton Context Engine Instance
context_engine_instance = ContextPersonalizationEngine()
