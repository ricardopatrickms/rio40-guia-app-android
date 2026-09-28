package br.com.rio40graus.guiascale.radar

import br.com.rio40graus.guiascale.rede.PedidoTrafegoGoogle
import br.com.rio40graus.guiascale.rede.PontoTrafego
import br.com.rio40graus.guiascale.rede.Rede
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fontes de dados do Radar — as MESMAS do web (scale-guide-pro):
 *
 *  - tempo e vento/mar: Open-Meteo (aberto, sem chave)
 *  - trânsito de referência: OSRM (roteador público)
 *  - trânsito ao vivo: Google Routes, via guias-api (a chave fica no servidor)
 *
 * Os números e a classificação seguem WeatherStrip/WindStrip/TrafficStrip para
 * os dois apps mostrarem a mesma coisa. Mexer aqui sem mexer lá faz divergir.
 */

/** Ponto de saída fixo do trânsito: Rio de Janeiro (Centro / Santos Dumont). */
val ORIGEM_TRAFEGO = CidadeRadar("Rio de Janeiro", -22.9068, -43.1729)

data class CidadeRadar(val nome: String, val lat: Double, val lon: Double)

/** Cidades da previsão do tempo — iguais ao WeatherStrip. */
val CIDADES_TEMPO = listOf(
    CidadeRadar("Angra dos Reis", -23.0067, -44.3181),
    CidadeRadar("Búzios", -22.7469, -41.8817),
    CidadeRadar("Arraial do Cabo", -22.9661, -42.0278),
    CidadeRadar("Petrópolis", -22.5050, -43.1786),
    CidadeRadar("Paraty", -23.2178, -44.7131),
    CidadeRadar("Rio de Janeiro", -22.9068, -43.1729),
)

/** Locais com vento/mar — iguais ao WindStrip (Paraty fica de fora, como no web). */
val LOCAIS_VENTO = listOf(
    CidadeRadar("Angra dos Reis", -23.0067, -44.3181),
    CidadeRadar("Búzios", -22.7469, -41.8817),
    CidadeRadar("Arraial do Cabo", -22.9661, -42.0278),
    CidadeRadar("Petrópolis", -22.5050, -43.1786),
    CidadeRadar("Rio de Janeiro", -22.9068, -43.1729),
)

/** Destinos do trânsito (saída sempre do Rio) — iguais ao TrafficStrip. */
val DESTINOS_TRAFEGO = listOf(
    CidadeRadar("Angra dos Reis", -23.0067, -44.3181),
    CidadeRadar("Búzios", -22.7469, -41.8817),
    CidadeRadar("Arraial do Cabo", -22.9661, -42.0278),
    CidadeRadar("Petrópolis", -22.5050, -43.1786),
)

data class PrevisaoTempo(
    val cidade: String,
    val codigo: Int,
    val tempMax: Double?,
    val tempMin: Double?,
    val chuvaPct: Int?,
)

data class PontoVento(
    val horaLabel: String,
    val nos: Double,
    val rajadaNos: Double,
    val direcaoGraus: Double,
    val ondaM: Double?,
)

data class RotaTrafego(
    val duracaoMin: Double,
    val livreMin: Double,
    val distanciaKm: Double,
    val descricao: String?,
    val pedagio: Boolean,
    val fonte: String,
    val fallback: Boolean,
)

enum class FonteTrafego { OSRM, GOOGLE }

private const val KMH_PARA_NOS = 0.539957

/** Horários da operação — os mesmos filtrados no WindStrip. */
private val HORAS_VENTO = setOf(6, 9, 12, 15, 18, 21)

/** optDouble devolve NaN quando falta o valor; aqui vira 0.0 para não propagar. */
private fun Double?.semNaN(): Double = this?.takeIf { !it.isNaN() } ?: 0.0

object RadarDados {

    /** Cliente próprio, sem o token do guia: as APIs abertas não pedem login. */
    private val clientePublico = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun buscarJson(url: String): JSONObject {
        val pedido = Request.Builder().url(url).get().build()
        clientePublico.newCall(pedido).execute().use { resp ->
            val corpo = resp.body?.string().orEmpty()
            if (!resp.isSuccessful || corpo.isBlank()) {
                throw RuntimeException("HTTP ${resp.code}")
            }
            return JSONObject(corpo)
        }
    }

