import com.android.build.gradle.LibraryExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

val jitpackGroup = providers.environmentVariable("GROUP")
    .zip(providers.environmentVariable("ARTIFACT")) { group, artifact ->
        "$group.$artifact"
    }
    .orElse("com.github.lee-sq.AndroidPlatformWorkspace")

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")

        group = jitpackGroup.get()
        version = providers.environmentVariable("VERSION")
            .orElse(providers.gradleProperty("${project.name}.version"))
            .orElse("0.1.0-SNAPSHOT")
            .get()

        extensions.configure<LibraryExtension>("android") {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        afterEvaluate {
            extensions.configure<PublishingExtension>("publishing") {
                publications {
                    register<MavenPublication>("release") {
                        from(components["release"])

                        groupId = project.group.toString()
                        artifactId = project.name
                        version = project.version.toString()

                        pom {
                            name.set(project.name)
                            description.set("HolderZone Android library module: ${project.name}")
                            url.set("https://github.com/lee-sq/AndroidPlatformWorkspace")
                        }
                    }
                }
            }
        }
    }
}
