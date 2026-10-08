package com.smartnotify.app.service

import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smartnotify.app.classifier.NotificationClassifier
import com.smartnotify.app.data.model.ActionTaken
import com.smartnotify.app.data.model.NotificationItem
import com.smartnotify.app.data.model.PriorityLevel
import com.smartnotify.app.data.model.ProcessingStatus
import com.smartnotify.app.data.repository.NotificationRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Android NotificationListenerService for SmartNotify.
 * Captures real Android system notifications (including WhatsApp, SMS, Slack, Gmail, etc.),
 * logs metadata safely, stores notifications immediately in repository, and triggers async ML classification.
 */
class SmartNotifyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "SmartNotifyListener"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "SMARTNOTIFY_NOTIFICATION_LISTENER_CONNECTED")
        NotificationRepository.instance.setListenerConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "SMARTNOTIFY_NOTIFICATION_LISTENER_DISCONNECTED")
        NotificationRepository.instance.setListenerConnected(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return

        // 1. Noise & System Notification Filtering (Screen Recorder, System UI, Downloads, Self)
        val lowerPkg = pkgName.lowercase(Locale.ROOT)
        val isIgnoredPkg = lowerPkg == applicationContext.packageName ||
                lowerPkg == "com.smartnotify.app" ||
                lowerPkg == "com.android.systemui" ||
                lowerPkg == "com.android.providers.downloads" ||
                lowerPkg.contains("screenrecorder") ||
                lowerPkg.contains("screen_recorder")

        if (isIgnoredPkg || sbn.isOngoing) {
            return
        }

        // Authoritative Single Source of Truth Focus Mode check
        val focusState = com.smartnotify.app.data.repository.FocusRepository.instance.focusState.value
        if (!focusState.enabled) {
            Log.d(TAG, "FOCUS_OFF_IGNORED | package=$pkgName | Focus Mode is OFF, notification ignored")
            return
        }

        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // 1. Resolve Package Name to Human-Readable App Name
            val appName = try {
                val pm = applicationContext.packageManager
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkgName, 0)
                }
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkgName.split(".").lastOrNull()?.capitalize(Locale.ROOT) ?: pkgName
            }

            // 2. Comprehensive Notification Title & Text Extraction (WhatsApp, MessagingStyle, Grouped)
            val extractedTitle = extractTitle(extras)
            val extractedMessage = extractMessageText(extras)

            val title = extractedTitle.ifEmpty { appName }
            val message = extractedMessage.ifEmpty { "New Notification" }

            val lowerTitle = title.lowercase(Locale.ROOT)
            val lowerAppName = appName.lowercase(Locale.ROOT)
            if (lowerTitle.contains("screen recorder") || lowerTitle.contains("recording screen") || lowerAppName.contains("screen recorder")) {
                return
            }

            val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sbn.postTime))

            // 3. Section 4 Requirement: Safe Metadata Logging (NO raw message content in production logs)
            Log.i(
                TAG,
                "NOTIFICATION_RECEIVED | package=$pkgName | hasTitle=${extractedTitle.isNotEmpty()} | " +
                        "hasText=${extractedMessage.isNotEmpty()} | key=${sbn.key} | time=$formattedTime"
            )

            val item = NotificationItem(
                id = sbn.key ?: UUID.randomUUID().toString(),
                appName = appName,
                packageName = pkgName,
                title = title,
                messagePreview = message,
                category = getCategoryString(notification),
                priority = PriorityLevel.UNCLASSIFIED,
                confidence = 0.0f,
                processingStatus = ProcessingStatus.PENDING,
                timestamp = formattedTime,
                actionTaken = ActionTaken.PENDING_CLASSIFICATION,
                sessionId = focusState.activeSessionId
            )

            // 4. Section 13 Requirement: Immediate & Unconditional Repository Storage
            NotificationRepository.instance.addNotification(item)
            Log.d(TAG, "NOTIFICATION_STORED | key=${item.id} | app=$appName")

            // 5. Asynchronous ML classification trigger
            NotificationClassifier.getInstance(applicationContext).classifyAsync(item)

        } catch (e: Exception) {
            Log.e(TAG, "NOTIFICATION_STORE_FAILED | error=${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        Log.d(TAG, "Notification removed | package=${sbn.packageName}")
    }

    /**
     * Extracts title from Notification extras (handles MessagingStyle, Conversation Title, Big Title).
     */
    private fun extractTitle(extras: Bundle): String {
        val title = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
        return title?.toString()?.trim() ?: ""
    }

    /**
     * Robust message text extraction supporting WhatsApp, MessagingStyle, BigText, SummaryText, and TextLines.
     */
    private fun extractMessageText(extras: Bundle): String {
        // Option A: Direct text or big text
        val directText = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)

        if (!directText.isNullOrBlank()) {
            return directText.toString().trim()
        }

        // Option B: MessagingStyle messages (EXTRA_MESSAGES array of Bundles or Parcelables)
        val messagesParcelable = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (!messagesParcelable.isNullOrEmpty()) {
            val lastMessage = messagesParcelable.lastOrNull()
            if (lastMessage is Bundle) {
                val text = lastMessage.getCharSequence("text")
                if (!text.isNullOrBlank()) return text.toString().trim()
            }
        }

        // Option C: Text lines array (Grouped WhatsApp notifications)
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!lines.isNullOrEmpty()) {
            return lines.lastOrNull()?.toString()?.trim()
                ?: lines.joinToString(" ") { it.toString().trim() }
        }

        return ""
    }

    private fun getCategoryString(n: Notification): String {
        return when (n.category) {
            Notification.CATEGORY_MESSAGE -> "Personal"
            Notification.CATEGORY_EMAIL -> "Work"
            Notification.CATEGORY_PROMO -> "Promotions"
            Notification.CATEGORY_EVENT -> "Calendar"
            Notification.CATEGORY_CALL -> "Call"
            Notification.CATEGORY_ALARM -> "Alerts"
            else -> "General"
        }
    }
}
