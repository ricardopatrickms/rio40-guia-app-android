package br.com.rio40graus.guiascale

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.ComprovanteEnviado
import br.com.rio40graus.guiascale.rede.ComprovanteFatura
import br.com.rio40graus.guiascale.rede.FaturaGuia
import br.com.rio40graus.guiascale.rede.FormaPagamentoGuia
import br.com.rio40graus.guiascale.rede.LancamentoFatura
import br.com.rio40graus.guiascale.rede.PaginaFaturas
import br.com.rio40graus.guiascale.rede.SituacaoFaturaGuia
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Financeiro do guia — espelho do `Financeiro.tsx` do web (visão de celular).
 *
 * As cores, os tamanhos e a ordem dos blocos seguem o web ponto a ponto: o tema
 * dele (index.css) e as classes do Tailwind estão traduzidos nas constantes
 * abaixo. Qualquer ajuste de layout deve ser feito lá primeiro.
 *
 * As faturas e os lançamentos vêm prontos da guias-api (a mesma conta do PDF do
 * financeiro). O guia NÃO lança pagamento real: ele envia uma SOLICITAÇÃO
 * (valor + conta + comprovante) e o financeiro aprova ou nega.
 *
 * Admin inspecionando um guia vê tudo, mas não envia (somenteLeitura).
 */
private const val FRASE_RODAPE_FATURA =
    "O pagamento é realizado com os dados de pagamento em nossos registros, caso deseje mudar avise com antecedência."

private const val LIMITE_ARQUIVO_BYTES = 5 * 1024 * 1024

/* ---- Tokens do tema do web (index.css) e do Tailwind ---- */
private val FUNDO_CARD = Color.White
private val TEXTO = Color(0xFF152826) // --foreground
private val TEXTO_SUAVE = Color(0xFF506261) // --muted-foreground
private val MUTED = Color(0xFFEFEFEB) // --muted
private val BORDA = Color(0xFFE3E3DE) // --border
private val SUCESSO = Color(0xFF22A050) // --success
private val AVISO = Color(0xFFF9B006) // --warning
private val AVISO_TEXTO = Color(0xFF332619) // --warning-foreground
private val DESTRUTIVO = Color(0xFFDF2020) // --destructive
private val PRIMARIA = Color(0xFF1C4C96) // --primary / --secondary
private val AZUL_500 = Color(0xFF3B82F6)
private val AZUL_600 = Color(0xFF2563EB)
private val ROXO_500 = Color(0xFFA855F7)
private val ROXO_600 = Color(0xFF9333EA)
private val LARANJA_500 = Color(0xFFF97316)
private val LARANJA_600 = Color(0xFFEA580C)
private val ARDOSIA_500 = Color(0xFF64748B)

/** Paleta do protótipo para a caixa de upload do comprovante. */
private val PROTO_CAIXA_BORDA = Color(0xFFBFDBFE)
private val PROTO_CAIXA_FUNDO = Color(0xFFEFF6FF)
private val PROTO_SUAVE = Color(0xFF94A3B8)
private val PROTO_VERDE = Color(0xFF059669)

private val FORMATO_MOEDA: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

private fun brl(v: Double): String = FORMATO_MOEDA.format(v)

private fun valorComSinal(v: Double): String =
    if (v == 0.0) brl(0.0) else "${if (v > 0) "+ " else "- "}${brl(abs(v))}"

private fun corDoValor(v: Double): Color = when {
    v > 0 -> SUCESSO
    v < 0 -> DESTRUTIVO
    else -> TEXTO_SUAVE
}

/** `bg-<cor>/<n>` do Tailwind: a cor com transparência sobre o branco do card. */
private fun Color.suave(alfa: Float): Color = copy(alpha = alfa).compositeOver(FUNDO_CARD)

private val MONO = FontFamily.Monospace

private data class EstiloSituacao(
    val rotulo: String,
    val borda: Color,
    /** Texto do selo. */
    val texto: Color,
    val fundoSelo: Color,
    val bordaSelo: Color,
    /** Cor do valor no cabeçalho. */
    val valor: Color,
    val sub: String,
)

private fun estiloDaSituacao(situacao: String): EstiloSituacao = when (situacao) {
    SituacaoFaturaGuia.APURACAO -> EstiloSituacao(
        "Em apuração", AVISO, AVISO_TEXTO, AVISO.suave(0.15f), AVISO.suave(0.40f), AVISO, "a apurar",
    )
    SituacaoFaturaGuia.DISPONIVEL -> EstiloSituacao(
        "Disponível para pagamento", AZUL_500, AZUL_600, AZUL_500.suave(0.10f), AZUL_500.suave(0.30f), AZUL_600, "aguardando pgto",
    )
    SituacaoFaturaGuia.CONFERIR -> EstiloSituacao(
        "Aguardando conferência", LARANJA_500, LARANJA_600, LARANJA_500.suave(0.10f), LARANJA_500.suave(0.30f), LARANJA_600, "aguardando conferência",
    )
    SituacaoFaturaGuia.PAGA -> EstiloSituacao(
        "Paga", SUCESSO, SUCESSO, SUCESSO.suave(0.10f), SUCESSO.suave(0.30f), SUCESSO, "pago",
    )
    else -> EstiloSituacao(
        "Fechada", ROXO_500, ROXO_600, ROXO_500.suave(0.10f), ROXO_500.suave(0.30f), ROXO_600, "fechada",
    )
}

private data class EstiloLancamento(val rotulo: String, val texto: Color, val fundo: Color, val borda: Color)

