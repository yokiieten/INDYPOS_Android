package com.indybrain.indypos_Android.presentation.barcodescanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.ui.theme.BaseBackground
import com.indybrain.indypos_Android.ui.theme.PrimaryButton
import com.indybrain.indypos_Android.ui.theme.PrimaryText
import com.indybrain.indypos_Android.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    onBackClick: () -> Unit = {},
    onBarcodeScanned: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    // Camera permission
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    // Camera state
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var zoomFactor by remember { mutableFloatStateOf(0.33f) } // 1.5x zoom (0.33 in linear zoom scale)
    var showZoomLabel by remember { mutableStateOf(false) }
    var maxZoomRatio by remember { mutableFloatStateOf(1.0f) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Focus animation
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusAlpha by remember { mutableFloatStateOf(0f) }
    val animatedFocusAlpha by animateFloatAsState(
        targetValue = focusAlpha,
        animationSpec = tween(300),
        label = "focus_alpha"
    )
    
    // Initialize camera provider
    LaunchedEffect(Unit) {
        try {
            val provider = ProcessCameraProvider.getInstance(context)
            provider.addListener({
                cameraProvider = provider.get()
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            errorMessage = context.getString(R.string.barcode_scanner_camera_error_message)
            showErrorDialog = true
        }
    }
    
    // Check if permission is granted
    val hasPermission = cameraPermissionState.status is PermissionStatus.Granted
    
    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Show permission dialog if denied permanently
    LaunchedEffect(cameraPermissionState.status) {
        when (val status = cameraPermissionState.status) {
            is PermissionStatus.Denied -> {
                if (!status.shouldShowRationale) {
                    // Permission is permanently denied
                    showPermissionDialog = true
                }
            }
            else -> {
                // Permission granted or not yet requested
            }
        }
    }
    
    // Preview view reference
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    
    // Update flash when changed
    LaunchedEffect(flashEnabled) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }
    
    // Update zoom when changed
    LaunchedEffect(zoomFactor) {
        camera?.cameraControl?.setLinearZoom(zoomFactor.coerceIn(0f, 1f))
    }
    
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        ),
                        color = PrimaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.product_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Flash button
                    IconButton(
                        onClick = { flashEnabled = !flashEnabled }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                                contentDescription = "Flash",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermission) {
                // Camera preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }.also {
                            previewViewRef = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Focus on tap
                                focusPoint = tapOffset
                                focusAlpha = 1f
                                
                                scope.launch {
                                    kotlinx.coroutines.delay(600)
                                    focusAlpha = 0f
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Pinch to zoom (linear zoom: 0.0 to 1.0)
                                val newZoom = (zoomFactor * zoom).coerceIn(0f, 1f)
                                zoomFactor = newZoom
                                
                                // Calculate zoom ratio for display (1.0x to maxZoomRatio)
                                val zoomRatio = 1f + (newZoom * (maxZoomRatio - 1f))
                                
                                showZoomLabel = true
                                
                                scope.launch {
                                    kotlinx.coroutines.delay(2000)
                                    showZoomLabel = false
                                }
                            }
                        }
                )
                
                // Setup camera when preview view and provider are ready
                LaunchedEffect(previewViewRef, cameraProvider, hasPermission) {
                    if (previewViewRef != null && cameraProvider != null && hasPermission && camera == null) {
                        setupCamera(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            cameraProvider = cameraProvider!!,
                            previewView = previewViewRef!!,
                                onBarcodeDetected = { barcode ->
                                    if (!isScanning) {
                                        isScanning = true
                                        // Vibrate on successful scan
                                        try {
                                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                            vibrator?.let {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    it.vibrate(100)
                                                }
                                            }
                                        } catch (e: SecurityException) {
                                            // Vibration permission not granted, ignore
                                        }
                                        onBarcodeScanned(barcode)
                                        // Note: onBarcodeScanned callback will handle navigation back
                                    }
                                },
                            onCameraReady = { cam ->
                                camera = cam
                                // Get max zoom ratio from camera info
                                // Note: zoomState is a Flow, we'll get it from the camera control
                                try {
                                    val cameraInfo = cam.cameraInfo
                                    // For now, use a reasonable default (most cameras support up to 5x)
                                    maxZoomRatio = 5f
                                } catch (e: Exception) {
                                    maxZoomRatio = 5f
                                }
                            },
                            flashEnabled = flashEnabled,
                            zoomFactor = zoomFactor
                        )
                    }
                }
                
                // Scanner frame overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Scanner frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(2.5f) // Width:Height ratio similar to iOS (0.85 width, 0.4 height = 2.125:1)
                    ) {
                        ScannerFrame(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Instruction text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_instruction),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
                
                // Zoom label
                if (showZoomLabel) {
                    val zoomRatio = 1f + (zoomFactor * (maxZoomRatio - 1f))
                    Text(
                        text = String.format("%.1fx", zoomRatio),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Small
                        ),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                
                // Focus indicator
                focusPoint?.let { point ->
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = point.x.dp - 50.dp, y = point.y.dp - 50.dp)
                    ) {
                        drawRect(
                            color = Color.Yellow.copy(alpha = animatedFocusAlpha),
                            topLeft = Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(100f, 100f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            } else {
                // Permission denied state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_permission_message),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }
        
        // Permission dialog
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_permission_title),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        )
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_permission_message),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.barcode_scanner_permission_settings),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPermissionDialog = false
                            onBackClick()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_button_ok),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = SecondaryText
                        )
                    }
                }
            )
        }
        
        // Error dialog
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.barcode_scanner_camera_error),
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Bold,
                            size = FontSize.Large
                        )
                    )
                },
                text = {
                    Text(
                        text = errorMessage,
                        style = FontUtils.mainFont(
                            style = AppFontStyle.Regular,
                            size = FontSize.Medium
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showErrorDialog = false
                            onBackClick()
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_button_ok),
                            style = FontUtils.mainFont(
                                style = AppFontStyle.Medium,
                                size = FontSize.Medium
                            ),
                            color = PrimaryButton
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ScannerFrame(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val cornerLength = 30.dp.toPx()
        
        // Draw white border frame
        drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Draw corner indicators
        // Top-left
        drawLine(
            color = Color.White,
            start = Offset(0f, 0f),
            end = Offset(cornerLength, 0f),
            strokeWidth = strokeWidth * 2
        )
        drawLine(
            color = Color.White,
            start = Offset(0f, 0f),
            end = Offset(0f, cornerLength),
            strokeWidth = strokeWidth * 2
        )
        
        // Top-right
        drawLine(
            color = Color.White,
            start = Offset(size.width, 0f),
            end = Offset(size.width - cornerLength, 0f),
            strokeWidth = strokeWidth * 2
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width, 0f),
            end = Offset(size.width, cornerLength),
            strokeWidth = strokeWidth * 2
        )
        
        // Bottom-left
        drawLine(
            color = Color.White,
            start = Offset(0f, size.height),
            end = Offset(cornerLength, size.height),
            strokeWidth = strokeWidth * 2
        )
        drawLine(
            color = Color.White,
            start = Offset(0f, size.height),
            end = Offset(0f, size.height - cornerLength),
            strokeWidth = strokeWidth * 2
        )
        
        // Bottom-right
        drawLine(
            color = Color.White,
            start = Offset(size.width, size.height),
            end = Offset(size.width - cornerLength, size.height),
            strokeWidth = strokeWidth * 2
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width, size.height),
            end = Offset(size.width, size.height - cornerLength),
            strokeWidth = strokeWidth * 2
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun setupCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    onBarcodeDetected: (String) -> Unit,
    onCameraReady: (Camera) -> Unit,
    flashEnabled: Boolean,
    zoomFactor: Float
) {
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    // Preview
    val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }
    
    // Image analysis for barcode scanning
    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    
    val scanner = BarcodeScanning.getClient()
    
    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
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
                            Barcode.TYPE_TEXT,
                            Barcode.TYPE_PRODUCT,
                            Barcode.TYPE_ISBN,
                            Barcode.TYPE_URL -> {
                                barcode.rawValue?.let { value ->
                                    if (value.length >= 2) {
                                        onBarcodeDetected(value)
                                    }
                                }
                            }
                            else -> {}
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
    
    try {
        cameraProvider.unbindAll()
        
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
        
        // Set initial zoom and flash
        camera.cameraControl.setLinearZoom(zoomFactor.coerceIn(0f, 1f))
        camera.cameraControl.enableTorch(flashEnabled)
        
        onCameraReady(camera)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

