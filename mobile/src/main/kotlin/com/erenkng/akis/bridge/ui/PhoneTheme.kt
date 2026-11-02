package com.erenkng.akis.bridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** BRIEF Bölüm 4, renk token'ları (telefon tarafı). */
object PhoneColors {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF141416)
    val SurfaceHigh = Color(0xFF202024)
    val OnSurface = Color(0xFFF2F0EB)
    val OnSurfaceDim = Color(0xFF9C9890)
    val Accent = Color(0xFFFF8A3D)
    val Danger = Color(0xFFFF5A5F)
}

private val AkisPhoneColorScheme = darkColorScheme(
    primary = PhoneColors.Accent,
    onPrimary = PhoneColors.Bg,
    background = PhoneColors.Bg,
    onBackground = PhoneColors.OnSurface,
    surface = PhoneColors.Surface,
    onSurface = PhoneColors.OnSurface,
    surfaceVariant = PhoneColors.SurfaceHigh,
    onSurfaceVariant = PhoneColors.OnSurfaceDim,
    error = PhoneColors.Danger,
)

@Composable
fun AkisPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AkisPhoneColorScheme, content = content)
}

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PhoneColors.Surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, color = PhoneColors.OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
