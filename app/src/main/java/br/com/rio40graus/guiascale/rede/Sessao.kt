package br.com.rio40graus.guiascale.rede

import android.content.Context
import androidx.core.content.edit

/**
 * O token do guia, entre uma abertura do app e a seguinte.
 *
 * Fica em memória para o interceptor ler sem custo, e no disco para o rastreio
 * sobreviver a fechar o app — que é o caso normal aqui: o guia loga de manhã,
 * minimiza e o serviço trabalha o dia inteiro sozinho.
 *
 * Guardado em SharedPreferences simples. Não é o ideal para um segredo, e a
 * troca por EncryptedSharedPreferences é uma linha; fica anotado porque o
 * token é de acesso, expira, e o app ainda não foi para a Play Store.
 */
object Sessao {

    private const val ARQUIVO = "guiascale.sessao"
    private const val CHAVE_TOKEN = "token"
    private const val CHAVE_LOGIN = "login"
    private const val CHAVE_NOME = "nome"
    private const val CHAVE_USU_ID = "usu_id"
    private const val CHAVE_TIPO = "tipo"
    private const val CHAVE_GUIA_INSP_ID = "guia_insp_id"
    private const val CHAVE_GUIA_INSP_NOME = "guia_insp_nome"

    @Volatile
    var token: String? = null
        private set

    @Volatile
    var login: String? = null
        private set

    /** Nome da pessoa (pes_nome), igual ao "GUIA:" do web. */
    @Volatile
    var nome: String? = null
        private set

    /** `usuarios.usu_id` — compara com `ocorrencia.usuario_id` (editar/excluir). */
    @Volatile
    var usuId: Int? = null
        private set

    /** "guia" ou "admin" — quem entrou. O app decide o modo por isto. */
    @Volatile
    var tipo: String? = null
        private set

    /** true quando um administrador entrou (modo somente-leitura + inspeção). */
    val ehAdmin: Boolean
        get() = tipo == "admin"

    /** Admin: o guia que está sendo inspecionado (usuarios.usu_id). */
    @Volatile
    var guiaInspecionadoId: Int? = null
        private set

    @Volatile
    var guiaInspecionadoNome: String? = null
        private set

    val autenticado: Boolean
        get() = !token.isNullOrBlank()

    /** Nome para exibir: pessoa, senão o login. */
    val nomeExibicao: String?
        get() = nome?.takeIf { it.isNotBlank() } ?: login

    /** Chamado no início do app, antes de qualquer requisição. */
    fun carregar(contexto: Context) {
        val prefs = contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        token = prefs.getString(CHAVE_TOKEN, null)
        login = prefs.getString(CHAVE_LOGIN, null)
        nome = prefs.getString(CHAVE_NOME, null)
        usuId = prefs.getInt(CHAVE_USU_ID, 0).takeIf { it > 0 }
        tipo = prefs.getString(CHAVE_TIPO, null)
        guiaInspecionadoId = prefs.getInt(CHAVE_GUIA_INSP_ID, 0).takeIf { it > 0 }
        guiaInspecionadoNome = prefs.getString(CHAVE_GUIA_INSP_NOME, null)
    }

    fun guardar(
        contexto: Context,
        token: String,
        login: String,
        nome: String? = null,
        usuId: Int? = null,
        tipo: String = "guia",
    ) {
        this.token = token
        this.login = login
        this.nome = nome?.takeIf { it.isNotBlank() }
        this.usuId = usuId?.takeIf { it > 0 }
        this.tipo = tipo
        // Troca de sessão zera o guia inspecionado.
        this.guiaInspecionadoId = null
        this.guiaInspecionadoNome = null

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            putString(CHAVE_TOKEN, token)
            putString(CHAVE_LOGIN, login)
            putString(CHAVE_TIPO, tipo)
            remove(CHAVE_GUIA_INSP_ID)
            remove(CHAVE_GUIA_INSP_NOME)
            if (this@Sessao.nome != null) {
                putString(CHAVE_NOME, this@Sessao.nome)
            } else {
                remove(CHAVE_NOME)
            }
            if (this@Sessao.usuId != null) {
                putInt(CHAVE_USU_ID, this@Sessao.usuId!!)
            } else {
                remove(CHAVE_USU_ID)
            }
        }
    }

    /** Admin escolhe qual guia inspecionar. */
    fun guardarGuiaInspecionado(contexto: Context, usuId: Int?, nome: String?) {
        this.guiaInspecionadoId = usuId?.takeIf { it > 0 }
        this.guiaInspecionadoNome = nome
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            if (this@Sessao.guiaInspecionadoId != null) {
                putInt(CHAVE_GUIA_INSP_ID, this@Sessao.guiaInspecionadoId!!)
                putString(CHAVE_GUIA_INSP_NOME, nome)
            } else {
                remove(CHAVE_GUIA_INSP_ID)
                remove(CHAVE_GUIA_INSP_NOME)
            }
        }
    }

    fun guardarPerfil(contexto: Context, nome: String?, usuId: Int? = null) {
        this.nome = nome?.takeIf { it.isNotBlank() }
        if (usuId != null && usuId > 0) this.usuId = usuId
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            if (this@Sessao.nome != null) {
                putString(CHAVE_NOME, this@Sessao.nome)
            } else {
                remove(CHAVE_NOME)
            }
            if (this@Sessao.usuId != null) {
                putInt(CHAVE_USU_ID, this@Sessao.usuId!!)
            }
        }
    }

    fun limpar(contexto: Context) {
        token = null
        login = null
        nome = null
        usuId = null
        tipo = null
        guiaInspecionadoId = null
        guiaInspecionadoNome = null

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit { clear() }
    }
}
