import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }

val signingProperties = Properties().apply {
    val config = rootProject.file("keystore.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}

android {
    namespace = "com.qbox.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.qbox.tv"
        minSdk = 24
        // 与 OKTV 一致：规避电视生态 scoped storage / 后台限制，无 Play 上架需求
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = signingProperties.getProperty("storeFile")?.let { file(it) }
            storePassword = signingProperties.getProperty("storePassword")
            keyAlias = signingProperties.getProperty("keyAlias")
            keyPassword = signingProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        disable += "ExpiredTargetSdkVersion"
        abortOnError = false
    }

    packaging {
        resources.excludes += setOf("META-INF/versions/**", "META-INF/*.kotlin_module")
    }
}

configurations.configureEach {
    exclude(group = "xpp3", module = "xpp3")
    exclude(group = "stax", module = "stax-api")
    exclude(group = "com.android.support")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.annotation:annotation:1.8.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // 播放：media3 官方 maven（不走 FongMi fork）
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.8.0")

    // 网络 / JSON：同时也是 jar 兼容层的运行时依赖（类加载器可见）
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    // WebDAV：饭太 jar 的 AList 类源需要
    implementation("com.github.thegrizzlylabs:sardine-android:0.9")

    // 图片
    implementation("io.coil-kt:coil:2.7.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    testImplementation("junit:junit:4.13.2")
}
