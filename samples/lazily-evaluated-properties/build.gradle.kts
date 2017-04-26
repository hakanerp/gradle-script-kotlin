import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

apply {
    plugin(GreetingPlugin::class.java)
}

configure<GreetingPluginExtension> {
    message = "Hi from Gradle"
    outputFiles = files("$buildDir/a.txt", "$buildDir/b.txt")
}

open class GreetingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Add the 'greeting' extension object
        val greeting: GreetingPluginExtension = project.extensions.create(
            "greeting",
            GreetingPluginExtension::class.java,
            project)
        // Add a task that uses the configuration
        project.tasks {
            "hello"(Greeting::class) {
                provideMessage(greeting.messageProvider)
                outputFiles = greeting.outputFiles
            }
        }
    }
}

open class GreetingPluginExtension(val project: Project) {
    private
    val messageState = project.property(String::class.java)

    private
    val outputFileCollection = project.files()

    var message
        get() = messageState.get()
        set(value) = messageState.set(value)

    val messageProvider
        get() = messageState

    var outputFiles
        get() = outputFileCollection
        set(value) = outputFileCollection.setFrom(value)
}

open class Greeting : DefaultTask() {
    private
    val messageState = project.property(String::class.java)

    private
    val outputFileCollection = project.files()

    @get:Input
    var message
        get() = messageState.get()
        set(value) = messageState.set(value)

    @get:OutputFiles
    var outputFiles
        get() = outputFileCollection
        set(value) = outputFileCollection.setFrom(value)

    fun provideMessage(message: Provider<String>) = messageState.set(message)

    @TaskAction
    fun printMessage() {
        logger.info("Writing message '$message' to files ${outputFiles.files}")
        outputFiles.forEach { it.writeText(message) }
    }
}

// Test task used for integration testing
tasks {
    "testSample" {
        dependsOn("hello")
        doLast {
            listOf(file("build/a.txt"), file("build/b.txt")).forEach {
                require(it.readText() == "Hi from Gradle", { "Message was not written into $it" })
            }
        }
    }
}
