@file:OptIn(ExperimentalTextApi::class)

package br.com.rio40graus.guiascale.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.R

/*
 * As duas fontes do app web: DM Sans nos títulos, Inter no texto corrido.
 *
 * Os arquivos são variáveis — um TTF só que cobre do Thin ao Black pelo eixo
 * `wght`. Por isso cada peso abaixo aponta para o MESMO recurso, mudando só o
 * FontVariation: é o que evita carregar seis arquivos estáticos e o que mantém
 * o APK do tamanho de dois. Funciona a partir do Android 8, e o minSdk do app
 * já é 26.
 */

private fun peso(valor: Int) = FontVariation.Settings(FontVariation.weight(valor))

private val DmSans = FontFamily(
    Font(R.font.dm_sans, FontWeight.Medium, variationSettings = peso(500)),
    Font(R.font.dm_sans, FontWeight.SemiBold, variationSettings = peso(600)),
    Font(R.font.dm_sans, FontWeight.Bold, variationSettings = peso(700)),
)

private val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = peso(400)),
    Font(R.font.inter, FontWeight.Medium, variationSettings = peso(500)),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = peso(600)),
)

/*
 * Os tamanhos saem do Tailwind do web: text-2xl = 24, base = 16, sm = 14,
 * xs = 12. O espaçamento negativo nos títulos é o `tracking-tight` que o
 * index.css aplica em todo h1..h6 — sem ele o título fica visivelmente mais
 * largo que o do navegador.
 */
val TipografiaGuiaScale = Typography(
    headlineMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // O rótulo de campo e o texto de botão do web são ambos 14px/medium.
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
