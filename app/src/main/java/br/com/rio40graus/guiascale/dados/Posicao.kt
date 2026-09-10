package br.com.rio40graus.guiascale.dados

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Uma leitura do GPS, esperando a vez de subir.
 *
 * O rastro é gravado aqui ANTES de tentar a rede. O guia atravessa túnel e
 * estrada sem sinal, e é justamente esse trecho que interessa — se o app só
 * enviasse ao vivo, perderia o pedaço que ninguém consegue reconstituir depois.
 *
 * `enviada` marca o que já chegou ao servidor. As linhas não somem na hora:
 * ficam por um tempo para o app poder mostrar o trajeto do dia sem pedir nada
 * à rede, e a limpeza acontece por idade.
 */
@Entity(tableName = "posicoes")
data class Posicao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    /** Raio de erro em metros, como o aparelho informa. */
    val precisao: Float?,
    /** Metros por segundo, quando o aparelho sabe. */
    val velocidade: Float?,
    /** Momento da leitura no aparelho, em epoch milissegundos. */
    val capturadoEm: Long,
    /** Mapa de embarque do dia, quando o app já sabe qual é. */
    val mapaId: Int? = null,
    val enviada: Boolean = false,
)

@Dao
interface PosicaoDao {

    @Insert
    suspend fun inserir(posicao: Posicao)

    /**
     * O que falta enviar, mais antigo primeiro.
     *
     * O limite existe para o lote caber na requisição: depois de horas sem
     * sinal a fila tem milhares de pontos, e mandar tudo de uma vez estoura
     * tempo ou memória. O envio repete enquanto sobrar coisa.
     */
    @Query("SELECT * FROM posicoes WHERE enviada = 0 ORDER BY capturadoEm ASC LIMIT :limite")
    suspend fun pendentes(limite: Int): List<Posicao>

    @Query("SELECT COUNT(*) FROM posicoes WHERE enviada = 0")
    suspend fun quantasPendentes(): Int

    @Query("UPDATE posicoes SET enviada = 1 WHERE id IN (:ids)")
    suspend fun marcarEnviadas(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM posicoes WHERE capturadoEm >= :desde")
    suspend fun quantasDesde(desde: Long): Int

    /**
     * O trajeto já percorrido, para desenhar no mapa.
     *
     * Sai da fila local e não da API: os pontos estão aqui assim que o GPS os
     * entrega, enquanto no servidor só aparecem depois que o envio consegue
     * subir — e é justamente no trecho sem sinal que o guia quer ver por onde
     * andou.
     */
    @Query("SELECT * FROM posicoes WHERE capturadoEm >= :desde ORDER BY capturadoEm ASC")
    suspend fun desde(desde: Long): List<Posicao>

    /** Faxina do que já subiu e não serve mais para exibir. */
    @Query("DELETE FROM posicoes WHERE enviada = 1 AND capturadoEm < :antesDe")
    suspend fun limparEnviadasAntesDe(antesDe: Long)
}

@Database(entities = [Posicao::class], version = 1, exportSchema = false)
abstract class BancoLocal : RoomDatabase() {

    abstract fun posicoes(): PosicaoDao

    companion object {
        @Volatile
        private var instancia: BancoLocal? = null

        fun obter(contexto: Context): BancoLocal =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    BancoLocal::class.java,
                    "guiascale.db"
                ).build().also { instancia = it }
            }
    }
}
