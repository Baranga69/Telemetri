package com.commerin.telemetri.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.commerin.telemetri.ui.screens.home.HomeScreen
import com.commerin.telemetri.ui.screens.permissions.PermissionsScreen
import com.commerin.telemetri.ui.screens.usecases.*

@Composable
fun TelemetriNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "permissions"
    ) {
        composable("permissions") {
            PermissionsScreen(
                onPermissionsGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                onNavigateToUseCase = { useCaseId ->
                    navController.navigate("usecase/$useCaseId")
                }
            )
        }

        composable("usecase/{useCaseId}") { backStackEntry ->
            val useCaseId = backStackEntry.arguments?.getString("useCaseId") ?: "automotive"

            when (useCaseId) {
                "network_demo" -> {
                    NetworkTelemetryDemoScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                "automotive" -> {
                    AutomotiveUseCaseScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                "fitness" -> {
                    FitnessUseCaseScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                "environmental" -> {
                    EnvironmentalUseCaseScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                "security" -> {
                    SecurityUseCaseScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                "battery_saver" -> {
                    BatterySaverUseCaseScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
                else -> {
                    // Default to network demo
                    NetworkTelemetryDemoScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
