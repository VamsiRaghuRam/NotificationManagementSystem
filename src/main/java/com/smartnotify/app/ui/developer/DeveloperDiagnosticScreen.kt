package com.smartnotify.app.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.smartnotify.app.data.api.ApiClient
import com.smartnotify.app.data.repository.FocusRepository
import com.smartnotify.app.data.repository.NotificationRepository
import com.smartnotify.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDiagnosticScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val listenerConnected by NotificationRepository.instance.listenerConnected.collectAsState()
    val focusState by FocusRepository.instance.focusState.collectAsState()
    val backendConnected by NotificationRepository.instance.backendConnected.collectAsState()

    val capturedCount by NotificationRepository.instance.capturedCount.collectAsState()
    val storedCount by NotificationRepository.instance.storedCount.collectAsState()
    val mlProcessedCount by NotificationRepository.instance.mlProcessedCount.collectAsState()
    val mlFailedCount by NotificationRepository.instance.mlFailedCount.collectAsState()
    val lastCapturedApp by NotificationRepository.instance.lastCapturedApp.collectAsState()

    var serverUrlInput by remember { mutableStateOf(ApiClient.baseUrl) }
    var saveMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🛠️ Developer Diagnostics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNavigateBack) {
                Text(text = "Done", color = PrimaryIndigo, fontWeight = FontWeight.Bold)
            }
        }

        // Section 31 Requirement: Hidden Developer Telemetry
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
                Text(
                    text = "INTERNAL TELEMETRY STATUS",
                    color = PrimaryIndigo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                DiagRow("Notification Listener", if (listenerConnected) "CONNECTED" else "DISCONNECTED", if (listenerConnected) LowGreen else HighRed)
                DiagRow("Focus Mode", if (focusState.enabled) "ON" else "OFF", if (focusState.enabled) LowGreen else TextMuted)
                DiagRow("Last App", lastCapturedApp ?: "None", Color.White)
                DiagRow("Notifications Captured", capturedCount.toString(), Color.White)
                DiagRow("Notifications Stored", storedCount.toString(), LowGreen)
                DiagRow("ML Processed", mlProcessedCount.toString(), LowGreen)
                DiagRow("ML Failed", mlFailedCount.toString(), if (mlFailedCount > 0) HighRed else TextMuted)
                DiagRow("FastAPI Backend API", if (backendConnected) "CONNECTED" else "DISCONNECTED", if (backendConnected) LowGreen else HighRed)
            }
        }

        // Server URL Configuration (Developer Tool)
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
                Text(text = "🌐 Backend Server URL", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("Server URL", color = TextMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            ApiClient.updateBaseUrl(context, serverUrlInput)
                            serverUrlInput = ApiClient.baseUrl
                            saveMessage = "URL Saved: ${ApiClient.baseUrl}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LowGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Save URL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            ApiClient.updateBaseUrl(context, ApiClient.DEFAULT_EMULATOR_URL)
                            serverUrlInput = ApiClient.baseUrl
                            saveMessage = "Reset to Default URL"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333344)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Reset Default", color = Color.White, fontSize = 12.sp)
                    }
                }

                if (saveMessage.isNotEmpty()) {
                    Text(text = saveMessage, color = LowGreen, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
