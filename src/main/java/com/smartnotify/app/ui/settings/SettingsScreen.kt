package com.smartnotify.app.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.data.repository.NotificationRepository
import com.smartnotify.app.ui.theme.*
import com.smartnotify.app.utils.PermissionUtils
import com.smartnotify.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAlertPrefs: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDeveloperDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    var isAccessGranted by remember { mutableStateOf(PermissionUtils.isNotificationAccessGranted(context)) }
    
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var isPolicyAccessGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else true
        )
    }

    val backendConnected by NotificationRepository.instance.backendConnected.collectAsState()

    DisposableEffect(Unit) {
        isAccessGranted = PermissionUtils.isNotificationAccessGranted(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isPolicyAccessGranted = notificationManager.isNotificationPolicyAccessGranted
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Notification Access Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔔 Notification Access", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isAccessGranted) "Enabled" else "Disabled",
                        color = if (isAccessGranted) LowGreen else HighRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "SmartNotify requires Notification Access to automatically intercept real system notifications (WhatsApp, SMS, Slack, Gmail).",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (!isAccessGranted) {
                    Button(
                        onClick = { PermissionUtils.openNotificationAccessSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Enable Notification Access", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 9 Requirement: Official Notification Policy Access (DND / Zen Mode) Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🌙 Notification Policy (DND)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isPolicyAccessGranted) "Enabled" else "Disabled",
                        color = if (isPolicyAccessGranted) LowGreen else MedAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Allows SmartNotify to set priority DND interruption rules during active Focus Mode using supported Android OS APIs.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                if (!isPolicyAccessGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Enable DND Policy Access", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Backend Status Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "🤖 ML Service Status", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = "LinearSVC Content Classifier", color = TextMuted, fontSize = 12.sp)
                }
                Text(
                    text = if (backendConnected) "CONNECTED" else "UNAVAILABLE",
                    color = if (backendConnected) LowGreen else HighRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // User Configuration Options List
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsItem(title = "⚙️ Alert Preferences", subtitle = "Configure HIGH priority vibration & sound alerts", onClick = onNavigateToAlertPrefs)
                HorizontalDivider(color = CardBorder)
                SettingsItem(title = "🔒 Privacy & Data Policy", subtitle = "Local phone processing & non-logging policy", onClick = onNavigateToPrivacy)
                HorizontalDivider(color = CardBorder)
                SettingsItem(title = "ℹ️ About SmartNotify", subtitle = "System architecture & model performance", onClick = onNavigateToAbout)
                HorizontalDivider(color = CardBorder)
                SettingsItem(title = "🛠️ Developer Diagnostics", subtitle = "Internal telemetry & debug status", onClick = onNavigateToDeveloperDiagnostics)
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Text(text = "›", color = TextDim, fontSize = 20.sp)
    }
}
