package com.commerin.telemetri

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.commerin.telemetri.domain.model.PermissionState
import com.commerin.telemetri.domain.model.TelemetryPermissions
import com.commerin.telemetri.ui.screens.home.HomeScreen
import com.commerin.telemetri.ui.screens.permissions.PermissionsScreen
import com.commerin.telemetri.ui.screens.usecases.*
import com.commerin.telemetri.ui.theme.TelemetriTheme
import com.commerin.telemetri.ui.theme.ThemeState
import com.commerin.telemetri.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Create theme state that can be toggled
            val systemInDarkTheme = isSystemInDarkTheme()
            val themeState = remember { ThemeState(systemInDarkTheme) }

            TelemetriTheme(themeState = themeState) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelemetryApp()
                }
            }
        }
    }
}

@Composable
private fun TelemetryApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Check permission status on each composition
    var hasRequiredPermissions by remember { mutableStateOf(false) }
    var isCheckingPermissions by remember { mutableStateOf(true) }

    // Check permissions when the app starts and whenever permission state changes
    LaunchedEffect(Unit) {
        hasRequiredPermissions = checkEssentialPermissions(context)
        isCheckingPermissions = false
    }

    when {
        isCheckingPermissions -> {
            // Show loading while checking permissions
            LoadingScreen()
        }
        !hasRequiredPermissions -> {
            // Show permissions screen if essential permissions are missing
            PermissionsScreen(
                onPermissionsGranted = {
                    hasRequiredPermissions = true
                }
            )
        }
        else -> {
            // Show main app content when permissions are granted
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
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
                                    navController.navigateUp()
                                }
                            )
                        }
                        "automotive" -> {
                            AutomotiveUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        "fitness" -> {
                            FitnessUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        "environmental" -> {
                            EnvironmentalUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        "security" -> {
                            SecurityUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        "battery_saver" -> {
                            BatterySaverUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                        else -> {
                            // Default to automotive use case
                            AutomotiveUseCaseScreen(
                                onBackPressed = {
                                    navController.navigateUp()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            // Simple loading indicator
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Check if essential permissions for basic telemetry functionality are granted
 * You can customize this logic based on your minimum requirements
 */
private fun checkEssentialPermissions(context: android.content.Context): Boolean {
    // For basic functionality, require at least location permissions
    // You can adjust this based on your SDK's minimum requirements
    val essentialPermissions = listOf(
        TelemetryPermissions.LOCATION_FINE,
        TelemetryPermissions.LOCATION_COARSE
    )

    return essentialPermissions.all { permission ->
        PermissionUtils.getPermissionState(context, permission) is PermissionState.Granted
    }
}
