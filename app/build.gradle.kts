plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "br.com.rio40graus.guiascale"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.rio40graus.guiascale"

        /*
         * minSdk 26 (Android 8).
         *
         * É a versão em que os serviços em primeiro plano e os canais de
         * notificação passaram a funcionar como o app precisa. Abaixo disso o
         * rastreio com a tela apagada não é confiável, e não vale manter código
         * para um aparelho que não entrega a função principal.
         */
        minSdk = 26
        targetSdk = 35

        versionCode = 1
        versionName = "0.1.0"


        /*
         * O logo da agência não está na guias-api: quem guarda é o Supabase do
         * app web, em `app_settings.logo_url`, e é de lá que o app web também
         * lê. Para não haver dois logos diferentes, o Android lê da mesma
         * fonte — ver rede/LogoDaAgencia.
         *
         * A chave abaixo é a `anon`, a mesma que vai no pacote JavaScript do
         * app web e aparece no navegador de qualquer um. Ela não é segredo: o
         * que protege a tabela é a RLS do Supabase, e a leitura de app_settings
         * é pública de propósito, senão a tela de login não teria como mostrar
         * o logo antes de alguém entrar.
         */
        /*
         * Chave do Google Maps para Android.
         *
         * Vai embutida no APK porque o SDK do Maps a lê do manifesto — não há
         * como escondê-la, e o Google não espera que se esconda: o que protege
         * a chave é a restrição no console, que a amarra ao nome do pacote
         * (br.com.rio40graus.guiascale) e à impressão SHA-1 do certificado de
         * assinatura. Sem esses dois conferindo, a chave não funciona em app
         * nenhum, mesmo extraída.
         *
         * Duas consequências práticas: a chave precisa listar o SHA-1 do
         * keystore de DEBUG para o mapa aparecer aqui, e o do keystore de
         * RELEASE para aparecer em produção. Se o mapa nascer cinza com o
         * resto funcionando, é quase sempre isso.
         *
         * Para testar outra chave sem editar o arquivo:
         *     ./gradlew -Pguiascale.mapsKey=SUA_CHAVE installDebug
         */
        manifestPlaceholders["mapsApiKey"] =
            (project.findProperty("guiascale.mapsKey") as String?)
                ?: "AIzaSyAJD4920A7GiI8GQIER0TgqQjWgDAgNN5g"

        buildConfigField("String", "SUPABASE_URL", "\"https://gvauopyruthqebwaxrqy.supabase.co\"")
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd2YXVvcHlydXRocWVid2F4cnF5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzczMTg2NTEsImV4cCI6MjA5Mjg5NDY1MX0.M3WMaR763_Us0FIt7UstBiHTCmQLUac4qDGE3NlCZqk\"",
        )
    }

    /*
     * Endereço da API, por tipo de build — trocar aqui, não no código.
     *
     * O debug fala com a guias-api que roda na máquina de quem desenvolve.
     * 10.0.2.2 é o endereço fixo com que o emulador enxerga o host: dentro
     * dele, `localhost` é o próprio Android, e uma chamada para a 8092 de lá
     * não sai do aparelho virtual.
     *
     * Em celular real esse endereço não existe. Passe o IP da máquina na rede:
     *
     *     ./gradlew -Pguiascale.apiUrl=http://192.168.0.10:8092/api/ installDebug
     *
     * Para testar contra homologação, o mesmo caminho serve — é só passar a
     * URL dela.
     */
    val apiDeDebug = (project.findProperty("guiascale.apiUrl") as String?)
        ?: "http://10.0.2.2:8092/api/"

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_URL", "\"$apiDeDebug\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_URL", "\"https://api-guia.titanx.ia.br/api/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose: a BOM fixa as versões do conjunto, para não brigarem entre si.
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Posição do aparelho: o Fused junta GPS, rede e sensores, e é o que
    // consegue precisão razoável sem manter o GPS ligado o tempo todo.
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Fila local das posições — o que segura o rastro enquanto não há sinal.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Reenvio: o WorkManager sobrevive a fechar o app e a reiniciar o aparelho.
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    /*
     * Mapa: Google Maps.
     *
     * maps-compose é o invólucro oficial para Compose — ele cuida do ciclo de
     * vida do MapView, que fora do Compose exigiria repassar onCreate, onResume,
     * onPause e onDestroy na mão.
     *
     * Exige Play Services no aparelho. Não é problema no público do app (celular
     * de guia, com Play Store), mas quebra em emulador de imagem "AOSP" — use
     * uma imagem "Google APIs" ou "Play Store".
     */
    implementation("com.google.maps.android:maps-compose:6.4.1")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
}
