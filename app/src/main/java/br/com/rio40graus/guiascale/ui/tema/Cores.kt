package br.com.rio40graus.guiascale.ui.tema

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * As cores do app web, convertidas uma a uma.
 *
 * A fonte é o `:root` de scale-guide-pro/src/index.css, onde os tokens estão
 * em HSL. Aqui ficam em hexadecimal porque é o que o Compose lê — mas o
 * comentário de cada linha guarda o HSL original, para quem for conferir
 * conseguir voltar ao arquivo do web sem recalcular nada.
 *
 * Mexer aqui sem mexer lá faz os dois apps divergirem, que é exatamente o que
 * este arquivo existe para evitar.
 */

// ------------------------------------------------------------------- claro
private val FundoClaro = Color(0xFFF8F8F6)          // 60 14% 97%
private val TextoClaro = Color(0xFF152826)          // 175 30% 12%
private val CartaoClaro = Color(0xFFFFFFFF)         // 0 0% 100%
private val PrimariaClara = Color(0xFF1C4D97)       // 216 69% 35% — o azul da marca
private val AcentoClaro = Color(0xFFE9F0FB)         // 216 69% 95%
private val TextoAcentoClaro = Color(0xFF184281)    // 216 69% 30%
private val NeutroClaro = Color(0xFFEFEFEB)         // 60 10% 93%
private val TextoNeutroClaro = Color(0xFF506261)    // 175 10% 35%
private val BordaClara = Color(0xFFE3E3DE)          // 60 8% 88%
private val ErroClaro = Color(0xFFDF2020)           // 0 75% 50%

// -------------------------------------------------------------------- escuro
private val FundoEscuro = Color(0xFF0E1B19)         // 175 30% 8%
private val TextoEscuro = Color(0xFFF4F4F0)         // 60 14% 95%
private val CartaoEscuro = Color(0xFF122120)        // 175 30% 10%
private val PrimariaEscura = Color(0xFF286ED7)      // 216 69% 50%
private val AcentoEscuro = Color(0xFF11305F)        // 216 69% 22%
private val NeutroEscuro = Color(0xFF1D302E)        // 175 25% 15%
private val TextoNeutroEscuro = Color(0xFF9DAFAD)   // 175 10% 65%
private val BordaEscura = Color(0xFF223937)         // 175 25% 18%

/*
 * `success` e `warning` não existem no ColorScheme do Material — ele não tem
 * esse par de papéis. Ficam de fora, em CoresExtras, para o estado do rastreio
 * usar o mesmo verde que o guia já vê no app web.
 */
object CoresExtras {
    val Sucesso = Color(0xFF22A050)                 // 142 65% 38%
    val Aviso = Color(0xFFF9B006)                   // 42 95% 50%
    val TextoAviso = Color(0xFF32261B)              // 30 30% 15%
}

/*
 * As quatro famílias de `surface` recebem o mesmo branco de propósito: no web
 * todo cartão é branco puro sobre o fundo levemente creme, e o Material, se
 * deixado por conta própria, escurece cada nível um pouco mais — o que
 * afastaria a tela do original a cada componente novo.
 */
val EsquemaClaro = lightColorScheme(
    primary = PrimariaClara,
    onPrimary = Color.White,
    primaryContainer = AcentoClaro,
    onPrimaryContainer = TextoAcentoClaro,
    secondary = PrimariaClara,
    onSecondary = Color.White,
    secondaryContainer = AcentoClaro,
    onSecondaryContainer = TextoAcentoClaro,
    tertiary = CoresExtras.Sucesso,
    onTertiary = Color.White,
    background = FundoClaro,
    onBackground = TextoClaro,
    surface = FundoClaro,
    onSurface = TextoClaro,
    surfaceVariant = NeutroClaro,
    onSurfaceVariant = TextoNeutroClaro,
    surfaceContainerLowest = CartaoClaro,
    surfaceContainerLow = CartaoClaro,
    surfaceContainer = CartaoClaro,
    surfaceContainerHigh = CartaoClaro,
    surfaceContainerHighest = CartaoClaro,
    outline = BordaClara,
    outlineVariant = BordaClara,
    error = ErroClaro,
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF7A1111),
)

val EsquemaEscuro = darkColorScheme(
    primary = PrimariaEscura,
    onPrimary = Color.White,
    primaryContainer = AcentoEscuro,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF163E79),                  // 216 69% 28%
    onSecondary = Color.White,
    secondaryContainer = AcentoEscuro,
    onSecondaryContainer = Color.White,
    tertiary = CoresExtras.Sucesso,
    onTertiary = Color.White,
    background = FundoEscuro,
    onBackground = TextoEscuro,
    surface = FundoEscuro,
    onSurface = TextoEscuro,
    surfaceVariant = NeutroEscuro,
    onSurfaceVariant = TextoNeutroEscuro,
    surfaceContainerLowest = CartaoEscuro,
    surfaceContainerLow = CartaoEscuro,
    surfaceContainer = CartaoEscuro,
    surfaceContainerHigh = CartaoEscuro,
    surfaceContainerHighest = CartaoEscuro,
    outline = BordaEscura,
    outlineVariant = BordaEscura,
    error = ErroClaro,
    onError = Color.White,
)
