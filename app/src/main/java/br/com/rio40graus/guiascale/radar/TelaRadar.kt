package br.com.rio40graus.guiascale.radar

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.ui.tema.CoresExtras
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Radar — espelho da tela Radar do web (scale-guide-pro/src/pages/Radar.tsx):
 * seletor geral (cidade + data) e quatro blocos que compartilham essa escolha —
 * previsão do tempo, vento e mar, e trânsito (referência OSRM + ao vivo Google).
 *
 * As fontes e a classificação são as mesmas do web — ver RadarDados.
 */

private val CIDADES_SELETOR = listOf(
    "Angra dos Reis", "Búzios", "Arraial do Cabo", "Petrópolis", "Paraty", "Rio de Janeiro",
)

private val fmtIso = SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))
private val fmtBr = SimpleDateFormat("EEE, d 'de' MMM", Locale("pt", "BR"))
private val fmtHora = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

private fun hojeIso(): String = fmtIso.format(java.util.Date())

private fun rotuloData(iso: String): String =
    runCatching { fmtBr.format(fmtIso.parse(iso)!!) }
        .getOrDefault(iso)
        .replaceFirstChar { it.uppercase() }

@Composable
fun TelaRadar(modifier: Modifier = Modifier) {
    var cidade by remember { mutableStateOf(CIDADES_SELETOR[0]) }
    var data by remember { mutableStateOf(hojeIso()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Radar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        SeletorGeral(
            cidade = cidade,
            data = data,
            aoTrocarCidade = { cidade = it },
            aoTrocarData = { data = it },
        )

        BlocoTempo(data = data)
        BlocoVento(data = data, cidadeGlobal = cidade)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text("Trânsito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        BlocoTrafego(
            cidadeGlobal = cidade,
            fonte = FonteTrafego.OSRM,
            titulo = "Trânsito e chegada (saída do Rio de Janeiro)",
        )
        BlocoTrafego(
            cidadeGlobal = cidade,
            fonte = FonteTrafego.GOOGLE,
            titulo = "Trânsito ao vivo (saída do Rio de Janeiro)",
        )

        Spacer(Modifier.height(8.dp))
    }
}

// --------------------------------------------------------------------- seletor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorGeral(
    cidade: String,
    data: String,
    aoTrocarCidade: (String) -> Unit,
    aoTrocarData: (String) -> Unit,
) {
    var menuCidade by remember { mutableStateOf(false) }
    var calendario by remember { mutableStateOf(false) }

    Cartao {
        Text(
            "SELETOR GERAL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { menuCidade = true },
                    shape = FormaBotaoPequeno,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(cidade, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuCidade, onDismissRequest = { menuCidade = false }) {
                    CIDADES_SELETOR.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = { aoTrocarCidade(c); menuCidade = false },
                        )
                    }
                }
            }
            OutlinedButton(onClick = { calendario = true }, shape = FormaBotaoPequeno) {
                Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(rotuloData(data))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "aplica cidade e data em todos os cards abaixo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (calendario) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = runCatching { fmtIso.parse(data)!!.time }.getOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { aoTrocarData(fmtIso.format(java.util.Date(it))) }
                    calendario = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = estado)
        }
    }
}

// ------------------------------------------------------------------------ tempo

private data class DescTempo(val rotulo: String, val icone: String)

private fun descreverTempo(codigo: Int): DescTempo = when (codigo) {
    0 -> DescTempo("Céu limpo", "☀️")
    1 -> DescTempo("Predomínio de sol", "🌤️")
    2 -> DescTempo("Parcialmente nublado", "⛅")
    3 -> DescTempo("Nublado", "☁️")
    45, 48 -> DescTempo("Neblina", "🌫️")
    51 -> DescTempo("Garoa fraca", "🌦️")
    53 -> DescTempo("Garoa", "🌦️")
    55 -> DescTempo("Garoa intensa", "🌧️")
    61 -> DescTempo("Chuva fraca", "🌦️")
    63 -> DescTempo("Chuva", "🌧️")
    65 -> DescTempo("Chuva forte", "🌧️")
    71, 73 -> DescTempo("Neve", "🌨️")
    75 -> DescTempo("Neve forte", "❄️")
    80 -> DescTempo("Pancadas fracas", "🌦️")
    81 -> DescTempo("Pancadas de chuva", "🌧️")
    82 -> DescTempo("Pancadas fortes", "⛈️")
    95 -> DescTempo("Trovoadas", "⛈️")
    96, 99 -> DescTempo("Trovoadas c/ granizo", "⛈️")
    else -> DescTempo("—", "🌡️")
}

