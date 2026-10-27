package br.com.joaoduartemariucio.kmais_app_android.data.mappers

import br.com.joaoduartemariucio.kmais_app_android.data.models.FuelEntryModel
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry

object FuelEntryMapper {
    fun entity(model: FuelEntryModel): FuelEntry =
        FuelEntry(
            id = model.id,
            vehicleId = model.vehicleId,
            date = model.date,
            odometer = model.odometer,
            liters = model.liters,
            pricePerLiter = model.pricePerLiter,
            isFullTank = model.isFullTank,
        )

    fun model(entity: FuelEntry): FuelEntryModel =
        FuelEntryModel(
            id = entity.id,
            vehicleId = entity.vehicleId,
            date = entity.date,
            odometer = entity.odometer,
            liters = entity.liters,
            pricePerLiter = entity.pricePerLiter,
            isFullTank = entity.isFullTank,
        )
}
