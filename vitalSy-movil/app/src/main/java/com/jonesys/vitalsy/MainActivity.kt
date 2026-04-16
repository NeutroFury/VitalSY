package com.jonesys.vitalsy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jonesys.vitalsy.ui.screen.LoginScreen
import com.jonesys.vitalsy.ui.theme.VitalSYTheme
import com.jonesys.vitalsy.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitalSYTheme {
                val viewModel = LoginViewModel()
                LoginScreen(viewModel = viewModel)
            }
        }
    }

}
