package com.kolvexo.kolaki.data

data class StickerItem(
    val fileName: String,
    val animated: Boolean = false,
    val sourceFileName: String? = null,
    val editScale: Float = 1f,
    val editOffsetX: Float = 0f,
    val editOffsetY: Float = 0f,
    val rotationQuarterTurns: Int = 0,
    val emojis: List<String> = listOf("😀"),
    val accessibilityText: String = "Figurinha criada no KolaKi",
)

data class StickerPack(
    val identifier: String,
    val name: String,
    val publisher: String = "KolaKi",
    val trayImageFile: String = "tray.png",
    val imageDataVersion: Int = 1,
    val stickers: List<StickerItem> = emptyList(),
) {
    val isReady: Boolean get() = stickers.size >= PackRules.MIN_STICKERS
    val isAnimated: Boolean get() = stickers.isNotEmpty() && stickers.all(StickerItem::animated)
}

object PackRules {
    const val MIN_STICKERS = 3
    const val MAX_STICKERS = 30
    const val STICKER_SIZE = 512
    const val TRAY_SIZE = 96
    const val MAX_STATIC_BYTES = 100 * 1024
    const val MAX_ANIMATED_BYTES = 500 * 1024
    const val MAX_ANIMATION_DURATION_MS = 10_000

    fun safeIdentifier(name: String, seed: Long = System.currentTimeMillis()): String {
        val base = name
            .lowercase()
            .replace(Regex("[^a-z0-9._ -]"), "")
            .trim()
            .replace(Regex("[ .]+"), "_")
            .take(72)
            .ifBlank { "pacote" }
        return "${base}_${seed.toString(36)}"
    }
}

sealed interface StickerEdit {
    data object Rotate : StickerEdit

    data class Crop(
        val scale: Float,
        val offsetX: Float,
        val offsetY: Float,
    ) : StickerEdit
}
