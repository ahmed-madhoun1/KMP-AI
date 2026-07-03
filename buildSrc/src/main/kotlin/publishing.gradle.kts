import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

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
