package com.actium.app

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

data class ChartPoint(val date: LocalDate, val value: Double)

fun dayOf(value: String, zone: ZoneId = ZoneId.systemDefault()): LocalDate? =
    runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate() }.getOrNull()

fun dailyMeans(points: List<ChartPoint>, from: LocalDate, through: LocalDate): List<ChartPoint> =
    points.filter { it.value.isFinite() && !it.date.isBefore(from) && !it.date.isAfter(through) }
        .groupBy { it.date }.map { (day, values) -> ChartPoint(day, values.map { it.value }.average()) }
        .sortedBy { it.date }

fun weeklyMean(points: List<ChartPoint>, through: LocalDate): Double? =
    dailyMeans(points, through.minusDays(6), through).takeIf { it.isNotEmpty() }?.map { it.value }?.average()

fun progressFraction(value: Double, target: Double): Float =
    if (target <= 0 || !value.isFinite() || !target.isFinite()) 0f else (value / target).coerceIn(0.0, 1.0).toFloat()

fun chartDomain(points: List<ChartPoint>): Pair<Double, Double> {
    val values = points.map { it.value }.filter { it.isFinite() }
    if (values.isEmpty()) return 0.0 to 1.0
    val min = values.min(); val max = values.max()
    val padding = maxOf((max - min) * .2, .5)
    return (min - padding) to (max + padding)
}
