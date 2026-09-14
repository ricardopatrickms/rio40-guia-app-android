package br.com.rio40graus.guiascale

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
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
 * Abaixo de 15 minutos é aviso; de 15 em diante é atraso de verdade.
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
 * O mapa ocupa a tela inteira. A lista sobe num painel arrastável (bottom sheet),
 * como no Uber: puxa para cima para ver mais pontos, em vez de só rolar dentro
 * de uma faixa fixa.
 *
 * O primeiro check-in liga o rastreio; daí em diante a linha azul no mapa é o
 * caminho que a van já fez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEmbarque(
    aoCarregar: (aoTerminar: (List<MapaEmbarque>?, String?) -> Unit) -> Unit,
    aoTrocarStatus: (
        reservaId: Int,
        mapaId: Int,
        statusId: Int,
        motivoId: Int?,
        parcial: Map<String, Int>?,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
    aoCarregarMotivos: ((RespostaMotivos?, String?) -> Unit) -> Unit,
    aoCarregarFormas: ((List<FormaPagamento>?, String?) -> Unit) -> Unit,
    aoCarregarParcelas: ((List<ParcelaOpcao>?, String?) -> Unit) -> Unit,
    aoCarregarIdiomas: ((List<IdiomaOpcao>?, String?) -> Unit) -> Unit,
    aoSalvarPagamentos: (Int, List<ItemPagamento>, (String?) -> Unit) -> Unit,
    aoSalvarIdioma: (Int, Int, (String?) -> Unit) -> Unit,
    minhaPosicao: Location?,
    trajeto: List<Pair<Double, Double>>,
    aoPedirLocalizacao: () -> Unit = {},
) {
    var mapas by remember { mutableStateOf<List<MapaEmbarque>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf<Int?>(null) }
    var recarregar by remember { mutableStateOf(0) }
    var selecionada by remember { mutableStateOf<Int?>(null) }
    var editando by remember { mutableStateOf<Pair<MapaEmbarque, ReservaEmbarque>?>(null) }
    var apenasAtrasados by remember { mutableStateOf(false) }

    val agora = remember(recarregar) { System.currentTimeMillis() }
    val painel = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true,
        ),
    )

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

    when {
        carregando -> EstadoEmbarque {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }

        erro != null -> EstadoEmbarque {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = erro!!, style = MaterialTheme.typography.bodyMedium)
                BotaoSecundario(
                    texto = stringResource(R.string.embarque_recarregar),
                    aoClicar = { recarregar++ },
                )
            }
        }

        pontos.isEmpty() -> EstadoEmbarque {
            Text(
                text = stringResource(R.string.embarque_sem_mapa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> {
            BottomSheetScaffold(
                scaffoldState = painel,
                // Peek: próximo + começo da lista. Arrastar a alça sobe o painel.
                sheetPeekHeight = 300.dp,
                sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                sheetShadowElevation = 8.dp,
                sheetDragHandle = { BottomSheetDefaults.DragHandle() },
                sheetContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.88f)
                            .padding(horizontal = 12.dp)
                            .padding(bottom = ESPACO_DA_BARRA + 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pontos.firstOrNull { !resolvida(it.second.status?.id) }?.let { (_, proxima) ->
                            ProximoEmbarque(reserva = proxima, minhaPosicao = minhaPosicao)
                        }

                        if (pontos.size > comCoordenada.size) {
                            Cartao {
                                Text(
                                    text = "${pontos.size - comCoordenada.size} ponto(s) sem latitude/longitude no cadastro do sistema.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                            }
                        }

                        // A lista rola depois que o painel já subiu — o gesto
                        // vertical no topo arrasta a folha; no miolo da lista, rola.
                        Box(modifier = Modifier.weight(1f, fill = true)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                ListaDePontos(
                                    pontos = pontos,
                                    agora = agora,
                                    apenasAtrasados = apenasAtrasados,
                                    enviando = enviando,
                                    aoAlternarFiltro = { apenasAtrasados = !apenasAtrasados },
                                    aoTocarPonto = { _, reserva ->
                                        if (reserva.latitude != null && reserva.longitude != null) {
                                            selecionada = reserva.id
                                        }
                                    },
                                    aoEditar = { mapa, reserva ->
                                        selecionada = null
                                        editando = mapa to reserva
                                    },
                                )
                            }
                        }
                    }
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
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
                        focarEm = selecionada,
                        paddingTopo = 100.dp,
                        paddingBase = 300.dp,
                        aoPedirLocalizacao = aoPedirLocalizacao,
                        modifier = Modifier.fillMaxSize(),
                    )

                    pontos.firstOrNull { it.second.id == selecionada }?.let { (mapa, reserva) ->
                        BalaoDoPonto(
                            reserva = reserva,
                            bloqueado = mapa.bloqueado,
                            aoFechar = { selecionada = null },
                            aoEditar = {
                                selecionada = null
                                editando = mapa to reserva
                            },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    Cartao(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .safeDrawingPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Cabecalho(
                                noMapa = comCoordenada.size,
                                total = pontos.size,
                            )
                        }
                    }
                }
            }

            editando?.let { (mapa, reserva) ->
                // Reserva sempre atualizada após reload (status/pagamentos).
                val atual = pontos.firstOrNull { it.second.id == reserva.id } ?: (mapa to reserva)
                DialogCheckInEmbarque(
                    reserva = atual.second,
                    bloqueado = atual.first.bloqueado,
                    enviando = enviando == atual.second.id,
                    acoes = AcoesCheckIn(
                        carregarMotivos = aoCarregarMotivos,
                        carregarFormas = aoCarregarFormas,
                        carregarParcelas = aoCarregarParcelas,
                        carregarIdiomas = aoCarregarIdiomas,
                        trocarStatus = { statusId, motivoId, parcial, aoTerminar ->
                            enviando = atual.second.id
                            aoTrocarStatus(
                                atual.second.id,
                                atual.first.id,
                                statusId,
                                motivoId,
                                parcial,
                            ) { falha ->
                                enviando = null
                                aoTerminar(falha)
                            }
                        },
                        salvarPagamentos = { itens, aoTerminar ->
                            aoSalvarPagamentos(atual.second.id, itens, aoTerminar)
                        },
                        salvarIdioma = { idiomaId, aoTerminar ->
                            aoSalvarIdioma(atual.second.id, idiomaId, aoTerminar)
                        },
                    ),
                    aoFechar = { if (enviando != atual.second.id) editando = null },
                    aoRecarregar = { recarregar++ },
                )
            }
        }
    }
}

