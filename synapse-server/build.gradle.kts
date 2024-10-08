plugins {
    id("java")
    id("application")
    // ShadowJar (https://github.com/GradleUp/shadow/releases)
    id("com.gradleup.shadow") version "8.3.3"
}

group = "gjum.minecraft.civ"
version = "SNAPSHOT"

dependencies {
    implementation(project(":synapse-common"))
    implementation(libs.bundles.common)
    implementation(libs.bundles.nonmc)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.8")
}

base {
    archivesName = "SynapseServer"
}

application {
    mainClass = "gjum.minecraft.civ.synapse.server.Server"
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        mergeServiceFiles()
    }
    processResources {
        filesMatching("logback.xml") {
            expand(
                "logger_level" to (rootProject.findProperty("synapseLoggerLevel") ?: "INFO")
            )
        }
    }
    getByName<JavaExec>("run") {
        environment("SYNAPSE_GAME_ADDRESS" to "localhost:25565")
        environment("SYNAPSE_REQUIRES_AUTH" to "false")
        environment("SYNAPSE_UUID_MAPPER_PATH" to "synapse/uuids.tsv")
        environment("SYNAPSE_USER_LIST_PATH" to "synapse/users.tsv")
        environment("SYNAPSE_ADMIN_LIST_PATH" to "synapse/admins.tsv")
    }
}
