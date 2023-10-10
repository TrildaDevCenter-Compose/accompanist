/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    `java-library`
    id("kotlin")
    id(libs.plugins.jetbrains.dokka.get().pluginId)
    id(libs.plugins.android.lint.get().pluginId)
}

kotlin {
    explicitApi()
}

lint {
    htmlReport = true
    htmlOutput = file("lint-report.html")
    textReport = true
    absolutePaths = false
    ignoreTestSources = true
}

affectedTestConfiguration {
    jvmTestTask = "test"
}

/**
 * Creates a configuration for users to use that will be used bundle these dependency
 * jars inside of this lint check's jar. This is required because lintPublish does
 * not currently support dependencies, so instead we need to bundle any dependencies with the
 * lint jar manually. (b/182319899)
 */
val bundleInside: Configuration = configurations.create("bundleInside")
// bundleInside dependencies should be included as compileOnly and testImplementation as well
configurations.getByName("compileOnly").setExtendsFrom(setOf(bundleInside))
configurations.getByName("testImplementation").setExtendsFrom(setOf(bundleInside))

tasks.getByName<Jar>("jar") {
    this.dependsOn(bundleInside)
    this.from({
       bundleInside
               // The stdlib is already bundled with lint, so no need to include it manually
               // in the lint.jar if any dependencies here depend on it
               .filter { !it.name.contains("kotlin-stdlib") }
               .map { file ->
                   if (file.isDirectory) {
                       file
                   } else {
                       zipTree(file)
                   }
               }
   })
}

dependencies {
    // Bundle metadataJvm inside the Jar
    bundleInside(libs.kotlin.metadataJvm)

    compileOnly(libs.android.tools.lint.api)
    compileOnly(libs.kotlin.reflect)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.stdlibJdk8) // Override version from transitive dependencies

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.stdlibJdk8) // Override version from transitive dependencies
    testImplementation(libs.android.tools.lint.lint)
    testImplementation(libs.android.tools.lint.tests)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}