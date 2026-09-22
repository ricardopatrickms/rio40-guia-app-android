package br.com.rio40graus.guiascale

import androidx.annotation.DrawableRes

data class IncisoLei(val rom: String, val texto: String)

data class ArtigoLei(
    val numero: String,
    val titulo: String? = null,
    val texto: String,
    val comentario: String,
    val incisos: List<IncisoLei> = emptyList(),
)

data class MetaSecaoLei(
    val id: String,
    val numero: Int,
    val titulo: String,
    val resumo: String,
    val searchable: String,
    /** Mesmo ícone Lucide do web (`LeiDoGuia.tsx`). */
    @DrawableRes val iconeRes: Int,
)

data class CategoriaLei(
    val nome: String,
    val ambito: String,
    val requisitos: List<String>,
    val obs: String,
)

data class PassoHabilitacaoLei(val n: Int, val titulo: String, val desc: String)

data class DeverLei(val titulo: String, val desc: String)

data class VedacaoLei(
    val rom: String,
    val titulo: String,
    val proibido: String,
    val consequencia: String,
)

data class PenalidadeLei(val nome: String, val quando: String, val efeito: String, val obs: String)

data class NormaLei(val nome: String, val desc: String)

data class FaqLei(val q: String, val a: String)

data class ElementoDefinicaoLei(val elemento: String, val descricao: String)

data class TipoExcursaoLei(val nome: String, val descricao: String)

