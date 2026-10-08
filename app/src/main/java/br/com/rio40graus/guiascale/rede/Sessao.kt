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
    private const val CHAVE_FOTO = "foto"
    private const val CHAVE_USU_ID = "usu_id"
    private const val CHAVE_TIPO = "tipo"
    private const val CHAVE_GUIA_INSP_ID = "guia_insp_id"
    private const val CHAVE_GUIA_INSP_PARID = "guia_insp_parid"
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

    /** URL da foto do guia (a mesma do avatar do mapa), quando houver. */
    @Volatile
    var foto: String? = null
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

    /**
     * Admin: o parceiro do guia inspecionado (parceiros.par_id).
     *
     * Guia operacional/apoio sem login não tem usu_id; o mapa de embarque é
     * inspecionável por par_id (as demais telas exigem login). Ver web
     * (SeletorGuiaAdmin.tsx: id `erp:` com login, `par:` sem login).
     */
    @Volatile
    var guiaInspecionadoParId: Int? = null
        private set

    @Volatile
    var guiaInspecionadoNome: String? = null
        private set

    /** Chave única da inspeção (muda ao trocar de guia). Null = ninguém escolhido. */
    fun chaveInspecao(): String? =
        guiaInspecionadoId?.let { "u$it" } ?: guiaInspecionadoParId?.let { "p$it" }

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
        foto = prefs.getString(CHAVE_FOTO, null)
        usuId = prefs.getInt(CHAVE_USU_ID, 0).takeIf { it > 0 }
        tipo = prefs.getString(CHAVE_TIPO, null)
        guiaInspecionadoId = prefs.getInt(CHAVE_GUIA_INSP_ID, 0).takeIf { it > 0 }
        guiaInspecionadoParId = prefs.getInt(CHAVE_GUIA_INSP_PARID, 0).takeIf { it > 0 }
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
        // Nova sessão: a foto vem depois, no /guia/me.
        this.foto = null
        // Troca de sessão zera o guia inspecionado.
        this.guiaInspecionadoId = null
        this.guiaInspecionadoParId = null
        this.guiaInspecionadoNome = null

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            putString(CHAVE_TOKEN, token)
            putString(CHAVE_LOGIN, login)
            putString(CHAVE_TIPO, tipo)
            remove(CHAVE_FOTO)
            remove(CHAVE_GUIA_INSP_ID)
            remove(CHAVE_GUIA_INSP_PARID)
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

    /**
     * Admin escolhe qual guia inspecionar. `usuId` quando o guia tem login;
     * `parId` é o parceiro (serve ao mapa mesmo sem login). Guardamos os dois:
     * o mapa usa usu_id quando há, senão par_id.
     */
    fun guardarGuiaInspecionado(contexto: Context, usuId: Int?, parId: Int?, nome: String?) {
        this.guiaInspecionadoId = usuId?.takeIf { it > 0 }
        this.guiaInspecionadoParId = parId?.takeIf { it > 0 }
        this.guiaInspecionadoNome = nome
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            if (this@Sessao.guiaInspecionadoId != null) {
                putInt(CHAVE_GUIA_INSP_ID, this@Sessao.guiaInspecionadoId!!)
            } else {
                remove(CHAVE_GUIA_INSP_ID)
            }
            if (this@Sessao.guiaInspecionadoParId != null) {
                putInt(CHAVE_GUIA_INSP_PARID, this@Sessao.guiaInspecionadoParId!!)
            } else {
                remove(CHAVE_GUIA_INSP_PARID)
            }
            if (this@Sessao.guiaInspecionadoId != null || this@Sessao.guiaInspecionadoParId != null) {
                putString(CHAVE_GUIA_INSP_NOME, nome)
            } else {
                remove(CHAVE_GUIA_INSP_NOME)
            }
        }
    }

    fun guardarPerfil(contexto: Context, nome: String?, usuId: Int? = null, foto: String? = null) {
        this.nome = nome?.takeIf { it.isNotBlank() }
        if (usuId != null && usuId > 0) this.usuId = usuId
        this.foto = foto?.takeIf { it.isNotBlank() }
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            if (this@Sessao.nome != null) {
                putString(CHAVE_NOME, this@Sessao.nome)
            } else {
                remove(CHAVE_NOME)
            }
            if (this@Sessao.usuId != null) {
                putInt(CHAVE_USU_ID, this@Sessao.usuId!!)
            }
            if (this@Sessao.foto != null) {
                putString(CHAVE_FOTO, this@Sessao.foto)
            } else {
                remove(CHAVE_FOTO)
            }
        }
    }

    /** Guarda só a foto — usado logo após o guia trocar a foto no app. */
    fun guardarFoto(contexto: Context, foto: String?) {
        this.foto = foto?.takeIf { it.isNotBlank() }
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            if (this@Sessao.foto != null) {
                putString(CHAVE_FOTO, this@Sessao.foto)
            } else {
                remove(CHAVE_FOTO)
            }
        }
    }

    fun limpar(contexto: Context) {
        token = null
        login = null
        nome = null
        foto = null
        usuId = null
        tipo = null
        guiaInspecionadoId = null
        guiaInspecionadoParId = null
        guiaInspecionadoNome = null

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit { clear() }
    }
}
