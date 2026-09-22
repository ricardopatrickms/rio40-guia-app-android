package br.com.rio40graus.guiascale

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.ProgressoDaLei
import br.com.rio40graus.guiascale.ui.tema.CoresExtras
import br.com.rio40graus.guiascale.ui.tema.FormaBotao
import br.com.rio40graus.guiascale.ui.tema.FormaCampo
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_LEI = "lei_do_guia"
private const val KEY_PROGRESS = "lei_do_guia_progress"

private enum class SyncEstadoLei {
    Idle,
    Loading,
    Saving,
    Synced,
    Offline,
}

private fun serializarProgressoLocal(openIds: Set<String>, lastSection: String?): String {
    val ids = openIds.joinToString(",")
    return if (!lastSection.isNullOrBlank()) "$ids|$lastSection" else ids
}

private fun deserializarProgressoLocal(raw: String): Pair<Set<String>, String?> {
    if (raw.isBlank()) return emptySet<String>() to null
    val parts = raw.split("|", limit = 2)
    val ids = parts[0].split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val last = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    return ids to last
}

private fun lerProgressoLocal(contexto: Context): Pair<Set<String>, String?>? {
    val raw = contexto.getSharedPreferences(PREFS_LEI, Context.MODE_PRIVATE)
        .getString(KEY_PROGRESS, null)
        ?: return null
    return deserializarProgressoLocal(raw)
}

