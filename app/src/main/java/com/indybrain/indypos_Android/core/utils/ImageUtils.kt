package com.indybrain.indypos_Android.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
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
            
            // Decode bitmap with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val inputStream2 = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2?.close()
            
            if (bitmap == null) return null
            
            // Handle orientation
            val orientedBitmap = handleOrientation(bitmap, imageUri, context)
            
            // Resize to exact dimensions
            Bitmap.createScaledBitmap(orientedBitmap, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save bitmap to file
     */
    fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int = 85): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
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
}

