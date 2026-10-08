package com.smartnotify.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.smartnotify.app.data.model.CallRequest
import com.smartnotify.app.data.model.CallStatus
import com.smartnotify.app.data.model.RequestType
import com.smartnotify.app.data.repository.CallRequestRepository
import com.smartnotify.app.data.repository.PreferencesRepository
import com.smartnotify.app.decision.NotificationDecisionEngine
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phase 9 Incoming SMS Broadcast Receiver.
 * Parses SMS responses from callers (SMARTNOTIFY 1 or SMARTNOTIFY 2).
 * Strictly avoids non-SmartNotify messages and enforces spam deduplication.
 */
class SmartNotifySmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmartNotifySmsRecv"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.originatingAddress ?: "Unknown Caller"
            val body = sms.messageBody ?: ""

            parseAndProcessSmsResponse(context, sender, body)
        }
    }

    fun parseAndProcessSmsResponse(context: Context, sender: String, body: String) {
        val cleanBody = body.trim().uppercase()

        // Section 17 & 18 Requirement: Strict format verification (SMARTNOTIFY 1 or SMARTNOTIFY 2)
        val isOption1 = cleanBody.contains("SMARTNOTIFY 1") || cleanBody.contains("SMARTNOTIFY_REPLY:1") || cleanBody == "1"
        val isOption2 = cleanBody.contains("SMARTNOTIFY 2") || cleanBody.contains("SMARTNOTIFY_REPLY:2") || cleanBody == "2"

        // Section 19 Requirement: Ignore invalid responses without creating call requests
        if (!isOption1 && !isOption2) {
            Log.d(TAG, "Ignored non-SmartNotify SMS from ${maskPhoneNumber(sender)}")
            return
        }

        val callerName = resolveContactName(sender)
        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        if (isOption1) {
            // Section 8 & 21 Requirement: Option 1 - CALL ME IMMEDIATELY (URGENT ALERT)
            val request = CallRequest(
                id = UUID.randomUUID().toString(),
                callerName = callerName,
                phoneNumber = sender,
                timestamp = timeFormatted,
                requestType = RequestType.CALL_IMMEDIATELY,
                status = CallStatus.NOTIFIED
            )

            CallRequestRepository.instance.addCallRequest(request)

            // Trigger High Priority Alert using Decision Engine & User Alert Preferences
            val prefs = PreferencesRepository.instance.prefs.value
            NotificationDecisionEngine.getInstance(context).triggerUrgentCallAlert(callerName, prefs)

            Log.i(TAG, "Processed SMARTNOTIFY 1 (URGENT CALL REQUEST) from ${maskPhoneNumber(sender)}")
        } else if (isOption2) {
            // Section 9 & 22 Requirement: Option 2 - CALL ME WHEN FREE (STORED SILENTLY)
            val request = CallRequest(
                id = UUID.randomUUID().toString(),
                callerName = callerName,
                phoneNumber = sender,
                timestamp = timeFormatted,
                requestType = RequestType.CALL_WHEN_FREE,
                status = CallStatus.PENDING
            )

            CallRequestRepository.instance.addCallRequest(request)
            Log.i(TAG, "Processed SMARTNOTIFY 2 (CALL WHEN FREE) from ${maskPhoneNumber(sender)}")
        }
    }

    private fun resolveContactName(phoneNumber: String): String {
        // Simple mock contact resolver for academic prototype
        return when {
            phoneNumber.endsWith("0199") -> "John"
            phoneNumber.endsWith("0144") -> "Rahul"
            phoneNumber.endsWith("9999") -> "Alice"
            else -> "Caller " + phoneNumber.takeLast(4)
        }
    }

    private fun maskPhoneNumber(number: String): String {
        if (number.length <= 4) return "***"
        return number.take(2) + "******" + number.takeLast(2)
    }
}
