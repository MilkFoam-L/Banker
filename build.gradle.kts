import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.dokka") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/public")
}

dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.bstats:bstats-bukkit:3.0.2")
}

tasks {
    withType<ShadowJar> {
        archiveClassifier.set("")
        exclude("META-INF/maven/**")
        exclude("META-INF/tf/**")
        exclude("module-info.java")
        // kotlin
        relocate("kotlin.", "pers.neige.neigeitems.libs.kotlin.") {
            exclude("kotlin.Metadata")
        }
        // bstats
        relocate("org.bstats", "pers.neige.neigeitems.libs.bstats")
    }
    kotlinSourcesJar {
        // include subprojects
        rootProject.subprojects.forEach { from(it.sourceSets["main"].allSource) }
    }
    build {
        dependsOn(shadowJar)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

tasks.create("apiJar", Jar::class){
    dependsOn(tasks.compileJava, tasks.compileKotlin)
    from(tasks.compileJava, tasks.compileKotlin)

    // clean no-class file
    include { it.isDirectory or it.name.endsWith(".class") }
    includeEmptyDirs = false

    archiveClassifier.set("api")
}

tasks.assemble{
    dependsOn(tasks["apiJar"])
}

// 将plugin.yml中的"${version}"替换为插件版本
tasks.register("replaceVersionInPluginYml") {
    doLast {
        val inputFile = File("src/main/resources/plugin.yml")
        val outputFile = File("build/resources/main/plugin.yml")

        val inputText = inputFile.readText()

        val projectVersion = version.toString()
        val replacedText = inputText.replace("\${version}", projectVersion)

        outputFile.writeText(replacedText)
    }
}

tasks.named("assemble") {
    dependsOn("replaceVersionInPluginYml")
}
