package com.jonesys.vitalsy.navigate

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jonesys.vitalsy.ui.screen.LoginScreen
import com.jonesys.vitalsy.viewmodel.LoginViewModel

@Composable
fun AppNavigate() {
    val navController = rememberNavController()
    val loginViewModel = LoginViewModel()

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(viewModel = loginViewModel, navController = navController)
        }
    }
}