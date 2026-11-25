package br.com.rio40graus.guiascale.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import br.com.rio40graus.guiascale.R
import br.com.rio40graus.guiascale.mapa.TrajetoNasRuas
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.sp
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Um ponto desenhável no mapa, já sem nada de rede nem de tela.
 */
data class PinoMapa(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val cor: Int,
    val titulo: String,
)

/**
 * O mapa da tela de embarque.
 *
 * Google Maps, pelo maps-compose. Os pinos seguem o formato do app web: gota
 * colorida por status, contorno branco e miolo branco no meio — eles são
 * desenhados aqui em bitmap, e não vêm do Google.
 *
 * O toque no pino não abre a janelinha padrão do Maps: ele apenas avisa quem
 * chamou, e o cartão de detalhes é desenhado em Compose por cima.
 *
 * [paddingTopo] e [paddingBase] tiram da área “útil” o cartão de Embarque e o
 * painel de pontos. Sem isso o fitBounds centra os pinos na tela inteira — e
 * eles ficam escondidos atrás do bottom sheet, como se não existissem.
 */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapaGoogle(
    pinos: List<PinoMapa>,
    minhaPosicao: Pair<Double, Double>?,
    trajeto: List<Pair<Double, Double>>,
    aoTocarPino: (Int) -> Unit,
    modifier: Modifier = Modifier,
    paddingTopo: Dp = 100.dp,
    paddingBase: Dp = 300.dp,
    /** Reserva selecionada na lista: o mapa recentraliza nela, como no web. */
    focarEm: Int? = null,
    /** Muda a cada toque na lista para forçar o recenter mesmo no mesmo ponto. */
    focoPedido: Int = 0,
    /** Sem permissão de GPS: o toque no botão pede a liberação. */
    aoPedirLocalizacao: () -> Unit = {},
    /**
     * Expandir o mapa em tela cheia. Sem callback o botão não aparece.
     * Em tela cheia use [aoFecharExpandido] (botão X) — o arraste do mapa
     * funciona sozinho, sem modo "mover".
     */
    aoExpandirMapa: (() -> Unit)? = null,
    aoFecharExpandido: (() -> Unit)? = null,
    /**
     * Admin inspecionando: o marcador é o GUIA, com as iniciais e o nome
     * (igual ao chip "FR Fabio Reis" do web). null = ponto azul do aparelho.
     */
    rotuloGuia: String? = null,
    /** Foto do guia (URL) para o chip; sem foto ficam as iniciais. */
    fotoGuia: String? = null,
    /** Onde o guia iniciou o embarque — bandeira verde (admin). */
    inicio: Pair<Double, Double>? = null,
    /** Onde o guia encerrou o embarque — bandeira vermelha (admin). */
    fim: Pair<Double, Double>? = null,
    /** Visão "Todos": cada guia com cor, trajeto e posição atual. */
    guias: List<GuiaNoMapa> = emptyList(),
) {
    val contexto = LocalContext.current
    val density = LocalDensity.current
    val escopo = rememberCoroutineScope()

    /*
     * BitmapDescriptorFactory só funciona depois que o SDK do Maps carregou as
     * classes nativas, e aqui os ícones nascem ANTES do primeiro desenho do
     * mapa — então a inicialização tem de vir antes deles.
     */
    remember { MapsInitializer.initialize(contexto) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-22.9068, -43.1729), 13f)
    }

    val cores = remember(pinos) { pinos.map { it.cor }.toSet() }
    var iconesPorCor by remember { mutableStateOf<Map<Int, BitmapDescriptor>>(emptyMap()) }
    var iconeDoGuia by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var mapaPronto by remember { mutableStateOf(false) }

    // Ícones só depois do mapa pronto: antes disso a fábrica às vezes devolve
    // descriptor inválido e o pino some sem erro.
    LaunchedEffect(mapaPronto, cores) {
        if (!mapaPronto) return@LaunchedEffect
        iconesPorCor = cores.associateWith { cor ->
            // Se o ícone desenhado falhar, cai no marcador padrão DA MESMA COR —
            // não num azul fixo, senão o pino deixa de seguir a legenda.
            runCatching { iconeDoPino(contexto, cor) }.getOrElse {
                BitmapDescriptorFactory.defaultMarker(hueDoArgb(cor))
            }
        }
        iconeDoGuia = runCatching { iconeDoPontoDoGuia(contexto) }.getOrElse {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }

    val paddingTopoPx = with(density) { paddingTopo.roundToPx() }
    val paddingBasePx = with(density) { paddingBase.roundToPx() }

    // Trajeto bruto → geometria pela rua (OSRM). Enquanto calcula, mostra o bruto.
    var trajetoNasRuas by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    LaunchedEffect(trajeto) {
        if (trajeto.size < 2) {
            trajetoNasRuas = emptyList()
            return@LaunchedEffect
        }
        trajetoNasRuas = trajeto.map { LatLng(it.first, it.second) }
        delay(350)
        val alinhada = TrajetoNasRuas.alinhar(trajeto)
        trajetoNasRuas = alinhada.map { LatLng(it.first, it.second) }
    }

    // Só os pontos de embarque entram na chave: GPS do guia muda a cada poucos
    // segundos e cancelaria o enquadramento no meio (câmera presa no Rio default).
    val chavePinos = remember(pinos) {
        pinos.joinToString("|") { "${it.id}:${"%.5f".format(it.latitude)}:${"%.5f".format(it.longitude)}" }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            contentPadding = PaddingValues(top = paddingTopo, bottom = paddingBase),
            properties = MapProperties(mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
            ),
            onMapLoaded = { mapaPronto = true },
            onMapClick = { },
        ) {
            // Admin: o guia entra no enquadramento junto com os pinos (o web faz o
            // mesmo). Só na 1ª posição — o polling não fica re-enquadrando.
            val guiaNoEnquadramento = if (rotuloGuia != null) minhaPosicao else null
            val guiasComPosicao = guias.mapNotNull { it.atual }
            MapEffect(
                chavePinos, paddingTopoPx, paddingBasePx,
                guiaNoEnquadramento != null, guiasComPosicao.size,
            ) { map ->
                map.setPadding(0, paddingTopoPx, 0, paddingBasePx)

                val pontos = pinos.map { LatLng(it.latitude, it.longitude) } +
                    listOfNotNull(guiaNoEnquadramento?.let { (lat, lon) -> LatLng(lat, lon) }) +
                    guiasComPosicao.map { (lat, lon) -> LatLng(lat, lon) }
                if (pontos.isEmpty()) return@MapEffect

                // newLatLngBounds exige largura/altura > 0; no emulador isso pode
                // atrasar alguns frames depois do onMapLoaded.
                repeat(12) { tentativa ->
                    val ok = runCatching {
                        if (pontos.size == 1) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pontos.first(), 15f))
                        } else {
                            val limites = LatLngBounds.builder()
                                .apply { pontos.forEach { include(it) } }
                                .build()
                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(limites, 80))
                        }
                    }.isSuccess
                    if (ok) return@MapEffect
                    delay(50L * (tentativa + 1))
                }

                // Último recurso: pelo menos um pino na tela.
                runCatching {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(pontos.first(), 14f))
                }
            }

            if (trajetoNasRuas.size > 1) {
                Polyline(
                    points = trajetoNasRuas,
                    color = androidx.compose.ui.graphics.Color(0xD92563EB),
                    width = 12f,
                )
            }

            pinos.forEach { p ->
                key(p.id) {
                    val icone = iconesPorCor[p.cor]
                        ?: BitmapDescriptorFactory.defaultMarker(hueDoArgb(p.cor))
                    Marker(
                        state = rememberMarkerState(
                            key = "pino-${p.id}",
                            position = LatLng(p.latitude, p.longitude),
                        ),
                        icon = icone,
                        anchor = Offset(0.5f, 1f),
                        title = p.titulo,
                        zIndex = 1f,
                        onClick = {
                            aoTocarPino(p.id)
                            // Sempre vai até o ponto — não depende do LaunchedEffect
                            // (que não reage se o mesmo id já estava selecionado).
                            escopo.launch {
                                runCatching {
                                    camera.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(p.latitude, p.longitude),
                                            16f,
                                        ),
                                    )
                                }
                            }
                            true
                        },
                    )
                }
            }

            if (rotuloGuia != null) minhaPosicao?.let { (lat, lon) ->
                key("guia-rotulo") {
                    val estadoRotulo = rememberMarkerState(key = "guia-rotulo", position = LatLng(lat, lon))
                    LaunchedEffect(lat, lon) {
                        estadoRotulo.position = LatLng(lat, lon)
                    }
                    val foto = rememberFotoDoGuia(fotoGuia)
                    MarkerComposable(
                        rotuloGuia, foto ?: Unit,
                        state = estadoRotulo,
                        anchor = Offset(0.15f, 0.5f),
                        zIndex = 3f,
                        onClick = { true },
                    ) {
                        ChipDoGuia(rotuloGuia, foto = foto)
                    }
                }
            } else if (guias.isEmpty()) minhaPosicao?.let { (lat, lon) ->
                key("guia") {
                    val estadoGuia = rememberMarkerState(key = "guia", position = LatLng(lat, lon))
                    LaunchedEffect(lat, lon) {
                        estadoGuia.position = LatLng(lat, lon)
                    }
                    Marker(
                        state = estadoGuia,
                        icon = iconeDoGuia
                            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                        anchor = Offset(0.5f, 0.5f),
                        zIndex = 2f,
                        onClick = { true },
                    )
                }
            }

            // Bandeiras de início (verde) e fim (vermelha) do embarque do guia.
            inicio?.let { (lat, lon) ->
                key("inicio-embarque") {
                    MarkerComposable(
                        "inicio",
                        state = rememberMarkerState(key = "inicio-embarque", position = LatLng(lat, lon)),
                        anchor = Offset(0.2f, 1f),
                        title = "Início do embarque",
                        zIndex = 2f,
                    ) { Bandeira(corPano = 0xFF16A34A, corMastro = 0xFF15803D) }
                }
            }
            fim?.let { (lat, lon) ->
                key("fim-embarque") {
                    MarkerComposable(
                        "fim",
                        state = rememberMarkerState(key = "fim-embarque", position = LatLng(lat, lon)),
                        anchor = Offset(0.2f, 1f),
                        title = "Fim do embarque",
                        zIndex = 2f,
                    ) { Bandeira(corPano = 0xFFDC2626, corMastro = 0xFF991B1B) }
                }
            }

            // Visão "Todos": um percurso e um chip por guia, na cor dele.
            guias.forEach { g ->
                key("guia-todos-${g.usuId}") {
                    if (g.trajeto.size > 1) {
                        Polyline(
                            points = g.trajeto.map { (lat, lon) -> LatLng(lat, lon) },
                            color = androidx.compose.ui.graphics.Color(g.cor).copy(alpha = 0.85f),
                            width = 10f,
                        )
                    }
                    g.atual?.let { (lat, lon) ->
                        val estado = rememberMarkerState(key = "guia-todos-${g.usuId}", position = LatLng(lat, lon))
                        LaunchedEffect(lat, lon) { estado.position = LatLng(lat, lon) }
                        val foto = rememberFotoDoGuia(g.foto)
                        MarkerComposable(
                            g.usuId, g.nome.orEmpty(), g.cor, foto ?: Unit,
                            state = estado,
                            anchor = Offset(0.15f, 0.5f),
                            zIndex = 3f,
                            onClick = { true },
                        ) {
                            ChipDoGuia(g.nome ?: "Guia", cor = g.cor, foto = foto)
                        }
                    }
                }
            }
        }

        /*
         * FABs no canto inferior direito.
         *
         * Em tela cheia o Dialog vai sob a barra de gestos e o inset do
         * Window às vezes vem zerado — por isso o padding de baixo sobe
         * (24.dp) e ainda aplicamos navigationBarsPadding quando existir.
         */
        val expandido = aoFecharExpandido != null
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(2f)
                .then(if (expandido) Modifier.navigationBarsPadding() else Modifier)
                .padding(end = 12.dp, bottom = if (expandido) 96.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            FloatingActionButton(
                onClick = {
                    escopo.launch {
                        val pos = minhaPosicao
                            ?: obterPosicaoAgora(contexto)
                            ?: trajeto.lastOrNull()

                        if (pos == null) {
                            Toast.makeText(
                                contexto,
                                contexto.getString(R.string.embarque_sem_localizacao),
                                Toast.LENGTH_SHORT,
                            ).show()
                            aoPedirLocalizacao()
                            return@launch
                        }

                        if (!mapaPronto) return@launch
                        runCatching {
                            camera.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(pos.first, pos.second),
                                    16f,
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier.size(40.dp),
                shape = FloatingActionButtonDefaults.smallShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (minhaPosicao != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = stringResource(R.string.embarque_centralizar),
                    modifier = Modifier.size(18.dp),
                )
            }

            // Fechar (X) sai daqui: em tela cheia ele vai para o topo à esquerda,
            // onde fica óbvio e longe da barra de gestos. Ver abaixo.
            if (aoExpandirMapa != null) {
                FloatingActionButton(
                    onClick = aoExpandirMapa,
                    modifier = Modifier.size(40.dp),
                    shape = FloatingActionButtonDefaults.smallShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenWith,
                        contentDescription = stringResource(R.string.embarque_expandir_mapa),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // Mapa em tela cheia: botão X no canto superior esquerdo, abaixo da barra
        // de status, bem visível — não escondido junto da lupa lá embaixo.
        if (aoFecharExpandido != null) {
            FloatingActionButton(
                onClick = aoFecharExpandido,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(2f)
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 12.dp)
                    .size(40.dp),
                shape = FloatingActionButtonDefaults.smallShape,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.embarque_fechar_mapa),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    // Toque na lista: vai até o ponto, como o "Ver no mapa" do web.
    LaunchedEffect(focarEm, focoPedido, mapaPronto) {
        if (!mapaPronto) return@LaunchedEffect
        val id = focarEm ?: return@LaunchedEffect
        if (focoPedido <= 0) return@LaunchedEffect
        val pino = pinos.firstOrNull { it.id == id } ?: return@LaunchedEffect
        runCatching {
            camera.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(pino.latitude, pino.longitude),
                    16f,
                ),
            )
        }
    }
}

/**
 * Pedido ativo de GPS no toque do botão.
 *
 * lastLocation sozinho falha no emulador e em aparelhos sem cache recente —
 * getCurrentLocation força uma leitura nova quando a permissão já existe.
 */
private suspend fun obterPosicaoAgora(contexto: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null

    return try {
        suspendCancellableCoroutine { cont ->
            val cancelamento = CancellationTokenSource()
            cont.invokeOnCancellation { cancelamento.cancel() }
            LocationServices.getFusedLocationProviderClient(contexto)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancelamento.token)
                .addOnSuccessListener { loc ->
                    cont.resume(loc?.let { it.latitude to it.longitude })
                }
                .addOnFailureListener { cont.resume(null) }
        }
    } catch (_: SecurityException) {
        null
    }
}

/**
 * A gota do web, desenhada em bitmap.
 *
 * Não dá para usar um vetor tingido: a gota tem duas cores — o corpo, que muda
 * com o status, e o contorno e o miolo, que são sempre brancos. O tint do
 * Android pinta o desenho inteiro de uma cor só.
 */
/**
 * Matiz (0–360) de uma cor ARGB, para o marcador padrão do Google seguir a
 * legenda quando o ícone desenhado não estiver disponível.
 */
private fun hueDoArgb(cor: Int): Float {
    val hsv = FloatArray(3)
    Color.colorToHSV(cor, hsv)
    return hsv[0]
}

private fun iconeDoPino(contexto: Context, cor: Int): BitmapDescriptor {
    val d = contexto.resources.displayMetrics.density
    val largura = (26 * d).toInt().coerceAtLeast(1)
    val altura = (36 * d).toInt().coerceAtLeast(1)
    val raio = 13f * d

    val bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888)
    val tela = Canvas(bitmap)

    val gota = Path().apply {
        addCircle(raio, raio, raio - 1.5f * d, Path.Direction.CW)
        moveTo(raio - 6.5f * d, raio + 7.5f * d)
        lineTo(raio, altura - 1.5f * d)
        lineTo(raio + 6.5f * d, raio + 7.5f * d)
        close()
    }

    val tinta = Paint(Paint.ANTI_ALIAS_FLAG)

    tinta.style = Paint.Style.FILL
    tinta.color = cor
    tela.drawPath(gota, tinta)

    tinta.style = Paint.Style.STROKE
    tinta.strokeWidth = 2f * d
    tinta.color = Color.WHITE
    tela.drawPath(gota, tinta)

    tinta.style = Paint.Style.FILL
    tela.drawCircle(raio, raio, 4.5f * d, tinta)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** O círculo azul de "você está aqui", igual ao guideIcon do web. */
private fun iconeDoPontoDoGuia(contexto: Context): BitmapDescriptor {
    val d = contexto.resources.displayMetrics.density
    val lado = (22 * d).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val tela = Canvas(bitmap)
    val meio = lado / 2f

    val tinta = Paint(Paint.ANTI_ALIAS_FLAG)
    tinta.color = Color.WHITE
    tela.drawCircle(meio, meio, meio - 1f, tinta)

    tinta.color = Color.parseColor("#2563EB")
    tela.drawCircle(meio, meio, meio - 3f * d, tinta)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** Um guia na visão "Todos" do Geocheck-in. */
data class GuiaNoMapa(
    val usuId: Int,
    val nome: String?,
    val foto: String?,
    /** ARGB — a mesma cor no percurso e no aro do chip. */
    val cor: Int,
    val trajeto: List<Pair<Double, Double>>,
    val atual: Pair<Double, Double>?,
)

/** Azul do marcador do guia quando não há cor própria (igual ao web). */
private const val AZUL_GUIA = 0xFF2563EB.toInt()

/**
 * Cor estável por guia (passo áureo 137,5°) — a mesma conta do corDoGuia do
 * web, para o mesmo guia ter a mesma cor nas duas telas.
 */
fun corDoGuia(usuId: Int): Int {
    val hue = ((usuId * 137.508) % 360).toFloat()
    return android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.82f, 0.77f))
}

/**
 * Baixa a foto do guia (Coil) como bitmap. MarkerComposable desenha uma vez só,
 * então a foto entra como chave do marcador — quando chega, ele se redesenha.
 */
@Composable
private fun rememberFotoDoGuia(url: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val contexto = LocalContext.current
    var foto by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        // No emulador, "localhost" do servidor local é o 10.0.2.2.
        val alvo = if (br.com.rio40graus.guiascale.BuildConfig.API_URL.contains("10.0.2.2")) {
            url.replace("://localhost", "://10.0.2.2").replace("://127.0.0.1", "://10.0.2.2")
        } else {
            url
        }
        val pedido = coil.request.ImageRequest.Builder(contexto)
            .data(alvo)
            .allowHardware(false)
            .size(96)
            .build()
        val resultado = coil.ImageLoader(contexto).execute(pedido)
        val drawable = (resultado as? coil.request.SuccessResult)?.drawable ?: return@LaunchedEffect
        foto = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.asImageBitmap()
    }
    return foto
}

