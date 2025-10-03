
group = "io.github.abaddon.kcqrs"

object Meta {
    const val desc = "KCQRS EventStoreDB repository library"
    const val license = "Apache-2.0"
    const val githubRepo = "abaddon/kcqrs-EventStoreDB"
    const val developerName = "Stefano Longhi"
    const val developerOrganization = ""
    const val organizationUrl = "https://github.com/abaddon"
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.git.version)
    jacoco
    `maven-publish`
    signing
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()

val lastTag = details.lastTag.substring(1)
val snapshotTag = {
    val list = lastTag.split(".")
    val third = (list.last().toInt() + 1).toString()
    "${list[0]}.${list[1]}.$third-SNAPSHOT"
}
version = if(details.isCleanTag) lastTag else snapshotTag()

repositories {
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
    mavenLocal()
}

dependencies {

    compileOnly(libs.bundles.log)
    implementation(libs.bundles.ksqrs.eventstore)

    //Test
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.log.test)
    implementation(libs.bundles.ksqrs.eventstore.test)
}

jacoco {
    toolVersion = "0.8.11"
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

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Added to explicitly set Java toolchain
    }
    withSourcesJar()
    withJavadocJar()
}

signing {
    val signingKey = providers
        .environmentVariable("GPG_SIGNING_KEY")
    val signingPassphrase = providers
        .environmentVariable("GPG_SIGNING_PASSPHRASE")
    if (signingKey.isPresent && signingPassphrase.isPresent) {
        useInMemoryPgpKeys(signingKey.get(), signingPassphrase.get())
        val extension = extensions
            .getByName("publishing") as PublishingExtension
        sign(extension.publications)
    }
}

publishing {
    publications {
        create<MavenPublication>("kcqrs-eventStoreDB") {
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
                        name.set(Meta.developerName)
                        organization.set(Meta.developerOrganization)
                        organizationUrl.set(Meta.organizationUrl)
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
        // see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            username = providers.environmentVariable("SONATYPE_USERNAME")
            password = providers.environmentVariable("SONATYPE_TOKEN")
        }
    }
}