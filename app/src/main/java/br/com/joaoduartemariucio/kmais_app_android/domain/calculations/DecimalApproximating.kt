package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import java.math.BigDecimal

fun Double.toDecimal(): BigDecimal = if (isFinite()) BigDecimal(toString()) else BigDecimal.ZERO

fun Iterable<BigDecimal>.sum(): BigDecimal = fold(BigDecimal.ZERO, BigDecimal::add)
