plugins {
    kotlin("jvm")
    id("com.jfrog.artifactory")
    id("maven-publish")
}

val ktorVersion: String by project
val mockkVersion: String by project
val assertKVersion: String by project
val slf4jVersion: String by project
group = "codes.laurence.warden"

dependencies {
    implementation(project(":warden-core", "jvmRuntimeElements"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("org.slf4j", "slf4j-api", slf4jVersion)

    testImplementation("io.ktor", "ktor-websockets", ktorVersion)
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertKVersion")
    testImplementation(kotlin("test"))
}

val artifactName = "warden-ktor"
val artifactGroup = "codes.laurence.warden"

val pomUrl = "https://warden-kotlin.netlify.com/"
val pomScmUrl = "https://github.com/lgwillmore/warden"
val pomIssueUrl = "https://github.com/lgwillmore/warden/issues"
val pomDesc = "https://github.com/lgwillmore/warden"

val githubRepo = "lgwillmore/warden"
val githubReadme = "README.md"

val pomLicenseName = "MIT"
val pomLicenseUrl = "https://opensource.org/licenses/mit-license.php"
val pomLicenseDist = "repo"

val pomDeveloperId = "lgwillmore"
val pomDeveloperName = "Laurence Willmore"

publishing {
    publications {
        create<MavenPublication>("ktor") {
            groupId = artifactGroup
            artifactId = artifactName
            version = version
            artifact("$buildDir/libs/warden-ktor-${project.version}-sources.jar") {
                classifier = "sources"
            }

            artifact("$buildDir/libs/warden-ktor-${project.version}.jar")

            pom.withXml {
                asNode().apply {
                    appendNode("description", pomDesc)
                    appendNode("name", rootProject.name)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", pomDeveloperId)
                        appendNode("name", pomDeveloperName)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                    }
                }
            }
        }
    }
}

artifactory {
    setContextUrl("https://laurencecodes.jfrog.io/artifactory")
    publish(
        delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig> {
            repository(
                delegateClosureOf<org.jfrog.gradle.plugin.artifactory.dsl.DoubleDelegateWrapper> {
                    setProperty("repoKey", "codes.laurence.warden")
                    setProperty("username", System.getenv("JFROG_USER"))
                    setProperty("password", System.getenv("JFROG_PASSWORD"))
                    setProperty("maven", true)
                }
            )
            defaults(
                delegateClosureOf<org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask> {
                    publications("ktor")
                }
            )
        }
    )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {

    val build by existing {
        dependsOn("kotlinSourcesJar")
    }

    named("artifactoryPublish") {
        dependsOn("build")
    }
}
