package com.example.config

import com.example.BuildConfig

/**
 * ============================================================================
 * CENTRALIZED ADMOB & APP CONFIGURATION — QR SCANNER & GENERATOR
 * ============================================================================
 *
 * DEBUG builds automatically use Google's official sample/test Ad Unit IDs
 * to prevent ERROR_CODE_NO_FILL (Error 3) and policy violations during testing.
 *
 * RELEASE builds automatically use the configured production Ad Unit IDs.
 * ============================================================================
 */
object AppConfig {

    // =========================================================================
    // 1. ADMOB CONFIGURATION (Official Google Test IDs vs Production IDs)
    // =========================================================================

    /** Official Google AdMob Test App ID & Ad Unit IDs for Android */
    const val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_APP_OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

    /** Production AdMob Application ID & Ad Unit IDs */
    const val PROD_ADMOB_APP_ID = "ca-app-pub-5899285579056432~4339771033"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-5899285579056432/5820373669"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-5899285579056432/9400526020"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5899285579056432/1713607694"
    const val PROD_APP_OPEN_AD_UNIT_ID = "ca-app-pub-5899285579056432/1200375872"

    /** Active AdMob Application ID (Official Test ID in DEBUG, Production ID in RELEASE) */
    val ADMOB_APP_ID: String
        get() = if (BuildConfig.DEBUG) TEST_ADMOB_APP_ID else PROD_ADMOB_APP_ID

    /** Active Banner Ad Unit ID (Official Google Test ID in DEBUG, Production ID in RELEASE) */
    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    /** Active Rewarded Ad Unit ID (Official Google Test ID in DEBUG, Production ID in RELEASE) */
    val REWARDED_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID

    /** Active Interstitial Ad Unit ID (Official Google Test ID in DEBUG, Production ID in RELEASE) */
    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    /** Active App Open Ad Unit ID (Official Google Test ID in DEBUG, Production ID in RELEASE) */
    val APP_OPEN_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_APP_OPEN_AD_UNIT_ID else PROD_APP_OPEN_AD_UNIT_ID

    /** Cooldown interval between Interstitial ads in seconds (minimum 45s) */
    const val INTERSTITIAL_COOLDOWN_SECONDS = 45L

    // =========================================================================
    // 2. SOCIAL MEDIA CONFIGURATION
    // =========================================================================

    const val X_URL = "https://x.com/QRScannerG"
    const val TELEGRAM_URL = "https://t.me/QRScannerGenerator"
    const val FACEBOOK_URL = "https://facebook.com/QRScannerGenerator"
    const val YOUTUBE_URL = "https://www.youtube.com/@QRScannerGenerator"
    const val WEBSITE_URL = "https://qrscannerg.blogspot.com"

    // =========================================================================
    // =========================================================================
    // 3. LEGAL & POLICY URLS
    // =========================================================================

    const val PRIVACY_POLICY_URL = "https://qrscannerg.blogspot.com/2026/08/privacy-policy-of-qrscanner-generator.html"
    const val TERMS_URL = "https://qrscannerg.blogspot.com/2026/08/qrscanner-generator-of-terms.html"

    // =========================================================================
    // 4. APP METADATA & STORE RATING
    // =========================================================================

    const val RATE_APP_PACKAGE_NAME = "com.aistudio.qrscannergen.kxmpzq"
    const val APP_VERSION = "1.0.0"
    const val APP_NAME = "QR Scanner & Generator"

    // =========================================================================
    // 5. DONATION & SUPPORT CONFIGURATION
    // =========================================================================

    // --- 🟡 Binance Donation Configuration ---
    const val BINANCE_PAYMENT_URL = "https://www.binance.com/register?ref=1035323924"
    const val BINANCE_PAY_ID = "1035323924"
    const val BINANCE_ID = "1035323924"

    // --- 🏦 Bank Transfer Donation Configuration ---
    /**
     * Bank transfer donation configuration placeholders.
     * Use placeholders only. Do not hard-code passwords, PINs, OTPs,
     * card details, API secrets, private keys, or bank login credentials.
     */
    const val BANK_NAME = "Islami Bank Bangladesh Limited"
    const val ACCOUNT_NAME = "Ziaur Rahman"
    const val ACCOUNT_NUMBER = "20501476700037911"
    const val BRANCH_NAME = "Teknaf"
    const val ROUTING_NUMBER = "125220910"

    /**
     * General externally hosted donation webpage URL.
     * Configure this with your platform link (e.g. Ko-fi, Buy Me a Coffee, GitHub Sponsors, PayPal.me).
     * DO NOT hard-code personal banking information.
     */
    const val DONATION_URL = "#"

    /**
     * Checks whether a given URL is a real configured URL rather than empty or root domain placeholder.
     */
    fun isUrlConfigured(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        val placeholderPatterns = listOf("https://example.com", "https://example.com/", "https://x.com/", "https://t.me/", "https://facebook.com/", "https://youtube.com/")
        return !placeholderPatterns.contains(trimmed) && trimmed.startsWith("http") && trimmed.length > 10
    }
}
