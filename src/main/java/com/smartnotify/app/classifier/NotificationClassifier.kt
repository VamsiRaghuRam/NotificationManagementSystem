package com.smartnotify.app.classifier

import android.content.Context
import android.util.Log
import com.smartnotify.app.data.api.ApiClient
import com.smartnotify.app.data.api.PredictionApiRequest
import com.smartnotify.app.data.model.NotificationItem
import com.smartnotify.app.data.model.PriorityLevel
import com.smartnotify.app.data.model.ProcessingStatus
import com.smartnotify.app.data.repository.FocusRepository
import com.smartnotify.app.data.repository.NotificationRepository
import com.smartnotify.app.data.repository.PreferencesRepository
import com.smartnotify.app.decision.NotificationDecisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Asynchronous Classification & Alert Execution Worker for SmartNotify (Phase 8 Integration).
 * Handles API call to FastAPI backend POST /predict, passes priority to NotificationDecisionEngine,
 * executes supported Android alerts, and updates NotificationRepository.
 */
class NotificationClassifier(private val context: Context? = null) {

    companion object {
        private const val TAG = "SmartNotifyClassifier"
        private var instanceRef: NotificationClassifier? = null

        fun getInstance(context: Context? = null): NotificationClassifier {
            if (instanceRef == null) {
                instanceRef = NotificationClassifier(context?.applicationContext)
            }
            return instanceRef!!
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    fun classifyAsync(item: NotificationItem) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            var attempts = 0
            var success = false

            var finalPriority = PriorityLevel.UNCLASSIFIED
            var finalConfidence = 0.0f
            var finalModel = "Support Vector Machine"
            var finalStatus = ProcessingStatus.FAILED

            while (attempts < 2 && !success) {
                attempts++
                try {
                    val requestBody = PredictionApiRequest(
                        title = item.title,
                        message = item.messagePreview,
                        app = item.appName,
                        category = item.category
                    )

                    val response = ApiClient.api.predictNotification(requestBody)

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        finalPriority = parsePriority(body.priority)
                        finalConfidence = body.confidence
                        finalModel = body.model
                        finalStatus = ProcessingStatus.CLASSIFIED
                        success = true

                        val durationMs = System.currentTimeMillis() - startTime
                        Log.i(
                            TAG,
                            "ML PREDICTION SUCCESS | App: '${item.appName}' | Priority: ${body.priority} | " +
                                    "Confidence: ${(body.confidence * 100).toInt()}% | Latency: ${durationMs}ms"
                        )
                    } else {
                        Log.w(TAG, "API Error (Attempt $attempts): HTTP ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Backend network call failed (Attempt $attempts/2): ${e.message}")
                }
            }

            if (!success) {
                Log.e(TAG, "Classification FAILED for notification | App: '${item.appName}' | Setting status to FAILED")
                finalPriority = PriorityLevel.UNCLASSIFIED
                finalConfidence = 0.0f
                finalModel = "FastAPI Backend Offline"
                finalStatus = ProcessingStatus.FAILED
            }

            // Phase 8 Decision Engine Execution
            val isFocusActive = FocusRepository.instance.focusState.value.enabled
            val alertPrefs = PreferencesRepository.instance.prefs.value

            val decisionEngine = context?.let { NotificationDecisionEngine.getInstance(it) }
            val action = decisionEngine?.evaluateAction(finalPriority, isFocusActive, alertPrefs)
                ?: com.smartnotify.app.data.model.ActionTaken.NORMAL_BEHAVIOR

            // Execute Android notification alert action
            context?.let {
                decisionEngine?.executeAlertAction(item, action, alertPrefs)
            }

            // Update NotificationRepository with ML results and decision action
            NotificationRepository.instance.updateNotificationPrediction(
                id = item.id,
                priority = finalPriority,
                confidence = finalConfidence,
                modelName = finalModel,
                status = finalStatus,
                actionTaken = action
            )
        }
    }

    private fun parsePriority(priorityStr: String): PriorityLevel {
        return when (priorityStr.uppercase(Locale.ROOT)) {
            "LOW" -> PriorityLevel.LOW
            "MEDIUM" -> PriorityLevel.MEDIUM
            "HIGH" -> PriorityLevel.HIGH
            else -> PriorityLevel.UNCLASSIFIED
        }
    }
}
