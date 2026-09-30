package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.ScannerCornerColor
import com.example.ui.theme.ScannerLaserColor
import com.example.ui.theme.ScannerOverlayDark
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerView(
    modifier: Modifier = Modifier,
    isScanningActive: Boolean = true,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }

                val image = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val rawValue = barcodes[0].rawValue ?: barcodes[0].displayValue
                            if (!rawValue.isNullOrBlank()) {
                                onQrCodeDetected(rawValue)
                            } else {
                                errorMessage = "No readable QR code found in selected image."
                            }
                        } else {
                            errorMessage = "No QR code detected in this photo. Try another image."
                        }
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Failed to scan photo: ${e.localizedMessage}"
                    }
            } catch (e: Exception) {
                errorMessage = "Could not load image: ${e.localizedMessage}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("qr_scanner_view")
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview View
            CameraPreview(
                lifecycleOwner = lifecycleOwner,
                isScanningActive = isScanningActive,
                onCameraReady = { camera ->
                    cameraInstance = camera
                },
                onQrCodeDetected = onQrCodeDetected
            )

            // Scanning Viewfinder Overlay with Animated Laser Beam
            ScannerOverlay(
                isScanningActive = isScanningActive
            )

            // Flashlight and Gallery Control Bar at the top/bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flashlight Button
                FilledIconButton(
                    onClick = {
                        val newTorch = !isFlashlightOn
                        cameraInstance?.cameraControl?.enableTorch(newTorch)
                        isFlashlightOn = newTorch
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isFlashlightOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("flashlight_button")
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flashlight",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Scan From Gallery Button
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .testTag("scan_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Scan Image",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            // Permission Denied / Rationale View
            PermissionDeniedView(
                shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                onRequestPermission = {
                    cameraPermissionState.launchPermissionRequest()
                },
                onPickFromGallery = {
                    galleryLauncher.launch("image/*")
                }
            )
        }

        // Error message popup if gallery scan fails
        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(
                        onClick = { errorMessage = null },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isScanningActive: Boolean,
    onCameraReady: (Camera) -> Unit,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    var isProcessing = false

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy: ImageProxy ->
                        if (!isScanningActive || isProcessing) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            isProcessing = true
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty() && isScanningActive) {
                                        val barcode = barcodes[0]
                                        val raw = barcode.rawValue ?: barcode.displayValue
                                        if (!raw.isNullOrBlank()) {
                                            onQrCodeDetected(raw)
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                    isProcessing = false
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(camera)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera binding failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
private fun ScannerOverlay(
    isScanningActive: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Square scan box in the center (about 70% of screen width, max 300dp)
        val boxDimension = (canvasWidth * 0.72f).coerceAtMost(320.dp.toPx())
        val left = (canvasWidth - boxDimension) / 2f
        val top = (canvasHeight - boxDimension) / 2.3f
        val right = left + boxDimension
        val bottom = top + boxDimension

        // Draw dark semi-transparent overlay around the scan window
        val path = Path().apply {
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
            addRoundRect(
                RoundRect(
                    rect = Rect(left, top, right, bottom),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            )
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        drawPath(path = path, color = ScannerOverlayDark)

        // Draw glowing laser line if scanning is active
        if (isScanningActive) {
            val laserY = top + (boxDimension * laserFraction)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ScannerLaserColor.copy(alpha = 0.9f),
                        ScannerLaserColor,
                        ScannerLaserColor.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    startX = left + 10.dp.toPx(),
                    endX = right - 10.dp.toPx()
                ),
                start = Offset(left + 10.dp.toPx(), laserY),
                end = Offset(right - 10.dp.toPx(), laserY),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw high-tech corner brackets
        val cornerLength = 36.dp.toPx()
        val cornerStroke = 4.5.dp.toPx()
        val cornerRadius = 24.dp.toPx()
        val cornerColor = ScannerCornerColor

        // Top-Left Corner
        drawPath(
            path = Path().apply {
                moveTo(left, top + cornerLength)
                lineTo(left, top + cornerRadius)
                quadraticTo(left, top, left + cornerRadius, top)
                lineTo(left + cornerLength, top)
            },
            color = cornerColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Top-Right Corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, top)
                lineTo(right - cornerRadius, top)
                quadraticTo(right, top, right, top + cornerRadius)
                lineTo(right, top + cornerLength)
            },
            color = cornerColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Bottom-Left Corner
        drawPath(
            path = Path().apply {
                moveTo(left, bottom - cornerLength)
                lineTo(left, bottom - cornerRadius)
                quadraticTo(left, bottom, left + cornerRadius, bottom)
                lineTo(left + cornerLength, bottom)
            },
            color = cornerColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )

        // Bottom-Right Corner
        drawPath(
            path = Path().apply {
                moveTo(right - cornerLength, bottom)
                lineTo(right - cornerRadius, bottom)
                quadraticTo(right, bottom, right, bottom - cornerRadius)
                lineTo(right, bottom - cornerLength)
            },
            color = cornerColor,
            style = Stroke(width = cornerStroke, cap = StrokeCap.Round)
        )
    }

    // Hint text above viewfinder
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Align QR code within the frame to scan automatically",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionDeniedView(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Camera Permission Needed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (shouldShowRationale) {
                        "QR Scanner needs camera access to scan QR codes in real-time. Please grant permission to continue."
                    } else {
                        "Camera permission is required to scan QR codes and barcodes directly with your device's camera."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_camera_permission_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onPickFromGallery,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scan_gallery_fallback_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan from Gallery Image")
                }
            }
        }
    }
}
