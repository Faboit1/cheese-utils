plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "com.faboit"
version = "1.0.0"
base.archivesName.set("CheeseUtils")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.46.0.1")
    implementation("com.mysql:mysql-connector-j:9.0.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
