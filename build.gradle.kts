
group = "io.github.abaddon.kcqrs"
version = "0.0.2"

object Meta {
    const val desc = "KCQRS EventStoreDB repository library"
    const val license = "Apache-2.0"
    const val githubRepo = "abaddon/kcqrs-EventStoreDB"
    const val release = "https://s01.oss.sonatype.org/service/local/"
    const val snapshot = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    const val developerName = "Stefano Longhi"
    const val developerOrganization = ""
    const val organizationUrl = "https://github.com/abaddon"
}

object Versions {
    const val kcqrsCoreVersion = "0.0.1"
    const val kcqrsTestVersion = "0.0.1"
    const val eventStoreDBVersion = "1.0.0"
    const val slf4jVersion = "1.7.25"
    const val kotlinVersion = "1.6.0"
    const val kotlinCoroutineVersion = "1.6.0"
    const val jacksonModuleKotlinVersion = "2.13.0"
    const val testContainerVersion = "1.16.2"
    const val junitJupiterVersion = "5.7.0"
    const val jacocoToolVersion = "0.8.7"
    const val jvmTarget = "11"
}

plugins {
    kotlin("jvm") version "1.6.0"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    jacoco
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("kcqrs-eventStoreDB") {
            from(components["kotlin"])
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    //KCQRS Modules
    implementation("io.github.abaddon.kcqrs:kcqrs-core:${Versions.kcqrsCoreVersion}")
    implementation("io.github.abaddon.kcqrs:kcqrs-test:${Versions.kcqrsTestVersion}")

    implementation("org.slf4j:slf4j-api:${Versions.slf4jVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlinVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutineVersion}")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${Versions.jacksonModuleKotlinVersion}")

    //EventStoreDB
    implementation("com.eventstore:db-client-java:${Versions.eventStoreDBVersion}")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:${Versions.junitJupiterVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinCoroutineVersion}")
    testImplementation("org.testcontainers:testcontainers:${Versions.testContainerVersion}")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.testContainerVersion}")
}

jacoco {
    toolVersion = Versions.jacocoToolVersion
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = Versions.jvmTarget
}

java {
    withSourcesJar()
    withJavadocJar()
}

signing {
    val signingKey = providers
        .environmentVariable("GPG_SIGNING_KEY")
        .forUseAtConfigurationTime()
    val signingPassphrase = providers
        .environmentVariable("GPG_SIGNING_PASSPHRASE")
        .forUseAtConfigurationTime()
    if (signingKey.isPresent && signingPassphrase.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
        val extension = extensions
            .getByName("publishing") as PublishingExtension
        sign(extension.publications)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set(project.name)
                description.set(Meta.desc)
                url.set("https://github.com/${Meta.githubRepo}")
                licenses {
                    license {
                        name.set(Meta.license)
                        url.set("https://opensource.org/licenses/Apache-2.0")
                    }
                }
                developers {
                    developer {
                        name.set("${Meta.developerName}")
                        organization.set("${Meta.developerOrganization}")
                        organizationUrl.set("${Meta.organizationUrl}")
                    }
                }
                scm {
                    url.set(
                        "https://github.com/${Meta.githubRepo}.git"
                    )
                    connection.set(
                        "scm:git:git://github.com/${Meta.githubRepo}.git"
                    )
                    developerConnection.set(
                        "scm:git:git://github.com/${Meta.githubRepo}.git"
                    )
                }
                issueManagement {
                    url.set("https://github.com/${Meta.githubRepo}/issues")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri(Meta.release))
            snapshotRepositoryUrl.set(uri(Meta.snapshot))
            val ossrhUsername = providers
                .environmentVariable("OSSRH_USERNAME")
                .forUseAtConfigurationTime()
            val ossrhPassword = providers
                .environmentVariable("OSSRH_PASSWORD")
                .forUseAtConfigurationTime()
            if (ossrhUsername.isPresent && ossrhPassword.isPresent) {
                username.set(ossrhUsername.get())
                password.set(ossrhPassword.get())
            }
        }
    }
}
