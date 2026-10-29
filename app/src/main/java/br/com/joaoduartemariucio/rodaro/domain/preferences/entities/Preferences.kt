package br.com.joaoduartemariucio.rodaro.domain.preferences.entities

enum class UnitSystem {
    METRIC,
    IMPERIAL,
}

enum class CurrencyOption(
    val code: String,
) {
    BRL("BRL"),
    USD("USD"),
    EUR("EUR"),
    GBP("GBP"),
    ARS("ARS"),
    CLP("CLP"),
    PYG("PYG"),
    UYU("UYU"),
    MXN("MXN"),
    CAD("CAD"),
}

enum class LanguageOption(
    val tag: String,
) {
    SYSTEM(""),
    PORTUGUESE("pt-BR"),
    ENGLISH("en"),
}

data class Preferences(
    val pushEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val currency: CurrencyOption? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val language: LanguageOption = LanguageOption.SYSTEM,
)
