package br.com.rio40graus.guiascale

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.ReservaEmbarque

/*
 * Contato do pax a um toque — o mesmo padrão do web (components/mapa/ContatoPax.tsx):
 * o botão redondo disca, o número abre o WhatsApp. Reserva com mais de um pax
 * ("+ Pax" no painel) mostra um contato por pax, separados por "|".
 */

/** Os pax da reserva quando ela tem mais de um; vazio = reserva de um pax só. */
internal fun paxDaReserva(reserva: ReservaEmbarque): List<Pair<String?, String?>> =
    if (reserva.passageiros.size > 1) {
        reserva.passageiros.map { p ->
            p.nome?.trim()?.takeIf { it.isNotEmpty() } to p.telefone?.trim()?.takeIf { it.isNotEmpty() }
        }
    } else {
        emptyList()
    }

/** Um telefone: botão de ligar + "NOME telefone" que abre o WhatsApp. */
@Composable
internal fun TelefonePax(telefone: String, nome: String? = null, tamanho: TextUnit = 13.sp) {
    val contexto = LocalContext.current
    val uriHandler = LocalUriHandler.current
    // Com mais de um pax na reserva, o nome vai junto: "ANA BERRIOS +1939…".
    val texto = if (nome != null) "$nome $telefone" else telefone
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painter = painterResource(R.drawable.ic_lucide_phone),
            contentDescription = stringResource(R.string.mapa_embarque_ligar),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .clickable {
                    linkLigacao(telefone)?.let { uri ->
                        runCatching { contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri))) }
                    }
                }
                .padding(6.dp),
        )
        Text(
            texto,
            modifier = Modifier.clickable { linkWhatsapp(telefone)?.let { uriHandler.openUri(it) } },
            color = MaterialTheme.colorScheme.primary,
            fontSize = tamanho,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Um pax: com telefone, o TelefonePax; sem, só o nome no mesmo azul. */
@Composable
internal fun ContatoPax(nome: String?, telefone: String?, tamanho: TextUnit = 13.sp) {
    when {
        telefone != null -> TelefonePax(telefone, nome, tamanho)
        nome != null -> Text(
            nome,
            color = MaterialTheme.colorScheme.primary,
            fontSize = tamanho,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Todos os pax da reserva na mesma linha, separados por "|" (quebra quando não
 * cabe). [limite] mostra só os primeiros e um "+N pax" / "ver menos" — o card
 * aberto do web mostra 2; o travado mostra todos.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListaPax(
    pax: List<Pair<String?, String?>>,
    modifier: Modifier = Modifier,
    limite: Int? = null,
    tamanho: TextUnit = 13.sp,
) {
    var verTodos by remember(pax) { mutableStateOf(false) }
    val visiveis = if (limite == null || verTodos) pax else pax.take(limite)
    val ocultos = pax.size - visiveis.size
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        visiveis.forEachIndexed { i, (nome, telefone) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (i > 0) Text("|", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = tamanho)
                ContatoPax(nome, telefone, tamanho)
            }
        }
        if (limite != null && pax.size > limite && (ocultos > 0 || verTodos)) {
            Text(
                if (verTodos) "ver menos" else "+$ocultos pax",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { verTodos = !verTodos }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
