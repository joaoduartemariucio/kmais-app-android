package br.com.joaoduartemariucio.rodaro.domain.common.calculations

import java.math.BigDecimal

fun Double.toDecimal(): BigDecimal = if (isFinite()) BigDecimal(toString()) else BigDecimal.ZERO

fun Iterable<BigDecimal>.sum(): BigDecimal = fold(BigDecimal.ZERO, BigDecimal::add)
