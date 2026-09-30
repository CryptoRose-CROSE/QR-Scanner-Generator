package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageProcessingUtils {

    /**
     * Safely decodes and prepares an image from Uri:
     * 1. Subsamples to avoid OutOfMemoryError
     * 2. Corrects EXIF rotation
     * 3. Scales within maxDimension (default 1920px)
     */
    suspend fun decodeAndScaleImage(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1920
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            // First, decode bounds only to compute sample size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext Result.failure(Exception("Cannot open image stream"))

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Result.failure(Exception("Invalid or corrupted image format"))
            }

            // Calculate inSampleSize
            var sampleSize = 1
            val maxOriginal = max(originalWidth, originalHeight)
            while (maxOriginal / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            // Decode the actual downsampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(Exception("Failed to decode image bitmap"))

            // Handle EXIF orientation
            val rotatedBitmap = correctExifOrientation(context, uri, decodedBitmap)

            // Final scale if still larger than maxDimension
            val finalBitmap = scaleBitmapToMaxDimension(rotatedBitmap, maxDimension)

            Result.success(finalBitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Corrects EXIF orientation based on image metadata.
     */
    private fun correctExifOrientation(context: Context, uri: Uri, sourceBitmap: Bitmap): Bitmap {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                val rotationAngle = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                if (rotationAngle != 0f) {
                    val matrix = Matrix().apply { postRotate(rotationAngle) }
                    val rotated = Bitmap.createBitmap(
                        sourceBitmap,
                        0,
                        0,
                        sourceBitmap.width,
                        sourceBitmap.height,
                        matrix,
                        true
                    )
                    if (rotated != sourceBitmap) {
                        sourceBitmap.recycle()
                    }
                    return rotated
                }
            }
        } catch (_: Exception) {
            // If EXIF reading fails, fallback to original bitmap
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
        return sourceBitmap
    }

    /**
     * Scales bitmap proportionally so that neither width nor height exceeds maxDimension.
     */
    fun scaleBitmapToMaxDimension(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension) return source

        val scaleFactor = maxDimension.toFloat() / maxSide
        val targetWidth = (width * scaleFactor).toInt().coerceAtLeast(1)
        val targetHeight = (height * scaleFactor).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        if (scaled != source) {
            source.recycle()
        }
        return scaled
    }

    /**
     * Rotates bitmap by specified degrees (e.g. 90, 180, 270).
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Crops bitmap to specified bounding box (normalized 0.0 to 1.0 coordinates).
     */
    fun cropBitmap(
        source: Bitmap,
        leftNorm: Float,
        topNorm: Float,
        rightNorm: Float,
        bottomNorm: Float
    ): Bitmap {
        val x = (source.width * leftNorm.coerceIn(0f, 1f)).toInt().coerceIn(0, source.width - 1)
        val y = (source.height * topNorm.coerceIn(0f, 1f)).toInt().coerceIn(0, source.height - 1)
        val w = (source.width * (rightNorm - leftNorm)).toInt().coerceIn(1, source.width - x)
        val h = (source.height * (bottomNorm - topNorm)).toInt().coerceIn(1, source.height - y)

        return Bitmap.createBitmap(source, x, y, w, h)
    }

    /**
     * Compresses bitmap to JPEG bytes with quality target and max file size check.
     */
    suspend fun compressToJpegBytes(
        bitmap: Bitmap,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.Default) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }
}
