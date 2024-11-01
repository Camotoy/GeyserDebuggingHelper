plugins {
    id("java")
}

group = "net.camotoy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://repo.opencollab.dev/maven-releases")
}

dependencies {
    compileOnly("org.geysermc.geyser:core:2.4.4-SNAPSHOT")
}