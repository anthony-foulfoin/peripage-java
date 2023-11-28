plugins {
    id("java")
    id("application")
}

group = "io.peripage"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "io.peripage.Main"
}

dependencies {
    implementation("io.ultreia:bluecove:2.1.1")
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("net.glxn:qrgen:1.4")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:5.6.0")

}

tasks.test {
    useJUnitPlatform()
}
