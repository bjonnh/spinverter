import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.11"
    id("no.tornado.fxlauncher") version "1.0.20"
}

group = "net.nprod"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("no.tornado:tornadofx:1.7.17")
    compile(kotlin("reflect:1.3.11"))
    compile("org.openscience.cdk:cdk-core:2.2")
    compile("org.openscience.cdk:cdk-data:2.2")
    compile("org.openscience.cdk:cdk-smiles:2.2")
    compile("org.openscience.cdk:cdk-valencycheck:2.2")
    compile("org.openscience.cdk:cdk-depict:2.2")
    compile("org.openscience.cdk:cdk-libiocml:2.2")
    testCompile("junit", "junit", "4.12")
}

fxlauncher {
    applicationVendor = "Jonathan Bisson"
    applicationUrl = "https://github.com/bjonnh/spinverter"
    applicationMainClass = "net.nprod.spinverter.MainKt"
    acceptDowngrade = false
    deployTarget = "$buildDir"
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}