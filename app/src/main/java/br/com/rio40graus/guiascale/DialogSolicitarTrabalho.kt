package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.rio40graus.guiascale.rede.DiasSemanaGuia
import br.com.rio40graus.guiascale.rede.SolicitacaoTrabalho
import br.com.rio40graus.guiascale.ui.tema.FormaBotao
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Espelho do `WorkRequestDialog.tsx`: pedir dias fora da escala do guia.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogSolicitarTrabalho(
    aberto: Boolean,
    aoFechar: () -> Unit,
    aoCarregarDiasSemana: (aoTerminar: (DiasSemanaGuia?, String?) -> Unit) -> Unit,
    aoCarregarSolicitacoes: (from: String?, aoTerminar: (List<SolicitacaoTrabalho>?, String?) -> Unit) -> Unit,
    aoEnviar: (datas: List<String>, motivo: String?, aoTerminar: (String?) -> Unit) -> Unit,
) {
    if (!aberto) return

    val fmtApi = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val fmtExibir = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val fmtMes = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }

    var mesVisivel by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            zerarHora()
        })
    }
    var selecionadas by remember { mutableStateOf<Set<String>>(emptySet()) }
    var motivo by remember { mutableStateOf("") }
    var escala by remember { mutableStateOf<DiasSemanaGuia?>(null) }
    var pedidos by remember { mutableStateOf<List<SolicitacaoTrabalho>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val hojeIso = remember {
        fmtApi.format(Calendar.getInstance().apply { zerarHora() }.time)
    }

    LaunchedEffect(aberto) {
        if (!aberto) return@LaunchedEffect
        carregando = true
        erro = null
        selecionadas = emptySet()
        motivo = ""
        aviso = null
        var pendentes = 2
        fun pronto() {
            pendentes--
            if (pendentes <= 0) carregando = false
        }
        aoCarregarDiasSemana { dados, falha ->
            if (falha != null) erro = falha
            escala = dados
            pronto()
        }
        aoCarregarSolicitacoes(hojeIso) { lista, falha ->
            if (falha != null) erro = falha
            pedidos = lista.orEmpty()
            pronto()
        }
    }

    val atendeTodos = escala != null && !escala!!.restrito
    val jaPedidos = remember(pedidos) {
        pedidos.map { it.date.take(10) }.toSet()
    }
    val porSituacao = remember(pedidos) {
        pedidos.groupBy { (it.situacao ?: "").lowercase(Locale.US) }
            .mapValues { (_, v) -> v.map { it.date.take(10) }.toSet() }
    }
    val pendentesIso = porSituacao["pendente"].orEmpty()
    val aprovadasIso = porSituacao["aprovada"].orEmpty()
    val recusadasIso = porSituacao["recusada"].orEmpty()
    val recusasComMotivo = pedidos.filter {
        it.situacao.equals("recusada", ignoreCase = true) && !it.motivo_decisao.isNullOrBlank()
    }

    fun diaDaEscala(cal: Calendar): Boolean {
        if (escala?.restrito != true) return false
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val js = if (dow == Calendar.SUNDAY) 0 else dow - 1
        return escala?.dias?.contains(js) == true
    }

    fun diaDesabilitado(iso: String, cal: Calendar): Boolean {
        if (iso < hojeIso) return true
        if (atendeTodos) return true
        if (diaDaEscala(cal)) return true
        if (jaPedidos.contains(iso)) return true
        return false
    }

    fun fechar() {
        selecionadas = emptySet()
        motivo = ""
        aviso = null
        aoFechar()
    }

    fun toggle(iso: String, cal: Calendar) {
        if (diaDesabilitado(iso, cal)) return
        aviso = null
        selecionadas = if (selecionadas.contains(iso)) selecionadas - iso else selecionadas + iso
    }

    fun enviar() {
        if (selecionadas.isEmpty()) {
            aviso = "Selecione ao menos uma data"
            return
        }
        enviando = true
        erro = null
        aoEnviar(selecionadas.sorted(), motivo.trim().ifBlank { null }) { falha ->
            enviando = false
            if (falha != null) erro = falha else fechar()
        }
    }

    val nomesDiasEscala = remember(escala) {
        val nomes = listOf("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado")
        escala?.dias.orEmpty().sorted().mapNotNull { nomes.getOrNull(it) }.joinToString(", ")
    }

    Dialog(
        onDismissRequest = { if (!enviando) fechar() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = FormaCartao,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.size(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.solicitar_titulo),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.solicitar_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { if (!enviando) fechar() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancelar))
                    }
                }

                if (carregando) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = {
                            mesVisivel = (mesVisivel.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        }) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
                        Text(
                            text = fmtMes.format(mesVisivel.time).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString()
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = {
                            mesVisivel = (mesVisivel.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        }) { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    CalendarioSolicitar(
                        mes = mesVisivel,
                        selecionadas = selecionadas,
                        hojeIso = hojeIso,
                        fmtApi = fmtApi,
                        diaDesabilitado = ::diaDesabilitado,
                        diaDaEscala = ::diaDaEscala,
                        pendentes = pendentesIso,
                        aprovadas = aprovadasIso,
                        recusadas = recusadasIso,
                        aoClicar = ::toggle,
                    )

                    when {
                        atendeTodos -> Text(
                            text = stringResource(R.string.solicitar_atende_todos),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        escala?.restrito == true && nomesDiasEscala.isNotBlank() -> Text(
                            text = stringResource(R.string.solicitar_escala, nomesDiasEscala),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (pendentesIso.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.solicitar_legenda_pendente),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (aprovadasIso.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.solicitar_legenda_aprovada),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (recusadasIso.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.solicitar_legenda_recusada),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (recusasComMotivo.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                recusasComMotivo.forEach { s ->
                                    val data = runCatching { fmtApi.parse(s.date.take(10)) }.getOrNull()
                                    Text(
                                        text = "${data?.let { fmtExibir.format(it) } ?: s.date}: ${s.motivo_decisao}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB91C1C),
                                    )
                                }
                            }
                        }
                    }

                    if (selecionadas.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            selecionadas.sorted().forEach { iso ->
                                val data = runCatching { fmtApi.parse(iso) }.getOrNull()
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = data?.let { fmtExibir.format(it) } ?: iso,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { selecionadas = selecionadas - iso }
                                                .padding(2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.solicitar_motivo_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { if (it.length <= 500) motivo = it },
                        placeholder = { Text(stringResource(R.string.solicitar_motivo_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4,
                        shape = FormaBotao,
                    )
                }

                aviso?.let {
                    Text(text = it, color = Color(0xFFB45309), style = MaterialTheme.typography.bodySmall)
                }
                erro?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { if (!enviando) fechar() }, enabled = !enviando) {
                        Text(stringResource(R.string.cancelar))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = ::enviar,
                        enabled = !enviando && !carregando && selecionadas.isNotEmpty() && !atendeTodos,
                        shape = FormaBotao,
                    ) {
                        if (enviando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(stringResource(R.string.solicitar_enviar))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioSolicitar(
    mes: Calendar,
    selecionadas: Set<String>,
    hojeIso: String,
    fmtApi: SimpleDateFormat,
    diaDesabilitado: (String, Calendar) -> Boolean,
    diaDaEscala: (Calendar) -> Boolean,
    pendentes: Set<String>,
    aprovadas: Set<String>,
    recusadas: Set<String>,
    aoClicar: (String, Calendar) -> Unit,
) {
    val celulas = remember(mes) { montarCelulasMes(mes) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        celulas.chunked(7).forEach { semana ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                semana.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dia != null) {
                            val iso = fmtApi.format(dia.time)
                            val desabilitado = diaDesabilitado(iso, dia)
                            val selecionado = selecionadas.contains(iso)
                            val naEscala = diaDaEscala(dia)
                            val pendente = pendentes.contains(iso)
                            val aprovada = aprovadas.contains(iso)
                            val recusada = recusadas.contains(iso)
                            val ehHoje = iso == hojeIso

                            val fundo = when {
                                selecionado -> MaterialTheme.colorScheme.primary
                                aprovada -> Color(0xFFBBF7D0)
                                pendente -> Color(0xFFFEF3C7)
                                recusada -> Color(0xFFFEE2E2)
                                naEscala -> Color(0xFFD1FAE5)
                                else -> Color.Transparent
                            }
                            val textoCor = when {
                                selecionado -> Color.White
                                desabilitado && !naEscala && !pendente && !aprovada && !recusada ->
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                recusada -> Color(0xFFB91C1C)
                                pendente -> Color(0xFFB45309)
                                aprovada || naEscala -> Color(0xFF047857)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(fundo)
                                    .then(
                                        if (ehHoje && !selecionado) {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier,
                                    )
                                    .clickable(enabled = !desabilitado) { aoClicar(iso, dia) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${dia.get(Calendar.DAY_OF_MONTH)}",
                                    color = textoCor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textDecoration = if (recusada && !selecionado) {
                                        TextDecoration.LineThrough
                                    } else TextDecoration.None,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun Calendar.zerarHora() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun montarCelulasMes(mes: Calendar): List<Calendar?> {
    val primeiro = (mes.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val offset = primeiro.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val diasNoMes = primeiro.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lista = MutableList<Calendar?>(42) { null }
    for (d in 1..diasNoMes) {
        lista[offset + d - 1] = (primeiro.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
    }
    return lista
}
