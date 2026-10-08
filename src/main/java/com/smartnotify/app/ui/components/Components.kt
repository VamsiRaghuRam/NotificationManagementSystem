package com.smartnotify.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.data.model.ActionTaken
import com.smartnotify.app.data.model.NotificationItem
import com.smartnotify.app.data.model.PriorityLevel
import com.smartnotify.app.data.model.ProcessingStatus
import com.smartnotify.app.ui.theme.*

@Composable
fun PriorityBadge(priority: PriorityLevel, confidence: Float = 0.0f) {
    val (bgColor, textColor, label) = when (priority) {
        PriorityLevel.UNCLASSIFIED -> Triple(Color(0x336B7280), TextDim, "UNCLASSIFIED")
        PriorityLevel.LOW -> Triple(Color(0x3310B981), LowGreen, "LOW")
        PriorityLevel.MEDIUM -> Triple(Color(0x33F59E0B), MedAmber, "MEDIUM")
        PriorityLevel.HIGH -> Triple(Color(0x40EF4444), HighRed, "HIGH")
    }

    val displayLabel = label

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayLabel,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionTakenText(item: NotificationItem) {
    val (text, color) = when (item.processingStatus) {
        ProcessingStatus.PENDING -> "⏳ Pending ML Classification..." to MedAmber
        ProcessingStatus.FAILED -> "⚠️ Classification Failed (Backend Offline)" to TextDim
        ProcessingStatus.CLASSIFIED -> when (item.actionTaken) {
            ActionTaken.SUPPRESSED_BY_SMARTNOTIFY -> "🚫 SUPPRESSED BY SMARTNOTIFY (Focus Protected)" to LowGreen
            ActionTaken.SOFT_ALERT -> "🔔 SOFT ALERT (Low-Intrusion Banner Issued)" to MedAmber
            ActionTaken.IMPORTANT_ALERT -> "🚨 IMPORTANT ALERT (Focus Mode Override Triggered)" to HighRed
            ActionTaken.NORMAL_BEHAVIOR -> "📱 NORMAL BEHAVIOR (Focus Mode OFF)" to TextDim
            else -> "⏳ Pending Action" to TextMuted
        }
    }

    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onFeedback: ((com.smartnotify.app.data.model.UserFeedbackRating) -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📲 ${item.appName} • ${item.timestamp}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                PriorityBadge(priority = item.priority, confidence = item.confidence)
            }

            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = item.messagePreview,
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            ActionTakenText(item = item)

            // User Feedback Section for Priority 5 & 6
            if (item.processingStatus == ProcessingStatus.CLASSIFIED) {
                androidx.compose.material3.Divider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Was classification useful?",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (item.userFeedback == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { onFeedback?.invoke(com.smartnotify.app.data.model.UserFeedbackRating.CORRECT) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("👍 Correct", fontSize = 11.sp, color = LowGreen)
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { onFeedback?.invoke(com.smartnotify.app.data.model.UserFeedbackRating.INCORRECT) },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("👎 Incorrect", fontSize = 11.sp, color = HighRed)
                            }
                        }
                    } else {
                        val isCorrect = item.userFeedback == com.smartnotify.app.data.model.UserFeedbackRating.CORRECT
                        Text(
                            text = if (isCorrect) "Feedback: Correct ✓" else "Feedback: Incorrect ✗",
                            color = if (isCorrect) LowGreen else HighRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