private fun salvarProgressoLocal(contexto: Context, openIds: Set<String>, lastSection: String?) {
    contexto.getSharedPreferences(PREFS_LEI, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_PROGRESS, serializarProgressoLocal(openIds, lastSection))
        .apply()
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ConteudoLeiDoGuia(
    progressoInicial: ProgressoDaLei?,
    carregando: Boolean,
    erro: String?,
    aoSalvarProgresso: (ProgressoDaLei, aoTerminar: (ok: Boolean) -> Unit) -> Unit,
) {
    val contexto = LocalContext.current
    val escopo = rememberCoroutineScope()

    var busca by remember { mutableStateOf("") }
    var openIds by remember { mutableStateOf(setOf("contexto")) }
    var lastSection by remember { mutableStateOf<String?>(null) }
    var secaoParaContinuar by remember { mutableStateOf<String?>(null) }
    var syncState by remember { mutableStateOf(SyncEstadoLei.Idle) }
    var hidratado by remember { mutableStateOf(false) }
    var servidorAplicado by remember { mutableStateOf(false) }
    var faqAbertos by remember { mutableStateOf(setOf<Int>()) }

    val requesters = remember {
        SECOES_LEI.associate { it.id to BringIntoViewRequester() }
    }

    LaunchedEffect(Unit) {
        lerProgressoLocal(contexto)?.let { (ids, last) ->
            if (progressoInicial == null) {
                openIds = if (ids.isNotEmpty()) ids else setOf("contexto")
                lastSection = last
                secaoParaContinuar = last
            }
        }
        hidratado = true
    }

    LaunchedEffect(progressoInicial) {
        if (progressoInicial != null && !servidorAplicado) {
            servidorAplicado = true
            openIds = if (progressoInicial.open_ids.isNotEmpty()) {
                progressoInicial.open_ids.toSet()
            } else {
                setOf("contexto")
            }
            lastSection = progressoInicial.last_section
            secaoParaContinuar = progressoInicial.last_section
            syncState = SyncEstadoLei.Synced
        }
    }

    LaunchedEffect(carregando, progressoInicial, erro) {
        when {
            carregando && progressoInicial == null -> syncState = SyncEstadoLei.Loading
            erro != null && progressoInicial == null && !carregando -> syncState = SyncEstadoLei.Offline
        }
    }

    LaunchedEffect(openIds, lastSection, hidratado) {
        if (!hidratado) return@LaunchedEffect
        salvarProgressoLocal(contexto, openIds, lastSection)
        delay(700)
        syncState = SyncEstadoLei.Saving
        val payload = ProgressoDaLei(
            open_ids = openIds.toList(),
            last_section = lastSection,
        )
        aoSalvarProgresso(payload) { ok ->
            syncState = if (ok) SyncEstadoLei.Synced else SyncEstadoLei.Offline
        }
    }

    val buscaAtiva = busca.trim().isNotEmpty()
    val filtradas = remember(busca) {
        val q = busca.trim().lowercase()
        if (q.isEmpty()) {
            SECOES_LEI
        } else {
            SECOES_LEI.filter { secao ->
                secao.titulo.lowercase().contains(q) ||
                    secao.resumo.lowercase().contains(q) ||
                    secao.searchable.lowercase().contains(q)
            }
        }
    }

    fun expandirSecao(id: String, rolar: Boolean = false) {
        if (!buscaAtiva) {
            openIds = openIds + id
            lastSection = id
        }
        if (rolar) {
            escopo.launch {
                requesters[id]?.bringIntoView()
            }
        }
    }

    fun alternarSecao(id: String) {
        if (buscaAtiva) return
        val abrindo = id !in openIds
        openIds = if (abrindo) openIds + id else openIds - id
        if (abrindo) lastSection = id
    }

    fun estaExpandida(id: String): Boolean =
        if (buscaAtiva) filtradas.any { it.id == id } else id in openIds

    val metaContinuar = secaoParaContinuar?.let { id -> SECOES_LEI.find { it.id == id } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (carregando && progressoInicial == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
        if (erro != null && progressoInicial == null) {
            Text(
                text = erro,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        CartaoHeroLei()

        OutlinedTextField(
            value = busca,
            onValueChange = { busca = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = FormaCampo,
            placeholder = {
                Text(
                    text = stringResource(R.string.lei_buscar),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
            ),
        )

        if (!buscaAtiva) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SECOES_LEI.forEach { secao ->
                    ChipSecaoLei(
                        secao = secao,
                        onClick = { expandirSecao(secao.id, rolar = true) },
                    )
                }
            }
        }

        LinhaSyncLei(estado = syncState)

        if (metaContinuar != null && metaContinuar.id != "contexto" && !buscaAtiva) {
            FaixaContinuarLei(
                secao = metaContinuar,
                onRetomar = { expandirSecao(metaContinuar.id, rolar = true) },
            )
        }

        if (filtradas.isEmpty()) {
            Surface(
                shape = FormaCartao,
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.lei_vazia, busca.trim()),
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            filtradas.forEach { secao ->
                val requester = requesters.getValue(secao.id)
                CartaoSecaoLei(
                    secao = secao,
                    expandida = estaExpandida(secao.id),
                    onToggle = { alternarSecao(secao.id) },
                    modifier = Modifier.bringIntoViewRequester(requester),
                ) {
                    CorpoSecaoLei(
                        id = secao.id,
                        faqAbertos = faqAbertos,
                        onFaqToggle = { idx ->
                            faqAbertos = if (idx in faqAbertos) faqAbertos - idx else faqAbertos + idx
                        },
                    )
                }
            }
        }

        CartaoCompromissoLei()
    }
}

@Composable
private fun CartaoHeroLei() {
    val primaria = MaterialTheme.colorScheme.primary
    Surface(
        shape = FormaCartao,
        color = primaria.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, primaria.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaria.copy(alpha = 0.15f),
                ) {
                    Icon(
                        Icons.Filled.Balance,
                        contentDescription = null,
                        tint = primaria,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.lei_legislacao).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaria,
                    )
                    Text(
                        text = stringResource(R.string.lei_titulo_lei),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.lei_subtitulo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CelulaKpiLei(
                        titulo = stringResource(R.string.lei_kpi_anos),
                        subtitulo = stringResource(R.string.lei_kpi_anos_s),
                        modifier = Modifier.weight(1f),
                    )
                    CelulaKpiLei(
                        titulo = stringResource(R.string.lei_kpi_artigos),
                        subtitulo = stringResource(R.string.lei_kpi_artigos_s),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CelulaKpiLei(
                        titulo = stringResource(R.string.lei_kpi_cadastur),
                        subtitulo = stringResource(R.string.lei_kpi_cadastur_s),
                        modifier = Modifier.weight(1f),
                    )
                    CelulaKpiLei(
                        titulo = stringResource(R.string.lei_kpi_nacional),
                        subtitulo = stringResource(R.string.lei_kpi_nacional_s),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CelulaKpiLei(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitulo.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChipSecaoLei(secao: MetaSecaoLei, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(secao.iconeRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${secao.numero}. ${secao.titulo}",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LinhaSyncLei(estado: SyncEstadoLei) {
    if (estado == SyncEstadoLei.Idle) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (estado) {
            SyncEstadoLei.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.lei_sync_carregando),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SyncEstadoLei.Saving -> {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.lei_sync_salvando),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SyncEstadoLei.Synced -> {
                Icon(
                    Icons.Filled.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CoresExtras.Sucesso,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.lei_sync_ok),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SyncEstadoLei.Offline -> {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CoresExtras.Aviso,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.lei_sync_offline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SyncEstadoLei.Idle -> Unit
        }
    }
}

@Composable
private fun FaixaContinuarLei(secao: MetaSecaoLei, onRetomar: () -> Unit) {
    val primaria = MaterialTheme.colorScheme.primary
    Surface(
        shape = FormaCartao,
        color = primaria.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, primaria.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.BookmarkAdded,
                contentDescription = null,
                tint = primaria,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.lei_continuar_titulo).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.lei_continuar_secao, secao.numero, secao.titulo),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = onRetomar, shape = FormaBotao) {
                Text(stringResource(R.string.lei_retomar))
            }
        }
    }
}

@Composable
private fun CartaoSecaoLei(
    secao: MetaSecaoLei,
    expandida: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Surface(
        shape = FormaCartao,
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                ) {
                    Icon(
                        painter = painterResource(secao.iconeRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.lei_secao, secao.numero).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = secao.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = secao.resumo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (expandida) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expandida,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(10.dp))
                    conteudo()
                }
            }
        }
    }
}

@Composable
private fun CartaoCompromissoLei() {
    Surface(
        shape = FormaCartao,
        color = CoresExtras.Sucesso.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, CoresExtras.Sucesso.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = CoresExtras.Sucesso,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.lei_compromisso_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.lei_compromisso_texto),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CorpoSecaoLei(
    id: String,
    faqAbertos: Set<Int>,
    onFaqToggle: (Int) -> Unit,
) {
    when (id) {
        "contexto" -> SecaoContextoLei()
        "artigos" -> SecaoArtigosLei()
        "definicao" -> SecaoDefinicaoLei()
        "categorias" -> SecaoCategoriasLei()
        "habilitacao" -> SecaoHabilitacaoLei()
        "direitos" -> SecaoDireitosLei()
        "deveres" -> SecaoDeveresLei()
        "vedacoes" -> SecaoVedacoesLei()
        "penalidades" -> SecaoPenalidadesLei()
        "normas" -> SecaoNormasLei()
        "aplicacao" -> SecaoAplicacaoLei()
        "faq" -> SecaoFaqLei(faqAbertos, onFaqToggle)
    }
}

@Composable
private fun SecaoContextoLei() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Nas décadas de 1970 e 1980, o turismo brasileiro vivia expansão acelerada, mas sem legislação específica para o guia. Qualquer pessoa podia atuar como guia, sem formação, ética ou responsabilidade legal — prejudicando turistas, empresas sérias e a imagem do Brasil.",
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
        )
        Text(
            text = "A Embratur tinha normas infralegais, mas era preciso uma lei federal que definisse quem poderia exercer a profissão. Em 28 de janeiro de 1993, o presidente Itamar Franco sancionou a Lei Federal nº 8.623, com 12 artigos. A lei foi regulamentada pelo Decreto nº 946/1993.",
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 20.sp,
        )
        CalloutLei(
            titulo = "Dado importante",
            texto = "Foi uma das primeiras leis brasileiras a regulamentar uma profissão do setor de serviços turísticos. Mais de 30 anos depois, segue como marco regulatório central.",
            corBorda = MaterialTheme.colorScheme.primary,
            corFundo = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        )
    }
}

@Composable
private fun SecaoArtigosLei() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ARTIGOS.forEach { artigo ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 4.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    ),
            ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    text = artigo.numero,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            artigo.titulo?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Text(
                            text = artigo.texto,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp,
                        )
                        if (artigo.incisos.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                artigo.incisos.forEach { inciso ->
                                    Row {
                                        Text(
                                            text = "${inciso.rom} — ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = inciso.texto,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = stringResource(R.string.lei_comentario).uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = artigo.comentario,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecaoDefinicaoLei() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DEFINICAO_ELEMENTOS.forEach { el ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(0.48f),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = el.elemento,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = el.descricao,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(
            text = "Tipos de excursão abrangidos",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        TIPOS_EXCURSAO.forEach { tipo ->
            Text(
                text = "• ${tipo.nome} — ${tipo.descricao}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecaoCategoriasLei() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CATEGORIAS.forEach { cat ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = cat.nome,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = cat.ambito,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    cat.requisitos.forEach { req ->
                        Text(
                            text = "• $req",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = cat.obs,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CalloutLei(
            titulo = "Sobreposição de categorias",
            texto = "Nacional pode atuar em qualidade de qualquer categoria. Regional NÃO pode conduzir em outros estados sem a habilitação correspondente. Local está restrito ao seu atrativo.",
            corBorda = CoresExtras.Aviso,
            corFundo = CoresExtras.Aviso.copy(alpha = 0.12f),
            icone = Icons.Filled.Warning,
        )
    }
}

@Composable
private fun SecaoHabilitacaoLei() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HABILITACAO_PASSOS.forEach { passo ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = passo.n.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column {
                        Text(
                            text = passo.titulo,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = passo.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        CalloutLei(
            titulo = "Sobre o Cadastur",
            corBorda = MaterialTheme.colorScheme.primary,
            corFundo = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        ) {
            listOf(
                "Documento oficial que comprova a habilitação — deve ser portado durante o serviço.",
                "Validade de 2 anos, renovação obrigatória antes do vencimento.",
                "Pode ser verificado online por qualquer pessoa, garantindo transparência.",
                "Cadastur ativo dá acesso a capacitações, eventos e seguros.",
                "Inatividade configura exercício irregular da profissão.",
            ).forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SecaoDireitosLei() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ListaComChecksLei(
            titulo = "Direitos profissionais",
            icone = Icons.Filled.VerifiedUser,
            itens = DIREITOS,
        )
        ListaComChecksLei(
            titulo = "Prerrogativas operacionais",
            icone = Icons.Filled.EmojiEvents,
            itens = PRERROGATIVAS,
        )
    }
}

@Composable
private fun ListaComChecksLei(
    titulo: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    itens: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        itens.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CoresExtras.Sucesso,
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecaoDeveresLei() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DEVERES.forEach { dever ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(0.48f),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = dever.titulo,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = dever.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        CalloutLei(
            titulo = "Responsabilidade civil e criminal",
            texto = "O guia pode ser responsabilizado civilmente por negligência, imprudência ou imperícia. Em casos graves — abandono de grupo com incapaz, omissão de socorro ou transmissão dolosa de informações falsas — a responsabilidade pode ser também criminal. As sanções administrativas NÃO excluem as civis e penais.",
            corBorda = MaterialTheme.colorScheme.error,
            corFundo = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            icone = Icons.Filled.Gavel,
        )
    }
}

@Composable
private fun SecaoVedacoesLei() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VEDACOES.forEach { v ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = v.rom,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                        Text(
                            text = v.titulo,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "Proibido: ${v.proibido}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Consequências: ${v.consequencia}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecaoPenalidadesLei() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PENALIDADES.forEachIndexed { idx, p ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text = "Nível ${idx + 1}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Text(
                            text = p.nome,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = "Quando: ${p.quando}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Efeito: ${p.efeito}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = p.obs,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Fiscalização",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                listOf(
                    "Agentes do Ministério do Turismo e órgãos estaduais",
                    "Denúncias de turistas, empresas e outros guias",
                    "Auditorias em empresas de turismo",
                    "Ações com polícia de turismo em aeroportos e eventos",
                    "Verificação online do Cadastur pelo portal do Mtur",
                ).forEach { item ->
                    Text(
                        text = "• $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecaoNormasLei() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NORMAS.forEach { norma ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = norma.nome,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = norma.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SecaoAplicacaoLei() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ListaComChecksLei(
            titulo = "Obrigações do guia",
            icone = Icons.Filled.Shield,
            itens = APLICACAO_GUIA,
        )
        ListaComChecksLei(
            titulo = "Obrigações da Rio40º (Art. 10)",
            icone = Icons.Filled.Business,
            itens = APLICACAO_EMPRESA,
        )
        CalloutLei(
            titulo = "Verificação do Cadastur — passo a passo",
            corBorda = MaterialTheme.colorScheme.primary,
            corFundo = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
        ) {
            listOf(
                "Acesse cadastur.turismo.gov.br",
                "Clique em \"Consultar Cadastrados\" e selecione \"Guia de Turismo\"",
                "Informe o nome ou número do Cadastur",
                "Verifique a situação: \"Ativo\" é a única condição válida",
                "Confira a categoria — deve corresponder ao serviço contratado",
                "Guarde o print como comprovação da diligência",
            ).forEachIndexed { idx, passo ->
                Text(
                    text = "${idx + 1}. $passo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SecaoFaqLei(
    faqAbertos: Set<Int>,
    onFaqToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FAQ.forEachIndexed { idx, item ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFaqToggle(idx) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.q,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (idx in faqAbertos) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    AnimatedVisibility(visible = idx in faqAbertos) {
                        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = item.a,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalloutLei(
    titulo: String,
    texto: String? = null,
    corBorda: Color,
    corFundo: Color,
    icone: androidx.compose.ui.graphics.vector.ImageVector? = null,
    conteudoExtra: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(corFundo)
            .border(
                width = 4.dp,
                color = corBorda,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icone?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = corBorda)
                }
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            texto?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
            conteudoExtra?.invoke()
    }
}

@Composable
private fun CalloutLei(
    titulo: String,
    corBorda: Color,
    corFundo: Color,
    conteudoExtra: @Composable () -> Unit,
) {
    CalloutLei(
        titulo = titulo,
        texto = null,
        corBorda = corBorda,
        corFundo = corFundo,
        conteudoExtra = conteudoExtra,
    )
}