@Composable
private fun BlocoTempo(data: String) {
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    var lista by remember { mutableStateOf<List<PrevisaoTempo>>(emptyList()) }

    LaunchedEffect(data) {
        carregando = true; erro = false
        runCatching { RadarDados.previsaoTempo(data) }
            .onSuccess { lista = it }
            .onFailure { erro = true }
        carregando = false
    }

    Cartao {
        CabecalhoBloco("🌤", "Previsão do tempo", rotuloData(data))
        Spacer(Modifier.height(10.dp))
        when {
            carregando -> CarregandoLinha()
            erro || lista.isEmpty() -> TextoVazio("Não foi possível carregar a previsão.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lista.chunked(2).forEach { par ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        par.forEach { w ->
                            val d = descreverTempo(w.codigo)
                            CartaoInterno(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(d.icone, fontSize = 26.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            w.cidade,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${(w.tempMax ?: 0.0).roundToInt()}° / ${(w.tempMin ?: 0.0).roundToInt()}°",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            d.rotulo + (w.chuvaPct?.let { " · 💧 $it%" } ?: ""),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------ vento

private data class ClasseVento(val rotulo: String, val conselho: String, val cor: Color)

@Composable
private fun classificarVento(nos: Double): ClasseVento = when {
    nos < 7 -> ClasseVento("Vento fraco", "Mar tranquilo — condições ótimas", CoresExtras.Sucesso)
    nos < 15 -> ClasseVento("Vento moderado", "Mar levemente agitado — sem restrição", MaterialTheme.colorScheme.primary)
    nos < 22 -> ClasseVento("Vento forte", "Mar agitado — atenção nos passeios de barco", CoresExtras.Aviso)
    else -> ClasseVento("Vento muito forte", "Mar bravo — risco de cancelamento", MaterialTheme.colorScheme.error)
}

private val DIRECOES = listOf("N", "NE", "L", "SE", "S", "SO", "O", "NO")
private fun rotuloDirecao(graus: Double): String =
    DIRECOES[(((graus % 360) / 45).roundToInt()) % 8]

@Composable
private fun BlocoVento(data: String, cidadeGlobal: String) {
    var local by remember { mutableStateOf(LOCAIS_VENTO.first()) }
    // Segue o seletor geral quando a cidade existir na lista de vento.
    LaunchedEffect(cidadeGlobal) {
        LOCAIS_VENTO.firstOrNull { it.nome == cidadeGlobal }?.let { local = it }
    }
    var menuLocal by remember { mutableStateOf(false) }

    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    var pontos by remember { mutableStateOf<List<PontoVento>>(emptyList()) }

    LaunchedEffect(data, local.nome) {
        carregando = true; erro = false
        runCatching { RadarDados.ventoEMar(local, data) }
            .onSuccess { pontos = it }
            .onFailure { erro = true }
        carregando = false
    }

    val pico = pontos.maxByOrNull { it.nos }
    val media = if (pontos.isNotEmpty()) pontos.sumOf { it.nos } / pontos.size else null

    Cartao {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🌬", fontSize = 18.sp)
            Text("Vento e mar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Box {
                OutlinedButton(onClick = { menuLocal = true }, shape = FormaBotaoPequeno) {
                    Text(local.nome, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuLocal, onDismissRequest = { menuLocal = false }) {
                    LOCAIS_VENTO.forEach { s ->
                        DropdownMenuItem(text = { Text(s.nome) }, onClick = { local = s; menuLocal = false })
                    }
                }
            }
        }

        if (pico != null) {
            val c = classificarVento(pico.nos)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PontoColorido(c.cor)
                Spacer(Modifier.width(6.dp))
                Text(c.conselho, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${c.rotulo} · pico ${pico.nos.roundToInt()} nós" +
                    (media?.let { "  ·  média ${it.roundToInt()} nós" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.cor,
            )
        }

        Spacer(Modifier.height(10.dp))
        when {
            carregando -> CarregandoLinha()
            erro || pontos.isEmpty() -> TextoVazio("Não foi possível carregar a previsão de vento.")
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pontos.chunked(2).forEach { par ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        par.forEach { p ->
                            val c = classificarVento(p.nos)
                            Box(modifier = Modifier.weight(1f)) { CartaoVento(p, c) }
                        }
                        if (par.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Escala: até 7 nós vento fraco · 7–15 moderado · 15–22 forte · acima de 22 muito forte.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (media != null) {
            Spacer(Modifier.height(10.dp))
            CartaoInterno(cor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)) {
                Text(
                    "MÉDIA DO PERÍODO (${pontos.size} horários)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${media.roundToInt()} nós",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "referência usada nos indicadores abaixo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Passeio de barco em Arraial do Cabo / Búzios, pela média do dia.
        if (media != null && (local.nome == "Arraial do Cabo" || local.nome == "Búzios")) {
            val m = media.roundToInt()
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AvisoBarco("Passeio de barco confirmado", CoresExtras.Sucesso, ativo = m <= 13)
                AvisoBarco("Grande chance de cancelamento", CoresExtras.Aviso, ativo = m in 14..17)
                AvisoBarco("Passeio de barco cancelado", MaterialTheme.colorScheme.error, ativo = m >= 18)
            }
        }
    }
}

@Composable
private fun CartaoVento(p: PontoVento, c: ClasseVento) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, c.cor.copy(alpha = 0.35f), FormaBotaoPequeno)
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(p.horaLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Navigation,
                    null,
                    tint = c.cor,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate((p.direcaoGraus + 180).toFloat()),
                )
                Spacer(Modifier.width(4.dp))
                Text(rotuloDirecao(p.direcaoGraus), style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            "${p.nos.roundToInt()} nós",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = c.cor,
        )
        Text(
            "${(p.nos / 0.539957).roundToInt()} km/h · rajada ${p.rajadaNos.roundToInt()} nós",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            (p.ondaM?.let { "onda ${"%.1f".format(it)} m · " } ?: "") + c.rotulo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AvisoBarco(mensagem: String, cor: Color, ativo: Boolean) {
    Surface(
        color = cor.copy(alpha = if (ativo) 0.18f else 0.06f),
        shape = FormaBotaoPequeno,
        border = BorderStroke(1.dp, cor.copy(alpha = if (ativo) 0.6f else 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            PontoColorido(cor.copy(alpha = if (ativo) 1f else 0.4f))
            Spacer(Modifier.width(8.dp))
            Text(
                mensagem,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (ativo) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------- trânsito

private data class ClasseTrafego(val rotulo: String, val conselho: String, val cor: Color)

@Composable
private fun classificarTrafego(razao: Double): ClasseTrafego = when {
    razao < 1.15 -> ClasseTrafego("Trânsito livre", "Percurso fluindo bem — sem retenções relevantes", CoresExtras.Sucesso)
    razao < 1.4 -> ClasseTrafego("Trânsito moderado", "Lentidão em alguns trechos — saia com folga", CoresExtras.Aviso)
    else -> ClasseTrafego("Trânsito intenso", "Congestionamento no trajeto — atraso provável no embarque", MaterialTheme.colorScheme.error)
}

private fun fmtDuracao(min: Double): String {
    val h = (min / 60).toInt()
    val m = (min % 60).roundToInt()
    return if (h > 0) "${h}h${m.toString().padStart(2, '0')}" else "$m min"
}

private val INTERVALOS = listOf(1, 5, 10, 30, 0) // minutos; 0 = desligado

@Composable
private fun BlocoTrafego(
    cidadeGlobal: String,
    fonte: FonteTrafego,
    titulo: String,
) {
    val contexto = LocalContext.current
    var destino by remember { mutableStateOf(DESTINOS_TRAFEGO.first()) }
    LaunchedEffect(cidadeGlobal) {
        DESTINOS_TRAFEGO.firstOrNull { it.nome == cidadeGlobal }?.let { destino = it }
    }
    var menuDestino by remember { mutableStateOf(false) }
    var intervalo by remember { mutableStateOf(5) }
    var menuIntervalo by remember { mutableStateOf(false) }
    var recarregar by remember { mutableStateOf(0) }

    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf(false) }
    var rota by remember { mutableStateOf<RotaTrafego?>(null) }
    var atualizadoEm by remember { mutableStateOf<Long?>(null) }
    var restanteSeg by remember { mutableStateOf(0) }

    LaunchedEffect(destino.nome, recarregar) {
        carregando = true; erro = false
        runCatching { RadarDados.rotaTrafego(destino, fonte) }
            .onSuccess { rota = it; atualizadoEm = System.currentTimeMillis() }
            .onFailure { erro = true }
        carregando = false
        restanteSeg = intervalo * 60
    }

    // Atualização automática + contagem regressiva (como no web).
    LaunchedEffect(intervalo, destino.nome) {
        if (intervalo <= 0) { restanteSeg = 0; return@LaunchedEffect }
        restanteSeg = intervalo * 60
        while (true) {
            delay(1000)
            if (restanteSeg <= 1) {
                restanteSeg = intervalo * 60
                recarregar++
            } else {
                restanteSeg--
            }
        }
    }

    val mapsUrl = "https://www.google.com/maps/dir/?api=1" +
        "&origin=${ORIGEM_TRAFEGO.lat},${ORIGEM_TRAFEGO.lon}" +
        "&destination=${destino.lat},${destino.lon}&travelmode=driving"

    Cartao {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Navigation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { menuDestino = true },
                    shape = FormaBotaoPequeno,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(destino.nome, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuDestino, onDismissRequest = { menuDestino = false }) {
                    DESTINOS_TRAFEGO.forEach { d ->
                        DropdownMenuItem(text = { Text(d.nome) }, onClick = { destino = d; menuDestino = false })
                    }
                }
            }
            Box {
                OutlinedButton(
                    onClick = { menuIntervalo = true },
                    shape = FormaBotaoPequeno,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                ) {
                    Text(if (intervalo == 0) "Desligado" else "Atualiza ${intervalo}min")
                    Icon(Icons.Filled.UnfoldMore, null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuIntervalo, onDismissRequest = { menuIntervalo = false }) {
                    INTERVALOS.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(if (v == 0) "Desligado" else "Atualiza $v min") },
                            onClick = { intervalo = v; menuIntervalo = false },
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { recarregar++ },
                shape = FormaBotaoPequeno,
                contentPadding = PaddingValues(horizontal = 10.dp),
                enabled = !carregando,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        when {
            carregando && rota == null -> CarregandoLinha()
            erro && rota == null -> TextoVazio("Não foi possível calcular a rota agora. Use \"Abrir no mapa\" para ver o trajeto.")
            rota != null -> {
                val r = rota!!
                val razao = r.duracaoMin / maxOf(r.livreMin, 1.0)
                val cls = classificarTrafego(razao)
                val agora = System.currentTimeMillis()
                val chegada = fmtHora.format(java.util.Date(agora + (r.duracaoMin * 60000).toLong()))
                val atraso = maxOf(0, (r.duracaoMin - r.livreMin).roundToInt())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaixaNumero("Distância", "${r.distanciaKm.roundToInt()} km", "${ORIGEM_TRAFEGO.nome} → ${destino.nome}", Modifier.weight(1f))
                    CaixaNumero("Tempo estimado", fmtDuracao(r.duracaoMin), "Sem trânsito: ${fmtDuracao(r.livreMin)}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CartaoInterno(cor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                    Text("PREVISÃO DE CHEGADA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(chegada, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("saindo agora do Rio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    atualizadoEm?.let { at ->
                        val quando = fmtHora.format(java.util.Date(at))
                        val extra = if (intervalo > 0) {
                            val mm = (restanteSeg / 60).toString().padStart(2, '0')
                            val ss = (restanteSeg % 60).toString().padStart(2, '0')
                            " · próxima em $mm:$ss"
                        } else " · auto off"
                        Text(
                            "atualizado às $quando$extra",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Surface(
                    color = cls.cor.copy(alpha = 0.12f),
                    shape = FormaBotaoPequeno,
                    border = BorderStroke(1.dp, cls.cor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        PontoColorido(cls.cor)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cls.rotulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = cls.cor)
                                Spacer(Modifier.width(8.dp))
                                Etiqueta("+$atraso min")
                                Spacer(Modifier.width(6.dp))
                                Etiqueta("fonte: ${rotuloFonte(r.fonte)}")
                            }
                            if (r.fallback) {
                                Spacer(Modifier.height(4.dp))
                                Etiqueta("fallback automático", cor = CoresExtras.Aviso)
                            }
                            if (r.pedagio) {
                                Spacer(Modifier.height(4.dp))
                                Etiqueta("com pedágio")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(cls.conselho, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            r.descricao?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(2.dp))
                                Text("via $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))) },
            shape = FormaBotaoPequeno,
        ) {
            Text("Abrir no mapa")
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CaixaNumero(rotulo: String, valor: String, apoio: String, modifier: Modifier = Modifier) {
    CartaoInterno(modifier = modifier) {
        Text(rotulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(apoio, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- reutilizáveis

@Composable
private fun Cartao(conteudo: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = FormaCartao,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { conteudo() }
    }
}

@Composable
private fun CartaoInterno(
    modifier: Modifier = Modifier,
    cor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    conteudo: @Composable () -> Unit,
) {
    Surface(
        color = cor,
        shape = FormaBotaoPequeno,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) { conteudo() }
    }
}

@Composable
private fun CabecalhoBloco(icone: String, titulo: String, direita: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(icone, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(direita, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Etiqueta(texto: String, cor: Color = MaterialTheme.colorScheme.outline) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.5f)),
        color = Color.Transparent,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PontoColorido(cor: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .border(0.dp, Color.Transparent, RoundedCornerShape(50))
    ) {
        Surface(color = cor, shape = RoundedCornerShape(50), modifier = Modifier.size(10.dp)) {}
    }
}

@Composable
private fun CarregandoLinha() {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun TextoVazio(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