val ARTIGOS: List<ArtigoLei> = listOf(
    ArtigoLei(
        numero = "Art. 1º",
        titulo = "Exclusividade profissional",
        texto = "O exercício da atividade de guia de turismo, em todo o território nacional, é privativo dos profissionais habilitados na forma desta lei.",
        comentario = "Princípio mais fundamental da lei. Somente quem estiver formalmente habilitado pode exercer a atividade. Quem se apresenta como guia sem habilitação está sujeito às penalidades e pode ser responsabilizado civil e penalmente em caso de danos ao turista.",
    ),
    ArtigoLei(
        numero = "Art. 2º",
        titulo = "Definição de guia de turismo",
        texto = "Para os efeitos desta lei, considera-se guia de turismo o profissional que, mediante remuneração, acompanha, orienta e transmite informações a pessoas ou grupos em visitas, excursões urbanas, municipais, estaduais, interestaduais, internacionais ou especializadas.",
        comentario = "A remuneração é elemento constitutivo. O voluntário gratuito não está, em princípio, sujeito às exigências. Cada tipo de visita corresponde a uma categoria de habilitação distinta.",
    ),
    ArtigoLei(
        numero = "Art. 3º",
        titulo = "Habilitação por curso de formação",
        texto = "A habilitação do guia de turismo será obtida mediante aprovação em curso de formação, organizado e reconhecido na forma em que dispuser o Poder Executivo. § 1º O Poder Executivo disporá sobre requisitos, currículo e carga horária mínimos. § 2º Os guias já habilitados pela Embratur ficam dispensados da exigência prevista no caput.",
        comentario = "Os cursos são oferecidos por instituições credenciadas pelo Ministério do Turismo. O § 2º é cláusula de salvaguarda dos direitos adquiridos pelos guias anteriores à lei.",
    ),
    ArtigoLei(
        numero = "Art. 4º",
        titulo = "Categorias de habilitação",
        texto = "A habilitação do guia de turismo far-se-á nas seguintes categorias:",
        comentario = "As categorias determinam o alcance geográfico. Um guia regional do RJ não pode conduzir grupos em SP sem a habilitação correspondente. Guias especializados se encaixam nas categorias conforme regulamento específico.",
        incisos = listOf(
            IncisoLei("I", "Guia de Turismo de Âmbito Nacional — autorizado a conduzir visitantes em todo o território nacional."),
            IncisoLei("II", "Guia de Turismo de Âmbito Regional — autorizado a atuar em uma ou mais unidades da federação."),
            IncisoLei("III", "Guia de Turismo Local — autorizado a conduzir visitantes em uma área ou atrativo específico."),
        ),
    ),
    ArtigoLei(
        numero = "Art. 5º",
        titulo = "Vedações ao guia de turismo",
        texto = "Ao guia de turismo é vedado:",
        comentario = "Artigo mais importante sob o aspecto disciplinar. O inciso V proíbe comissões que prejudiquem o turista. O inciso VI — abandono do grupo — pode gerar responsabilidade civil e criminal séria.",
        incisos = listOf(
            IncisoLei("I", "Exercer a atividade sem estar devidamente habilitado e cadastrado nos termos desta lei."),
            IncisoLei("II", "Praticar qualquer ato que atente contra a dignidade do turista ou lhe cause dano moral, físico ou material."),
            IncisoLei("III", "Induzir, facilitar ou aceitar qualquer prática que prejudique os interesses dos turistas ou das empresas que o contratam."),
            IncisoLei("IV", "Divulgar informações falsas ou enganosas sobre locais, serviços ou preços."),
            IncisoLei("V", "Receber comissões, gratificações ou vantagens de estabelecimentos comerciais que prejudiquem os interesses dos turistas que acompanha."),
            IncisoLei("VI", "Abandonar o grupo de turistas durante a realização do serviço contratado, salvo motivo de força maior."),
        ),
    ),
    ArtigoLei(
        numero = "Art. 6º",
        titulo = "Responsabilidade pela segurança",
        texto = "O guia de turismo é responsável pela segurança e bem-estar dos turistas que acompanha, devendo adotar todas as medidas necessárias para prevenir acidentes e atender situações de emergência.",
        comentario = "O guia não é apenas um narrador: é legalmente responsável pelo bem-estar físico e segurança do grupo. Implica conhecer rotas seguras, riscos locais, primeiros socorros e acionar emergência quando necessário.",
    ),
    ArtigoLei(
        numero = "Art. 7º",
        titulo = "Cadastur e competência fiscalizatória",
        texto = "O cadastramento e o controle do exercício da atividade de guia de turismo são de competência do Ministério da Indústria, do Comércio e do Turismo, que baixará as normas regulamentares necessárias.",
        comentario = "A competência foi transferida para o Ministério do Turismo, que instituiu o Cadastur. O certificado é obrigatório, deve ser renovado periodicamente e portado durante o serviço.",
    ),
    ArtigoLei(
        numero = "Art. 8º",
        titulo = "Penalidades",
        texto = "O exercício em desacordo com a lei sujeitará o infrator às seguintes penalidades: § 1º O cancelamento do cadastro implica a proibição definitiva do exercício da profissão. § 2º As penalidades não excluem a responsabilidade civil ou criminal do infrator.",
        comentario = "Penalidades progressivas. O § 2º é crucial: sanções administrativas NÃO excluem ação civil (indenização) ou criminal.",
        incisos = listOf(
            IncisoLei("I", "Advertência por escrito."),
            IncisoLei("II", "Multa de 5 a 50 vezes o maior salário mínimo vigente no País, dobrada na reincidência."),
            IncisoLei("III", "Cancelamento do cadastro."),
        ),
    ),
    ArtigoLei(
        numero = "Art. 9º",
        titulo = "Apreensão de material",
        texto = "O guia que exercer a atividade sem a devida habilitação e cadastro ficará sujeito a apreensão de qualquer material utilizado no exercício irregular da atividade.",
        comentario = "Medida cautelar para impedir a continuação imediata da irregularidade. Inclui microfones, bandeiras, documentos falsos etc.",
    ),
    ArtigoLei(
        numero = "Art. 10",
        titulo = "Responsabilidade solidária da empresa",
        texto = "As empresas de turismo são solidariamente responsáveis pelas infrações cometidas pelos guias que contratarem, quando não exigirem a comprovação de habilitação e cadastro previstos nesta lei.",
        comentario = "Crucial para receptivos como a Rio40º. Verificar o Cadastur ativo não é só boa prática — é obrigação legal com consequências financeiras diretas.",
    ),
    ArtigoLei(
        numero = "Art. 11",
        titulo = "Regulamentação",
        texto = "O Poder Executivo regulamentará esta lei no prazo de noventa dias, a contar de sua publicação.",
        comentario = "Regulamentado pelo Decreto nº 946/1993, que detalhou procedimentos, categorias, currículos e fiscalização.",
    ),
    ArtigoLei(
        numero = "Art. 12",
        titulo = "Vigência",
        texto = "Esta lei entra em vigor na data de sua publicação.",
        comentario = "Em vigor desde 28 de janeiro de 1993, em todo o território nacional.",
    ),
)

