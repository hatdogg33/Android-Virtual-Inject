import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * SDK Auto-Install Script
 * Automatically detects and installs required Android SDK, NDK, and CMake components
 */

// Configuration - versions to install
val requiredCompileSdk: Int by rootProject.ext
val requiredNdkVersion: String by rootProject.ext
val requiredCmakeVersion: String by rootProject.ext

// Android SDK path
val androidSdkRoot: String = System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: "${System.getProperty("user.home")}/Android/Sdk"

val sdkManagerPath: String
    get() {
        val cmdlineTools = File(androidSdkRoot, "cmdline-tools/latest/bin")
        return when {
            File(cmdlineTools, "sdkmanager").exists() -> File(cmdlineTools, "sdkmanager").absolutePath
            File(cmdlineTools, "sdkmanager.bat").exists() -> File(cmdlineTools, "sdkmanager.bat").absolutePath
            else -> ""
        }
    }

val platformToolsPath: String
    get() = File(androidSdkRoot, "platform-tools").absolutePath

/**
 * Check if a command is available in the system
 */
fun isCommandAvailable(command: String): Boolean {
    return try {
        val process = if (System.getProperty("os.name").lowercase().contains("win")) {
            ProcessBuilder("cmd", "/c", "where", command)
        } else {
            ProcessBuilder("which", command)
        }
        process.redirectErrorStream(true)
        val result = process.start().waitFor(5, TimeUnit.SECONDS)
        result == 0
    } catch (e: Exception) {
        false
    }
}

/**
 * Run a shell command and return the output
 */
fun runCommand(command: List<String>, workingDir: File? = null): Pair<Int, String> {
    return try {
        val processBuilder = ProcessBuilder(command)
        workingDir?.let { processBuilder.directory(it) }
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor(5, TimeUnit.MINUTES)
        Pair(exitCode, output)
    } catch (e: Exception) {
        Pair(-1, e.message ?: "Unknown error")
    }
}

/**
 * Check if Android SDK is installed
 */
fun isAndroidSdkInstalled(): Boolean {
    val sdkFile = File(androidSdkRoot)
    return sdkFile.exists() && sdkFile.isDirectory &&
            File(sdkFile, "platforms").exists() &&
            File(sdkFile, "build-tools").exists()
}

/**
 * Check if a specific platform is installed
 */
fun isPlatformInstalled(platform: String): Boolean {
    val platformsDir = File(androidSdkRoot, "platforms")
    return platformsDir.exists() && File(platformsDir, platform).exists()
}

/**
 * Check if a specific NDK version is installed
 */
fun isNdkInstalled(ndkVersion: String): Boolean {
    val ndkDir = File(androidSdkRoot, "ndk/$ndkVersion")
    return ndkDir.exists() && ndkDir.isDirectory
}

/**
 * Check if a specific CMake version is installed
 */
fun isCmakeInstalled(cmakeVersion: String): Boolean {
    val cmakeDir = File(androidSdkRoot, "cmake/$cmakeVersion")
    return cmakeDir.exists() && cmakeDir.isDirectory
}

/**
 * Install Android SDK platform
 */
fun installPlatform(platform: String) {
    println("Installing $platform...")
    if (sdkManagerPath.isNotEmpty()) {
        val command = listOf(sdkManagerPath, "--install", platform)
        val (exitCode, output) = runCommand(command)
        if (exitCode == 0) {
            println("Successfully installed $platform")
        } else {
            println("Failed to install $platform: $output")
            throw GradleException("Failed to install $platform")
        }
    } else {
        throw GradleException("SDK Manager not found. Please install Android SDK command-line tools.")
    }
}

/**
 * Install NDK
 */
fun installNdk(ndkVersion: String) {
    println("Installing NDK $ndkVersion...")
    if (sdkManagerPath.isNotEmpty()) {
        val command = listOf(sdkManagerPath, "--install", "ndk;$ndkVersion")
        val (exitCode, output) = runCommand(command)
        if (exitCode == 0) {
            println("Successfully installed NDK $ndkVersion")
        } else {
            println("Failed to install NDK $ndkVersion: $output")
            throw GradleException("Failed to install NDK $ndkVersion")
        }
    } else {
        throw GradleException("SDK Manager not found. Please install Android SDK command-line tools.")
    }
}

/**
 * Install CMake
 */
fun installCmake(cmakeVersion: String) {
    println("Installing CMake $cmakeVersion...")
    if (sdkManagerPath.isNotEmpty()) {
        val command = listOf(sdkManagerPath, "--install", "cmake;$cmakeVersion")
        val (exitCode, output) = runCommand(command)
        if (exitCode == 0) {
            println("Successfully installed CMake $cmakeVersion")
        } else {
            println("Failed to install CMake $cmakeVersion: $output")
            throw GradleException("Failed to install CMake $cmakeVersion")
        }
    } else {
        throw GradleException("SDK Manager not found. Please install Android SDK command-line tools.")
    }
}

