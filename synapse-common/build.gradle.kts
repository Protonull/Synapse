plugins {
	id("java-library")
}

dependencies {
	compileOnly(libs.bundles.common)
}

repositories {
	mavenCentral()
}
