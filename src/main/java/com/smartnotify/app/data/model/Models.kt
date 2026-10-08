package com.smartnotify.app.data.model

/**
 * Data models for SmartNotify application architecture (Phase 9 Caller Urgency Handling).
 */

enum class PriorityLevel {
    UNCLASSIFIED,
    LOW,
    MEDIUM,
    HIGH
}

enum class ActionTaken {
    PENDING_CLASSIFICATION,
    NORMAL_BEHAVIOR,
    SUPPRESSED_BY_SMARTNOTIFY,
    SOFT_ALERT,
    IMPORTANT_ALERT
}

enum class ProcessingStatus {
    PENDING,
    CLASSIFIED,
    FAILED
}

enum class RequestType {
    CALL_IMMEDIATELY,
    CALL_WHEN_FREE
}

enum class CallStatus {
    PENDING,
    NOTIFIED,
    COMPLETED,
    DISMISSED
}

data class CallRequest(
    val id: String,
    val callerName: String,
    val phoneNumber: String, // Masked in debug logs for privacy
    val timestamp: String,
    val requestType: RequestType,
    val status: CallStatus = CallStatus.PENDING
)

enum class UserFeedbackRating {
    CORRECT,
    INCORRECT
}

data class NotificationItem(
    val id: String,
    val appName: String,
    val packageName: String,
    val sender: String = "",
    val title: String,
    val messagePreview: String,
    val category: String = "General",
    val priority: PriorityLevel = PriorityLevel.UNCLASSIFIED,
    val confidence: Float = 0.0f,
    val modelName: String = "Support Vector Machine",
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val timestamp: String,
    val actionTaken: ActionTaken = ActionTaken.PENDING_CLASSIFICATION,
    val sessionId: String = "",
    val userFeedback: UserFeedbackRating? = null
)

data class FocusModeState(
    val enabled: Boolean = false,
    val activeSessionId: String = "",
    val hasNotificationAccess: Boolean = true,
    val hasPolicyAccess: Boolean = true,
    val policyAccessMissing: Boolean = false,
    val enableCallHandling: Boolean = true,
    val enableAutoSms: Boolean = true
)

data class AlertPreferences(
    val vibrate: Boolean = true,
    val screenOn: Boolean = true,
    val sound: Boolean = false,
    val ring: Boolean = false
)

data class AppStatistics(
    val totalAnalyzed: Int = 0,
    val classifiedCount: Int = 0,
    val failedCount: Int = 0,
    val unclassifiedCount: Int = 0,
    val lowCount: Int = 0,
    val mediumCount: Int = 0,
    val highCount: Int = 0,
    val suppressedCount: Int = 0,
    val softAlertCount: Int = 0,
    val importantAlertCount: Int = 0,
    val normalBehaviorCount: Int = 0,
    // Phase 9 Call Stats
    val totalCallRequests: Int = 0,
    val urgentCallRequests: Int = 0,
    val whenFreeCallRequests: Int = 0
)
