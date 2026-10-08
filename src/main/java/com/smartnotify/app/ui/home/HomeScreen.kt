package com.smartnotify.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.ui.theme.*
import com.smartnotify.app.viewmodel.HomeViewModel

import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSessionSummary: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusState by viewModel.focusState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val callRequests by viewModel.callRequests.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    // Isolated current focus session notifications query
    val currentSessionNotifs = remember(notifications, focusState.activeSessionId) {
        if (focusState.activeSessionId.isNotEmpty()) {
            notifications.filter { it.sessionId == focusState.activeSessionId }
        } else {
            emptyList()
        }
    }

    val messageCount = currentSessionNotifs.size
    val callCount = callRequests.size
    val importantAlertCount = currentSessionNotifs.count { it.priority == com.smartnotify.app.data.model.PriorityLevel.HIGH }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Brand Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SMARTNOTIFY",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Intelligent Notification Screening",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // Notification Policy Permission Banner (Priority 1 Requirement)
        if (!focusState.hasPolicyAccess || focusState.policyAccessMissing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, HighRed, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⚠️ Notification access required for Focus Mode",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Focus Mode unavailable until required Do Not Disturb access is granted.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = { com.smartnotify.app.utils.PermissionUtils.openNotificationPolicySettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = HighRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Enable Access", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Primary Control: FOCUS MODE [ ON / OFF ] Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "FOCUS MODE",
                    color = TextMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                // Large ON / OFF Toggle Button
                Button(
                    onClick = { viewModel.toggleFocusMode(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (focusState.enabled) LowGreen else Color(0xFF333344)
                    ),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp)
                ) {
                    Text(
                        text = if (focusState.enabled) "ON" else "OFF",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Status Description
                Text(
                    text = if (focusState.enabled)
                        "Focus Mode is ON. SmartNotify is screening incoming communications."
                    else
                        "Focus Mode is OFF. Notifications behave normally.",
                    color = if (focusState.enabled) LowGreen else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Current Session Metrics Card (Isolated to Current Session ID)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Focus Session",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (focusState.enabled) "ACTIVE" else "COMPLETED",
                        color = if (focusState.enabled) LowGreen else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(label = "Messages", value = messageCount.toString(), color = PrimaryIndigo, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(10.dp))
                    MetricBox(label = "Calls", value = callCount.toString(), color = MedAmber, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(10.dp))
                    MetricBox(label = "Important Alerts", value = importantAlertCount.toString(), color = HighRed, modifier = Modifier.weight(1f))
                }

                if (messageCount == 0 && callCount == 0) {
                    Text(
                        text = "No communications received yet.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = onNavigateToSessionSummary,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "View Current Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DarkBg, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
