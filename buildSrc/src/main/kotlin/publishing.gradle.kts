import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    // Sign only when GPG credentials are present (CI / Maven Central release).
    // Without this guard, publishToMavenLocal fails locally because Gradle
    // cannot wire the signing tasks when no key is configured.
    val hasSigningKey = providers.environmentVariable("ORG_GRADLE_PROJECT_signingInMemoryKey").isPresent
        || project.hasProperty("signing.keyId")
        || project.hasProperty("signingInMemoryKey")
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates(
        groupId    = "dev.kmpai",
        artifactId = project.name,
        version    = project.version.toString(),
    )

    pom {
        name        = "KMP AI — ${project.name}"
        description = "Kotlin Multiplatform SDK — unified, type-safe API for multiple AI providers"
        url         = "https://github.com/ahmed-madhoun1/KMP-AI"
        inceptionYear = "2026"
        licenses {
            license {
                name         = "Apache License 2.0"
                url          = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id   = "ahmed-madhoun1"
                name = "Ahmed Madhoun"
                url  = "https://github.com/ahmed-madhoun1"
            }
        }
        scm {
            url                 = "https://github.com/ahmed-madhoun1/KMP-AI"
            connection          = "scm:git:git://github.com/ahmed-madhoun1/KMP-AI.git"
            developerConnection = "scm:git:ssh://git@github.com/ahmed-madhoun1/KMP-AI.git"
        }
    }
}
