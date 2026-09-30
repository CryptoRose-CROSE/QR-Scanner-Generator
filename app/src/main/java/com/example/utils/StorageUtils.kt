package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageUtils {

    private const val FOLDER_NAME = "QR Scanner & Generator"

    /**
     * Saves a QR Bitmap image directly to the device's Pictures/QR Scanner & Generator folder
     * using modern Android MediaStore APIs (Android 10+) or standard filesystem fallback.
     */
    suspend fun saveQrImageToGallery(
        context: Context,
        bitmap: Bitmap,
        filenamePrefix: String = "QR"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "${filenamePrefix}_${timeStamp}.png"
            var imageUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + FOLDER_NAME
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

                imageUri = uri
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                // Pre-Android 10 fallback
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val qrDir = File(picturesDir, FOLDER_NAME)
                if (!qrDir.exists()) {
                    qrDir.mkdirs()
                }
                val imageFile = File(qrDir, filename)
                FileOutputStream(imageFile).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.flush()
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.TITLE, filename)
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                }
                imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }

            if (imageUri != null) {
                Result.success(imageUri)
            } else {
                Result.failure(Exception("Failed to register image in gallery."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
