package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.config.AppConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object AdManager {

    private const val TAG = "AdManager"

    private var isInitialized = false

    // App Open Ad
    private var appOpenAd: AppOpenAd? = null
    private var isAppOpenAdLoading = false
    private var appOpenAdLoadTime: Long = 0

    // Interstitial Ad
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var lastInterstitialShownTime: Long = 0

    // Rewarded Ad
    private var rewardedAd: RewardedAd? = null
    private var isRewardedAdLoading = false

    // Global ad display lock to prevent multiple ads overlaying simultaneously
    var isShowingAd = false
        private set

    /**
     * Initializes Google Mobile Ads SDK once per application lifecycle.
     */
    fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "AdMob MobileAds already initialized.")
            return
        }

        Log.d(TAG, "Initializing Google Mobile Ads SDK with App ID: ${AppConfig.ADMOB_APP_ID}")
        MobileAds.initialize(context) { initializationStatus ->
            isInitialized = true
            val statusMap = initializationStatus.adapterStatusMap
            Log.d(TAG, "Mobile Ads SDK initialization complete. Adapters count: ${statusMap.size}")
            for ((adapterClass, adapterStatus) in statusMap) {
                Log.d(
                    TAG,
                    "  -> Adapter: $adapterClass, State: ${adapterStatus.initializationState}, " +
                        "Latency: ${adapterStatus.latency}ms, Description: ${adapterStatus.description}"
                )
            }

            // Preload ads immediately after initialization
            preloadAppOpenAd(context)
            preloadInterstitialAd(context)
            preloadRewardedAd(context)
        }
    }

    // =========================================================================
    // ERROR DIAGNOSTICS LOGGING HELPERS
    // =========================================================================

    private fun logLoadAdError(adType: String, error: LoadAdError) {
        val codeName = getErrorCodeName(error.code)
        val responseId = error.responseInfo?.responseId ?: "none"
        val adapterName = error.responseInfo?.mediationAdapterClassName ?: "none"
        Log.e(
            TAG,
            "[$adType Load Failed] Code: ${error.code} ($codeName), " +
                "Message: '${error.message}', Domain: '${error.domain}', " +
                "ResponseId: '$responseId', Adapter: '$adapterName'"
        )
    }

    private fun logAdError(adType: String, error: AdError) {
        val codeName = getErrorCodeName(error.code)
        Log.e(
            TAG,
            "[$adType Show Failed] Code: ${error.code} ($codeName), " +
                "Message: '${error.message}', Domain: '${error.domain}'"
        )
    }

    private fun getErrorCodeName(code: Int): String = when (code) {
        AdRequest.ERROR_CODE_INTERNAL_ERROR -> "ERROR_CODE_INTERNAL_ERROR (0)"
        AdRequest.ERROR_CODE_INVALID_REQUEST -> "ERROR_CODE_INVALID_REQUEST (1)"
        AdRequest.ERROR_CODE_NETWORK_ERROR -> "ERROR_CODE_NETWORK_ERROR (2)"
        AdRequest.ERROR_CODE_NO_FILL -> "ERROR_CODE_NO_FILL (3)"
        AdRequest.ERROR_CODE_APP_ID_MISSING -> "ERROR_CODE_APP_ID_MISSING (8)"
        else -> "UNKNOWN_CODE ($code)"
    }

    // =========================================================================
    // REWARDED AD MANAGEMENT (STRICT REWARD FLOW — NO FREE FALLBACKS)
    // =========================================================================

    /**
     * Preloads a Rewarded Ad into memory if one is not already ready or loading.
     */
    fun preloadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedAdLoading) {
            return
        }

        val adUnitId = AppConfig.REWARDED_AD_UNIT_ID
        isRewardedAdLoading = true
        Log.d(TAG, "[Rewarded Ad] Load started with adUnitId: $adUnitId")

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "[Rewarded Ad] Rewarded ad loaded successfully.")
                    rewardedAd = ad
                    isRewardedAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logLoadAdError("Rewarded Ad", loadAdError)
                    rewardedAd = null
                    isRewardedAdLoading = false
                }
            }
        )
    }

    /**
     * Checks if a Rewarded Ad is currently loaded and ready to show.
     */
    fun isRewardedAdReady(): Boolean {
        return rewardedAd != null
    }

    /**
     * Shows a Rewarded Ad strictly requiring completion to earn reward for QR Generation.
     */
    fun showRewardedAdForGeneration(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        showRewardedAd(
            activity = activity,
            onRewardEarned = onRewardEarned,
            onDismissedWithoutReward = onDismissedWithoutReward,
            onAdFailed = onAdFailed
        )
    }

    /**
     * Shows a Rewarded Ad strictly requiring completion to earn reward for QR Download.
     */
    fun showRewardedAdForDownload(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        showRewardedAd(
            activity = activity,
            onRewardEarned = onRewardEarned,
            onDismissedWithoutReward = onDismissedWithoutReward,
            onAdFailed = onAdFailed
        )
    }

    /**
     * Shows the preloaded Rewarded Ad.
     *
     * Invariants strictly guaranteed:
     * 1. onRewardEarned is ONLY called if the user completed the ad and the official callback was invoked.
     * 2. onDismissedWithoutReward is called if the ad is dismissed before reward is granted.
     * 3. onAdFailed is called if the ad is not ready, fails to show, or another ad is currently showing.
     * 4. Automatic preloading of the next ad is triggered immediately after dismissal or failure.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onDismissedWithoutReward: () -> Unit,
        onAdFailed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w(TAG, "[Rewarded Ad] Cannot show rewarded ad: Activity is finishing or destroyed.")
            onAdFailed()
            return
        }

        if (isShowingAd) {
            Log.w(TAG, "[Rewarded Ad] Cannot show rewarded ad: Another ad is already showing.")
            onAdFailed()
            return
        }

        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "[Rewarded Ad] Cannot show rewarded ad: Ad is not ready.")
            preloadRewardedAd(activity.applicationContext)
            onAdFailed()
            return
        }

        val hasEarnedReward = AtomicBoolean(false)
        val hasFinishedCallback = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "[Rewarded Ad] Rewarded ad showed full screen.")
                isShowingAd = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logAdError("Rewarded Ad", adError)
                isShowingAd = false
                rewardedAd = null
                preloadRewardedAd(activity.applicationContext)
                if (hasFinishedCallback.compareAndSet(false, true)) {
                    onAdFailed()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                val earned = hasEarnedReward.get()
                Log.d(TAG, "[Rewarded Ad] Rewarded ad dismissed. Earned reward = $earned")
                isShowingAd = false
                rewardedAd = null
                preloadRewardedAd(activity.applicationContext)
                if (hasFinishedCallback.compareAndSet(false, true)) {
                    if (earned) {
                        Log.d(TAG, "[Rewarded Ad] Dispatched onRewardEarned callback.")
                        onRewardEarned()
                    } else {
                        Log.d(TAG, "[Rewarded Ad] Dispatched onDismissedWithoutReward callback.")
                        onDismissedWithoutReward()
                    }
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "[Rewarded Ad] Impression recorded.")
            }

            override fun onAdClicked() {
                Log.d(TAG, "[Rewarded Ad] Click recorded.")
            }
        }

        Log.d(TAG, "[Rewarded Ad] Show started.")
        ad.show(activity) { rewardItem ->
            Log.d(TAG, "[Rewarded Ad] User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            hasEarnedReward.set(true)
        }
    }

    // =========================================================================
    // INTERSTITIAL AD MANAGEMENT (WITH COOLDOWN PROTECTION)
    // =========================================================================

    /**
     * Preloads an Interstitial Ad into memory if needed.
     */
    fun preloadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) {
            return
        }

        val adUnitId = AppConfig.INTERSTITIAL_AD_UNIT_ID
        isInterstitialLoading = true
        Log.d(TAG, "[Interstitial Ad] Load started with adUnitId: $adUnitId")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "[Interstitial Ad] Interstitial ad loaded successfully.")
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logLoadAdError("Interstitial Ad", loadAdError)
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    /**
     * Shows an Interstitial Ad respecting the configured cooldown period (45s).
     * If the ad is not ready, in cooldown, or fails, the [onComplete] callback is invoked immediately.
     */
    fun showInterstitialAdWithCooldown(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || isShowingAd) {
            onComplete()
            return
        }

        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastInterstitialShownTime) / 1000

        if (elapsedSeconds < AppConfig.INTERSTITIAL_COOLDOWN_SECONDS) {
            Log.d(TAG, "[Interstitial Ad] Skipped due to cooldown ($elapsedSeconds / ${AppConfig.INTERSTITIAL_COOLDOWN_SECONDS}s)")
            onComplete()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "[Interstitial Ad] Ad not ready yet; preloading for future transitions.")
            preloadInterstitialAd(activity.applicationContext)
            onComplete()
            return
        }

        val callbackDispatched = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "[Interstitial Ad] Showed full screen content.")
                isShowingAd = true
                lastInterstitialShownTime = System.currentTimeMillis()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logAdError("Interstitial Ad", adError)
                isShowingAd = false
                interstitialAd = null
                preloadInterstitialAd(activity.applicationContext)
                if (callbackDispatched.compareAndSet(false, true)) {
                    onComplete()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "[Interstitial Ad] Dismissed.")
                isShowingAd = false
                interstitialAd = null
                preloadInterstitialAd(activity.applicationContext)
                if (callbackDispatched.compareAndSet(false, true)) {
                    onComplete()
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "[Interstitial Ad] Impression recorded.")
            }

            override fun onAdClicked() {
                Log.d(TAG, "[Interstitial Ad] Click recorded.")
            }
        }

        Log.d(TAG, "[Interstitial Ad] Show started.")
        ad.show(activity)
    }

    // =========================================================================
    // APP OPEN AD MANAGEMENT (WITH 4-HOUR EXPIRATION HANDLING)
    // =========================================================================

    /**
     * Preloads an App Open Ad into memory.
     */
    fun preloadAppOpenAd(context: Context) {
        if (isAppOpenAdAvailable() || isAppOpenAdLoading) {
            return
        }

        val adUnitId = AppConfig.APP_OPEN_AD_UNIT_ID
        isAppOpenAdLoading = true
        Log.d(TAG, "[App Open Ad] Load started with adUnitId: $adUnitId")

        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context.applicationContext,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "[App Open Ad] App Open Ad loaded successfully.")
                    appOpenAd = ad
                    appOpenAdLoadTime = Date().time
                    isAppOpenAdLoading = false
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    logLoadAdError("App Open Ad", loadAdError)
                    appOpenAd = null
                    isAppOpenAdLoading = false
                }
            }
        )
    }

    /**
     * Utility method to check if App Open Ad was loaded less than 4 hours ago.
     */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = Date().time - appOpenAdLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /**
     * Checks if an App Open Ad is available and not expired.
     */
    fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Shows the App Open Ad when the application comes to the foreground, if available.
     */
    fun showAppOpenAdIfAvailable(
        activity: Activity,
        onComplete: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || isShowingAd) {
            onComplete()
            return
        }

        if (!isAppOpenAdAvailable()) {
            Log.d(TAG, "[App Open Ad] Not available or expired; preloading next.")
            preloadAppOpenAd(activity.applicationContext)
            onComplete()
            return
        }

        val ad = appOpenAd ?: run {
            onComplete()
            return
        }

        val callbackDispatched = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "[App Open Ad] Showed full screen content.")
                isShowingAd = true
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                logAdError("App Open Ad", adError)
                isShowingAd = false
                appOpenAd = null
                preloadAppOpenAd(activity.applicationContext)
                if (callbackDispatched.compareAndSet(false, true)) {
                    onComplete()
                }
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "[App Open Ad] Dismissed.")
                isShowingAd = false
                appOpenAd = null
                preloadAppOpenAd(activity.applicationContext)
                if (callbackDispatched.compareAndSet(false, true)) {
                    onComplete()
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "[App Open Ad] Impression recorded.")
            }

            override fun onAdClicked() {
                Log.d(TAG, "[App Open Ad] Click recorded.")
            }
        }

        Log.d(TAG, "[App Open Ad] Show started.")
        ad.show(activity)
    }
}
