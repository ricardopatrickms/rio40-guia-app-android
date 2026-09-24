package br.com.rio40graus.guiascale

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.rede.STATUS_NO_SHOW
import br.com.rio40graus.guiascale.rede.STATUS_PARCIAL
import br.com.rio40graus.guiascale.rede.STATUS_RESERVADO
import br.com.rio40graus.guiascale.dados.Posicao
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
import br.com.rio40graus.guiascale.ui.MapaGoogle
import br.com.rio40graus.guiascale.ui.PinoMapa
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    3 to Color(0xFFDA4553), // No show — mesma do web (#da4553)
    8 to Color(0xFFF59E0B),
)

private val COR_PADRAO = Color(0xFF94A3B8)

/**
 * Mapa ao qual o rastro do "Iniciar embarque" deve se vincular.
 *
 * Prefere o mapa do próximo ponto ainda não resolvido; se todos já foram,
 * cai no primeiro mapa do dia. Sem mapa não dá para ligar o rastreio com dono.
 */
private fun mapaDoEmbarque(mapas: List<MapaEmbarque>?): Int? {
    val lista = mapas.orEmpty()
    if (lista.isEmpty()) return null
    val proximo = lista
        .flatMap { mapa -> mapa.reservas.map { mapa to it } }
        .filter { !resolvida(it.second.status?.id) }
        .minByOrNull { it.second.hora ?: "99:99" }
    return proximo?.first?.id ?: lista.first().id
}

private fun corDoStatus(id: Int?) = COR_DO_STATUS[id] ?: COR_PADRAO

/** Status em que o guia já resolveu o embarque — ver STATUS_RESOLVIDOS do web. */
private fun resolvida(id: Int?) = id == STATUS_CHECK_IN || id == 3 || id == 8

/**
 * Atraso do embarque — espelho de `computeDelay` + `delayReferenceTime` do web.
 *
 * Só usa o horário do check-in / parcial / no-show (`checked_in_at`).
 * Sem esse horário, não mostra atraso (não usa o relógio "agora").
 */
private data class Atraso(val minutos: Int, val rotulo: String, val grave: Boolean)

private fun parseCheckedInAtMs(raw: String?): Long? {
    if (raw.isNullOrBlank()) return null
    val limpo = raw.trim().replace(' ', 'T')
    val formatos = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    )
    for (padrao in formatos) {
        runCatching {
            val sdf = SimpleDateFormat(padrao, Locale.US)
            return sdf.parse(limpo)?.time
        }
    }
    return null
}

