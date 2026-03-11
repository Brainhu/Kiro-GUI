import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.intelliJPlatform)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // LSP communication
    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)

    // Kotlin serialization
    implementation(libs.kotlinx.serialization.json)

    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Markdown rendering
    implementation(libs.commonmark)

    // Testing - JUnit 5
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)

    // JUnit Vintage engine (required to run JUnit 3/4 BasePlatformTestCase tests on JUnit 5 platform)
    testRuntimeOnly(libs.junit.vintage.engine)

    // JUnit 4 (required by IntelliJ Platform test framework)
    testImplementation("junit:junit:4.13.2")

    // Testing - Kotest Property-Based Testing
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner.junit5)

    // IntelliJ Platform
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion"))

        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map {
            it.split(',').filter(String::isNotBlank)
        })

        plugins(providers.gradleProperty("platformPlugins").map {
            it.split(',').filter(String::isNotBlank)
        })

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        useJUnitPlatform()
    }
}
