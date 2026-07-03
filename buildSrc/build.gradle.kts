plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.plugins.kotlin.multiplatform.toDependency())
    implementation(libs.plugins.android.library.toDependency())
    implementation(libs.plugins.vanniktech.publish.toDependency())
}

fun Provider<PluginDependency>.toDependency() =
    map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
