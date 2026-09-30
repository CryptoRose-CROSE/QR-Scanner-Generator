package com.example.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.config.AppConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AdManager"

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AppConfig.BANNER_AD_UNIT_ID
) {
    var isAdFailed by remember { mutableStateOf(false) }
    var debugErrorMessage by remember { mutableStateOf<String?>(null) }
    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
        }
    }

    if (!isAdFailed) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("banner_ad_container"),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    Log.d(TAG, "[Banner Ad] Load started with adUnitId: $adUnitId")
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        setAdUnitId(adUnitId)
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d(TAG, "[Banner Ad] Banner loaded successfully.")
                                isAdFailed = false
                                debugErrorMessage = null
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                val errorDetails = "code=${error.code}, message='${error.message}', domain='${error.domain}'"
                                Log.e(TAG, "[Banner Ad] Banner failed to load: $errorDetails")
                                isAdFailed = true
                                debugErrorMessage = "AdMob Banner Notice: ${error.message} (Code ${error.code})"
                            }

                            override fun onAdOpened() {
                                Log.d(TAG, "[Banner Ad] Banner opened.")
                            }

                            override fun onAdClosed() {
                                Log.d(TAG, "[Banner Ad] Banner closed.")
                            }

                            override fun onAdClicked() {
                                Log.d(TAG, "[Banner Ad] Banner clicked.")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                        adViewInstance = this
                    }
                }
            )
        }
    } else if (BuildConfig.DEBUG && debugErrorMessage != null) {
        // Non-blocking diagnostic in DEBUG builds only
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "[DEBUG] $debugErrorMessage",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

