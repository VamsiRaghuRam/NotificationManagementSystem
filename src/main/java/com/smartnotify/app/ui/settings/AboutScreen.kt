package com.smartnotify.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.ui.theme.*

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "About SmartNotify", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🔔 SmartNotify", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "Intelligent Notification Management System", color = TextMuted, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = CardBorder)

                TechRow("Mobile Platform", "Native Android (Kotlin + Jetpack Compose)")
                TechRow("Design System", "Material 3 Dark Theme")
                TechRow("Backend Engine", "Python FastAPI REST Service")
                TechRow("Feature Extractor", "TF-IDF Vectorizer (5,000 features)")
                TechRow("Trained Models", "Naive Bayes, Logistic Regression, SVM")
                TechRow("Selected Model", "Support Vector Machine (LinearSVC)")
                TechRow("Dataset", "10,800 records (NotifAI + Smartphone Notifications)")
            }
        }
    }
}

@Composable
fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