private fun estiloDoLancamento(tipo: String): EstiloLancamento = when (tipo) {
    "diaria" -> EstiloLancamento("Diária", SUCESSO, SUCESSO.suave(0.10f), SUCESSO.suave(0.30f))
    "reembolso" -> EstiloLancamento("Reembolso", SUCESSO, SUCESSO.suave(0.10f), SUCESSO.suave(0.30f))
    "adiantamento" -> EstiloLancamento("Adiantamento", DESTRUTIVO, DESTRUTIVO.suave(0.10f), DESTRUTIVO.suave(0.30f))
    "devolucao" -> EstiloLancamento("Devolução", DESTRUTIVO, DESTRUTIVO.suave(0.10f), DESTRUTIVO.suave(0.30f))
    "reserva" -> EstiloLancamento("Reserva", ROXO_600, ROXO_500.suave(0.10f), ROXO_500.suave(0.30f))
    "ajuste" -> EstiloLancamento("Ajuste", TEXTO_SUAVE, MUTED, BORDA)
    // Tipo novo na API não pode derrubar a tela: cai num rótulo neutro.
    else -> EstiloLancamento(tipo.ifBlank { "Lançamento" }, TEXTO_SUAVE, MUTED, BORDA)
}

/**
 * O tema do app dá ao Text uma altura de linha de 24sp e um espaçamento entre letras
 * de 0,5sp — é o que deixava o Financeiro mais solto e largo que o do web. Aqui o
 * texto herda o ritmo do Tailwind (linha ~1,4× e letras sem espaçamento).
 */
@Composable
fun TelaFinanceiro(
    somenteLeitura: Boolean,
    aoCarregar: (pagina: Int, aoTerminar: (PaginaFaturas?, String?) -> Unit) -> Unit,
    aoEnviarComprovante: (
        faturaId: String,
        valor: Double,
        formaId: Int,
        arquivo: Uri,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
    aoBaixarComprovante: (
        id: Int,
        solicitacao: Boolean,
        nome: String,
        aoTerminar: (Uri?, String?) -> Unit,
    ) -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.merge(
            TextStyle(lineHeight = 1.4.em, letterSpacing = 0.sp),
        ),
    ) {
        TelaFinanceiroConteudo(somenteLeitura, aoCarregar, aoEnviarComprovante, aoBaixarComprovante)
    }
}

@Composable
private fun TelaFinanceiroConteudo(
    somenteLeitura: Boolean,
    aoCarregar: (pagina: Int, aoTerminar: (PaginaFaturas?, String?) -> Unit) -> Unit,
    aoEnviarComprovante: (
        faturaId: String,
        valor: Double,
        formaId: Int,
        arquivo: Uri,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
    aoBaixarComprovante: (
        id: Int,
        solicitacao: Boolean,
        nome: String,
        aoTerminar: (Uri?, String?) -> Unit,
    ) -> Unit,
) {
    val contexto = LocalContext.current
    var pagina by remember { mutableIntStateOf(1) }
    var recarregar by remember { mutableIntStateOf(0) }
    var dados by remember { mutableStateOf<PaginaFaturas?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var abertas by remember { mutableStateOf<Set<String>>(emptySet()) }
    var baixando by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pagina, recarregar) {
        carregando = true
        erro = null
        aoCarregar(pagina) { resposta, falha ->
            if (resposta != null) dados = resposta
            erro = falha
            carregando = false
        }
    }

    // Abre por padrão a fatura disponível para pagamento, como no web.
    LaunchedEffect(dados) {
        if (abertas.isEmpty()) {
            dados?.faturas?.firstOrNull { it.situacao == SituacaoFaturaGuia.DISPONIVEL }
                ?.let { abertas = setOf(it.id) }
        }
    }

    val faturas = dados?.faturas.orEmpty()
    val todasAbertas = faturas.isNotEmpty() && faturas.all { it.id in abertas }
    val resumo = dados?.resumo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), // space-y-4
    ) {
        // Título + botão de atualizar
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Financeiro", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TEXTO)
                Text("Faturas & Lançamentos em tempo real", fontSize = 12.sp, color = TEXTO_SUAVE)
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, BORDA, RoundedCornerShape(6.dp))
                    .clickable(enabled = !carregando) { recarregar++ },
                contentAlignment = Alignment.Center,
            ) {
                if (carregando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TEXTO)
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar faturas", tint = TEXTO, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Cards do topo: 2 colunas, gap-3.
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                Stat(
                    "Fatura atual",
                    resumo?.atual?.let { brl(it.total) } ?: "—",
                    resumo?.atual?.let { "${it.periodo} · em apuração" } ?: "sem fatura em apuração",
                    AVISO,
                    Modifier.weight(1f),
                )
                Stat(
                    "Aguardando pgto",
                    resumo?.aguardando?.let { brl(it.total) } ?: "—",
                    resumo?.aguardando?.let { a ->
                        val venc = a.vencimento?.take(5)
                        val dias = a.faltamDias
                        when {
                            venc == null -> "nenhuma em aberto"
                            dias != null && dias >= 0 -> "Venc. $venc · $dias dias"
                            else -> "Venc. $venc · vencida"
                        }
                    } ?: "nenhuma em aberto",
                    AZUL_600,
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                Stat(
                    "Último pago",
                    resumo?.ultimaPaga?.let { brl(it.total) } ?: "—",
                    resumo?.ultimaPaga?.pagaEm ?: "nenhum pagamento",
                    SUCESSO,
                    Modifier.weight(1f),
                )
                Stat(
                    "Acertos pendentes",
                    (resumo?.acertosPendentes ?: 0).toString(),
                    "aguardando conferência",
                    ROXO_600,
                    Modifier.weight(1f),
                )
            }
        }

        // "Minhas faturas" + contador + expandir todas
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Minhas faturas", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TEXTO)
            Text(
                "${dados?.total ?: 0} faturas",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MUTED)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TEXTO_SUAVE,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                BotaoContorno(
                    texto = if (todasAbertas) "Recolher todas" else "Expandir todas",
                    habilitado = faturas.isNotEmpty(),
                    aoClicar = { abertas = if (todasAbertas) emptySet() else faturas.map { it.id }.toSet() },
                )
            }
        }

        when {
            carregando && dados == null -> Text("Carregando faturas…", fontSize = 14.sp, color = TEXTO_SUAVE)
            erro != null && dados == null -> Text(erro ?: "", fontSize = 14.sp, color = DESTRUTIVO)
            faturas.isEmpty() -> Text("Nenhuma fatura por enquanto.", fontSize = 14.sp, color = TEXTO_SUAVE)
            else -> Column(
                modifier = Modifier.alpha(if (carregando) 0.6f else 1f),
                verticalArrangement = Arrangement.spacedBy(14.dp), // mb-3.5
            ) {
                faturas.forEach { f ->
                    CartaoFatura(
                        f = f,
                        formas = dados?.formasPagamento.orEmpty(),
                        aberta = f.id in abertas,
                        aoAlternar = { abertas = if (f.id in abertas) abertas - f.id else abertas + f.id },
                        somenteLeitura = somenteLeitura,
                        baixando = baixando,
                        aoEnviarComprovante = aoEnviarComprovante,
                        aoEnviado = { recarregar++ },
                        aoBaixar = { id, solicitacao, tipo ->
                            baixando = id
                            aoBaixarComprovante(id, solicitacao, "comprovante-${f.numero}.${tipo.ifBlank { "pdf" }}") { uri, falha ->
                                baixando = null
                                if (uri != null) {
                                    abrirComprovante(contexto, uri)
                                } else {
                                    Toast.makeText(contexto, falha ?: "Não foi possível baixar o comprovante.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                    )
                }
            }
        }

        // Aviso de uma atualização que falhou, sem esconder o que já está na tela.
        if (erro != null && dados != null) {
            Text(erro ?: "", fontSize = 12.sp, color = DESTRUTIVO)
        }

        val totalPaginas = dados?.totalPaginas ?: 1
        if (totalPaginas > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BotaoContorno("Anterior", habilitado = pagina > 1 && !carregando, icone = Icons.Outlined.ChevronLeft, iconeAntes = true) { pagina-- }
                Text("Página $pagina de $totalPaginas", fontSize = 12.sp, color = TEXTO_SUAVE)
                BotaoContorno("Próxima", habilitado = pagina < totalPaginas && !carregando, icone = Icons.Outlined.ChevronRight, iconeAntes = false) { pagina++ }
            }
        }
    }
}

private fun abrirComprovante(contexto: Context, uri: Uri) {
    val tipo = contexto.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, tipo)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        contexto.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(contexto, "Nenhum aplicativo no aparelho abre este arquivo.", Toast.LENGTH_LONG).show()
    }
}

