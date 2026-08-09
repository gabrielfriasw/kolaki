package com.kolvexo.kolaki.provider

/*
 * segue o contrato de uri e colunas da amostra oficial de figurinhas do WhatsApp
 * copyright Meta Platforms, Inc., sob licença BSD
 */

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.kolvexo.kolaki.BuildConfig
import com.kolvexo.kolaki.data.PackStore
import com.kolvexo.kolaki.data.StickerPack

class StickerContentProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        val appContext = requireNotNull(context)
        require(BuildConfig.CONTENT_PROVIDER_AUTHORITY.startsWith(appContext.packageName)) {
            "A autoridade do provider precisa começar com o applicationId"
        }
        matcher.addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, METADATA, METADATA_ALL)
        matcher.addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, "$METADATA/*", METADATA_ONE)
        matcher.addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, "$STICKERS/*", STICKERS_FOR_PACK)
        matcher.addURI(BuildConfig.CONTENT_PROVIDER_AUTHORITY, "$ASSETS/*/*", STICKER_ASSET)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = when (matcher.match(uri)) {
        METADATA_ALL -> metadataCursor(uri, packs())
        METADATA_ONE -> metadataCursor(
            uri,
            packs().filter { it.identifier == uri.lastPathSegment },
        )
        STICKERS_FOR_PACK -> stickersCursor(uri, uri.lastPathSegment.orEmpty())
        else -> throw IllegalArgumentException("URI desconhecida: $uri")
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        require(mode == "r") { "O provider é somente leitura" }
        if (matcher.match(uri) != STICKER_ASSET) return null
        val segments = uri.pathSegments
        if (segments.size != 3) return null
        val packId = segments[1]
        val fileName = segments[2]
        val pack = packs().firstOrNull { it.identifier == packId } ?: return null
        val allowed = fileName == pack.trayImageFile || pack.stickers.any { it.fileName == fileName }
        if (!allowed) return null
        val file = PackStore.imageFile(requireNotNull(context), packId, fileName)
        if (!file.isFile) return null
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(descriptor, 0, file.length())
    }

    override fun getType(uri: Uri): String = when (matcher.match(uri)) {
        METADATA_ALL -> "vnd.android.cursor.dir/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$METADATA"
        METADATA_ONE -> "vnd.android.cursor.item/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$METADATA"
        STICKERS_FOR_PACK -> "vnd.android.cursor.dir/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$STICKERS"
        STICKER_ASSET -> if (uri.lastPathSegment?.endsWith(".png") == true) "image/png" else "image/webp"
        else -> throw IllegalArgumentException("URI desconhecida: $uri")
    }

    private fun metadataCursor(uri: Uri, packs: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(METADATA_COLUMNS)
        packs.forEach { pack ->
            cursor.addRow(
                arrayOf<Any?>(
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    pack.trayImageFile,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    pack.imageDataVersion.toString(),
                    0,
                    if (pack.isAnimated) 1 else 0,
                ),
            )
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    private fun stickersCursor(uri: Uri, packId: String): Cursor {
        val cursor = MatrixCursor(STICKER_COLUMNS)
        packs().firstOrNull { it.identifier == packId }?.stickers?.forEach { sticker ->
            cursor.addRow(
                arrayOf<Any?>(
                    sticker.fileName,
                    sticker.emojis.joinToString(","),
                    sticker.accessibilityText,
                ),
            )
        }
        cursor.setNotificationUri(requireNotNull(context).contentResolver, uri)
        return cursor
    }

    private fun packs(): List<StickerPack> = PackStore.read(requireNotNull(context))

    override fun insert(uri: Uri, values: ContentValues?): Uri = readOnly()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = readOnly()
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = readOnly()

    private fun <T> readOnly(): T = throw UnsupportedOperationException("Provider somente leitura")

    companion object {
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val ASSETS = "stickers_asset"
        private const val METADATA_ALL = 1
        private const val METADATA_ONE = 2
        private const val STICKERS_FOR_PACK = 3
        private const val STICKER_ASSET = 4

        private val METADATA_COLUMNS = arrayOf(
            "sticker_pack_identifier",
            "sticker_pack_name",
            "sticker_pack_publisher",
            "sticker_pack_icon",
            "android_play_store_link",
            "ios_app_download_link",
            "sticker_pack_publisher_email",
            "sticker_pack_publisher_website",
            "sticker_pack_privacy_policy_website",
            "sticker_pack_license_agreement_website",
            "image_data_version",
            "whatsapp_will_not_cache_stickers",
            "animated_sticker_pack",
        )
        private val STICKER_COLUMNS = arrayOf(
            "sticker_file_name",
            "sticker_emoji",
            "sticker_accessibility_text",
        )

        val metadataUri: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            .appendPath(METADATA)
            .build()
    }
}