private fun calcularAtraso(
    dataIso: String?,
    hora: String?,
    checkedInAt: String?,
): Atraso? {
    if (dataIso.isNullOrBlank() || hora.isNullOrBlank()) return null
    val referenciaMs = parseCheckedInAtMs(checkedInAt) ?: return null
    val partesHora = hora.take(5).split(":")
    val h = partesHora.getOrNull(0)?.toIntOrNull() ?: return null
    val m = partesHora.getOrNull(1)?.toIntOrNull() ?: return null
    val partesData = dataIso.split("-")
    val ano = partesData.getOrNull(0)?.toIntOrNull() ?: return null
    val mes = partesData.getOrNull(1)?.toIntOrNull() ?: return null
    val dia = partesData.getOrNull(2)?.toIntOrNull() ?: return null

    val previsto = Calendar.getInstance().apply {
        set(Calendar.YEAR, ano)
        set(Calendar.MONTH, mes - 1)
        set(Calendar.DAY_OF_MONTH, dia)
        set(Calendar.HOUR_OF_DAY, h)
        set(Calendar.MINUTE, m)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val minutos = Math.round((referenciaMs - previsto) / 60000.0).toInt()
    if (minutos <= 0) return Atraso(minutos, "", false)

    val rotulo = when {
        minutos >= 60 -> "+%dh %02d".format(minutos / 60, minutos % 60)
        minutos == 1 -> "+1 minuto"
        else -> "+$minutos minutos"
    }
    return Atraso(minutos, rotulo, minutos >= 15)
}

/**
 * Geocheck-in: os pontos de embarque do dia no mapa, e o check-in de cada um.
 *
 * Layout igual ao web no celular: cabeçalho com filtro de data, legendas,
 * próximo embarque, mapa no meio e lista embaixo.
 *
 * O primeiro check-in liga o rastreio; daí em diante a linha azul no mapa é o
 * caminho que a van já fez. Em data diferente de hoje, entra no modo histórico
 * (sem "próximo" nem rastro ao vivo).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TelaEmbarque(
    aoCarregar: (data: String, aoTerminar: (List<MapaEmbarque>?, String?) -> Unit) -> Unit,
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
    trajetoLocal: List<Posicao>,
    aoPedirLocalizacao: () -> Unit = {},
    embarqueAtivo: Boolean = false,
    aoAlternarEmbarque: (mapaId: Int?) -> Unit = {},
    aoCarregarTrajeto: (data: String, mapaIds: List<Int>, aoTerminar: (List<Pair<Double, Double>>?, String?) -> Unit) -> Unit = { _, _, done -> done(emptyList(), null) },
) {
    val hojeIso = remember { dataIsoDeHoje() }
    var dataSelecionada by remember { mutableStateOf(hojeIso) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    var mapas by remember { mutableStateOf<List<MapaEmbarque>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf<Int?>(null) }
    var recarregar by remember { mutableStateOf(0) }
    var selecionada by remember { mutableStateOf<Int?>(null) }
    /** Incrementa a cada toque na lista para forçar o mapa a recentralizar. */
    var focoPedido by remember { mutableStateOf(0) }
    var editando by remember { mutableStateOf<Pair<MapaEmbarque, ReservaEmbarque>?>(null) }
    var apenasAtrasados by remember { mutableStateOf(false) }
    var trajetoRemoto by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }

    val ehHoje = dataSelecionada == hojeIso
    val dataExibicao = remember(dataSelecionada) { formatarDataBr(dataSelecionada) }
    val idsMapas = remember(mapas) { mapas.orEmpty().map { it.id } }
    val mapaParaRastreio = remember(mapas) { mapaDoEmbarque(mapas) }

    LaunchedEffect(dataSelecionada, recarregar) {
        carregando = true
        erro = null
        selecionada = null
        apenasAtrasados = false
        trajetoRemoto = emptyList()
        aoCarregar(dataSelecionada) { resultado, falha ->
            mapas = resultado
            erro = falha
            carregando = false
        }
    }

    /**
     * Atualiza mapas sem tirar a tela (nem fechar o dialog de check-in).
     * Usado depois de status / pagamento / idioma — igual ao web, que só
     * invalida o query e mantém a modal aberta.
     */
    fun atualizarEmSilencio() {
        aoCarregar(dataSelecionada) { resultado, falha ->
            if (resultado != null) mapas = resultado
            if (falha != null) erro = falha
        }
    }

    // Histórico: o rastro já está no servidor, filtrado por mapa.
    LaunchedEffect(dataSelecionada, idsMapas, ehHoje, recarregar) {
        if (ehHoje || idsMapas.isEmpty()) {
            trajetoRemoto = emptyList()
            return@LaunchedEffect
        }
        aoCarregarTrajeto(dataSelecionada, idsMapas) { pontos, _ ->
            trajetoRemoto = pontos.orEmpty()
        }
    }

    if (mostrarCalendario) {
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = millisUtcDeIso(dataSelecionada),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estadoData.selectedDateMillis?.let { ms ->
                            dataSelecionada = isoDeMillisUtc(ms)
                        }
                        mostrarCalendario = false
                    },
                ) {
                    Text(stringResource(R.string.confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        ) {
            DatePicker(state = estadoData)
        }
    }

    // Cada reserva junto do mapa a que pertence: o check-in precisa dos dois.
    val pontos = remember(mapas) {
        mapas.orEmpty().flatMap { mapa -> mapa.reservas.map { mapa to it } }
            .sortedBy { it.second.hora ?: "" }
    }
    val comCoordenada = pontos.filter { it.second.latitude != null && it.second.longitude != null }
    val posicaoMapa = if (ehHoje) minhaPosicao else null
    // Cada mapa só vê o próprio rastro — local (hoje) ou remoto (histórico).
    val trajetoMapa = remember(ehHoje, trajetoLocal, trajetoRemoto, idsMapas) {
        if (ehHoje) {
            val ids = idsMapas.toSet()
            trajetoLocal
                .filter { it.mapaId != null && it.mapaId in ids }
                .map { it.latitude to it.longitude }
        } else {
            trajetoRemoto
        }
    }

    val aoIniciarOuParar: () -> Unit = {
        if (embarqueAtivo) {
            aoAlternarEmbarque(null)
        } else if (mapaParaRastreio != null) {
            // Sem mapa do dia não inicia: o rastro precisa de dono.
            aoAlternarEmbarque(mapaParaRastreio)
        }
    }

    when {
        carregando -> EstadoEmbarque {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        }

        erro != null -> Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Cabecalho(
                    dataExibicao = dataExibicao,
                    noMapa = 0,
                    total = 0,
                    ehHoje = ehHoje,
                    embarqueAtivo = embarqueAtivo,
                    aoAbrirCalendario = { mostrarCalendario = true },
                    aoIrParaHoje = { dataSelecionada = hojeIso },
                    aoAtualizar = { recarregar++ },
                    aoAlternarEmbarque = aoIniciarOuParar,
                )
                Cartao {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
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
            }
        }

        pontos.isEmpty() -> Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Cabecalho(
                    dataExibicao = dataExibicao,
                    noMapa = 0,
                    total = 0,
                    ehHoje = ehHoje,
                    embarqueAtivo = embarqueAtivo,
                    aoAbrirCalendario = { mostrarCalendario = true },
                    aoIrParaHoje = { dataSelecionada = hojeIso },
                    aoAtualizar = { recarregar++ },
                    aoAlternarEmbarque = aoIniciarOuParar,
                )
                Cartao {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.embarque_sem_mapa),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Igual ao web: mapa com ~60vh (mínimo 380dp) — a lista fica abaixo da dobra.
                val alturaMapa = max(maxHeight * 0.60f, 380.dp)

                // Modo "mover o mapa" (botão no canto do mapa): com ele ligado a
                // tela para de rolar, senão a rolagem fica com o arraste vertical
                // e o guia não consegue andar pelo mapa.
                var moverMapa by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState(), enabled = !moverMapa)
                            .padding(horizontal = 12.dp)
                            .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Cabecalho(
                            dataExibicao = dataExibicao,
                            noMapa = comCoordenada.size,
                            total = pontos.size,
                            ehHoje = ehHoje,
                            embarqueAtivo = embarqueAtivo,
                            aoAbrirCalendario = { mostrarCalendario = true },
                            aoIrParaHoje = { dataSelecionada = hojeIso },
                            aoAtualizar = { recarregar++ },
                            aoAlternarEmbarque = aoIniciarOuParar,
                        )

                        Legendas()

                        if (ehHoje) {
                            pontos.firstOrNull { !resolvida(it.second.status?.id) }?.let { (_, proxima) ->
                                ProximoEmbarque(reserva = proxima, minhaPosicao = posicaoMapa)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(alturaMapa)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    // Borda destacada enquanto o mapa está "preso" ao dedo.
                                    width = if (moverMapa) 2.dp else 1.dp,
                                    color = if (moverMapa) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                ),
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
                                minhaPosicao = posicaoMapa?.let { it.latitude to it.longitude },
                                trajeto = trajetoMapa,
                                aoTocarPino = { selecionada = it },
                                focarEm = selecionada,
                                focoPedido = focoPedido,
                                paddingTopo = 8.dp,
                                paddingBase = 8.dp,
                                aoPedirLocalizacao = aoPedirLocalizacao,
                                moverMapa = moverMapa,
                                aoAlternarMoverMapa = { moverMapa = !moverMapa },
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
                        }

                        if (pontos.size > comCoordenada.size) {
                            Text(
                                text = stringResource(
                                    R.string.embarque_pontos_sem_coord,
                                    pontos.size - comCoordenada.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        ListaDePontos(
                            pontos = pontos,
                            dataIso = dataSelecionada,
                            ehHoje = ehHoje,
                            apenasAtrasados = apenasAtrasados,
                            enviando = enviando,
                            aoAlternarFiltro = { apenasAtrasados = !apenasAtrasados },
                            aoTocarPonto = { _, reserva ->
                                if (reserva.latitude != null && reserva.longitude != null) {
                                    selecionada = reserva.id
                                    focoPedido++
                                }
                            },
                            aoEditar = { mapa, reserva ->
                                selecionada = null
                                editando = mapa to reserva
                            },
                        )
                    }

                    editando?.let { (mapa, reserva) ->
                        val atual = pontos.firstOrNull { it.second.id == reserva.id } ?: (mapa to reserva)
                        DialogCheckInEmbarque(
                            reserva = atual.second,
                            nomeTour = atual.first.tour,
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
                            aoRecarregar = { atualizarEmSilencio() },
                        )
                    }
                }
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
private fun Cabecalho(
    dataExibicao: String,
    noMapa: Int,
    total: Int,
    ehHoje: Boolean,
    embarqueAtivo: Boolean,
    aoAbrirCalendario: () -> Unit,
    aoIrParaHoje: () -> Unit,
    aoAtualizar: () -> Unit,
    aoAlternarEmbarque: () -> Unit,
) {
    // Título à esquerda; data à direita. Na linha de baixo: Iniciar à esquerda,
    // Atualizar à direita — mesmo eixo visual do Atualizar, alinhados no começo/fim.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.geocheckin_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(
                        if (ehHoje) R.string.embarque_subtitulo
                        else R.string.embarque_subtitulo_historico,
                        dataExibicao,
                        noMapa,
                        total,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(FormaBotaoPequeno)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = FormaBotaoPequeno,
                        )
                        .clickable(onClick = aoAbrirCalendario)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = dataExibicao,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = stringResource(R.string.embarque_filtrar_data),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (!ehHoje) {
                    Text(
                        text = stringResource(R.string.embarque_hoje),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(FormaBotaoPequeno)
                            .clickable(onClick = aoIrParaHoje)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (ehHoje) Arrangement.SpaceBetween else Arrangement.End,
        ) {
            if (ehHoje) {
                val fundoIniciar = if (embarqueAtivo) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val textoIniciar = if (embarqueAtivo) {
                    MaterialTheme.colorScheme.onError
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                Row(
                    modifier = Modifier
                        .clip(FormaBotaoPequeno)
                        .background(fundoIniciar)
                        .clickable(onClick = aoAlternarEmbarque)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (embarqueAtivo) R.string.embarque_parar
                            else R.string.embarque_iniciar,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = textoIniciar,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(FormaBotaoPequeno)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = FormaBotaoPequeno,
                    )
                    .clickable(onClick = aoAtualizar)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.embarque_atualizar),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** Atraso e status — mesmas legendas do Geocheck-in web. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legendas() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.embarque_legenda_atraso),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ChipLegenda(
                texto = stringResource(R.string.embarque_atraso_ate_15),
                bolinha = Color(0xFF3B82F6),
                fundo = Color(0xFFDBEAFE),
                textoCor = Color(0xFF1E40AF),
                borda = Color(0xFF93C5FD),
            )
            ChipLegenda(
                texto = stringResource(R.string.embarque_atraso_acima_15),
                bolinha = Color(0xFFEF4444),
                fundo = Color(0xFFFEE2E2),
                textoCor = Color(0xFFB91C1C),
                borda = Color(0xFFFCA5A5),
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.embarque_legenda_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ItemStatusLegenda(stringResource(R.string.embarque_status_checkin), Color(0xFF16A34A))
            ItemStatusLegenda(stringResource(R.string.embarque_status_reservado), Color(0xFF3B82F6))
            ItemStatusLegenda(stringResource(R.string.embarque_status_parcial), Color(0xFFF59E0B))
            ItemStatusLegenda(stringResource(R.string.embarque_status_noshow), Color(0xFFDA4553))
        }
    }
}

@Composable
private fun ChipLegenda(
    texto: String,
    bolinha: Color,
    fundo: Color,
    textoCor: Color,
    borda: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borda, RoundedCornerShape(6.dp))
            .background(fundo)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(bolinha),
        )
        Text(text = texto, style = MaterialTheme.typography.labelSmall, color = textoCor)
    }
}

