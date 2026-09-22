package br.com.rio40graus.guiascale

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import br.com.rio40graus.guiascale.dados.BancoLocal
import br.com.rio40graus.guiascale.dados.Posicao
import br.com.rio40graus.guiascale.rastreio.EstadoRastreio
import br.com.rio40graus.guiascale.rastreio.RastreioService
import br.com.rio40graus.guiascale.rede.AgendaDoGuia
import br.com.rio40graus.guiascale.rede.BloqueioGuia
import br.com.rio40graus.guiascale.rede.DiasSemanaGuia
import br.com.rio40graus.guiascale.rede.FichaGuia
import br.com.rio40graus.guiascale.rede.FormaPagamento
import br.com.rio40graus.guiascale.rede.IdiomaOpcao
import br.com.rio40graus.guiascale.rede.ItemPagamento
import br.com.rio40graus.guiascale.rede.ItemPagamentoFornecedor
import br.com.rio40graus.guiascale.rede.KpisDoPainel
import br.com.rio40graus.guiascale.rede.LogoDaAgencia
import br.com.rio40graus.guiascale.rede.MapaEmbarque
import br.com.rio40graus.guiascale.rede.OcorrenciaMapa
import br.com.rio40graus.guiascale.rede.ParcelaOpcao
import br.com.rio40graus.guiascale.rede.PasseiosDoGuia
import br.com.rio40graus.guiascale.rede.PedidoBloqueios
import br.com.rio40graus.guiascale.rede.PedidoIdioma
import br.com.rio40graus.guiascale.rede.PedidoLogin
import br.com.rio40graus.guiascale.rede.PedidoOcorrencia
import br.com.rio40graus.guiascale.rede.PedidoPagamentos
import br.com.rio40graus.guiascale.rede.PedidoPagamentosFornecedor
import br.com.rio40graus.guiascale.rede.PedidoRemoverBloqueios
import br.com.rio40graus.guiascale.rede.PedidoSolicitacoes
import br.com.rio40graus.guiascale.rede.PedidoStatus
import br.com.rio40graus.guiascale.rede.PedidoChecklistFeito
import br.com.rio40graus.guiascale.rede.ProgressoDaLei
import br.com.rio40graus.guiascale.rede.Rede
import br.com.rio40graus.guiascale.rede.RespostaMotivos
import br.com.rio40graus.guiascale.rede.STATUS_CHECK_IN
import br.com.rio40graus.guiascale.rede.Sessao
import br.com.rio40graus.guiascale.rede.SolicitacaoTrabalho
import br.com.rio40graus.guiascale.rede.TarefaChecklist
import br.com.rio40graus.guiascale.rede.mensagemDeErro
import br.com.rio40graus.guiascale.rede.nomeDoGuia
import br.com.rio40graus.guiascale.rede.usuIdDoGuia
import br.com.rio40graus.guiascale.ui.tema.CoresExtras
import br.com.rio40graus.guiascale.ui.tema.FormaBotao
import br.com.rio40graus.guiascale.ui.tema.FormaCampo
import br.com.rio40graus.guiascale.ui.tema.FormaCartao
import br.com.rio40graus.guiascale.ui.tema.TemaGuiaScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * A única tela do app, por enquanto.
 *
 * Login, o botão que liga o rastreio e o estado da fila. Nada de mapa nem de
 * embarques: isso o guia já tem no app web, e este existe para a função que a
 * web não faz — capturar com o aparelho no bolso.
 *
 * A aparência acompanha a do app web (ver ui/tema/): mesma paleta, mesmas duas
 * fontes e o mesmo desenho de tela de entrada. É o mesmo guia usando os dois
 * no mesmo dia, e a troca não deve parecer troca de sistema.
 */
class MainActivity : ComponentActivity() {

    /*
     * As permissões vêm em duas etapas, e a ordem não é escolha nossa: o
     * Android recusa o pedido de "o tempo todo" se a permissão comum ainda não
     * foi concedida. Por isso são dois lançadores, um chamando o outro.
     */
    private val pedirLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidas ->
        if (concedidas[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            pedirSegundoPlano()
        }
    }

