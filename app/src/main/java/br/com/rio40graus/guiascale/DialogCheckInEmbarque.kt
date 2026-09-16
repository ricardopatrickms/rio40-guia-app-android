package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.ReservaEmbarque
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.rede.STATUS_NO_SHOW
import br.com.rio40graus.guiascale.rede.STATUS_PARCIAL
import br.com.rio40graus.guiascale.rede.STATUS_RESERVADO
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * Modal de check-in do cliente — espelho do PickupCheckinDialog do web:
 * status (seletor), pagamentos com resumo financeiro e idioma.
 */
data class AcoesCheckIn(
    val carregarMotivos: ((RespostaMotivos?, String?) -> Unit) -> Unit,
    val carregarFormas: ((List<FormaPagamento>?, String?) -> Unit) -> Unit,
    val carregarParcelas: ((List<ParcelaOpcao>?, String?) -> Unit) -> Unit,
    val carregarIdiomas: ((List<IdiomaOpcao>?, String?) -> Unit) -> Unit,
    val trocarStatus: (statusId: Int, motivoId: Int?, parcial: Map<String, Int>?, aoTerminar: (String?) -> Unit) -> Unit,
    val salvarPagamentos: (List<ItemPagamento>, aoTerminar: (String?) -> Unit) -> Unit,
    val salvarIdioma: (idiomaId: Int, aoTerminar: (String?) -> Unit) -> Unit,
)

private data class EntradaPagamento(
    val tipo: String = "",
    val formaId: Int? = null,
    val parcelaId: Int? = null,
    val valor: Double = 0.0,
    val pagId: Int? = null,
    val sugerido: Boolean = false,
)

private data class OpcaoStatus(
    val id: Int,
    val rotulo: String,
    val permitido: Boolean,
    val corAtiva: Color,
)