val CATEGORIAS: List<CategoriaLei> = listOf(
    CategoriaLei(
        nome = "Nacional",
        ambito = "Todo o território brasileiro",
        requisitos = listOf(
            "Curso com carga horária plena",
            "Domínio de idioma estrangeiro",
            "Cadastro nacional ativo",
        ),
        obs = "Categoria mais abrangente e exigente em formação.",
    ),
    CategoriaLei(
        nome = "Regional",
        ambito = "Uma ou mais unidades da federação (estados)",
        requisitos = listOf(
            "Curso regional reconhecido",
            "Conhecimento específico da região",
            "Cadastro estadual ativo",
        ),
        obs = "Cada estado pode ter regulamentos complementares.",
    ),
    CategoriaLei(
        nome = "Local",
        ambito = "Área ou atrativo específico (museu, parque, complexo)",
        requisitos = listOf(
            "Formação específica para o atrativo",
            "Autorização do órgão gestor",
            "Cadastro local",
        ),
        obs = "Guias de museus, parques nacionais e sítios históricos.",
    ),
)

val HABILITACAO_PASSOS: List<PassoHabilitacaoLei> = listOf(
    PassoHabilitacaoLei(1, "Escolha da Instituição de Ensino", "Procurar curso de formação credenciado pelo Ministério do Turismo ou reconhecido pelo Cadastur para a categoria desejada."),
    PassoHabilitacaoLei(2, "Conclusão do Curso", "Cumprir carga horária mínima — inclui história, cultura, geografia, idiomas e ética profissional."),
    PassoHabilitacaoLei(3, "Aprovação e Certificação", "Obter certificado de conclusão com aprovação nas avaliações da instituição credenciada."),
    PassoHabilitacaoLei(4, "Inscrição no Cadastur", "Registrar-se no sistema apresentando RG, CPF, comprovante de escolaridade, certificado do curso e foto."),
    PassoHabilitacaoLei(5, "Obtenção do Certificado Cadastur", "Após análise e aprovação, receber o certificado que autoriza o exercício legal da profissão."),
    PassoHabilitacaoLei(6, "Manutenção e Renovação", "Manter Cadastur ativo e renovar dentro do prazo (validade de 2 anos)."),
)

val DIREITOS: List<String> = listOf(
    "Exclusividade no exercício da atividade — proteção contra concorrência ilegal",
    "Identificação pública como profissional habilitado mediante porte do Cadastur",
    "Remuneração justa de acordo com o contrato firmado",
    "Recusa a ordens que contrariem a lei, ética ou segurança",
    "Acesso a capacitações e atualização via Cadastur",
    "Acesso gratuito ou com desconto a atrativos turísticos",
    "Reconhecimento profissional equivalente a outras profissões regulamentadas",
)

val PRERROGATIVAS: List<String> = listOf(
    "Acesso facilitado a atrativos mediante apresentação do Cadastur",
    "Prioridade de atendimento em locais que reconhecem o profissional",
    "Uso de equipamentos de apoio (microfone, bandeira) sem restrições",
    "Intermediação entre turistas e fornecedores com base em contratos legítimos",
    "Recusa a situações que comprometam a segurança, sem prejuízo ao contrato",
)

