package com.actium.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ChartMathTest {
    private val today=LocalDate.of(2026,11,15)
    @Test fun localDayUsesTheUsersTimezone(){
        assertEquals(today,dayOf("2026-11-16T02:30:00Z",ZoneId.of("America/La_Paz")))
        assertEquals(today.plusDays(1),dayOf("2026-11-16T05:30:00Z",ZoneId.of("America/La_Paz")))
        assertNull(dayOf("invalid"))
    }
    @Test fun averageDoesNotWeightDaysWithMoreMeasurementsMoreHeavily(){
        val points=listOf(ChartPoint(today,72.0),ChartPoint(today,74.0),ChartPoint(today.minusDays(1),71.0),ChartPoint(today.minusDays(8),99.0),ChartPoint(today.plusDays(1),20.0))
        assertEquals(72.0,weeklyMean(points,today)!!,.0001)
        assertEquals(2,dailyMeans(points,today.minusDays(6),today).size)
        assertNull(weeklyMean(emptyList(),today))
    }
    @Test fun graphBoundsRemainFiniteForEqualWeights(){
        val (low,high)=chartDomain(listOf(ChartPoint(today,72.0),ChartPoint(today.minusDays(1),72.0)))
        assertTrue(high>low);assertTrue(low<72&&high>72)
        assertEquals(0.0 to 1.0,chartDomain(emptyList()))
    }
    @Test fun progressHandlesDisabledTargetsAndOverages(){
        assertEquals(0f,progressFraction(100.0,0.0))
        assertEquals(1f,progressFraction(3000.0,2300.0))
        assertEquals(.5f,progressFraction(1150.0,2300.0))
        assertEquals(0f,progressFraction(Double.NaN,2300.0))
    }
}
