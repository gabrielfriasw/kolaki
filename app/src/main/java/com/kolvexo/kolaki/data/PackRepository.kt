package com.kolvexo.kolaki.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.kolvexo.kolaki.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class PackRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _packs = MutableStateFlow(PackStore.read(context))
    val packs: StateFlow<List<StickerPack>> = _packs.asStateFlow()

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean> = _processing.asStateFlow()

    fun stickerFile(packId: String, fileName: String): File =
        PackStore.imageFile(context, packId, fileName)

    suspend fun createPack(name: String): StickerPack = mutex.withLock {
        val pack = StickerPack(
            identifier = PackRules.safeIdentifier(name),
            name = name.trim().take(128).ifBlank { "Meu pacote" },
        )
        update(_packs.value + pack)
        pack
    }

    suspend fun addImages(packId: String, uris: List<Uri>): Result<Int> = runCatching {
        mutex.withLock {
            _processing.value = true
            try {
                val current = _packs.value.first { it.identifier == packId }
                val room = PackRules.MAX_STICKERS - current.stickers.size
                val selected = uris.take(room)
                val selectedKinds = selected.map { ImageProcessor.isAnimated(context, it) }
                val expectedAnimated = current.stickers.firstOrNull()?.animated ?: selectedKinds.firstOrNull()
                require(selectedKinds.all { it == expectedAnimated }) {
                    "Um pacote não pode misturar GIFs e imagens estáticas. Selecione apenas um tipo."
                }
                val added = mutableListOf<StickerItem>()
                val createdFiles = mutableListOf<File>()
                try {
                    selected.forEachIndexed { index, uri ->
                        val sequence = current.stickers.size + index + 1
                        val stem = "fig_${sequence}_${System.nanoTime().toString(36)}"
                        val fileName = "$stem.webp"
                        val destination = stickerFile(packId, fileName)
                        val animated = selectedKinds[index]
                        val sourceFileName = if (animated) {
                            "$stem-source${ImageProcessor.animatedSourceExtension(context, uri)}"
                        } else {
                            null
                        }
                        if (sourceFileName != null) {
                            // mantém a fonte animada para edições sem perda de quadros
                            val original = stickerFile(packId, sourceFileName)
                            ImageProcessor.copySource(context, uri, original)
                            createdFiles += original
                        }
                        ImageProcessor.createSticker(context, uri, destination)
                        createdFiles += destination
                        added += StickerItem(
                            fileName = fileName,
                            animated = animated,
                            sourceFileName = sourceFileName,
                        )
                    }
                } catch (error: Throwable) {
                    createdFiles.forEach(File::delete)
                    throw error
                }
                if (added.isNotEmpty()) {
                    val updatedPack = current.copy(
                        stickers = current.stickers + added,
                        imageDataVersion = current.imageDataVersion + 1,
                    )
                    val tray = stickerFile(packId, updatedPack.trayImageFile)
                    if (!tray.exists()) {
                        ImageProcessor.createTray(
                            stickerFile(packId, updatedPack.stickers.first().fileName),
                            tray,
                        )
                    }
                    update(_packs.value.map { if (it.identifier == packId) updatedPack else it })
                }
                added.size
            } finally {
                _processing.value = false
            }
        }
    }

    suspend fun editSticker(packId: String, fileName: String, edit: StickerEdit): Result<Unit> = runCatching {
        mutex.withLock {
            _processing.value = true
            try {
                val stickerFile = stickerFile(packId, fileName)
                val pack = _packs.value.first { it.identifier == packId }
                val sticker = pack.stickers.first { it.fileName == fileName }
                val editedSticker = when (edit) {
                    StickerEdit.Rotate -> if (sticker.animated) {
                        sticker.copy(
                            editOffsetX = -sticker.editOffsetY,
                            editOffsetY = sticker.editOffsetX,
                            rotationQuarterTurns = (sticker.rotationQuarterTurns + 1).mod(4),
                        ).also { renderAnimated(packId, it, stickerFile) }
                    } else {
                        ImageProcessor.rotateSticker(stickerFile)
                        sticker
                    }

                    is StickerEdit.Crop -> if (sticker.animated) {
                        val scale = edit.scale.coerceIn(1f, 5f)
                        val combinedScale = (sticker.editScale * scale).coerceIn(1f, 12f)
                        val maxOffset = (combinedScale - 1f) / 2f
                        sticker.copy(
                            editScale = combinedScale,
                            editOffsetX = (sticker.editOffsetX * scale + edit.offsetX)
                                .coerceIn(-maxOffset, maxOffset),
                            editOffsetY = (sticker.editOffsetY * scale + edit.offsetY)
                                .coerceIn(-maxOffset, maxOffset),
                        ).also { renderAnimated(packId, it, stickerFile) }
                    } else {
                        ImageProcessor.cropSticker(
                            source = stickerFile,
                            scale = edit.scale,
                            offsetX = edit.offsetX,
                            offsetY = edit.offsetY,
                        )
                        sticker
                    }
                }
                if (pack.stickers.firstOrNull()?.fileName == fileName) {
                    ImageProcessor.createTray(stickerFile, stickerFile(packId, pack.trayImageFile))
                }
                update(_packs.value.map {
                    if (it.identifier == packId) {
                        it.copy(
                            stickers = it.stickers.map { item ->
                                if (item.fileName == fileName) editedSticker else item
                            },
                            imageDataVersion = it.imageDataVersion + 1,
                        )
                    } else it
                })
            } finally {
                _processing.value = false
            }
        }
    }

    private suspend fun renderAnimated(packId: String, sticker: StickerItem, destination: File) {
        val sourceFileName = requireNotNull(sticker.sourceFileName) {
            "O GIF original desta figurinha não está disponível para edição"
        }
        ImageProcessor.renderAnimatedSticker(
            originalSource = stickerFile(packId, sourceFileName),
            destination = destination,
            scale = sticker.editScale,
            offsetX = sticker.editOffsetX,
            offsetY = sticker.editOffsetY,
            rotationQuarterTurns = sticker.rotationQuarterTurns,
        )
    }

    fun refresh() {
        scope.launch { _packs.value = PackStore.read(context) }
    }

    private fun update(packs: List<StickerPack>) {
        PackStore.write(context, packs)
        _packs.value = packs
        val resolver = context.contentResolver
        val base = "content://${BuildConfig.CONTENT_PROVIDER_AUTHORITY}"
        resolver.notifyChange("$base/metadata".toUri(), null)
        packs.forEach { resolver.notifyChange("$base/stickers/${it.identifier}".toUri(), null) }
    }
}
