package com.example.models

data class QrParsedResult(
    val type: QrType,
    val title: String,
    val rawContent: String,
    val displayDetails: List<Pair<String, String>> = emptyList(),
    // Specific parsed fields
    val url: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val emailSubject: String? = null,
    val emailBody: String? = null,
    val smsNumber: String? = null,
    val smsBody: String? = null,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiSecurity: WifiSecurity = WifiSecurity.WPA_WPA2,
    val contactName: String? = null,
    val contactOrg: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
