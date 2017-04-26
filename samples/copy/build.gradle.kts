import org.gradle.api.tasks.*
import org.apache.tools.ant.filters.*

//for including in the copy task
val dataContent = copySpec {
    from("src/data")
    include("*.data")
}

task<Copy>("initConfig") {

    from("src/main/config") {
        include("**/*.properties")
        include("**/*.xml")
        filter<ReplaceTokens>(
            "tokens" to mapOf("version" to "2.3.1"))
    }

    from("src/main/languages") {
        rename("EN_US_(.*)", "$1")
    }

    into("build/target/config")
    exclude("**/*.bak")
    includeEmptyDirs = false
    with(dataContent)
}

task<Delete>("clean") {
    delete(buildDir)
}

// Test task used for integration testing
tasks {
    "testSample" {
        dependsOn("initConfig")
        doLast {
            val target = "build/target/config"
            listOf(file("$target/copy.data"), file("$target/copy.xml")).forEach {
                require(it.exists(), { "File was not copied $it" })
            }
            listOf(file("$target/copy.bak"), file("$target/copy.txt")).forEach {
                require(!it.exists(), { "File was copied but should not have $it" })
            }
        }
    }
}
