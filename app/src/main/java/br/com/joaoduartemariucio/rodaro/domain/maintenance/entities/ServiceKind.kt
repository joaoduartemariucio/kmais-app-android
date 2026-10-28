package br.com.joaoduartemariucio.rodaro.domain.maintenance.entities

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