/* ------------------------------------------------------------------------ */
/* Peças pequenas                                                           */
/* ------------------------------------------------------------------------ */

/** `<Card>` do web: rounded-2xl, borda border/60, fundo card. */
private val FORMA_CARD = RoundedCornerShape(16.dp)
private val BORDA_CARD = BORDA.copy(alpha = 0.6f)

@Composable
private fun Stat(rotulo: String, valor: String, sub: String, cor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .shadow(2.dp, FORMA_CARD, ambientColor = TEXTO.copy(alpha = 0.08f), spotColor = TEXTO.copy(alpha = 0.08f))
            .clip(FORMA_CARD)
            .border(1.dp, BORDA_CARD, FORMA_CARD)
            .background(FUNDO_CARD)
            .padding(14.dp),
    ) {
        Text(
            rotulo.uppercase(Locale.getDefault()),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TEXTO_SUAVE,
        )
        Text(
            valor,
            modifier = Modifier.padding(top = 4.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = cor,
        )
        Text(sub, modifier = Modifier.padding(top = 2.dp), fontSize = 11.sp, color = TEXTO_SUAVE)
    }
}

/** `<Button variant="outline" size="sm">` do web: h-8, borda, texto xs. */
@Composable
private fun BotaoContorno(
    texto: String,
    habilitado: Boolean = true,
    icone: ImageVector? = null,
    iconeAntes: Boolean = true,
    aoClicar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, BORDA, RoundedCornerShape(6.dp))
            .background(FUNDO_CARD)
            .alpha(if (habilitado) 1f else 0.5f)
            .clickable(enabled = habilitado, onClick = aoClicar)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icone != null && iconeAntes) Icon(icone, null, tint = TEXTO, modifier = Modifier.size(16.dp))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TEXTO)
        if (icone != null && !iconeAntes) Icon(icone, null, tint = TEXTO, modifier = Modifier.size(16.dp))
    }
}

/** `<Badge variant="outline">`: pílula com borda, 10.5px, caixa alta. */
@Composable
private fun Selo(texto: String, cor: Color, fundo: Color, borda: Color, tamanho: Float = 10.5f) {
    // whitespace-nowrap do web: o selo nunca corta nem quebra o texto. Se a coluna
    // for mais estreita que ele (o valor da fatura ocupa o lado direito), ele passa
    // por baixo da coluna do valor — como no web, onde ele também transborda.
    Box(modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
        Text(
            texto.uppercase(Locale.getDefault()),
            modifier = Modifier
                .clip(CircleShape)
                .background(fundo)
                .border(1.dp, borda, CircleShape)
                .padding(horizontal = 10.dp, vertical = 2.dp),
            color = cor,
            fontSize = tamanho.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun Divisor(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(BORDA))
}

@Composable
private fun Dado(rotulo: String, valor: String, negrito: Boolean = false) {
    // Um texto só (rótulo + valor correndo na mesma linha), como o <span> do web:
    // quando não cabe, quebra nos espaços. Com dois Text lado a lado, o valor era
    // espremido e "movimentações" partia no meio da palavra.
    Text(
        androidx.compose.ui.text.buildAnnotatedString {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = TEXTO_SUAVE))
            append("$rotulo: ")
            pop()
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    fontWeight = if (negrito) FontWeight.Bold else FontWeight.Normal,
                    color = TEXTO.copy(alpha = if (negrito) 1f else 0.8f),
                ),
            )
            append(valor)
            pop()
        },
        fontSize = 12.sp,
    )
}

