import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript{
    repositories{mavenCentral()}
}
plugins{
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.springframework.boot") version "2.7.0"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.10"
    kotlin("plugin.allopen") version "1.4.31"
}

allprojects{
    group = "it.polito.wa2.g15"
    version = "0.0.1-SNAPSHOT"
    tasks.withType<KotlinCompile>{
        kotlinOptions{
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
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

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.16.3")
    }
}