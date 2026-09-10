@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package br.com.rio40graus.guiascale

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import br.com.rio40graus.guiascale.ui.MapaGoogle
import br.com.rio40graus.guiascale.ui.PinoMapa
import java.util.Calendar

/*
 * Cor do pino por status da reserva.
 *
 * Copiadas do COR_DO_STATUS do Geocheckin.tsx, inclusive a decisão que está
 * comentada lá: PARCIAL ganha âmbar em vez do azul que divide com RESERVADO no
 * resto do app, porque dois pinos azuis no mapa seriam indistinguíveis — e é
 * justamente o parcial que o guia precisa achar.
 *
 * As chaves são os ids do rio40graus: 1 RESERVADO, 2 CHECK-IN, 3 NO-SHOW,
 * 8 PARCIAL.
 */
private val COR_DO_STATUS = mapOf(
    1 to Color(0xFF3B82F6),
    2 to Color(0xFF16A34A),
    3 to Color(0xFFEA580C),
    8 to Color(0xFFF59E0B),
)

private val COR_PADRAO = Color(0xFF94A3B8)

private fun corDoStatus(id: Int?) = COR_DO_STATUS[id] ?: COR_PADRAO

/** Status em que o guia já resolveu o embarque — ver STATUS_RESOLVIDOS do web. */
private fun resolvida(id: Int?) = id == STATUS_CHECK_IN || id == 3 || id == 8

/**
 * Atraso do embarque, na mesma conta do `computeDelay` do web.
 *
 * Abaixo de 15 minutos é aviso; de 15 em diante é atraso de verdade. São os
 * mesmos dois patamares da legenda no topo da tela.
 */
private data class Atraso(val minutos: Int, val rotulo: String, val grave: Boolean)

private fun calcularAtraso(hora: String?, agora: Long): Atraso? {
    if (hora.isNullOrBlank()) return null
    val partes = hora.take(5).split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
    val m = partes.getOrNull(1)?.toIntOrNull() ?: return null

    val previsto = Calendar.getInstance().apply {
        timeInMillis = agora
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, m)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val minutos = Math.round((agora - previsto) / 60000.0).toInt()
    if (minutos <= 0) return Atraso(minutos, "", false)

    val rotulo = if (minutos >= 60) {
        "+%dh %02d".format(minutos / 60, minutos % 60)
    } else {
        "+$minutos min"
    }

    return Atraso(minutos, rotulo, minutos >= 15)
}

/**
 * Geocheck-in: os pontos de embarque do dia no mapa, e o check-in de cada um.
 *
 * É a mesma tela do app web (Geocheckin.tsx), com as mesmas partes e na mesma
 * ordem: legendas de atraso e status, o cartão do próximo embarque com a
 * distância, o mapa com um pino por reserva, e a lista embaixo. O que muda é
 * só o que a plataforma obriga — o balão do pino é desenhado por cima do mapa
 * em vez de ser uma janela do Leaflet.
 *
 * O primeiro check-in liga o rastreio; daí em diante a linha azul no mapa é o
 * caminho que a van já fez.
 */
