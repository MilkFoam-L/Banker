plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "1.56"
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.dokka") version "1.7.20"
}

taboolib {
    relocate("org.reflections","pers.neige.banker.libs.org.reflections")

    description {
        contributors {
            name("Neige")
        }
        dependencies {
            name("NeigeItems").with("bukkit")
            name("MythicMobs").with("bukkit")
            name("PlaceholderAPI").with("bukkit").optional(true)
        }
    }
    install(
        "common",
        "common-5",
        "module-chat",
        "module-configuration",
        "module-metrics",
        "platform-bukkit",
    )
    classifier = null
    version = "6.0.11-31"
}

configurations{
    maybeCreate("packShadow")
    get("compileOnly").extendsFrom(get("packShadow"))
    get("packShadow").extendsFrom(get("taboo"))
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/public")
    maven("https://repo.tabooproject.org/storages/public/releases")
    maven("https://r.irepo.space/maven/")
}

dependencies {
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("pers.neige.neigeitems:NeigeItems:1.15.47")
    taboo("org.reflections:reflections:0.10.2")
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