/* ------------------------------------------------------------------------ */
/* Cartão da fatura                                                         */
/* ------------------------------------------------------------------------ */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoFatura(
    f: FaturaGuia,
    formas: List<FormaPagamentoGuia>,
    aberta: Boolean,
    aoAlternar: () -> Unit,
    somenteLeitura: Boolean,
    baixando: Int?,
    aoEnviarComprovante: (String, Double, Int, Uri, (String?) -> Unit) -> Unit,
    aoEnviado: () -> Unit,
    aoBaixar: (id: Int, solicitacao: Boolean, tipo: String) -> Unit,
) {
    val s = estiloDaSituacao(f.situacao)
    var caixaAberta by remember(f.id) { mutableStateOf(false) }

    // Box (e não Row com altura intrínseca): a medida intrínseca subestimava o texto
    // que quebra em várias linhas e o card cortava o fim do conteúdo.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, FORMA_CARD, ambientColor = TEXTO.copy(alpha = 0.08f), spotColor = TEXTO.copy(alpha = 0.08f))
            .clip(FORMA_CARD)
            .border(1.dp, BORDA_CARD, FORMA_CARD)
            .background(FUNDO_CARD),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
            // Cabeçalho (botão que abre/fecha): px-4 py-4, gap-3.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = aoAlternar)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                // items-start: o valor fica no TOPO, alinhado ao número da fatura; só a
                // seta é centralizada na vertical (self-center).
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        modifier = Modifier.padding(bottom = 6.dp), // mb-1.5
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(f.numero, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TEXTO)
                        Text(f.periodo, fontSize = 12.sp, color = TEXTO_SUAVE)
                        Selo(s.rotulo, s.texto, s.fundoSelo, s.bordaSelo)
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp), // gap-x-5
                        verticalArrangement = Arrangement.spacedBy(4.dp), // gap-y-1
                    ) {
                        f.vencimentoPrevisto?.let { Dado("Venc. previsto", it) }
                        f.vencimento?.let { Dado("Vencimento", it, negrito = true) }
                        if (f.situacao == SituacaoFaturaGuia.DISPONIVEL) {
                            f.disponibilizadaEm?.let { Dado("Disponibilizada", it) }
                        }
                        f.faltamDias?.takeIf { it >= 0 }?.let { Dado("Faltam", "$it dias") }
                        f.pagaEm?.let { Dado("Pago em", it) }
                        if (f.situacao == SituacaoFaturaGuia.PAGA) {
                            f.pagamentos.firstOrNull()?.forma?.takeIf { it.isNotBlank() }?.let { forma ->
                                Dado("Forma", listOfNotNull(forma, f.pagamentos.first().conta).filter { it.isNotBlank() }.joinToString(" · "))
                            }
                        }
                        if (f.situacao == SituacaoFaturaGuia.APURACAO) {
                            val n = f.lancamentos.count { it.tipo == "diaria" }
                            Dado("Passeios", "$n ${if (n == 1) "lançamento" else "lançamentos"}")
                        } else {
                            val n = f.lancamentos.size
                            Dado("Lançamentos", "$n ${if (n == 1) "movimentação" else "movimentações"}")
                        }
                        f.acertoPendente?.let { Dado("Acerto", "$it — Pendente TitanX") }
                    }
                }
                Column(modifier = Modifier.widthIn(min = 96.dp), horizontalAlignment = Alignment.End) {
                    Text(
                        brl(f.total),
                        fontSize = 19.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MONO,
                        color = if (f.total < 0) DESTRUTIVO else s.valor,
                    )
                    Text(
                        if (f.situacao == SituacaoFaturaGuia.PAGA && f.pagamentos.any { it.comprovantes.isNotEmpty() })
                            "pago · comprovante ok" else s.sub,
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 11.sp,
                        color = TEXTO_SUAVE,
                        textAlign = TextAlign.End,
                    )
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TEXTO_SUAVE,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(16.dp)
                        .rotate(if (aberta) 180f else 0f),
                )
            }

            if (aberta) {
                Divisor() // border-t
                LinhaDoTempo(f)

                if (f.situacao == SituacaoFaturaGuia.APURACAO) {
                    Faixa(
                        icone = Icons.Outlined.Info,
                        destaque = "Fatura em apuração.",
                        texto = " Os lançamentos abaixo aguardam conferência pelo setor financeiro. Os valores são calculados automaticamente pela tabela de diárias vigente.",
                        cor = AZUL_600,
                        fundo = AZUL_500.suave(0.10f),
                    )
                }
                if (f.situacao == SituacaoFaturaGuia.DISPONIVEL && f.vencimento != null) {
                    val dias = f.faltamDias ?: 0
                    Faixa(
                        icone = Icons.Outlined.Warning,
                        destaque = if (dias < 0) "Venceu em ${f.vencimento}." else "Vence em $dias dias (${f.vencimento}).",
                        texto = " Realize o pagamento e registre o comprovante para evitar atraso.",
                        cor = AVISO_TEXTO,
                        fundo = AVISO.suave(0.15f),
                    )
                }

                // Pagamentos reais, já lançados pelo financeiro.
                f.pagamentos.forEach { p ->
                    CaixaComprovante(
                        titulo = "Comprovante de Pagamento",
                        corTitulo = TEXTO,
                        corIcone = SUCESSO,
                        fundo = MUTED.copy(alpha = 0.4f).compositeOver(FUNDO_CARD),
                        borda = BORDA,
                        linhas = listOfNotNull(
                            listOfNotNull(
                                p.forma,
                                p.data?.let { d -> "$d${p.hora?.let { h -> " às $h" } ?: ""}" },
                                brl(p.valor),
                            ).filter { it.isNotBlank() }.joinToString(" · "),
                            p.conta,
                            p.comprovantes.firstOrNull { it.conferido }?.let { c ->
                                c.conferidoPor?.takeIf { it.isNotBlank() }
                                    ?.let { "Conferido por: $it (TitanX)" } ?: "Conferido (TitanX)"
                            },
                        ),
                        externo = 16.dp,
                    ) {
                        p.comprovantes.forEachIndexed { n, c: ComprovanteFatura ->
                            BotaoBaixar(
                                rotulo = if (baixando == c.id) "Baixando…" else if (p.comprovantes.size > 1) "Baixar ${n + 1}" else "Baixar",
                                verde = true,
                                habilitado = baixando != c.id,
                                aoClicar = { aoBaixar(c.id, false, c.tipo.orEmpty()) },
                            )
                        }
                    }
                }

                // Solicitações do guia: azul = aguardando, verde = aprovado, vermelho = negado.
                if (f.comprovantesEnviados.isNotEmpty()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        f.comprovantesEnviados.forEach { c: ComprovanteEnviado ->
                            val cor = when (c.status) {
                                "aprovada" -> Triple(Color(0xFF16A34A), Color(0xFFF0FDF4), Color(0xFF86EFAC))
                                "negada" -> Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), Color(0xFFFCA5A5))
                                else -> Triple(AZUL_600, Color(0xFFEFF6FF), Color(0xFF93C5FD))
                            }
                            CaixaComprovante(
                                titulo = "Pagamento solicitado",
                                corTitulo = when (c.status) {
                                    "aprovada" -> Color(0xFF15803D)
                                    "negada" -> Color(0xFFB91C1C)
                                    else -> Color(0xFF1D4ED8)
                                },
                                corIcone = cor.first,
                                fundo = cor.second,
                                borda = cor.third,
                                linhas = listOf(
                                    listOfNotNull(
                                        c.valor?.let { "Valor informado ${brl(it)}" },
                                        c.forma?.takeIf { it.isNotBlank() },
                                        c.data?.let { d -> "$d${c.hora?.let { h -> " às $h" } ?: ""}" },
                                        when (c.status) {
                                            "negada" -> "Negado${c.motivo?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""}"
                                            "aprovada" -> "Aprovado"
                                            else -> "Aguardando conferência"
                                        },
                                    ).joinToString(" · "),
                                ),
                                externo = 0.dp,
                            ) {
                                BotaoBaixar(
                                    rotulo = if (baixando == c.id) "Baixando…" else "Baixar",
                                    verde = false,
                                    habilitado = baixando != c.id,
                                    aoClicar = { aoBaixar(c.id, true, c.tipo.orEmpty()) },
                                )
                            }
                        }
                    }
                }

                // Lançamentos (celular: um cartão por lançamento).
                f.lancamentos.forEach { l ->
                    CartaoLancamento(f, l)
                    Divisor()
                }
                val rotuloTotal = when (f.situacao) {
                    SituacaoFaturaGuia.PAGA -> "Total pago nesta fatura"
                    SituacaoFaturaGuia.APURACAO -> "Saldo apurado da fatura"
                    else -> "Total a receber nesta fatura"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MUTED.copy(alpha = 0.4f).compositeOver(FUNDO_CARD))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(rotuloTotal, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TEXTO, modifier = Modifier.weight(1f))
                    Text(brl(f.total), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = MONO, color = corDoValor(f.total))
                }

                if (f.situacao == SituacaoFaturaGuia.APURACAO) {
                    f.tabelaDiarias?.let { t -> TabelaDeDiarias(t.titulo, t.faixas) }
                }

                // Barra de totais (border-t, bg-muted/40, px-4 py-3).
                Divisor()
                Totais(f, s, somenteLeitura, caixaAberta) { caixaAberta = !caixaAberta }

                if (f.podeAnexarComprovante && caixaAberta) {
                    CaixaUpload(
                        fatura = f,
                        formas = formas,
                        somenteLeitura = somenteLeitura,
                        aoEnviar = aoEnviarComprovante,
                        aoConcluir = {
                            caixaAberta = false
                            aoEnviado()
                        },
                    )
                }

                Divisor()
                Text(
                    FRASE_RODAPE_FATURA,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontSize = 11.5.sp,
                    fontStyle = FontStyle.Italic,
                    color = TEXTO_SUAVE,
                )
            }
        }

        // border-l-4 na cor da situação, na altura real do card.
        // (matchParentSize dá a altura do card; o Box de dentro limita a 4dp de largura.)
        Box(modifier = Modifier.matchParentSize()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(s.borda))
        }
    }
}

