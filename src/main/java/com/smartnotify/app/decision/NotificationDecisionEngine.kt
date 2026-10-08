package com.smartnotify.app.decision

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartnotify.app.data.model.*

/**
 * SmartNotify Decision Engine (Phase 8).
 * Evaluates ML Priority + Focus Mode State + User Alert Preferences
 * to determine the official Android notification action and trigger appropriate alerts.
 */
class NotificationDecisionEngine(private val context: Context) {

    companion object {
        private const val TAG = "SmartNotifyDecision"
        
        const val CHANNEL_SOFT_ID = "smartnotify_soft"
        const val CHANNEL_IMPORTANT_ID = "smartnotify_important"

        private var instance: NotificationDecisionEngine? = null

        fun getInstance(context: Context): NotificationDecisionEngine {
            if (instance == null) {
                instance = NotificationDecisionEngine(context.applicationContext)
            }
            return instance!!
        }
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Section 12 & 13 Requirement: Create official Android Notification Channels.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Soft Alert Channel (Low Importance, Silent)
            val softChannel = NotificationChannel(
                CHANNEL_SOFT_ID,
                "SmartNotify Soft Alerts (Medium Priority)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Non-intrusive alerts during Focus Mode for medium-priority notifications."
                setSound(null, null)
                enableVibration(false)
            }

            // Channel 2: Important Alert Channel (High Importance, Ring/Vibrate, Bypass DND)
            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT_ID,
                "SmartNotify Important Alerts (High Priority)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Priority alerts that override Focus Mode for urgent notifications."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setBypassDnd(true)
            }

            notificationManager.createNotificationChannel(softChannel)
            notificationManager.createNotificationChannel(importantChannel)
            Log.i(TAG, "Notification Channels created: '$CHANNEL_SOFT_ID', '$CHANNEL_IMPORTANT_ID'")
        }
    }

    /**
     * Section 6 & 7 Requirement: Decision Table Evaluation.
     */
    fun evaluateAction(
        priority: PriorityLevel,
        isFocusActive: Boolean,
        prefs: AlertPreferences
    ): ActionTaken {
        // FOCUS MODE OFF: Normal native Android notification behavior
        if (!isFocusActive) {
            return ActionTaken.NORMAL_BEHAVIOR
        }

        // FOCUS MODE ON: Intelligent Priority Decision Matrix
        return when (priority) {
            PriorityLevel.LOW -> ActionTaken.SUPPRESSED_BY_SMARTNOTIFY
            PriorityLevel.MEDIUM -> ActionTaken.SOFT_ALERT
            PriorityLevel.HIGH -> ActionTaken.IMPORTANT_ALERT
            PriorityLevel.UNCLASSIFIED -> ActionTaken.SOFT_ALERT // Safe fallback (Section 19)
        }
    }

    /**
     * Section 9 & 10 Requirement: Execute Supported Android Alerts.
     */
    fun executeAlertAction(
        item: NotificationItem,
        action: ActionTaken,
        prefs: AlertPreferences
    ) {
        when (action) {
            ActionTaken.NORMAL_BEHAVIOR -> {
                Log.d(TAG, "Focus Mode OFF: No extra SmartNotify alert issued for '${item.appName}'")
            }
            ActionTaken.SUPPRESSED_BY_SMARTNOTIFY -> {
                // Section 8 Requirement: Low Priority suppressed during Focus Mode (No extra alert banner issued)
                Log.i(TAG, "Action: SUPPRESSED_BY_SMARTNOTIFY | App: '${item.appName}' | Focus Mode protected user")
            }
            ActionTaken.SOFT_ALERT -> {
                // Section 9 Requirement: Issue low-intrusion soft notification
                triggerNotificationBanner(
                    item = item,
                    channelId = CHANNEL_SOFT_ID,
                    titlePrefix = "[Medium Priority] ",
                    enableVibrate = false,
                    enableSound = false
                )
                Log.i(TAG, "Action: SOFT_ALERT | App: '${item.appName}' | Issued low-intrusion banner")
            }
            ActionTaken.IMPORTANT_ALERT -> {
                // Section 10 Requirement: Issue high-priority alert according to user preferences
                triggerNotificationBanner(
                    item = item,
                    channelId = CHANNEL_IMPORTANT_ID,
                    titlePrefix = "🚨 [URGENT ALERT] ",
                    enableVibrate = prefs.vibrate,
                    enableSound = prefs.sound || prefs.ring
                )
                Log.i(TAG, "Action: IMPORTANT_ALERT | App: '${item.appName}' | Triggered Focus Mode override alert (sound=${prefs.sound || prefs.ring}, vibrate=${prefs.vibrate})")
            }
            else -> {}
        }
    }

    private fun triggerNotificationBanner(
        item: NotificationItem,
        channelId: String,
        titlePrefix: String,
        enableVibrate: Boolean,
        enableSound: Boolean
    ) {
        val notifId = item.id.hashCode()

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$titlePrefix${item.appName}")
            .setContentText("${item.title}: ${item.messagePreview}")
            .setPriority(if (channelId == CHANNEL_IMPORTANT_ID) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setCategory(if (channelId == CHANNEL_IMPORTANT_ID) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        if (enableVibrate) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        if (enableSound) {
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(defaultSoundUri)
        } else {
            builder.setSound(null)
        }

        notificationManager.notify(notifId, builder.build())

        // Explicit direct vibration/ringtone fallback if system channel settings are cached
        if (enableVibrate) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 300, 150, 300), -1)
                    }
                }
            } catch (e: Exception) {
                // Vibration fallback error ignored
            }
        }

        if (enableSound) {
            try {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
            } catch (e: Exception) {
                // Sound fallback error ignored
            }
        }
    }

    /**
     * Phase 9 Requirement: Trigger High-Priority Alert for Urgent Call Requests (SMARTNOTIFY 1).
     */
    fun triggerUrgentCallAlert(callerName: String, prefs: AlertPreferences) {
        val notifId = ("call_urgent_" + System.currentTimeMillis()).hashCode()

        val builder = NotificationCompat.Builder(context, CHANNEL_IMPORTANT_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("🚨 URGENT CALL REQUEST")
            .setContentText("$callerName requested: Call me immediately")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (prefs.vibrate) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        if (prefs.sound || prefs.ring) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        }

        notificationManager.notify(notifId, builder.build())
        Log.i(TAG, "Triggered Urgent Call Alert notification for caller: $callerName")
    }
}
