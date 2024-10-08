plugins {
    id("java-library")
}

allprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
    }

    tasks {
        compileJava {
            options.encoding = "UTF-8"
            options.release = 21
        }
    }
}

tasks {
    create("testClient") {
        dependsOn(
            ":synapse-mod:clean",
            ":synapse-mod:runClient"
        )
    }
    create("testServer") {
        doFirst {
            project.ext.set("synapseLoggerLevel", "DEBUG")
        }
        finalizedBy(
            ":synapse-server:clean",
            ":synapse-server:run"
        )
    }
}