/** Aviso colorido logo abaixo da linha do tempo: ícone + frase. */
@Composable
private fun Faixa(icone: ImageVector, destaque: String, texto: String, cor: Color, fundo: Color) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(fundo)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icone, null, tint = cor, modifier = Modifier.size(16.dp))
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                    append(destaque)
                    pop()
                    append(texto)
                },
                fontSize = 12.5.sp,
                color = cor,
            )
        }
        Divisor()
    }
}

/** Quatro passos com a linha ligando os círculos, como o do web. */
@Composable
private fun LinhaDoTempo(f: FaturaGuia) {
    val nomes = listOf("Em apuração", "Fatura fechada", "Disponível pgto", "Pago")
    val icones = listOf(Icons.Outlined.BarChart, Icons.Outlined.Lock, Icons.Outlined.Email, Icons.Outlined.CheckCircle)
    val atual = when (f.situacao) {
        SituacaoFaturaGuia.APURACAO -> 0
        SituacaoFaturaGuia.FECHADA -> 1
        SituacaoFaturaGuia.DISPONIVEL -> 2
        else -> 3
    }
    val e = f.etapas

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MUTED.copy(alpha = 0.4f).compositeOver(FUNDO_CARD))
                .padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            nomes.forEachIndexed { i, padrao ->
                // O guia pediu a conferência: o último passo vira "Aguardando conferência".
                val nome = if (i == 3 && f.situacao == SituacaoFaturaGuia.CONFERIR) "Aguardando conferência" else padrao
                val feita = i < atual || (f.situacao == SituacaoFaturaGuia.PAGA && i == 3)
                val corrente = i == atual && !feita
                val cor = when {
                    feita -> SUCESSO
                    corrente -> AZUL_500
                    else -> AVISO
                }
                val corIcone = when {
                    feita -> SUCESSO
                    corrente -> AZUL_600
                    else -> AVISO
                }
                val data = when (i) {
                    0 -> (e?.apuracao ?: f.adicionadaEm).let {
                        if (f.situacao == SituacaoFaturaGuia.APURACAO) "desde ${it.take(5)}" else it
                    }
                    1 -> e?.fechada.orEmpty()
                    2 -> e?.disponivel.orEmpty()
                    else ->
                        if (f.situacao == SituacaoFaturaGuia.CONFERIR) e?.conferir.orEmpty()
                        else e?.paga ?: f.pagaEm ?: f.vencimento?.let { "até $it" } ?: "aguardando"
                }
                val ultimo = i == nomes.lastIndex
                val corLinha = if (i < atual) SUCESSO else BORDA

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .drawBehind {
                            // Linha até o próximo círculo: top 13px, a 15px do centro de cada um.
                            if (!ultimo) {
                                val y = 13.dp.toPx()
                                drawLine(
                                    color = corLinha,
                                    start = Offset(size.width / 2 + 15.dp.toPx(), y),
                                    end = Offset(size.width * 1.5f - 15.dp.toPx(), y),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Butt,
                                )
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(FUNDO_CARD)
                            .background(cor.copy(alpha = 0.10f))
                            .border(2.dp, cor, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (feita) Icons.Filled.Check else icones[i],
                            contentDescription = null,
                            tint = corIcone,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        nome,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 10.5.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        color = TEXTO.copy(alpha = 0.8f),
                    )
                    Text(data, fontSize = 10.sp, color = TEXTO_SUAVE, textAlign = TextAlign.Center)
                }
            }
        }
        Divisor() // border-b
    }
}

