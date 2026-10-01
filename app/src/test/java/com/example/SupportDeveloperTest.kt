package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.config.AppConfig
import com.example.ui.screens.BankDonationHelper
import com.example.ui.screens.BinanceDonationHelper
import com.example.ui.screens.DonationHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupportDeveloperTest {

    @Test
    fun testDonationUrlIsConfiguredAndValid() {
        val url = AppConfig.DONATION_URL
        assertNotNull("DONATION_URL must not be null", url)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handledSafely = try {
            DonationHandler.openDonationWebpage(context, url)
            true
        } catch (_: Exception) {
            false
        }
        assertTrue("DonationHandler must safely handle configured DONATION_URL without crashing", handledSafely)
    }

    @Test
    fun testBinanceConfigurationExists() {
        assertNotNull("BINANCE_PAYMENT_URL must be defined", AppConfig.BINANCE_PAYMENT_URL)
        assertTrue("BINANCE_PAYMENT_URL must not be blank", AppConfig.BINANCE_PAYMENT_URL.isNotBlank())

        assertNotNull("BINANCE_PAY_ID must be defined", AppConfig.BINANCE_PAY_ID)
        assertTrue("BINANCE_PAY_ID must not be blank", AppConfig.BINANCE_PAY_ID.isNotBlank())

        assertNotNull("BINANCE_ID must be defined", AppConfig.BINANCE_ID)
        assertTrue("BINANCE_ID must not be blank", AppConfig.BINANCE_ID.isNotBlank())
    }

    @Test
    fun testBankTransferConfigurationExists() {
        assertNotNull("BANK_NAME must be defined", AppConfig.BANK_NAME)
        assertTrue("BANK_NAME must not be blank", AppConfig.BANK_NAME.isNotBlank())

        assertNotNull("ACCOUNT_NAME must be defined", AppConfig.ACCOUNT_NAME)
        assertTrue("ACCOUNT_NAME must not be blank", AppConfig.ACCOUNT_NAME.isNotBlank())

        assertNotNull("ACCOUNT_NUMBER must be defined", AppConfig.ACCOUNT_NUMBER)
        assertTrue("ACCOUNT_NUMBER must not be blank", AppConfig.ACCOUNT_NUMBER.isNotBlank())

        assertNotNull("BRANCH_NAME must be defined", AppConfig.BRANCH_NAME)
        assertTrue("BRANCH_NAME must not be blank", AppConfig.BRANCH_NAME.isNotBlank())

        assertNotNull("ROUTING_NUMBER must be defined", AppConfig.ROUTING_NUMBER)
        assertTrue("ROUTING_NUMBER must not be blank", AppConfig.ROUTING_NUMBER.isNotBlank())
    }

    @Test
    fun testNoPersonalSensitiveInformationInAllConfigs() {
        val allConfigValues = listOf(
            AppConfig.BINANCE_PAYMENT_URL,
            AppConfig.BINANCE_PAY_ID,
            AppConfig.BINANCE_ID,
            AppConfig.BANK_NAME,
            AppConfig.ACCOUNT_NAME,
            AppConfig.ACCOUNT_NUMBER,
            AppConfig.BRANCH_NAME,
            AppConfig.ROUTING_NUMBER,
            AppConfig.DONATION_URL
        ).map { it.lowercase() }

        val forbiddenCredentials = listOf(
            "password", "pin", "otp", "cvv", "secret_key",
            "private_key", "seed_phrase", "mnemonic", "token"
        )
        for (value in allConfigValues) {
            for (cred in forbiddenCredentials) {
                assertFalse("Config value '$value' should not contain sensitive credential '$cred'", value.contains(cred))
            }
        }
    }

    @Test
    fun testCopyBinanceIdCopiesToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = BinanceDonationHelper.copyBinanceId(context, AppConfig.BINANCE_ID)
        assertTrue("Copying Binance ID should return true", result)

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        assertNotNull("ClipboardManager should be available", clipboardManager)
        val clip = clipboardManager?.primaryClip
        assertNotNull("Clipboard clip should not be null", clip)
        assertEquals(1, clip?.itemCount)
        assertEquals(AppConfig.BINANCE_ID, clip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun testCopyBinancePayIdCopiesToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = BinanceDonationHelper.copyBinancePayId(context, AppConfig.BINANCE_PAY_ID)
        assertTrue("Copying Binance Pay ID should return true", result)

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        assertNotNull("ClipboardManager should be available", clipboardManager)
        val clip = clipboardManager?.primaryClip
        assertNotNull("Clipboard clip should not be null", clip)
        assertEquals(1, clip?.itemCount)
        assertEquals(AppConfig.BINANCE_PAY_ID, clip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun testBinanceDonationHelperRejectsPlaceholderOrEmptyUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val emptyResult = BinanceDonationHelper.openBinanceDonation(context, "")
        assertFalse("Empty Binance URL must return false and not crash", emptyResult)

        val placeholderResult = BinanceDonationHelper.openBinanceDonation(context, "https://example.com/binance")
        assertFalse("Placeholder Binance URL must return false and not crash", placeholderResult)

        val invalidResult = BinanceDonationHelper.openBinanceDonation(context, "javascript:alert(1)")
        assertFalse("Javascript scheme must return false and not crash", invalidResult)
    }

    @Test
    fun testBinanceDonationHelperAcceptsValidHttpsUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validResult = BinanceDonationHelper.openBinanceDonation(context, "https://app.binance.com/qr/pay?id=12345")
        assertTrue("Valid https Binance URL should succeed with ACTION_VIEW", validResult)
    }

    @Test
    fun testCopyAccountNumberCopiesToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = BankDonationHelper.copyAccountNumber(context, AppConfig.ACCOUNT_NUMBER)
        assertTrue("Copying account number should return true", result)

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        assertNotNull("ClipboardManager should be available", clipboardManager)
        val clip = clipboardManager?.primaryClip
        assertNotNull("Clipboard clip should not be null", clip)
        assertEquals(1, clip?.itemCount)
        assertEquals(AppConfig.ACCOUNT_NUMBER, clip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun testCopyRoutingNumberCopiesToClipboard() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = BankDonationHelper.copyRoutingNumber(context, AppConfig.ROUTING_NUMBER)
        assertTrue("Copying routing number should return true", result)

        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        assertNotNull("ClipboardManager should be available", clipboardManager)
        val clip = clipboardManager?.primaryClip
        assertNotNull("Clipboard clip should not be null", clip)
        assertEquals(1, clip?.itemCount)
        assertEquals(AppConfig.ROUTING_NUMBER, clip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun testDonationHandlerRejectsEmptyUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = DonationHandler.openDonationWebpage(context, "")
        assertFalse("Empty URL must return false and not crash", result)

        val whitespaceResult = DonationHandler.openDonationWebpage(context, "   ")
        assertFalse("Whitespace URL must return false and not crash", whitespaceResult)
    }

    @Test
    fun testDonationHandlerRejectsInvalidScheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invalidResult = DonationHandler.openDonationWebpage(context, "javascript:alert(1)")
        assertFalse("Javascript scheme must return false and not crash", invalidResult)

        val malformedResult = DonationHandler.openDonationWebpage(context, "not-a-valid-url")
        assertFalse("Malformed URL must return false and not crash", malformedResult)
    }

    @Test
    fun testDonationHandlerAcceptsValidUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validResult = DonationHandler.openDonationWebpage(context, "https://example.com/donate")
        assertTrue("Valid https donation URL should succeed with ACTION_VIEW", validResult)
    }
}
