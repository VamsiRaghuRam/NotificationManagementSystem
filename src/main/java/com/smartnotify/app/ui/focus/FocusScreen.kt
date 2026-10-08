package com.smartnotify.app.ui.focus

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
import com.smartnotify.app.viewmodel.FocusViewModel

import androidx.compose.ui.platform.LocalContext

@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusState by viewModel.focusState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Focus Mode Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Notification Policy Permission Banner
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

        // Main Focus Mode Switch Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "FOCUS MODE STATUS", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                Button(
                    onClick = { viewModel.toggleFocus(context) },
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

                Text(
                    text = if (focusState.enabled)
                        "Focus Mode is ON. SmartNotify is intelligently screening notifications."
                    else
                        "Focus Mode is OFF. Notifications behave normally.",
                    color = if (focusState.enabled) LowGreen else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Call Handling Settings
        Text(text = "Call Urgency Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SmartNotify Call Handling", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Filter calls during Focus Mode", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = focusState.enableCallHandling,
                        onCheckedChange = { viewModel.updateCallOptions(it, focusState.enableAutoSms) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                    )
                }

                Divider(color = CardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Caller Automatic SMS Triage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Send SMARTNOTIFY 1/2 triage SMS", color = TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = focusState.enableAutoSms,
                        onCheckedChange = { viewModel.updateCallOptions(focusState.enableCallHandling, it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                    )
                }
            }
        }
    }
}
