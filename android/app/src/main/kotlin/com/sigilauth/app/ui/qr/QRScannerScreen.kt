package com.wagmilabs.sigil.ui.qr

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.wagmilabs.sigil.ui.theme.SigilColors
import com.wagmilabs.sigil.ui.theme.SigilSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * QR code scanner screen using CameraX + ML Kit.
 *
 * Scans verified HTTPS App Links from sigilauth.com:
 * - https://sigilauth.com/register
 * - https://sigilauth.com/mnemonic-init
 * - https://sigilauth.com/challenge
 *
 * Per aria-a11y-requirements.md §2.1:
 * - Accessible with TalkBack
 * - Permission request with clear explanation
 *
 * AGPL-3.0 License
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onQRCodeScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        SmallTopAppBar(
            title = { Text("Scan QR Code") },
            navigationIcon = {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        )

        when {
            cameraPermissionState.status.isGranted -> {
                // Camera preview
                var scannedOnce by remember { mutableStateOf(false) }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(
                                Executors.newSingleThreadExecutor(),
                                QRCodeAnalyzer { qrCode ->
                                    if (!scannedOnce) {
                                        scannedOnce = true
                                        onQRCodeScanned(qrCode)
                                    }
                                }
                            )

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Timber.e(e, "Camera binding failed")
                            }

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Instructions
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SigilColors.Surface,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "Align QR code within the frame",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SigilColors.TextMuted,
                        modifier = Modifier.padding(SigilSpacing.s4)
                    )
                }
            }
            else -> {
                // Permission request UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(SigilSpacing.s6),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera Access Required",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SigilColors.Text
                    )

                    Spacer(modifier = Modifier.height(SigilSpacing.s4))

                    Text(
                        text = "Camera permission is needed to scan QR codes for server registration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SigilColors.TextMuted,
                        modifier = Modifier.padding(horizontal = SigilSpacing.s4)
                    )

                    Spacer(modifier = Modifier.height(SigilSpacing.s6))

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SigilColors.Primary
                        )
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }
        }
    }
}

/**
 * ML Kit barcode analyzer.
 */
private class QRCodeAnalyzer(
    private val onQRCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        when (barcode.valueType) {
                            Barcode.TYPE_URL, Barcode.TYPE_TEXT -> {
                                barcode.rawValue?.let { qrValue ->
                                    if (qrValue.startsWith("https://sigilauth.com/")) {
                                        onQRCodeDetected(qrValue)
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
