package br.com.rio40graus.guiascale

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.ItemPagamentoFornecedor
import br.com.rio40graus.guiascale.rede.PagamentoFornecedor
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.round

/**
 * Modal "Formas de pagamento — {empresa}" do mapa de embarque web.
 *
 * O total vem do tarifário (somente leitura); o guia só distribui entre formas.
 * Soma acima do total bloqueia o salvamento — mesma regra da API.
 */
@Composable
fun DialogPagamentoFornecedor(
    empresa: String,
    total: Double,
    pagamentosIniciais: List<PagamentoFornecedor>,
    aoCarregarFormas: ((List<FormaPagamento>?, String?) -> Unit) -> Unit,
    aoCarregarParcelas: ((List<ParcelaOpcao>?, String?) -> Unit) -> Unit,
    aoSalvar: (List<ItemPagamentoFornecedor>, aoTerminar: (String?) -> Unit) -> Unit,
    aoFechar: () -> Unit,
    aoRecarregar: () -> Unit,
) {
    var formas by remember { mutableStateOf<List<FormaPagamento>>(emptyList()) }
    var parcelas by remember { mutableStateOf<List<ParcelaOpcao>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val entradas = remember {
        mutableStateListOf<EntradaForn>().apply {
            if (pagamentosIniciais.isEmpty()) return@apply
            addAll(
                pagamentosIniciais.map { p ->
                    EntradaForn(
                        formaId = p.forma_id,
                        parcelaId = p.parcela_id,
                        valor = p.valor,
                        pagId = p.id.takeIf { it > 0 },
                    )
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        var pendentes = 2
        fun pronto() {
            pendentes--
            if (pendentes <= 0) carregando = false
        }
        aoCarregarFormas { lista, falha ->
            if (falha != null) erro = falha
            formas = lista.orEmpty()
            // Preenche o tipo a partir da forma já gravada.
            entradas.forEachIndexed { i, e ->
                val forma = formas.firstOrNull { it.id == e.formaId }
                if (forma != null && e.tipo.isBlank()) {
                    entradas[i] = e.copy(tipo = forma.tipo.orEmpty())
                }
            }
            pronto()
        }
        aoCarregarParcelas { lista, falha ->
            if (falha != null && erro == null) erro = falha
            parcelas = lista.orEmpty()
            pronto()
        }
    }

    val tipos = remember(formas) {
        formas.mapNotNull { it.tipo?.takeIf { t -> t.isNotBlank() } }.distinct().sorted()
    }
    val soma = entradas.sumOf { it.valor }
    val falta = round2(total - soma)
    val excede = falta < -0.005

    AlertDialog(
        onDismissRequest = { if (!salvando) aoFechar() },
        title = {
            Text(stringResource(R.string.mapa_pag_forn_titulo, empresa))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.mapa_pag_forn_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Text(
                        dinheiroBr(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.mapa_pag_forn_formas),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    OutlinedButton(
                        onClick = {
                            entradas.add(
                                EntradaForn(valor = maxOf(0.0, falta)),
                            )
                        },
                        enabled = !salvando && !carregando,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            stringResource(R.string.mapa_pag_forn_adicionar),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                when {
                    carregando -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    tipos.isEmpty() -> {
                        Text(
                            stringResource(R.string.mapa_pag_forn_sem_formas),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    entradas.isEmpty() -> {
                        Text(
                            stringResource(R.string.mapa_pag_forn_vazio),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                entradas.forEachIndexed { idx, entrada ->
                    CartaoPagamentoForn(
                        entrada = entrada,
                        tipos = tipos,
                        formas = formas,
                        parcelas = parcelas,
                        bloqueado = salvando,
                        aoMudarTipo = { t ->
                            entradas[idx] = entrada.copy(tipo = t, formaId = null, parcelaId = null)
                        },
                        aoMudarForma = { id ->
                            entradas[idx] = entrada.copy(formaId = id)
                        },
                        aoMudarParcela = { id ->
                            entradas[idx] = entrada.copy(parcelaId = id)
                        },
                        aoMudarValor = { v ->
                            entradas[idx] = entrada.copy(valor = v)
                        },
                        aoRemover = { entradas.removeAt(idx) },
                    )
                }

                if (entradas.isNotEmpty()) {
                    val corFundo = when {
                        excede -> MaterialTheme.colorScheme.errorContainer
                        falta > 0.005 -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                    val corTexto = when {
                        excede -> MaterialTheme.colorScheme.onErrorContainer
                        falta > 0.005 -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onTertiaryContainer
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, corFundo, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(
                                R.string.mapa_pag_forn_soma,
                                dinheiroBr(soma),
                                dinheiroBr(total),
                            ),
                            color = corTexto,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            when {
                                excede -> stringResource(
                                    R.string.mapa_pag_forn_excede,
                                    dinheiroBr(kotlin.math.abs(falta)),
                                )
                                falta > 0.005 -> stringResource(
                                    R.string.mapa_pag_forn_falta,
                                    dinheiroBr(falta),
                                )
                                else -> stringResource(R.string.mapa_pag_forn_ok)
                            },
                            color = corTexto,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                erro?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !salvando && !carregando && !excede,
                onClick = {
                    salvando = true
                    erro = null
                    val itens = entradas
                        .filter { it.formaId != null && it.valor > 0 }
                        .map { e ->
                            val credito = formas.firstOrNull { it.id == e.formaId }?.credito == true
                            ItemPagamentoFornecedor(
                                forma_id = e.formaId!!,
                                valor = round2(e.valor),
                                parcela_id = if (credito) e.parcelaId else null,
                                id = e.pagId,
                            )
                        }
                    aoSalvar(itens) { falha ->
                        salvando = false
                        if (falha == null) {
                            aoRecarregar()
                            aoFechar()
                        } else {
                            erro = falha
                        }
                    }
                },
            ) {
                if (salvando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.mapa_pag_forn_salvar))
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar, enabled = !salvando) {
                Text(stringResource(R.string.cancelar))
            }
        },
    )
}

private data class EntradaForn(
    val tipo: String = "",
    val formaId: Int? = null,
    val parcelaId: Int? = null,
    val valor: Double = 0.0,
    val pagId: Int? = null,
)

@Composable
private fun CartaoPagamentoForn(
    entrada: EntradaForn,
    tipos: List<String>,
    formas: List<FormaPagamento>,
    parcelas: List<ParcelaOpcao>,
    bloqueado: Boolean,
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
        MenuTextoForn(
            rotulo = entrada.tipo.ifBlank { stringResource(R.string.checkin_tipo) },
            opcoes = tipos,
            bloqueado = bloqueado,
            aoEscolher = aoMudarTipo,
        )
        MenuIdForn(
            rotulo = formas.firstOrNull { it.id == entrada.formaId }?.nome
                ?: stringResource(R.string.checkin_forma),
            opcoes = formasDoTipo.map { it.id to (it.nome ?: "#${it.id}") },
            bloqueado = bloqueado || entrada.tipo.isBlank(),
            aoEscolher = aoMudarForma,
        )
        if (formas.firstOrNull { it.id == entrada.formaId }?.credito == true) {
            MenuIdForn(
                rotulo = parcelas.firstOrNull { it.id == entrada.parcelaId }?.nome
                    ?: stringResource(R.string.checkin_parcela),
                opcoes = parcelas.map { it.id to (it.nome ?: "#${it.id}") },
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
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !bloqueado,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            IconButton(onClick = aoRemover, enabled = !bloqueado) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.checkin_remover),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MenuTextoForn(
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
                .clickable(enabled = !bloqueado && opcoes.isNotEmpty()) { aberto = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rotulo, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { nome ->
                DropdownMenuItem(
                    text = { Text(nome) },
                    onClick = {
                        aberto = false
                        aoEscolher(nome)
                    },
                )
            }
        }
    }
}

@Composable
private fun MenuIdForn(
    rotulo: String,
    opcoes: List<Pair<Int, String>>,
    bloqueado: Boolean,
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

private fun dinheiroBr(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun round2(v: Double): Double = round(v * 100.0) / 100.0
