package com.erenkng.akis.wear

import com.erenkng.akis.shared.model.CustomActionInfo
import com.erenkng.akis.wear.player.FocusGesture
import com.erenkng.akis.wear.player.FocusGestureTiming
import com.erenkng.akis.wear.player.LikeAction
import com.erenkng.akis.wear.player.LikeActionPolicy
import com.erenkng.akis.wear.ui.theme.ColorRules
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorRulesTest {

    @Test
    fun contrastOfKnownColours() {
        assertEquals(21.0, ColorRules.contrastOnBlack(0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, ColorRules.contrastOnBlack(0xFF000000.toInt()), 0.01)
    }

    @Test
    fun hslRoundTrip() {
        val colours = listOf(0xFFFF8A3D, 0xFF1E88E5, 0xFF7CB342, 0xFF8E24AA, 0xFF26A69A).map { it.toInt() }
        colours.forEach { argb ->
            val (h, s, l) = ColorRules.rgbToHsl(argb)
            val back = ColorRules.hslToRgb(h, s, l)
            listOf(16, 8, 0).forEach { shift ->
                val a = (argb shr shift) and 0xFF
                val b = (back shr shift) and 0xFF
                assertTrue(kotlin.math.abs(a - b) <= 2, "kanal $shift: $a ≠ $b")
            }
        }
    }

    /** Kabul kriteri (Faz 2): dinamik renk hiçbir kapakta 4.5:1 kontrastın altına düşmez. */
    @Test
    fun everyAccentMeetsContrastAndAvoidsBannedHue() {
        val random = Random(42)
        repeat(20_000) {
            val argb = (0xFF shl 24) or random.nextInt(0x1000000)
            val accent = ColorRules.accentFrom(argb) ?: return@repeat
            assertTrue(ColorRules.contrastOnBlack(accent) >= ColorRules.MIN_CONTRAST, "kontrast düşük: ${Integer.toHexString(argb)}")
            val (hue, saturation, _) = ColorRules.rgbToHsl(accent)
            if (saturation > 0.05f) assertFalse(ColorRules.isBannedHue(hue), "yasaklı ton: $hue (${Integer.toHexString(argb)})")
        }
    }

    @Test
    fun darkCoverColoursAreLightened() {
        val darkNavy = 0xFF0D1B3E.toInt()
        val accent = assertNotNull(ColorRules.accentFrom(darkNavy))
        assertTrue(ColorRules.contrastOnBlack(accent) >= 4.5)
        assertTrue(ColorRules.rgbToHsl(accent).third >= 0.55f)
    }

    @Test
    fun greyCoversFallBackToFixedAccent() {
        assertNull(ColorRules.accentFrom(0xFF808080.toInt()))
        assertNull(ColorRules.accentFrom(0xFF101010.toInt()))
    }

    @Test
    fun bannedGreenIsShifted() {
        assertEquals(165f, ColorRules.avoidBannedHue(145f))
        assertEquals(115f, ColorRules.avoidBannedHue(135f))
        assertEquals(100f, ColorRules.avoidBannedHue(100f))
    }
}

class FocusGestureTest {
    @Test
    fun classifiesGestures() {
        assertEquals(FocusGesture.TAP, FocusGestureTiming.classify(80, null, doubleTapEnabled = true))
        assertEquals(FocusGesture.TAP, FocusGestureTiming.classify(80, 400, doubleTapEnabled = true))
        assertEquals(FocusGesture.DOUBLE_TAP, FocusGestureTiming.classify(80, 150, doubleTapEnabled = true))
        assertEquals(FocusGesture.TAP, FocusGestureTiming.classify(80, 150, doubleTapEnabled = false))
        assertEquals(FocusGesture.LONG_PRESS, FocusGestureTiming.classify(650, null, doubleTapEnabled = true))
        assertEquals(FocusGesture.TAP, FocusGestureTiming.classify(599, null, doubleTapEnabled = true))
    }
}

class LikeActionPolicyTest {
    private fun action(label: String, id: String = label.lowercase()) = CustomActionInfo(id, label)

    @Test
    fun findsLikeAndItsState() {
        assertEquals(LikeAction("beğen", false), LikeActionPolicy.find(listOf(action("Beğen"))))
        assertEquals(LikeAction("add", false), LikeActionPolicy.find(listOf(action("Add to Liked Songs", "add"))))
        assertEquals(LikeAction("rm", true), LikeActionPolicy.find(listOf(action("Remove from Liked Songs", "rm"))))
        assertEquals(LikeAction("x", true), LikeActionPolicy.find(listOf(action("Beğenmekten vazgeç", "x"))))
        assertEquals(LikeAction("fav", false), LikeActionPolicy.find(listOf(action("Favorilere ekle", "fav"))))
        assertEquals(LikeAction("unfav", true), LikeActionPolicy.find(listOf(action("Favorilerden çıkar", "unfav"))))
    }

    @Test
    fun ignoresDislikeAndUnrelatedActions() {
        assertNull(LikeActionPolicy.find(listOf(action("Beğenme"), action("Shuffle"), action("Radyo başlat"))))
        assertNull(LikeActionPolicy.find(listOf(action("Thumbs down"))))
        assertNull(LikeActionPolicy.find(emptyList()))
        assertEquals("up", LikeActionPolicy.find(listOf(action("Beğenme", "down"), action("Beğen", "up")))?.actionId)
    }
}
