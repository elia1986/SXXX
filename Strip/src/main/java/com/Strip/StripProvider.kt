1m 57s
Run chmod +x gradlew && ./gradlew make
Downloading https://services.gradle.org/distributions/gradle-8.12-bin.zip
..................................................................................................................................
Unzipping /home/runner/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12-bin.zip to /home/runner/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv
Set executable permissions for: /home/runner/.gradle/wrapper/dists/gradle-8.12-bin/cetblhg4pflnnks72fxwobvgv/gradle-8.12/bin/gradle

Welcome to Gradle 8.12!

Here are the highlights of this release:
 - Enhanced error and warning reporting with the Problems API
 - File-system watching support on Alpine Linux
 - Build and test Swift 6 libraries and apps

For more details see https://docs.gradle.org/8.12/release-notes.html

Starting a Gradle Daemon (subsequent builds will be faster)

> Configure project :Chaturbate
Fetching JAR

> Task :Chaturbate:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :Chaturbate:preBuild UP-TO-DATE
> Task :Chaturbate:preDebugBuild UP-TO-DATE
> Task :Chaturbate:generateDebugResValues
> Task :Chaturbate:generateDebugResources
> Task :Chaturbate:packageDebugResources
> Task :Strip:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :Strip:preBuild UP-TO-DATE
> Task :Strip:preDebugBuild UP-TO-DATE
> Task :Strip:generateDebugResValues
> Task :Strip:generateDebugResources
> Task :Strip:packageDebugResources
> Task :Chaturbate:parseDebugLocalResources
> Task :Strip:parseDebugLocalResources
> Task :Strip:generateDebugRFile
> Task :Chaturbate:generateDebugRFile
e: file:///home/runner/work/SXXX/SXXX/Strip/src/main/java/com/Strip/StripProvider.kt:154:9 Argument type mismatch: actual type is 'kotlin.String', but 'com.lagradost.cloudstream3.utils.ExtractorLinkType?' was expected.
e: file:///home/runner/work/SXXX/SXXX/Strip/src/main/java/com/Strip/StripProvider.kt:155:9 Argument type mismatch: actual type is 'kotlin.Int', but 'kotlin.coroutines.SuspendFunction1<com.lagradost.cloudstream3.utils.ExtractorLink, kotlin.Unit>' was expected.
e: file:///home/runner/work/SXXX/SXXX/Strip/src/main/java/com/Strip/StripProvider.kt:156:9 Too many arguments for 'suspend fun newExtractorLink(source: String, name: String, url: String, type: ExtractorLinkType? = ..., initializer: suspend ExtractorLink.() -> Unit = ...): ExtractorLink'.

> Task :Strip:compileDebugKotlin FAILED

> Task :Chaturbate:compileDebugKotlin

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':Strip:compileDebugKotlin'.
> A failure occurred while executing org.jetbrains.kotlin.compilerRunner.GradleCompilerRunnerWithWorkers$GradleKotlinCompilerWorkAction
   > Compilation error. See log for more details

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
12 actionable tasks: 12 executed
> Get more help at https://help.gradle.org.

BUILD FAILED in 1m 56s
Error: Process completed with exit code 1.
