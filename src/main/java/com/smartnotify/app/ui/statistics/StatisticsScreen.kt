package com.smartnotify.app.ui.statistics

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
import com.smartnotify.app.ui.components.StatCard
import com.smartnotify.app.ui.theme.*
import com.smartnotify.app.viewmodel.StatisticsViewModel

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val stats = viewModel.getStats()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Analytics & Statistics", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // ML Priority Distribution Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "Total Analyzed Notifications", color = TextMuted, fontSize = 14.sp)
                Text(text = "${stats.totalAnalyzed}", color = PrimaryIndigo, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                Divider(color = CardBorder)

                // Priority Distribution Progress Bars
                DistributionBar("LOW Priority", stats.lowCount, stats.totalAnalyzed, LowGreen)
                DistributionBar("MEDIUM Priority", stats.mediumCount, stats.totalAnalyzed, MedAmber)
                DistributionBar("HIGH Priority", stats.highCount, stats.totalAnalyzed, HighRed)
            }
        }

        // Decision Engine Actions Overview
        Text(text = "SmartNotify Decision Actions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) { StatCard("Suppressed", "${stats.suppressedCount}", LowGreen) }
            Box(modifier = Modifier.weight(1f)) { StatCard("Soft Alert", "${stats.softAlertCount}", MedAmber) }
            Box(modifier = Modifier.weight(1f)) { StatCard("Important", "${stats.importantAlertCount}", HighRed) }
            Box(modifier = Modifier.weight(1f)) { StatCard("Normal", "${stats.normalBehaviorCount}", TextDim) }
        }
    }
}

@Composable
fun DistributionBar(label: String, count: Int, total: Int, color: Color) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    val pct = (progress * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = TextMuted, fontSize = 12.sp)
            Text(text = "$count ($pct%)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = Color(0x1AFFFFFF),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}
