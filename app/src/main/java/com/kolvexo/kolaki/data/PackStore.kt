package com.kolvexo.kolaki.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PackStore {
    private const val STORE_FILE = "packs.json"
    private const val PACKS_DIRECTORY = "packs"

    fun packsDirectory(context: Context): File = File(context.filesDir, PACKS_DIRECTORY).also(File::mkdirs)

    fun packDirectory(context: Context, identifier: String): File =
        safeChild(packsDirectory(context), identifier).also(File::mkdirs)

    fun imageFile(context: Context, packId: String, fileName: String): File =
        safeChild(packDirectory(context, packId), fileName)

    fun read(context: Context): List<StickerPack> {
        val file = File(context.filesDir, STORE_FILE)
        if (!file.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val packs = root.optJSONArray("packs") ?: JSONArray()
            buildList {
                for (index in 0 until packs.length()) {
                    val item = packs.getJSONObject(index)
                    val stickersJson = item.optJSONArray("stickers") ?: JSONArray()
                    val stickers = buildList {
                        for (stickerIndex in 0 until stickersJson.length()) {
                            val sticker = stickersJson.getJSONObject(stickerIndex)
                            val emojiJson = sticker.optJSONArray("emojis") ?: JSONArray()
                            add(
                                StickerItem(
                                    fileName = sticker.getString("fileName"),
                                    animated = sticker.optBoolean("animated", false),
                                    sourceFileName = sticker.optString("sourceFileName")
                                        .takeIf(String::isNotBlank),
                                    editScale = sticker.optDouble("editScale", 1.0).toFloat(),
                                    editOffsetX = sticker.optDouble("editOffsetX", 0.0).toFloat(),
                                    editOffsetY = sticker.optDouble("editOffsetY", 0.0).toFloat(),
                                    rotationQuarterTurns = sticker.optInt("rotationQuarterTurns", 0),
                                    emojis = buildList {
                                        for (emojiIndex in 0 until emojiJson.length()) {
                                            add(emojiJson.getString(emojiIndex))
                                        }
                                    }.ifEmpty { listOf("😀") },
                                    accessibilityText = sticker.optString(
                                        "accessibilityText",
                                        "Figurinha criada no KolaKi",
                                    ),
                                ),
                            )
                        }
                    }
                    add(
                        StickerPack(
                            identifier = item.getString("identifier"),
                            name = item.getString("name"),
                            publisher = item.optString("publisher", "KolaKi"),
                            trayImageFile = item.optString("trayImageFile", "tray.png"),
                            imageDataVersion = item.optInt("imageDataVersion", 1),
                            stickers = stickers,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun write(context: Context, packs: List<StickerPack>) {
        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("packs", JSONArray().apply {
                packs.forEach { pack ->
                    put(JSONObject().apply {
                        put("identifier", pack.identifier)
                        put("name", pack.name)
                        put("publisher", pack.publisher)
                        put("trayImageFile", pack.trayImageFile)
                        put("imageDataVersion", pack.imageDataVersion)
                        put("stickers", JSONArray().apply {
                            pack.stickers.forEach { sticker ->
                                put(JSONObject().apply {
                                    put("fileName", sticker.fileName)
                                    put("animated", sticker.animated)
                                    put("sourceFileName", sticker.sourceFileName)
                                    put("editScale", sticker.editScale)
                                    put("editOffsetX", sticker.editOffsetX)
                                    put("editOffsetY", sticker.editOffsetY)
                                    put("rotationQuarterTurns", sticker.rotationQuarterTurns)
                                    put("emojis", JSONArray(sticker.emojis))
                                    put("accessibilityText", sticker.accessibilityText)
                                })
                            }
                        })
                    })
                }
            })
        }

        val atomicFile = AtomicFile(File(context.filesDir, STORE_FILE))
        val stream = atomicFile.startWrite()
        try {
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    private fun safeChild(parent: File, name: String): File {
        require(name.isNotBlank() && name !in setOf(".", "..")) { "Nome de arquivo inválido" }
        require('/' !in name && '\\' !in name) { "Caminho de arquivo inválido" }
        return File(parent, name)
    }
}
