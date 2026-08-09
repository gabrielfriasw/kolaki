package com.kolvexo.kolaki.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {
    private const val ANIMATION_FPS = "12.5"

    @Suppress("DEPRECATION")
    fun isAnimated(context: Context, source: Uri): Boolean {
        if (isGif(context, source)) {
            return decodeMovie(context, source)?.duration()?.let { it > 0 } == true
        }
        if (!isWebP(context, source)) return false
        return readPrefix(context, source, 64 * 1024)
            .decodeToString()
            .contains("ANIM")
    }

    fun animatedSourceExtension(context: Context, source: Uri): String =
        if (isGif(context, source)) ".gif" else ".webp"

    suspend fun copySource(context: Context, source: Uri, destination: File) =
        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            context.contentResolver.openInputStream(source)?.use { input ->
                destination.outputStream().use(input::copyTo)
            } ?: error("Não consegui preservar a animação original")
        }

    suspend fun createSticker(context: Context, source: Uri, destination: File): Boolean =
        withContext(Dispatchers.IO) {
            val animated = isAnimated(context, source)
            if (animated) {
                val input = copyToTemporaryInput(
                    context,
                    source,
                    if (isGif(context, source)) ".gif" else ".webp",
                )
                try {
                    encodeAnimated(
                        input = input,
                        destination = destination,
                        filter = listOf(
                            "fps=$ANIMATION_FPS",
                            "scale=512:512:force_original_aspect_ratio=decrease:flags=lanczos",
                            "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0.0",
                            "format=rgba",
                        ).joinToString(","),
                    )
                } finally {
                    input.delete()
                }
            } else {
                val sourceBitmap = decodeOrientedBitmap(context, source)
                    ?: error("Não consegui abrir essa imagem")
                val normalized = renderTransform(sourceBitmap, StickerTransform())
                sourceBitmap.recycle()
                writeStaticWebP(normalized, destination)
                normalized.recycle()
            }
            animated
        }

    suspend fun rotateSticker(source: File) =
        withContext(Dispatchers.IO) {
            transformStaticSticker(source, StickerTransform(rotationDegrees = 90f))
        }

    suspend fun cropSticker(
        source: File,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
    ) = withContext(Dispatchers.IO) {
        transformStaticSticker(
            source,
            StickerTransform(
                scale = scale.coerceIn(1f, 5f),
                offsetX = offsetX.coerceIn(-2f, 2f),
                offsetY = offsetY.coerceIn(-2f, 2f),
            ),
        )
    }

    suspend fun renderAnimatedSticker(
        originalSource: File,
        destination: File,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotationQuarterTurns: Int,
    ) = withContext(Dispatchers.IO) {
        require(originalSource.extension.equals("gif", ignoreCase = true)) {
            "A edição animada está disponível para GIFs importados pelo KolaKi"
        }
        val filters = mutableListOf(
            "fps=$ANIMATION_FPS",
            "scale=512:512:force_original_aspect_ratio=decrease:flags=lanczos",
            "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black@0.0",
        )
        repeat(rotationQuarterTurns.mod(4)) { filters += "transpose=clock" }
        if (scale > 1.001f || offsetX != 0f || offsetY != 0f) {
            val safeScale = scale.coerceIn(1f, 12f)
            val moveX = offsetX * PackRules.STICKER_SIZE
            val moveY = offsetY * PackRules.STICKER_SIZE
            filters += "scale=iw*$safeScale:ih*$safeScale:flags=lanczos"
            filters += "crop=512:512:(in_w-out_w)/2-$moveX:(in_h-out_h)/2-$moveY"
        }
        filters += "format=rgba"
        encodeAnimated(originalSource, destination, filters.joinToString(","))
    }

    suspend fun createTray(sticker: File, destination: File) = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(sticker.absolutePath)
            ?: error("Não consegui criar o ícone do pacote")
        val tray = bitmap.scale(PackRules.TRAY_SIZE, PackRules.TRAY_SIZE)
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { tray.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (tray !== bitmap) tray.recycle()
        bitmap.recycle()
    }

    private fun transformStaticSticker(source: File, transform: StickerTransform) {
        val temporary = File(source.parentFile, ".${source.name}.editing")
        temporary.delete()
        try {
            val bitmap = BitmapFactory.decodeFile(source.absolutePath)
                ?: error("Não consegui editar essa figurinha")
            val result = renderTransform(bitmap, transform)
            bitmap.recycle()
            writeStaticWebP(result, temporary)
            result.recycle()
            temporary.copyTo(source, overwrite = true)
        } finally {
            temporary.delete()
        }
    }

    private fun encodeAnimated(input: File, destination: File, filter: String) {
        destination.parentFile?.mkdirs()
        // tenta primeiro a melhor qualidade que ainda respeite o limite
        val qualities = listOf(72, 58, 44, 32, 24)
        var smallestSize = Long.MAX_VALUE
        var lastError = ""
        var lostAnimation = false
        qualities.forEach { quality ->
            val candidate = File(destination.parentFile, ".${destination.name}.$quality.webp")
            candidate.delete()
            val session = FFmpegKit.executeWithArguments(
                arrayOf(
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-i", input.absolutePath,
                    "-t", (PackRules.MAX_ANIMATION_DURATION_MS / 1000).toString(),
                    "-vf", filter,
                    "-an",
                    "-c:v", "libwebp_anim",
                    "-lossless", "0",
                    "-compression_level", "6",
                    "-q:v", quality.toString(),
                    "-loop", "0",
                    candidate.absolutePath,
                ),
            )
            if (!ReturnCode.isSuccess(session.returnCode)) {
                lastError = session.allLogsAsString.orEmpty().lineSequence().lastOrNull().orEmpty()
                candidate.delete()
                return@forEach
            }
            smallestSize = min(smallestSize, candidate.length())
            val withinSizeLimit = candidate.isFile &&
                candidate.length() in 1..PackRules.MAX_ANIMATED_BYTES.toLong()
            val hasAnimation = withinSizeLimit && candidate.inputStream().use { inputStream ->
                    inputStream.readAtMost(64 * 1024).decodeToString().contains("ANIM")
                }
            if (withinSizeLimit && !hasAnimation) lostAnimation = true
            if (hasAnimation) {
                candidate.copyTo(destination, overwrite = true)
                candidate.delete()
                return
            }
            candidate.delete()
        }
        error(
            if (lostAnimation) {
                "A conversão perdeu os quadros da animação"
            } else if (smallestSize == Long.MAX_VALUE) {
                "Não consegui converter a animação para WebP" +
                    lastError.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()
            } else {
                "A animação ficou maior que 500 KB mesmo depois da otimização (${smallestSize / 1024} KB)"
            },
        )
    }

    private fun copyToTemporaryInput(context: Context, source: Uri, suffix: String): File {
        val file = File.createTempFile("kolaki-source-", suffix, context.cacheDir)
        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                file.outputStream().use(input::copyTo)
            } ?: error("Não consegui abrir a animação")
            return file
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
    }

    private fun decodeOrientedBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).rotationDegrees
            }
        }.getOrNull() ?: 0

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > 2048) sampleSize *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: return null
        if (orientation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    private fun renderTransform(source: Bitmap, transform: StickerTransform): Bitmap {
        val normalized = normalize(source)
        val size = PackRules.STICKER_SIZE
        val result = createBitmap(size, size)
        val canvas = Canvas(result)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.translate(
            size / 2f + transform.offsetX * size,
            size / 2f + transform.offsetY * size,
        )
        canvas.scale(transform.scale, transform.scale)
        canvas.rotate(transform.rotationDegrees)
        canvas.translate(-size / 2f, -size / 2f)
        canvas.drawBitmap(normalized, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        normalized.recycle()
        return result
    }

    private fun normalize(source: Bitmap): Bitmap {
        val size = PackRules.STICKER_SIZE
        val result = createBitmap(size, size)
        val canvas = Canvas(result)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val fit = min(size.toFloat() / source.width, size.toFloat() / source.height)
        val width = source.width * fit
        val height = source.height * fit
        val left = (size - width) / 2f
        val top = (size - height) / 2f
        canvas.drawBitmap(
            source,
            null,
            android.graphics.RectF(left, top, left + width, top + height),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return result
    }

    @Suppress("DEPRECATION")
    private fun writeStaticWebP(bitmap: Bitmap, destination: File) {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        var quality = 92
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { buffer ->
                bitmap.compress(format, quality, buffer)
                buffer.toByteArray()
            }
            quality -= 8
        } while (bytes.size > PackRules.MAX_STATIC_BYTES && quality >= 12)
        check(bytes.size <= PackRules.MAX_STATIC_BYTES) {
            "A imagem ficou maior que 100 KB mesmo depois da otimização"
        }
        destination.parentFile?.mkdirs()
        destination.writeBytes(bytes)
    }

    private fun isGif(context: Context, source: Uri): Boolean {
        if (context.contentResolver.getType(source)?.equals("image/gif", ignoreCase = true) == true) {
            return true
        }
        return readPrefix(context, source, 6).decodeToString().startsWith("GIF8")
    }

    private fun isWebP(context: Context, source: Uri): Boolean {
        if (context.contentResolver.getType(source)?.equals("image/webp", ignoreCase = true) == true) {
            return true
        }
        val header = readPrefix(context, source, 12)
        return header.size >= 12 &&
            header.copyOfRange(0, 4).decodeToString() == "RIFF" &&
            header.copyOfRange(8, 12).decodeToString() == "WEBP"
    }

    @Suppress("DEPRECATION")
    private fun decodeMovie(context: Context, source: Uri): Movie? =
        context.contentResolver.openInputStream(source)?.use(Movie::decodeStream)

    private fun readPrefix(context: Context, source: Uri, count: Int): ByteArray =
        runCatching {
            context.contentResolver.openInputStream(source)?.use { it.readAtMost(count) } ?: byteArrayOf()
        }.getOrDefault(byteArrayOf())

    private fun InputStream.readAtMost(count: Int): ByteArray {
        val bytes = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = read(bytes, offset, count - offset)
            if (read <= 0) break
            offset += read
        }
        return bytes.copyOf(offset)
    }

    private data class StickerTransform(
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val rotationDegrees: Float = 0f,
    )
}
