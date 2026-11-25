package br.com.rio40graus.guiascale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.EvolucaoMes
import br.com.rio40graus.guiascale.rede.PasseioRanking
import br.com.rio40graus.guiascale.rede.TransporteRanking
import br.com.rio40graus.guiascale.ui.tema.CoresExtras
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

/*
 * Os três blocos do fim da Visão Geral do web (pages/guia/VisaoGeral.tsx):
 * "Evolução — últimos 6 meses", "Tipos de transporte" e "Ranking de passeios".
 * Os dados já vêm no GET guia/painel (KpisDoPainel); aqui é só o desenho, em
 * Canvas, sem biblioteca de gráfico.
 */

/** Cartão no mesmo formato dos outros blocos do painel. */
@Composable
private fun CartaoGrafico(
    titulo: String,
    comIcone: Boolean = false,
    conteudo: @Composable () -> Unit,
) {
    Surface(
        shape = FormaCartao,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (comIcone) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            conteudo()
        }
    }
}

/** Teto "redondo" do eixo Y (8000 para 6158, 10 para 7…), como o recharts. */
private fun tetoDoEixo(maior: Double): Double {
    if (maior <= 0.0) return 4.0
    val ordem = 10.0.pow(kotlin.math.floor(log10(maior)))
    val passos = listOf(1.0, 2.0, 2.5, 4.0, 5.0, 8.0, 10.0)
    val n = passos.first { it * ordem >= maior }
    return n * ordem
}

private fun formatarInteiro(v: Double): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(v)