/**
 * Marcador do guia na visão do admin: avatar (foto ou iniciais) + o nome numa
 * pílula branca — o mesmo chip ("FR Fabio Reis") do Geocheck-in do web.
 */
@Composable
private fun ChipDoGuia(
    nome: String,
    cor: Int = AZUL_GUIA,
    foto: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    val iniciais = nome.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .let { partes ->
            ((partes.firstOrNull()?.take(1) ?: "") + (if (partes.size > 1) partes.last().take(1) else ""))
                .uppercase()
        }
    val corCompose = androidx.compose.ui.graphics.Color(cor)
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .shadow(3.dp, androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(
                androidx.compose.ui.graphics.Color.White,
                androidx.compose.foundation.shape.RoundedCornerShape(50),
            )
            .padding(start = 2.dp, top = 2.dp, bottom = 2.dp, end = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .background(corCompose, androidx.compose.foundation.shape.CircleShape)
                .padding(if (foto != null) 2.dp else 0.dp),
        ) {
            if (foto != null) {
                androidx.compose.foundation.Image(
                    bitmap = foto,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.CircleShape),
                )
            } else {
                androidx.compose.material3.Text(
                    iniciais,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        androidx.compose.material3.Text(
            nome,
            color = androidx.compose.ui.graphics.Color(0xFF152826),
            fontSize = 12.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** Bandeira de início/fim do embarque — o mesmo desenho do web (mastro + pano). */
@Composable
private fun Bandeira(corPano: Long, corMastro: Long) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(width = 30.dp, height = 38.dp)) {
        val ex = size.width / 30f
        val ey = size.height / 38f
        drawLine(
            color = androidx.compose.ui.graphics.Color(corMastro),
            start = Offset(6f * ex, 4f * ey),
            end = Offset(6f * ex, 36f * ey),
            strokeWidth = 3f * ex,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        val pano = androidx.compose.ui.graphics.Path().apply {
            moveTo(6f * ex, 5f * ey)
            lineTo(25f * ex, 10f * ey)
            lineTo(6f * ex, 16f * ey)
            close()
        }
        drawPath(pano, androidx.compose.ui.graphics.Color(corPano))
        drawPath(
            pano,
            androidx.compose.ui.graphics.Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * ex),
        )
    }
}