@Composable
fun TelaEmbarque(
    aoCarregar: (aoTerminar: (List<MapaEmbarque>?, String?) -> Unit) -> Unit,
    aoCheckIn: (reservaId: Int, mapaId: Int, aoTerminar: (String?) -> Unit) -> Unit,
    minhaPosicao: Location?,
    trajeto: List<Pair<Double, Double>>,
) {
    var mapas by remember { mutableStateOf<List<MapaEmbarque>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf<Int?>(null) }
    var recarregar by remember { mutableStateOf(0) }
    var selecionada by remember { mutableStateOf<Int?>(null) }
    var apenasAtrasados by remember { mutableStateOf(false) }

    val agora = remember(recarregar) { System.currentTimeMillis() }

    LaunchedEffect(recarregar) {
        carregando = true
        erro = null
        aoCarregar { resultado, falha ->
            mapas = resultado
            erro = falha
            carregando = false
        }
    }

    // Cada reserva junto do mapa a que pertence: o check-in precisa dos dois.
    val pontos = remember(mapas) {
        mapas.orEmpty().flatMap { mapa -> mapa.reservas.map { mapa to it } }
            .sortedBy { it.second.hora ?: "" }
    }
    val comCoordenada = pontos.filter { it.second.latitude != null && it.second.longitude != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = ESPACO_DA_BARRA),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Cabecalho(
            noMapa = comCoordenada.size,
            total = pontos.size,
            aoAtualizar = { recarregar++ },
        )

        Legendas()

        when {
            carregando -> Cartao {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }

            erro != null -> Cartao {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = erro!!, style = MaterialTheme.typography.bodyMedium)
                    BotaoSecundario(
                        texto = stringResource(R.string.embarque_recarregar),
                        aoClicar = { recarregar++ },
                    )
                }
            }

            pontos.isEmpty() -> Cartao {
                Text(
                    text = stringResource(R.string.embarque_sem_mapa),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }

            else -> {
                // Próximo embarque: o primeiro ainda não resolvido.
                pontos.firstOrNull { !resolvida(it.second.status?.id) }?.let { (_, proxima) ->
                    ProximoEmbarque(reserva = proxima, minhaPosicao = minhaPosicao)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(FormaCartao),
                ) {
                    MapaGoogle(
                        pinos = comCoordenada.map { (_, r) ->
                            PinoMapa(
                                id = r.id,
                                latitude = r.latitude!!,
                                longitude = r.longitude!!,
                                cor = corDoStatus(r.status?.id).toArgb(),
                                titulo = r.embarque.orEmpty(),
                            )
                        },
                        minhaPosicao = minhaPosicao?.let { it.latitude to it.longitude },
                        trajeto = trajeto,
                        aoTocarPino = { selecionada = it },
                        modifier = Modifier.fillMaxSize(),
                    )

                    // O balão do pino, desenhado por cima do mapa.
                    pontos.firstOrNull { it.second.id == selecionada }?.let { (mapa, reserva) ->
                        BalaoDoPonto(
                            reserva = reserva,
                            enviando = enviando == reserva.id,
                            bloqueado = mapa.bloqueado,
                            aoFechar = { selecionada = null },
                            aoCheckIn = {
                                enviando = reserva.id
                                aoCheckIn(reserva.id, mapa.id) { falha ->
                                    enviando = null
                                    if (falha == null) {
                                        selecionada = null
                                        recarregar++
                                    } else {
                                        erro = falha
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                if (pontos.size > comCoordenada.size) {
                    Text(
                        text = "${pontos.size - comCoordenada.size} ponto(s) sem latitude/longitude no cadastro do sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ListaDePontos(
                    pontos = pontos,
                    agora = agora,
                    apenasAtrasados = apenasAtrasados,
                    enviando = enviando,
                    aoAlternarFiltro = { apenasAtrasados = !apenasAtrasados },
                    aoCheckIn = { mapa, reserva ->
                        enviando = reserva.id
                        aoCheckIn(reserva.id, mapa.id) { falha ->
                            enviando = null
                            if (falha == null) recarregar++ else erro = falha
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Cabecalho(noMapa: Int, total: Int, aoAtualizar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.aba_embarque),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.embarque_no_mapa, noMapa, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .clip(FormaBotaoPequeno)
                .clickable(onClick = aoAtualizar)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.embarque_atualizar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** As duas leituras do mesmo ponto: atraso no horário, status na cor. */
@Composable
private fun Legendas() {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ItemLegenda(stringResource(R.string.atraso), null)
        ItemLegenda(stringResource(R.string.atraso_ate), Color(0xFF3B82F6))
        ItemLegenda(stringResource(R.string.atraso_acima), Color(0xFFDC2626))
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ItemLegenda(stringResource(R.string.legenda_status), null)
        ItemLegenda("Check-in", COR_DO_STATUS[2]!!)
        ItemLegenda("Reservado", COR_DO_STATUS[1]!!)
        ItemLegenda("Parcial", COR_DO_STATUS[8]!!)
        ItemLegenda("No show", COR_DO_STATUS[3]!!)
    }
}

@Composable
private fun ItemLegenda(texto: String, cor: Color?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        cor?.let {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(it))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProximoEmbarque(reserva: ReservaEmbarque, minhaPosicao: Location?) {
    /*
     * Distância em linha reta, como no web. Não é a da rua e não pretende ser:
     * serve para o guia saber se o próximo ponto está na esquina ou do outro
     * lado da cidade, e para isso a reta basta.
     */
    val metros = remember(reserva.id, minhaPosicao) {
        if (minhaPosicao == null || reserva.latitude == null || reserva.longitude == null) {
            null
        } else {
            FloatArray(1).also {
                Location.distanceBetween(
                    minhaPosicao.latitude, minhaPosicao.longitude,
                    reserva.latitude, reserva.longitude, it,
                )
            }[0]
        }
    }

    Cartao {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.embarque_proximo).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${reserva.hora?.take(5) ?: "--:--"} · ${reserva.embarque.orEmpty()}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            metros?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (it < 1000) "${it.toInt()} m" else "%.1f km".format(it / 1000),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.embarque_distancia),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * O balão do pino, com o mesmo conteúdo do Popup do Leaflet no web.
 *
 * Desenhado em Compose por cima do mapa, e não como janela do Maps: assim
 * a tipografia, o selo de status e o botão são os mesmos do resto do app.
 */
@Composable
private fun BalaoDoPonto(
    reserva: ReservaEmbarque,
    enviando: Boolean,
    bloqueado: Boolean,
    aoFechar: () -> Unit,
    aoCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Cartao(modifier = modifier.padding(24.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = reserva.embarque.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = aoFechar)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            reserva.hora?.let {
                LinhaDoBalao(stringResource(R.string.embarque_horario, it.take(5)))
            }
            reserva.pax?.takeIf { it.isNotBlank() }?.let {
                LinhaDoBalao(stringResource(R.string.embarque_cliente, it))
            }
            reserva.apto?.takeIf { it.isNotBlank() }?.let {
                LinhaDoBalao(stringResource(R.string.embarque_apto_popup, it))
            }
            reserva.bairro?.takeIf { it.isNotBlank() }?.let { LinhaDoBalao(it) }

            Box(modifier = Modifier.padding(top = 4.dp)) {
                Selo(reserva.status?.id, reserva.status?.nome)
            }

            if (!resolvida(reserva.status?.id) && !bloqueado) {
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    BotaoPrincipal(
                        texto = stringResource(R.string.check_in_editar),
                        carregando = enviando,
                        habilitado = !enviando,
                        aoClicar = aoCheckIn,
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDoBalao(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A lista de pontos de embarque, embaixo do mapa.
 *
 * Ordenada por horário, que é a ordem em que a van passa. À direita ficam as
 * duas informações que o guia consulta de relance: o selo de status e, quando
 * houver, o quanto aquele ponto está atrasado.
 */
@Composable
private fun ListaDePontos(
    pontos: List<Pair<MapaEmbarque, ReservaEmbarque>>,
    agora: Long,
    apenasAtrasados: Boolean,
    enviando: Int?,
    aoAlternarFiltro: () -> Unit,
    aoCheckIn: (MapaEmbarque, ReservaEmbarque) -> Unit,
) {
    val visiveis = pontos.filter { (_, r) ->
        if (!apenasAtrasados) return@filter true
        if (resolvida(r.status?.id)) return@filter false
        (calcularAtraso(r.hora, agora)?.minutos ?: 0) > 0
    }

    Cartao {
        Column {
            // Título em cima, filtro e contagem embaixo. É o `flex-col
            // sm:flex-row` do web: numa largura de celular os três não cabem
            // lado a lado, e o título quebrava no meio.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.embarque_pontos_titulo),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Row(
                    modifier = Modifier
                        .clip(FormaBotaoPequeno)
                        .clickable(onClick = aoAlternarFiltro)
                        .background(
                            if (apenasAtrasados) Color(0xFFFEF3C7) else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            if (apenasAtrasados) R.string.embarque_apenas_atrasados
                            else R.string.embarque_filtrar_atrasados
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.embarque_contagem, pontos.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                }
            }

            visiveis.forEach { (mapa, reserva) ->
                LinhaDoPonto(
                    reserva = reserva,
                    atraso = calcularAtraso(reserva.hora, agora),
                    enviando = enviando == reserva.id,
                    bloqueado = mapa.bloqueado,
                    aoCheckIn = { aoCheckIn(mapa, reserva) },
                )
            }
        }
    }
}

@Composable
private fun LinhaDoPonto(
    reserva: ReservaEmbarque,
    atraso: Atraso?,
    enviando: Boolean,
    bloqueado: Boolean,
    aoCheckIn: () -> Unit,
) {
    val feito = resolvida(reserva.status?.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = reserva.hora?.take(5) ?: "—",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(52.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reserva.embarque.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = listOfNotNull(
                    reserva.pax?.takeIf { it.isNotBlank() } ?: "—",
                    reserva.apto?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.embarque_apto, it) },
                    reserva.bairro?.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!feito && !bloqueado) {
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    BotaoSecundario(
                        texto = stringResource(R.string.check_in_editar),
                        aoClicar = aoCheckIn,
                        habilitado = !enviando,
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Selo(reserva.status?.id, reserva.status?.nome)

            when {
                atraso != null && atraso.minutos > 0 -> Etiqueta(
                    texto = atraso.rotulo,
                    cor = if (atraso.grave) Color(0xFFDC2626) else Color(0xFF3B82F6),
                )
                // "No horário" só enquanto o embarque não aconteceu: depois de
                // resolvido, o selo já diz o que houve.
                atraso != null && !feito -> Text(
                    text = stringResource(R.string.embarque_no_horario),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (reserva.latitude == null) {
                Text(
                    text = stringResource(R.string.embarque_sem_gps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** O selo de status, no mesmo formato do SeloStatus do web. */
@Composable
private fun Selo(statusId: Int?, nome: String?) {
    Etiqueta(texto = nome.orEmpty(), cor = corDoStatus(statusId))
}

@Composable
private fun Etiqueta(texto: String, cor: Color) {
    if (texto.isBlank()) return

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(cor.copy(alpha = 0.14f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = cor,
        )
    }
}
