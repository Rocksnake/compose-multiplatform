import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
//    id("org.jetbrains.compose")
}


kotlin {
    //jvm()
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:0.0.1-SNAPSHOT")
//                implementation(compose.runtime)
            }
        }

        val jsMain by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
            }
            dependencies {
                implementation(project(":internal-web-core-runtime"))
            }
        }

        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi")
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":test-utils"))
                implementation(kotlin("test-js"))
            }
        }

//        val jvmMain by getting {
//            dependencies {
//                implementation(compose.desktop.currentOs)
//            }
//        }
    }
}

project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xklib-enable-signature-clash-checks=false",
        "-Xplugin=${project.properties["compose.plugin.path"]}"
    )
}
