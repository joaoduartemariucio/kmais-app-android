package br.com.joaoduartemariucio.rodaro.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import br.com.joaoduartemariucio.rodaro.R

@Composable
fun distanceUnit(): String = stringResource(if (UnitConversion.isImperial) R.string.unit_miles else R.string.unit_km)

@Composable
fun volumeUnit(): String = stringResource(if (UnitConversion.isImperial) R.string.unit_gallons else R.string.unit_liters)

@Composable
fun volumeTitle(): String = stringResource(if (UnitConversion.isImperial) R.string.gallons else R.string.liters)

@Composable
fun consumptionUnit(): String = stringResource(if (UnitConversion.isImperial) R.string.unit_mpg else R.string.unit_km_per_liter)

@Composable
fun volumePriceTitle(): String = stringResource(if (UnitConversion.isImperial) R.string.price_per_gallon else R.string.price_per_liter)

@Composable
fun perDistanceTitle(): String = stringResource(if (UnitConversion.isImperial) R.string.cost_per_mile else R.string.cost_per_km)
