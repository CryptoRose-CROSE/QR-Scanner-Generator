package com.example.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.models.WifiSecurity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

object QrCodeGenerator {

    /**
     * Generates a QR Code Bitmap with specified dimensions and colors.
     */
    fun generateBitmap(
        content: String,
        size: Int = 600,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE,
        errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M
    ): Bitmap {
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, errorCorrection)
            put(EncodeHintType.MARGIN, 2)
        }

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) darkColor else lightColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    // Helper builders for supported QR data formats

    fun buildWifiString(ssid: String, password: String, security: WifiSecurity, hidden: Boolean = false): String {
        val escapedSsid = escapeWifi(ssid)
        val escapedPass = escapeWifi(password)
        val sec = when (security) {
            WifiSecurity.NONE -> "nopass"
            WifiSecurity.WEP -> "WEP"
            WifiSecurity.WPA_WPA2 -> "WPA"
            WifiSecurity.WPA3 -> "WPA3"
        }
        val hiddenFlag = if (hidden) "H:true;" else ""
        return "WIFI:S:$escapedSsid;T:$sec;P:$escapedPass;$hiddenFlag;"
    }

    fun buildContactVCard(name: String, phone: String, email: String, org: String): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (name.isNotBlank()) appendLine("FN:$name")
            if (name.isNotBlank()) appendLine("N:$name;;;;")
            if (phone.isNotBlank()) appendLine("TEL;TYPE=CELL:$phone")
            if (email.isNotBlank()) appendLine("EMAIL;TYPE=INTERNET:$email")
            if (org.isNotBlank()) appendLine("ORG:$org")
            append("END:VCARD")
        }
    }

    fun buildEmailString(email: String, subject: String, body: String): String {
        val encodedSub = android.net.Uri.encode(subject)
        val encodedBody = android.net.Uri.encode(body)
        return "mailto:$email?subject=$encodedSub&body=$encodedBody"
    }

    fun buildSmsString(phone: String, message: String): String {
        return if (message.isNotBlank()) {
            "smsto:$phone:$message"
        } else {
            "smsto:$phone"
        }
    }

    fun buildPhoneString(phone: String): String {
        return "tel:$phone"
    }

    fun buildUrlString(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun escapeWifi(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace(":", "\\:")
            .replace("\"", "\\\"")
    }
}