@Composable
fun DialogCheckInEmbarque(
    reserva: ReservaEmbarque,
    bloqueado: Boolean,
    enviando: Boolean,
    acoes: AcoesCheckIn,
    aoFechar: () -> Unit,
    aoRecarregar: () -> Unit,
) {
    var erro by remember { mutableStateOf<String?>(null) }
    var salvandoStatus by remember { mutableStateOf(false) }
    var salvandoPagamento by remember { mutableStateOf(false) }
    var salvandoIdioma by remember { mutableStateOf(false) }

    var dialogoMotivo by remember { mutableStateOf(false) }
    var dialogoParcial by remember { mutableStateOf(false) }

    var motivos by remember { mutableStateOf<RespostaMotivos?>(null) }
    var carregandoMotivos by remember { mutableStateOf(false) }
    var categoriaId by remember { mutableStateOf<Int?>(null) }
    var motivoId by remember { mutableStateOf<Int?>(null) }

    var adulto by remember { mutableStateOf((reserva.parcial?.adulto ?: 0).toString()) }
    var chd by remember { mutableStateOf((reserva.parcial?.chd ?: 0).toString()) }
    var infantil by remember { mutableStateOf((reserva.parcial?.infantil ?: 0).toString()) }
    var jovem by remember { mutableStateOf((reserva.parcial?.jovem ?: 0).toString()) }
    var idoso by remember { mutableStateOf((reserva.parcial?.idoso ?: 0).toString()) }

    var formas by remember { mutableStateOf<List<FormaPagamento>>(emptyList()) }
    var parcelas by remember { mutableStateOf<List<ParcelaOpcao>>(emptyList()) }
    var idiomas by remember { mutableStateOf<List<IdiomaOpcao>>(emptyList()) }
    var carregandoPag by remember { mutableStateOf(true) }

    val entradas = remember {
        mutableStateListOf<EntradaPagamento>()
    }
    // Só remonta as formas se o conteúdo gravado mudou — um refresh de status
    // não pode apagar o que o guia ainda está digitando no pagamento.
    val assinaturaPagamentos = reserva.pagamentos.joinToString("|") {
        "${it.pag_id}:${it.forma_id}:${it.valor}:${it.parcela_id}:${it.tipo}"
    }
    LaunchedEffect(reserva.id, assinaturaPagamentos) {
        entradas.clear()
        reserva.pagamentos.forEach { p ->
            entradas.add(
                EntradaPagamento(
                    tipo = p.tipo.orEmpty(),
                    formaId = p.forma_id.takeIf { it > 0 },
                    parcelaId = p.parcela_id,
                    valor = p.valor,
                    pagId = p.pag_id.takeIf { it > 0 },
                ),
            )
        }
    }

    var idiomaId by remember {
        mutableStateOf(
            reserva.idioma_id
                ?: idiomas.firstOrNull { it.codigo == reserva.idioma }?.id,
        )
    }

    // Status local: o botão muda na hora; o refresh silencioso confirma depois.
    var statusLocal by remember(reserva.id) { mutableStateOf(reserva.status?.id) }
    LaunchedEffect(reserva.status?.id) {
        statusLocal = reserva.status?.id
    }

    val totalPax = reserva.total ?: (reserva.adt + reserva.chd + reserva.inf + reserva.jovem + reserva.idoso)
    val paxTexto = listOf(
        reserva.adt to "ADT",
        reserva.chd to "CHD",
        reserva.inf to "INF",
        reserva.jovem to "Jovem",
        reserva.idoso to "Idoso",
    ).filter { it.first > 0 }.joinToString(" · ") { "${it.first} ${it.second}" }.ifBlank { "—" }

    val opcoesStatus = listOf(
        OpcaoStatus(STATUS_CHECK_IN, "Check-in", true, Color(0xFF16A34A)),
        OpcaoStatus(STATUS_RESERVADO, "Reservado", false, Color(0xFF3B82F6)),
        // Mesma cor do web / painel rio40graus (#da4553).
        OpcaoStatus(STATUS_NO_SHOW, "No show", true, Color(0xFFDA4553)),
        OpcaoStatus(STATUS_PARCIAL, "Parcial", true, Color(0xFF3B82F6)),
    )
    val statusAtual = statusLocal
    val opcaoAtual = opcoesStatus.firstOrNull { it.id == statusAtual }
        ?: OpcaoStatus(statusAtual ?: 0, reserva.status?.nome ?: "Status", false, Color(0xFF94A3B8))

    LaunchedEffect(Unit) {
        carregandoPag = true
        var pendentes = 3
        fun pronto() {
            pendentes--
            if (pendentes <= 0) carregandoPag = false
        }
        acoes.carregarFormas { lista, falha ->
            if (falha != null) erro = falha else formas = lista.orEmpty()
            pronto()
        }
        acoes.carregarParcelas { lista, falha ->
            if (falha != null) erro = falha else parcelas = lista.orEmpty()
            pronto()
        }
        acoes.carregarIdiomas { lista, falha ->
            if (falha != null) erro = falha
            else {
                idiomas = lista.orEmpty()
                if (idiomaId == null) {
                    idiomaId = lista.orEmpty().firstOrNull { it.codigo == reserva.idioma }?.id
                        ?: reserva.idioma_id
                }
            }
            pronto()
        }
    }

    LaunchedEffect(dialogoMotivo) {
        if (dialogoMotivo && motivos == null && !carregandoMotivos) {
            carregandoMotivos = true
            acoes.carregarMotivos { lista, falha ->
                carregandoMotivos = false
                if (falha != null) erro = falha else motivos = lista
            }
        }
    }

    fun gravarStatus(statusId: Int, motivo: Int? = null, parcial: Map<String, Int>? = null) {
        erro = null
        salvandoStatus = true
        acoes.trocarStatus(statusId, motivo, parcial) { falha ->
            salvandoStatus = false
            if (falha == null) {
                statusLocal = statusId
                dialogoMotivo = false
                dialogoParcial = false
                aoRecarregar()
            } else {
                erro = falha
            }
        }
    }

    fun escolherStatus(id: Int) {
        if (bloqueado || salvandoStatus || enviando) return
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

    val tipos = remember(formas) {
        formas.mapNotNull { it.tipo?.takeIf { t -> t.isNotBlank() } }.distinct().sorted()
    }

    fun formaDe(e: EntradaPagamento) = formas.firstOrNull { it.id == e.formaId }
    fun ehCredito(e: EntradaPagamento) = formaDe(e)?.credito == true
    fun taxaDa(e: EntradaPagamento): Double {
        if (!ehCredito(e)) return 0.0
        val p = e.parcelaId?.let { id -> parcelas.firstOrNull { it.id == id } }
            ?: parcelas.firstOrNull { it.id == 1 }
        return p?.taxa ?: 0.0
    }
    fun naMaquina(e: EntradaPagamento): Double {
        val base = e.valor
        return centavos(base + centavos(base * taxaDa(e) / 100.0))
    }
    fun baseQueFecha(idx: Int): Double {
        val outras = entradas.mapIndexed { i, e -> if (i == idx) 0.0 else e.valor }.sum()
        return centavos(max(0.0, reserva.a_receber - outras))
    }

    val somaBases = entradas.sumOf { it.valor }
    val somaRecebido = entradas.sumOf { naMaquina(it) }
    val diferenca = centavos(somaBases - reserva.a_receber)

    AlertDialog(
        onDismissRequest = { if (!enviando && !salvandoStatus && !salvandoPagamento) aoFechar() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.checkin_titulo), style = MaterialTheme.typography.titleLarge)
                Text(
                    reserva.embarque.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(reserva.pax?.takeIf { it.isNotBlank() } ?: stringResource(R.string.checkin_cliente_ausente))
                        reserva.apto?.takeIf { it.isNotBlank() }?.let { append(" · Apto $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                reserva.passeio?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        reserva.hora?.take(5) ?: "--:--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (statusAtual == STATUS_CHECK_IN) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.check_in_feito),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Secao(titulo = stringResource(R.string.checkin_capacidade)) {
                    Text(paxTexto, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.checkin_total_pax, totalPax),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Secao(titulo = stringResource(R.string.checkin_status)) {
                    SeletorStatus(
                        atual = opcaoAtual,
                        opcoes = opcoesStatus.filter { it.permitido || it.id == statusAtual },
                        bloqueado = bloqueado || salvandoStatus || enviando,
                        carregando = salvandoStatus || enviando,
                        aoEscolher = ::escolherStatus,
                    )
                }

                Secao(titulo = stringResource(R.string.checkin_pagamentos)) {
                    if (carregandoPag) {
                        Text(
                            stringResource(R.string.checkin_carregando_formas),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (tipos.isEmpty()) {
                        Text(
                            stringResource(R.string.checkin_formas_erro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        if (entradas.isEmpty()) {
                            Text(
                                stringResource(R.string.checkin_sem_forma),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        entradas.forEachIndexed { idx, entrada ->
                            CartaoPagamento(
                                entrada = entrada,
                                tipos = tipos,
                                formas = formas,
                                parcelas = parcelas,
                                bloqueado = bloqueado,
                                naMaquina = if (ehCredito(entrada) && taxaDa(entrada) > 0) naMaquina(entrada) else null,
                                taxa = taxaDa(entrada),
                                aoMudarTipo = { t ->
                                    entradas[idx] = entrada.copy(tipo = t, formaId = null, parcelaId = null)
                                },
                                aoMudarForma = { id ->
                                    val forma = formas.firstOrNull { it.id == id }
                                    val sugerir = entrada.valor <= 0
                                    entradas[idx] = entrada.copy(
                                        formaId = id,
                                        parcelaId = if (forma?.credito == true) {
                                            entrada.parcelaId ?: parcelas.firstOrNull()?.id
                                        } else null,
                                        valor = if (sugerir) baseQueFecha(idx) else entrada.valor,
                                        sugerido = sugerir,
                                    )
                                },
                                aoMudarParcela = { id ->
                                    entradas[idx] = entrada.copy(
                                        parcelaId = id,
                                        valor = if (entrada.sugerido) baseQueFecha(idx) else entrada.valor,
                                    )
                                },
                                aoMudarValor = { v ->
                                    entradas[idx] = entrada.copy(valor = v, sugerido = false)
                                },
                                aoRemover = { entradas.removeAt(idx) },
                            )
                        }
                        OutlinedButton(
                            onClick = { entradas.add(EntradaPagamento()) },
                            enabled = !bloqueado,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("+ ", fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.checkin_adicionar_forma))
                        }

                        ResumoFinanceiro(
                            recebido = somaRecebido,
                            aReceber = reserva.a_receber,
                            diferenca = diferenca,
                            faltaOuExcesso = centavos(reserva.a_receber - somaBases),
                        )

                        Button(
                            onClick = {
                                salvandoPagamento = true
                                erro = null
                                val itens = entradas
                                    .filter { it.formaId != null && it.valor > 0 }
                                    .map {
                                        ItemPagamento(
                                            forma_id = it.formaId!!,
                                            valor = centavos(it.valor),
                                            parcela_id = if (ehCredito(it)) it.parcelaId else null,
                                            pag_id = it.pagId,
                                        )
                                    }
                                acoes.salvarPagamentos(itens) { falha ->
                                    salvandoPagamento = false
                                    if (falha == null) aoRecarregar() else erro = falha
                                }
                            },
                            enabled = !bloqueado && !salvandoPagamento,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (salvandoPagamento) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.checkin_salvar_pagamento))
                        }
                    }
                }

                Secao(titulo = stringResource(R.string.checkin_idioma)) {
                    MenuSimples(
                        rotulo = idiomas.firstOrNull { it.id == idiomaId }?.let {
                            "${it.codigo ?: ""} ${it.nome.orEmpty()}".trim()
                        } ?: stringResource(R.string.checkin_selecionar_idioma),
                        opcoes = idiomas.map { it.id to "${it.codigo ?: ""} ${it.nome.orEmpty()}".trim() },
                        bloqueado = bloqueado || salvandoIdioma,
                        aoEscolher = { id ->
                            idiomaId = id
                            salvandoIdioma = true
                            acoes.salvarIdioma(id) { falha ->
                                salvandoIdioma = false
                                if (falha == null) aoRecarregar() else erro = falha
                            }
                        },
                    )
                }

                erro?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = aoFechar,
                enabled = !enviando && !salvandoStatus && !salvandoPagamento,
            ) {
                Text(stringResource(R.string.fechar))
            }
        },
    )

    if (dialogoMotivo) {
        AlertDialog(
            onDismissRequest = { if (!salvandoStatus) dialogoMotivo = false },
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
                            MenuSimples(
                                rotulo = motivos!!.categorias.firstOrNull { it.id == categoriaId }?.nome
                                    ?: stringResource(R.string.checkin_categoria),
                                opcoes = motivos!!.categorias.map { it.id to (it.nome ?: "#${it.id}") },
                                aoEscolher = { categoriaId = it; motivoId = null },
                            )
                            val filtrados = motivos!!.motivos.filter {
                                categoriaId == null || it.categoria_id == categoriaId
                            }
                            MenuSimples(
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
                    enabled = motivoId != null && !salvandoStatus,
                ) { Text(stringResource(R.string.confirmar)) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoMotivo = false }, enabled = !salvandoStatus) {
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
            onDismissRequest = { if (!salvandoStatus) dialogoParcial = false },
            title = { Text(stringResource(R.string.checkin_parcial_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    enabled = !invalido && !salvandoStatus,
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
                TextButton(onClick = { dialogoParcial = false }, enabled = !salvandoStatus) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}

@Composable
private fun Secao(titulo: String, conteudo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        conteudo()
    }
}

@Composable
private fun SeletorStatus(
    atual: OpcaoStatus,
    opcoes: List<OpcaoStatus>,
    bloqueado: Boolean,
    carregando: Boolean = false,
    aoEscolher: (Int) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(atual.corAtiva)
                .clickable(enabled = !bloqueado && !carregando) { aberto = true }
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
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.White)
            }
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.rotulo, fontWeight = FontWeight.SemiBold) },
                    enabled = op.permitido,
                    onClick = {
                        aberto = false
                        if (op.permitido) aoEscolher(op.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuSimples(
    rotulo: String,
    opcoes: List<Pair<Int, String>>,
    bloqueado: Boolean = false,
    aoEscolher: (Int) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = !bloqueado && opcoes.isNotEmpty()) { aberto = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rotulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { (id, nome) ->
                DropdownMenuItem(
                    text = { Text(nome) },
                    onClick = {
                        aberto = false
                        aoEscolher(id)
                    },
                )
            }
        }
    }
}

@Composable
private fun CartaoPagamento(
    entrada: EntradaPagamento,
    tipos: List<String>,
    formas: List<FormaPagamento>,
    parcelas: List<ParcelaOpcao>,
    bloqueado: Boolean,
    naMaquina: Double?,
    taxa: Double,
    aoMudarTipo: (String) -> Unit,
    aoMudarForma: (Int) -> Unit,
    aoMudarParcela: (Int) -> Unit,
    aoMudarValor: (Double) -> Unit,
    aoRemover: () -> Unit,
) {
    val formasDoTipo = formas.filter { it.tipo == entrada.tipo }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MenuTexto(
            rotulo = entrada.tipo.ifBlank { stringResource(R.string.checkin_tipo) },
            opcoes = tipos,
            bloqueado = bloqueado,
            aoEscolher = aoMudarTipo,
        )
        MenuSimples(
            rotulo = formas.firstOrNull { it.id == entrada.formaId }?.nome
                ?: stringResource(R.string.checkin_forma),
            opcoes = formasDoTipo.map { it.id to (it.nome ?: "#${it.id}") },
            bloqueado = bloqueado || entrada.tipo.isBlank(),
            aoEscolher = aoMudarForma,
        )
        if (formas.firstOrNull { it.id == entrada.formaId }?.credito == true) {
            MenuSimples(
                rotulo = parcelas.firstOrNull { it.id == entrada.parcelaId }?.let {
                    buildString {
                        append(it.nome.orEmpty())
                        if (it.taxa > 0) append(" (${formatarPct(it.taxa)}%)")
                    }
                } ?: stringResource(R.string.checkin_parcela),
                opcoes = parcelas.map {
                    it.id to buildString {
                        append(it.nome.orEmpty())
                        if (it.taxa > 0) append(" (${formatarPct(it.taxa)}%)")
                    }
                },
                bloqueado = bloqueado,
                aoEscolher = aoMudarParcela,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = if (entrada.valor == 0.0) "" else String.format(Locale.US, "%.2f", entrada.valor),
                onValueChange = { txt ->
                    aoMudarValor(txt.replace(',', '.').toDoubleOrNull() ?: 0.0)
                },
                label = { Text(stringResource(R.string.checkin_valor)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                enabled = !bloqueado,
            )
            IconButton(onClick = aoRemover, enabled = !bloqueado) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.checkin_remover), tint = MaterialTheme.colorScheme.error)
            }
        }
        if (naMaquina != null && entrada.valor > 0) {
            Text(
                text = stringResource(R.string.checkin_na_maquina, brl(naMaquina), formatarPct(taxa)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MenuTexto(
    rotulo: String,
    opcoes: List<String>,
    bloqueado: Boolean,
    aoEscolher: (String) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = !bloqueado) { aberto = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rotulo, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t) },
                    onClick = {
                        aberto = false
                        aoEscolher(t)
                    },
                )
            }
        }
    }
}

@Composable
private fun ResumoFinanceiro(
    recebido: Double,
    aReceber: Double,
    diferenca: Double,
    faltaOuExcesso: Double,
) {
    val rotulo = when {
        abs(diferenca) < 0.005 -> stringResource(R.string.checkin_total)
        diferenca < 0 -> stringResource(R.string.checkin_falta)
        else -> stringResource(R.string.checkin_excesso)
    }
    val cor = when {
        abs(diferenca) < 0.005 -> Color(0xFF16A34A)
        diferenca < 0 -> MaterialTheme.colorScheme.error
        else -> Color(0xFF1D4ED8)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.checkin_resumo).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Empilhados: em largura de celular, dois cards lado a lado quebram
        // o "R$ 100,00" no meio (vírgula numa linha, centavos na outra).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinhaResumo(
                rotulo = stringResource(R.string.checkin_recebido),
                valor = brl(recebido),
            )
            LinhaResumo(
                rotulo = rotulo,
                valor = brl(abs(faltaOuExcesso)),
                cor = cor,
                negrito = true,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Text(
                stringResource(R.string.checkin_a_receber).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                brl(aReceber),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun LinhaResumo(
    rotulo: String,
    valor: String,
    cor: Color = Color.Unspecified,
    negrito: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = if (cor == Color.Unspecified) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                cor
            },
            fontWeight = if (negrito) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            color = if (cor == Color.Unspecified) {
                MaterialTheme.colorScheme.onSurface
            } else {
                cor
            },
            fontWeight = if (negrito) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun centavos(v: Double) = round(v * 100.0) / 100.0

private fun brl(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(v)

private fun formatarPct(v: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
        maximumFractionDigits = 2
    }.format(v)
