plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

val ktlintRules =
    mapOf(
        "ktlint_code_style" to "ktlint_official",
        "max_line_length" to "140",
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
        "ktlint_standard_package-name" to "disabled",
        "ktlint_standard_backing-property-naming" to "disabled",
        "ktlint_standard_property-naming" to "disabled",
    )

spotless {
    kotlin {
        target("app/src/main/**/*.kt", "app/src/debug/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
    format("kotlinTests", com.diffplug.gradle.spotless.KotlinExtension::class.java) {
        target("app/src/test/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            ktlintRules + mapOf("ktlint_standard_function-naming" to "disabled", "max_line_length" to "off"),
        )
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintRules)
    }
}
