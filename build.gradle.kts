import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.3.11"
}

group = "net.nprod"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    implementation("org.openscience.cdk:cdk-core:2.2")
    implementation("org.openscience.cdk:cdk-data:2.2")
    implementation("org.openscience.cdk:cdk-smiles:2.2")
    implementation("org.openscience.cdk:cdk-valencycheck:2.2")
    implementation("org.openscience.cdk:cdk-depict:2.2")
    implementation("org.openscience.cdk:cdk-libiocml:2.2")
    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}