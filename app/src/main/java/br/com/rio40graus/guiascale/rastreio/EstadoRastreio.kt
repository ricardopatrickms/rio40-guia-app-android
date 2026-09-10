package br.com.rio40graus.guiascale.rastreio

import android.content.Context
import androidx.core.content.edit

/**
 * Se o rastreio está ligado e a qual mapa de embarque ele pertence.
 *
 * Fica no disco, e não em memória, por dois motivos. O serviço é START_STICKY:
 * quando o Android mata o processo por falta de memória, ele recria o serviço
 * do zero, e sem isto o percurso seguinte perderia o vínculo com o mapa. E a
 * tela precisa saber o estado real ao abrir — antes ela guardava um booleano
 * próprio, que mentia sempre que o app era reaberto com a captura em curso.
 */
object EstadoRastreio {

    private const val ARQUIVO = "guiascale.rastreio"
    private const val CHAVE_ATIVO = "ativo"
    private const val CHAVE_MAPA = "mapa_id"

    /** Guardado como -1 porque SharedPreferences não tem Int nulo. */
    private const val SEM_MAPA = -1

    private fun prefs(contexto: Context) =
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun ativo(contexto: Context): Boolean =
        prefs(contexto).getBoolean(CHAVE_ATIVO, false)

    fun mapaId(contexto: Context): Int? =
        prefs(contexto).getInt(CHAVE_MAPA, SEM_MAPA).takeIf { it != SEM_MAPA }

    fun ligado(contexto: Context, mapaId: Int?) {
        prefs(contexto).edit {
            putBoolean(CHAVE_ATIVO, true)
            putInt(CHAVE_MAPA, mapaId ?: SEM_MAPA)
        }
    }

    fun desligado(contexto: Context) {
        prefs(contexto).edit {
            putBoolean(CHAVE_ATIVO, false)
            // O mapa some junto: o próximo passeio é outro, e um id velho
            // carimbaria o percurso seguinte com o mapa de ontem.
            remove(CHAVE_MAPA)
        }
    }
}
