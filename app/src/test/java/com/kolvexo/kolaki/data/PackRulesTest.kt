package com.kolvexo.kolaki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackRulesTest {
    @Test
    fun identifierUsesOnlyCharactersAcceptedByWhatsApp() {
        val id = PackRules.safeIdentifier("  Memes do João! 🤝  ", seed = 42)
        assertEquals("memes_do_joo_16", id)
        assertTrue(id.matches(Regex("[a-zA-Z0-9_. -]+")))
    }

    @Test
    fun blankNameGetsStableFallback() {
        assertEquals("pacote_1", PackRules.safeIdentifier("✨", seed = 1))
    }

    @Test
    fun packIsReadyWithThreeStickers() {
        val empty = StickerPack("id", "Pacote")
        val one = empty.copy(stickers = listOf(StickerItem("1.webp")))
        val three = empty.copy(
            stickers = listOf(
                StickerItem("1.webp"),
                StickerItem("2.webp"),
                StickerItem("3.webp"),
            ),
        )
        assertFalse(empty.isReady)
        assertFalse(one.isReady)
        assertTrue(three.isReady)
    }
}