/**
 * Install platform-tools (adb, etc.)
 */
fun installPlatformTools() {
    println("Installing platform-tools...")
    if (sdkManagerPath.isNotEmpty()) {
        val command = listOf(sdkManagerPath, "--install", "platform-tools")
        val (exitCode, output) = runCommand(command)
        if (exitCode == 0) {
            println("Successfully installed platform-tools")
        } else {
            println("Failed to install platform-tools: $output")
        }
    }
}

/**
 * Check JDK version
 */
fun checkJdkVersion(): String? {
    return try {
        val process = ProcessBuilder("java", "-version")
        process.redirectErrorStream(true)
        val process = process.start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(5, TimeUnit.SECONDS)
        
        // Extract version from output
        val versionRegex = Regex("version \"([^\"]+)\"")
        val match = versionRegex.find(output)
        match?.groupValues?.get(1)
    } catch (e: Exception) {
        null
    }
}

/**
 * Verify Java 17 is available
 */
fun verifyJava17() {
    val javaVersion = checkJdkVersion()
    if (javaVersion != null) {
        val majorVersion = if (javaVersion.startsWith("1.")) {
            javaVersion.substringAfter("1.").substringBefore(".")
        } else {
            javaVersion.substringBefore(".")
        }
        
        if (majorVersion.toIntOrNull() != null && majorVersion.toInt() >= 17) {
            println("Java $javaVersion detected (OK)")
            return
        }
    }
    
    println("Warning: Java 17+ is recommended for Android development")
    println("Detected Java version: $javaVersion")
    println("Please install JDK 17 or later from https://adoptium.net/")
}

// Tasks

tasks.register("checkSdk") {
    group = "SDK Auto-Install"
    description = "Check if required SDK components are installed"
    
    doLast {
        println("=== SDK Status Check ===")
        println("SDK Root: $androidSdkRoot")
        println("SDK Installed: ${isAndroidSdkInstalled()}")
        println("Platform android-$requiredCompileSdk: ${isPlatformInstalled("android-$requiredCompileSdk")}")
        println("NDK $requiredNdkVersion: ${isNdkInstalled(requiredNdkVersion)}")
        println("CMake $requiredCmakeVersion: ${isCmakeInstalled(requiredCmakeVersion)}")
        println("Java Version: ${checkJdkVersion() ?: "Not found"}")
        println("========================")
    }
}

tasks.register("installSdk") {
    group = "SDK Auto-Install"
    description = "Install required Android SDK platform"
    
    doLast {
        val platform = "android-$requiredCompileSdk"
        if (!isPlatformInstalled(platform)) {
            installPlatform(platform)
        } else {
            println("$platform is already installed")
        }
    }
}

tasks.register("installNdk") {
    group = "SDK Auto-Install"
    description = "Install required Android NDK"
    
    doLast {
        if (!isNdkInstalled(requiredNdkVersion)) {
            installNdk(requiredNdkVersion)
        } else {
            println("NDK $requiredNdkVersion is already installed")
        }
    }
}

tasks.register("installCmake") {
    group = "SDK Auto-Install"
    description = "Install required CMake"
    
    doLast {
        if (!isCmakeInstalled(requiredCmakeVersion)) {
            installCmake(requiredCmakeVersion)
        } else {
            println("CMake $requiredCmakeVersion is already installed")
        }
    }
}

tasks.register("installPlatformTools") {
    group = "SDK Auto-Install"
    description = "Install Android platform-tools (adb)"
    
    doLast {
        installPlatformTools()
    }
}

tasks.register("installAllSdk") {
    group = "SDK Auto-Install"
    description = "Install all required SDK components"
    dependsOn("installSdk", "installNdk", "installCmake", "installPlatformTools")
    
    doLast {
        println("=== All SDK components installed ===")
    }
}

tasks.register("verifyJava") {
    group = "SDK Auto-Install"
    description = "Verify Java 17 installation"
    
    doLast {
        verifyJava17()
    }
}

// Auto-check on project configuration
gradle.taskGraph.whenReady {
    val allTasks = taskGraph.allTasks.map { it.name }
    if (allTasks.any { it.contains("assemble") || it.contains("build") }) {
        println("=== Auto-checking SDK components ===")
        
        val platform = "android-$requiredCompileSdk"
        val missing = mutableListOf<String>()
        
        if (!isPlatformInstalled(platform)) missing.add("Platform $platform")
        if (!isNdkInstalled(requiredNdkVersion)) missing.add("NDK $requiredNdkVersion")
        if (!isCmakeInstalled(requiredCmakeVersion)) missing.add("CMake $requiredCmakeVersion")
        
        if (missing.isNotEmpty()) {
            println("Missing components: ${missing.joinToString(", ")}")
            println("Run './gradlew installAllSdk' to install missing components")
        } else {
            println("All required SDK components are installed")
        }
        println("===================================")
    }
}
