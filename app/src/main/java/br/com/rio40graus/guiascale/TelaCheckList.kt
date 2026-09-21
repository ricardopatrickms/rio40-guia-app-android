package br.com.rio40graus.guiascale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.TarefaChecklist
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Check list do guia — espelho do `CheckList.tsx` / `ChecklistSection.tsx` do web
 * (visão do guia: marcar itens do embarque do dia, sem CRUD de escritório).
 */
@Composable
fun TelaCheckList(
    aoCarregarMapas: (data: String, aoTerminar: (List<MapaEmbarque>?, String?) -> Unit) -> Unit,
    aoCarregarChecklist: (
        tourId: Int,
        data: String,
        mapaId: Int?,
        aoTerminar: (List<TarefaChecklist>?, String?) -> Unit,
    ) -> Unit,
    aoMarcarChecklist: (
        tarefaId: String,
        mapaId: Int,
        data: String,
        done: Boolean,
        aoTerminar: (String?) -> Unit,
    ) -> Unit,
) {
    val hoje = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var mapas by remember { mutableStateOf<List<MapaEmbarque>?>(null) }
    var erroMapas by remember { mutableStateOf<String?>(null) }
    var carregandoMapas by remember { mutableStateOf(true) }
    var chaveSelecionada by remember { mutableStateOf<String?>(null) }
    var tarefas by remember { mutableStateOf<List<TarefaChecklist>>(emptyList()) }
    var carregandoTarefas by remember { mutableStateOf(false) }
    var erroTarefas by remember { mutableStateOf<String?>(null) }
    var marcandoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(hoje) {
        carregandoMapas = true
        erroMapas = null
        aoCarregarMapas(hoje) { lista, falha ->
            mapas = lista
            erroMapas = falha
            carregandoMapas = false
        }
    }

    val opcoes = remember(mapas) {
        val abertos = mapas.orEmpty()
            .filter { it.tour_id != null && !it.bloqueado }
        val nomes = abertos.map { it.tour }
        val repetidos = nomes.filter { n -> nomes.count { it == n } > 1 }.toSet()
        abertos.map { m ->
            val primeira = m.reservas.mapNotNull { it.hora }.filter { it.isNotBlank() }.minOrNull()
            val nomeBase = m.tour ?: "Passeio"
            val nome = if (m.tour in repetidos && !primeira.isNullOrBlank()) {
                "$nomeBase — ${primeira.take(5)}"
            } else {
                nomeBase
            }
            OpcaoChecklist(
                chave = "mapa-${m.id}",
                tourId = m.tour_id!!,
                mapaId = m.id,
                nome = nome,
                hora = primeira?.take(5),
                bloqueado = m.bloqueado,
            )
        }.sortedBy { it.hora ?: "99:99" }
    }

    val todosFechados = remember(mapas) {
        val doDia = mapas.orEmpty().filter { it.tour_id != null }
        doDia.isNotEmpty() && doDia.all { it.bloqueado }
    }

    val emAndamento = remember(opcoes) {
        if (opcoes.isEmpty()) null
        else {
            val agora = SimpleDateFormat("HH:mm", Locale.US).format(Date())
            val comecados = opcoes.filter { o -> o.hora != null && o.hora <= agora }
            (comecados.lastOrNull() ?: opcoes.first()).chave
        }
    }

    LaunchedEffect(opcoes, emAndamento) {
        if (opcoes.isEmpty()) {
            chaveSelecionada = null
            return@LaunchedEffect
        }
        if (chaveSelecionada == null || opcoes.none { it.chave == chaveSelecionada }) {
            chaveSelecionada = emAndamento
        }
    }

    val selecionada = opcoes.firstOrNull { it.chave == chaveSelecionada }

    LaunchedEffect(selecionada?.chave) {
        val sel = selecionada ?: run {
            tarefas = emptyList()
            return@LaunchedEffect
        }
        carregandoTarefas = true
        erroTarefas = null
        aoCarregarChecklist(sel.tourId, hoje, sel.mapaId) { lista, falha ->
            tarefas = lista.orEmpty()
            erroTarefas = falha
            carregandoTarefas = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = ESPACO_DA_BARRA + 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.checklist_titulo),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        if (opcoes.size > 1) {
            SeletorPasseioChecklist(
                opcoes = opcoes,
                selecionada = selecionada,
                aoEscolher = { chaveSelecionada = it },
            )
        }

        when {
            carregandoMapas -> Box(
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }

            erroMapas != null -> Text(
                erroMapas!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            selecionada == null -> Text(
                stringResource(
                    if (todosFechados) R.string.checklist_todos_fechados
                    else R.string.checklist_sem_escala,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 48.dp).fillMaxWidth(),
            )

            else -> CartaoChecklist(
                nome = selecionada.nome,
                tarefas = tarefas,
                carregando = carregandoTarefas,
                erro = erroTarefas,
                marcavel = !selecionada.bloqueado,
                marcandoId = marcandoId,
                aoAlternar = { tarefa, done ->
                    if (selecionada.bloqueado || marcandoId != null) return@CartaoChecklist
                    marcandoId = tarefa.id
                    // Otimista, igual ao web.
                    tarefas = tarefas.map {
                        if (it.id == tarefa.id) it.copy(done = done) else it
                    }
                    aoMarcarChecklist(tarefa.id, selecionada.mapaId, hoje, done) { falha ->
                        marcandoId = null
                        if (falha != null) {
                            erroTarefas = falha
                            aoCarregarChecklist(selecionada.tourId, hoje, selecionada.mapaId) { lista, _ ->
                                if (lista != null) tarefas = lista
                            }
                        }
                    }
                },
            )
        }
    }
}

private data class OpcaoChecklist(
    val chave: String,
    val tourId: Int,
    val mapaId: Int,
    val nome: String,
    val hora: String?,
    val bloqueado: Boolean,
)

@Composable
private fun SeletorPasseioChecklist(
    opcoes: List<OpcaoChecklist>,
    selecionada: OpcaoChecklist?,
    aoEscolher: (String) -> Unit,
) {
    var aberto by remember { mutableStateOf(false) }
    Column {
        Text(
            stringResource(R.string.checklist_passeio),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable { aberto = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selecionada?.nome ?: stringResource(R.string.checklist_selecione),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
                opcoes.forEach { o ->
                    DropdownMenuItem(
                        text = { Text(o.nome) },
                        onClick = {
                            aberto = false
                            aoEscolher(o.chave)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CartaoChecklist(
    nome: String,
    tarefas: List<TarefaChecklist>,
    carregando: Boolean,
    erro: String?,
    marcavel: Boolean,
    marcandoId: String?,
    aoAlternar: (TarefaChecklist, Boolean) -> Unit,
) {
    val feitas = tarefas.count { it.done }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FormaCartao)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, FormaCartao)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            nome.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        when {
            carregando -> CircularProgressIndicator(
                modifier = Modifier.size(22.dp).align(Alignment.CenterHorizontally),
                strokeWidth = 2.dp,
            )

            erro != null -> Text(
                erro,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )

            tarefas.isEmpty() -> Text(
                stringResource(R.string.checklist_sem_tarefas),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            else -> {
                Text(
                    stringResource(R.string.checklist_concluidas, feitas, tarefas.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tarefas.forEach { tarefa ->
                        LinhaTarefaChecklist(
                            titulo = tarefa.title.orEmpty(),
                            feita = tarefa.done,
                            habilitada = marcavel && marcandoId == null,
                            aoClicar = { aoAlternar(tarefa, !tarefa.done) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaTarefaChecklist(
    titulo: String,
    feita: Boolean,
    habilitada: Boolean,
    aoClicar: () -> Unit,
) {
    val fundo = Color(0xFFF4F4F2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(fundo)
            .clickable(enabled = habilitada, onClick = aoClicar)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (feita) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (feita) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            titulo,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (feita) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (feita) TextDecoration.LineThrough else null,
        )
    }
}
