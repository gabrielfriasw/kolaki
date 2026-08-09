package com.kolvexo.kolaki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as KolakiApplication
        setContent {
            KolakiApp(
                repository = application.repository,
                preferences = application.preferences,
            )
        }
    }
}
