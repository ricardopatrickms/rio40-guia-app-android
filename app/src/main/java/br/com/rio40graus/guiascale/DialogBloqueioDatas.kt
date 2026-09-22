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
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.rio40graus.guiascale.rede.BloqueioGuia
import br.com.rio40graus.guiascale.rede.DiasSemanaGuia
import br.com.rio40graus.guiascale.ui.tema.FormaBotao
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Espelho do `BlockDatesDialog.tsx`: bloquear / desbloquear várias datas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogBloqueioDatas(
    aberto: Boolean,
    aoFechar: () -> Unit,
    aoCarregarBloqueios: (from: String?, aoTerminar: (List<BloqueioGuia>?, String?) -> Unit) -> Unit,
    aoCarregarDiasSemana: (aoTerminar: (DiasSemanaGuia?, String?) -> Unit) -> Unit,
    aoCarregarDiasOcupados: (from: String?, aoTerminar: (List<String>?, String?) -> Unit) -> Unit,
    aoSalvar: (datas: List<String>, motivo: String?, aoTerminar: (String?) -> Unit) -> Unit,
    aoRemover: (datas: List<String>, aoTerminar: (String?) -> Unit) -> Unit,
) {
    if (!aberto) return

    val fmtApi = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val fmtExibir = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val fmtMes = remember {
        SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
    }

    var mesVisivel by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }
    var selecionadas by remember { mutableStateOf<Set<String>>(emptySet()) }
    var motivo by remember { mutableStateOf("") }
    var bloqueios by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ocupados by remember { mutableStateOf<Set<String>>(emptySet()) }
    var escala by remember { mutableStateOf<DiasSemanaGuia?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val hojeIso = remember {
        fmtApi.format(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time)
    }

    LaunchedEffect(aberto) {
        if (!aberto) return@LaunchedEffect
        carregando = true
        erro = null
        selecionadas = emptySet()
        motivo = ""
        aviso = null
        var pendentes = 3
        fun pronto() {
            pendentes--
            if (pendentes <= 0) carregando = false
        }
        aoCarregarBloqueios(hojeIso) { lista, falha ->
            if (falha != null) erro = falha
            bloqueios = lista.orEmpty().map { it.date.take(10) }.toSet()
            pronto()
        }
        aoCarregarDiasSemana { dados, falha ->
            if (falha != null) erro = falha
            escala = dados
            pronto()
        }
        aoCarregarDiasOcupados(hojeIso) { lista, falha ->
            if (falha != null) erro = falha
            ocupados = lista.orEmpty().map { it.take(10) }.toSet()
            pronto()
        }
    }

    fun fechar() {
        selecionadas = emptySet()
        motivo = ""
        aviso = null
        aoFechar()
    }

    fun diaDesabilitado(iso: String, cal: Calendar): Boolean {
        if (iso < hojeIso) return true
        val restrito = escala?.restrito == true
        if (restrito) {
            val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=dom … 7=sáb
            val js = if (dow == Calendar.SUNDAY) 0 else dow - 1
            if (escala?.dias?.contains(js) != true) return true
        }
        return false
    }

    fun toggleDia(iso: String, cal: Calendar) {
        if (diaDesabilitado(iso, cal)) return
        if (ocupados.contains(iso)) {
            aviso = "Você tem reserva em ${fmtExibir.format(cal.time)}. Para bloquear este dia, fale com o escritório."
            return
        }
        aviso = null
        selecionadas = if (selecionadas.contains(iso)) {
            selecionadas - iso
        } else {
            selecionadas + iso
        }
    }

    fun aplicar(bloquear: Boolean) {
        if (selecionadas.isEmpty()) {
            aviso = "Selecione ao menos uma data"
            return
        }
        salvando = true
        erro = null
        val datas = selecionadas.sorted()
        val aoTerminar: (String?) -> Unit = { falha ->
            salvando = false
            if (falha != null) {
                erro = falha
            } else {
                fechar()
            }
        }
        if (bloquear) {
            aoSalvar(datas, motivo.trim().ifBlank { null }, aoTerminar)
        } else {
            aoRemover(datas, aoTerminar)
        }
    }

    val nomesDiasEscala = remember(escala) {
        val nomes = listOf("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado")
        escala?.dias.orEmpty().sorted().mapNotNull { nomes.getOrNull(it) }.joinToString(", ")
    }

    Dialog(
        onDismissRequest = { if (!salvando) fechar() },
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
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.bloqueio_titulo),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (!salvando) fechar() }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancelar))
                    }
                }

                if (carregando) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Cabeçalho do mês
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = {
                            mesVisivel = (mesVisivel.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        }) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                        }
                        Text(
                            text = fmtMes.format(mesVisivel.time)
                                .replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString()
                                },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = {
                            mesVisivel = (mesVisivel.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        }) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    }

                    // Dias da semana
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

                    CalendarioMes(
                        mes = mesVisivel,
                        selecionadas = selecionadas,
                        bloqueados = bloqueios,
                        ocupados = ocupados,
                        hojeIso = hojeIso,
                        fmtApi = fmtApi,
                        diaDesabilitado = ::diaDesabilitado,
                        aoClicar = ::toggleDia,
                    )

                    if (bloqueios.isNotEmpty()) {
                        LegendaCor(
                            cor = Color(0xFFFEE2E2),
                            borda = Color(0xFFFCA5A5),
                            texto = stringResource(R.string.bloqueio_legenda_bloqueado),
                        )
                    }
                    if (ocupados.isNotEmpty()) {
                        LegendaCor(
                            cor = Color(0xFFFEF3C7),
                            borda = Color(0xFFF59E0B),
                            texto = stringResource(R.string.bloqueio_legenda_reserva),
                        )
                    }
                    if (escala?.restrito == true && nomesDiasEscala.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.bloqueio_escala, nomesDiasEscala),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                                                .clickable {
                                                    selecionadas = selecionadas - iso
                                                }
                                                .padding(2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.bloqueio_motivo_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { if (it.length <= 500) motivo = it },
                        placeholder = { Text(stringResource(R.string.bloqueio_motivo_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = FormaBotao,
                    )
                }

                aviso?.let {
                    Text(
                        text = it,
                        color = Color(0xFFB45309),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                erro?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { if (!salvando) fechar() },
                        enabled = !salvando,
                    ) {
                        Text(stringResource(R.string.cancelar))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { aplicar(bloquear = false) },
                        enabled = !salvando && !carregando && selecionadas.isNotEmpty(),
                        shape = FormaBotao,
                    ) {
                        Text(stringResource(R.string.bloqueio_desbloquear, selecionadas.size))
                    }
                    Button(
                        onClick = { aplicar(bloquear = true) },
                        enabled = !salvando && !carregando && selecionadas.isNotEmpty(),
                        shape = FormaBotao,
                    ) {
                        if (salvando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(stringResource(R.string.bloqueio_bloquear, selecionadas.size))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendaCor(cor: Color, borda: Color, texto: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cor)
                .border(1.dp, borda, RoundedCornerShape(2.dp)),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarioMes(
    mes: Calendar,
    selecionadas: Set<String>,
    bloqueados: Set<String>,
    ocupados: Set<String>,
    hojeIso: String,
    fmtApi: SimpleDateFormat,
    diaDesabilitado: (String, Calendar) -> Boolean,
    aoClicar: (String, Calendar) -> Unit,
) {
    val celulas = remember(mes) { montarCelulasDoMes(mes) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        celulas.chunked(7).forEach { semana ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                semana.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dia != null) {
                            val iso = fmtApi.format(dia.time)
                            val desabilitado = diaDesabilitado(iso, dia)
                            val selecionado = selecionadas.contains(iso)
                            val bloqueado = bloqueados.contains(iso)
                            val ocupado = ocupados.contains(iso)
                            val ehHoje = iso == hojeIso

                            val fundo = when {
                                selecionado -> MaterialTheme.colorScheme.primary
                                ocupado -> Color(0xFFFEF3C7)
                                bloqueado -> Color(0xFFFEE2E2)
                                else -> Color.Transparent
                            }
                            val textoCor = when {
                                selecionado -> Color.White
                                desabilitado -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                ocupado -> Color(0xFFB45309)
                                bloqueado -> Color(0xFFB91C1C)
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(fundo)
                                    .then(
                                        if (ehHoje && !selecionado) {
                                            Modifier.border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clickable(enabled = !desabilitado) { aoClicar(iso, dia) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${dia.get(Calendar.DAY_OF_MONTH)}",
                                    color = textoCor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selecionado || bloqueado || ocupado) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    textDecoration = if (ocupado && !selecionado) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    },
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

/** Grade de 6×7: null = célula vazia; Calendar = dia do mês. */
private fun montarCelulasDoMes(mes: Calendar): List<Calendar?> {
    val primeiro = (mes.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    // Calendar: SUNDAY=1 … — queremos domingo na coluna 0
    val offset = primeiro.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val diasNoMes = primeiro.getActualMaximum(Calendar.DAY_OF_MONTH)
    val lista = MutableList<Calendar?>(42) { null }
    for (d in 1..diasNoMes) {
        val cal = (primeiro.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, d)
        }
        lista[offset + d - 1] = cal
    }
    return lista
}
