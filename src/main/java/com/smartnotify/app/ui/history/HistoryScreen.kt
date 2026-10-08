package com.smartnotify.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartnotify.app.data.model.PriorityLevel
import com.smartnotify.app.ui.components.NotificationCard
import com.smartnotify.app.ui.theme.*
import com.smartnotify.app.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var showCurrentSessionOnly by remember { mutableStateOf(true) }

    val baseList = remember(notifications, activeSessionId, showCurrentSessionOnly) {
        if (showCurrentSessionOnly && activeSessionId.isNotEmpty()) {
            notifications.filter { it.sessionId == activeSessionId }
        } else {
            notifications
        }
    }

    val filteredList = remember(baseList, selectedFilter) {
        if (selectedFilter == null) baseList
        else baseList.filter { it.priority == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showCurrentSessionOnly) "Current Session" else "All History",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showCurrentSessionOnly = !showCurrentSessionOnly }) {
                    Text(
                        text = if (showCurrentSessionOnly) "Show All History" else "Show Current Session",
                        color = PrimaryIndigo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = { viewModel.notificationRepository.clearHistory() }) {
                        Text(text = "Clear", color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("All (${baseList.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == PriorityLevel.HIGH,
                onClick = { viewModel.setFilter(PriorityLevel.HIGH) },
                label = { Text("HIGH", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == PriorityLevel.MEDIUM,
                onClick = { viewModel.setFilter(PriorityLevel.MEDIUM) },
                label = { Text("MEDIUM", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == PriorityLevel.LOW,
                onClick = { viewModel.setFilter(PriorityLevel.LOW) },
                label = { Text("LOW", fontSize = 11.sp) }
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📲", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showCurrentSessionOnly) "No communications in active session" else "No real notifications captured yet",
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notifications received during Focus Mode will be screened and listed here.",
                        color = TextDim,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredList) { item ->
                    NotificationCard(
                        item = item,
                        onFeedback = { rating ->
                            viewModel.submitFeedback(item.id, rating)
                        }
                    )
                }
            }
        }
    }
}