/**
 * Caixa de comprovante do web: `flex flex-wrap items-center gap-3.5 rounded-lg
 * border p-3.5`. Ícone + texto (mín. 160px) e, à direita, os botões — que descem
 * para a linha de baixo quando não cabem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaixaComprovante(
    titulo: String,
    corTitulo: Color,
    corIcone: Color,
    fundo: Color,
    borda: Color,
    linhas: List<String>,
    /** `m-4` dos pagamentos reais; as solicitações já vêm dentro de um `p-4`. */
    externo: Dp,
    acoes: @Composable () -> Unit,
) {
    // Ícone + texto em cima; os botões TODOS juntos logo abaixo, sem um deles
    // sobrando ao lado do texto.
    Column(
        modifier = Modifier
            .padding(externo)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(fundo)
            .border(1.dp, borda, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // items-center: o ícone fica no meio da altura do texto, como no web.
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_lucide_receipt), null, tint = corIcone, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = corTitulo)
                linhas.filter { it.isNotBlank() }.forEach {
                    Text(it, fontSize = 11.5.sp, color = TEXTO_SUAVE)
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            acoes()
        }
    }
}

/** Botão pequeno (h-8, xs, bold). `verde`: o tom do comprovante pago; senão, o contorno comum. */
@Composable
private fun BotaoBaixar(rotulo: String, verde: Boolean, habilitado: Boolean, aoClicar: () -> Unit) {
    val cor = if (verde) SUCESSO else TEXTO
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (verde) SUCESSO.suave(0.10f) else FUNDO_CARD)
            .border(1.dp, if (verde) SUCESSO.suave(0.30f) else BORDA, RoundedCornerShape(6.dp))
            .alpha(if (habilitado) 1f else 0.5f)
            .clickable(enabled = habilitado, onClick = aoClicar)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.Download, null, tint = cor, modifier = Modifier.size(14.dp))
        Text(rotulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cor)
    }
}

@Composable
private fun CartaoLancamento(f: FaturaGuia, l: LancamentoFatura) {
    val ehDiaria = l.tipo == "diaria"
    val partes = l.passeio.orEmpty().split(" - ")
    val tour = partes.firstOrNull().orEmpty()
    val produto = partes.drop(1).joinToString(" - ")
    val principal = if (ehDiaria) tour.ifBlank { l.descricao ?: l.operacao.orEmpty() } else l.descricao ?: l.operacao.orEmpty()
    val sub = (if (ehDiaria) listOf(produto.ifBlank { null }, l.paxDetalhe) else listOf(l.passeio))
        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
    val pax = if (f.situacao == SituacaoFaturaGuia.APURACAO) l.paxDetalhe ?: l.pax else l.pax
    val ref = l.refDiaria?.let {
        if (f.situacao == SituacaoFaturaGuia.APURACAO) it.replace(Regex(" · (.+)$"), " · faixa $1 pax") else it
    }
    val estilo = estiloDoLancamento(l.tipo)

    // space-y-1.5 border-b px-4 py-3
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(l.ref.orEmpty(), fontFamily = MONO, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TEXTO)
            Text(l.data ?: "—", fontSize = 12.sp, color = TEXTO_SUAVE)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Selo(estilo.rotulo, estilo.texto, estilo.fundo, estilo.borda, tamanho = 10f)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(principal, fontSize = 13.sp, color = TEXTO)
                if (sub.isNotBlank()) Text(sub, fontSize = 11.sp, color = TEXTO_SUAVE)
                val extra = listOfNotNull(pax, ref).filter { it.isNotBlank() }.joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, fontSize = 11.sp, color = TEXTO_SUAVE)
            }
            Text(
                valorComSinal(l.valor),
                fontFamily = MONO,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = corDoValor(l.valor),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TabelaDeDiarias(titulo: String, faixas: List<br.com.rio40graus.guiascale.rede.FaixaDiaria>) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BORDA, RoundedCornerShape(8.dp)),
    ) {
        Text(
            "Tabela de Diárias — $titulo",
            modifier = Modifier
                .fillMaxWidth()
                .background(MUTED.copy(alpha = 0.4f).compositeOver(FUNDO_CARD))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TEXTO,
        )
        Divisor()
        // grid-cols-2
        faixas.chunked(2).forEach { par ->
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                par.forEachIndexed { i, x ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            (x.faixa + if (x.aplicada) " ✓" else "").uppercase(Locale.getDefault()),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = TEXTO_SUAVE,
                        )
                        Text(
                            brl(x.valor),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (x.aplicada) SUCESSO else TEXTO,
                        )
                        if (x.aplicada) Text("aplicado neste acerto", fontSize = 11.sp, color = TEXTO_SUAVE)
                    }
                    if (i == 0 && par.size > 1) Box(Modifier.width(1.dp).fillMaxSize().background(BORDA))
                }
                if (par.size == 1) Box(Modifier.weight(1f))
            }
            Divisor()
        }
    }
}

