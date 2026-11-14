// AGP 9 内置 Kotlin 支持，默认带的是 KGP 2.2.10。
// 新版 Compose / 序列化库是按更高版本 Kotlin 编译的，这里显式把 KGP 抬到 2.4.21。
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
