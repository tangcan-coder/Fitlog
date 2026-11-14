plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fitlog.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fitlog.app"
        // Glasmio 的 AAR manifest 声明 minSdkVersion=33，低于这个值会在清单合并阶段直接失败。
        // 液态玻璃本身也依赖 API 31+ 的 RenderEffect / RuntimeShader。
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // 自用发布包：不做 R8（避免和 Glasmio 的反射/资源打架），
            // 但用 debug 签名，方便直接 gradlew assembleRelease 装到自己的手机上。
            // 关键收益是 isDebuggable=false —— debug 包里 Compose 跑在解释执行下，
            // 滚动玻璃界面会明显发涩，release 包能好一个量级。
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.glasmio.ui)
    implementation(libs.glasmio.scaffold)

    // 预览 / Layout Inspector 只在 debug 包里需要，不进正式包
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
