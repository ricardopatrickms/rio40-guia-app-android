package br.com.rio40graus.guiascale.ui.tema

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/*
 * Os raios do web, em dp.
 *
 * O `--radius` do index.css é 0.875rem = 14px, e os componentes derivam dele:
 * campo e botão em 12 (rounded-md / rounded-xl), cartão em 16 (rounded-2xl).
 * O Material não deixa o Shapes mandar na forma do Button nem do
 * OutlinedTextField — os dois usam tokens próprios, um deles totalmente
 * arredondado. Por isso as três formas abaixo também existem soltas: são
 * passadas na mão em cada componente da tela.
 */
val FormaCampo = RoundedCornerShape(12.dp)
val FormaBotao = RoundedCornerShape(12.dp)
val FormaCartao = RoundedCornerShape(16.dp)

/** Botões de barra (Atualizar, filtros): o `rounded-md` do web, 10dp. */
val FormaBotaoPequeno = RoundedCornerShape(10.dp)

private val FormasGuiaScale = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = FormaCampo,
    medium = RoundedCornerShape(14.dp),
    large = FormaCartao,
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * O tema do app.
 *
 * Sem `dynamicColor` de propósito. A partir do Android 12 o Material tinge o
 * app com as cores do papel de parede do aparelho — foi o que deixou a tela
 * roxa e sem relação nenhuma com o azul da marca. Aqui a paleta é fixa, e é a
 * mesma que o guia vê no navegador.
 */
@Composable
fun TemaGuiaScale(
    escuro: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val esquema = if (escuro) EsquemaEscuro else EsquemaClaro
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val janela = (view.context as Activity).window
            // Sem isto os ícones do relógio e da bateria nascem brancos, e no
            // fundo claro do app eles somem.
            WindowCompat.getInsetsController(janela, view)
                .isAppearanceLightStatusBars = !escuro
        }
    }

    MaterialTheme(
        colorScheme = esquema,
        typography = TipografiaGuiaScale,
        shapes = FormasGuiaScale,
        content = content,
    )
}
