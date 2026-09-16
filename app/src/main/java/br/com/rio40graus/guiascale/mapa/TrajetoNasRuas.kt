package br.com.rio40graus.guiascale.mapa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Encaixa o rastro do GPS nas ruas.
 *
 * O app grava pontos do aparelho; se só ligarmos um ao outro, a linha corta
 * quarteirão. Aqui pedimos ao OSRM (o mesmo roteador que o web já usa no
 * tráfego) a geometria pela via — Match para breadcrumbs, Route como fallback.
 */
object TrajetoNasRuas {

    private const val BASE = "https://router.project-osrm.org"
    /** OSRM público engasga com lista enorme; amostramos sem perder o desenho. */
    private const val MAX_PONTOS = 80

    /**
     * @return pontos já na rua, ou a lista original se a rede/roteador falhar.
     */
    suspend fun alinhar(pontos: List<Pair<Double, Double>>): List<Pair<Double, Double>> =
        withContext(Dispatchers.IO) {
            if (pontos.size < 2) return@withContext pontos
            val amostra = amostrar(pontos, MAX_PONTOS)
            casarNaVia(amostra) ?: rotear(amostra) ?: pontos
        }

    /** Mantém início/fim e espalha o meio para caber no limite do OSRM. */
    private fun amostrar(
        pontos: List<Pair<Double, Double>>,
        maximo: Int,
    ): List<Pair<Double, Double>> {
        if (pontos.size <= maximo) return pontos
        val passo = (pontos.size - 1).toDouble() / (maximo - 1)
        return List(maximo) { i ->
            pontos[Math.round(i * passo).toInt().coerceIn(0, pontos.lastIndex)]
        }
    }

    /**
     * Match: feito para GPS com ruído — “cola” o rastro na rua mais próxima.
     * https://project-osrm.org/docs/v5.24.0/api/#match-service
     */
    private fun casarNaVia(pontos: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        val coords = pontos.joinToString(";") { (lat, lon) -> "$lon,$lat" }
        val url =
            "$BASE/match/v1/driving/$coords?overview=full&geometries=geojson&tidy=true"
        val json = getJson(url) ?: return null
        if (json.optString("code") != "Ok") return null

        val matchings = json.optJSONArray("matchings") ?: return null
        if (matchings.length() == 0) return null

        val saida = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until matchings.length()) {
            val geom = matchings.getJSONObject(i).optJSONObject("geometry") ?: continue
            saida += lerGeoJsonLinha(geom)
        }
        return saida.takeIf { it.size >= 2 }
    }

    /**
     * Route: quando o Match não fecha (pontos muito esparsos), calcula a rota
     * de direção entre os pontos amostrados.
     */
    private fun rotear(pontos: List<Pair<Double, Double>>): List<Pair<Double, Double>>? {
        val coords = pontos.joinToString(";") { (lat, lon) -> "$lon,$lat" }
        val url =
            "$BASE/route/v1/driving/$coords?overview=full&geometries=geojson&continue_straight=true"
        val json = getJson(url) ?: return null
        if (json.optString("code") != "Ok") return null

        val routes = json.optJSONArray("routes") ?: return null
        if (routes.length() == 0) return null
        val geom = routes.getJSONObject(0).optJSONObject("geometry") ?: return null
        return lerGeoJsonLinha(geom).takeIf { it.size >= 2 }
    }

    private fun lerGeoJsonLinha(geometry: JSONObject): List<Pair<Double, Double>> {
        val coords = geometry.optJSONArray("coordinates") ?: return emptyList()
        val lista = ArrayList<Pair<Double, Double>>(coords.length())
        for (i in 0 until coords.length()) {
            val par = coords.getJSONArray(i)
            // GeoJSON: [longitude, latitude]
            lista += par.getDouble(1) to par.getDouble(0)
        }
        return lista
    }

    private fun getJson(url: String): JSONObject? =
        try {
            val conexao = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "GuiaScale-Android")
            }
            conexao.inputStream.bufferedReader().use { leitor ->
                JSONObject(leitor.readText())
            }
        } catch (_: Exception) {
            null
        }
}
