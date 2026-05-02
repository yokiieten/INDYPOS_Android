package com.indybrain.indypos_Android.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import coil.request.ImageRequest
import coil.request.CachePolicy
import coil.size.Precision
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility class for image operations
 */
object ImageUtils {
    
    /**
     * Resize image to specified dimensions
     * @param imageUri URI of the image
     * @param targetWidth Target width in pixels
     * @param targetHeight Target height in pixels
     * @param context Context for accessing content resolver
     * @return Resized bitmap or null if failed
     */
    fun resizeImage(
        imageUri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        context: Context
    ): Bitmap? {
        return try {
            // Read image from URI
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) return null
            
            // Decode bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // Calculate sample size
            val sampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            
            // Decode bitmap with sample size and high quality settings
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888  // Use highest quality color format
                inDither = false  // Disable dithering for better quality
                inScaled = false  // Don't scale automatically
            }
            val inputStream2 = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2?.close()
            
            if (bitmap == null) return null
            
            // Handle orientation
            val orientedBitmap = handleOrientation(bitmap, imageUri, context)
            
            // Resize to exact dimensions using Matrix with high quality settings
            val scaleX = targetWidth.toFloat() / orientedBitmap.width
            val scaleY = targetHeight.toFloat() / orientedBitmap.height
            val matrix = Matrix().apply {
                setScale(scaleX, scaleY)
            }
            // Use ARGB_8888 config for maximum quality
            val resizedBitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(resizedBitmap)
            val paint = Paint().apply {
                isAntiAlias = true  // Enable anti-aliasing
                isFilterBitmap = true  // Enable filtering for smoother scaling
                isDither = false  // Disable dithering for better quality
            }
            canvas.drawBitmap(orientedBitmap, matrix, paint)
            resizedBitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Re-rasterizes [bitmap] to grayscale at the same pixel size, using [Bitmap.Config.RGB_565]
     * (no alpha) and luminance from a desaturating [ColorMatrix] — closer to thermal / 1-bit pipelines.
     * If conversion fails, returns the same [bitmap] instance; callers should recycle the source only when
     * the returned bitmap is a different instance (`!==`).
     */
    fun toGrayscaleForThermalPrint(bitmap: Bitmap): Bitmap {
        return try {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return bitmap
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            out
        } catch (_: Exception) {
            bitmap
        }
    }
    
    /**
     * Save bitmap to file using JPEG format with high quality (95-100)
     * Provides excellent quality with smaller file size than PNG
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 95): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                // Use JPEG with high quality (95-100) for good balance between quality and file size
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculate sample size for efficient loading
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Handle image orientation based on EXIF data
     */
    private fun handleOrientation(bitmap: Bitmap, uri: Uri, context: Context): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }
            
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            // Recycle original bitmap if it's different from rotated
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            rotatedBitmap
        } catch (e: Exception) {
            bitmap
        }
    }
    
    /**
     * Load bitmap from URI (with orientation handling)
     */
    fun loadBitmap(uri: Uri, context: Context): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap?.let { handleOrientation(it, uri, context) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crop a region from bitmap and resize to target dimensions.
     * @param bitmap Source bitmap
     * @param cropRect Region to crop in bitmap pixels (will be clamped to bitmap bounds)
     * @param targetWidth Output width
     * @param targetHeight Output height
     * @param file Output file to save
     * @return Uri of saved file or null on failure
     */
    fun cropBitmapToRegion(
        bitmap: Bitmap,
        cropRect: RectF,
        targetWidth: Int,
        targetHeight: Int,
        file: File,
        context: Context
    ): Uri? {
        return try {
            val left = cropRect.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = cropRect.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = (cropRect.right.toInt().coerceIn(1, bitmap.width)).coerceAtLeast(left + 1)
            val bottom = (cropRect.bottom.toInt().coerceIn(1, bitmap.height)).coerceAtLeast(top + 1)

            val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)

            val matrix = Matrix().apply {
                setScale(
                    targetWidth.toFloat() / cropped.width,
                    targetHeight.toFloat() / cropped.height
                )
            }
            val scaled = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scaled)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            canvas.drawBitmap(cropped, matrix, paint)

            val saved = saveBitmapToFile(scaled, file, 95)
            cropped.recycle()
            scaled.recycle()
            if (saved) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create a high-quality ImageRequest for Coil with optimized settings
     * This ensures images are loaded with maximum quality and clarity
     */
    fun createHighQualityImageRequest(
        context: Context,
        data: Any,
        crossfade: Boolean = true
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .crossfade(crossfade)
            .precision(Precision.EXACT)  // Load exact size for better quality
            .allowHardware(false)  // Use software rendering for better quality
            .allowRgb565(false)  // Force ARGB_8888 for better color quality
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}