    /**
     * Previsão das 6 cidades para o dia — uma chamada por cidade, em paralelo.
     *
     * Cada cidade é independente: se uma falhar (timeout, limite momentâneo da
     * API), as outras continuam aparecendo. Antes um único erro derrubava a
     * previsão inteira ("Não foi possível carregar a previsão").
     */
    suspend fun previsaoTempo(data: String): List<PrevisaoTempo> = withContext(Dispatchers.IO) {
        coroutineScope {
            CIDADES_TEMPO.map { c ->
                async {
                    runCatching {
                        val url = "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=${c.lat}&longitude=${c.lon}" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                            "&timezone=America/Sao_Paulo&start_date=$data&end_date=$data"
                        val diario = buscarJson(url).optJSONObject("daily")
                        PrevisaoTempo(
                            cidade = c.nome,
                            codigo = diario?.optJSONArray("weather_code")?.optInt(0, 0) ?: 0,
                            tempMax = diario?.optJSONArray("temperature_2m_max")?.optDouble(0)
                                ?.takeIf { !it.isNaN() },
                            tempMin = diario?.optJSONArray("temperature_2m_min")?.optDouble(0)
                                ?.takeIf { !it.isNaN() },
                            chuvaPct = diario?.optJSONArray("precipitation_probability_max")
                                ?.optInt(0, 0),
                        )
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }
    }

    /** Vento e mar de um local, nos horários da operação. */
    suspend fun ventoEMar(local: CidadeRadar, data: String): List<PontoVento> =
        withContext(Dispatchers.IO) {
            val base = "latitude=${local.lat}&longitude=${local.lon}" +
                "&timezone=America/Sao_Paulo&start_date=$data&end_date=$data"

            val vento = buscarJson(
                "https://api.open-meteo.com/v1/forecast?$base" +
                    "&hourly=wind_speed_10m,wind_gusts_10m,wind_direction_10m",
            ).optJSONObject("hourly") ?: return@withContext emptyList()

            // O mar é opcional — se falhar, mostra vento sem a altura da onda.
            val ondas = runCatching {
                buscarJson(
                    "https://marine-api.open-meteo.com/v1/marine?$base&hourly=wave_height",
                ).optJSONObject("hourly")?.optJSONArray("wave_height")
            }.getOrNull()

            val tempos = vento.optJSONArray("time") ?: return@withContext emptyList()
            val velocidade = vento.optJSONArray("wind_speed_10m")
            val rajada = vento.optJSONArray("wind_gusts_10m")
            val direcao = vento.optJSONArray("wind_direction_10m")

            val pontos = mutableListOf<PontoVento>()
            for (i in 0 until tempos.length()) {
                val iso = tempos.optString(i)
                val hora = iso.substring(11, 13).toIntOrNull() ?: continue
                if (hora !in HORAS_VENTO) continue
                pontos += PontoVento(
                    horaLabel = iso.substring(11, 16),
                    nos = (velocidade?.optDouble(i).semNaN()) * KMH_PARA_NOS,
                    rajadaNos = (rajada?.optDouble(i).semNaN()) * KMH_PARA_NOS,
                    direcaoGraus = direcao?.optDouble(i).semNaN(),
                    ondaM = ondas?.optDouble(i)?.takeIf { !it.isNaN() },
                )
            }
            pontos
        }

    /** OSRM: rota por roteador aberto; "sem trânsito" estimado a 85 km/h. */
    private suspend fun rotaOsrm(destino: CidadeRadar): RotaTrafego = withContext(Dispatchers.IO) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "${ORIGEM_TRAFEGO.lon},${ORIGEM_TRAFEGO.lat};${destino.lon},${destino.lat}" +
            "?overview=false&alternatives=false"
        val rota = buscarJson(url).optJSONArray("routes")?.optJSONObject(0)
            ?: throw RuntimeException("Rota não encontrada (OSRM)")
        val distanciaKm = rota.optDouble("distance", 0.0) / 1000.0
        RotaTrafego(
            duracaoMin = rota.optDouble("duration", 0.0) / 60.0,
            livreMin = distanciaKm / 85.0 * 60.0,
            distanciaKm = distanciaKm,
            descricao = null,
            pedagio = false,
            fonte = "osrm",
            fallback = false,
        )
    }

    /** Google Routes (ao vivo), via guias-api. */
    private suspend fun rotaGoogle(destino: CidadeRadar): RotaTrafego {
        val r = Rede.api.trafegoGoogle(
            PedidoTrafegoGoogle(
                origin = PontoTrafego(ORIGEM_TRAFEGO.lat, ORIGEM_TRAFEGO.lon),
                destination = PontoTrafego(destino.lat, destino.lon),
            ),
        )
        if (!r.error.isNullOrBlank()) throw RuntimeException(r.error)
        return RotaTrafego(
            duracaoMin = r.durationMin,
            livreMin = if (r.staticDurationMin > 0) r.staticDurationMin else r.durationMin,
            distanciaKm = r.distanceKm,
            descricao = r.description,
            pedagio = r.tolls,
            fonte = "google",
            fallback = false,
        )
    }

    /**
     * Busca na fonte preferida e, se ela falhar, cai para a outra —
     * o mesmo comportamento de fetchTrafficWithFallback no web.
     */
    suspend fun rotaTrafego(destino: CidadeRadar, preferida: FonteTrafego): RotaTrafego {
        val principal: suspend (CidadeRadar) -> RotaTrafego =
            if (preferida == FonteTrafego.GOOGLE) ::rotaGoogle else ::rotaOsrm
        val alternativa: suspend (CidadeRadar) -> RotaTrafego =
            if (preferida == FonteTrafego.GOOGLE) ::rotaOsrm else ::rotaGoogle
        return try {
            principal(destino)
        } catch (e: Exception) {
            alternativa(destino).copy(fallback = true)
        }
    }
}

/** Rótulo da fonte, igual ao trafficSourceLabel do web. */
fun rotuloFonte(fonte: String): String = if (fonte == "google") "rota ao vivo" else "OSRM"
