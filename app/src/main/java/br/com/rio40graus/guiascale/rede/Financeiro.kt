package br.com.rio40graus.guiascale.rede

/*
 * Financeiro do guia — espelho de `src/lib/financeiroGuia.ts` do web.
 *
 * Os nomes dos campos são os do JSON da guias-api (GET guia/faturas), em
 * camelCase. Todos com valor padrão: o Gson monta o objeto sem passar pelo
 * construtor, e um campo ausente não pode virar null onde o Kotlin diz não-nulo.
 */

/** apuracao | fechada | disponivel | conferir | paga */
object SituacaoFaturaGuia {
    const val APURACAO = "apuracao"
    const val FECHADA = "fechada"
    const val DISPONIVEL = "disponivel"
    const val CONFERIR = "conferir"
    const val PAGA = "paga"
}

data class LancamentoFatura(
    val ref: String? = null,
    /** dd/MM/yyyy */
    val data: String? = null,
    val operacao: String? = null,
    val descricao: String? = null,
    val passeio: String? = null,
    val pax: String? = null,
    val paxDetalhe: String? = null,
    val refDiaria: String? = null,
    val confirmado: Boolean = true,
    /** diaria | reembolso | adiantamento | devolucao | ajuste | reserva */
    val tipo: String = "",
    val credito: Double = 0.0,
    val debito: Double = 0.0,
    val valor: Double = 0.0,
)

data class ComprovanteFatura(
    val id: Int = 0,
    val tipo: String? = null,
    val conferido: Boolean = false,
    val conferidoEm: String? = null,
    val conferidoPor: String? = null,
)

/** Pagamento real da fatura (já lançado pelo financeiro). */
data class PagamentoFatura(
    val data: String? = null,
    val hora: String? = null,
    val forma: String? = null,
    val conta: String? = null,
    val valor: Double = 0.0,
    val comprovantes: List<ComprovanteFatura> = emptyList(),
)

data class FaixaDiaria(
    val faixa: String = "",
    val valor: Double = 0.0,
    val aplicada: Boolean = false,
)

data class TabelaDiariasFatura(
    val titulo: String = "",
    val faixas: List<FaixaDiaria> = emptyList(),
)

/** Solicitação de pagamento que o guia enviou (ainda não é pagamento real). */
data class ComprovanteEnviado(
    val id: Int = 0,
    /** Valor que o guia informou ao pagar. */
    val valor: Double? = null,
    val data: String? = null,
    val hora: String? = null,
    val tipo: String? = null,
    val conferido: Boolean = false,
    /** Forma solicitada ("PIX · Bradesco"). */
    val forma: String? = null,
    /** pendente | aprovada | negada */
    val status: String? = null,
    /** Motivo, quando o financeiro negou. */
    val motivo: String? = null,
)

data class EtapasFatura(
    val apuracao: String? = null,
    val fechada: String? = null,
    val disponivel: String? = null,
    val conferir: String? = null,
    val paga: String? = null,
)

data class FaturaGuia(
    val id: String = "",
    val numero: String = "",
    val periodo: String = "",
    val situacao: String = SituacaoFaturaGuia.FECHADA,
    val vencimento: String? = null,
    val vencimentoPrevisto: String? = null,
    val pagaEm: String? = null,
    val faltamDias: Int? = null,
    val acertoPendente: String? = null,
    val adicionadaEm: String = "",
    val disponibilizadaEm: String? = null,
    val etapas: EtapasFatura? = null,
    val tabelaDiarias: TabelaDiariasFatura? = null,
    /** Fatura já enviada (ou vencida): o guia pode registrar o pagamento. */
    val podeAnexarComprovante: Boolean = false,
    val comprovantesEnviados: List<ComprovanteEnviado> = emptyList(),
    /** Total, crédito e débito vêm da mesma função do PDF do financeiro. */
    val total: Double = 0.0,
    val credito: Double = 0.0,
    val debito: Double = 0.0,
    val lancamentos: List<LancamentoFatura> = emptyList(),
    val pagamentos: List<PagamentoFatura> = emptyList(),
)

data class ResumoAtual(val total: Double = 0.0, val periodo: String = "")
data class ResumoAguardando(
    val total: Double = 0.0,
    val vencimento: String? = null,
    val faltamDias: Int? = null,
)
data class ResumoUltimaPaga(val total: Double = 0.0, val pagaEm: String? = null)

data class ResumoFinanceiroGuia(
    val atual: ResumoAtual? = null,
    val aguardando: ResumoAguardando? = null,
    val ultimaPaga: ResumoUltimaPaga? = null,
    val acertosPendentes: Int = 0,
)

/** Conta configurada no parceiro (guia), para o select do pagamento. */
data class FormaPagamentoGuia(
    val id: Int = 0,
    val tipo: String = "",
    val conta: String = "",
    /** "PIX · Stone" */
    val nome: String = "",
)

data class PaginaFaturas(
    val faturas: List<FaturaGuia> = emptyList(),
    val formasPagamento: List<FormaPagamentoGuia> = emptyList(),
    val resumo: ResumoFinanceiroGuia? = null,
    val pagina: Int = 1,
    val porPagina: Int = 10,
    val total: Int = 0,
    val totalPaginas: Int = 1,
)
