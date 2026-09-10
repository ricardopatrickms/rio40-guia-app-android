package br.com.rio40graus.guiascale.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

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
 * chamou, e o cartão de detalhes é desenhado em Compose por cima. É o que
 * mantém o conteúdo do balão igual ao do web — tipografia, selo de status e
 * botão — em vez de uma janela com o visual do Google.
 */
@Composable
fun MapaGoogle(
    pinos: List<PinoMapa>,
    minhaPosicao: Pair<Double, Double>?,
    trajeto: List<Pair<Double, Double>>,
    aoTocarPino: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current

    /*
     * BitmapDescriptorFactory só funciona depois que o SDK do Maps carregou as
     * classes nativas, e aqui os ícones nascem ANTES do primeiro desenho do
     * mapa — então a inicialização tem de vir antes deles.
     *
     * Precisa ser `remember`, e não `LaunchedEffect`: o efeito só roda DEPOIS
     * da composição terminar, enquanto os `remember` dos ícones logo abaixo
     * rodam DURANTE. Com o efeito, a fábrica ainda estava vazia na primeira
     * passagem e o app morria com "IBitmapDescriptorFactory is not
     * initialized". Os blocos de `remember` executam na ordem em que aparecem,
     * e é isso que garante que este venha primeiro.
     */
    remember { MapsInitializer.initialize(contexto) }

    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-22.9068, -43.1729), 13f)
    }

    // Recriar os ícones a cada recomposição custaria um bitmap novo por pino a
    // cada 3 segundos, que é o ritmo com que a tela relê posição e fila.
    val iconesPorCor = remember(pinos.map { it.cor }.toSet()) {
        pinos.map { it.cor }.toSet().associateWith { cor -> iconeDoPino(contexto, cor) }
    }
    val iconeDoGuia = remember { iconeDoPontoDoGuia(contexto) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(
            // O web também não mostra botões de zoom no celular, e a pinça já
            // resolve. O botão de "minha localização" sai porque a posição do
            // guia já é desenhada como pino próprio.
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
        ),
        onMapClick = { },
    ) {
        if (trajeto.size > 1) {
            // Mesma linha do web: azul grossa, levemente transparente.
            Polyline(
                points = trajeto.map { LatLng(it.first, it.second) },
                color = androidx.compose.ui.graphics.Color(0xD92563EB),
                width = 12f,
            )
        }

        pinos.forEach { p ->
            Marker(
                state = rememberMarkerState(
                    key = p.id.toString(),
                    position = LatLng(p.latitude, p.longitude),
                ),
                icon = iconesPorCor[p.cor],
                // A ponta da gota é que aponta o endereço, não o centro dela.
                anchor = Offset(0.5f, 1f),
                title = p.titulo,
                onClick = {
                    aoTocarPino(p.id)
                    // true: consome o toque, e o Maps não abre a própria janela
                    // nem recentraliza o mapa por baixo do cartão.
                    true
                },
            )
        }

        minhaPosicao?.let { (lat, lon) ->
            Marker(
                state = rememberMarkerState(key = "guia", position = LatLng(lat, lon)),
                icon = iconeDoGuia,
                anchor = Offset(0.5f, 0.5f),
                onClick = { true },
            )
        }
    }

    /*
     * Enquadra tudo de uma vez, como o FitBounds do web. Só na primeira vez que
     * há pontos: refazer isso a cada atualização desfaria o zoom que o guia
     * acabou de dar para achar a rua.
     */
    val pontos = pinos.map { LatLng(it.latitude, it.longitude) } +
        listOfNotNull(minhaPosicao?.let { LatLng(it.first, it.second) })

    LaunchedEffect(pontos.isNotEmpty()) {
        if (pontos.isEmpty()) return@LaunchedEffect
        enquadrar(camera, pontos)
    }
}

/** Move a câmera para caber todos os pontos, com folga nas bordas. */
private suspend fun enquadrar(camera: CameraPositionState, pontos: List<LatLng>) {
    if (pontos.size == 1) {
        camera.animate(CameraUpdateFactory.newLatLngZoom(pontos.first(), 15f))
        return
    }

    val limites = LatLngBounds.builder().apply { pontos.forEach { include(it) } }.build()
    /*
     * newLatLngBounds exige que o mapa já tenha tamanho medido; chamado cedo
     * demais ele lança IllegalStateException. Quando isso acontece, centralizar
     * no primeiro ponto é melhor do que derrubar a tela.
     */
    runCatching { camera.animate(CameraUpdateFactory.newLatLngBounds(limites, 96)) }
        .onFailure { camera.animate(CameraUpdateFactory.newLatLngZoom(pontos.first(), 13f)) }
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
    val largura = (26 * d).toInt()
    val altura = (36 * d).toInt()
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
    val lado = (22 * d).toInt()

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
