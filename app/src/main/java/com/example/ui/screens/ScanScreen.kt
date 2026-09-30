package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.QRApplication
import com.example.ads.AdManager
import com.example.data.local.QrItem
import com.example.models.QrParsedResult
import com.example.models.QrType
import com.example.ui.components.QrResultBottomSheet
import com.example.ui.components.QrScannerView
import com.example.utils.FeedbackUtils
import com.example.utils.QrParser
import com.example.utils.ShareUtils
import kotlinx.coroutines.launch

@Composable
fun ScanScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val qrRepository = QRApplication.instance.qrRepository
    val userPreferencesRepository = QRApplication.instance.userPreferencesRepository

    val vibrationEnabled by userPreferencesRepository.vibrationEnabledFlow.collectAsState(initial = true)
    val soundEnabled by userPreferencesRepository.soundEnabledFlow.collectAsState(initial = true)
    val autoOpenUrl by userPreferencesRepository.autoOpenUrlFlow.collectAsState(initial = false)

    var currentScanResult by remember { mutableStateOf<QrParsedResult?>(null) }
    var isScanningActive by remember { mutableStateOf(true) }

    fun handleScan(rawString: String) {
        if (!isScanningActive || currentScanResult != null) return
        isScanningActive = false

        val parsed = QrParser.parse(rawString)
        currentScanResult = parsed

        // Haptic & Sound Feedback
        FeedbackUtils.performScanSuccessFeedback(
            context = context,
            vibrate = vibrationEnabled,
            sound = soundEnabled
        )

        // Save to Room database history
        coroutineScope.launch {
            try {
                qrRepository.insertItem(
                    QrItem(
                        content = parsed.rawContent,
                        qrType = parsed.type.name,
                        title = parsed.title,
                        isGenerated = false,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (_: Exception) {
                // Ignore DB insertion errors
            }
        }

        // Auto-open URL if configured
        if (autoOpenUrl && parsed.type == QrType.URL && !parsed.url.isNullOrBlank()) {
            ShareUtils.openUrlSafely(context, parsed.url)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("scan_screen_root")
    ) {
        QrScannerView(
            isScanningActive = isScanningActive && currentScanResult == null,
            onQrCodeDetected = { raw ->
                handleScan(raw)
            }
        )

        // Result bottom sheet
        currentScanResult?.let { result ->
            QrResultBottomSheet(
                result = result,
                onDismiss = {
                    currentScanResult = null
                    isScanningActive = true
                    // Show interstitial ad if cooldown passed at natural transition point
                    if (activity != null) {
                        AdManager.showInterstitialAdWithCooldown(activity) {}
                    }
                }
            )
        }
    }
}
