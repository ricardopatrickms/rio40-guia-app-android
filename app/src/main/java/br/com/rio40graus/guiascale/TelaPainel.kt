package br.com.rio40graus.guiascale

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rio40graus.guiascale.rede.AgendaDoGuia
import br.com.rio40graus.guiascale.rede.BloqueioGuia
import br.com.rio40graus.guiascale.rede.ComparativoPasseio
import br.com.rio40graus.guiascale.rede.DetalhePasseio
import br.com.rio40graus.guiascale.rede.DiasSemanaGuia
import br.com.rio40graus.guiascale.rede.FichaGuia
import br.com.rio40graus.guiascale.rede.ItemAgenda
import br.com.rio40graus.guiascale.rede.KpisDoPainel
import br.com.rio40graus.guiascale.rede.PasseioListaItem
import br.com.rio40graus.guiascale.rede.PasseiosDoGuia
import br.com.rio40graus.guiascale.rede.ProgressoDaLei
import br.com.rio40graus.guiascale.rede.Sessao
import br.com.rio40graus.guiascale.rede.SolicitacaoTrabalho
import br.com.rio40graus.guiascale.ui.tema.CoresExtras
import br.com.rio40graus.guiascale.ui.tema.FormaBotao
import br.com.rio40graus.guiascale.ui.tema.FormaBotaoPequeno
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/** Ícones Lucide iguais ao `PainelGuia.tsx` do web. */
private enum class AbaPainel(
    val rotulo: Int,
    @DrawableRes val iconeRes: Int,
) {
    VISAO(R.string.painel_aba_visao, R.drawable.ic_lucide_layout_grid),
    PASSEIOS(R.string.painel_aba_passeios, R.drawable.ic_lucide_map_pin),
    AGENDA(R.string.painel_aba_agenda, R.drawable.ic_lucide_calendar_clock),
    FICHA(R.string.painel_aba_ficha, R.drawable.ic_lucide_id_card),
    LEI(R.string.painel_aba_lei, R.drawable.ic_lucide_scale),
}

private enum class PeriodoPainelUi(val api: String, val rotulo: Int) {
    MES("mes", R.string.painel_periodo_mes),
    SEMANA("semana", R.string.painel_periodo_semana),
    ACUMULADO("acumulado", R.string.painel_periodo_acumulado),
}