/** Barra de totais: os mesmos Crédito, Débito e Valor total do PDF do financeiro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Totais(
    f: FaturaGuia,
    s: EstiloSituacao,
    somenteLeitura: Boolean,
    caixaAberta: Boolean,
    aoAlternarCaixa: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MUTED.copy(alpha = 0.4f).compositeOver(FUNDO_CARD))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp), // gap-x-6
        verticalArrangement = Arrangement.spacedBy(8.dp), // gap-y-2
    ) {
        Total("Créditos (diárias)", valorComSinal(f.credito), corDoValor(f.credito), 14.sp)
        Total("Débitos", valorComSinal(-f.debito), corDoValor(-f.debito), 14.sp)
        Total(
            when (f.situacao) {
                SituacaoFaturaGuia.PAGA -> "Total pago"
                SituacaoFaturaGuia.APURACAO -> "Saldo parcial"
                else -> "Saldo a receber"
            },
            brl(f.total),
            if (f.total < 0) DESTRUTIVO else s.valor,
            16.sp,
        )
        f.acertoPendente?.let {
            Column {
                Text("STATUS TITANX", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = TEXTO_SUAVE)
                Text("Acerto $it pendente", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AVISO)
            }
        }
        if (f.podeAnexarComprovante) {
            // ml-auto: o botão vai para a ponta direita da linha em que couber.
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PROTO_VERDE)
                    .alpha(if (somenteLeitura) 0.5f else 1f)
                    .clickable(enabled = !somenteLeitura, onClick = aoAlternarCaixa)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.AttachFile, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text("Registrar Comprovante", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun Total(rotulo: String, valor: String, cor: Color, tamanho: androidx.compose.ui.unit.TextUnit) {
    Column {
        Text(
            rotulo.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TEXTO_SUAVE,
        )
        Text(valor, fontFamily = MONO, fontWeight = FontWeight.ExtraBold, fontSize = tamanho, color = cor)
    }
}

/* ------------------------------------------------------------------------ */
/* Caixa de upload (inline, abaixo da barra de totais)                      */
/* ------------------------------------------------------------------------ */

/** Uma linha do pagamento: a fatura pode ser paga em vários, como no Titan. */
private class LinhaPagamento(valorInicial: String = "") {
    var valor by mutableStateOf(valorInicial)
    var tipo by mutableStateOf("")
    var formaId by mutableStateOf<Int?>(null)
    var arquivo by mutableStateOf<Uri?>(null)
    var nomeArquivo by mutableStateOf("")
    var tamanho by mutableStateOf(0L)
}

private fun paraNumero(texto: String): Double {
    val t = texto.trim()
    return (if (t.contains(",")) t.replace(".", "").replace(",", ".") else t).toDoubleOrNull() ?: Double.NaN
}

private fun nomeETamanho(contexto: Context, uri: Uri): Pair<String, Long> {
    contexto.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val iNome = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val iTam = c.getColumnIndex(OpenableColumns.SIZE)
            return (if (iNome >= 0) c.getString(iNome) else null).orEmpty() to
                (if (iTam >= 0 && !c.isNull(iTam)) c.getLong(iTam) else 0L)
        }
    }
    return "" to 0L
}

