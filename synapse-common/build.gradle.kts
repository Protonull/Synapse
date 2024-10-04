plugins {
    id("java-library")
}

dependencies {
    compileOnly(libs.bundles.common)
    compileOnly(libs.bundles.nonmc)
}