/**
 * Painel do guia — espelho do `PainelGuia.tsx` + abas Visão Geral / Passeios /
 * Agenda / Minha Ficha / Lei do Guia (dados da guias-api).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelaPainel(
    aoCarregarPainel: (periodo: String, aoTerminar: (KpisDoPainel?, String?) -> Unit) -> Unit,
    aoCarregarPasseios: (periodo: String, aoTerminar: (PasseiosDoGuia?, String?) -> Unit) -> Unit,
    aoCarregarAgenda: (de: String, ate: String, aoTerminar: (AgendaDoGuia?, String?) -> Unit) -> Unit,
    aoCarregarFicha: (aoTerminar: (FichaGuia?, String?) -> Unit) -> Unit,
    aoCarregarLei: (aoTerminar: (ProgressoDaLei?, String?) -> Unit) -> Unit,
    aoSalvarLei: (ProgressoDaLei, aoTerminar: (Boolean) -> Unit) -> Unit,
    aoCarregarBloqueios: (from: String?, aoTerminar: (List<BloqueioGuia>?, String?) -> Unit) -> Unit,
    aoCarregarDiasSemana: (aoTerminar: (DiasSemanaGuia?, String?) -> Unit) -> Unit,
    aoCarregarDiasOcupados: (from: String?, aoTerminar: (List<String>?, String?) -> Unit) -> Unit,
    aoSalvarBloqueios: (datas: List<String>, motivo: String?, aoTerminar: (String?) -> Unit) -> Unit,
    aoRemoverBloqueios: (datas: List<String>, aoTerminar: (String?) -> Unit) -> Unit,
    aoCarregarSolicitacoes: (from: String?, aoTerminar: (List<SolicitacaoTrabalho>?, String?) -> Unit) -> Unit,
    aoCriarSolicitacoes: (datas: List<String>, motivo: String?, aoTerminar: (String?) -> Unit) -> Unit,
    aoAbrirEmbarque: () -> Unit,
) {
    val contexto = LocalContext.current
    var periodo by remember { mutableStateOf(PeriodoPainelUi.MES) }
    var aba by remember { mutableStateOf(AbaPainel.VISAO) }
    var menuPeriodo by remember { mutableStateOf(false) }

    var kpis by remember { mutableStateOf<KpisDoPainel?>(null) }
    var carregandoKpis by remember { mutableStateOf(true) }
    var erroKpis by remember { mutableStateOf<String?>(null) }

    var passeios by remember { mutableStateOf<PasseiosDoGuia?>(null) }
    var carregandoPasseios by remember { mutableStateOf(false) }
    var erroPasseios by remember { mutableStateOf<String?>(null) }

    var ficha by remember { mutableStateOf<FichaGuia?>(null) }
    var carregandoFicha by remember { mutableStateOf(false) }
    var erroFicha by remember { mutableStateOf<String?>(null) }

    var lei by remember { mutableStateOf<ProgressoDaLei?>(null) }
    var carregandoLei by remember { mutableStateOf(false) }
    var erroLei by remember { mutableStateOf<String?>(null) }

    var listaPasseiosAberta by remember { mutableStateOf(false) }
    var bloqueioAberto by remember { mutableStateOf(false) }
    var solicitarAberto by remember { mutableStateOf(false) }

    var bloqueiosProximos by remember { mutableStateOf<List<BloqueioGuia>>(emptyList()) }
    var removendoData by remember { mutableStateOf<String?>(null) }
    var recarregarBloqueios by remember { mutableStateOf(0) }

    val hojeIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val fmtExibir = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val fmtApi = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    fun recarregarListaBloqueios() {
        aoCarregarBloqueios(hojeIso) { lista, _ ->
            bloqueiosProximos = lista.orEmpty().sortedBy { it.date }
        }
    }

    LaunchedEffect(recarregarBloqueios) {
        recarregarListaBloqueios()
    }

    LaunchedEffect(periodo) {
        carregandoKpis = true
        erroKpis = null
        aoCarregarPainel(periodo.api) { dados, falha ->
            kpis = dados
            erroKpis = falha
            carregandoKpis = false
        }
    }

    LaunchedEffect(aba, periodo) {
        when (aba) {
            AbaPainel.PASSEIOS -> {
                carregandoPasseios = true
                erroPasseios = null
                aoCarregarPasseios(periodo.api) { dados, falha ->
                    passeios = dados
                    erroPasseios = falha
                    carregandoPasseios = false
                }
            }
            AbaPainel.AGENDA -> {
                // A agenda gerencia o mês sozinha (igual ao web).
            }
            AbaPainel.FICHA -> {
                if (ficha == null && !carregandoFicha) {
                    carregandoFicha = true
                    erroFicha = null
                    aoCarregarFicha { dados, falha ->
                        ficha = dados
                        erroFicha = falha
                        carregandoFicha = false
                    }
                }
            }
            AbaPainel.LEI -> {
                if (lei == null && !carregandoLei) {
                    carregandoLei = true
                    erroLei = null
                    aoCarregarLei { dados, falha ->
                        lei = dados
                        erroLei = falha
                        carregandoLei = false
                    }
                }
            }
            AbaPainel.VISAO -> Unit
        }
    }

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.painel_titulo),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.painel_subtitulo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IndicadorRealtime()
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = Sessao.login.orEmpty().ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = CoresExtras.Aviso,
            ) {
                Text(
                    text = stringResource(R.string.painel_demanda_media),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CoresExtras.TextoAviso,
                )
            }
            Box {
                Surface(
                    shape = FormaBotaoPequeno,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { menuPeriodo = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(periodo.rotulo),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DropdownMenu(expanded = menuPeriodo, onDismissRequest = { menuPeriodo = false }) {
                    PeriodoPainelUi.entries.forEach { opcao ->
                        DropdownMenuItem(
                            text = { Text(stringResource(opcao.rotulo)) },
                            onClick = {
                                periodo = opcao
                                menuPeriodo = false
                            },
                        )
                    }
                }
            }
        }

        CartaoAcaoPrimario(
            iconeRes = R.drawable.ic_lucide_map_pinned,
            titulo = stringResource(R.string.painel_mapa_titulo),
            descricao = stringResource(R.string.painel_mapa_desc),
            onClick = aoAbrirEmbarque,
        )
        CartaoAcaoSecundario(
            iconeRes = R.drawable.ic_lucide_calendar_range,
            titulo = stringResource(R.string.painel_disp_titulo),
            descricao = stringResource(R.string.painel_disp_desc),
            onClick = { bloqueioAberto = true },
        )

        if (bloqueiosProximos.isNotEmpty()) {
            Surface(
                shape = FormaCartao,
                color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.bloqueios_proximos),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    bloqueiosProximos.forEach { b ->
                        val iso = b.date.take(10)
                        val dataExibir = runCatching {
                            fmtApi.parse(iso)?.let { fmtExibir.format(it) }
                        }.getOrNull() ?: iso
                        // Igual ao web: `rounded-md border` sobre fundo branco do card.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(8.dp),
                                )
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dataExibir,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (!b.reason.isNullOrBlank()) {
                                    Text(
                                        text = b.reason!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    removendoData = iso
                                    aoRemoverBloqueios(listOf(iso)) { falha ->
                                        removendoData = null
                                        if (falha != null) {
                                            Toast.makeText(contexto, falha, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                contexto,
                                                contexto.getString(R.string.bloqueios_removido),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            recarregarBloqueios++
                                        }
                                    }
                                },
                                enabled = removendoData == null,
                            ) {
                                if (removendoData == iso) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.bloqueios_remover),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        CartaoAcaoSecundario(
            iconeRes = R.drawable.ic_lucide_briefcase,
            titulo = stringResource(R.string.painel_solicitar_titulo),
            descricao = stringResource(R.string.painel_solicitar_desc),
            onClick = { solicitarAberto = true },
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AbaPainel.entries.forEach { item ->
                val ativa = item == aba
                Surface(
                    shape = FormaBotaoPequeno,
                    color = if (ativa) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (ativa) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier.clickable { aba = item },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(item.iconeRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (ativa) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Text(
                            text = stringResource(item.rotulo),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ativa) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        when (aba) {
            AbaPainel.VISAO -> ConteudoVisaoGeral(
                kpis = kpis,
                carregando = carregandoKpis,
                erro = erroKpis,
                bloqueios = bloqueiosProximos,
                aoAbrirLista = { listaPasseiosAberta = true },
            )
            AbaPainel.PASSEIOS -> ConteudoPasseios(
                dados = passeios,
                carregando = carregandoPasseios,
                erro = erroPasseios,
            )
            AbaPainel.AGENDA -> ConteudoAgenda(
                aoCarregarAgenda = aoCarregarAgenda,
            )
            AbaPainel.FICHA -> ConteudoFicha(
                dados = ficha,
                carregando = carregandoFicha,
                erro = erroFicha,
            )
            AbaPainel.LEI -> ConteudoLeiDoGuia(
                progressoInicial = lei,
                carregando = carregandoLei,
                erro = erroLei,
                aoSalvarProgresso = aoSalvarLei,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (listaPasseiosAberta) {
        ListaPasseiosDialog(
            itens = kpis?.passeiosLista.orEmpty(),
            onDismiss = { listaPasseiosAberta = false },
        )
    }

    DialogBloqueioDatas(
        aberto = bloqueioAberto,
        aoFechar = {
            bloqueioAberto = false
            recarregarBloqueios++
        },
        aoCarregarBloqueios = aoCarregarBloqueios,
        aoCarregarDiasSemana = aoCarregarDiasSemana,
        aoCarregarDiasOcupados = aoCarregarDiasOcupados,
        aoSalvar = { datas, motivo, aoTerminar ->
            aoSalvarBloqueios(datas, motivo) { falha ->
                if (falha == null) recarregarBloqueios++
                aoTerminar(falha)
            }
        },
        aoRemover = { datas, aoTerminar ->
            aoRemoverBloqueios(datas) { falha ->
                if (falha == null) recarregarBloqueios++
                aoTerminar(falha)
            }
        },
    )

    DialogSolicitarTrabalho(
        aberto = solicitarAberto,
        aoFechar = { solicitarAberto = false },
        aoCarregarDiasSemana = aoCarregarDiasSemana,
        aoCarregarSolicitacoes = aoCarregarSolicitacoes,
        aoEnviar = aoCriarSolicitacoes,
    )
}

@Composable
private fun IndicadorRealtime() {
    // Sem canal realtime no app (o web usa Supabase). Espelha o estado Offline
    // do RealtimeIndicator quando connected=false.
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
            Icon(
                Icons.Filled.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.painel_offline),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "· ${stringResource(R.string.painel_aguardando)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoAcaoPrimario(
    @DrawableRes iconeRes: Int,
    titulo: String,
    descricao: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = FormaCartao,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(iconeRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = descricao,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CartaoAcaoSecundario(
    @DrawableRes iconeRes: Int,
    titulo: String,
    descricao: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = FormaCartao,
        color = Color.White,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(iconeRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = descricao,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConteudoVisaoGeral(
    kpis: KpisDoPainel?,
    carregando: Boolean,
    erro: String?,
    bloqueios: List<BloqueioGuia>,
    aoAbrirLista: () -> Unit,
) {
    when {
        carregando && kpis == null -> GradeEsqueleto()
        erro != null && kpis == null -> TextoErro(erro)
        kpis != null -> {
            val aniv = kpis.aniversario
            val diasAniv = diasAteAniversario(aniv)
            val idade = idadeDe(aniv)
            val anivLabel = aniv?.let { iso ->
                val p = iso.split("-")
                if (p.size >= 3) "${p[2]}/${p[1]}" else iso
            }.orEmpty()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_dias),
                        valor = "${kpis.diasTrabalhados}",
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_passeios),
                        valor = "${kpis.passeiosRealizados}",
                        modifier = Modifier.weight(1f),
                        onClick = aoAbrirLista,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_valores),
                        valor = formatarBrl(kpis.receita),
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_servicos),
                        valor = "${kpis.totalServicos}",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_agendamentos),
                        valor = "${kpis.agendamentos}",
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = stringResource(R.string.painel_kpi_bloqueados),
                        valor = "${bloqueios.size}",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (diasAniv != null) {
                    KpiCard(
                        label = stringResource(
                            R.string.painel_kpi_aniv,
                            if (idade != null) " · $idade anos" else "",
                        ),
                        valor = anivLabel,
                        meta = when (diasAniv) {
                            0 -> stringResource(R.string.painel_aniv_hoje)
                            1 -> stringResource(R.string.painel_aniv_1)
                            else -> stringResource(R.string.painel_aniv_n, diasAniv)
                        },
                        destaque = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                BlocoListaAgenda(
                    titulo = stringResource(R.string.painel_ultimos_7),
                    itens = kpis.ultimos7dias,
                )
                BlocoListaAgenda(
                    titulo = stringResource(R.string.painel_proximos),
                    itens = kpis.proximos,
                )
            }
        }
        else -> GradeEsqueleto()
    }
}

private enum class SortPasseio { NOME, QTY, PAX, RATING, RECEITA }

@Composable
private fun ConteudoPasseios(
    dados: PasseiosDoGuia?,
    carregando: Boolean,
    erro: String?,
) {
    var sort by remember { mutableStateOf(SortPasseio.QTY) }
    var asc by remember { mutableStateOf(false) }

    when {
        carregando && dados == null -> GradeEsqueleto()
        erro != null && dados == null -> TextoErro(erro)
        dados != null -> {
            val rows = remember(dados.detalhamento, sort, asc) {
                dados.detalhamento.sortedWith { a, b ->
                    val cmp = when (sort) {
                        SortPasseio.NOME -> (a.nome ?: "").compareTo(b.nome ?: "", ignoreCase = true)
                        SortPasseio.QTY -> a.qty.compareTo(b.qty)
                        SortPasseio.PAX -> a.pax.compareTo(b.pax)
                        SortPasseio.RATING -> (a.avaliacao ?: 0.0).compareTo(b.avaliacao ?: 0.0)
                        SortPasseio.RECEITA -> a.receita.compareTo(b.receita)
                    }
                    if (asc) cmp else -cmp
                }
            }
            val totalQty = rows.sumOf { it.qty }
            val totalPax = rows.sumOf { it.pax }
            val totalReceita = rows.sumOf { it.receita }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = FormaCartao,
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.passeios_comparativo),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (dados.comparativo.isEmpty()) {
                            Text(
                                text = stringResource(R.string.painel_sem_passeios),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            GraficoComparativo(itens = dados.comparativo)
                            Spacer(modifier = Modifier.height(10.dp))
                            // Ordem igual ao web / Recharts: anterior (cinza) → atual (verde)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LegendaBarra(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    stringResource(R.string.passeios_mes_anterior),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                LegendaBarra(CoresExtras.Sucesso, stringResource(R.string.passeios_mes_atual))
                            }
                        }
                    }
                }

                Surface(
                    shape = FormaCartao,
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)) {
                        Text(
                            text = stringResource(R.string.passeios_detalhamento),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Cabeçalho com fundo muted (igual TableHead do web).
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CabecalhoSort(
                                texto = stringResource(R.string.passeios_col_passeio),
                                ativo = sort == SortPasseio.NOME,
                                asc = asc,
                                modifier = Modifier.weight(1.4f),
                            ) {
                                if (sort == SortPasseio.NOME) asc = !asc else {
                                    sort = SortPasseio.NOME; asc = true
                                }
                            }
                            CabecalhoSort(
                                texto = stringResource(R.string.passeios_col_qtd),
                                ativo = sort == SortPasseio.QTY,
                                asc = asc,
                                alignEnd = true,
                                modifier = Modifier.weight(0.55f),
                            ) {
                                if (sort == SortPasseio.QTY) asc = !asc else {
                                    sort = SortPasseio.QTY; asc = false
                                }
                            }
                            CabecalhoSort(
                                texto = stringResource(R.string.passeios_col_pax),
                                ativo = sort == SortPasseio.PAX,
                                asc = asc,
                                alignEnd = true,
                                modifier = Modifier.weight(0.55f),
                            ) {
                                if (sort == SortPasseio.PAX) asc = !asc else {
                                    sort = SortPasseio.PAX; asc = false
                                }
                            }
                            CabecalhoSort(
                                texto = stringResource(R.string.passeios_col_avaliacao),
                                ativo = sort == SortPasseio.RATING,
                                asc = asc,
                                alignEnd = true,
                                modifier = Modifier.weight(0.85f),
                            ) {
                                if (sort == SortPasseio.RATING) asc = !asc else {
                                    sort = SortPasseio.RATING; asc = false
                                }
                            }
                            CabecalhoSort(
                                texto = stringResource(R.string.passeios_col_receita),
                                ativo = sort == SortPasseio.RECEITA,
                                asc = asc,
                                alignEnd = true,
                                modifier = Modifier.weight(0.95f),
                            ) {
                                if (sort == SortPasseio.RECEITA) asc = !asc else {
                                    sort = SortPasseio.RECEITA; asc = false
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )

                        if (rows.isEmpty()) {
                            Text(
                                text = stringResource(R.string.painel_sem_passeios),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            rows.forEachIndexed { index, r ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 1.dp,
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = r.nome ?: "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.4f),
                                    )
                                    Text(
                                        text = "${r.qty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.55f),
                                    )
                                    Text(
                                        text = "${r.pax}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.55f),
                                    )
                                    Text(
                                        text = r.avaliacao?.let {
                                            String.format(Locale("pt", "BR"), "%.1f", it)
                                        } ?: "—",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.85f),
                                    )
                                    Text(
                                        text = formatarBrl(r.receita),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(0.95f),
                                    )
                                }
                            }
                            // Igual ao web: `border-t-2 font-medium` no Total
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline,
                                thickness = 2.dp,
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.passeios_total),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.4f),
                                )
                                Text(
                                    text = "$totalQty",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.55f),
                                )
                                Text(
                                    text = "$totalPax",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.55f),
                                )
                                Text(
                                    text = "—",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(0.85f),
                                )
                                Text(
                                    text = formatarBrl(totalReceita),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.95f),
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> GradeEsqueleto()
    }
}

@Composable
private fun CabecalhoSort(
    texto: String,
    ativo: Boolean,
    asc: Boolean,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ativo) {
            Icon(
                if (asc) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendaBarra(cor: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cor),
        )
        Text(text = texto, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun GraficoComparativo(itens: List<ComparativoPasseio>) {
    /*
     * Espelho do BarChart Recharts (Passeios.tsx, modo celular):
     * - cabe na largura (sem scroll) — barras encolhem se tiver muitos passeios
     * - eixo Y + base X com stroke (linhas ao lado dos números e embaixo)
     * - cada passeio = par anterior (cinza) | atual (verde)
     */
    val visiveis = itens.filter { it.atual > 0 || it.anterior > 0 }
    if (visiveis.isEmpty()) return

    val bruto = visiveis.maxOf { max(it.atual, it.anterior) }.coerceAtLeast(1)
    val maxEixo = eixoArredondado(bruto)
    val ticks = listOf(0, maxEixo / 4, maxEixo / 2, (maxEixo * 3) / 4, maxEixo)
        .distinct()
        .sortedDescending()

    val corAnterior = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val corAtual = CoresExtras.Sucesso
    val corEixo = MaterialTheme.colorScheme.onSurfaceVariant
    val corGrade = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    val alturaPlot = 160.dp
    val alturaLabel = 36.dp
    val n = visiveis.size

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .width(26.dp)
                .height(alturaPlot),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            ticks.forEach { t ->
                Text(
                    text = "$t",
                    style = MaterialTheme.typography.labelSmall,
                    color = corEixo,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaPlot),
            ) {
                val w = size.width
                val h = size.height
                val strokeEixo = 1.dp.toPx()
                val tickLen = 4.dp.toPx()
                val tickCount = (ticks.size - 1).coerceAtLeast(1)

                // Grade horizontal
                ticks.forEachIndexed { idx, _ ->
                    val y = h * idx / tickCount
                    drawLine(
                        color = corGrade,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = strokeEixo,
                    )
                }

                // Eixo Y (linha vertical ao lado dos números) + tracinhos
                drawLine(
                    color = corEixo,
                    start = Offset(0f, 0f),
                    end = Offset(0f, h),
                    strokeWidth = strokeEixo,
                )
                ticks.forEachIndexed { idx, _ ->
                    val y = h * idx / tickCount
                    drawLine(
                        color = corEixo,
                        start = Offset(0f, y),
                        end = Offset(tickLen, y),
                        strokeWidth = strokeEixo,
                    )
                }

                // Eixo X (linha de base embaixo)
                drawLine(
                    color = corEixo,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = strokeEixo,
                )

                // Barras agrupadas — cabem sempre na largura (sem scroll)
                val slot = w / n
                val folga = slot * 0.16f
                val barGap = 3.dp.toPx()
                val util = (slot - folga).coerceAtLeast(barGap * 2)
                val barW = ((util - barGap) / 2f).coerceAtLeast(3.dp.toPx())
                val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                visiveis.forEachIndexed { i, item ->
                    val base = slot * i + folga / 2f
                    val xAnt = base
                    val xAtual = base + barW + barGap

                    fun desenhar(x: Float, valor: Int, cor: Color) {
                        if (valor <= 0) return
                        val bh = h * (valor.toFloat() / maxEixo)
                        drawRoundRect(
                            color = cor,
                            topLeft = Offset(x, h - bh),
                            size = Size(barW, bh),
                            cornerRadius = radius,
                        )
                    }
                    desenhar(xAnt, item.anterior, corAnterior)
                    desenhar(xAtual, item.atual, corAtual)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaLabel)
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                visiveis.forEach { item ->
                    Text(
                        text = item.nome ?: "—",
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = corEixo,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

/** Escala “bonita” do eixo Y, no estilo do Recharts (ex.: 8 → 8, 5 → 5, 3 → 4). */
private fun eixoArredondado(valor: Int): Int {
    if (valor <= 4) return 4
    if (valor <= 8) return 8
    if (valor <= 10) return 10
    val passo = when {
        valor <= 20 -> 5
        valor <= 50 -> 10
        else -> 20
    }
    return (ceil(valor.toDouble() / passo) * passo).toInt()
}

@Composable
private fun ConteudoAgenda(
    aoCarregarAgenda: (de: String, ate: String, aoTerminar: (AgendaDoGuia?, String?) -> Unit) -> Unit,
) {
    var cursor by remember {
        mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) })
    }
    var dados by remember { mutableStateOf<AgendaDoGuia?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    val fmtApi = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val fmtTituloMes = remember { SimpleDateFormat("MMMM", Locale("pt", "BR")) }
    val fmtTituloAno = remember { SimpleDateFormat("yyyy", Locale("pt", "BR")) }

    LaunchedEffect(cursor) {
        carregando = true
        erro = null
        val ini = (cursor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val fim = (cursor.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        aoCarregarAgenda(fmtApi.format(ini.time), fmtApi.format(fim.time)) { lista, falha ->
            dados = lista
            erro = falha
            carregando = false
        }
    }

    val itens = dados?.itens.orEmpty()
    val porSemana = remember(itens) {
        itens.groupBy { item ->
            semanaDoMes(item.date)
        }.toSortedMap()
    }
    val resumoDias = itens.map { it.date?.take(10) }.filterNotNull().toSet().size
    val resumoPasseios = itens.count { !(it.status ?: "").equals("cancelado", true) }
    val resumoPax = itens.sumOf { it.pax }

    val tituloMes = remember(cursor) {
        val loc = Locale("pt", "BR")
        val mes = fmtTituloMes.format(cursor.time)
        val ano = fmtTituloAno.format(cursor.time)
        // Igual ao web: `capitalize` em "setembro de 2026" → "Setembro De 2026"
        "$mes de $ano".split(" ").joinToString(" ") { parte ->
            parte.replaceFirstChar { if (it.isLowerCase()) it.titlecase(loc) else it.toString() }
        }
    }

    Surface(
        shape = FormaCartao,
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(
                    onClick = {
                        cursor = (cursor.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    },
                    shape = FormaBotao,
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.agenda_anterior))
                }
                Text(
                    text = tituloMes,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedButton(
                    onClick = {
                        cursor = (cursor.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    },
                    shape = FormaBotao,
                ) {
                    Text(stringResource(R.string.agenda_proximo))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            when {
                carregando && dados == null -> GradeEsqueleto()
                erro != null && dados == null -> TextoErro(erro!!)
                porSemana.isEmpty() -> Text(
                    text = stringResource(R.string.agenda_vazia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
                else -> porSemana.forEach { (sem, lista) ->
                    Text(
                        text = stringResource(R.string.agenda_semana, sem).uppercase(Locale("pt", "BR")),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                    )
                    lista.forEach { s ->
                        ItemAgendaLinha(s)
                    }
                }
            }

            // Igual ao web: só `border-t`, sem linha embaixo dos números
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ResumoAgenda(
                    label = stringResource(R.string.agenda_dias),
                    valor = "$resumoDias",
                    modifier = Modifier.weight(1f),
                )
                ResumoAgenda(
                    label = stringResource(R.string.agenda_passeios),
                    valor = "$resumoPasseios",
                    modifier = Modifier.weight(1f),
                )
                ResumoAgenda(
                    label = stringResource(R.string.agenda_pax),
                    valor = "$resumoPax",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Mesma fórmula do web (`weekOfMonth` em Agenda.tsx). */
private fun semanaDoMes(dateIso: String?): Int {
    val p = dateIso.orEmpty().take(10).split("-").mapNotNull { it.toIntOrNull() }
    if (p.size != 3) return 1
    val cal = Calendar.getInstance().apply { set(p[0], p[1] - 1, p[2]) }
    val first = Calendar.getInstance().apply {
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1)
    }
    // JS getDay(): 0=dom … Calendar.DAY_OF_WEEK: 1=dom → subtrai 1
    val diaSemanaPrimeiro = first.get(Calendar.DAY_OF_WEEK) - 1
    return ceil((cal.get(Calendar.DAY_OF_MONTH) + diaSemanaPrimeiro) / 7.0).toInt().coerceAtLeast(1)
}

@Composable
private fun ResumoAgenda(label: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale("pt", "BR")),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ItemAgendaLinha(s: ItemAgenda) {
    val dataLabel = remember(s.date) {
        val iso = s.date.orEmpty().take(10)
        val p = iso.split("-").mapNotNull { it.toIntOrNull() }
        if (p.size == 3) {
            val cal = Calendar.getInstance().apply { set(p[0], p[1] - 1, p[2]) }
            // Igual ao web: "seg., 14 de set."
            SimpleDateFormat("EEE, dd 'de' MMM", Locale("pt", "BR")).format(cal.time)
                .lowercase(Locale("pt", "BR"))
        } else iso
    }
    val status = (s.status ?: "").lowercase(Locale.US)
    val (badgeBg, badgeFg, badgeTxt) = when (status) {
        "confirmado" -> Triple(CoresExtras.Sucesso, Color.White, "Confirmado")
        "cancelado" -> Triple(MaterialTheme.colorScheme.error, Color.White, "Cancelado")
        else -> Triple(CoresExtras.Aviso, CoresExtras.TextoAviso, "Pendente")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dataLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = s.start_time?.take(5).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = s.tour_name ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = buildString {
                    if (s.pax > 0) append("${s.pax} pax · ")
                    append(s.company ?: "—")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!s.transport.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = s.transport!!,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = badgeBg) {
                    Text(
                        text = badgeTxt,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeFg,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConteudoFicha(
    dados: FichaGuia?,
    carregando: Boolean,
    erro: String?,
) {
    when {
        carregando && dados == null -> GradeEsqueleto()
        erro != null && dados == null -> TextoErro(erro)
        dados != null -> {
            val g = dados.guide
            val e = dados.estatisticas
            val nome = g?.name ?: Sessao.nomeExibicao ?: "—"
            val iniciais = iniciaisFicha(nome)
            val ativo = (g?.status ?: "active").equals("active", ignoreCase = true)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = FormaCartao,
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = iniciais,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = nome,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = if (!g?.cadastur.isNullOrBlank()) {
                                    stringResource(R.string.painel_guia_cadastur_ativo)
                                } else {
                                    stringResource(R.string.painel_guia_turismo)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        // Igual ao web: `border-t` sob "Guia de Turismo"
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CampoFicha(
                                rotulo = stringResource(R.string.painel_ficha_label_cadastur),
                                valor = g?.cadastur?.takeIf { it.isNotBlank() } ?: "—",
                            )
                            CampoFicha(
                                rotulo = stringResource(R.string.painel_ficha_label_telefone),
                                valor = g?.whatsapp?.takeIf { it.isNotBlank() } ?: "—",
                            )
                            CampoFicha(
                                rotulo = stringResource(R.string.painel_ficha_label_email),
                                valor = g?.email?.takeIf { it.isNotBlank() } ?: "—",
                            )
                            CampoFicha(
                                rotulo = stringResource(R.string.painel_ficha_label_entrada),
                                valor = formatarDataBr(g?.data_entrada),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (ativo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Text(
                                text = stringResource(
                                    if (ativo) R.string.painel_ficha_ativo else R.string.painel_ficha_inativo,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ativo) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (!g?.observacoes.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = g!!.observacoes!!,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (e != null) {
                    FlowRowKpisFicha(
                        itens = listOf(
                            stringResource(R.string.painel_ficha_tempo) to
                                (e.tempoEmpresa ?: "—"),
                            stringResource(R.string.painel_ficha_passeios_hist) to
                                "${e.totalPasseios}",
                            stringResource(R.string.painel_ficha_avaliacao) to
                                (e.avaliacaoMedia?.let { String.format(Locale("pt", "BR"), "%.1f", it) } ?: "—"),
                            stringResource(R.string.painel_ficha_pontualidade) to
                                (e.pontualidadePct?.let { "${it.toInt()}%" } ?: "—"),
                            stringResource(R.string.painel_ficha_nivel) to
                                (e.nivel?.replaceFirstChar { it.titlecase(Locale("pt", "BR")) } ?: "—"),
                            stringResource(R.string.painel_ficha_ocorrencias) to
                                "${e.ocorrenciasAno}",
                        ),
                    )
                }

                Surface(
                    shape = FormaCartao,
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = stringResource(R.string.painel_ficha_especialidades_titulo),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (dados.especialidades.isEmpty()) {
                            Text(
                                text = stringResource(R.string.painel_ficha_sem_esp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            dados.especialidades.forEach { esp ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = esp.destino ?: "—",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            text = "${esp.nivel}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = esp.nivel.coerceIn(0, 100) / 100f,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> GradeEsqueleto()
    }
}

@Composable
private fun CampoFicha(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FlowRowKpisFicha(itens: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itens.chunked(2).forEach { par ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                par.forEach { (label, valor) ->
                    Surface(
                        shape = FormaCartao,
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = label.uppercase(Locale("pt", "BR")),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = valor,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                if (par.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun iniciaisFicha(nome: String?): String {
    if (nome.isNullOrBlank()) return "G"
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (partes.isEmpty()) return "G"
    val a = partes.first().firstOrNull()?.uppercaseChar() ?: return "G"
    val b = if (partes.size > 1) partes.last().firstOrNull()?.uppercaseChar() else null
    return if (b != null) "$a$b" else "$a"
}

@Composable
private fun KpiCard(
    label: String,
    valor: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    destaque: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val fundo = if (destaque) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surface
    val borda = if (destaque) Color(0xFFFCD34D) else MaterialTheme.colorScheme.outlineVariant
    Surface(
        shape = FormaCartao,
        color = fundo,
        border = BorderStroke(1.dp, borda),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label.uppercase(Locale("pt", "BR")),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BlocoListaAgenda(
    titulo: String,
    itens: List<ItemAgenda>,
    mostrarVazio: Boolean = true,
) {
    Surface(
        shape = FormaCartao,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = titulo,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (itens.isEmpty()) {
                if (mostrarVazio) {
                    Text(
                        text = stringResource(R.string.painel_sem_agenda),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    )
                }
            } else {
                itens.take(12).forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(
                            text = item.tour_name ?: "—",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                formatarDataBr(item.date),
                                item.start_time?.take(5),
                                item.transport,
                                if (item.pax > 0) "${item.pax} pax" else null,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaPasseiosDialog(
    itens: List<PasseioListaItem>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.painel_passeios_realizados, itens.size))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (itens.isEmpty()) {
                    Text(
                        text = stringResource(R.string.painel_sem_passeios),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    itens.forEach { p ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = p.tour_name ?: "—",
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = listOfNotNull(
                                    formatarDataBr(p.date),
                                    p.start_time?.take(5),
                                    p.transport,
                                    p.company,
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        },
        shape = FormaCartao,
    )
}

@Composable
private fun GradeEsqueleto() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(FormaCartao)
                    .background(Color(0xFFE8E8E4)),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(FormaCartao)
                    .background(Color(0xFFE8E8E4)),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(FormaCartao)
                    .background(Color(0xFFE8E8E4)),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(88.dp)
                    .clip(FormaCartao)
                    .background(Color(0xFFE8E8E4)),
            )
        }
    }
}

@Composable
private fun TextoErro(msg: String) {
    Text(
        text = msg,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 16.dp),
    )
}

@Composable
private fun TextoVazio(msg: String) {
    Text(
        text = msg,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}

private fun formatarBrl(valor: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    fmt.currency = Currency.getInstance("BRL")
    fmt.maximumFractionDigits = 0
    return fmt.format(valor)
}

private fun formatarDataBr(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val p = iso.take(10).split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
}

private fun intervaloDoPeriodo(periodo: PeriodoPainelUi): Pair<String, String> {
    val cal = Calendar.getInstance()
    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return when (periodo) {
        PeriodoPainelUi.MES -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val de = fmt.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            de to fmt.format(cal.time)
        }
        PeriodoPainelUi.SEMANA -> {
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val de = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 6)
            de to fmt.format(cal.time)
        }
        PeriodoPainelUi.ACUMULADO -> {
            cal.set(Calendar.DAY_OF_YEAR, 1)
            val de = fmt.format(cal.time)
            de to fmt.format(Calendar.getInstance().time)
        }
    }
}

private fun diasAteAniversario(iso: String?): Int? {
    if (iso.isNullOrBlank()) return null
    val p = iso.split("-").mapNotNull { it.toIntOrNull() }
    if (p.size < 3) return null
    val m = p[1]
    val d = p[2]
    val hoje = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val prox = Calendar.getInstance().apply {
        set(Calendar.MONTH, m - 1)
        set(Calendar.DAY_OF_MONTH, d)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (prox.before(hoje)) prox.add(Calendar.YEAR, 1)
    val diff = (prox.timeInMillis - hoje.timeInMillis) / (1000L * 60 * 60 * 24)
    return diff.toInt()
}

private fun idadeDe(iso: String?): Int? {
    if (iso.isNullOrBlank()) return null
    val p = iso.split("-").mapNotNull { it.toIntOrNull() }
    if (p.size < 3) return null
    val y = p[0]
    val m = p[1]
    val d = p[2]
    val hoje = Calendar.getInstance()
    var idade = hoje.get(Calendar.YEAR) - y
    val anivEsteAno = Calendar.getInstance().apply {
        set(Calendar.YEAR, hoje.get(Calendar.YEAR))
        set(Calendar.MONTH, m - 1)
        set(Calendar.DAY_OF_MONTH, d)
    }
    if (hoje.before(anivEsteAno)) idade--
    return idade
}
