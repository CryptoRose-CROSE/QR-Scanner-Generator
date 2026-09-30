package com.example.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QRApplication
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.data.local.QrItem
import com.example.models.QrType
import com.example.models.WifiSecurity
import com.example.ui.components.QrPreviewBottomSheet
import com.example.utils.QrCodeGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val qrRepository = QRApplication.instance.qrRepository

    var selectedType by remember { mutableStateOf(QrType.TEXT) }

    // Form states
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var emailSubjectInput by remember { mutableStateOf("") }
    var emailBodyInput by remember { mutableStateOf("") }
    var smsPhoneInput by remember { mutableStateOf("") }
    var smsBodyInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf(WifiSecurity.WPA_WPA2) }
    var isWifiHidden by remember { mutableStateOf(false) }
    var isWifiPasswordVisible by remember { mutableStateOf(false) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }

    var inputError by remember { mutableStateOf<String?>(null) }
    var adStatusNotice by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    // Generated result state
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var generatedRawContent by remember { mutableStateOf("") }
    var generatedTitle by remember { mutableStateOf("") }

    fun buildContentAndValidate(): Pair<String, String>? {
        inputError = null
        adStatusNotice = null

        return when (selectedType) {
            QrType.TEXT -> {
                if (textInput.isBlank()) {
                    inputError = "Please enter text to encode."
                    null
                } else {
                    val title = if (textInput.length > 30) textInput.take(27) + "..." else textInput
                    textInput.trim() to title
                }
            }
            QrType.URL -> {
                val trimmed = urlInput.trim()
                if (trimmed.isBlank()) {
                    inputError = "Please enter a website URL."
                    null
                } else if (!trimmed.contains(".") || trimmed.length < 4) {
                    inputError = "Please enter a valid website address (e.g. example.com or https://...)."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildUrlString(trimmed)
                    encoded to "Web: $trimmed"
                }
            }
            QrType.PHONE -> {
                val trimmed = phoneInput.trim()
                if (trimmed.isBlank()) {
                    inputError = "Please enter a phone number."
                    null
                } else if (trimmed.length < 3 || !trimmed.any { it.isDigit() }) {
                    inputError = "Please enter a valid phone number with digits."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildPhoneString(trimmed)
                    encoded to "Phone: $trimmed"
                }
            }
            QrType.EMAIL -> {
                val trimmed = emailInput.trim()
                if (trimmed.isBlank() || !trimmed.contains("@") || !trimmed.contains(".")) {
                    inputError = "Please enter a valid email address (e.g. name@domain.com)."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildEmailString(trimmed, emailSubjectInput.trim(), emailBodyInput.trim())
                    encoded to "Email: $trimmed"
                }
            }
            QrType.SMS -> {
                val trimmed = smsPhoneInput.trim()
                if (trimmed.isBlank()) {
                    inputError = "Please enter recipient phone number."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildSmsString(trimmed, smsBodyInput.trim())
                    encoded to "SMS: $trimmed"
                }
            }
            QrType.WIFI -> {
                val trimmed = wifiSsid.trim()
                if (trimmed.isBlank()) {
                    inputError = "Please enter Wi-Fi network name (SSID)."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildWifiString(trimmed, wifiPassword.trim(), wifiSecurity, isWifiHidden)
                    encoded to "Wi-Fi: $trimmed"
                }
            }
            QrType.CONTACT -> {
                val name = contactName.trim()
                val phone = contactPhone.trim()
                if (name.isBlank() && phone.isBlank()) {
                    inputError = "Please enter at least a contact name or phone number."
                    null
                } else {
                    val encoded = QrCodeGenerator.buildContactVCard(name, phone, contactEmail.trim(), contactOrg.trim())
                    val title = name.ifBlank { phone }
                    encoded to "Contact: $title"
                }
            }
            else -> {
                if (textInput.isBlank()) {
                    inputError = "Please enter data to encode."
                    null
                } else {
                    textInput.trim() to "QR Code"
                }
            }
        }
    }

    fun executeGeneration(content: String, title: String) {
        coroutineScope.launch {
            try {
                isGenerating = true
                val finalContent = content
                val finalTitle = title

                // Generate high-resolution 1024x1024 QR bitmap
                val bitmap = QrCodeGenerator.generateBitmap(content = finalContent, size = 1024)
                generatedBitmap = bitmap
                generatedRawContent = finalContent
                generatedTitle = finalTitle
                adStatusNotice = null

                // Save generated item to Room Database
                qrRepository.insertItem(
                    QrItem(
                        content = finalContent,
                        qrType = selectedType.name,
                        title = finalTitle,
                        isGenerated = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                inputError = "Failed to generate QR code: ${e.localizedMessage}"
            } finally {
                isGenerating = false
            }
        }
    }

    fun onGenerateClick() {
        if (isGenerating || AdManager.isShowingAd) return

        val validPair = buildContentAndValidate() ?: return
        val (content, title) = validPair

        if (activity != null) {
            if (!AdManager.isRewardedAdReady()) {
                adStatusNotice = "Rewarded ad is not ready yet. Please try again."
                Toast.makeText(
                    context,
                    "Rewarded ad is not ready yet. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
                AdManager.preloadRewardedAd(context)
                return
            }

            isGenerating = true
            adStatusNotice = null

            AdManager.showRewardedAdForGeneration(
                activity = activity,
                onRewardEarned = {
                    executeGeneration(content, title)
                },
                onDismissedWithoutReward = {
                    isGenerating = false
                    adStatusNotice = "Please watch the full rewarded ad to generate your QR code."
                    Toast.makeText(
                        context,
                        "Please complete the rewarded ad to generate your QR code.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onAdFailed = {
                    isGenerating = false
                    adStatusNotice = "Rewarded ad is not ready yet. Please try again."
                    Toast.makeText(
                        context,
                        "Rewarded ad is not ready yet. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        } else {
            adStatusNotice = "Rewarded ad is not ready yet. Please try again."
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("generate_screen_root"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create QR Code",
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
            // QR Type Selector Horizontal Chips
            Text(
                text = "Select QR Type",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    QrType.TEXT,
                    QrType.URL,
                    QrType.WIFI,
                    QrType.CONTACT,
                    QrType.PHONE,
                    QrType.EMAIL,
                    QrType.SMS
                )

                types.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedType = type
                            inputError = null
                            adStatusNotice = null
                        },
                        label = { Text(type.title) },
                        leadingIcon = {
                            Icon(
                                imageVector = type.getIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("chip_${type.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Input Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = selectedType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    when (selectedType) {
                        QrType.TEXT -> {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it; inputError = null; adStatusNotice = null },
                                label = { Text("Text Message / Note") },
                                placeholder = { Text("Enter any text...") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_text"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        QrType.URL -> {
                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { urlInput = it; inputError = null; adStatusNotice = null },
                                label = { Text("Website URL") },
                                placeholder = { Text("example.com or https://...") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_url"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        QrType.PHONE -> {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it; inputError = null; adStatusNotice = null },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+1 234 567 8900") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_phone"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        QrType.EMAIL -> {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it; inputError = null; adStatusNotice = null },
                                label = { Text("Email Address *") },
                                placeholder = { Text("name@example.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_email"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailSubjectInput,
                                onValueChange = { emailSubjectInput = it },
                                label = { Text("Subject (Optional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_email_subject"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailBodyInput,
                                onValueChange = { emailBodyInput = it },
                                label = { Text("Message Body (Optional)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_email_body"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        QrType.SMS -> {
                            OutlinedTextField(
                                value = smsPhoneInput,
                                onValueChange = { smsPhoneInput = it; inputError = null; adStatusNotice = null },
                                label = { Text("Recipient Phone *") },
                                placeholder = { Text("+1 234 567 8900") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sms_phone"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = smsBodyInput,
                                onValueChange = { smsBodyInput = it },
                                label = { Text("Message Text (Optional)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sms_body"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        QrType.WIFI -> {
                            var securityMenuExpanded by remember { mutableStateOf(false) }

                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it; inputError = null; adStatusNotice = null },
                                label = { Text("Network Name (SSID) *") },
                                placeholder = { Text("Home_WiFi") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_wifi_ssid"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Security Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = wifiSecurity.displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Security Type") },
                                    trailingIcon = {
                                        IconButton(onClick = { securityMenuExpanded = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Security")
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_wifi_security"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(
                                    expanded = securityMenuExpanded,
                                    onDismissRequest = { securityMenuExpanded = false }
                                ) {
                                    WifiSecurity.values().forEach { sec ->
                                        DropdownMenuItem(
                                            text = { Text(sec.displayName) },
                                            onClick = {
                                                wifiSecurity = sec
                                                securityMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (wifiSecurity != WifiSecurity.NONE) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = wifiPassword,
                                    onValueChange = { wifiPassword = it },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = if (isWifiPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isWifiPasswordVisible = !isWifiPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isWifiPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Password Visibility"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_wifi_password"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hidden Network",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Switch(
                                    checked = isWifiHidden,
                                    onCheckedChange = { isWifiHidden = it },
                                    modifier = Modifier.testTag("switch_wifi_hidden")
                                )
                            }
                        }
                        QrType.CONTACT -> {
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it; inputError = null; adStatusNotice = null },
                                label = { Text("Full Name *") },
                                placeholder = { Text("John Doe") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_contact_name"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it; inputError = null; adStatusNotice = null },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+1 234 567 8900") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_contact_phone"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Email (Optional)") },
                                placeholder = { Text("john@example.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_contact_email"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = contactOrg,
                                onValueChange = { contactOrg = it },
                                label = { Text("Organization / Company (Optional)") },
                                placeholder = { Text("Acme Corp") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_contact_org"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        else -> {}
                    }

                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = inputError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (adStatusNotice != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = adStatusNotice ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Generate QR Button (triggers Rewarded Ad — strictly requiring completion)
                    Button(
                        onClick = { onGenerateClick() },
                        enabled = !isGenerating && !AdManager.isShowingAd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_qr_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating...")
                        } else {
                            Icon(Icons.Default.QrCode2, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generate QR Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner Ad at bottom
            BannerAdView()

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Preview bottom sheet when generated
        generatedBitmap?.let { bitmap ->
            QrPreviewBottomSheet(
                bitmap = bitmap,
                rawContent = generatedRawContent,
                qrType = selectedType,
                title = generatedTitle,
                onDismiss = {
                    generatedBitmap = null
                },
                onRegenerate = {
                    generatedBitmap = null
                }
            )
        }
    }
}
