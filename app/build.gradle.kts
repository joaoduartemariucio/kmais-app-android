plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    jacoco
}

val orsApiKeyFromLocalProperties =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull
        ?.lineSequence()
        ?.firstOrNull { it.startsWith("ors.api.key=") }
        ?.substringAfter("=")
        ?.trim()
        .orEmpty()

val orsApiKeyFromEnvironment =
    providers
        .environmentVariable("OPEN_ROUTE_API_KEY")
        .orNull
        ?.trim()
        .orEmpty()

val orsApiKey = orsApiKeyFromLocalProperties.ifBlank { orsApiKeyFromEnvironment }

fun environment(name: String): String? =
    providers
        .environmentVariable(name)
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseKeystorePath = environment("KEYSTORE_PATH")
val versionProperties =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("version.properties"))
        .asText
        .get()
        .lineSequence()
        .map { it.trim() }
        .filter { "=" in it }
        .associate { it.substringBefore("=") to it.substringAfter("=") }

val appVersionCode = versionProperties.getValue("VERSION_CODE").toInt()
val appVersionName = versionProperties.getValue("VERSION_NAME")

tasks
    .matching { it.name in setOf("assembleRelease", "bundleRelease") }
    .configureEach {
        val missingKey = orsApiKey.isBlank()
        doFirst {
            if (missingKey) {
                throw GradleException(
                    "OpenRouteService key missing: add ors.api.key=<key> to local.properties " +
                        "or set the OPEN_ROUTE_API_KEY environment variable before building a release.",
                )
            }
        }
    }

android {
    namespace = "br.com.joaoduartemariucio.rodaro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.joaoduartemariucio.rodaro"
        minSdk = 28
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = environment("KEYSTORE_PASSWORD")
                keyAlias = environment("KEY_ALIAS")
                keyPassword = environment("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            buildConfigField("String", "ORS_API_KEY", "\"$orsApiKey\"")
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_unit_id", "ca-app-pub-3940256099942544/9214589741")
            resValue("string", "admob_interstitial_unit_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "admob_native_unit_id", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "admob_app_open_unit_id", "ca-app-pub-3940256099942544/9257395921")
        }
        release {
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
            buildConfigField("String", "ORS_API_KEY", "\"$orsApiKey\"")
            resValue("string", "admob_app_id", "ca-app-pub-1068825778444539~3367882368")
            resValue("string", "admob_banner_unit_id", "ca-app-pub-1068825778444539/5512371965")
            resValue("string", "admob_interstitial_unit_id", "ca-app-pub-1068825778444539/8484477679")
            resValue("string", "admob_native_unit_id", "ca-app-pub-1068825778444539/3466673450")
            resValue("string", "admob_app_open_unit_id", "ca-app-pub-1068825778444539/6672702556")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        resValues = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = false
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi", "LogNotTimber")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.config)
    implementation(libs.firebase.installations)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.maplibre.android)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}
