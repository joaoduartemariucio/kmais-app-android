package br.com.joaoduartemariucio.kmais_app_android.data.mappers

import br.com.joaoduartemariucio.kmais_app_android.data.models.ServiceEntryModel
import br.com.joaoduartemariucio.kmais_app_android.data.models.ServiceRuleModel
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule

object ServiceMapper {
    fun entity(model: ServiceEntryModel): ServiceEntry =
        ServiceEntry(
            id = model.id,
            vehicleId = model.vehicleId,
            kind = model.kind,
            date = model.date,
            odometer = model.odometer,
            cost = model.cost,
            notes = model.notes,
        )

    fun model(entity: ServiceEntry): ServiceEntryModel =
        ServiceEntryModel(
            id = entity.id,
            vehicleId = entity.vehicleId,
            kind = entity.kind,
            date = entity.date,
            odometer = entity.odometer,
            cost = entity.cost,
            notes = entity.notes,
        )

    fun entity(model: ServiceRuleModel): ServiceRule =
        ServiceRule(
            id = model.id,
            vehicleId = model.vehicleId,
            kind = model.kind,
            kilometerInterval = model.kilometerInterval,
            monthInterval = model.monthInterval,
        )

    fun model(entity: ServiceRule): ServiceRuleModel =
        ServiceRuleModel(
            id = entity.id,
            vehicleId = entity.vehicleId,
            kind = entity.kind,
            kilometerInterval = entity.kilometerInterval,
            monthInterval = entity.monthInterval,
        )
}