@Composable
private fun ItemStatusLegenda(texto: String, cor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(cor),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
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
 * A lista de pontos de embarque, abaixo do mapa (como no web).
 *
 * Ordenada por horário, que é a ordem em que a van passa. À direita ficam as
 * duas informações que o guia consulta de relance: o selo de status e, quando
 * houver, o quanto aquele ponto está atrasado.
 */
@Composable
private fun ListaDePontos(
    pontos: List<Pair<MapaEmbarque, ReservaEmbarque>>,
    dataIso: String,
    ehHoje: Boolean,
    apenasAtrasados: Boolean,
    enviando: Int?,
    aoAlternarFiltro: () -> Unit,
    aoTocarPonto: (MapaEmbarque, ReservaEmbarque) -> Unit,
    aoEditar: (MapaEmbarque, ReservaEmbarque) -> Unit,
) {
    // Igual ao web: filtro "apenas atrasados" usa previsto × checked_in_at.
    val visiveis = pontos.filter { (_, r) ->
        if (!ehHoje || !apenasAtrasados) return@filter true
        (calcularAtraso(dataIso, r.hora, r.checked_in_at)?.minutos ?: 0) > 0
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
                if (ehHoje) {
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
                }
                Text(
                    text = stringResource(R.string.embarque_contagem, pontos.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                }
            }

            visiveis.forEachIndexed { index, (mapa, reserva) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    )
                }
                LinhaDoPonto(
                    reserva = reserva,
                    // Qualquer dia: atraso = previsto × horário do check-in (web).
                    atraso = calcularAtraso(dataIso, reserva.hora, reserva.checked_in_at),
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
    val qtdPax = reserva.adt + reserva.chd + reserva.inf
    // Igual ao web: nome · Apto · N pax · idioma
    val subtitulo = listOfNotNull(
        reserva.pax?.takeIf { it.isNotBlank() } ?: "—",
        reserva.apto?.takeIf { it.isNotBlank() }
            ?.let { stringResource(R.string.embarque_apto, it) },
        stringResource(R.string.embarque_lista_pax, qtdPax),
        reserva.idioma?.takeIf { it.isNotBlank() }?.uppercase(Locale.US),
    ).joinToString(" · ")

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
                text = subtitulo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!bloqueado) {
                // Web: Button size="sm" h-7 text-[11px] variant="outline" — compacto.
                Text(
                    text = stringResource(R.string.check_in_editar),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        .clickable(enabled = !enviando, onClick = aoEditar)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Selo(reserva.status?.id, reserva.status?.nome)

            when {
                atraso != null && atraso.minutos > 0 -> EtiquetaAtraso(
                    texto = atraso.rotulo,
                    grave = atraso.grave,
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

/** Cores do SeloStatus do web (fundo / texto / borda). */
private fun coresSeloLista(statusId: Int?): Triple<Color, Color, Color> = when (statusId) {
    STATUS_CHECK_IN -> Triple(Color(0xFFD1FAE5), Color(0xFF047857), Color(0xFF6EE7B7))
    STATUS_PARCIAL -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFCD34D))
    STATUS_NO_SHOW -> Triple(Color(0xFFFCE7EB), Color(0xFFDA4553), Color(0xFFF5A3AB))
    STATUS_RESERVADO -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF93C5FD))
    else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFFCBD5E1))
}

