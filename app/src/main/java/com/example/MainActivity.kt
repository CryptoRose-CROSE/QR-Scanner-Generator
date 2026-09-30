package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ads.AdManager
import com.example.ui.screens.BankDonationScreen
import com.example.ui.screens.BinanceDonationScreen
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SupportDeveloperScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen(val route: String, val title: String, val icon: ImageVector) {
    SCAN("scan", "Scan", Icons.Default.QrCodeScanner),
    GENERATE("generate", "Generate", Icons.Default.QrCode2),
    HISTORY("history", "History", Icons.Default.History),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Preload interstitial and rewarded ads
        AdManager.preloadInterstitialAd(this)
        AdManager.preloadRewardedAd(this)

        setContent {
            val userPreferences = QRApplication.instance.userPreferencesRepository
            val darkModeSetting by userPreferences.darkModeFlow.collectAsState(initial = 0)

            MyApplicationTheme(darkModeSetting = darkModeSetting) {
                MainAppNavContainer()
            }
        }
    }
}

@Composable
fun MainAppNavContainer() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.SCAN.route

    val screens = listOf(
        Screen.SCAN,
        Screen.GENERATE,
        Screen.HISTORY,
        Screen.SETTINGS
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (screens.any { it.route == currentRoute }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_bottom_nav")
                ) {
                    screens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.SCAN.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.SCAN.route) {
                ScanScreen()
            }
            composable(Screen.GENERATE.route) {
                GenerateScreen()
            }
            composable(Screen.HISTORY.route) {
                HistoryScreen()
            }
            composable(Screen.SETTINGS.route) {
                SettingsScreen(
                    onNavigateToSupport = {
                        navController.navigate("support_developer")
                    }
                )
            }
            composable("support_developer") {
                SupportDeveloperScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onNavigateToBinanceDonation = {
                        navController.navigate("binance_donation")
                    },
                    onNavigateToBankDonation = {
                        navController.navigate("bank_donation")
                    }
                )
            }
            composable("binance_donation") {
                BinanceDonationScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable("bank_donation") {
                BankDonationScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
