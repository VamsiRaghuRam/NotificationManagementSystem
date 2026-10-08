package com.smartnotify.app.ui.settings

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
import com.smartnotify.app.viewmodel.SettingsViewModel

@Composable
fun AlertPreferencesScreen(viewModel: SettingsViewModel) {
    val prefs by viewModel.alertPreferences.collectAsState()

    var vibrate by remember { mutableStateOf(prefs.vibrate) }
    var screenOn by remember { mutableStateOf(prefs.screenOn) }
    var sound by remember { mutableStateOf(prefs.sound) }
    var ring by remember { mutableStateOf(prefs.ring) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Alert Preferences", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "These preferences are used when SmartNotify identifies a notification as HIGH priority.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Divider(color = CardBorder)

                PreferenceSwitch("📳 Vibrate", vibrate) { vibrate = it; viewModel.updatePreferences(vibrate, screenOn, sound, ring) }
                PreferenceSwitch("⚡ Screen On", screenOn) { screenOn = it; viewModel.updatePreferences(vibrate, screenOn, sound, ring) }
                PreferenceSwitch("🔔 Sound", sound) { sound = it; viewModel.updatePreferences(vibrate, screenOn, sound, ring) }
                PreferenceSwitch("📞 Ring", ring) { ring = it; viewModel.updatePreferences(vibrate, screenOn, sound, ring) }
            }
        }
    }
}

@Composable
fun PreferenceSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryIndigo)
        )
    }
}
