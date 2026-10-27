package br.com.joaoduartemariucio.kmais_app_android.domain.entities

enum class ServiceKind(
    val rawValue: String,
) {
    OIL_CHANGE("oilChange"),
    OIL_FILTER("oilFilter"),
    AIR_FILTER("airFilter"),
    FUEL_FILTER("fuelFilter"),
    SPARK_PLUGS("sparkPlugs"),
    BRAKES("brakes"),
    TIRES("tires"),
    CHAIN("chain"),
    COOLANT("coolant"),
    INSPECTION("inspection"),
    OTHER("other"),
}
