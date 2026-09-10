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

    @Volatile
    var token: String? = null
        private set

    @Volatile
    var login: String? = null
        private set

    val autenticado: Boolean
        get() = !token.isNullOrBlank()

    /** Chamado no início do app, antes de qualquer requisição. */
    fun carregar(contexto: Context) {
        val prefs = contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)
        token = prefs.getString(CHAVE_TOKEN, null)
        login = prefs.getString(CHAVE_LOGIN, null)
    }

    fun guardar(contexto: Context, token: String, login: String) {
        this.token = token
        this.login = login

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit {
            putString(CHAVE_TOKEN, token)
            putString(CHAVE_LOGIN, login)
        }
    }

    fun limpar(contexto: Context) {
        token = null
        login = null

        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit { clear() }
    }
}