/** O selo de status, no mesmo formato do SeloStatus do web (borda mais escura). */
@Composable
private fun Selo(statusId: Int?, nome: String?) {
    val texto = nome?.takeIf { it.isNotBlank() } ?: return
    val (fundo, textoCor, borda) = coresSeloLista(statusId)
    Box(
        modifier = Modifier
            .heightIn(min = 20.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borda, RoundedCornerShape(4.dp))
            .background(fundo)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto.uppercase(Locale.getDefault()),
            color = textoCor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Badge de atraso — delayBadgeClasses do web (borda mais escura que o fundo). */
@Composable
private fun EtiquetaAtraso(texto: String, grave: Boolean) {
    if (texto.isBlank()) return
    val (fundo, textoCor, borda) = if (grave) {
        Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFFFCA5A5))
    } else {
        Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF93C5FD))
    }
    Box(
        modifier = Modifier
            .heightIn(min = 20.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borda, RoundedCornerShape(4.dp))
            .background(fundo)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto.uppercase(Locale.getDefault()),
            color = textoCor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
        )
    }
}


/** Hoje em yyyy-MM-dd (fuso local), igual ao filtro do web. */
private fun dataIsoDeHoje(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun formatarDataBr(iso: String): String = try {
    val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    format.format(parse.parse(iso)!!)
} catch (_: Exception) {
    iso
}

/**
 * DatePicker do Material usa millis em UTC midnight. Converte yyyy-MM-dd
 * local para esse valor sem deslocar o dia.
 */
private fun millisUtcDeIso(iso: String): Long {
    val partes = iso.split("-")
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, partes.getOrNull(0)?.toIntOrNull() ?: 1970)
        set(Calendar.MONTH, (partes.getOrNull(1)?.toIntOrNull() ?: 1) - 1)
        set(Calendar.DAY_OF_MONTH, partes.getOrNull(2)?.toIntOrNull() ?: 1)
    }
    return cal.timeInMillis
}

private fun isoDeMillisUtc(ms: Long): String {
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = ms
    }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}
