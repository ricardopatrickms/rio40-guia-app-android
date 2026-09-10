# Ambiente de build do GuiaScale.
#
# Existe para que ninguém precise instalar JDK e Android SDK na máquina: o
# `./gradlew` roda aqui dentro e o APK sai em app/build/outputs, na pasta do
# projeto mesmo, porque o código é montado como volume.
#
# Só compila. A instalação no aparelho fica de fora de propósito: o daemon do
# Docker Desktop roda dentro de uma VM e não enxerga nem o USB nem o servidor
# adb da máquina. Quem instala é o adb do host — ver ./instalar.sh.
FROM eclipse-temurin:17-jdk-jammy

# unzip para abrir o pacote do sdkmanager; git porque o Gradle de alguns
# plugins consulta o repositório para gerar versão.
RUN apt-get update && apt-get install -y --no-install-recommends \
        unzip \
        curl \
        git \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=${ANDROID_HOME}
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Versão do pacote de ferramentas de linha de comando (13.0). Fixa de
# propósito: "latest" muda sem aviso e quebra build que funcionava ontem.
ARG CMDLINE_TOOLS=13114758

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL -o /tmp/cmdline-tools.zip \
        https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# compileSdk/targetSdk 35 e build-tools da mesma linha — ver app/build.gradle.kts.
RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install \
        "platform-tools" \
        "platforms;android-35" \
        "build-tools;35.0.0"

# Usuário com o mesmo UID de quem está fora, senão o APK e a pasta build saem
# pertencendo ao root e o Android Studio depois não consegue mexer.
ARG UID=1000
ARG GID=1000
RUN groupadd -g ${GID} construtor 2>/dev/null || true \
    && useradd -u ${UID} -g ${GID} -m -s /bin/bash construtor \
    && chown -R ${UID}:${GID} ${ANDROID_HOME}

ENV GRADLE_USER_HOME=/home/construtor/.gradle

# A pasta precisa existir JÁ com o dono certo: o volume nomeado do
# docker-compose é iniciado a partir dela e herda essa permissão. Sem isso o
# Docker cria o volume vazio, pertencendo ao root, e o wrapper não consegue
# nem baixar o Gradle.
RUN mkdir -p ${GRADLE_USER_HOME} && chown ${UID}:${GID} ${GRADLE_USER_HOME}

# Pasta do keystore de debug. Precisa existir com o dono certo ANTES do bind
# mount do docker-compose: se o Docker criar o caminho sozinho, cria como root.
RUN mkdir -p /home/construtor/.android && chown ${UID}:${GID} /home/construtor/.android

USER construtor
WORKDIR /projeto

CMD ["./gradlew", "assembleDebug"]
