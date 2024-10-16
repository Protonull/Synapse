plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
}

version = "${rootProject.extra["project_version"]}-${project.extra["minecraft_version"]}"
group = "${rootProject.extra["maven_group"]}.mod"

dependencies {
    minecraft("com.mojang:minecraft:${project.extra["minecraft_version"]}")
    loom {
        @Suppress("UnstableApiUsage")
        mappings(layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:${project.extra["parchment_name"]}:${project.extra["parchment_version"]}@zip")
        })
    }

    modImplementation("net.fabricmc:fabric-loader:${project.extra["fabric_loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.extra["fabric_api_version"]}")

    project(":synapse-common").also {
        implementation(it)
        include(it)
    }

    modImplementation("dev.isxander:yet-another-config-lib:${project.extra["yacl_version"]}")

    // This is quite literally just for LegacyComponentSerializer. If some other way is found to convert chat components
    // into fully legacy-formatted strings, please do let me know.
    "net.kyori:adventure-platform-fabric:5.14.1".also {
        modImplementation(it)
        include(it)
    }
    "net.kyori:adventure-text-serializer-legacy:4.17.0".also {
        implementation(it)
        include(it)
    }

    "maven.modrinth:combatradar:${project.extra["combatradar_version"]}".also {
        modCompileOnly(it)
        //modLocalRuntime(it) // Uncomment if you want to test
    }

    "maven.modrinth:voxelmap-updated:${project.extra["voxelmap_version"]}".also {
        modCompileOnly(it)
        //modLocalRuntime(it) // Uncomment if you want to test
    }

    //modLocalRuntime("maven.modrinth:journeymap:${project.extra["journeymap_version"]}") // Uncomment if you want to test
    modCompileOnly("info.journeymap:journeymap-api-fabric:${project.extra["journeymap_api_version"]}")

    // This is literally only here to make Minecraft SHUT UP about non-signed messages while testing.
    // https://modrinth.com/mod/no-chat-reports/version/Fabric-1.21-v2.8.0
    modLocalRuntime("maven.modrinth:no-chat-reports:riMhCAII")
}

repositories {
    maven(url = "https://maven.parchmentmc.org") {
        name = "ParchmentMC"
    }
    // For YACL
    maven(url = "https://maven.isxander.dev/releases/") {
        name = "Xander Maven"
        content {
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("dev.isxander")
            @Suppress("UnstableApiUsage")
            includeGroupAndSubgroups("org.quiltmc")
        }
    }
    maven(url = "https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven(url = "https://jm.gserv.me/repository/maven-public/") {
        name = "JourneyMap (Public)"
    }
}

base {
    archivesName.set("${project.extra["archives_base_name"]}")
}

loom {
    accessWidenerPath = file("src/main/resources/synapse.accesswidener")
    runConfigs.configureEach {
        this.programArgs.addAll("--username LocalModTester".split(" "))
    }
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "mod_name" to project.extra["mod_name"],
                "mod_version" to rootProject.extra["project_version"],
                "mod_description" to project.extra["mod_description"],
                "copyright_licence" to rootProject.extra["copyright_licence"],

                "mod_home_url" to project.extra["mod_home_url"],
                "mod_source_url" to project.extra["mod_source_url"],
                "mod_issues_url" to project.extra["mod_issues_url"],

                "minecraft_version" to project.extra["minecraft_version"],
                "fabric_loader_version" to project.extra["fabric_loader_version"],
                "fabric_api_version" to project.extra["fabric_api_version"],
                "yacl_version" to project.extra["yacl_version"],
            )
        }
        filesMatching("assets/synapse/lang/en_us.json") {
            expand("mod_name" to project.extra["mod_name"])
        }
    }
    register<Delete>("cleanJar") {
        delete(fileTree("./dist") {
            include("*.jar")
        })
    }
    register<Copy>("copyJar") {
        dependsOn(getByName("cleanJar"))
        from(getByName("remapJar"))
        into("./dist")
        rename("(.*?)\\.jar", "\$1-fabric.jar")
    }
    build {
        dependsOn(getByName("copyJar"))
    }
}
