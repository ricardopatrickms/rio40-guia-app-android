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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
            runCatching { iconeDoPino(contexto, cor) }.getOrElse {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
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
            MapEffect(chavePinos, paddingTopoPx, paddingBasePx) { map ->
                map.setPadding(0, paddingTopoPx, 0, paddingBasePx)

                val pontos = pinos.map { LatLng(it.latitude, it.longitude) }
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
                        ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
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

            minhaPosicao?.let { (lat, lon) ->
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
                .padding(end = 12.dp, bottom = if (expandido) 48.dp else 12.dp),
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

            when {
                aoFecharExpandido != null -> {
                    FloatingActionButton(
                        onClick = aoFecharExpandido,
                        modifier = Modifier.size(40.dp),
                        shape = FloatingActionButtonDefaults.smallShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.embarque_fechar_mapa),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                aoExpandirMapa != null -> {
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