    private val pedirEmSegundoPlano = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* o estado é relido pela tela */ }

    private val pedirNotificacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sem ela o serviço roda, mas o guia não vê o aviso */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
         * A partir do Android 15 o sistema desenha por baixo das barras quer o
         * app peça, quer não. Declarar aqui é o que permite tratar isso com
         * safeDrawingPadding lá embaixo, em vez de o título nascer colado no
         * relógio.
         */
        enableEdgeToEdge()
        Sessao.carregar(this)
        if (Sessao.autenticado && Sessao.nome.isNullOrBlank()) {
            atualizarPerfil()
        }

        setContent {
            TemaGuiaScale {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Tela(
                        aoEntrar = ::entrar,
                        aoSair = ::sair,
                        aoCarregarMapas = ::carregarMapas,
                        aoCarregarTrajeto = ::carregarTrajeto,
                        aoCheckIn = ::checkIn,
                        aoCarregarMotivos = ::carregarMotivos,
                        aoCarregarFormas = ::carregarFormas,
                        aoCarregarParcelas = ::carregarParcelas,
                        aoCarregarIdiomas = ::carregarIdiomas,
                        aoSalvarPagamentos = ::salvarPagamentos,
                        aoSalvarIdioma = ::salvarIdioma,
                        aoCarregarOcorrencias = ::carregarOcorrencias,
                        aoCriarOcorrencia = ::criarOcorrencia,
                        aoEditarOcorrencia = ::editarOcorrencia,
                        aoExcluirOcorrencia = ::excluirOcorrencia,
                        aoCarregarChecklist = ::carregarChecklist,
                        aoMarcarChecklist = ::marcarChecklist,
                        aoCarregarPainel = ::carregarPainel,
                        aoCarregarPasseiosGuia = ::carregarPasseiosGuia,
                        aoCarregarAgenda = ::carregarAgenda,
                        aoCarregarFicha = ::carregarFicha,
                        aoCarregarLei = ::carregarLei,
                        aoSalvarLei = ::salvarLei,
                        aoCarregarBloqueios = ::carregarBloqueios,
                        aoCarregarDiasSemana = ::carregarDiasSemana,
                        aoCarregarDiasOcupados = ::carregarDiasOcupados,
                        aoSalvarBloqueios = ::salvarBloqueios,
                        aoRemoverBloqueios = ::removerBloqueios,
                        aoCarregarSolicitacoes = ::carregarSolicitacoes,
                        aoCriarSolicitacoes = ::criarSolicitacoes,
                        aoSalvarPagamentoFornecedor = ::salvarPagamentoFornecedor,
                        aoCarregarNomeGuia = ::carregarNomeGuia,
                        aoPedirPermissoes = ::pedirPermissoes,
                        aoLigar = { mapaId -> RastreioService.iniciar(this, mapaId) },
                        aoDesligar = { RastreioService.parar(this) },
                        aoAbrirBateria = ::abrirIsencaoDeBateria,
                    )
                }
            }
        }
    }

    private fun entrar(login: String, senha: String, aoTerminar: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.login(PedidoLogin(login = login, senha = senha))
                val token = resposta.access_token

                if (token.isNullOrBlank()) {
                    aoTerminar(getString(R.string.erro_login))
                } else {
                    Sessao.guardar(
                        this@MainActivity,
                        token,
                        login,
                        nomeDoGuia(resposta.user, resposta.guia),
                        usuIdDoGuia(resposta.user, resposta.guia),
                    )
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    /** Preenche o nome do guia quando a sessão antiga só tinha o login/e-mail. */
    private fun atualizarPerfil() {
        lifecycleScope.launch {
            try {
                val me = Rede.api.me()
                Sessao.guardarPerfil(
                    this@MainActivity,
                    nomeDoGuia(me.user, me.guia),
                    usuIdDoGuia(me.user, me.guia),
                )
            } catch (_: Exception) {
                // Sem nome o app continua; só exibe o login.
            }
        }
    }

    private fun carregarNomeGuia(aoTerminar: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val me = Rede.api.me()
                Sessao.guardarPerfil(
                    this@MainActivity,
                    nomeDoGuia(me.user, me.guia),
                    usuIdDoGuia(me.user, me.guia),
                )
                aoTerminar(Sessao.nomeExibicao)
            } catch (_: Exception) {
                aoTerminar(Sessao.nomeExibicao)
            }
        }
    }

    /**
     * Sair.
     *
     * A ordem importa. A captura é desligada ANTES de o token sumir: um serviço
     * em primeiro plano que continue gravando sem sessão enche a fila com
     * pontos que o envio vai recusar para sempre, e a notificação permanente
     * ficaria na barra de um guia que acha que saiu.
     *
     * O aviso ao servidor é o último passo e o único dispensável. Ele falha
     * sempre que não há sinal — que é metade do dia de trabalho — e nada disso
     * pode impedir alguém de sair do próprio app. O token expira sozinho.
     */
    private fun sair(aoTerminar: () -> Unit) {
        lifecycleScope.launch {
            RastreioService.parar(this@MainActivity)
            runCatching { Rede.api.logout() }
            Sessao.limpar(this@MainActivity)
            aoTerminar()
        }
    }

    private fun carregarMapas(data: String, aoTerminar: (List<MapaEmbarque>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.mapaEmbarque(data).mapas, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    /**
     * Trajeto já no servidor, um por mapa — cada embarque tem o seu rastro.
     */
    private fun carregarTrajeto(
        data: String,
        mapaIds: List<Int>,
        aoTerminar: (List<Pair<Double, Double>>?, String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                if (mapaIds.isEmpty()) {
                    aoTerminar(emptyList(), null)
                    return@launch
                }
                val pontos = mapaIds.flatMap { mapaId ->
                    Rede.api.listarPosicoes(data = data, mapaId = mapaId)
                        .posicoes
                        .map { it.latitude to it.longitude }
                }
                aoTerminar(pontos, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    /**
     * Troca de status no embarque (check-in, no-show ou parcial).
     *
     * O primeiro CHECK-IN do dia liga o rastreio (se ainda não estiver ligado
     * pelo "Iniciar embarque"), carimbando o mapa da reserva. Do primeiro
     * embarque em diante, cada ponto sai vinculado a esse mapa.
     */
    private fun checkIn(
        reservaId: Int,
        mapaId: Int,
        statusId: Int,
        motivoId: Int?,
        parcial: Map<String, Int>?,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val corpo = PedidoStatus(
                    status_id = statusId,
                    motivo_id = motivoId,
                    adulto = parcial?.get("adulto"),
                    chd = parcial?.get("chd"),
                    infantil = parcial?.get("infantil"),
                    jovem = parcial?.get("jovem"),
                    idoso = parcial?.get("idoso"),
                )
                val resposta = Rede.api.trocarStatus(reservaId, corpo)

                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                    return@launch
                }

                if (statusId == STATUS_CHECK_IN && !EstadoRastreio.ativo(this@MainActivity)) {
                    pedirPermissoes()
                    RastreioService.iniciar(this@MainActivity, mapaId)
                }

                aoTerminar(null)
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarMotivos(aoTerminar: (RespostaMotivos?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.statusMotivos(), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarFormas(aoTerminar: (List<FormaPagamento>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.formasPagamento().formas, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarParcelas(aoTerminar: (List<ParcelaOpcao>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.parcelas().parcelas, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarIdiomas(aoTerminar: (List<IdiomaOpcao>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.idiomas().idiomas, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun salvarPagamentos(
        reservaId: Int,
        itens: List<ItemPagamento>,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.salvarPagamentos(reservaId, PedidoPagamentos(itens))
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun salvarPagamentoFornecedor(
        acertoFornecedorId: Int,
        itens: List<ItemPagamentoFornecedor>,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.salvarPagamentosFornecedor(
                    acertoFornecedorId,
                    PedidoPagamentosFornecedor(itens),
                )
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun salvarIdioma(
        reservaId: Int,
        idiomaId: Int,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.salvarIdioma(reservaId, PedidoIdioma(idiomaId))
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarOcorrencias(
        mapaId: Int,
        aoTerminar: (List<OcorrenciaMapa>?, String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.listarOcorrencias(mapaId).ocorrencias, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun criarOcorrencia(
        mapaId: Int,
        relato: String,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.criarOcorrencia(mapaId, PedidoOcorrencia(relato))
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun editarOcorrencia(
        id: Int,
        relato: String,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.editarOcorrencia(id, PedidoOcorrencia(relato))
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun excluirOcorrencia(
        id: Int,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.excluirOcorrencia(id)
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarChecklist(
        tourId: Int,
        data: String,
        mapaId: Int?,
        aoTerminar: (List<TarefaChecklist>?, String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                aoTerminar(
                    Rede.api.checklist(tourId = tourId, data = data, mapaId = mapaId).tarefas,
                    null,
                )
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun marcarChecklist(
        tarefaId: String,
        mapaId: Int,
        data: String,
        done: Boolean,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.marcarChecklist(
                    tarefaId,
                    PedidoChecklistFeito(mapa_id = mapaId, data = data, done = done),
                )
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarPainel(periodo: String, aoTerminar: (KpisDoPainel?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.painel(periodo), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarPasseiosGuia(periodo: String, aoTerminar: (PasseiosDoGuia?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.passeiosGuia(periodo), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarAgenda(de: String, ate: String, aoTerminar: (AgendaDoGuia?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.agenda(de, ate), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarFicha(aoTerminar: (FichaGuia?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.ficha(), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarLei(aoTerminar: (ProgressoDaLei?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.leiProgresso(), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun salvarLei(dados: ProgressoDaLei, aoTerminar: (Boolean) -> Unit) {
        lifecycleScope.launch {
            try {
                Rede.api.salvarLeiProgresso(dados)
                aoTerminar(true)
            } catch (_: Exception) {
                aoTerminar(false)
            }
        }
    }

    private fun carregarBloqueios(from: String?, aoTerminar: (List<BloqueioGuia>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.listarBloqueios(from).bloqueios, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarDiasSemana(aoTerminar: (DiasSemanaGuia?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.diasSemana(), null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarDiasOcupados(from: String?, aoTerminar: (List<String>?, String?) -> Unit) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.diasOcupados(from).dias, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun salvarBloqueios(
        datas: List<String>,
        motivo: String?,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                Rede.api.criarBloqueios(
                    PedidoBloqueios(dates = datas, reason = motivo, source = "app-guia"),
                )
                aoTerminar(null)
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun removerBloqueios(datas: List<String>, aoTerminar: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val resposta = Rede.api.removerBloqueios(PedidoRemoverBloqueios(dates = datas))
                if (!resposta.isSuccessful) {
                    aoTerminar(mensagemDeErro(this@MainActivity, resposta))
                } else {
                    aoTerminar(null)
                }
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun carregarSolicitacoes(
        from: String?,
        aoTerminar: (List<SolicitacaoTrabalho>?, String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                aoTerminar(Rede.api.listarSolicitacoes(from).solicitacoes, null)
            } catch (erro: Exception) {
                aoTerminar(null, mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun criarSolicitacoes(
        datas: List<String>,
        motivo: String?,
        aoTerminar: (String?) -> Unit,
    ) {
        lifecycleScope.launch {
            try {
                Rede.api.criarSolicitacoes(
                    PedidoSolicitacoes(dates = datas, reason = motivo, source = "app-guia"),
                )
                aoTerminar(null)
            } catch (erro: Exception) {
                aoTerminar(mensagemDeErro(this@MainActivity, erro))
            }
        }
    }

    private fun pedirPermissoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        pedirLocalizacao.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    private fun pedirSegundoPlano() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (temPermissaoDeFundo(this)) return

        /*
         * A partir do Android 11 o sistema NÃO mostra mais o diálogo aqui: ele
         * manda o usuário para as configurações do app, onde a opção se chama
         * "Permitir o tempo todo". O lançador continua sendo o caminho certo —
         * é ele que abre essa tela.
         */
        pedirEmSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    /**
     * A tela de economia de bateria do fabricante.
     *
     * Samsung e Xiaomi matam serviço legítimo para poupar energia, e é a causa
     * mais comum de rastro interrompido no meio do dia. Não dá para resolver em
     * código: só levando o guia até a configuração.
     */
    private fun abrirIsencaoDeBateria() {
        val intencao = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        runCatching { startActivity(intencao) }
    }

    companion object {
        fun temPermissaoDeFundo(contexto: Context): Boolean =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                true
            } else {
                ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            }
    }
}

/**
 * Espaço reservado embaixo de cada tela.
 *
 * O menu inferior agora entra no fluxo do layout (não sobrepõe o conteúdo),
 * então só sobra uma folga pequena para o último item não colar na borda.
 */
internal val ESPACO_DA_BARRA = 12.dp

@Composable
private fun Tela(
    aoEntrar: (String, String, (String?) -> Unit) -> Unit,
    aoSair: (() -> Unit) -> Unit,
    aoCarregarMapas: (String, (List<MapaEmbarque>?, String?) -> Unit) -> Unit,
    aoCarregarTrajeto: (String, List<Int>, (List<Pair<Double, Double>>?, String?) -> Unit) -> Unit,
    aoCheckIn: (Int, Int, Int, Int?, Map<String, Int>?, (String?) -> Unit) -> Unit,
    aoCarregarMotivos: ((RespostaMotivos?, String?) -> Unit) -> Unit,
    aoCarregarFormas: ((List<FormaPagamento>?, String?) -> Unit) -> Unit,
    aoCarregarParcelas: ((List<ParcelaOpcao>?, String?) -> Unit) -> Unit,
    aoCarregarIdiomas: ((List<IdiomaOpcao>?, String?) -> Unit) -> Unit,
    aoSalvarPagamentos: (Int, List<ItemPagamento>, (String?) -> Unit) -> Unit,
    aoSalvarIdioma: (Int, Int, (String?) -> Unit) -> Unit,
    aoCarregarOcorrencias: (Int, (List<OcorrenciaMapa>?, String?) -> Unit) -> Unit,
    aoCriarOcorrencia: (Int, String, (String?) -> Unit) -> Unit,
    aoEditarOcorrencia: (Int, String, (String?) -> Unit) -> Unit,
    aoExcluirOcorrencia: (Int, (String?) -> Unit) -> Unit,
    aoCarregarChecklist: (Int, String, Int?, (List<TarefaChecklist>?, String?) -> Unit) -> Unit,
    aoMarcarChecklist: (String, Int, String, Boolean, (String?) -> Unit) -> Unit,
    aoCarregarPainel: (String, (KpisDoPainel?, String?) -> Unit) -> Unit,
    aoCarregarPasseiosGuia: (String, (PasseiosDoGuia?, String?) -> Unit) -> Unit,
    aoCarregarAgenda: (String, String, (AgendaDoGuia?, String?) -> Unit) -> Unit,
    aoCarregarFicha: ((FichaGuia?, String?) -> Unit) -> Unit,
    aoCarregarLei: ((ProgressoDaLei?, String?) -> Unit) -> Unit,
    aoSalvarLei: (ProgressoDaLei, (Boolean) -> Unit) -> Unit,
    aoCarregarBloqueios: (String?, (List<BloqueioGuia>?, String?) -> Unit) -> Unit,
    aoCarregarDiasSemana: ((DiasSemanaGuia?, String?) -> Unit) -> Unit,
    aoCarregarDiasOcupados: (String?, (List<String>?, String?) -> Unit) -> Unit,
    aoSalvarBloqueios: (List<String>, String?, (String?) -> Unit) -> Unit,
    aoRemoverBloqueios: (List<String>, (String?) -> Unit) -> Unit,
    aoCarregarSolicitacoes: (String?, (List<SolicitacaoTrabalho>?, String?) -> Unit) -> Unit,
    aoCriarSolicitacoes: (List<String>, String?, (String?) -> Unit) -> Unit,
    aoSalvarPagamentoFornecedor: (Int, List<ItemPagamentoFornecedor>, (String?) -> Unit) -> Unit,
    aoCarregarNomeGuia: ((String?) -> Unit) -> Unit,
    aoPedirPermissoes: () -> Unit,
    aoLigar: (mapaId: Int?) -> Unit,
    aoDesligar: () -> Unit,
    aoAbrirBateria: () -> Unit,
) {
    val contexto = LocalContext.current

    var login by remember { mutableStateOf(Sessao.login ?: "") }
    var senha by remember { mutableStateOf("") }
    var autenticado by remember { mutableStateOf(Sessao.autenticado) }
    var entrando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    var aba by remember { mutableStateOf(Aba.PAINEL) }
    // Onde o guia está agora, e por onde já passou. Os dois vão para a tela de
    // embarque: a posição vira a distância até o próximo ponto, o trajeto vira
    // a linha azul no mapa (filtrada por mapa na TelaEmbarque).
    var minhaPosicao by remember { mutableStateOf<android.location.Location?>(null) }
    var trajeto by remember { mutableStateOf<List<Posicao>>(emptyList()) }
    var confirmandoSaida by remember { mutableStateOf(false) }
    var saindo by remember { mutableStateOf(false) }

    // Lido do EstadoRastreio, e não guardado só aqui: o rastreio pode ter sido
    // ligado pelo check-in, ou continuar de uma sessão anterior do app.
    var rastreando by remember { mutableStateOf(EstadoRastreio.ativo(contexto)) }
    var pendentes by remember { mutableStateOf(0) }
    var podeEmSegundoPlano by remember { mutableStateOf(MainActivity.temPermissaoDeFundo(contexto)) }

    // Enquanto a tela estiver aberta, mostra a fila andando. É o único jeito de
    // o guia saber que o rastreio está de fato gravando.
    LaunchedEffect(rastreando) {
        while (true) {
            val dao = BancoLocal.obter(contexto).posicoes()
            pendentes = dao.quantasPendentes()
            podeEmSegundoPlano = MainActivity.temPermissaoDeFundo(contexto)
            rastreando = EstadoRastreio.ativo(contexto)

            /*
             * O trajeto sai da fila local, e não da API: o ponto está aqui
             * assim que o GPS o entrega, enquanto no servidor só aparece
             * depois que o envio consegue subir — e é no trecho sem sinal que
             * o guia mais quer ver por onde andou.
             */
            trajeto = dao.desde(inicioDoDia())

            // Posição do pino/distância: ao vivo mesmo com embarque parado.
            // Só o RastreioService grava no banco — aqui é só leitura.
            minhaPosicao = posicaoParaExibicao(contexto, rastreando)
            delay(if (rastreando) 3_000 else 5_000)
        }
    }

    /*
     * O fundo em degradê é o mesmo do Login.tsx: parte do creme do app e cai
     * para o azul do acento nos 40% finais, no sentido da diagonal.
     */
    val degrade = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(degrade),
    ) {
        if (!autenticado) {
            CartaoDeEntrada(
                login = login,
                senha = senha,
                entrando = entrando,
                erro = erro,
                aoMudarLogin = { login = it; erro = null },
                aoMudarSenha = { senha = it; erro = null },
                aoConfirmar = {
                    entrando = true
                    erro = null
                    aoEntrar(login, senha) { falha ->
                        entrando = false
                        erro = falha
                        autenticado = falha == null
                    }
                },
            )
        } else {
            // Header no topo (como o web celular) + conteúdo + menu inferior.
            // Conta: avatar à direita abre menu com Sair (igual ao web).
            var nomeCabecalho by remember {
                mutableStateOf(Sessao.nomeExibicao ?: "")
            }
            LaunchedEffect(Unit) {
                aoCarregarNomeGuia { nome ->
                    nomeCabecalho = nome?.takeIf { it.isNotBlank() }
                        ?: Sessao.nomeExibicao
                        ?: ""
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                CabecalhoTopo(
                    titulo = when (aba) {
                        Aba.PAINEL -> stringResource(R.string.app_name)
                        Aba.CHECK_LIST -> stringResource(R.string.checklist_titulo)
                        Aba.EMBARQUE -> stringResource(R.string.app_name)
                        Aba.CHECK_IN -> stringResource(R.string.app_name)
                        else -> stringResource(R.string.app_name)
                    },
                    nomeGuia = nomeCabecalho,
                    aoPedirSaida = { confirmandoSaida = true },
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (aba) {
                        Aba.PAINEL -> TelaPainel(
                            aoCarregarPainel = aoCarregarPainel,
                            aoCarregarPasseios = aoCarregarPasseiosGuia,
                            aoCarregarAgenda = aoCarregarAgenda,
                            aoCarregarFicha = aoCarregarFicha,
                            aoCarregarLei = aoCarregarLei,
                            aoSalvarLei = aoSalvarLei,
                            aoCarregarBloqueios = aoCarregarBloqueios,
                            aoCarregarDiasSemana = aoCarregarDiasSemana,
                            aoCarregarDiasOcupados = aoCarregarDiasOcupados,
                            aoSalvarBloqueios = aoSalvarBloqueios,
                            aoRemoverBloqueios = aoRemoverBloqueios,
                            aoCarregarSolicitacoes = aoCarregarSolicitacoes,
                            aoCriarSolicitacoes = aoCriarSolicitacoes,
                            aoAbrirEmbarque = { aba = Aba.EMBARQUE },
                        )

                        Aba.EMBARQUE -> TelaMapaEmbarque(
                            aoCarregar = aoCarregarMapas,
                            aoTrocarStatus = aoCheckIn,
                            aoCarregarMotivos = aoCarregarMotivos,
                            aoCarregarFormas = aoCarregarFormas,
                            aoCarregarParcelas = aoCarregarParcelas,
                            aoCarregarIdiomas = aoCarregarIdiomas,
                            aoSalvarPagamentos = aoSalvarPagamentos,
                            aoSalvarIdioma = aoSalvarIdioma,
                            aoCarregarOcorrencias = aoCarregarOcorrencias,
                            aoCriarOcorrencia = aoCriarOcorrencia,
                            aoEditarOcorrencia = aoEditarOcorrencia,
                            aoExcluirOcorrencia = aoExcluirOcorrencia,
                            aoSalvarPagamentoFornecedor = aoSalvarPagamentoFornecedor,
                            aoCarregarNomeGuia = aoCarregarNomeGuia,
                        )

                        Aba.CHECK_LIST -> TelaCheckList(
                            aoCarregarMapas = aoCarregarMapas,
                            aoCarregarChecklist = aoCarregarChecklist,
                            aoMarcarChecklist = aoMarcarChecklist,
                        )

                        Aba.CHECK_IN -> TelaEmbarque(
                            aoCarregar = aoCarregarMapas,
                            aoTrocarStatus = aoCheckIn,
                            aoCarregarMotivos = aoCarregarMotivos,
                            aoCarregarFormas = aoCarregarFormas,
                            aoCarregarParcelas = aoCarregarParcelas,
                            aoCarregarIdiomas = aoCarregarIdiomas,
                            aoSalvarPagamentos = aoSalvarPagamentos,
                            aoSalvarIdioma = aoSalvarIdioma,
                            minhaPosicao = minhaPosicao,
                            trajetoLocal = trajeto,
                            aoPedirLocalizacao = aoPedirPermissoes,
                            embarqueAtivo = rastreando,
                            aoAlternarEmbarque = { mapaId ->
                                if (rastreando) {
                                    aoDesligar()
                                    rastreando = false
                                } else {
                                    aoPedirPermissoes()
                                    aoLigar(mapaId)
                                    rastreando = true
                                }
                            },
                            aoCarregarTrajeto = aoCarregarTrajeto,
                        )

                        else -> { /* abas desabilitadas / Conta no menu do avatar */ }
                    }
                }

                NavInferior(
                    atual = aba,
                    aoTrocar = { aba = it },
                )
            }
        }
    }

    if (confirmandoSaida) {
        AlertDialog(
            onDismissRequest = { confirmandoSaida = false },
            title = { Text(stringResource(R.string.sair_titulo)) },
            text = { Text(stringResource(R.string.sair_texto)) },
            shape = FormaCartao,
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoSaida = false
                        saindo = true
                        aoSair {
                            saindo = false
                            // Volta ao estado de quem nunca entrou: o próximo
                            // guia a abrir o aparelho não pode herdar nada.
                            autenticado = false
                            rastreando = false
                            login = ""
                            senha = ""
                            erro = null
                            aba = Aba.PAINEL
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.sair),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoSaida = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            },
        )
    }
}

/**
 * Abas iguais ao BottomNav do web (Painel…Radar).
 * Conta fica no avatar do header (menu com Sair), não neste menu.
 *
 * Embarque = Mapa de embarque do web; Check-in = Geocheck-in.
 * Radar fica desabilitado por enquanto.
 */
private enum class Aba(
    val rotulo: Int,
    val icone: ImageVector,
    val habilitada: Boolean,
    val noMenuInferior: Boolean = true,
) {
    PAINEL(R.string.aba_painel, Icons.Filled.Speed, true),
    EMBARQUE(R.string.aba_embarque, Icons.Filled.PersonPin, true),
    CHECK_LIST(R.string.aba_check_list, Icons.Filled.Assignment, true),
    CHECK_IN(R.string.aba_check_in_nav, Icons.Filled.MyLocation, true),
    RADAR(R.string.aba_radar, Icons.Filled.WbSunny, false),
    CONTA(R.string.aba_conta, Icons.Filled.Person, true, noMenuInferior = false),
}

/**
 * Menu inferior igual ao web no celular:
 * fundo azul médio, item ativo azul mais escuro em pílula compacta.
 */
@Composable
private fun NavInferior(
    atual: Aba,
    aoTrocar: (Aba) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    // Tom médio como o glass-dark do web sobre fundo claro.
    val fundoMenu = Color(0xFF355EA3)
    // Ativo mais escuro (--sidebar-accent).
    val fundoAtivo = Color(0xFF163E79)
    val corAtiva = Color.White
    val corInativa = Color.White.copy(alpha = 0.70f)
    val corDesabilitada = Color.White.copy(alpha = 0.40f)

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 12.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = fundoMenu,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Aba.entries.filter { it.noMenuInferior }.forEach { item ->
                val ativo = item == atual && item.habilitada
                val cor = when {
                    !item.habilitada -> corDesabilitada
                    ativo -> corAtiva
                    else -> corInativa
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (ativo) fundoAtivo else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (item.habilitada) {
                                    aoTrocar(item)
                                } else {
                                    Toast.makeText(
                                        contexto,
                                        contexto.getString(R.string.aba_em_breve),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icone,
                            contentDescription = null,
                            tint = cor,
                            modifier = Modifier.size(if (ativo) 22.dp else 20.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.rotulo),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = cor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabeçalho igual ao Header do web no celular.
 * Sem hamburger. Avatar à direita abre o menu (nome, em breve, Sair).
 */
@Composable
private fun CabecalhoTopo(
    titulo: String,
    nomeGuia: String,
    aoPedirSaida: () -> Unit,
) {
    val dataHoje = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
    }
    val nome = nomeGuia.ifBlank { Sessao.nomeExibicao ?: stringResource(R.string.conta_sem_nome) }
    val iniciais = remember(nome) { iniciaisDoNome(nome.ifBlank { Sessao.login }) }
    var menuAberto by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dataHoje,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { menuAberto = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = iniciais,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    DropdownMenu(
                        expanded = menuAberto,
                        onDismissRequest = { menuAberto = false },
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .widthIn(min = 200.dp),
                        ) {
                            Text(
                                text = nome,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Sessao.login?.takeIf { it.isNotBlank() }?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.conta_sair_sistema),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                menuAberto = false
                                aoPedirSaida()
                            },
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
        }
    }
}

private fun iniciaisDoNome(nome: String?): String {
    if (nome.isNullOrBlank()) return "?"
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        partes.size >= 2 ->
            "${partes.first().first()}${partes.last().first()}".uppercase(Locale.getDefault())
        else -> partes.first().take(2).uppercase(Locale.getDefault())
    }
}

/**
 * A marca: o logo da agência, o mesmo arquivo que o app web mostra.
 *
 * Sai do Supabase, que é onde a tela de Configurações do web o grava — ver
 * LogoDaAgencia. Enquanto ele não chega, e também quando a agência ainda não
 * subiu nenhum, fica o alfinete do próprio ícone sobre o azul da marca; o app
 * abre em estrada sem sinal com frequência, e uma tela de entrada que depende
 * de rede para desenhar não serve.
 *
 * O fundo branco e o `ContentScale.Fit` copiam o `variant="mark"` do web, que
 * é `bg-white object-contain`: logo de agência costuma vir com fundo branco
 * embutido, e recortar ou esticar deformaria a marca de quem contratou.
 */
@Composable
internal fun Marca(tamanho: Dp = 64.dp) {
    val contexto = LocalContext.current
    var logo by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        logo = LogoDaAgencia.doCache(contexto)
        LogoDaAgencia.atualizar(contexto)?.let { logo = it }
    }

    val imagem = logo
    Box(
        modifier = Modifier
            .size(tamanho)
            .clip(FormaCartao)
            .background(
                if (imagem != null) Color.White else MaterialTheme.colorScheme.primary
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imagem != null) {
            Image(
                bitmap = imagem,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tamanho * 0.1f),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(tamanho * 0.53f),
            )
        }
    }
}

/**
 * Rótulo acima do campo, como no web.
 *
 * O Material prefere o rótulo flutuando dentro da borda; o app web usa <Label>
 * por fora, em 14px medium. Manter o de fora é o que faz as duas telas se
 * lerem do mesmo jeito.
 */
@Composable
private fun Campo(
    rotulo: String,
    valor: String,
    aoMudar: (String) -> Unit,
    senha: Boolean = false,
    temErro: Boolean = false,
    acaoFinal: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = rotulo, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = valor,
            onValueChange = aoMudar,
            singleLine = true,
            isError = temErro,
            shape = FormaCampo,
            /*
             * Declarar o tipo de teclado não é enfeite: sem isto o Android
             * trata os dois campos como texto comum, com autocorreção ligada, e
             * o IME pode trocar o que foi digitado antes de o app enxergar —
             * numa senha isso vira um "login ou senha incorretos" que o guia
             * não tem como entender, porque na tela estava certo.
             *
             * O tipo Email ainda dá ao teclado a tecla "@" na primeira camada,
             * que é o que os guias digitam.
             */
            keyboardOptions = KeyboardOptions(
                keyboardType = if (senha) KeyboardType.Password else KeyboardType.Email,
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
                imeAction = if (acaoFinal != null) ImeAction.Done else ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onDone = { acaoFinal?.invoke() }),
            visualTransformation = if (senha) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CartaoDeEntrada(
    login: String,
    senha: String,
    entrando: Boolean,
    erro: String?,
    aoMudarLogin: (String) -> Unit,
    aoMudarSenha: (String) -> Unit,
    aoConfirmar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Cartao(modifier = Modifier.widthIn(max = 448.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Marca()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.entrar_titulo),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(R.string.entrar_explicacao),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Campo(
                    rotulo = stringResource(R.string.campo_login),
                    valor = login,
                    aoMudar = aoMudarLogin,
                    temErro = erro != null,
                )

                Campo(
                    rotulo = stringResource(R.string.campo_senha),
                    valor = senha,
                    aoMudar = aoMudarSenha,
                    senha = true,
                    temErro = erro != null,
                    // Enter no teclado entra, sem obrigar a fechar o teclado
                    // para alcançar o botão.
                    acaoFinal = {
                        if (!entrando && login.isNotBlank() && senha.isNotBlank()) aoConfirmar()
                    },
                )

                erro?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                BotaoPrincipal(
                    texto = stringResource(if (entrando) R.string.entrando else R.string.entrar),
                    carregando = entrando,
                    habilitado = !entrando && login.isNotBlank() && senha.isNotBlank(),
                    aoClicar = aoConfirmar,
                )
            }
        }
    }
}

@Composable
private fun Painel(
    rastreando: Boolean,
    pendentes: Int,
    podeEmSegundoPlano: Boolean,
    aoPedirPermissoes: () -> Unit,
    aoAbrirBateria: () -> Unit,
    aoAlternarRastreio: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .padding(bottom = ESPACO_DA_BARRA),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabeçalho com a marca à esquerda, como o header do app web.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Marca(tamanho = 40.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Cartao {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // O ponto colorido é o mesmo sinal de estado do app web:
                    // verde de `--success` quando grava, cinza quando não.
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (rastreando) CoresExtras.Sucesso
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                    )
                    Text(
                        text = stringResource(
                            if (rastreando) R.string.rastreio_ativo else R.string.rastreio_parado
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.fila_pendente, pendentes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!podeEmSegundoPlano) {
            Cartao {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.aviso_segundo_plano),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    BotaoSecundario(
                        texto = stringResource(R.string.liberar_permissao),
                        aoClicar = aoPedirPermissoes,
                    )
                }
            }
        }

        BotaoPrincipal(
            texto = stringResource(
                if (rastreando) R.string.parar_rastreio else R.string.iniciar_rastreio
            ),
            aoClicar = aoAlternarRastreio,
        )

        BotaoSecundario(
            texto = stringResource(R.string.liberar_bateria),
            aoClicar = aoAbrirBateria,
        )
    }
}

/**
 * O cartão do web: branco, canto de 16, borda fina e sombra discreta.
 *
 * A borda existe porque o `--shadow-card` do web é fraco de propósito; sem a
 * linha, o cartão branco sobre o fundo creme quase não se separa do fundo.
 */
@Composable
internal fun Cartao(
    modifier: Modifier = Modifier,
    conteudo: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FormaCartao,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        ),
    ) {
        conteudo()
    }
}

@Composable
internal fun BotaoPrincipal(
    texto: String,
    aoClicar: () -> Unit,
    carregando: Boolean = false,
    habilitado: Boolean = true,
) {
    Button(
        onClick = aoClicar,
        enabled = habilitado,
        shape = FormaBotao,
        // O web não acinzenta o botão inativo: mantém o azul com 50% de
        // opacidade (`disabled:opacity-50`). O Material troca por cinza, o que
        // muda o peso da tela inteira — por isso as cores desligadas também
        // vêm declaradas aqui.
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (carregando) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun BotaoSecundario(
    texto: String,
    aoClicar: () -> Unit,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = aoClicar,
        enabled = habilitado,
        shape = FormaBotao,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text = texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Meia-noite de hoje, em epoch — o recorte do trajeto do dia. */
private fun inicioDoDia(): Long = java.util.Calendar.getInstance().apply {
    set(java.util.Calendar.HOUR_OF_DAY, 0)
    set(java.util.Calendar.MINUTE, 0)
    set(java.util.Calendar.SECOND, 0)
    set(java.util.Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * Posição do guia só para a tela (pino e distância até o próximo ponto).
 *
 * Com o embarque ligado o RastreioService já alimenta o Fused — `lastLocation`
 * basta e não gasta GPS de novo. Com o embarque parado, força
 * `getCurrentLocation` para acompanhar a simulação do emulador / o GPS real
 * sem gravar nada no banco.
 */
private suspend fun posicaoParaExibicao(
    contexto: Context,
    rastreando: Boolean,
): android.location.Location? {
    if (!temPermissaoDeLocalizacao(contexto)) return null
    return if (rastreando) {
        ultimaPosicao(contexto) ?: posicaoAtual(contexto)
    } else {
        posicaoAtual(contexto) ?: ultimaPosicao(contexto)
    }
}

private fun temPermissaoDeLocalizacao(contexto: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/** Cache do Fused — barato; pode ficar velho se ninguém pediu update. */
private suspend fun ultimaPosicao(contexto: Context): android.location.Location? =
    try {
        suspendCancellableCoroutine { continuacao ->
            com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(contexto)
                .lastLocation
                .addOnSuccessListener { continuacao.resume(it) }
                .addOnFailureListener { continuacao.resume(null) }
        }
    } catch (_: SecurityException) {
        null
    }

/** Pedido ativo: atualiza mesmo com o rastreio desligado. */
private suspend fun posicaoAtual(contexto: Context): android.location.Location? =
    try {
        suspendCancellableCoroutine { continuacao ->
            val cancelamento = com.google.android.gms.tasks.CancellationTokenSource()
            continuacao.invokeOnCancellation { cancelamento.cancel() }
            com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(contexto)
                .getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    cancelamento.token,
                )
                .addOnSuccessListener { continuacao.resume(it) }
                .addOnFailureListener { continuacao.resume(null) }
        }
    } catch (_: SecurityException) {
        null
    }
