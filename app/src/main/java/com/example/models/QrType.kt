package com.example.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

enum class QrType(
    val title: String,
    val description: String
) {
    TEXT("Text", "Plain text message or note"),
    URL("URL / Website", "Web address or link"),
    WIFI("Wi-Fi Network", "Wireless network credentials"),
    CONTACT("Contact (vCard)", "Business contact or card"),
    PHONE("Phone Number", "Direct dial phone number"),
    EMAIL("Email", "Email address and message"),
    SMS("SMS Message", "SMS recipient and message body"),
    GEO("Location", "Geographical map coordinates"),
    OTHER("Barcode / Other", "Standard barcode or raw data");

    fun getIcon(): ImageVector = when (this) {
        TEXT -> Icons.Default.TextFields
        URL -> Icons.Default.Language
        WIFI -> Icons.Default.Wifi
        CONTACT -> Icons.Default.ContactPage
        PHONE -> Icons.Default.Phone
        EMAIL -> Icons.Default.AlternateEmail
        SMS -> Icons.AutoMirrored.Filled.Message
        GEO -> Icons.Default.LocationOn
        OTHER -> Icons.Default.QrCode
    }
}

enum class WifiSecurity(val raw: String, val displayName: String) {
    WPA_WPA2("WPA", "WPA / WPA2 (Recommended)"),
    WEP("WEP", "WEP (Legacy)"),
    NONE("nopass", "No Password (Open)"),
    WPA3("WPA3", "WPA3 (Modern)");

    companion object {
        fun fromString(type: String?): WifiSecurity {
            return when (type?.uppercase()) {
                "WPA", "WPA2", "WPA/WPA2" -> WPA_WPA2
                "WEP" -> WEP
                "NOPASS", "NONE", "" -> NONE
                "WPA3" -> WPA3
                else -> WPA_WPA2
            }
        }
    }
}