/** Tela vazia / erro / loading, sem mapa — só o aviso central. */
@Composable
private fun EstadoEmbarque(conteudo: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = ESPACO_DA_BARRA),
        contentAlignment = Alignment.Center,
    ) {
        Cartao {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                conteudo()
            }
        }
    }
}

@Composable
private fun Cabecalho(noMapa: Int, total: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
    bloqueado: Boolean,
    aoFechar: () -> Unit,
    aoEditar: () -> Unit,
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

            // Igual ao web: o botão aparece sempre (mesmo após check-in) e abre
            // a modal de edição — não some quando o status já está resolvido.
            if (!bloqueado) {
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    BotaoPrincipal(
                        texto = stringResource(R.string.check_in_editar),
                        aoClicar = aoEditar,
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
 * A lista de pontos de embarque, dentro do painel arrastável.
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
    aoTocarPonto: (MapaEmbarque, ReservaEmbarque) -> Unit,
    aoEditar: (MapaEmbarque, ReservaEmbarque) -> Unit,
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
                    aoTocar = { aoTocarPonto(mapa, reserva) },
                    aoEditar = { aoEditar(mapa, reserva) },
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
    aoTocar: () -> Unit,
    aoEditar: () -> Unit,
) {
    val feito = resolvida(reserva.status?.id)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = aoTocar)
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

            if (!bloqueado) {
                Box(modifier = Modifier.padding(top = 6.dp)) {
                    BotaoSecundario(
                        texto = stringResource(R.string.check_in_editar),
                        aoClicar = aoEditar,
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
