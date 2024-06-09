plugins {
    id("java-library")
}

allprojects {
    apply(plugin = "java-library")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
    }

    tasks {
        compileJava {
            options.encoding = "UTF-8"
            options.release = 17
        }
    }
}
