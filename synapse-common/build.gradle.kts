plugins {
    id("java-library")
}

version = rootProject.extra["project_version"]!!
group = "${rootProject.extra["maven_group"]}.common"

dependencies {
    compileOnly(libs.bundles.common)
}

repositories {
    mavenCentral()
}
