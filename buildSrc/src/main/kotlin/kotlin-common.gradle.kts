/*
 *  Copyright 2023 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(plugin = "java-common")
apply<KotlinPlatformJvmPlugin>()
dependencies {
    val implementation: Configuration by configurations
    implementation(Deps.Kotlin.stdlib)
    implementation(Deps.Kotlin.reflect)
}
tasks {
    withType<KotlinCompile>() {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}