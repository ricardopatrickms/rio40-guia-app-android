package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.ItemProdutoReserva
import br.com.rio40graus.guiascale.rede.ReservaVitrine
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

/**
 * Vitrine de produtos da reserva — espelho do `VitrineProdutosDialog.tsx` do web.
 *
 * O guia ajusta a quantidade de cada produto (ex.: Drone) e salva; a API grava
 * em reservas_produtos_vendas e o valor entra por cima do saldo a receber.
 */
@Composable
fun DialogProdutosReserva(
    reserva: ReservaVitrine,
    bloqueado: Boolean,
    salvando: Boolean,
    aoFechar: () -> Unit,
    aoSalvar: (List<ItemProdutoReserva>) -> Unit,
) {
    val quantidades = remember(reserva.id) {
        mutableStateMapOf<Int, Int>().apply {
            reserva.produtos.forEach { put(it.id, it.quantidade) }
        }
    }

    fun alterar(produtoId: Int, quantidade: Int) {
        if (bloqueado || salvando) return
        quantidades[produtoId] = maxOf(0, quantidade)
    }

    AlertDialog(
        onDismissRequest = { if (!salvando) aoFechar() },
        title = { Text("Produtos") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    buildString {
                        append(reserva.nome.orEmpty())
                        reserva.voucher?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                reserva.produtos.chunked(2).forEach { linha ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        linha.forEach { produto ->
                            CartaoProduto(
                                nome = produto.nome.orEmpty(),
                                imagem = urlImagemProduto(produto.imagem),
                                valor = produto.valor,
                                valorRegistrado = produto.valor_registrado,
                                obrigatorio = produto.obrigatorio,
                                quantidade = quantidades[produto.id] ?: 0,
                                habilitado = !bloqueado && !salvando,
                                aoAlterar = { alterar(produto.id, it) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Mantém o card sozinho da última linha com meia largura.
                        if (linha.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    aoSalvar(
                        reserva.produtos.map {
                            ItemProdutoReserva(it.id, quantidades[it.id] ?: 0)
                        },
                    )
                },
                enabled = !salvando && !bloqueado,
            ) {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (salvando) "Salvando…" else "Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = aoFechar, enabled = !salvando) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun CartaoProduto(
    nome: String,
    imagem: String?,
    valor: Double,
    valorRegistrado: Boolean,
    obrigatorio: Boolean,
    quantidade: Int,
    habilitado: Boolean,
    aoAlterar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val verde = Color(0xFF10B981)
    val forma = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .border(
                1.dp,
                if (quantidade > 0) verde else MaterialTheme.colorScheme.outlineVariant,
                forma,
            )
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (imagem != null) {
                AsyncImage(
                    model = imagem,
                    contentDescription = nome,
                    modifier = Modifier.fillMaxWidth().height(88.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(nome, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 15.sp)
            Text(
                dinheiroProduto(valor),
                color = Color(0xFF059669),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                if (valorRegistrado) "valor registrado na reserva" else "por unidade",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
            if (obrigatorio) {
                Text("Obrigatório", color = Color(0xFFD97706), fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { aoAlterar(quantidade - 1) },
                    enabled = habilitado && quantidade > 0,
                    modifier = Modifier.size(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                ) { Text("−", fontSize = 16.sp) }
                Text(
                    "$quantidade",
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(
                    onClick = { aoAlterar(quantidade + 1) },
                    enabled = habilitado,
                    modifier = Modifier.size(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = verde,
                        contentColor = Color.White,
                    ),
                ) { Text("+", fontSize = 16.sp) }
            }
        }
    }
}

/**
 * Caminho gravado no produto, servido pelo storage do painel — igual ao
 * `urlImagemProduto` do web. Aceita caminho relativo ou URL completa.
 */
internal fun urlImagemProduto(caminho: String?): String? {
    if (caminho.isNullOrBlank()) return null
    val base = BuildConfig.PAINEL_URL.trim().trimEnd('/')
    if (base.isBlank()) return null
    val relativo = caminho
        .replace(Regex("^https?://[^/]+", RegexOption.IGNORE_CASE), "")
        .trimStart('/')
        .removePrefix("storage/")
    return "$base/storage/$relativo"
}

private fun dinheiroProduto(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)