@Composable
private fun CaixaUpload(
    fatura: FaturaGuia,
    formas: List<FormaPagamentoGuia>,
    somenteLeitura: Boolean,
    aoEnviar: (String, Double, Int, Uri, (String?) -> Unit) -> Unit,
    aoConcluir: () -> Unit,
) {
    val contexto = LocalContext.current
    // A 1ª linha já vem com o valor da fatura.
    val linhas = remember {
        mutableStateListOf(
            LinhaPagamento(String.format(Locale("pt", "BR"), "%,.2f", abs(fatura.total))),
        )
    }
    var enviando by remember { mutableStateOf(false) }
    var linhaDoArquivo by remember { mutableStateOf<LinhaPagamento?>(null) }

    val seletor = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val alvo = linhaDoArquivo
        if (uri != null && alvo != null) {
            val (nome, tamanho) = nomeETamanho(contexto, uri)
            alvo.arquivo = uri
            alvo.nomeArquivo = nome.ifBlank { "comprovante" }
            alvo.tamanho = tamanho
        }
    }

    // Como no Titan: 1º o tipo (CASH, PIX…), 2º a conta daquele tipo.
    val tipos = remember(formas) { formas.map { it.tipo }.filter { it.isNotBlank() }.distinct() }
    val bloqueado = enviando || somenteLeitura
    val varias = linhas.size > 1
    val completo = linhas.all { it.formaId != null && it.arquivo != null && it.valor.isNotBlank() }

    fun toast(msg: String) = Toast.makeText(contexto, msg, Toast.LENGTH_LONG).show()

    fun validar(): String? {
        linhas.forEachIndexed { i, l ->
            val n = if (varias) "Pagamento ${i + 1}: " else ""
            val numero = paraNumero(l.valor)
            if (numero.isNaN() || numero <= 0) return "${n}informe o valor pago."
            if (l.formaId == null) return "${n}escolha a forma de pagamento."
            if (l.arquivo == null) return "${n}escolha o arquivo do comprovante."
            if (l.tamanho > LIMITE_ARQUIVO_BYTES) return "${n}o arquivo deve ter no máximo 5 MB."
        }
        return null
    }

    // Cada linha vira uma solicitação, enviada em sequência: as que já foram saem
    // da tela, a que falhar fica para reenviar.
    fun enviarDaFila(total: Int) {
        val atual = linhas.firstOrNull()
        if (atual == null) {
            toast(if (total > 1) "Pagamentos enviados. O financeiro vai conferir." else "Pagamento enviado. O financeiro vai conferir.")
            enviando = false
            aoConcluir()
            return
        }
        aoEnviar(fatura.id, paraNumero(atual.valor), atual.formaId!!, atual.arquivo!!) { falha ->
            if (falha != null) {
                toast(falha)
                enviando = false
            } else {
                linhas.remove(atual)
                enviarDaFila(total)
            }
        }
    }

    // mx-4 mb-4 mt-3 space-y-2 rounded-lg border-2 border-dashed p-3
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
            .fillMaxWidth()
            .alpha(if (somenteLeitura) 0.6f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(PROTO_CAIXA_FUNDO)
            .drawBehind {
                drawRoundRect(
                    color = PROTO_CAIXA_BORDA,
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        linhas.forEachIndexed { i, l ->
            val contas = if (l.tipo.isBlank()) emptyList() else formas.filter { it.tipo == l.tipo }
            val conta = contas.firstOrNull { it.id == l.formaId }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, PROTO_CAIXA_BORDA, RoundedCornerShape(6.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (varias) "Pagamento ${i + 1}" else "Pagamento",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TEXTO,
                    )
                    if (varias) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Remover pagamento ${i + 1}",
                            tint = TEXTO_SUAVE,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(enabled = !bloqueado) { linhas.remove(l) },
                        )
                    }
                }

                CampoRotulado("Forma de pagamento") {
                    Selecao(
                        texto = l.tipo.ifBlank { "Selecione a forma" },
                        vazio = l.tipo.isBlank(),
                        opcoes = tipos,
                        habilitado = !bloqueado,
                        aoEscolher = { t ->
                            l.tipo = t
                            val doTipo = formas.filter { it.tipo == t }
                            // Uma conta só: já vem escolhida.
                            l.formaId = if (doTipo.size == 1) doTipo.first().id else null
                        },
                    )
                }
                CampoRotulado("Fornecedor") {
                    Selecao(
                        texto = conta?.conta ?: if (l.tipo.isBlank()) "Selecione a forma" else "Selecione",
                        vazio = conta == null,
                        opcoes = contas.map { it.conta },
                        habilitado = !bloqueado && l.tipo.isNotBlank() && contas.size != 1,
                        travado = contas.size == 1,
                        aoEscolher = { nome -> l.formaId = contas.firstOrNull { it.conta == nome }?.id },
                    )
                }
                CampoRotulado("Valor (R$)") {
                    CampoValor(l.valor, !bloqueado) { l.valor = it }
                }
                CampoRotulado("Comprovante") {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (l.arquivo != null) AZUL_600 else ARDOSIA_500)
                                .clickable(enabled = !bloqueado) {
                                    linhaDoArquivo = l
                                    seletor.launch(arrayOf("application/pdf", "image/jpeg", "image/png"))
                                }
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.AttachFile, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text(
                                if (l.arquivo != null) l.nomeArquivo else "Enviar comprovante",
                                modifier = Modifier.widthIn(max = 150.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            "PDF, JPG ou PNG · máx. 5 MB",
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = PROTO_SUAVE,
                        )
                    }
                }
            }
        }

        // flex-col gap-2: "Nova forma de pagamento" (secundário) e "Enviar" (verde).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PRIMARIA)
                .alpha(if (bloqueado) 0.5f else 1f)
                .clickable(enabled = !bloqueado) { linhas.add(LinhaPagamento()) },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("Nova forma de pagamento", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        val podeEnviar = !bloqueado && completo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(PROTO_VERDE)
                .alpha(if (podeEnviar) 1f else 0.5f)
                .clickable(enabled = podeEnviar) {
                    val erro = validar()
                    if (erro != null) {
                        toast(erro)
                    } else {
                        enviando = true
                        enviarDaFila(linhas.size)
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AttachFile, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(
                if (enviando) "Enviando…" else if (varias) "Enviar pagamentos" else "Enviar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        if (somenteLeitura) {
            Text(
                "Só o guia consegue anexar (você está vendo como administrador).",
                fontSize = 11.5.sp,
                color = PROTO_SUAVE,
            )
        }
    }
}

@Composable
private fun CampoRotulado(rotulo: String, conteudo: @Composable () -> Unit) {
    Column {
        Text(
            rotulo.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(bottom = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TEXTO_SUAVE,
        )
        conteudo()
    }
}

/** `<Select>` do web: h-10, borda, fundo branco, seta à direita. */
@Composable
private fun Selecao(
    texto: String,
    vazio: Boolean,
    opcoes: List<String>,
    habilitado: Boolean,
    travado: Boolean = false,
    aoEscolher: (String) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (travado) MUTED else Color.White)
                .border(1.dp, BORDA, RoundedCornerShape(6.dp))
                .alpha(if (habilitado || travado) 1f else 0.5f)
                .clickable(enabled = habilitado) { aberto = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                texto,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (vazio) TEXTO_SUAVE else TEXTO,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ExpandMore, null, tint = TEXTO_SUAVE, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = {
                        aberto = false
                        aoEscolher(o)
                    },
                )
            }
        }
    }
}

@Composable
private fun CampoValor(valor: String, habilitado: Boolean, aoMudar: (String) -> Unit) {
    BasicTextField(
        value = valor,
        onValueChange = aoMudar,
        enabled = habilitado,
        singleLine = true,
        textStyle = TextStyle(fontFamily = MONO, fontSize = 14.sp, color = TEXTO),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { campo ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, BORDA, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) { campo() }
        },
    )
}
