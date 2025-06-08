package ru.gd_alt.youwilldrive

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import androidx.core.graphics.scale

class UtilsProvider {
    companion object {
        fun timestampToDate(timestamp: Int): LocalDateTime {
            return LocalDateTime.ofEpochSecond(timestamp.toLong(), 0, ZoneOffset.ofHours(3))
        }

        fun dateToTimestamp(date: LocalDateTime): Int {
            return date.toEpochSecond(ZoneOffset.ofHours(3)).toInt()
        }

        /**
         * Resizes an image from a given URI, compresses it, and encodes it to a Base64 string.
         * The longest side of the image will be scaled down to MAX_IMAGE_SIZE.
         *
         * @param context The application context.
         * @param imageUri The URI of the image to process.
         * @return A Base64 encoded string of the processed image, or null if an error occurs.
         */
        fun resizeAndEncodeImage(context: Context, imageUri: Uri): String? {
            val MAX_IMAGE_SIZE = 512 // pixels, longest side (e.g., 512x512)
            val JPEG_QUALITY = 85 // JPEG compression quality (0-100)

            return try {
                // First, decode image bounds to calculate inSampleSize
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)
                }

                var imageHeight = options.outHeight
                var imageWidth = options.outWidth
                var inSampleSize = 1

                // Calculate inSampleSize to roughly scale down large images efficiently
                if (imageHeight > MAX_IMAGE_SIZE || imageWidth > MAX_IMAGE_SIZE) {
                    val halfHeight = imageHeight / 2
                    val halfWidth = imageWidth / 2

                    while ((halfHeight / inSampleSize) >= MAX_IMAGE_SIZE || (halfWidth / inSampleSize) >= MAX_IMAGE_SIZE) {
                        inSampleSize *= 2
                    }
                }
                options.inSampleSize = inSampleSize
                options.inJustDecodeBounds = false // Now decode the full bitmap

                // Decode the bitmap with calculated inSampleSize
                var bitmap: Bitmap? = null
                context.contentResolver.openInputStream(imageUri)?.use { actualInputStream ->
                    bitmap = BitmapFactory.decodeStream(actualInputStream, null, options)
                }

                if (bitmap == null) {
                    Log.e("ImageUtils", "Failed to decode bitmap from URI: $imageUri")
                    return null
                }

                // If the bitmap is still larger than MAX_IMAGE_SIZE after inSampleSize,
                // perform exact scaling to fit within the target dimensions while maintaining aspect ratio.
                if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
                    val scaleFactor =
                        (MAX_IMAGE_SIZE.toFloat() / bitmap.width).coerceAtMost(MAX_IMAGE_SIZE.toFloat() / bitmap.height)
                    val newWidth = (bitmap.width * scaleFactor).toInt()
                    val newHeight = (bitmap.height * scaleFactor).toInt()
                    bitmap = bitmap.scale(newWidth, newHeight)
                }

                // Compress the bitmap to JPEG and encode to Base64
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                val imageBytes = outputStream.toByteArray()

                Base64.encodeToString(imageBytes, Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e("ImageUtils", "Error resizing and encoding image: ${e.message}", e)
                null
            }
        }
    }
}