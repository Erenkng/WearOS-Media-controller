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
 * Tüm metinler bu stilleri açıkça kullanır; Material bileşenlerinin varsayılan yazı tipi Akış'ın görünümüne sızmaz.
 * `TextDirection.Content`: Arapça şarkı adları gibi sağdan sola metinler doğru yönde akar.
 *
 * Oynatıcı ölçüleri, kullanıcının referans ekranlarındaki oranlara göre (225 dp çaplı ekran):
 * saat ~18 sp, şarkı adı ~22 sp, sanatçı ~16 sp; Odak ekranında başlık 24 sp.
 */
object AkisType {
    private val base = TextStyle(fontFamily = Manrope, color = AkisColors.OnSurface, textDirection = TextDirection.Content)

    // Oynatıcı
    val clock = base.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    val trackTitle = base.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
    val artist = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    val live = base.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em)
    val volumeValue = base.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")

    // Odak Modu
    val focusClock = base.copy(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")
    val focusTitle = base.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp)
    val focusArtist = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

    // Listeler ve diğer ekranlar
    val sourceLabel = base.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em, color = AkisColors.OnSurfaceDim)
    val screenTitle = base.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    val sectionHeader = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AkisColors.OnSurfaceDim)
    val body = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
    val bodyDim = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AkisColors.OnSurfaceDim)
    val button = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val listTitle = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val listSubtitle = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AkisColors.OnSurfaceDim)
}
