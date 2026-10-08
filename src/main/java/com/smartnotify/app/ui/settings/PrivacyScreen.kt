package com.smartnotify.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.ui.theme.*

@Composable
fun PrivacyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Privacy Policy & Data Principles", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrivacyBullet("On-Premise Machine Learning", "SmartNotify processes notifications using a local scikit-learn model pipeline without sending raw message text to third-party AI clouds.")
                PrivacyBullet("Non-Logging Guarantee", "The FastAPI backend logging framework explicitly omits notification titles and messages from server logs to protect sensitive personal conversations.")
                PrivacyBullet("Zero External LLM Calls", "Core notification classification operates 100% independently of external APIs such as Gemini or OpenAI.")
                PrivacyBullet("User Permission Control", "Android Notification Listener Access requires explicit user authorization and can be revoked at any time from System Settings.")
            }
        }
    }
}

@Composable
fun PrivacyBullet(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "🔒 $title", color = PrimaryIndigo, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = description, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
