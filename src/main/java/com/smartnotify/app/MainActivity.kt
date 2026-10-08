package com.smartnotify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smartnotify.app.navigation.SmartNotifyMainApp
import com.smartnotify.app.ui.theme.DarkBg
import com.smartnotify.app.ui.theme.SmartNotifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.smartnotify.app.data.api.ApiClient.init(applicationContext)
        setContent {
            SmartNotifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    SmartNotifyMainApp()
                }
            }
        }
    }
}
