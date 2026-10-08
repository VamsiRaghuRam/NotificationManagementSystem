package com.smartnotify.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.smartnotify.app.data.api.ApiClient
import com.smartnotify.app.data.api.ModelInfoResponse
import com.smartnotify.app.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class NotificationRepository {

    companion object {
        val instance: NotificationRepository by lazy { NotificationRepository() }
    }

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String>("")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _backendConnected = MutableStateFlow(false)
    val backendConnected: StateFlow<Boolean> = _backendConnected.asStateFlow()

    private val _modelInfo = MutableStateFlow<ModelInfoResponse?>(null)
    val modelInfo: StateFlow<ModelInfoResponse?> = _modelInfo.asStateFlow()

    // Diagnostic & Telemetry Counters
    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    private val _capturedCount = MutableStateFlow(0)
    val capturedCount: StateFlow<Int> = _capturedCount.asStateFlow()

    private val _storedCount = MutableStateFlow(0)
    val storedCount: StateFlow<Int> = _storedCount.asStateFlow()

    private val _mlProcessedCount = MutableStateFlow(0)
    val mlProcessedCount: StateFlow<Int> = _mlProcessedCount.asStateFlow()

    private val _mlFailedCount = MutableStateFlow(0)
    val mlFailedCount: StateFlow<Int> = _mlFailedCount.asStateFlow()

    private val _lastCapturedApp = MutableStateFlow<String?>(null)
    val lastCapturedApp: StateFlow<String?> = _lastCapturedApp.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setListenerConnected(connected: Boolean) {
        _listenerConnected.value = connected
    }

    fun startNewSession(sessionId: String) {
        _activeSessionId.value = sessionId
        _capturedCount.value = 0
        _mlProcessedCount.value = 0
        _mlFailedCount.value = 0
    }

    fun addNotification(item: NotificationItem) {
        val updated = listOf(item) + _notifications.value.filterNot { it.id == item.id }
        _notifications.value = updated
        _capturedCount.value = _capturedCount.value + 1
        _storedCount.value = _storedCount.value + 1
        _lastCapturedApp.value = item.appName
    }

    fun updateNotificationPrediction(
        id: String,
        priority: PriorityLevel,
        confidence: Float,
        modelName: String,
        status: ProcessingStatus,
        actionTaken: ActionTaken
    ) {
        if (status == ProcessingStatus.CLASSIFIED) {
            _mlProcessedCount.value = _mlProcessedCount.value + 1
        } else if (status == ProcessingStatus.FAILED) {
            _mlFailedCount.value = _mlFailedCount.value + 1
        }
        val updatedList = _notifications.value.map { item ->
            if (item.id == id) {
                item.copy(
                    priority = priority,
                    confidence = confidence,
                    modelName = modelName,
                    processingStatus = status,
                    actionTaken = actionTaken
                )
            } else {
                item
            }
        }
        _notifications.value = updatedList
    }

    fun submitUserFeedback(notificationId: String, rating: UserFeedbackRating) {
        val targetItem = _notifications.value.find { it.id == notificationId }
        val updatedList = _notifications.value.map { item ->
            if (item.id == notificationId) {
                item.copy(userFeedback = rating)
            } else {
                item
            }
        }
        _notifications.value = updatedList

        if (targetItem != null) {
            scope.launch {
                try {
                    val req = com.smartnotify.app.data.api.FeedbackApiRequest(
                        targetName = targetItem.title.ifBlank { targetItem.appName },
                        rating = rating.name,
                        notificationId = notificationId.hashCode()
                    )
                    ApiClient.api.submitFeedback(req)
                } catch (e: Exception) {
                    // Feedback endpoint error handled gracefully
                }
            }
        }
    }

    fun clearHistory() {
        _notifications.value = emptyList()
    }

    suspend fun checkBackendHealth() {
        try {
            val response = ApiClient.api.checkHealth()
            if (response.isSuccessful && response.body()?.status == "healthy") {
                _backendConnected.value = true
                fetchModelInfo()
            } else {
                _backendConnected.value = false
            }
        } catch (e: Exception) {
            _backendConnected.value = false
        }
    }

    private suspend fun fetchModelInfo() {
        try {
            val response = ApiClient.api.getModelInfo()
            if (response.isSuccessful && response.body() != null) {
                _modelInfo.value = response.body()
            }
        } catch (e: Exception) {
            // Non-critical metadata check error
        }
    }

    fun getStatistics(): AppStatistics {
        val current = _notifications.value
        val classified = current.count { it.processingStatus == ProcessingStatus.CLASSIFIED }
        val failed = current.count { it.processingStatus == ProcessingStatus.FAILED }
        val unclassified = current.count { it.priority == PriorityLevel.UNCLASSIFIED }
        val low = current.count { it.priority == PriorityLevel.LOW }
        val med = current.count { it.priority == PriorityLevel.MEDIUM }
        val high = current.count { it.priority == PriorityLevel.HIGH }

        val suppressed = current.count { it.actionTaken == ActionTaken.SUPPRESSED_BY_SMARTNOTIFY }
        val soft = current.count { it.actionTaken == ActionTaken.SOFT_ALERT }
        val important = current.count { it.actionTaken == ActionTaken.IMPORTANT_ALERT }
        val normal = current.count { it.actionTaken == ActionTaken.NORMAL_BEHAVIOR }

        val callRequests = CallRequestRepository.instance.callRequests.value

        return AppStatistics(
            totalAnalyzed = current.size,
            classifiedCount = classified,
            failedCount = failed,
            unclassifiedCount = unclassified,
            lowCount = low,
            mediumCount = med,
            highCount = high,
            suppressedCount = suppressed,
            softAlertCount = soft,
            importantAlertCount = important,
            normalBehaviorCount = normal,
            totalCallRequests = callRequests.size,
            urgentCallRequests = callRequests.count { it.requestType == RequestType.CALL_IMMEDIATELY },
            whenFreeCallRequests = callRequests.count { it.requestType == RequestType.CALL_WHEN_FREE }
        )
    }
}

