/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package integration

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import org.apache.tools.ant.util.TeeOutputStream

import java.io.File
import java.io.FileOutputStream

/**
 * Checks a single sample project.
 */
open class CheckSample : DefaultTask() {

    var sampleDir: File? = null

    @get:InputDirectory
    var installation: File? = null

    @get:Input
    var additionalGradleArguments = emptyList<String>()

    @get:Input
    var taskNames = listOf("tasks")

    @Suppress("unused")
    @get:InputFiles
    val inputFiles: FileCollection by lazy {
        project.fileTree(sampleDir!!).apply {
            exclude("**/build/**")
            include("**/*.gradle")
            include("**/*.gradle.kts")
        }
    }

    @get:OutputFile
    val outputFile: File by lazy {
        File(buildDir, "check-samples/${sampleDir!!.name}.txt")
    }

    @Suppress("unused")
    @TaskAction
    fun run() {
        withDaemonRegistry(customDaemonRegistry()) {
            outputFile.outputStream().use { stdout ->
                runBuild(sampleDir!!, stdout)
            }
        }
    }

    private
    fun customDaemonRegistry() =
        File(buildDir, "custom/daemon-registry")

    private
    val buildDir: File?
        get() = project.buildDir

    private
    fun runBuild(projectDir: File, stdout: FileOutputStream) {
        withConnectionFrom(connectorFor(projectDir).useInstallation(installation!!)) {
            newBuild()
                .withArguments(
                    listOf("--recompile-scripts", "--stacktrace") + additionalGradleArguments)
                .forTasks(*taskNames.toTypedArray())
                .setStandardOutput(TeeOutputStream(System.out, stdout))
                .setStandardError(TeeOutputStream(System.err, stdout))
                .run()
        }
    }
}
