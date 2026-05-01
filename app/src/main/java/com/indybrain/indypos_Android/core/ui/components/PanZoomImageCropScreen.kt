package com.indybrain.indypos_Android.core.ui.components

import android.graphics.RectF
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.indybrain.indypos_Android.R
import com.indybrain.indypos_Android.core.ui.AppFontStyle
import com.indybrain.indypos_Android.core.ui.FontSize
import com.indybrain.indypos_Android.core.ui.FontUtils
import com.indybrain.indypos_Android.core.utils.ImageUtils
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Full-screen pan/zoom crop: fixed aspect frame, user aligns the image underneath.
 *
 * @param cropAspectWidth crop frame width portion (e.g. 16f for 16:9)
 * @param cropAspectHeight crop frame height portion (e.g. 9f for 16:9)
 */
@Composable
fun PanZoomImageCropScreen(
    imageUri: Uri,
    cropAspectWidth: Float,
    cropAspectHeight: Float,
    outputWidthPx: Int,
    outputHeightPx: Int,
    title: String,
    confirmLabel: String,
    outputFileNamePrefix: String,
    onDismiss: () -> Unit,
    onConfirm: (Uri) -> Unit
) {
    require(cropAspectWidth > 0f && cropAspectHeight > 0f) {
        "cropAspectWidth and cropAspectHeight must be positive"
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadError by remember { mutableStateOf(false) }

    var scale by remember { mutableStateOf(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    LaunchedEffect(imageUri) {
        bitmap = withContext(Dispatchers.IO) {
            ImageUtils.loadBitmap(imageUri, context)
        }
        if (bitmap == null) loadError = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        when {
            loadError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.home_error_title),
                        style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.home_cancel), color = Color.White)
                    }
                }
            }
            bitmap != null -> {
                val density = LocalDensity.current
                val config = LocalConfiguration.current
                val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
                val offsetX = offsetXAnim.value
                val offsetY = offsetYAnim.value
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    scale = (scale * zoomChange).coerceIn(0.5f, 4f)
                    scope.launch(Dispatchers.Main.immediate) {
                        offsetXAnim.snapTo(offsetXAnim.value + panChange.x)
                        offsetYAnim.snapTo(offsetYAnim.value + panChange.y)
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(transformState)
                ) {
                    val bmp = bitmap!!
                    val imgW = bmp.width.toFloat()
                    val imgH = bmp.height.toFloat()
                    val screenW = with(density) { maxWidth.toPx() }
                    val screenH = with(density) { maxHeight.toPx() }

                    val aw = cropAspectWidth
                    val ah = cropAspectHeight
                    val frameW = minOf(screenW, screenH * aw / ah)
                    val frameH = frameW * ah / aw
                    val scrCenterX = screenW / 2f
                    val scrCenterY = screenH / 2f
                    val frameLeft = (screenW - frameW) / 2f
                    val frameTop = (screenH - frameH) / 2f

                    val fillScale = maxOf(frameW / imgW, frameH / imgH)
                    val initialScale = remember(imgW, imgH, frameW, frameH) {
                        maxOf(fillScale, 1f)
                    }
                    val currentScale = initialScale * scale
                    val imgDrawW = imgW * currentScale
                    val imgDrawH = imgH * currentScale
                    val minOffsetX = minOf((frameW - imgDrawW) / 2f, (imgDrawW - frameW) / 2f)
                    val maxOffsetX = maxOf((frameW - imgDrawW) / 2f, (imgDrawW - frameW) / 2f)
                    val minOffsetY = minOf((frameH - imgDrawH) / 2f, (imgDrawH - frameH) / 2f)
                    val maxOffsetY = maxOf((frameH - imgDrawH) / 2f, (imgDrawH - frameH) / 2f)

                    LaunchedEffect(transformState.isTransformInProgress) {
                        if (!transformState.isTransformInProgress) {
                            offsetXAnim.animateTo(
                                offsetXAnim.value.coerceIn(minOffsetX, maxOffsetX),
                                animationSpec = tween(300)
                            )
                            offsetYAnim.animateTo(
                                offsetYAnim.value.coerceIn(minOffsetY, maxOffsetY),
                                animationSpec = tween(300)
                            )
                        }
                    }

                    val imgLeft = scrCenterX - imgDrawW / 2f + offsetX
                    val imgTop = scrCenterY - imgDrawH / 2f + offsetY

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(Color.Black)
                        drawContext.canvas.nativeCanvas.apply {
                            save()
                            translate(imgLeft, imgTop)
                            scale(currentScale, currentScale)
                            drawBitmap(bmp, 0f, 0f, null)
                            restore()
                        }
                    }

                    val overlayColor = Color.Black.copy(alpha = 0.6f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            overlayColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(screenW, frameTop)
                        )
                        drawRect(
                            overlayColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, frameTop + frameH),
                            size = androidx.compose.ui.geometry.Size(screenW, screenH - frameTop - frameH)
                        )
                        drawRect(
                            overlayColor,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, frameTop),
                            size = androidx.compose.ui.geometry.Size(frameLeft, frameH)
                        )
                        drawRect(
                            overlayColor,
                            topLeft = androidx.compose.ui.geometry.Offset(frameLeft + frameW, frameTop),
                            size = androidx.compose.ui.geometry.Size(screenW - frameLeft - frameW, frameH)
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color.White,
                            topLeft = androidx.compose.ui.geometry.Offset(frameLeft, frameTop),
                            size = androidx.compose.ui.geometry.Size(frameW, frameH),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = FontUtils.mainFont(style = AppFontStyle.Medium, size = FontSize.Medium),
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (isLandscape) 16.dp else 24.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        val cropLeft = imgW / 2f + (-frameW / 2f - offsetX) / currentScale
                                        val cropRight = imgW / 2f + (frameW / 2f - offsetX) / currentScale
                                        val cropTop = imgH / 2f + (-frameH / 2f - offsetY) / currentScale
                                        val cropBottom = imgH / 2f + (frameH / 2f - offsetY) / currentScale
                                        val cropRect = RectF(cropLeft, cropTop, cropRight, cropBottom)
                                        val tempFile = File(
                                            context.cacheDir,
                                            "${outputFileNamePrefix}_${System.currentTimeMillis()}.jpg"
                                        )
                                        val croppedUri = withContext(Dispatchers.IO) {
                                            ImageUtils.cropBitmapToRegion(
                                                bitmap = bmp,
                                                cropRect = cropRect,
                                                targetWidth = outputWidthPx,
                                                targetHeight = outputHeightPx,
                                                file = tempFile,
                                                context = context
                                            )
                                        }
                                        croppedUri?.let { onConfirm(it) }
                                    }
                                }
                            ) {
                                Text(
                                    text = confirmLabel,
                                    style = FontUtils.mainFont(style = AppFontStyle.Bold, size = FontSize.Medium),
                                    color = Color(0xFF5EA6ED)
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "...",
                        color = Color.White,
                        style = FontUtils.mainFont(style = AppFontStyle.Regular, size = FontSize.Medium)
                    )
                }
            }
        }
    }
}
