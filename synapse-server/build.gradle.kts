import org.apache.tools.ant.taskdefs.ExecuteJava

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

	implementation("com.squareup.okhttp3:okhttp:4.12.0")
	implementation("org.slf4j:slf4j-simple:2.0.16")
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
	getByName<JavaExec>("run") {
		environment("UUID_MAPPER_PATH" to "synapse/uuids.tsv")
		environment("USER_LIST_PATH" to "synapse/users.tsv")
		environment("ADMIN_LIST_PATH" to "synapse/admins.tsv")
	}
}