val DEVERES: List<DeverLei> = listOf(
    DeverLei("Segurança do grupo", "Garantir integridade física e bem-estar; medidas preventivas e resposta a emergências."),
    DeverLei("Veracidade das informações", "Transmitir apenas informações verdadeiras, verificadas e culturalmente respeitosas."),
    DeverLei("Pontualidade e roteiro", "Cumprir horários e o roteiro contratado; comunicar alterações."),
    DeverLei("Confidencialidade", "Sigilo sobre dados dos turistas e da empresa contratante."),
    DeverLei("Imparcialidade comercial", "Não favorecer estabelecimentos em detrimento dos turistas."),
    DeverLei("Comunicação de problemas", "Reportar imediatamente qualquer ocorrência, acidente ou risco."),
    DeverLei("Porte do Cadastur", "Portar o certificado ativo e apresentá-lo quando solicitado."),
)

val VEDACOES: List<VedacaoLei> = listOf(
    VedacaoLei("I", "Exercício sem habilitação", "Atuar como guia sem estar habilitado e cadastrado.", "Penalidade do Art. 8º; apreensão de materiais (Art. 9º); empresa responde solidariamente (Art. 10)."),
    VedacaoLei("II", "Atos contra a dignidade do turista", "Praticar ato que atente contra dignidade ou cause dano moral, físico ou material.", "Responsabilidade civil e criminal (lesão, injúria, calúnia). Procon e Ministério do Turismo."),
    VedacaoLei("III", "Prejudicar interesses dos turistas ou da empresa", "Induzir, facilitar ou aceitar prática prejudicial.", "Rescisão imediata do contrato e processo administrativo."),
    VedacaoLei("IV", "Informações falsas ou enganosas", "Divulgar informações falsas sobre locais, serviços ou preços.", "Viola o CDC — ação por propaganda enganosa."),
    VedacaoLei("V", "Comissões de estabelecimentos", "Receber comissões que prejudiquem os interesses dos turistas.", "Sanção administrativa; frequentemente fiscalizada."),
    VedacaoLei("VI", "Abandono do grupo", "Abandonar o grupo durante o serviço, salvo força maior.", "Vedação mais grave: pode configurar crime de abandono, omissão de socorro e responsabilidade civil plena."),
)

val PENALIDADES: List<PenalidadeLei> = listOf(
    PenalidadeLei("Advertência Escrita", "Infração leve, primeiro contato com irregularidade", "Registro no histórico — não impede o exercício", "Serve de alerta formal"),
    PenalidadeLei("Multa (5 a 50 salários mínimos)", "Infrações de média gravidade ou reincidência", "Pagamento ao órgão; dobro na reincidência", "Valor atualizado com o salário mínimo"),
    PenalidadeLei("Cancelamento do Cadastro", "Infrações graves, reincidência sistemática", "Proibição DEFINITIVA do exercício profissional", "§ 1º: definitivo e irreversível"),
)

val NORMAS: List<NormaLei> = listOf(
    NormaLei("Decreto nº 946/1993", "Regulamenta a lei: procedimentos de habilitação, categorias, formação, currículos mínimos e fiscalização."),
    NormaLei("Portarias do Ministério do Turismo", "Atualizam critérios de habilitação, requisitos curriculares, regras do Cadastur e diretrizes especializadas."),
    NormaLei("Resoluções da Embratur", "Anteriores à lei; algumas vigoram complementarmente. Definiram padrões éticos e operacionais."),
    NormaLei("Lei nº 11.771/2008 — Política Nacional de Turismo", "Reforça o papel do guia e confirma o Cadastur como instrumento central de regulação."),
    NormaLei("Código de Defesa do Consumidor (Lei nº 8.078/1990)", "Aplica-se ao turista como consumidor: responsabilidade por danos, publicidade enganosa, direito à informação."),
    NormaLei("Legislações estaduais e municipais", "Complementam a lei federal, especialmente para guias locais em sítios históricos e parques."),
)

val APLICACAO_GUIA: List<String> = listOf(
    "Manter o Cadastur ativo e renovado",
    "Portar o certificado em todos os serviços",
    "Atuação somente dentro da categoria habilitada",
    "Comunicar imediatamente qualquer ocorrência",
    "Não realizar vendas ou aceitar comissões",
    "Nunca abandonar o grupo durante o serviço",
    "Informar verazmente sobre roteiros e atrações",
)

