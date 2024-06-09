plugins {
    id("java-library")
    // // https://github.com/johnrengelman/shadow/releases
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

version = rootProject.extra["project_version"]!!
group = "${rootProject.extra["maven_group"]}.server"

base {
    archivesName.set("SynapseServer")
}

application {
    mainClass = "gjum.minecraft.civ.synapse.server.Server"
}

dependencies {
    implementation(project(":synapse-common"))
    implementation(libs.bundles.common)
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}

repositories {
    mavenCentral()
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }
}
