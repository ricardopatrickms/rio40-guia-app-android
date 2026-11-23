package br.com.rio40graus.guiascale

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.AtividadeDespesa
import br.com.rio40graus.guiascale.rede.DespesaSemPreco
import br.com.rio40graus.guiascale.rede.PedidoDespesaExtra

/**
 * "Adicionar despesa" da aba Fornecedores — espelho do `AdicionarDespesa` do
 * web: Atividade e Despesa ("Fornecedor - Despesa") mais o valor. Só aparecem
 * as despesas que o financeiro cadastrou SEM PREÇO no fornecedor; o valor é o
 * guia quem informa. A despesa entra no acerto do mapa.
 */
@Composable
fun DialogDespesaExtra(
    aoCarregarAtividades: ((List<AtividadeDespesa>?, String?) -> Unit) -> Unit,
    aoCarregarDespesas: (atividadeId: Int, aoTerminar: (List<DespesaSemPreco>?, String?) -> Unit) -> Unit,
    aoAdicionar: (despesa: PedidoDespesaExtra, aoTerminar: (String?) -> Unit) -> Unit,
    aoFechar: () -> Unit,
) {
    var atividades by remember { mutableStateOf<List<AtividadeDespesa>?>(null) }
    var atividade by remember { mutableStateOf<AtividadeDespesa?>(null) }
    var despesas by remember { mutableStateOf<List<DespesaSemPreco>?>(null) }
    var despesa by remember { mutableStateOf<DespesaSemPreco?>(null) }
    var valorTexto by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    // "1.234,56" (pt-BR) ou "35.5": com vírgula, ponto é milhar.
    val valor = if (valorTexto.contains(',')) {
        valorTexto.replace(".", "").replace(',', '.').toDoubleOrNull() ?: 0.0
    } else {
        valorTexto.toDoubleOrNull() ?: 0.0
    }

    LaunchedEffect(Unit) {
        aoCarregarAtividades { lista, falha ->
            atividades = lista.orEmpty()
            erro = falha
        }
    }

    LaunchedEffect(atividade?.id) {
        val id = atividade?.id ?: return@LaunchedEffect
        despesas = null
        aoCarregarDespesas(id) { lista, falha ->
            despesas = lista.orEmpty()
            erro = falha
        }
    }

    val carregando = stringResource(R.string.despesa_extra_carregando)

    AlertDialog(
        onDismissRequest = { if (!salvando) aoFechar() },
        title = { Text(stringResource(R.string.despesa_extra_adicionar)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.despesa_extra_atividade), style = MaterialTheme.typography.labelMedium)
                    MenuDespesa(
                        rotulo = atividade?.nome
                            ?: if (atividades == null) carregando else stringResource(R.string.despesa_extra_selecione),
                        opcoes = atividades.orEmpty().map { it.id to (it.nome ?: "—") },
                        habilitado = !salvando,
                        aoEscolher = { id ->
                            atividade = atividades?.firstOrNull { it.id == id }
                            despesa = null
                        },
                    )
                    if (atividades?.isEmpty() == true) {
                        Text(
                            stringResource(R.string.despesa_extra_nenhuma_cadastrada),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.despesa_extra_despesa), style = MaterialTheme.typography.labelMedium)
                    MenuDespesa(
                        rotulo = despesa?.nome
                            ?: if (atividade != null && despesas == null) carregando
                            else stringResource(R.string.despesa_extra_selecione_despesa),
                        opcoes = despesas.orEmpty().map { it.id to (it.nome ?: "—") },
                        habilitado = !salvando && atividade != null,
                        aoEscolher = { id -> despesa = despesas?.firstOrNull { it.id == id } },
                    )
                }
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { novo -> valorTexto = novo.filter { it.isDigit() || it == ',' || it == '.' } },
                    label = { Text(stringResource(R.string.despesa_extra_valor)) },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    enabled = !salvando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                erro?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = despesa != null && valor > 0 && !salvando,
                onClick = {
                    val d = despesa ?: return@Button
                    salvando = true
                    erro = null
                    aoAdicionar(
                        PedidoDespesaExtra(
                            parceiro_tarifario_id = d.id,
                            valor = Math.round(valor * 100) / 100.0,
                        ),
                    ) { falha ->
                        salvando = false
                        if (falha == null) aoFechar() else erro = falha
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (salvando) R.string.despesa_extra_adicionando else R.string.despesa_extra_botao,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar, enabled = !salvando) { Text(stringResource(R.string.fechar)) }
        },
    )
}

@Composable
private fun MenuDespesa(
    rotulo: String,
    opcoes: List<Pair<Int, String>>,
    habilitado: Boolean,
    aoEscolher: (Int) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = habilitado && opcoes.isNotEmpty()) { aberto = true }
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