val APLICACAO_EMPRESA: List<String> = listOf(
    "Verificar o Cadastur de todo guia antes de contratar",
    "Manter registro de Cadastur de todos os guias ativos",
    "Não escalar guia com Cadastur vencido ou cancelado",
    "Responder solidariamente por infrações de guias contratados",
    "Capacitar os guias nos procedimentos da empresa",
    "Reportar irregularidades ao Ministério do Turismo",
)

val FAQ: List<FaqLei> = listOf(
    FaqLei("Um guia com Cadastur vencido pode trabalhar em serviços emergenciais?", "Não. Cadastur vencido equivale a inativo — exercício irregular, sujeito ao Art. 8º. Renove com pelo menos 30 dias de margem."),
    FaqLei("A empresa pode ser multada mesmo sem saber que o Cadastur estava vencido?", "Sim. O Art. 10 exige que a empresa comprove a habilitação. Ignorância não afasta a solidariedade."),
    FaqLei("Um guia regional do RJ pode conduzir passeio em Angra dos Reis (RJ)?", "Depende do escopo do Cadastur. Se a habilitação regional cobre o Estado do RJ, sim. Se for específica para a cidade do RJ, pode não incluir Angra."),
    FaqLei("O turista pode exigir ver o Cadastur do guia?", "Sim. O guia deve apresentar quando solicitado. Recusa pode ser reportada ao Ministério do Turismo."),
    FaqLei("Um guia pode aceitar gorjeta de turista?", "Sim, se for espontânea e não comprometer a imparcialidade. A vedação do Art. 5º V refere-se a comissões de estabelecimentos que prejudiquem o turista."),
    FaqLei("O que fazer se um fornecedor oferecer comissão ao guia?", "Recusar e comunicar à Rio40º. Aceitação pode resultar em cancelamento do Cadastur."),
    FaqLei("Um guia pode conduzir turistas sem contrato formal com a empresa?", "Para a lei do guia, importa o Cadastur ativo. Porém, sem contrato há riscos trabalhistas, fiscais e civis. A Rio40º exige formalização sempre."),
)

val DEFINICAO_ELEMENTOS: List<ElementoDefinicaoLei> = listOf(
    ElementoDefinicaoLei("Remuneração", "Serviço prestado mediante pagamento; voluntário gratuito não se enquadra."),
    ElementoDefinicaoLei("Acompanhar", "Presença física junto ao grupo durante todo o serviço."),
    ElementoDefinicaoLei("Orientar", "Dirigir o percurso, gerir o grupo e garantir segurança."),
    ElementoDefinicaoLei("Transmitir informações", "Narrar, explicar e contextualizar pontos e atrações."),
    ElementoDefinicaoLei("Pessoas ou grupos", "Pode ser individual ou coletivo, sem limites na lei."),
)

val TIPOS_EXCURSAO: List<TipoExcursaoLei> = listOf(
    TipoExcursaoLei("Urbanas", "city tours e roteiros dentro de um município."),
    TipoExcursaoLei("Municipais", "foco em atrativos de um município."),
    TipoExcursaoLei("Estaduais", "destinos dentro de um mesmo estado."),
    TipoExcursaoLei("Interestaduais", "roteiros entre estados."),
    TipoExcursaoLei("Internacionais", "grupos estrangeiros no Brasil ou brasileiros no exterior."),
    TipoExcursaoLei("Especializadas", "ecoturismo, aventura, espeleo, náutico, rural."),
)

private fun searchableArtigos(): String = ARTIGOS.joinToString(" ") { a ->
    val inc = a.incisos.joinToString(" ") { it.texto }
    "${a.numero} ${a.titulo.orEmpty()} ${a.texto} ${a.comentario} $inc"
}

private fun searchableFaq(): String = FAQ.joinToString(" ") { "${it.q} ${it.a}" }

