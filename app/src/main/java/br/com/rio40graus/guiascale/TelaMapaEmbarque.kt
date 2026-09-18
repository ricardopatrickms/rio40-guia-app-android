package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.FornecedorMapa
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.OcorrenciaMapa
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.rede.STATUS_NO_SHOW
import br.com.rio40graus.guiascale.rede.STATUS_PARCIAL
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
 * EMBARQUES DO DIA e OCORRÊNCIAS. O check-in de cada ponto reusa o mesmo dialog
 * do Geocheck-in.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var mostrarNovaOcorrencia by remember { mutableStateOf(false) }
    var relatoNovo by remember { mutableStateOf("") }
    var salvandoOcorrencia by remember { mutableStateOf(false) }

    val dataExibicao = remember(dataSelecionada) { formatarDataMapaBr(dataSelecionada) }
    val nomeGuia = Sessao.login ?: stringResource(R.string.conta_sem_nome)

    LaunchedEffect(dataSelecionada, recarregar) {
        carregando = true
        erro = null
        aoCarregar(dataSelecionada) { resultado, falha ->
            mapas = resultado
            erro = falha
            carregando = false
            val lista = resultado.orEmpty()
            val emAndamento = lista.filter { !it.bloqueado }
            val finalizados = lista.filter { it.bloqueado }
            val alvo = if (abaFinalizados) finalizados else emAndamento
            if (alvo.none { it.id == mapaSelecionadoId }) {
                mapaSelecionadoId = alvo.firstOrNull()?.id
            }
        }
    }

    val todos = mapas.orEmpty()
    val emAndamento = remember(todos) { todos.filter { !it.bloqueado } }
    val finalizados = remember(todos) { todos.filter { it.bloqueado } }
    val daAba = if (abaFinalizados) finalizados else emAndamento
    val mapaAtual = daAba.firstOrNull { it.id == mapaSelecionadoId } ?: daAba.firstOrNull()

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
                    .statusBarsPadding()
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
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.mapa_embarque_titulo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.mapa_embarque_guia, nomeGuia),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.mapa_embarque_data_rotulo, dataExibicao),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    modifier = Modifier
                        .clip(FormaBotaoPequeno)
                        .border(1.dp, MaterialTheme.colorScheme.outline, FormaBotaoPequeno)
                        .clickable { mostrarCalendario = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(dataExibicao, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                }

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
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (ativo) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .border(
                                        1.dp,
                                        if (ativo) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(20.dp),
                                    )
                                    .clickable { mapaSelecionadoId = m.id }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    "${m.tour.orEmpty()} · $hora · $pax pax",
                                    color = if (ativo) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (m.eh_apoio) {
                                    Text(
                                        stringResource(R.string.mapa_embarque_apoio),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6D28D9),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
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
                            abertaInicial = true,
                        ) { SecaoTour(mapa) }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_fornecedores),
                            abertaInicial = false,
                        ) { SecaoFornecedores(mapa) }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_embarques),
                            abertaInicial = true,
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
                                        CartaoEmbarque(
                                            reserva = reserva,
                                            bloqueado = mapa.bloqueado,
                                            enviando = enviando == reserva.id,
                                            aoEditar = { editando = mapa to reserva },
                                        )
                                    }
                                }
                            }
                        }

                        SecaoExpansivel(
                            titulo = stringResource(R.string.mapa_embarque_ocorrencias),
                            abertaInicial = false,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ocorrencias.isEmpty()) {
                                    Text(
                                        stringResource(R.string.mapa_embarque_sem_ocorrencia),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                } else {
                                    ocorrencias.forEach { o ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(FormaBotaoPequeno)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaBotaoPequeno)
                                                .padding(10.dp),
                                        ) {
                                            Text(
                                                o.autor_nome.orEmpty(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                o.relato.orEmpty(),
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                            o.status?.takeIf { it.isNotBlank() }?.let {
                                                Text(
                                                    it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                                if (!mapa.bloqueado) {
                                    OutlinedButton(
                                        onClick = {
                                            relatoNovo = ""
                                            mostrarNovaOcorrencia = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.mapa_embarque_nova_ocorrencia))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        editando?.let { (mapa, reserva) ->
            val atual = mapa.reservas.firstOrNull { it.id == reserva.id } ?: reserva
            DialogCheckInEmbarque(
                reserva = atual,
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
                aoRecarregar = { recarregar++ },
            )
        }

        if (mostrarNovaOcorrencia && mapaAtual != null) {
            AlertDialog(
                onDismissRequest = { if (!salvandoOcorrencia) mostrarNovaOcorrencia = false },
                title = { Text(stringResource(R.string.mapa_embarque_nova_ocorrencia)) },
                text = {
                    OutlinedTextField(
                        value = relatoNovo,
                        onValueChange = { relatoNovo = it },
                        label = { Text(stringResource(R.string.mapa_embarque_relato)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = relatoNovo.isNotBlank() && !salvandoOcorrencia,
                        onClick = {
                            salvandoOcorrencia = true
                            aoCriarOcorrencia(mapaAtual.id, relatoNovo.trim()) { falha ->
                                salvandoOcorrencia = false
                                if (falha == null) {
                                    mostrarNovaOcorrencia = false
                                    recarregar++
                                } else {
                                    erro = falha
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.mapa_embarque_salvar_ocorrencia)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { mostrarNovaOcorrencia = false },
                        enabled = !salvandoOcorrencia,
                    ) { Text(stringResource(R.string.cancelar)) }
                },
            )
        }
    }
}

@Composable
private fun SecaoTour(mapa: MapaEmbarque) {
    val totais = mapa.totais
    val reservas = mapa.reservas
    val saida = reservas.mapNotNull { it.hora }.minOrNull()?.take(5) ?: "--:--"
    val totalAReceber = reservas.sumOf { it.a_receber + it.taxas }
    val porForma = mutableMapOf<String, Double>()
    var totalRecebido = 0.0
    reservas.forEach { r ->
        r.pagamentos.forEach { p ->
            val nome = listOfNotNull(p.tipo, p.forma).joinToString(" - ").ifBlank { "Pagamento" }
            porForma[nome] = (porForma[nome] ?: 0.0) + p.valor
            totalRecebido += p.valor
        }
    }
    val saldo = round2(totalAReceber - totalRecebido)
    val ocupacao = mapa.ocupacao?.pax ?: totais?.pax ?: reservas.sumOf { it.total ?: 0 }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinhaCyan(stringResource(R.string.mapa_embarque_ocupacao), ocupacao.toString())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LinhaCyan(stringResource(R.string.mapa_embarque_adultos), (totais?.adt ?: 0).toString())
            LinhaCyan(stringResource(R.string.mapa_embarque_chd), (totais?.chd ?: 0).toString())
            LinhaCyan(stringResource(R.string.mapa_embarque_inf), (totais?.inf ?: 0).toString())
        }
        if ((totais?.jovem ?: 0) > 0 || (totais?.idoso ?: 0) > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinhaCyan(stringResource(R.string.mapa_embarque_jovem), (totais?.jovem ?: 0).toString())
                LinhaCyan(stringResource(R.string.mapa_embarque_idoso), (totais?.idoso ?: 0).toString())
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
private fun SecaoFornecedores(mapa: MapaEmbarque) {
    val fornecedores = mapa.fornecedores
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (mapa.fornecedor != null || mapa.motorista != null || mapa.veiculo != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(FormaBotaoPequeno)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaBotaoPequeno)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    mapa.fornecedor ?: "—",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "${stringResource(R.string.mapa_embarque_motorista)}: ${mapa.motorista?.nome ?: "—"}  ·  " +
                        "${stringResource(R.string.mapa_embarque_telefone)}: ${mapa.motorista?.telefone ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "${stringResource(R.string.mapa_embarque_carro)}: " +
                        listOfNotNull(mapa.veiculo?.modelo, mapa.veiculo?.tipo, mapa.veiculo?.placa)
                            .joinToString(" · ")
                            .ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                )
                mapa.transporte_despesa?.let { d ->
                    Text(dinheiro(d.valor), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (fornecedores.isEmpty() && mapa.fornecedor == null) {
            Text(
                stringResource(R.string.mapa_embarque_sem_fornecedor),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        fornecedores.forEach { f ->
            CartaoFornecedor(f)
        }

        val totalAPagar = fornecedores.sumOf { it.valor } + (mapa.transporte_despesa?.valor ?: 0.0)
        val pagos = fornecedores.flatMap { it.pagamentos } +
            (mapa.transporte_despesa?.pagamentos.orEmpty())
        val totalPago = pagos.sumOf { it.valor }
        val saldo = round2(totalAPagar - totalPago)
        if (totalAPagar > 0 || totalPago > 0) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            LinhaCyan(stringResource(R.string.mapa_embarque_valor_pagar), dinheiro(totalAPagar))
            if (totalPago > 0) {
                LinhaCyan(stringResource(R.string.mapa_embarque_total_pago), dinheiro(totalPago))
            }
            LinhaCyan(
                stringResource(R.string.mapa_embarque_saldo_pagar),
                dinheiro(saldo),
                destaque = saldo > 0.009,
            )
        }
    }
}

@Composable
private fun CartaoFornecedor(f: FornecedorMapa) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaBotaoPequeno)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaBotaoPequeno)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            f.nome ?: "—",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        f.atividade?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.labelSmall)
        }
        f.descricao?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Text(dinheiro(f.valor), fontWeight = FontWeight.SemiBold)
        f.pagamentos.forEach { p ->
            Text(
                "${p.forma ?: "Pago"}: ${dinheiro(p.valor)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoEmbarque(
    reserva: ReservaEmbarque,
    bloqueado: Boolean,
    enviando: Boolean,
    aoEditar: () -> Unit,
) {
    val statusCor = when (reserva.status?.id) {
        STATUS_CHECK_IN -> Color(0xFF16A34A)
        STATUS_NO_SHOW -> Color(0xFFDA4553)
        STATUS_PARCIAL -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCartao)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaCartao)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${reserva.hora?.take(5) ?: "--:--"} · ${reserva.embarque.orEmpty()}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                reserva.status?.nome ?: "—",
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusCor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        reserva.pax?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            listOfNotNull(
                reserva.bairro,
                reserva.apto?.takeIf { it.isNotBlank() }?.let { "Apto $it" },
            ).joinToString(" · ").ifBlank { reserva.endereco.orEmpty() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "A receber: ${dinheiro(reserva.a_receber)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        if (enviando) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            TextButton(
                onClick = aoEditar,
                enabled = !bloqueado,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.mapa_embarque_checkin_editar))
            }
        }
    }
}

@Composable
private fun SecaoExpansivel(
    titulo: String,
    abertaInicial: Boolean,
    conteudo: @Composable () -> Unit,
) {
    var aberta by remember { mutableStateOf(abertaInicial) }
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
                .clickable { aberta = !aberta }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                titulo,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                if (aberta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
        }
        if (aberta) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(Modifier.padding(12.dp)) { conteudo() }
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
