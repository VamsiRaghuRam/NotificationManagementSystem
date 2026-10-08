package com.smartnotify.app.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionUtils {

    /**
     * Dynamically checks whether SmartNotify has been granted Notification Access by the user.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(packageName)
    }

    /**
     * Opens the Android System Settings screen where the user can grant Notification Access.
     */
    fun openNotificationAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Dynamically checks whether SmartNotify has Do Not Disturb / Notification Policy Access.
     */
    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the Android System Settings screen where the user can grant Notification Policy / DND Access.
     */
    fun openNotificationPolicySettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Phase 9 Requirement: Check SMS permissions dynamically.
     */
    fun hasSmsPermissions(context: Context): Boolean {
        val sendSms = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val receiveSms = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return sendSms && receiveSms
    }

    /**
     * Phase 9 Requirement: Check Phone State permissions dynamically.
     */
    fun hasPhonePermissions(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    const val SMS_PERMISSION_RATIONALE = "SmartNotify can send a message to callers explaining that you are currently busy and allowing them to request an urgent callback."
}
