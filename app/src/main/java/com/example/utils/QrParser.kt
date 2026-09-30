package com.example.utils

import android.net.Uri
import com.example.models.QrParsedResult
import com.example.models.QrType
import com.example.models.WifiSecurity

object QrParser {

    fun parse(raw: String): QrParsedResult {
        val trimmed = raw.trim()

        // 1. Wi-Fi
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            return parseWifi(trimmed)
        }

        // 2. URL
        if (isLikelyUrl(trimmed)) {
            return parseUrl(trimmed)
        }

        // 3. Contact (vCard / MeCard)
        if (trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) || trimmed.startsWith("MECARD:", ignoreCase = true)) {
            return parseContact(trimmed)
        }

        // 4. Phone
        if (trimmed.startsWith("tel:", ignoreCase = true) || (trimmed.startsWith("+") && trimmed.length > 6 && trimmed.drop(1).all { it.isDigit() || it == ' ' || it == '-' })) {
            val phone = if (trimmed.startsWith("tel:", ignoreCase = true)) trimmed.substring(4) else trimmed
            return QrParsedResult(
                type = QrType.PHONE,
                title = "Phone: $phone",
                rawContent = trimmed,
                phoneNumber = phone,
                displayDetails = listOf("Phone Number" to phone)
            )
        }

        // 5. Email
        if (trimmed.startsWith("mailto:", ignoreCase = true) || trimmed.startsWith("MATMSG:", ignoreCase = true)) {
            return parseEmail(trimmed)
        }

        // 6. SMS
        if (trimmed.startsWith("sms:", ignoreCase = true) || trimmed.startsWith("smsto:", ignoreCase = true)) {
            return parseSms(trimmed)
        }

        // 7. Geo
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            return parseGeo(trimmed)
        }

        // Default Plain Text
        val preview = if (trimmed.length > 50) trimmed.take(47) + "..." else trimmed
        return QrParsedResult(
            type = QrType.TEXT,
            title = "Text Note",
            rawContent = trimmed,
            displayDetails = listOf("Content" to trimmed)
        )
    }

    private fun isLikelyUrl(text: String): Boolean {
        val lower = text.lowercase()
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return true
        }
        if (lower.startsWith("www.") && lower.contains(".")) {
            return true
        }
        return false
    }

    private fun parseUrl(raw: String): QrParsedResult {
        val url = if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
            "https://$raw"
        } else {
            raw
        }

        val domain = try {
            val host = Uri.parse(url).host
            host ?: url
        } catch (_: Exception) {
            url
        }

        return QrParsedResult(
            type = QrType.URL,
            title = domain ?: url,
            rawContent = raw,
            url = url,
            displayDetails = listOf(
                "Web Address" to url,
                "Domain" to (domain ?: "Unknown")
            )
        )
    }

    private fun parseWifi(raw: String): QrParsedResult {
        // Format: WIFI:S:MySSID;T:WPA;P:MyPassword;H:false;;
        var ssid = ""
        var password = ""
        var type = "WPA"
        var hidden = false

        val tokens = raw.removePrefix("WIFI:").removePrefix("wifi:").split(";")
        for (token in tokens) {
            when {
                token.startsWith("S:", ignoreCase = true) -> ssid = token.substring(2)
                token.startsWith("P:", ignoreCase = true) -> password = token.substring(2)
                token.startsWith("T:", ignoreCase = true) -> type = token.substring(2)
                token.startsWith("H:", ignoreCase = true) -> hidden = token.substring(2).toBoolean()
            }
        }

        val security = WifiSecurity.fromString(type)
        val details = mutableListOf<Pair<String, String>>()
        details.add("Network (SSID)" to (ssid.ifEmpty { "Hidden SSID" }))
        details.add("Security" to security.displayName)
        if (password.isNotEmpty()) {
            details.add("Password" to password)
        }
        if (hidden) {
            details.add("Hidden Network" to "Yes")
        }

        return QrParsedResult(
            type = QrType.WIFI,
            title = if (ssid.isNotEmpty()) "Wi-Fi: $ssid" else "Wi-Fi Network",
            rawContent = raw,
            wifiSsid = ssid,
            wifiPassword = password,
            wifiSecurity = security,
            displayDetails = details
        )
    }

    private fun parseContact(raw: String): QrParsedResult {
        var name = ""
        var phone = ""
        var email = ""
        var org = ""

        if (raw.startsWith("MECARD:", ignoreCase = true)) {
            val tokens = raw.removePrefix("MECARD:").removePrefix("mecard:").split(";")
            for (token in tokens) {
                when {
                    token.startsWith("N:", ignoreCase = true) -> name = token.substring(2).replace(",", " ")
                    token.startsWith("TEL:", ignoreCase = true) -> phone = token.substring(4)
                    token.startsWith("EMAIL:", ignoreCase = true) -> email = token.substring(6)
                    token.startsWith("ORG:", ignoreCase = true) -> org = token.substring(4)
                }
            }
        } else {
            // vCard
            val lines = raw.lines()
            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("FN:", ignoreCase = true) -> name = trimmedLine.substring(3)
                    trimmedLine.startsWith("N:", ignoreCase = true) && name.isEmpty() -> {
                        name = trimmedLine.substring(2).split(";").filter { it.isNotEmpty() }.reversed().joinToString(" ")
                    }
                    trimmedLine.startsWith("TEL", ignoreCase = true) -> {
                        phone = trimmedLine.substringAfter(":")
                    }
                    trimmedLine.startsWith("EMAIL", ignoreCase = true) -> {
                        email = trimmedLine.substringAfter(":")
                    }
                    trimmedLine.startsWith("ORG:", ignoreCase = true) -> {
                        org = trimmedLine.substring(4)
                    }
                }
            }
        }

        val details = mutableListOf<Pair<String, String>>()
        if (name.isNotEmpty()) details.add("Name" to name)
        if (phone.isNotEmpty()) details.add("Phone" to phone)
        if (email.isNotEmpty()) details.add("Email" to email)
        if (org.isNotEmpty()) details.add("Organization" to org)

        return QrParsedResult(
            type = QrType.CONTACT,
            title = if (name.isNotEmpty()) name else "Contact Card",
            rawContent = raw,
            contactName = name,
            contactPhone = phone,
            contactEmail = email,
            contactOrg = org,
            displayDetails = if (details.isNotEmpty()) details else listOf("Raw Card" to raw)
        )
    }

    private fun parseEmail(raw: String): QrParsedResult {
        var email = ""
        var subject = ""
        var body = ""

        if (raw.startsWith("mailto:", ignoreCase = true)) {
            val withoutScheme = raw.substring(7)
            email = withoutScheme.substringBefore("?")
            if (withoutScheme.contains("?")) {
                val query = withoutScheme.substringAfter("?")
                val params = query.split("&")
                for (param in params) {
                    val key = param.substringBefore("=").lowercase()
                    val rawVal = param.substringAfter("=")
                    val value = try {
                        Uri.decode(rawVal)
                    } catch (_: Exception) {
                        rawVal
                    }
                    when (key) {
                        "subject" -> subject = value
                        "body" -> body = value
                    }
                }
            }
        } else if (raw.startsWith("MATMSG:", ignoreCase = true)) {
            val tokens = raw.removePrefix("MATMSG:").removePrefix("matmsg:").split(";")
            for (token in tokens) {
                when {
                    token.startsWith("TO:", ignoreCase = true) -> email = token.substring(3)
                    token.startsWith("SUB:", ignoreCase = true) -> subject = token.substring(4)
                    token.startsWith("BODY:", ignoreCase = true) -> body = token.substring(5)
                }
            }
        }

        val details = mutableListOf<Pair<String, String>>()
        if (email.isNotEmpty()) details.add("Recipient" to email)
        if (subject.isNotEmpty()) details.add("Subject" to subject)
        if (body.isNotEmpty()) details.add("Message Body" to body)

        return QrParsedResult(
            type = QrType.EMAIL,
            title = if (email.isNotEmpty()) "Email: $email" else "Email Message",
            rawContent = raw,
            email = email,
            emailSubject = subject,
            emailBody = body,
            displayDetails = details
        )
    }

    private fun parseSms(raw: String): QrParsedResult {
        var phone = ""
        var body = ""

        val cleaned = raw.removePrefix("smsto:").removePrefix("SMSTO:").removePrefix("sms:").removePrefix("SMS:")
        if (cleaned.contains(":")) {
            phone = cleaned.substringBefore(":")
            body = cleaned.substringAfter(":")
        } else if (cleaned.contains("?body=")) {
            phone = cleaned.substringBefore("?body=")
            body = try {
                Uri.decode(cleaned.substringAfter("?body="))
            } catch (_: Exception) {
                cleaned.substringAfter("?body=")
            }
        } else {
            phone = cleaned
        }

        val details = mutableListOf<Pair<String, String>>()
        if (phone.isNotEmpty()) details.add("Recipient" to phone)
        if (body.isNotEmpty()) details.add("Message" to body)

        return QrParsedResult(
            type = QrType.SMS,
            title = if (phone.isNotEmpty()) "SMS to $phone" else "SMS Message",
            rawContent = raw,
            smsNumber = phone,
            smsBody = body,
            displayDetails = details
        )
    }

    private fun parseGeo(raw: String): QrParsedResult {
        val coords = raw.removePrefix("geo:").removePrefix("GEO:").substringBefore("?")
        val parts = coords.split(",")
        val lat = parts.getOrNull(0)?.toDoubleOrNull()
        val lng = parts.getOrNull(1)?.toDoubleOrNull()

        return QrParsedResult(
            type = QrType.GEO,
            title = if (lat != null && lng != null) "Location ($lat, $lng)" else "Map Location",
            rawContent = raw,
            latitude = lat,
            longitude = lng,
            displayDetails = listOf(
                "Coordinates" to coords,
                "Latitude" to (lat?.toString() ?: "N/A"),
                "Longitude" to (lng?.toString() ?: "N/A")
            )
        )
    }
}
