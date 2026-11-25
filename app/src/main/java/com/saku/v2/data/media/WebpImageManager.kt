package com.saku.v2.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.saku.v2.data.MediaManagerContract
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Compresses imported receipt and savings-cover images into local WebP files
 * with EXIF orientation normalization and path-traversal-safe file resolution.
 */
class WebpImageManager(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaManagerContract {

    companion object {
        const val MEDIA_DIR_NAME = "saku_media"
        private const val RECEIPT_MAX_DIMENSION = 1280
        private const val RECEIPT_QUALITY = 78
        private const val COVER_MAX_DIMENSION = 800
        private const val COVER_QUALITY = 80
    }

    private val appContext: Context = context.applicationContext ?: context

    val mediaDir: File
        get() = File(appContext.filesDir, MEDIA_DIR_NAME)

    override suspend fun importReceiptWebp(uri: Uri): Result<String> {
        return importWebp(
            uri = uri,
            prefix = "receipt_",
            maxDimension = RECEIPT_MAX_DIMENSION,
            quality = RECEIPT_QUALITY
        )
    }

    override suspend fun importCoverWebp(uri: Uri): Result<String> {
        return importWebp(
            uri = uri,
            prefix = "cover_",
            maxDimension = COVER_MAX_DIMENSION,
            quality = COVER_QUALITY
        )
    }

    override fun resolveMediaFile(fileName: String): File {
        val safeName = File(fileName).name
        val sanitized = if (safeName.isBlank() || safeName == "." || safeName == "..") {
            "_invalid"
        } else {
            safeName
        }
        return File(ensureMediaDir(), sanitized)
    }

    override suspend fun cleanupOrphanMedia(referencedFiles: Set<String>) {
        withContext(ioDispatcher) {
            val dir = mediaDir
            if (!dir.exists() || !dir.isDirectory) return@withContext

            val keepNames = referencedFiles.mapNotNullTo(HashSet()) { rawName ->
                File(rawName).name.takeIf { it.isNotBlank() && it != "." && it != ".." }
            }

            dir.listFiles()?.forEach { file ->
                if (file.isFile &&
                    file.extension.equals("webp", ignoreCase = true) &&
                    file.name !in keepNames
                ) {
                    file.delete()
                }
            }
        }
    }

    private suspend fun importWebp(
        uri: Uri,
        prefix: String,
        maxDimension: Int,
        quality: Int
    ): Result<String> = withContext(ioDispatcher) {
        var decodedBitmap: Bitmap? = null
        var outputBitmap: Bitmap? = null
        val dir = ensureMediaDir()
        val fileName = "$prefix${UUID.randomUUID()}.webp"
        val targetFile = File(dir, fileName)
        val tempFile = File(dir, "$fileName.tmp")

        try {
            val resolver = appContext.contentResolver

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val boundsInput = resolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Tidak dapat membuka gambar terpilih"))
            boundsInput.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Format gambar tidak valid atau rusak"))
            }

            val orientation = readExifOrientation(uri)

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(rawWidth, rawHeight, maxDimension)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val pixelInput = resolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Gagal membaca ulang data gambar"))
            val decoded = pixelInput.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext Result.failure(IOException("Gagal mendecode gambar"))
            decodedBitmap = decoded

            val transformMatrix = buildTransformMatrix(
                width = decoded.width,
                height = decoded.height,
                maxDimension = maxDimension,
                orientation = orientation
            )

            val finalBitmap = if (transformMatrix.isIdentity) {
                decoded
            } else {
                Bitmap.createBitmap(
                    decoded,
                    0,
                    0,
                    decoded.width,
                    decoded.height,
                    transformMatrix,
                    true
                )
            }
            outputBitmap = finalBitmap

            if (finalBitmap !== decoded && !decoded.isRecycled) {
                decoded.recycle()
                decodedBitmap = null
            }

            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }

            FileOutputStream(tempFile).use { out ->
                val compressed = finalBitmap.compress(format, quality, out)
                if (!compressed) {
                    throw IOException("Gagal mengompres gambar ke format WebP")
                }
                out.flush()
            }

            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(fileName)
        } catch (ce: CancellationException) {
            tempFile.delete()
            targetFile.delete()
            throw ce
        } catch (t: Throwable) {
            tempFile.delete()
            targetFile.delete()
            Result.failure(t)
        } finally {
            if (outputBitmap != null && outputBitmap !== decodedBitmap && !outputBitmap.isRecycled) {
                outputBitmap.recycle()
            }
            if (decodedBitmap != null && !decodedBitmap.isRecycled) {
                decodedBitmap.recycle()
            }
        }
    }

    private fun readExifOrientation(uri: Uri): Int {
        return try {
            appContext.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        val longest = maxOf(width, height)
        if (longest > maxDimension) {
            while ((longest / (inSampleSize * 2)) >= maxDimension) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun buildTransformMatrix(
        width: Int,
        height: Int,
        maxDimension: Int,
        orientation: Int
    ): Matrix {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        val longest = maxOf(width, height)
        if (longest > maxDimension) {
            val scale = maxDimension.toFloat() / longest.toFloat()
            matrix.postScale(scale, scale)
        }
        return matrix
    }

    private fun ensureMediaDir(): File {
        val dir = mediaDir
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
