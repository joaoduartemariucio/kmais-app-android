package br.com.joaoduartemariucio.kmais_app_android.data.mappers

import br.com.joaoduartemariucio.kmais_app_android.data.models.VehicleModel
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle

object VehicleMapper {
    fun entity(model: VehicleModel): Vehicle =
        Vehicle(
            id = model.id,
            name = model.name,
            kind = model.kind,
            plate = model.plate,
            initialOdometer = model.initialOdometer,
            tankCapacity = model.tankCapacity,
            createdAt = model.createdAt,
            isArchived = model.isArchived,
        )

    fun model(entity: Vehicle): VehicleModel =
        VehicleModel(
            id = entity.id,
            name = entity.name,
            kind = entity.kind,
            plate = entity.plate,
            initialOdometer = entity.initialOdometer,
            tankCapacity = entity.tankCapacity,
            createdAt = entity.createdAt,
            isArchived = entity.isArchived,
        )
}
