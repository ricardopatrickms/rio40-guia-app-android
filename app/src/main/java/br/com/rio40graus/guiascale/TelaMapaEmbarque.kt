package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.ItemPagamentoFornecedor
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.OcorrenciaMapa
import br.com.rio40graus.guiascale.rede.PagamentoFornecedor
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.ParcialEmbarque
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.rede.STATUS_NO_SHOW
import br.com.rio40graus.guiascale.rede.STATUS_PARCIAL
import br.com.rio40graus.guiascale.rede.STATUS_RESERVADO
import br.com.rio40graus.guiascale.rede.StatusReserva
import br.com.rio40graus.guiascale.rede.Sessao
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

/**
 * Mapa de embarque do app — espelho do `MapaEmbarque.tsx` do web (visão do guia).
 *
 * Abas Em andamento / Finalizados, chip do tour, seções TOUR, FORNECEDORES,
 * EMBARQUES DO DIA, OCORRÊNCIAS e INFORMAÇÕES. O check-in de cada ponto reusa
 * o mesmo dialog do Geocheck-in.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TelaMapaEmbarque(
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
    aoCarregarOcorrencias: (mapaId: Int, aoTerminar: (List<OcorrenciaMapa>?, String?) -> Unit) -> Unit,
    aoCriarOcorrencia: (mapaId: Int, relato: String, aoTerminar: (String?) -> Unit) -> Unit,
    aoEditarOcorrencia: (id: Int, relato: String, aoTerminar: (String?) -> Unit) -> Unit,
    aoExcluirOcorrencia: (id: Int, aoTerminar: (String?) -> Unit) -> Unit,
    aoSalvarPagamentoFornecedor: (
        acertoFornecedorId: Int,
        itens: List<ItemPagamentoFornecedor>,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
    aoCarregarNomeGuia: ((String?) -> Unit) -> Unit = {},
) {
    val hojeIso = remember { dataIsoMapaHoje() }
    var dataSelecionada by remember { mutableStateOf(hojeIso) }
    var mostrarCalendario by remember { mutableStateOf(false) }
    var mapas by remember { mutableStateOf<List<MapaEmbarque>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var recarregar by remember { mutableStateOf(0) }
    var abaFinalizados by remember { mutableStateOf(false) }
    var mapaSelecionadoId by remember { mutableStateOf<Int?>(null) }
    var enviando by remember { mutableStateOf<Int?>(null) }
    var editando by remember { mutableStateOf<Pair<MapaEmbarque, ReservaEmbarque>?>(null) }
    var ocorrencias by remember { mutableStateOf<List<OcorrenciaMapa>>(emptyList()) }
    var relatoNovo by remember { mutableStateOf("") }
    var salvandoOcorrencia by remember { mutableStateOf(false) }
    var editandoOcorrenciaId by remember { mutableStateOf<Int?>(null) }
    var relatoEdicao by remember { mutableStateOf("") }
    var salvandoEdicao by remember { mutableStateOf(false) }
    var excluirOcorrenciaId by remember { mutableStateOf<Int?>(null) }
    var erroOcorrencia by remember { mutableStateOf<String?>(null) }
    var editandoFornecedor by remember { mutableStateOf<FornecedorUi?>(null) }
    // Só uma seção expansível aberta por vez (TOUR / FORNECEDORES / …).
    var secaoAbertaId by remember(mapaSelecionadoId) {
        mutableStateOf<String?>(SecaoMapaIds.TOUR)
    }
    var nomeGuia by remember {
        mutableStateOf(Sessao.nomeExibicao ?: "")
    }
    var meuUsuId by remember { mutableStateOf(Sessao.usuId) }

    val dataExibicao = remember(dataSelecionada) { formatarDataMapaBr(dataSelecionada) }
    val semNome = stringResource(R.string.conta_sem_nome)

    LaunchedEffect(Unit) {
        aoCarregarNomeGuia { nome ->
            nomeGuia = nome?.takeIf { it.isNotBlank() }
                ?: Sessao.nomeExibicao
                ?: semNome
            meuUsuId = Sessao.usuId
        }
    }

    fun aplicarMapas(resultado: List<MapaEmbarque>?) {
        mapas = resultado
        val lista = resultado.orEmpty()
        val emAndamento = lista.filter { !it.bloqueado }
        val finalizados = lista.filter { it.bloqueado }
        val alvo = if (abaFinalizados) finalizados else emAndamento
        if (alvo.none { it.id == mapaSelecionadoId }) {
            mapaSelecionadoId = alvo.firstOrNull()?.id
        }
    }

    /** Sem spinner de tela cheia — só atualiza os dados por baixo. */
    fun recarregarSilencioso() {
        aoCarregar(dataSelecionada) { resultado, falha ->
            if (resultado != null) aplicarMapas(resultado)
            if (falha != null) erro = falha
        }
    }

    fun atualizarStatusLocal(
        reservaId: Int,
        statusId: Int,
        parcial: Map<String, Int>?,
    ) {
        val nome = when (statusId) {
            STATUS_CHECK_IN -> "Check-in"
            STATUS_NO_SHOW -> "No show"
            STATUS_PARCIAL -> "Parcial"
            STATUS_RESERVADO -> "Reservado"
            else -> null
        }
        val parcialNovo = parcial?.let {
            ParcialEmbarque(
                adulto = it["adulto"] ?: 0,
                chd = it["chd"] ?: 0,
                infantil = it["infantil"] ?: 0,
                jovem = it["jovem"] ?: 0,
                idoso = it["idoso"] ?: 0,
            )
        }
        mapas = mapas?.map { mapa ->
            mapa.copy(
                reservas = mapa.reservas.map { r ->
                    if (r.id != reservaId) r
                    else r.copy(
                        status = StatusReserva(
                            id = statusId,
                            nome = nome ?: r.status?.nome,
                            cor = r.status?.cor,
                        ),
                        parcial = parcialNovo ?: r.parcial,
                    )
                },
            )
        }
    }

    LaunchedEffect(dataSelecionada, recarregar) {
        carregando = true
        erro = null
        aoCarregar(dataSelecionada) { resultado, falha ->
            aplicarMapas(resultado)
            erro = falha
            carregando = false
        }
    }

    val todos = mapas.orEmpty()
    val emAndamento = remember(todos) { todos.filter { !it.bloqueado } }
    val finalizados = remember(todos) { todos.filter { it.bloqueado } }
    val daAba = if (abaFinalizados) finalizados else emAndamento
    val mapaAtual = daAba.firstOrNull { it.id == mapaSelecionadoId } ?: daAba.firstOrNull()

    val (paxPresentes, paxReservados) = remember(mapaAtual) {
        val reservas = mapaAtual?.reservas.orEmpty()
        val reservados = reservas.sumOf { it.adt + it.chd + it.inf + it.jovem + it.idoso }
        val presentes = reservas.sumOf { r -> paxPresentesDaReserva(r) }
        presentes to reservados
    }

    fun recarregarOcorrencias() {
        val id = mapaAtual?.id ?: return
        aoCarregarOcorrencias(id) { lista, falha ->
            if (lista != null) ocorrencias = lista
            if (falha != null) erroOcorrencia = falha
        }
    }

    LaunchedEffect(mapaAtual?.id, recarregar) {
        val id = mapaAtual?.id ?: return@LaunchedEffect
        aoCarregarOcorrencias(id) { lista, _ ->
            ocorrencias = lista.orEmpty()
        }
    }

    if (mostrarCalendario) {
        val estadoData = rememberDatePickerState(
            initialSelectedDateMillis = millisUtcMapa(dataSelecionada),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoData.selectedDateMillis?.let { dataSelecionada = isoDeMillisMapa(it) }
                    mostrarCalendario = false
                }) { Text(stringResource(R.string.confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        ) { DatePicker(state = estadoData) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }

            erro != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(erro!!, style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { recarregar++ }) {
                    Text(stringResource(R.string.embarque_recarregar))
                }
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.mapa_embarque_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildString {
                            append(stringResource(R.string.mapa_embarque_guia))
                            append(' ')
                            append(nomeGuia.ifBlank { semNome })
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .clip(FormaBotaoPequeno)
                            .border(1.dp, MaterialTheme.colorScheme.outline, FormaBotaoPequeno)
                            .clickable { mostrarCalendario = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(dataExibicao, style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                LegendaAtrasoMapa(
                    paxPresentes = if (mapaAtual != null) paxPresentes else null,
                    paxReservados = if (mapaAtual != null) paxReservados else null,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AbaChip(
                        texto = stringResource(R.string.mapa_embarque_em_andamento, emAndamento.size),
                        ativa = !abaFinalizados,
                        aoClicar = {
                            abaFinalizados = false
                            mapaSelecionadoId = emAndamento.firstOrNull()?.id
                        },
                        modifier = Modifier.weight(1f),
                    )
                    AbaChip(
                        texto = stringResource(R.string.mapa_embarque_finalizados, finalizados.size),
                        ativa = abaFinalizados,
                        aoClicar = {
                            abaFinalizados = true
                            mapaSelecionadoId = finalizados.firstOrNull()?.id
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                if (daAba.isEmpty()) {
                    Cartao {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(R.string.mapa_embarque_sem_mapa),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        daAba.forEach { m ->
                            val pax = m.totais?.pax ?: m.ocupacao?.pax ?: m.reservas.sumOf { it.total ?: 0 }
                            val hora = m.reservas.mapNotNull { it.hora }.minOrNull()?.take(5) ?: "--:--"
                            val ativo = m.id == mapaAtual?.id
                            // Igual ao web: borda primary + fundo primary/10 + texto primary.
                            val formaChip = RoundedCornerShape(8.dp)
                            val corBorda = if (ativo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                            val corFundo = if (ativo) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.surface
                            val corTexto = if (ativo) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                            val corSecundaria = if (ativo) MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                            Row(
                                modifier = Modifier
                                    .clip(formaChip)
                                    .background(corFundo)
                                    .border(1.dp, corBorda, formaChip)
                                    .clickable { mapaSelecionadoId = m.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    m.tour.orEmpty().ifBlank { "Passeio" },
                                    color = corTexto,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Medium,
                                )
                                if (m.eh_apoio) {
                                    Text(
                                        stringResource(R.string.mapa_embarque_apoio),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEEE0FF))
                                            .border(1.dp, Color(0xFFC4B5FD), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6D28D9),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Text(
                                    "· $hora · $pax pax",
                                    color = corSecundaria,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }

                    mapaAtual?.let { mapa ->
                        if (mapa.bloqueado) {
                            Text(
                                stringResource(R.string.mapa_embarque_acerto),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        SecaoExpansivel(
                            titulo = "${stringResource(R.string.mapa_embarque_tour)}: ${mapa.tour.orEmpty()}",
                            aberta = secaoAbertaId == SecaoMapaIds.TOUR,
                            aoAlternar = {
                                secaoAbertaId =
                                    if (secaoAbertaId == SecaoMapaIds.TOUR) null else SecaoMapaIds.TOUR
                            },
                        ) { SecaoTour(mapa) }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_fornecedores),
                            aberta = secaoAbertaId == SecaoMapaIds.FORNECEDORES,
                            aoAlternar = {
                                secaoAbertaId =
                                    if (secaoAbertaId == SecaoMapaIds.FORNECEDORES) null
                                    else SecaoMapaIds.FORNECEDORES
                            },
                        ) {
                            SecaoFornecedores(
                                mapa = mapa,
                                aoInformarPagamento = { editandoFornecedor = it },
                            )
                        }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_embarques),
                            aberta = secaoAbertaId == SecaoMapaIds.EMBARQUES,
                            aoAlternar = {
                                secaoAbertaId =
                                    if (secaoAbertaId == SecaoMapaIds.EMBARQUES) null
                                    else SecaoMapaIds.EMBARQUES
                            },
                        ) {
                            if (mapa.reservas.isEmpty()) {
                                Text(
                                    stringResource(R.string.mapa_embarque_sem_embarque),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    mapa.reservas.sortedBy { it.hora ?: "" }.forEach { reserva ->
                                        key(reserva.id) {
                                            CartaoEmbarque(
                                                reserva = reserva,
                                                dataMapa = dataSelecionada,
                                                bloqueado = mapa.bloqueado,
                                                enviando = enviando == reserva.id,
                                                aoEditar = { editando = mapa to reserva },
                                                aoTrocarStatus = { statusId, motivoId, parcial, aoTerminar ->
                                                    enviando = reserva.id
                                                    aoTrocarStatus(
                                                        reserva.id,
                                                        mapa.id,
                                                        statusId,
                                                        motivoId,
                                                        parcial,
                                                    ) { falha ->
                                                        enviando = null
                                                        aoTerminar(falha)
                                                        if (falha == null) {
                                                            atualizarStatusLocal(reserva.id, statusId, parcial)
                                                            recarregarSilencioso()
                                                        }
                                                    }
                                                },
                                                aoCarregarMotivos = aoCarregarMotivos,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_ocorrencias),
                            aberta = secaoAbertaId == SecaoMapaIds.OCORRENCIAS,
                            aoAlternar = {
                                secaoAbertaId =
                                    if (secaoAbertaId == SecaoMapaIds.OCORRENCIAS) null
                                    else SecaoMapaIds.OCORRENCIAS
                            },
                        ) {
                            SecaoOcorrencias(
                                ocorrencias = ocorrencias,
                                bloqueado = mapa.bloqueado,
                                relatoNovo = relatoNovo,
                                salvando = salvandoOcorrencia,
                                editandoId = editandoOcorrenciaId,
                                relatoEdicao = relatoEdicao,
                                salvandoEdicao = salvandoEdicao,
                                erro = erroOcorrencia,
                                meuUsuId = meuUsuId,
                                aoMudarRelato = { relatoNovo = it },
                                aoSalvar = {
                                    val texto = relatoNovo.trim()
                                    if (texto.isEmpty() || salvandoOcorrencia) return@SecaoOcorrencias
                                    salvandoOcorrencia = true
                                    erroOcorrencia = null
                                    aoCriarOcorrencia(mapa.id, texto) { falha ->
                                        salvandoOcorrencia = false
                                        if (falha == null) {
                                            relatoNovo = ""
                                            recarregarOcorrencias()
                                        } else {
                                            erroOcorrencia = falha
                                        }
                                    }
                                },
                                aoIniciarEdicao = { o ->
                                    editandoOcorrenciaId = o.id
                                    relatoEdicao = o.relato.orEmpty()
                                },
                                aoCancelarEdicao = {
                                    editandoOcorrenciaId = null
                                    relatoEdicao = ""
                                },
                                aoMudarEdicao = { relatoEdicao = it },
                                aoSalvarEdicao = { id ->
                                    val texto = relatoEdicao.trim()
                                    if (texto.isEmpty() || salvandoEdicao) return@SecaoOcorrencias
                                    salvandoEdicao = true
                                    erroOcorrencia = null
                                    aoEditarOcorrencia(id, texto) { falha ->
                                        salvandoEdicao = false
                                        if (falha == null) {
                                            editandoOcorrenciaId = null
                                            relatoEdicao = ""
                                            recarregarOcorrencias()
                                        } else {
                                            erroOcorrencia = falha
                                        }
                                    }
                                },
                                aoPedirExcluir = { excluirOcorrenciaId = it },
                            )
                        }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_informacoes),
                            aberta = secaoAbertaId == SecaoMapaIds.INFORMACOES,
                            aoAlternar = {
                                secaoAbertaId =
                                    if (secaoAbertaId == SecaoMapaIds.INFORMACOES) null
                                    else SecaoMapaIds.INFORMACOES
                            },
                        ) {
                            // API ainda não envia info_importante (igual ao web).
                            Text(
                                stringResource(R.string.mapa_embarque_sem_info),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        editando?.let { (mapa, reserva) ->
            val atual = mapa.reservas.firstOrNull { it.id == reserva.id } ?: reserva
            DialogCheckInEmbarque(
                reserva = atual,
                nomeTour = mapa.tour,
                bloqueado = mapa.bloqueado,
                enviando = enviando == atual.id,
                acoes = AcoesCheckIn(
                    carregarMotivos = aoCarregarMotivos,
                    carregarFormas = aoCarregarFormas,
                    carregarParcelas = aoCarregarParcelas,
                    carregarIdiomas = aoCarregarIdiomas,
                    trocarStatus = { statusId, motivoId, parcial, aoTerminar ->
                        enviando = atual.id
                        aoTrocarStatus(atual.id, mapa.id, statusId, motivoId, parcial) { falha ->
                            enviando = null
                            aoTerminar(falha)
                        }
                    },
                    salvarPagamentos = { itens, aoTerminar ->
                        aoSalvarPagamentos(atual.id, itens, aoTerminar)
                    },
                    salvarIdioma = { idiomaId, aoTerminar ->
                        aoSalvarIdioma(atual.id, idiomaId, aoTerminar)
                    },
                ),
                aoFechar = { if (enviando != atual.id) editando = null },
                aoRecarregar = { recarregarSilencioso() },
            )
        }

        excluirOcorrenciaId?.let { idExcluir ->
            AlertDialog(
                onDismissRequest = { excluirOcorrenciaId = null },
                title = { Text(stringResource(R.string.mapa_embarque_excluir_titulo)) },
                text = { Text(stringResource(R.string.mapa_embarque_excluir_texto)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            aoExcluirOcorrencia(idExcluir) { falha ->
                                if (falha == null) {
                                    excluirOcorrenciaId = null
                                    if (editandoOcorrenciaId == idExcluir) {
                                        editandoOcorrenciaId = null
                                        relatoEdicao = ""
                                    }
                                    recarregarOcorrencias()
                                } else {
                                    erroOcorrencia = falha
                                    excluirOcorrenciaId = null
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.confirmar)) }
                },
                dismissButton = {
                    TextButton(onClick = { excluirOcorrenciaId = null }) {
                        Text(stringResource(R.string.cancelar))
                    }
                },
            )
        }

        editandoFornecedor?.let { forn ->
            val acertoId = forn.acertoFornecedorId ?: return@let
            DialogPagamentoFornecedor(
                empresa = forn.empresa,
                total = forn.total ?: 0.0,
                pagamentosIniciais = forn.pagamentos,
                aoCarregarFormas = aoCarregarFormas,
                aoCarregarParcelas = aoCarregarParcelas,
                aoSalvar = { itens, aoTerminar ->
                    aoSalvarPagamentoFornecedor(acertoId, itens, aoTerminar)
                },
                aoFechar = { editandoFornecedor = null },
                aoRecarregar = { recarregarSilencioso() },
            )
        }
    }
}

/** Espelho do `OcorrenciasSection.tsx` do web. */
@Composable
private fun SecaoOcorrencias(
    ocorrencias: List<OcorrenciaMapa>,
    bloqueado: Boolean,
    relatoNovo: String,
    salvando: Boolean,
    editandoId: Int?,
    relatoEdicao: String,
    salvandoEdicao: Boolean,
    erro: String?,
    meuUsuId: Int?,
    aoMudarRelato: (String) -> Unit,
    aoSalvar: () -> Unit,
    aoIniciarEdicao: (OcorrenciaMapa) -> Unit,
    aoCancelarEdicao: () -> Unit,
    aoMudarEdicao: (String) -> Unit,
    aoSalvarEdicao: (Int) -> Unit,
    aoPedirExcluir: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!bloqueado) {
            OutlinedTextField(
                value = relatoNovo,
                onValueChange = aoMudarRelato,
                placeholder = { Text(stringResource(R.string.mapa_embarque_relato_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                enabled = !salvando,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = aoSalvar,
                    enabled = relatoNovo.isNotBlank() && !salvando,
                ) {
                    if (salvando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.mapa_embarque_salvando_relato))
                    } else {
                        Text(stringResource(R.string.mapa_embarque_salvar_relato))
                    }
                }
            }
        }

        erro?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (ocorrencias.isEmpty()) {
            Text(
                stringResource(R.string.mapa_embarque_sem_ocorrencia),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ocorrencias.forEach { o ->
                    val resolvida = o.status.equals("RESOLVIDA", ignoreCase = true)
                    val podeEditar = !bloqueado && !resolvida &&
                        meuUsuId != null && o.usuario_id == meuUsuId
                    val editando = editandoId == o.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val criadoFmt = formatarDataOcorrencia(o.data_adicionado)
                            val alteradoFmt = formatarDataOcorrencia(o.data_alterado)
                            val criado = parseDataOcorrencia(o.data_adicionado)
                            val alterado = parseDataOcorrencia(o.data_alterado)
                            val editadoSufixo = if (
                                criado != null && alterado != null &&
                                alterado.time - criado.time > 60_000 &&
                                alteradoFmt != null
                            ) {
                                stringResource(R.string.mapa_embarque_editado_em, alteradoFmt)
                            } else {
                                ""
                            }
                            val resolvidaSufixo = if (resolvida) {
                                " · ${stringResource(R.string.mapa_embarque_resolvida)}"
                            } else {
                                ""
                            }
                            Text(
                                buildString {
                                    append(o.autor_nome?.takeIf { it.isNotBlank() } ?: "—")
                                    if (criadoFmt != null) {
                                        append(" · ")
                                        append(criadoFmt)
                                    }
                                    append(editadoSufixo)
                                    append(resolvidaSufixo)
                                },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (resolvida) Color(0xFF059669)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (podeEditar) {
                                if (editando) {
                                    IconButton(
                                        onClick = { aoSalvarEdicao(o.id) },
                                        enabled = relatoEdicao.isNotBlank() && !salvandoEdicao,
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        if (salvandoEdicao) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = stringResource(R.string.confirmar),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = aoCancelarEdicao,
                                        enabled = !salvandoEdicao,
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.cancelar),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { aoIniciarEdicao(o) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.mapa_embarque_editar_ocorrencia),
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { aoPedirExcluir(o.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.mapa_embarque_excluir_ocorrencia),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                        if (editando) {
                            OutlinedTextField(
                                value = relatoEdicao,
                                onValueChange = aoMudarEdicao,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                minLines = 4,
                                enabled = !salvandoEdicao,
                            )
                        } else {
                            Text(
                                o.relato.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseDataOcorrencia(valor: String?): Date? {
    if (valor.isNullOrBlank()) return null
    val bruto = valor.trim()
    val candidatos = listOf(bruto, bruto.replace(' ', 'T'))
    val formatos = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd",
    )
    for (texto in candidatos) {
        for (padrao in formatos) {
            val data = runCatching {
                SimpleDateFormat(padrao, Locale.US).apply { isLenient = true }.parse(texto)
            }.getOrNull()
            if (data != null) return data
        }
    }
    return null
}

private fun formatarDataOcorrencia(valor: String?): String? {
    val data = parseDataOcorrencia(valor) ?: return null
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(data)
}

/** Igual a `presentes()` / `reservados()` do web (paxDaReserva.ts). */
private data class PaxPorCategoria(
    val adt: Int = 0,
    val chd: Int = 0,
    val inf: Int = 0,
    val jovem: Int = 0,
    val idoso: Int = 0,
) {
    val total: Int get() = adt + chd + inf + jovem + idoso
}

private fun paxReservadosDaReserva(r: ReservaEmbarque): PaxPorCategoria =
    PaxPorCategoria(r.adt, r.chd, r.inf, r.jovem, r.idoso)

private fun paxPresentesCategorias(r: ReservaEmbarque): PaxPorCategoria {
    val statusId = r.status?.id
    if (statusId == STATUS_NO_SHOW) return PaxPorCategoria()
    val total = paxReservadosDaReserva(r)
    if (statusId != STATUS_PARCIAL) return total
    val p = r.parcial
    return PaxPorCategoria(
        adt = maxOf(0, r.adt - (p?.adulto ?: 0)),
        chd = maxOf(0, r.chd - (p?.chd ?: 0)),
        inf = maxOf(0, r.inf - (p?.infantil ?: 0)),
        jovem = maxOf(0, r.jovem - (p?.jovem ?: 0)),
        idoso = maxOf(0, r.idoso - (p?.idoso ?: 0)),
    )
}

/** Igual a `presentes()` do web (paxDaReserva.ts), usando ids de status da API. */
private fun paxPresentesDaReserva(r: ReservaEmbarque): Int = paxPresentesCategorias(r).total

private fun acumularPax(lista: List<PaxPorCategoria>): PaxPorCategoria {
    var adt = 0
    var chd = 0
    var inf = 0
    var jovem = 0
    var idoso = 0
    lista.forEach {
        adt += it.adt
        chd += it.chd
        inf += it.inf
        jovem += it.jovem
        idoso += it.idoso
    }
    return PaxPorCategoria(adt, chd, inf, jovem, idoso)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendaAtrasoMapa(
    paxPresentes: Int? = null,
    paxReservados: Int? = null,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                text = stringResource(R.string.mapa_embarque_legenda_atraso),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChipAtraso(
            texto = stringResource(R.string.embarque_atraso_ate_15),
            bolinha = Color(0xFF3B82F6),
            fundo = Color(0xFFDBEAFE),
            textoCor = Color(0xFF1E40AF),
            borda = Color(0xFF93C5FD),
        )
        ChipAtraso(
            texto = stringResource(R.string.embarque_atraso_acima_15),
            bolinha = Color(0xFFEF4444),
            fundo = Color(0xFFFEE2E2),
            textoCor = Color(0xFFB91C1C),
            borda = Color(0xFFFCA5A5),
        )
        if (paxPresentes != null && paxReservados != null) {
            Text(
                stringResource(R.string.mapa_embarque_pax, paxPresentes, paxReservados),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFDCFCE7))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color(0xFF15803D),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ChipAtraso(
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
private fun SecaoTour(mapa: MapaEmbarque) {
    val reservas = mapa.reservas
    val paxPresentes = remember(reservas) {
        acumularPax(reservas.map { paxPresentesCategorias(it) })
    }
    val paxReservados = remember(reservas) {
        acumularPax(reservas.map { paxReservadosDaReserva(it) })
    }
    val totalPresentes = paxPresentes.total
    val totalReservados = paxReservados.total
    // Infantil não ocupa lugar (regra do web / cliente).
    val capacidadeVeiculo = mapa.veiculo?.capacidade?.takeIf { it > 0 }
    val lugaresOcupados = totalPresentes - paxPresentes.inf
    val vagasLivres = capacidadeVeiculo?.let { it - lugaresOcupados }

    val saida = reservas.mapNotNull { it.hora }.minOrNull()?.take(5) ?: "--:--"
    // Igual ao web (MapaEmbarque.tsx): a_receber já inclui taxas de cais;
    // só se soma a taxa de cartão dos pagamentos lançados (taxaCartaoLancada).
    val totalAReceber = reservas.sumOf { r ->
        r.a_receber + r.pagamentos.sumOf { it.taxa }
    }
    val porForma = mutableMapOf<String, Double>()
    var totalRecebido = 0.0
    reservas.forEach { r ->
        r.pagamentos.forEach { p ->
            // Valor na máquina = base + taxa, como rotuloDoPagamento no web.
            val naMaquina = p.valor + p.taxa
            val nome = listOfNotNull(p.tipo, p.forma).joinToString(" - ").ifBlank { "Pagamento" }
            porForma[nome] = (porForma[nome] ?: 0.0) + naMaquina
            totalRecebido += naMaquina
        }
    }
    val saldo = round2(totalAReceber - totalRecebido)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                buildString {
                    append(stringResource(R.string.mapa_embarque_ocupacao))
                    append(' ')
                    append(totalPresentes)
                    if (totalPresentes != totalReservados) {
                        append(stringResource(R.string.mapa_embarque_ocupacao_de, totalReservados))
                    }
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        vagasLivres?.let { livres ->
            val capacidade = capacidadeVeiculo!!
            val (fundo, textoCor, borda) = when {
                livres < 0 -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFFFCA5A5))
                livres == 0 -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), Color(0xFFFCD34D))
                else -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF86EFAC))
            }
            val rotulo = when {
                livres < 0 -> stringResource(
                    R.string.mapa_embarque_capacidade_veiculo_acima,
                    capacidade,
                    -livres,
                )
                livres == 0 -> stringResource(
                    R.string.mapa_embarque_capacidade_veiculo_lotado,
                    capacidade,
                )
                livres == 1 -> stringResource(
                    R.string.mapa_embarque_capacidade_veiculo_livre,
                    capacidade,
                    livres,
                )
                else -> stringResource(
                    R.string.mapa_embarque_capacidade_veiculo_livres,
                    capacidade,
                    livres,
                )
            }
            Text(
                text = rotulo.uppercase(Locale.getDefault()),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, borda, RoundedCornerShape(4.dp))
                    .background(fundo)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = textoCor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinhaCyanComReferencia(
                stringResource(R.string.mapa_embarque_adultos),
                paxPresentes.adt,
                paxReservados.adt,
            )
            LinhaCyanComReferencia(
                stringResource(R.string.mapa_embarque_chd),
                paxPresentes.chd,
                paxReservados.chd,
            )
            LinhaCyanComReferencia(
                stringResource(R.string.mapa_embarque_inf),
                paxPresentes.inf,
                paxReservados.inf,
            )
        }
        if (paxPresentes.jovem > 0 || paxPresentes.idoso > 0 ||
            paxReservados.jovem > 0 || paxReservados.idoso > 0
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaCyanComReferencia(
                    stringResource(R.string.mapa_embarque_jovem),
                    paxPresentes.jovem,
                    paxReservados.jovem,
                )
                LinhaCyanComReferencia(
                    stringResource(R.string.mapa_embarque_idoso),
                    paxPresentes.idoso,
                    paxReservados.idoso,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LinhaCyan(stringResource(R.string.mapa_embarque_saida), saida)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Spacer(Modifier.height(6.dp))
        LinhaCyan(stringResource(R.string.mapa_embarque_total_receber), dinheiro(totalAReceber))
        if (porForma.isNotEmpty()) {
            LinhaCyan(stringResource(R.string.mapa_embarque_total_recebido), dinheiro(totalRecebido))
            porForma.forEach { (nome, valor) ->
                Text(
                    "$nome: ${dinheiro(valor)}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        LinhaCyan(
            stringResource(R.string.mapa_embarque_saldo_receber),
            dinheiro(saldo),
            destaque = saldo > 0.009,
        )
        mapa.caixa?.let { caixa ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinhaCyan(stringResource(R.string.mapa_embarque_saldo_cash), dinheiro(caixa.saldo))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.mapa_embarque_saldo_cash_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SecaoFornecedores(
    mapa: MapaEmbarque,
    aoInformarPagamento: (FornecedorUi) -> Unit,
) {
    // Mesma montagem do web (`paraFornecedores`): transporte do mapa + demais.
    val fornecedoresUi = remember(mapa) { fornecedoresDoMapa(mapa) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (fornecedoresUi.isEmpty()) {
            Text(
                stringResource(R.string.mapa_embarque_sem_fornecedor),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            fornecedoresUi.forEach { f ->
                BlocoFornecedor(
                    f = f,
                    bloqueado = mapa.bloqueado,
                    aoInformarPagamento = { aoInformarPagamento(f) },
                )
            }
        }

        // Rodapé igual ao web (`MapaEmbarque.tsx`): TOTAL PAGO / SALDO vêm de
        // `payment_method` via parsePaymentMethod. No adapter (`paraFornecedores`)
        // esse campo é sempre null — o pago real mora em `pagamentos` e só
        // aparece no card ("Ver pagamentos · pago …"). Espelhar o web aqui.
        val totalAPagar = fornecedoresUi.sumOf { it.total ?: 0.0 }
        val totalPago = 0.0
        val saldo = round2(totalAPagar - totalPago)
        if (fornecedoresUi.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LinhaCyan(stringResource(R.string.mapa_embarque_valor_pagar), dinheiro(totalAPagar))
                LinhaCyan(
                    stringResource(R.string.mapa_embarque_saldo_pagar),
                    dinheiro(saldo),
                    destaque = saldo > 0.009,
                )
            }
        }
    }
}

/**
 * Espelho de `paraFornecedores` no web: 1º o transporte do mapa, depois a lista.
 */
private fun fornecedoresDoMapa(mapa: MapaEmbarque): List<FornecedorUi> {
    val temTransporte = !mapa.fornecedor.isNullOrBlank() ||
        mapa.motorista?.nome != null ||
        mapa.veiculo?.modelo != null
    val demais = mapa.fornecedores.mapIndexed { i, f ->
        FornecedorUi(
            id = "forn-$i",
            ehTransporte = false,
            empresa = f.nome ?: "—",
            atividade = f.atividade,
            contato = null,
            motorista = null,
            telefone = null,
            carroModelo = null,
            capacidade = null,
            placa = null,
            pax = f.pax,
            total = f.valor,
            observacao = f.descricao,
            acertoFornecedorId = f.acerto_fornecedor_id,
            permitePagamento = f.permite_pagamento,
            pagamentos = f.pagamentos,
        )
    }
    if (!temTransporte) return demais

    val transporte = mapa.transporte_despesa
    val totalTransporte = when {
        transporte != null -> transporte.valor
        (mapa.totais?.a_pagar ?: 0.0) > 0 -> mapa.totais!!.a_pagar
        else -> null
    }
    val carro = listOfNotNull(mapa.veiculo?.tipo, mapa.veiculo?.modelo)
        .joinToString(" · ")
        .ifBlank { null }

    val linhaTransporte = FornecedorUi(
        id = "transporte",
        ehTransporte = true,
        empresa = mapa.fornecedor ?: "—",
        atividade = mapa.tour,
        contato = mapa.motorista?.nome,
        motorista = mapa.motorista?.nome,
        telefone = mapa.motorista?.telefone,
        carroModelo = carro,
        capacidade = mapa.veiculo?.capacidade?.toString(),
        placa = mapa.veiculo?.placa,
        pax = mapa.totais?.pax,
        total = totalTransporte,
        observacao = transporte?.descricao,
        acertoFornecedorId = transporte?.acerto_fornecedor_id?.takeIf { it > 0 },
        permitePagamento = transporte?.permite_pagamento != false && transporte != null,
        pagamentos = transporte?.pagamentos.orEmpty(),
    )
    return listOf(linhaTransporte) + demais
}

private data class FornecedorUi(
    val id: String,
    val ehTransporte: Boolean,
    val empresa: String,
    val atividade: String?,
    val contato: String?,
    val motorista: String?,
    val telefone: String?,
    val carroModelo: String?,
    val capacidade: String?,
    val placa: String?,
    val pax: Int?,
    val total: Double?,
    val observacao: String?,
    val acertoFornecedorId: Int?,
    val permitePagamento: Boolean,
    val pagamentos: List<PagamentoFornecedor>,
)

@Composable
private fun BlocoFornecedor(
    f: FornecedorUi,
    bloqueado: Boolean,
    aoInformarPagamento: () -> Unit,
) {
    val podeLancar = f.acertoFornecedorId != null && f.permitePagamento && !bloqueado
    val pago = f.pagamentos.sumOf { it.valor }
    val total = f.total ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LinhaCampo(stringResource(R.string.mapa_embarque_empresa), f.empresa)
        if (!f.ehTransporte) {
            f.atividade?.takeIf { it.isNotBlank() }?.let {
                LinhaCampo(stringResource(R.string.mapa_embarque_atividade), it)
            }
        }
        f.contato?.takeIf { it.isNotBlank() }?.let {
            LinhaCampo(stringResource(R.string.mapa_embarque_contato), it)
        }
        if (f.motorista != null) {
            LinhaCampo(
                stringResource(R.string.mapa_embarque_motorista_rotulo),
                f.motorista,
            )
        }
        if (f.telefone != null) {
            LinhaCampo(
                stringResource(R.string.mapa_embarque_telefone_rotulo),
                f.telefone,
            )
        }
        if (f.carroModelo != null) {
            LinhaCampo(
                stringResource(R.string.mapa_embarque_carro_rotulo),
                f.carroModelo,
            )
        }
        if (f.capacidade != null) {
            LinhaCampo(stringResource(R.string.mapa_embarque_capacidade), f.capacidade)
        }
        f.placa?.takeIf { it.isNotBlank() }?.let {
            LinhaCampo(stringResource(R.string.mapa_embarque_placa), it)
        }
        f.pax?.let {
            LinhaCampo(stringResource(R.string.mapa_embarque_qtd_pax), it.toString())
        }
        if (f.total != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (podeLancar) Modifier.clickable(onClick = aoInformarPagamento)
                        else Modifier,
                    ),
                text = buildString {
                    append(stringResource(R.string.mapa_embarque_total))
                    append(' ')
                    append(dinheiro(f.total))
                },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        if (podeLancar) {
            val falta = round2(total - pago)
            Text(
                text = when {
                    pago > 0 && falta > 0.005 -> stringResource(
                        R.string.mapa_embarque_ver_pagamentos_falta,
                        dinheiro(pago),
                        dinheiro(falta),
                    )
                    pago > 0 -> stringResource(R.string.mapa_embarque_ver_pagamentos, dinheiro(pago))
                    else -> stringResource(R.string.mapa_embarque_informar_pagamento)
                },
                modifier = Modifier.clickable(onClick = aoInformarPagamento),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        // Igual ao web (`s.observations`): descrição abaixo do pagamento.
        // No acerto a API manda atividade === descricao; não filtrar isso,
        // senão some o texto que o guia lê embaixo de "Informar pagamento".
        f.observacao?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LinhaCampo(
    rotulo: String,
    valor: String,
    valorSublinhado: Boolean = false,
) {
    Row {
        Text(
            rotulo,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            valor,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textDecoration = if (valorSublinhado) TextDecoration.Underline else null,
        )
    }
}

private data class OpcaoStatusEmbarque(
    val id: Int,
    val rotulo: String,
    val permitido: Boolean,
    val corAtiva: Color,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoEmbarque(
    reserva: ReservaEmbarque,
    dataMapa: String,
    bloqueado: Boolean,
    enviando: Boolean,
    aoEditar: () -> Unit,
    aoTrocarStatus: (
        statusId: Int,
        motivoId: Int?,
        parcial: Map<String, Int>?,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
    aoCarregarMotivos: ((RespostaMotivos?, String?) -> Unit) -> Unit,
) {
    var aberto by remember(reserva.id) { mutableStateOf(false) }
    var dialogoMotivo by remember { mutableStateOf(false) }
    var dialogoParcial by remember { mutableStateOf(false) }
    var motivos by remember { mutableStateOf<RespostaMotivos?>(null) }
    var carregandoMotivos by remember { mutableStateOf(false) }
    var categoriaId by remember { mutableStateOf<Int?>(null) }
    var motivoId by remember { mutableStateOf<Int?>(null) }
    var adulto by remember { mutableStateOf("0") }
    var chd by remember { mutableStateOf("0") }
    var infantil by remember { mutableStateOf("0") }
    var jovem by remember { mutableStateOf("0") }
    var idoso by remember { mutableStateOf("0") }
    var statusLocal by remember(reserva.id) { mutableStateOf(reserva.status?.id) }
    var erroStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reserva.status?.id) {
        statusLocal = reserva.status?.id
    }

    LaunchedEffect(dialogoMotivo) {
        if (dialogoMotivo && motivos == null && !carregandoMotivos) {
            carregandoMotivos = true
            aoCarregarMotivos { lista, falha ->
                carregandoMotivos = false
                if (falha != null) erroStatus = falha else motivos = lista
            }
        }
    }

    val contexto = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val situacao = situacaoFinanceira(reserva.a_receber, reserva.pagamentos.sumOf { it.valor })
    val (fundo, borda) = when (situacao) {
        SituacaoFinanceira.RECEBIDO -> Color(0xFFDCFCE7) to Color(0xFF86EFAC)
        SituacaoFinanceira.PARCIAL -> Color(0xFFFEF3C7) to Color(0xFFFCD34D)
        SituacaoFinanceira.ABERTO -> Color(0xFFFEE2E2) to Color(0xFFFCA5A5)
    }
    val (seloFundo, seloTexto, seloBorda) = coresSeloStatus(statusLocal)
    val hotel = reserva.embarque?.takeIf { it.isNotBlank() }
        ?: reserva.endereco?.takeIf { it.isNotBlank() }
        ?: "—"
    val enderecoExtra = reserva.endereco?.takeIf {
        it.isNotBlank() && !reserva.embarque.isNullOrBlank() && it != reserva.embarque
    }
    val hora = reserva.hora?.take(5) ?: "—"
    val opcoesStatus = listOf(
        OpcaoStatusEmbarque(STATUS_CHECK_IN, "Check-in", true, Color(0xFF16A34A)),
        OpcaoStatusEmbarque(STATUS_RESERVADO, "Reservado", false, Color(0xFF3B82F6)),
        OpcaoStatusEmbarque(STATUS_NO_SHOW, "No show", true, Color(0xFFDA4553)),
        OpcaoStatusEmbarque(STATUS_PARCIAL, "Parcial", true, Color(0xFF3B82F6)),
    )
    val opcaoAtual = opcoesStatus.firstOrNull { it.id == statusLocal }
        ?: OpcaoStatusEmbarque(
            statusLocal ?: 0,
            reserva.status?.nome ?: "Status",
            false,
            Color(0xFF94A3B8),
        )
    val statusNome = opcaoAtual.rotulo.uppercase(Locale.getDefault())
    val ehParcial = statusLocal == STATUS_PARCIAL
    val qtdAdt = if (ehParcial) reserva.parcial?.adulto ?: 0 else reserva.adt
    val qtdChd = if (ehParcial) reserva.parcial?.chd ?: 0 else reserva.chd
    val qtdInf = if (ehParcial) reserva.parcial?.infantil ?: 0 else reserva.inf
    val bandeira = bandeiraIdioma(reserva.idioma)
    val totalCobrar = reserva.a_receber + reserva.pagamentos.sumOf { it.taxa }
    val taxasCadastro = reserva.taxas
    val taxasNoSaldo = minOf(taxasCadastro, totalCobrar)
    val passeioNoSaldo = maxOf(0.0, totalCobrar - taxasNoSaldo)
    val mostrarBreakdown = taxasNoSaldo > 0.005

    val resolvido = statusLocal == STATUS_CHECK_IN
        || statusLocal == STATUS_NO_SHOW
        || statusLocal == STATUS_PARCIAL
    val referenciaAtraso = parseCheckedInAtMs(reserva.checked_in_at)
    val atraso = if (resolvido && referenciaAtraso != null) {
        calcularAtrasoEmbarque(dataMapa, reserva.hora, referenciaAtraso)
    } else {
        null
    }
    val checkInQuando = formatarCheckInEm(reserva.checked_in_at)
    val temContato = !reserva.telefone.isNullOrBlank()
        || !reserva.observacao.isNullOrBlank()
        || statusLocal == STATUS_CHECK_IN

    fun gravarStatus(statusId: Int, motivo: Int? = null, parcial: Map<String, Int>? = null) {
        erroStatus = null
        aoTrocarStatus(statusId, motivo, parcial) { falha ->
            if (falha == null) {
                statusLocal = statusId
                dialogoMotivo = false
                dialogoParcial = false
            } else {
                erroStatus = falha
            }
        }
    }

    fun escolherStatus(id: Int) {
        if (bloqueado || enviando || id == statusLocal) return
        when (id) {
            STATUS_CHECK_IN -> gravarStatus(STATUS_CHECK_IN)
            STATUS_NO_SHOW -> {
                categoriaId = null
                motivoId = null
                dialogoMotivo = true
            }
            STATUS_PARCIAL -> {
                adulto = (reserva.parcial?.adulto ?: 0).toString()
                chd = (reserva.parcial?.chd ?: 0).toString()
                infantil = (reserva.parcial?.infantil ?: 0).toString()
                jovem = (reserva.parcial?.jovem ?: 0).toString()
                idoso = (reserva.parcial?.idoso ?: 0).toString()
                dialogoParcial = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(fundo)
            .border(1.dp, borda, RoundedCornerShape(8.dp)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_grip_vertical),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp, top = 14.dp)
                    .size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { aberto = !aberto }
                .padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_map_pin),
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp),
                )
                // Nome quebra linha (break-words do web); horário fica fixo à direita.
                Text(
                    hotel,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                )
                Text(
                    hora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    softWrap = false,
                    maxLines = 1,
                )
            }
            enderecoExtra?.let { end ->
                Text(
                    end,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    softWrap = true,
                    overflow = TextOverflow.Visible,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                statusNome,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(seloFundo)
                    .border(1.dp, seloBorda, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = seloTexto,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            if (atraso != null && atraso.minutos > 0) {
                val (atrasoFundo, atrasoTexto, atrasoBorda) = if (atraso.grave) {
                    Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Color(0xFFFCA5A5))
                } else {
                    Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), Color(0xFF93C5FD))
                }
                Text(
                    atraso.rotulo,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(atrasoFundo)
                        .border(1.dp, atrasoBorda, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = atrasoTexto,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Mesma grade do web: minmax(0,1.2fr) | minmax(0,1fr) | auto
        // Fontes compactas (10/7/16sp) para caber como no web sem quebrar "IDIOMA".
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = if (temContato) 4.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CampoCaixaEmbarque(
                modifier = Modifier
                    .weight(1.2f)
                    .widthIn(min = 0.dp),
                rotulo = "Apto",
            ) {
                Text(
                    reserva.apto?.takeIf { it.isNotBlank() } ?: "—",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CampoCaixaEmbarque(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                rotulo = null,
                paddingHorizontal = 6.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                ) {
                    listOf(
                        "ADT" to qtdAdt,
                        "CHD" to qtdChd,
                        "INF" to qtdInf,
                    ).forEachIndexed { i, (rotulo, valor) ->
                        if (i > 0) {
                            Text(
                                "·",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                rotulo,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp,
                                lineHeight = 8.sp,
                                letterSpacing = 0.3.sp,
                            )
                            Text(
                                "$valor",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
            CampoCaixaEmbarque(
                modifier = Modifier.widthIn(min = 52.dp),
                rotulo = stringResource(R.string.mapa_embarque_idioma),
                centralizado = true,
                paddingHorizontal = 6.dp,
            ) {
                Text(
                    if (bandeira.isNotEmpty()) bandeira else "—",
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                )
            }
        }

        if (temContato) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                reserva.telefone?.takeIf { it.isNotBlank() }?.let { tel ->
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_phone),
                        contentDescription = stringResource(R.string.mapa_embarque_ligar),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                linkLigacao(tel)?.let { uri ->
                                    runCatching {
                                        contexto.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse(uri)),
                                        )
                                    }
                                }
                            }
                            .padding(6.dp),
                    )
                    Text(
                        tel,
                        modifier = Modifier.clickable {
                            linkWhatsapp(tel)?.let { uriHandler.openUri(it) }
                        },
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!reserva.observacao.isNullOrBlank()) {
                    Box {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_mail),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                        )
                    }
                }
                if (statusLocal == STATUS_CHECK_IN) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_check_circle_2),
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (aberto) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
                Row {
                    Text(
                        "ID ",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${reserva.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    reserva.voucher?.takeIf { it.isNotBlank() }?.let { v ->
                        Text(" · ", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Voucher: ",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
                reserva.pax?.takeIf { it.isNotBlank() }?.let {
                    LinhaCampo(stringResource(R.string.mapa_embarque_nome), it)
                }
                reserva.passeio?.takeIf { it.isNotBlank() }?.let {
                    LinhaCampo(stringResource(R.string.mapa_embarque_pacote), it)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !bloqueado) { aoEditar() }
                        .padding(vertical = 2.dp),
                ) {
                    Row {
                        Text(
                            stringResource(R.string.mapa_embarque_a_receber_rotulo),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            dinheiro(if (mostrarBreakdown) passeioNoSaldo else totalCobrar),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                        )
                        if (statusLocal == STATUS_NO_SHOW && totalCobrar > 0.005) {
                            Text(
                                " (taxa de no-show)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    if (mostrarBreakdown) {
                        Text(
                            "+ TAXAS: ${dinheiro(taxasNoSaldo)} = TOTAL: ${dinheiro(totalCobrar)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if ((reserva.cortesia ?: 0.0) > 0.005) {
                        Text(
                            "Cortesia",
                            color = Color(0xFF047857),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                reserva.observacao?.takeIf { it.isNotBlank() }?.let { obs ->
                    Row {
                        Text(
                            stringResource(R.string.mapa_embarque_obs),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            obs,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (statusLocal == STATUS_CHECK_IN && checkInQuando != null) {
                    Text(
                        stringResource(R.string.mapa_embarque_checkin_em, checkInQuando),
                        color = Color(0xFF16A34A),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                erroStatus?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    stringResource(R.string.mapa_embarque_alterar_status),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                SeletorStatusEmbarque(
                    atual = opcaoAtual,
                    opcoes = opcoesStatus.filter { it.permitido || it.id == statusLocal },
                    bloqueado = bloqueado || enviando,
                    carregando = enviando,
                    aoEscolher = ::escolherStatus,
                )
            }
            }
        }
    }
    }

    if (dialogoMotivo) {
        AlertDialog(
            onDismissRequest = { if (!enviando) dialogoMotivo = false },
            title = { Text(stringResource(R.string.checkin_motivo_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        carregandoMotivos -> CircularProgressIndicator(strokeWidth = 2.dp)
                        motivos == null -> Text(
                            stringResource(R.string.checkin_motivo_erro),
                            color = MaterialTheme.colorScheme.error,
                        )
                        else -> {
                            MenuSimplesEmbarque(
                                rotulo = motivos!!.categorias.firstOrNull { it.id == categoriaId }?.nome
                                    ?: stringResource(R.string.checkin_categoria),
                                opcoes = motivos!!.categorias.map { it.id to (it.nome ?: "#${it.id}") },
                                aoEscolher = { categoriaId = it; motivoId = null },
                            )
                            val filtrados = motivos!!.motivos.filter {
                                categoriaId == null || it.categoria_id == categoriaId
                            }
                            MenuSimplesEmbarque(
                                rotulo = filtrados.firstOrNull { it.mot_id == motivoId }?.mot_nome
                                    ?: stringResource(R.string.checkin_motivo),
                                opcoes = filtrados.map { it.mot_id to (it.mot_nome ?: "#${it.mot_id}") },
                                aoEscolher = { motivoId = it },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { motivoId?.let { gravarStatus(STATUS_NO_SHOW, motivo = it) } },
                    enabled = motivoId != null && !enviando,
                ) { Text(stringResource(R.string.confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoMotivo = false }, enabled = !enviando) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }

    if (dialogoParcial) {
        val invalido = listOf(
            (adulto.toIntOrNull() ?: 0) to reserva.adt,
            (chd.toIntOrNull() ?: 0) to reserva.chd,
            (infantil.toIntOrNull() ?: 0) to reserva.inf,
            (jovem.toIntOrNull() ?: 0) to reserva.jovem,
            (idoso.toIntOrNull() ?: 0) to reserva.idoso,
        ).any { it.first > it.second }

        AlertDialog(
            onDismissRequest = { if (!enviando) dialogoParcial = false },
            title = { Text(stringResource(R.string.checkin_parcial_titulo)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        Triple("ADT", reserva.adt, adulto to { v: String -> adulto = v }),
                        Triple("CHD", reserva.chd, chd to { v: String -> chd = v }),
                        Triple("INF", reserva.inf, infantil to { v: String -> infantil = v }),
                        Triple("Jovem", reserva.jovem, jovem to { v: String -> jovem = v }),
                        Triple("Idoso", reserva.idoso, idoso to { v: String -> idoso = v }),
                    ).filter { it.second > 0 }.forEach { (rotulo, reservado, campo) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rotulo, modifier = Modifier.weight(1f))
                            Text("$reservado", modifier = Modifier.padding(end = 8.dp))
                            OutlinedTextField(
                                value = campo.first,
                                onValueChange = campo.second,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(80.dp),
                            )
                        }
                    }
                    if (invalido) {
                        Text(
                            stringResource(R.string.checkin_parcial_invalido),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !invalido && !enviando,
                    onClick = {
                        gravarStatus(
                            STATUS_PARCIAL,
                            parcial = mapOf(
                                "adulto" to (adulto.toIntOrNull() ?: 0),
                                "chd" to (chd.toIntOrNull() ?: 0),
                                "infantil" to (infantil.toIntOrNull() ?: 0),
                                "jovem" to (jovem.toIntOrNull() ?: 0),
                                "idoso" to (idoso.toIntOrNull() ?: 0),
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoParcial = false }, enabled = !enviando) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}

@Composable
private fun SeletorStatusEmbarque(
    atual: OpcaoStatusEmbarque,
    opcoes: List<OpcaoStatusEmbarque>,
    bloqueado: Boolean,
    carregando: Boolean = false,
    aoEscolher: (Int) -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(atual.corAtiva)
                .clickable(enabled = !bloqueado && !carregando) { menuAberto = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = atual.rotulo,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (carregando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_down),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
            opcoes.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.rotulo, fontWeight = FontWeight.SemiBold) },
                    enabled = op.permitido,
                    onClick = {
                        menuAberto = false
                        if (op.permitido) aoEscolher(op.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuSimplesEmbarque(
    rotulo: String,
    opcoes: List<Pair<Int, String>>,
    aoEscolher: (Int) -> Unit,
) {
    var menuAberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = opcoes.isNotEmpty()) { menuAberto = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rotulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(
                painter = painterResource(R.drawable.ic_lucide_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
            opcoes.forEach { (id, nome) ->
                DropdownMenuItem(
                    text = { Text(nome) },
                    onClick = {
                        menuAberto = false
                        aoEscolher(id)
                    },
                )
            }
        }
    }
}

private enum class SituacaoFinanceira { RECEBIDO, PARCIAL, ABERTO }

/** Igual a `situacaoFinanceira` do web: cor do card = dinheiro, não status. */
private fun situacaoFinanceira(aReceber: Double, recebido: Double): SituacaoFinanceira {
    val alvo = aReceber
    if (alvo <= 0.005 || recebido >= alvo - 0.005) return SituacaoFinanceira.RECEBIDO
    if (recebido > 0.005) return SituacaoFinanceira.PARCIAL
    return SituacaoFinanceira.ABERTO
}

private fun coresSeloStatus(statusId: Int?): Triple<Color, Color, Color> = when (statusId) {
    STATUS_CHECK_IN -> Triple(Color(0xFFD1FAE5), Color(0xFF047857), Color(0xFF6EE7B7))
    STATUS_PARCIAL -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFCD34D))
    STATUS_NO_SHOW -> Triple(Color(0xFFFCE7EB), Color(0xFFDA4553), Color(0xFFF5A3AB))
    STATUS_RESERVADO -> Triple(Color(0xFFDBEAFE), Color(0xFF1D4ED8), Color(0xFF93C5FD))
    else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Color(0xFFCBD5E1))
}

private fun bandeiraIdioma(idioma: String?): String {
    if (idioma.isNullOrBlank()) return ""
    val chave = idioma.trim().uppercase(Locale.US).take(2)
    return when (chave) {
        "PT", "BR" -> "🇧🇷"
        "EN" -> "🇺🇸"
        "ES" -> "🇪🇸"
        "FR" -> "🇫🇷"
        "IT" -> "🇮🇹"
        "DE" -> "🇩🇪"
        else -> "🏳️"
    }
}

private fun digitosTelefone(telefone: String): String =
    telefone.filter { it.isDigit() }

private fun linkWhatsapp(telefone: String): String? {
    val numero = digitosTelefone(telefone)
    if (numero.length < 10) return null
    val internacional = telefone.trim().startsWith("+") || numero.length > 11
    return "https://wa.me/${if (internacional) numero else "55$numero"}"
}

private fun linkLigacao(telefone: String): String? {
    val numero = digitosTelefone(telefone)
    if (numero.length < 8) return null
    val prefixo = if (telefone.trim().startsWith("+")) "+" else ""
    return "tel:$prefixo$numero"
}

private object SecaoMapaIds {
    const val TOUR = "tour"
    const val FORNECEDORES = "fornecedores"
    const val EMBARQUES = "embarques"
    const val OCORRENCIAS = "ocorrencias"
    const val INFORMACOES = "informacoes"
}

@Composable
private fun SecaoExpansivel(
    titulo: String,
    aberta: Boolean,
    aoAlternar: () -> Unit,
    conteudo: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCartao)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaCartao)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = aoAlternar)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_grip_vertical),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                titulo,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Igual ao web: círculo primary; fechada →; aberta ↓
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (aberta) 0f else -90f),
                )
            }
        }
        if (aberta) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(modifier = Modifier.padding(12.dp)) { conteudo() }
        }
    }
}

@Composable
private fun AbaChip(
    texto: String,
    ativa: Boolean,
    aoClicar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (ativa) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(onClick = aoClicar)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            texto,
            color = if (ativa) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LinhaCyan(rotulo: String, valor: String, destaque: Boolean = false) {
    Row {
        Text(
            rotulo,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            valor,
            color = if (destaque) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Presentes com referência da reserva (ex.: 41/44), igual ao web. */
@Composable
private fun LinhaCyanComReferencia(rotulo: String, presentes: Int, reservados: Int) {
    Row {
        Text(
            rotulo,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$presentes",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        if (presentes != reservados) {
            Text(
                "/$reservados",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CampoCaixaEmbarque(
    modifier: Modifier = Modifier,
    rotulo: String?,
    centralizado: Boolean = false,
    paddingHorizontal: Dp = 8.dp,
    conteudo: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(horizontal = paddingHorizontal, vertical = 4.dp),
        horizontalAlignment = if (centralizado) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        if (rotulo != null) {
            Text(
                rotulo.uppercase(Locale.getDefault()),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        conteudo()
    }
}

/**
 * Atraso = previsto × horário do check-in (igual ao `computeDelay` do web).
 * Só aparece depois do check-in / parcial / no-show.
 */
private data class AtrasoEmbarque(val minutos: Int, val rotulo: String, val grave: Boolean)

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

private fun calcularAtrasoEmbarque(
    dataIso: String?,
    hora: String?,
    referenciaMs: Long,
): AtrasoEmbarque? {
    if (dataIso.isNullOrBlank() || hora.isNullOrBlank()) return null
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
    if (minutos <= 0) return AtrasoEmbarque(minutos, "", false)

    val rotulo = when {
        minutos >= 60 -> "+%dh %02d".format(minutos / 60, minutos % 60)
        minutos == 1 -> "+1 minuto"
        else -> "+$minutos minutos"
    }
    return AtrasoEmbarque(minutos, rotulo, minutos >= 15)
}

private fun formatarCheckInEm(checkedInAt: String?): String? {
    val ms = parseCheckedInAtMs(checkedInAt) ?: return null
    return SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(ms))
}

private fun dinheiro(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun round2(v: Double): Double = round(v * 100.0) / 100.0

private fun dataIsoMapaHoje(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun formatarDataMapaBr(iso: String): String = try {
    val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val format = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    format.format(parse.parse(iso)!!)
} catch (_: Exception) {
    iso
}

private fun millisUtcMapa(iso: String): Long {
    val partes = iso.split("-")
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, partes.getOrNull(0)?.toIntOrNull() ?: 1970)
        set(Calendar.MONTH, (partes.getOrNull(1)?.toIntOrNull() ?: 1) - 1)
        set(Calendar.DAY_OF_MONTH, partes.getOrNull(2)?.toIntOrNull() ?: 1)
    }
    return cal.timeInMillis
}

private fun isoDeMillisMapa(ms: Long): String {
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = ms
    }
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}
