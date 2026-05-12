plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "com.faboit"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    test {
        useJUnitPlatform()
    }
}
