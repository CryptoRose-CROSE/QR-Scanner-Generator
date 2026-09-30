package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QRApplication
import com.example.ads.BannerAdView
import com.example.config.AppConfig
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.SectionHeader
import com.example.utils.ShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToSupport: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPreferencesRepository = QRApplication.instance.userPreferencesRepository

    val darkMode by userPreferencesRepository.darkModeFlow.collectAsState(initial = 0)
    val vibrationEnabled by userPreferencesRepository.vibrationEnabledFlow.collectAsState(initial = true)
    val soundEnabled by userPreferencesRepository.soundEnabledFlow.collectAsState(initial = true)
    val autoOpenUrl by userPreferencesRepository.autoOpenUrlFlow.collectAsState(initial = false)
    val language by userPreferencesRepository.languageFlow.collectAsState(initial = "en")

    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    fun openSocialLink(url: String) {
        ShareUtils.openAppOrBrowser(context, url)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & About",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // General Preferences
            SectionHeader(title = "App Preferences")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    // Dark Mode Setting
                    SettingsClickableItem(
                        icon = Icons.Default.Brightness4,
                        title = "Appearance / Theme",
                        subtitle = when (darkMode) {
                            1 -> "Light Mode"
                            2 -> "Dark Mode"
                            else -> "System Default"
                        },
                        onClick = { showDarkModeDialog = true },
                        testTag = "setting_theme"
                    )

                    // Vibration Switch
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "Haptic Vibration",
                        subtitle = "Vibrate phone on successful scan",
                        checked = vibrationEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                userPreferencesRepository.setVibrationEnabled(checked)
                            }
                        },
                        testTag = "setting_vibration"
                    )

                    // Sound Switch
                    SettingsSwitchItem(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Beep Sound",
                        subtitle = "Play tone on successful scan",
                        checked = soundEnabled,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                userPreferencesRepository.setSoundEnabled(checked)
                            }
                        },
                        testTag = "setting_sound"
                    )

                    // Auto Open URL Switch
                    SettingsSwitchItem(
                        icon = Icons.Default.Security,
                        title = "Auto-Open Scanned URLs",
                        subtitle = "Automatically open trusted links in browser",
                        checked = autoOpenUrl,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                userPreferencesRepository.setAutoOpenUrl(checked)
                            }
                        },
                        testTag = "setting_auto_open_url"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social Media Follow Us Section
            SectionHeader(title = "Follow Us & Community")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    // X (Twitter)
                    SocialMediaRow(
                        platformName = "X (Twitter)",
                        description = "Latest updates, announcements & news",
                        url = AppConfig.X_URL,
                        onClick = { openSocialLink(AppConfig.X_URL) },
                        testTag = "social_x"
                    )

                    // Telegram
                    SocialMediaRow(
                        platformName = "Telegram",
                        description = "Join community discussions & chat channel",
                        url = AppConfig.TELEGRAM_URL,
                        onClick = { openSocialLink(AppConfig.TELEGRAM_URL) },
                        testTag = "social_telegram"
                    )

                    // Facebook
                    SocialMediaRow(
                        platformName = "Facebook",
                        description = "Official community page & updates",
                        url = AppConfig.FACEBOOK_URL,
                        onClick = { openSocialLink(AppConfig.FACEBOOK_URL) },
                        testTag = "social_facebook"
                    )

                    // YouTube
                    SocialMediaRow(
                        platformName = "YouTube",
                        description = "Tutorials, guides & feature spotlights",
                        url = AppConfig.YOUTUBE_URL,
                        onClick = { openSocialLink(AppConfig.YOUTUBE_URL) },
                        testTag = "social_youtube"
                    )

                    // Website
                    SocialMediaRow(
                        platformName = "Official Website",
                        description = "Visit developer portal & web utilities",
                        url = AppConfig.WEBSITE_URL,
                        onClick = { openSocialLink(AppConfig.WEBSITE_URL) },
                        testTag = "social_website"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legal & About Section
            SectionHeader(title = "About & Support")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    SettingsClickableItem(
                        icon = Icons.Default.Favorite,
                        title = "❤️ Support the Developer",
                        subtitle = "Support future updates and development",
                        onClick = onNavigateToSupport,
                        testTag = "setting_support_developer"
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.Star,
                        title = "Rate App",
                        subtitle = "Leave a review on Google Play",
                        onClick = {
                            try {
                                val uri = Uri.parse("market://details?id=${AppConfig.RATE_APP_PACKAGE_NAME}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (_: Exception) {
                                ShareUtils.openAppOrBrowser(context, "https://play.google.com/store/apps/details?id=${AppConfig.RATE_APP_PACKAGE_NAME}")
                            }
                        },
                        testTag = "setting_rate_app"
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        subtitle = "Learn how your data is protected",
                        onClick = { showPrivacyDialog = true },
                        testTag = "setting_privacy_policy"
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.Gavel,
                        title = "Terms & Conditions",
                        subtitle = "Terms of service and usage guidelines",
                        onClick = { showTermsDialog = true },
                        testTag = "setting_terms"
                    )

                    SettingsClickableItem(
                        icon = Icons.Default.Info,
                        title = "About ${AppConfig.APP_NAME}",
                        subtitle = "Version ${AppConfig.APP_VERSION} • Production Ready",
                        onClick = { showAboutDialog = true },
                        testTag = "setting_about"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner Ad at bottom
            BannerAdView()

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Dark Mode Selection Dialog
        if (showDarkModeDialog) {
            ConfirmationDialog(
                title = "Choose Theme",
                message = "Select your preferred visual mode for QR Scanner & Generator:\n\n• System Default (Follows OS setting)\n• Light Mode\n• Dark Mode (Eye-safe OLED dark)",
                confirmButtonText = "Dark Mode",
                dismissButtonText = "Light / System",
                icon = Icons.Default.DarkMode,
                onConfirm = {
                    showDarkModeDialog = false
                    coroutineScope.launch {
                        userPreferencesRepository.setDarkMode(2)
                    }
                },
                onDismiss = {
                    showDarkModeDialog = false
                    coroutineScope.launch {
                        userPreferencesRepository.setDarkMode(if (darkMode == 1) 0 else 1)
                    }
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            ConfirmationDialog(
                title = "Privacy Policy",
                message = "Your privacy is paramount.\n\n• All scanned QR data and generated codes are stored exclusively locally on your device.\n• Camera permission is strictly used for real-time QR/barcode detection.\n• No scanned contents are uploaded to remote servers without your explicit consent.\n\nVisit our online policy page for more information.",
                confirmButtonText = "Open Online Policy",
                dismissButtonText = "Close",
                icon = Icons.Default.Policy,
                onConfirm = {
                    showPrivacyDialog = false
                    ShareUtils.openAppOrBrowser(context, AppConfig.PRIVACY_POLICY_URL)
                },
                onDismiss = {
                    showPrivacyDialog = false
                }
            )
        }

        // Terms & Conditions Dialog
        if (showTermsDialog) {
            ConfirmationDialog(
                title = "Terms & Conditions",
                message = "Terms of Service:\n\n1. Use QR Scanner & Generator responsibly and verify destination URLs before browsing.\n2. Do not encode malicious or illegal content.\n3. The application is provided as-is with industry-standard security and error handling.\n\nVisit our legal portal for full terms.",
                confirmButtonText = "Open Full Terms",
                dismissButtonText = "Close",
                icon = Icons.Default.Description,
                onConfirm = {
                    showTermsDialog = false
                    ShareUtils.openAppOrBrowser(context, AppConfig.TERMS_URL)
                },
                onDismiss = {
                    showTermsDialog = false
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            ConfirmationDialog(
                title = AppConfig.APP_NAME,
                message = "Version ${AppConfig.APP_VERSION}\n\nBuilt with Kotlin, Material 3, CameraX, Google ML Kit, ZXing, and Google AdMob SDK.\n\nFeatures modern offline-first Room database, adaptive UI, and MediaStore storage support.",
                confirmButtonText = "OK",
                dismissButtonText = "Share App",
                icon = Icons.Default.Info,
                onConfirm = {
                    showAboutDialog = false
                },
                onDismiss = {
                    showAboutDialog = false
                    ShareUtils.shareText(context, "Check out ${AppConfig.APP_NAME} - the modern QR scanner and creator!")
                }
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SocialMediaRow(
    platformName: String,
    description: String,
    url: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = platformName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open", fontSize = 12.sp)
        }
    }
}
