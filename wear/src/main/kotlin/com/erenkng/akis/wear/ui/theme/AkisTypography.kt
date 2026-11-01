package com.erenkng.akis.wear.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.erenkng.akis.wear.R

/** Manrope, `res/font` içine gömülü (OFL). İnternet gerektirmez. */
val Manrope = FontFamily(
    Font(R.font.manrope_300, FontWeight.Light),
    Font(R.font.manrope_500, FontWeight.Medium),
    Font(R.font.manrope_600, FontWeight.SemiBold),
    Font(R.font.manrope_800, FontWeight.ExtraBold),
)

/**
 * BRIEF Bölüm 4, "Tipografi". Tüm metinler bu stilleri açıkça kullanır; böylece Material
 * bileşenlerinin varsayılan yazı tipi Akış'ın görünümüne sızmaz.
 * `TextDirection.Content`: Arapça şarkı adları gibi sağdan sola metinler doğru yönde akar.
 */
object AkisType {
    private val base = TextStyle(fontFamily = Manrope, color = AkisColors.OnSurface, textDirection = TextDirection.Content)

    val trackTitle = base.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    val artist = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AkisColors.OnSurfaceDim)
    val sourceLabel = base.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em, color = AkisColors.OnSurfaceDim)
    val duration = base.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum", color = AkisColors.OnSurfaceDim)
    val volumeNumber = base.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")

    val screenTitle = base.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    val sectionHeader = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AkisColors.OnSurfaceDim)
    val body = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val bodyDim = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AkisColors.OnSurfaceDim)
    val button = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val listTitle = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val listSubtitle = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AkisColors.OnSurfaceDim)
}
