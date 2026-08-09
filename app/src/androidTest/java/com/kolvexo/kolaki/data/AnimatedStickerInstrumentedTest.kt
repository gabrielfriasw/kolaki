package com.kolvexo.kolaki.data

import android.net.Uri
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kolvexo.kolaki.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class AnimatedStickerInstrumentedTest {
    @Test
    fun gifBecomesWhatsAppCompatibleAnimatedWebP() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "animated-sticker-test").also(File::mkdirs)
        val gif = File(directory, "source.gif")
        val webp = File(directory, "result.webp")
        try {
            gif.writeBytes(Base64.decode(TEST_GIF_BASE64, Base64.DEFAULT))

            val animated = ImageProcessor.createSticker(context, Uri.fromFile(gif), webp)

            assertTrue(animated)
            assertAnimatedSticker(context, webp)

            ImageProcessor.renderAnimatedSticker(
                originalSource = gif,
                destination = webp,
                scale = 1.8f,
                offsetX = .08f,
                offsetY = -.06f,
                rotationQuarterTurns = 0,
            )
            assertAnimatedSticker(context, webp)

            ImageProcessor.renderAnimatedSticker(
                originalSource = gif,
                destination = webp,
                scale = 1.8f,
                offsetX = .06f,
                offsetY = .08f,
                rotationQuarterTurns = 1,
            )
            assertAnimatedSticker(context, webp)
        } finally {
            gif.delete()
            webp.delete()
            directory.delete()
        }
    }

    @Test
    fun staticCropAppliesZoomAndPan() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.cacheDir, "static-crop-source.png")
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
            for (x in 0 until 256) {
                for (y in 0 until height) setPixel(x, y, Color.RED)
            }
        }
        try {
            FileOutputStream(source).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ImageProcessor.cropSticker(source, scale = 2f, offsetX = .25f, offsetY = 0f)

            val cropped = requireNotNull(BitmapFactory.decodeFile(source.absolutePath))
            val center = cropped.getPixel(256, 256)
            assertTrue(Color.red(center) > Color.blue(center))
            assertEquals(PackRules.STICKER_SIZE, cropped.width)
            assertEquals(PackRules.STICKER_SIZE, cropped.height)
            cropped.recycle()
        } finally {
            bitmap.recycle()
            source.delete()
        }
    }

    @Test
    fun providerExposesOnlyTheStickersCreatedByTheUser() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousPacks = PackStore.read(context)
        val identifier = "provider_test_${System.nanoTime()}"
        val fileName = "single.webp"
        val directory = PackStore.packDirectory(context, identifier)
        PackStore.imageFile(context, identifier, fileName).writeBytes(byteArrayOf(1, 2, 3, 4))
        try {
            PackStore.write(
                context,
                previousPacks + StickerPack(
                    identifier = identifier,
                    name = "Teste de uma figurinha",
                    stickers = listOf(StickerItem(fileName)),
                ),
            )
            val stickersUri =
                "content://${BuildConfig.CONTENT_PROVIDER_AUTHORITY}/stickers/$identifier".toUri()
            context.contentResolver.query(stickersUri, null, null, null, null).use { cursor ->
                assertEquals(1, requireNotNull(cursor).count)
            }
            val aliasUri =
                "content://${BuildConfig.CONTENT_PROVIDER_AUTHORITY}/stickers_asset/$identifier/kolaki_compat_0.webp"
                    .toUri()
            context.contentResolver.openAssetFileDescriptor(aliasUri, "r").use { descriptor ->
                assertNull(descriptor)
            }
        } finally {
            PackStore.write(context, previousPacks)
            directory.deleteRecursively()
        }
    }

    private fun assertAnimatedSticker(context: android.content.Context, webp: File) {
        assertTrue(webp.isFile)
        assertTrue(webp.length() in 1..PackRules.MAX_ANIMATED_BYTES.toLong())
        assertTrue(ImageProcessor.isAnimated(context, Uri.fromFile(webp)))
        val firstFrame = requireNotNull(BitmapFactory.decodeFile(webp.absolutePath))
        assertEquals(PackRules.STICKER_SIZE, firstFrame.width)
        assertEquals(PackRules.STICKER_SIZE, firstFrame.height)
        firstFrame.recycle()
    }

    private companion object {
        const val TEST_GIF_BASE64 =
            "R0lGODlhQABAAIEAAAAAAP9XMwAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh+QQJDAAAACwAAAAAQABAAAAIxQABCBxIsKDBgwgTKlzIsKHDhxAjSpxIsaLFixgzatzIsaPHjyBDihxJsqTJkyhTqlzJsqXLlzBjymQooKbNmzZl4tx5E6bNAECDCg2Qs+XPoUiJ1jQqIKnTpSxrOk0KdaXUqUOrqryKNajWlFy7KmUq1qsAsmXHRm2a9ivKsF3dnoSLVa5JulPtlsT79OzatGqtsi2rlyRfqn4FD65b2PBhoEVd8pysc3LPmZgza97MubPnz6BDix5NurTp06hTq17NWmRAACH5BAkMAAAALBYAFAATABcAgQAAAL//AAAAAAAAAAhWAAEIFECwoMGCAhMeXGhQIcEAECNKDICw4MSLEA9i3GhwI8aOHieCDBlxJEmKFk9mTKnSJEmXIWF6lMmR5UmaH22+1BkTp8SKD3sKcMhwYcKBRRsKDAgAIfkECQwAAAAsKAAUABMAFwCBAAAA/9I/AAAAAAAACFYAAQgUQLCgwYICEx5caFAhwQAQI0oMgLDgxIsQD2LcaHAjxo4eJ4IMGXEkSYoWT2ZMqdIkSZchYXqUyZHlSZofbb7UGROnxIoPewpwyHBhwoFFGwoMCAA7"
    }
}
