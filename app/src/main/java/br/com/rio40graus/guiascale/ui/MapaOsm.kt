package br.com.rio40graus.guiascale.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

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
 * osmdroid sobre os tiles do OpenStreetMap — os MESMOS que o Leaflet do app
 * web carrega, então o desenho do mapa é idêntico nos dois. Os pinos também
 * seguem o formato de lá: gota colorida por status, contorno branco e miolo
 * branco no meio.
 *
 * O toque no pino não abre balão do osmdroid: ele apenas avisa quem chamou,
 * e o cartão de detalhes é desenhado em Compose por cima. É o que mantém o
 * conteúdo do balão igual ao do web — tipografia, selo de status e botão —
 * em vez de uma janelinha com o visual do osmdroid.
 */
@Composable
fun MapaOsm(
    pinos: List<PinoMapa>,
    minhaPosicao: Pair<Double, Double>?,
    trajeto: List<Pair<Double, Double>>,
    aoTocarPino: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { contexto ->
            /*
             * O osmdroid exige um User-Agent próprio. Sem isto o servidor de
             * tiles do OpenStreetMap recusa as requisições e o mapa nasce
             * cinza — a política deles bloqueia clientes anônimos.
             */
            Configuration.getInstance().apply {
                load(contexto, contexto.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                userAgentValue = contexto.packageName
            }

            MapView(contexto).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                // O zoom por botões não aparece: o web também não os mostra no
                // celular, e no toque o gesto de pinça já resolve.
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                controller.setZoom(13.0)

                /*
                 * O mapa vive dentro de uma coluna rolável, e sem isto os dois
                 * disputam o mesmo arrasto: puxar o mapa para o lado até
                 * funcionava, mas puxar para cima rolava a página em vez de
                 * deslocar o mapa. Enquanto o dedo está sobre o mapa, a rolagem
                 * de fora fica proibida de interceptar.
                 */
                setOnTouchListener { visao, evento ->
                    when (evento.actionMasked) {
                        MotionEvent.ACTION_DOWN ->
                            visao.parent?.requestDisallowInterceptTouchEvent(true)

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            visao.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    // false: quem trata o gesto continua sendo o osmdroid.
                    false
                }
            }
        },
        update = { mapa ->
            /*
             * Só refaz as camadas quando algo mudou de verdade.
             *
             * A tela relê posição e fila a cada 3 segundos, e cada leitura
             * dispara recomposição. Reconstruir os 26 marcadores nesse ritmo
             * não é só desperdício: um toque que chegue entre o clear() e o
             * add() encontra o marcador destruído, e o balão nunca abria.
             */
            val assinatura = listOf(
                pinos.joinToString { "${'$'}{it.id}:${'$'}{it.cor}" },
                trajeto.size,
                minhaPosicao?.first?.toString().orEmpty(),
                minhaPosicao?.second?.toString().orEmpty(),
            ).joinToString("|")

            if (mapa.getTag(R_ASSINATURA) == assinatura) return@AndroidView
            mapa.setTag(R_ASSINATURA, assinatura)

            mapa.overlays.clear()

            if (trajeto.size > 1) {
                // Mesma linha do web: azul grossa, levemente transparente.
                mapa.overlays.add(
                    Polyline(mapa).apply {
                        setPoints(trajeto.map { GeoPoint(it.first, it.second) })
                        outlinePaint.color = Color.parseColor("#2563EB")
                        outlinePaint.strokeWidth = 12f
                        outlinePaint.alpha = 217
                    }
                )
            }

            pinos.forEach { p ->
                mapa.overlays.add(
                    Marker(mapa).apply {
                        position = GeoPoint(p.latitude, p.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = desenharPino(mapa.context, p.cor)
                        title = p.titulo
                        setOnMarkerClickListener { _, _ ->
                            aoTocarPino(p.id)
                            true
                        }
                    }
                )
            }

            minhaPosicao?.let { (lat, lon) ->
                mapa.overlays.add(
                    Marker(mapa).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = desenharPontoDoGuia(mapa.context)
                        setOnMarkerClickListener { _, _ -> true }
                    }
                )
            }

            /*
             * Enquadra tudo de uma vez, como o FitBounds do web. Só na primeira
             * vez que há pontos: refazer isso a cada atualização desfaria o
             * zoom que o guia acabou de dar para achar a rua.
             */
            val todos = pinos.map { GeoPoint(it.latitude, it.longitude) } +
                listOfNotNull(minhaPosicao?.let { GeoPoint(it.first, it.second) })

            if (todos.isNotEmpty() && mapa.getTag(R_ENQUADRADO) == null) {
                mapa.setTag(R_ENQUADRADO, true)
                mapa.post {
                    if (todos.size == 1) {
                        mapa.controller.setCenter(todos.first())
                        mapa.controller.setZoom(15.0)
                    } else {
                        mapa.zoomToBoundingBox(
                            BoundingBox.fromGeoPointsSafe(todos).increaseByScale(1.25f),
                            false,
                        )
                    }
                }
            }

            mapa.invalidate()
        },
    )

    // O MapView tem thread de tiles própria; sem soltar, ela sobrevive à tela.
    DisposableEffect(Unit) { onDispose { } }
}

/*
 * Chaves de tag da View.
 *
 * setTag(int, Any) exige um id de recurso, e não um inteiro qualquer — o
 * Android usa o mesmo espaço das tags de layout e recusa valores fora dele.
 * Reaproveitar ids que já existem no app é o caminho barato de conseguir dois.
 */
private val R_ASSINATURA = br.com.rio40graus.guiascale.R.string.aba_embarque
private val R_ENQUADRADO = br.com.rio40graus.guiascale.R.string.aba_rastreio

/**
 * A gota do web, desenhada em bitmap.
 *
 * Não dá para usar um vetor tingido: a gota tem duas cores — o corpo, que muda
 * com o status, e o contorno e o miolo, que são sempre brancos. O tint do
 * Android pinta o desenho inteiro de uma cor só.
 */
private fun desenharPino(contexto: Context, cor: Int): Drawable {
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

    return BitmapDrawable(contexto.resources, bitmap)
}

/** O círculo azul de "você está aqui", igual ao guideIcon do web. */
private fun desenharPontoDoGuia(contexto: Context): Drawable {
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

    return BitmapDrawable(contexto.resources, bitmap)
}
