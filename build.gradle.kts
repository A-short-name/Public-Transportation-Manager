import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript{
    repositories{mavenCentral()}
}
plugins{
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.springframework.boot") version "2.7.0"
}

allprojects{
    group = "it.polito.wa2.g15"
    version = "0.0.1-SNAPSHOT"
    tasks.withType<KotlinCompile>{
        kotlinOptions{
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
subprojects{
    repositories {
        mavenCentral()
    }
}