/** "Evolução — últimos 6 meses": Dias, Passeios e Receita por mês. */
@Composable
internal fun BlocoEvolucao(evolucao: List<EvolucaoMes>) {
    val corDias = CoresExtras.Aviso
    val corPasseios = CoresExtras.Sucesso
    val corReceita = MaterialTheme.colorScheme.primary
    val corEixo = MaterialTheme.colorScheme.onSurfaceVariant
    val corGrade = MaterialTheme.colorScheme.outlineVariant
    val medidor = rememberTextMeasurer()
    // Toque num mês mostra os valores dele (o tooltip do web).
    var selecionado by remember(evolucao) { mutableStateOf<Int?>(null) }

    CartaoGrafico(titulo = "Evolução — últimos 6 meses", comIcone = true) {
        if (evolucao.isEmpty()) {
            Text(
                "Sem dados no período.",
                style = MaterialTheme.typography.bodySmall,
                color = corEixo,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            return@CartaoGrafico
        }

        val series = listOf(
            Triple("Dias", corDias) { m: EvolucaoMes -> m.diasTrabalhados.toDouble() },
            Triple("Passeios", corPasseios) { m: EvolucaoMes -> m.passeios.toDouble() },
            Triple("Receita", corReceita) { m: EvolucaoMes -> m.receita },
        )
        val maior = evolucao.maxOf { m -> series.maxOf { it.third(m) } }
        val teto = tetoDoEixo(maior)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(evolucao) {
                    detectTapGestures { toque ->
                        val esquerda = 44.dp.toPx()
                        val largura = size.width - esquerda - 8.dp.toPx()
                        if (evolucao.size == 1) {
                            selecionado = 0
                        } else {
                            val passo = largura / (evolucao.size - 1)
                            selecionado = ((toque.x - esquerda) / passo)
                                .let { kotlin.math.round(it).toInt() }
                                .coerceIn(0, evolucao.lastIndex)
                        }
                    }
                },
        ) {
            val esquerda = 44.dp.toPx()
            val direita = 8.dp.toPx()
            val topo = 8.dp.toPx()
            val base = 22.dp.toPx()
            val largura = size.width - esquerda - direita
            val altura = size.height - topo - base
            val estiloEixo = TextStyle(fontSize = 11.sp, color = corEixo)

            fun xDe(i: Int) =
                if (evolucao.size == 1) esquerda + largura / 2 else esquerda + largura * i / (evolucao.size - 1)
            fun yDe(v: Double) = topo + altura * (1 - (v / teto)).toFloat()

            // Eixo Y: 5 marcas (0, 25%, 50%, 75%, 100% do teto).
            for (k in 0..4) {
                val valor = teto * k / 4
                val y = yDe(valor)
                drawLine(corGrade, Offset(esquerda, y), Offset(esquerda - 4.dp.toPx(), y), strokeWidth = 1.dp.toPx())
                val rotulo = medidor.measure(formatarInteiro(valor), estiloEixo)
                drawText(rotulo, topLeft = Offset(esquerda - 6.dp.toPx() - rotulo.size.width, y - rotulo.size.height / 2))
            }
            drawLine(corGrade, Offset(esquerda, topo), Offset(esquerda, topo + altura), strokeWidth = 1.dp.toPx())
            drawLine(corGrade, Offset(esquerda, topo + altura), Offset(esquerda + largura, topo + altura), strokeWidth = 1.dp.toPx())

            // Eixo X: o mês.
            evolucao.forEachIndexed { i, m ->
                val rotulo = medidor.measure(m.mes.orEmpty(), estiloEixo)
                drawText(rotulo, topLeft = Offset(xDe(i) - rotulo.size.width / 2, topo + altura + 4.dp.toPx()))
            }

            selecionado?.let { i ->
                drawLine(corGrade, Offset(xDe(i), topo), Offset(xDe(i), topo + altura), strokeWidth = 1.dp.toPx())
            }

            // Linhas suaves (o "monotone" do recharts), com bolinha em cada mês.
            series.forEach { (_, cor, valorDe) ->
                val pontos = evolucao.mapIndexed { i, m -> Offset(xDe(i), yDe(valorDe(m))) }
                if (pontos.size > 1) {
                    val caminho = Path().apply {
                        moveTo(pontos[0].x, pontos[0].y)
                        for (i in 1 until pontos.size) {
                            val a = pontos[i - 1]
                            val b = pontos[i]
                            val meio = (b.x - a.x) / 2
                            cubicTo(a.x + meio, a.y, b.x - meio, b.y, b.x, b.y)
                        }
                    }
                    drawPath(caminho, cor, style = Stroke(width = 2.dp.toPx()))
                }
                pontos.forEach { p ->
                    drawCircle(Color.White, radius = 3.5.dp.toPx(), center = p)
                    drawCircle(cor, radius = 3.5.dp.toPx(), center = p, style = Stroke(width = 1.5.dp.toPx()))
                }
            }
        }

        // Legenda, centralizada como no web.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            series.forEachIndexed { i, (nome, cor, _) ->
                if (i > 0) Spacer(Modifier.width(12.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
                Spacer(Modifier.width(4.dp))
                Text(nome, fontSize = 12.sp, color = cor)
            }
        }

        // Valores do mês tocado (o tooltip do web).
        selecionado?.let { i ->
            val m = evolucao[i]
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, corGrade, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(m.mes.orEmpty(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Dias: ${m.diasTrabalhados}", fontSize = 12.sp, color = corDias)
                Text("Passeios: ${m.passeios}", fontSize = 12.sp, color = corPasseios)
                Text("Receita: ${formatarInteiro(m.receita)}", fontSize = 12.sp, color = corReceita)
            }
        }
    }
}

/** "Tipos de transporte": um quadrinho por tipo + a rosca. */
@Composable
internal fun BlocoTransportes(transportes: List<TransporteRanking>) {
    // Mesma ordem de cores do TRANSPORT_COLORS do web.
    val cores = listOf(
        MaterialTheme.colorScheme.primary,
        CoresExtras.Sucesso,
        CoresExtras.Aviso,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CartaoGrafico(titulo = "Tipos de transporte") {
        if (transportes.isEmpty()) {
            Text(
                "Sem dados de transporte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            )
            return@CartaoGrafico
        }

        // Grade de 2 colunas, como o grid-cols-2 do web.
        transportes.chunked(2).forEach { par ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                par.forEach { t ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            t.nome.orEmpty(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${t.quantidade}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "${formatarInteiro(t.percentual)}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        // A rosca (innerRadius 40 / outerRadius 70 no web).
        val total = transportes.sumOf { it.quantidade }.coerceAtLeast(1)
        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val espessura = size.minDimension * (30f / 140f)
                val raio = size.minDimension / 2 - espessura / 2
                val canto = Offset(center.x - raio, center.y - raio)
                var inicio = 0f
                transportes.forEachIndexed { i, t ->
                    val varredura = 360f * t.quantidade / total
                    // Anti-horário a partir das 3h, como o recharts (Pie startAngle 0).
                    drawArc(
                        color = cores[i % cores.size],
                        startAngle = -(inicio + varredura),
                        sweepAngle = varredura,
                        useCenter = false,
                        topLeft = canto,
                        size = Size(raio * 2, raio * 2),
                        style = Stroke(width = espessura),
                    )
                    // Fresta branca entre as fatias, como o recharts.
                    if (transportes.size > 1) {
                        drawArc(
                            color = Color.White,
                            startAngle = -inicio - 0.6f,
                            sweepAngle = 1.2f,
                            useCenter = false,
                            topLeft = canto,
                            size = Size(raio * 2, raio * 2),
                            style = Stroke(width = espessura + 1f),
                        )
                    }
                    inicio += varredura
                }
            }
        }
    }
}

/** "Ranking de passeios": posição, nome, quantidade e a barra relativa ao 1º. */
@Composable
internal fun BlocoRankingPasseios(ranking: List<PasseioRanking>) {
    CartaoGrafico(titulo = "Ranking de passeios") {
        if (ranking.isEmpty()) {
            Text(
                "Nenhum passeio registrado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            )
            return@CartaoGrafico
        }
        val maximo = max(ranking.first().qty, 1)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ranking.forEachIndexed { i, p ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${i + 1}.",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(p.nome.orEmpty(), fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text("${p.qty}", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    val fracao = (p.qty.toFloat() / maximo).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fracao)
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}
