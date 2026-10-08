package com.smartnotify.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.smartnotify.app.data.repository.FocusRepository
import com.smartnotify.app.utils.PermissionUtils

/**
 * Phase 9 Caller Urgency Handling - Call State Broadcast Receiver.
 * Listens for incoming phone state changes using standard Android APIs.
 * Privacy Rule: Phone numbers are masked in logs and NEVER sent to ML/external APIs.
 */
class SmartNotifyCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmartNotifyCallRecv"
        private val lastSentTimestampMap = mutableMapOf<String, Long>()
        private const val DEDUPLICATION_WINDOW_MS = 60_000L // 60s deduplication per number
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown Caller"

        Log.d(TAG, "Call State Change Detected: State=$state | MaskedNumber=${maskPhoneNumber(incomingNumber)}")

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            handleIncomingCall(context, incomingNumber)
        }
    }

    private fun handleIncomingCall(context: Context, phoneNumber: String) {
        val focusState = FocusRepository.instance.focusState.value

        // Section 5 Requirement: If Focus Mode is OFF, normal call behavior occurs
        if (!focusState.enabled) {
            Log.d(TAG, "Focus Mode OFF: Ignoring incoming call for normal Android handling.")
            return
        }

        // Section 14 & 15 Requirement: Check user preferences for call handling
        if (!focusState.enableCallHandling || !focusState.enableAutoSms) {
            Log.d(TAG, "Focus Mode ON, but Call Handling / Auto SMS is disabled by user.")
            return
        }

        // Section 20 Requirement: Spam Protection / Deduplication
        val now = System.currentTimeMillis()
        val lastSent = lastSentTimestampMap[phoneNumber] ?: 0L
        if (now - lastSent < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "Deduplicating auto-SMS for ${maskPhoneNumber(phoneNumber)} (sent recently)")
            return
        }

        // Section 3 & 16 Requirement: Request / check SMS permission before sending
        if (PermissionUtils.hasSmsPermissions(context)) {
            sendCallerUrgencySms(context, phoneNumber)
            lastSentTimestampMap[phoneNumber] = now
        } else {
            Log.w(TAG, "SMS permission not granted. Cannot send automatic triage SMS to caller.")
        }
    }

    private fun sendCallerUrgencySms(context: Context, phoneNumber: String) {
        val message = "SmartNotify: The user is currently busy with Focus Mode. " +
                "Reply SMARTNOTIFY 1 to request an immediate callback, or SMARTNOTIFY 2 to request a callback when free."

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.i(TAG, "Auto SMS triage sent to caller: ${maskPhoneNumber(phoneNumber)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to ${maskPhoneNumber(phoneNumber)}: ${e.message}")
        }
    }

    private fun maskPhoneNumber(number: String): String {
        if (number.length <= 4) return "***"
        return number.take(2) + "******" + number.takeLast(2)
    }
}
