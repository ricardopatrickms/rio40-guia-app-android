# GuiaScale — app Android do guia

Captura a posição do guia durante o passeio, com o app minimizado e a tela
apagada. É a função que o app web não consegue fazer: no navegador o rastreio
morre quando a aba perde o foco.

## Para rodar

Android Studio → Open → apontar para esta pasta. Ele baixa o SDK, gera o
`gradle-wrapper.jar` e sincroniza. Precisa de JDK 17.

O emulador serve para conferir tela e envio; use a aba **Location → Routes**
dos Extended Controls para simular movimento. Mas o teste que decide é em
celular real: o que precisa ser provado é sobreviver horas com a tela apagada
e a economia de bateria do fabricante ligada.

## Como está montado

**`rastreio/RastreioService`** — serviço em primeiro plano, o coração do app.
Captura a cada 15s, com deslocamento mínimo de 10m para a van parada não gerar
pontos repetidos. A notificação permanente é obrigatória no Android e é o que
garante que ninguém seja rastreado às escondidas.

**`dados/Posicao`** — a fila local (Room). Cada leitura é gravada ANTES de
tentar a rede: o guia atravessa túnel e estrada sem sinal, e é justamente esse
trecho que ninguém consegue reconstituir depois.

**`rastreio/EnvioWorker`** — esvazia a fila em lotes de até 500, pelo
WorkManager. Fica fora do serviço de propósito: rede e GPS falham por motivos
diferentes, e uma queda de sinal não pode interromper a captura.

**`rede/`** — Retrofit apontando para a guias-api. O token entra por
interceptor; o login é o mesmo do app web (campos `login` e `senha`).

## API

Base em `app/build.gradle.kts`, no `buildConfigField API_URL`. Hoje aponta
para homologação.

- `POST guia/login` — devolve `access_token`
- `POST guia/posicoes` — lote de até 500 pontos
- `GET  guia/posicoes?data=YYYY-MM-DD` — trajeto do dia

O servidor guarda em `guias_posicoes`, com duas datas: quando o aparelho leu e
quando o servidor recebeu. Podem estar horas separadas, e quem vale para o
trajeto é a primeira.

## O que ainda não existe

- **`mapa_id` nas posições.** A coluna existe e o app manda o campo, mas ele
  vai sempre nulo: o app ainda não busca qual mapa o guia está fazendo.
- **Religar depois de reiniciar o aparelho.** A permissão está declarada no
  manifesto, mas o receptor de boot não foi escrito.
- **Token seguro.** Está em SharedPreferences simples. Trocar por
  EncryptedSharedPreferences é uma linha.

## Os três obstáculos reais

Não são de código, e é onde esse tipo de app costuma falhar:

1. **Permissão "o tempo todo"** — o Android pede em duas etapas, e a segunda
   leva o usuário para uma tela de configurações. Quem escolhe "só com o app
   aberto" fica sem rastreio, em silêncio.
2. **Fabricantes** — Samsung e Xiaomi matam serviço legítimo para poupar
   bateria. É a causa nº 1 de "sumiu o rastro". Só se resolve levando o
   usuário à configuração de bateria.
3. **Play Store** — localização em segundo plano exige formulário de
   justificativa e vídeo na revisão do Google. Distribuição interna pula isso.