/**
 * Repository for Phase 9 Caller Urgency Requests.
 */
class CallRequestRepository {

    companion object {
        val instance: CallRequestRepository by lazy { CallRequestRepository() }
    }

    private val _callRequests = MutableStateFlow<List<CallRequest>>(emptyList())
    val callRequests: StateFlow<List<CallRequest>> = _callRequests.asStateFlow()

    fun addCallRequest(request: CallRequest) {
        val existing = _callRequests.value.find {
            it.phoneNumber == request.phoneNumber && it.requestType == request.requestType && it.status == CallStatus.PENDING
        }
        if (existing != null) return

        _callRequests.value = listOf(request) + _callRequests.value
    }

    fun dismissRequest(id: String) {
        _callRequests.value = _callRequests.value.map {
            if (it.id == id) it.copy(status = CallStatus.DISMISSED) else it
        }
    }

    fun markCompleted(id: String) {
        _callRequests.value = _callRequests.value.map {
            if (it.id == id) it.copy(status = CallStatus.COMPLETED) else it
        }
    }

    fun dialCaller(context: Context, phoneNumber: String, requestId: String) {
        markCompleted(requestId)
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle devices without phone dialers
        }
    }
}

class FocusRepository {

    companion object {
        val instance: FocusRepository by lazy { FocusRepository() }
    }

    private val _focusState = MutableStateFlow(FocusModeState())
    val focusState: StateFlow<FocusModeState> = _focusState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun checkPermissions(context: Context) {
        val hasNotif = com.smartnotify.app.utils.PermissionUtils.isNotificationAccessGranted(context)
        val hasPolicy = com.smartnotify.app.utils.PermissionUtils.isNotificationPolicyAccessGranted(context)
        _focusState.value = _focusState.value.copy(
            hasNotificationAccess = hasNotif,
            hasPolicyAccess = hasPolicy,
            policyAccessMissing = !hasPolicy && _focusState.value.policyAccessMissing
        )
    }

    /**
     * Toggles Focus Mode state while controlling real Android Notification Policy (DND)
     * and managing Focus Session lifecycle.
     */
    fun toggleFocusMode(context: Context): Boolean {
        val isCurrentlyEnabled = _focusState.value.enabled

        if (!isCurrentlyEnabled) {
            // Check Notification Policy / DND permission
            val hasPolicyAccess = com.smartnotify.app.utils.PermissionUtils.isNotificationPolicyAccessGranted(context)
            if (!hasPolicyAccess) {
                // Cannot enable Focus Mode without required Android permission
                _focusState.value = _focusState.value.copy(
                    enabled = false,
                    hasPolicyAccess = false,
                    policyAccessMissing = true
                )
                return false
            }

            // Grant Focus Mode ON: Activate Android DND (Priority Interruption Filter)
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } catch (e: Exception) {
                android.util.Log.e("FocusRepository", "Failed to set DND filter: ${e.message}")
            }

            // Create NEW Focus Session ID
            val newSessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            _focusState.value = _focusState.value.copy(
                enabled = true,
                activeSessionId = newSessionId,
                hasPolicyAccess = true,
                policyAccessMissing = false
            )

            // Reset repository session isolation
            NotificationRepository.instance.startNewSession(newSessionId)

            // Notify backend of session start
            scope.launch {
                try {
                    ApiClient.api.startSession()
                } catch (e: Exception) {
                    // Non-critical backend call log
                }
            }
            return true
        } else {
            // Turn Focus Mode OFF: Restore normal Android notification interruption
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            } catch (e: Exception) {
                android.util.Log.e("FocusRepository", "Failed to restore notification filter: ${e.message}")
            }

            _focusState.value = _focusState.value.copy(
                enabled = false,
                policyAccessMissing = false
            )

            // Notify backend of session stop
            scope.launch {
                try {
                    ApiClient.api.stopSession()
                } catch (e: Exception) {
                    // Non-critical backend call log
                }
            }
            return true
        }
    }

    fun updateCallOptions(enableCallHandling: Boolean, enableAutoSms: Boolean) {
        _focusState.value = _focusState.value.copy(
            enableCallHandling = enableCallHandling,
            enableAutoSms = enableAutoSms
        )
    }
}

class PreferencesRepository {

    companion object {
        val instance: PreferencesRepository by lazy { PreferencesRepository() }
    }

    private val _prefs = MutableStateFlow(AlertPreferences())
    val prefs: StateFlow<AlertPreferences> = _prefs.asStateFlow()

    fun updatePreferences(vibrate: Boolean, screenOn: Boolean, sound: Boolean, ring: Boolean) {
        _prefs.value = AlertPreferences(vibrate, screenOn, sound, ring)
    }
}