val SECOES_LEI: List<MetaSecaoLei> = listOf(
    MetaSecaoLei(
        id = "contexto",
        numero = 1,
        titulo = "Contexto histórico e motivação",
        resumo = "Por que a lei foi criada e o cenário do turismo nos anos 1990.",
        searchable = "contexto histórico embratur itamar franco 1993 decreto 946 turismo receptivo",
        iconeRes = R.drawable.ic_lucide_book_open,
    ),
    MetaSecaoLei(
        id = "artigos",
        numero = 2,
        titulo = "Texto integral comentado — Artigos 1 a 12",
        resumo = "Cada artigo com texto original e comentário analítico.",
        searchable = searchableArtigos(),
        iconeRes = R.drawable.ic_lucide_scale,
    ),
    MetaSecaoLei(
        id = "definicao",
        numero = 3,
        titulo = "Conceito e definição legal",
        resumo = "Os 5 elementos constitutivos e os tipos de excursão.",
        searchable = "definição remuneração acompanhar orientar transmitir informações urbanas municipais estaduais interestaduais internacionais especializadas",
        iconeRes = R.drawable.ic_lucide_file_text,
    ),
    MetaSecaoLei(
        id = "categorias",
        numero = 4,
        titulo = "Categorias — Nacional, Regional e Local",
        resumo = "Alcance geográfico, requisitos e regras de sobreposição.",
        searchable = "categorias nacional regional local âmbito sobreposição",
        iconeRes = R.drawable.ic_lucide_map_pin,
    ),
    MetaSecaoLei(
        id = "habilitacao",
        numero = 5,
        titulo = "Habilitação, formação e Cadastur",
        resumo = "Passo a passo para se habilitar e manter o Cadastur ativo.",
        searchable = "habilitação cadastur formação curso renovação validade 2 anos",
        iconeRes = R.drawable.ic_lucide_graduation_cap,
    ),
    MetaSecaoLei(
        id = "direitos",
        numero = 6,
        titulo = "Direitos e prerrogativas",
        resumo = "O que a habilitação garante ao guia em termos de direitos.",
        searchable = "direitos prerrogativas exclusividade remuneração capacitação acesso",
        iconeRes = R.drawable.ic_lucide_award,
    ),
    MetaSecaoLei(
        id = "deveres",
        numero = 7,
        titulo = "Deveres e responsabilidades legais",
        resumo = "Obrigações fundamentais e responsabilidade civil/criminal.",
        searchable = "deveres responsabilidade civil criminal segurança veracidade pontualidade confidencialidade imparcialidade",
        iconeRes = R.drawable.ic_lucide_shield_alert,
    ),
    MetaSecaoLei(
        id = "vedacoes",
        numero = 8,
        titulo = "Vedações — o que é proibido",
        resumo = "As 6 vedações do Art. 5º com conduta proibida e consequências.",
        searchable = "vedações proibido habilitação dignidade comissões abandono informações falsas",
        iconeRes = R.drawable.ic_lucide_x_circle,
    ),
    MetaSecaoLei(
        id = "penalidades",
        numero = 9,
        titulo = "Infrações, penalidades e fiscalização",
        resumo = "Escala progressiva de sanções e como funciona a fiscalização.",
        searchable = "penalidades advertência multa cancelamento fiscalização ministério turismo",
        iconeRes = R.drawable.ic_lucide_gavel,
    ),
    MetaSecaoLei(
        id = "normas",
        numero = 10,
        titulo = "Regulamentação e normas complementares",
        resumo = "Decretos, portarias e leis correlatas que complementam a 8.623.",
        searchable = "decreto 946 portarias embratur lei 11771 política nacional turismo cdc estaduais municipais",
        iconeRes = R.drawable.ic_lucide_file_text,
    ),
    MetaSecaoLei(
        id = "aplicacao",
        numero = 11,
        titulo = "Aplicação prática na Rio40º Turismo",
        resumo = "Como a lei se aplica no dia a dia da operação receptiva.",
        searchable = "aplicação rio40 prática verificação cadastur empresa solidária",
        iconeRes = R.drawable.ic_lucide_building_2,
    ),
    MetaSecaoLei(
        id = "faq",
        numero = 12,
        titulo = "Perguntas frequentes e situações reais",
        resumo = "Respostas rápidas para dúvidas comuns do dia a dia.",
        searchable = searchableFaq(),
        iconeRes = R.drawable.ic_lucide_help_circle,
    ),
)
