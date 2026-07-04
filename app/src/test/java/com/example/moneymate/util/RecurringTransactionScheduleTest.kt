package com.example.moneymate.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class RecurringTransactionScheduleTest {

    @Test
    fun `daily repeat moves to next day`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Hang ngay"
        )

        assertDate(nextRun, 2026, 6, 16)
    }

    @Test
    fun `weekly repeat moves to next week`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Hang tuan"
        )

        assertDate(nextRun, 2026, 6, 22)
    }

    @Test
    fun `two week repeat moves by fourteen days`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Moi 2 tuan"
        )

        assertDate(nextRun, 2026, 6, 29)
    }

    @Test
    fun `monthly repeat moves to next month`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Hang thang"
        )

        assertDate(nextRun, 2026, 7, 15)
    }

    @Test
    fun `once value falls back to monthly for recurring transactions`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Mot lan"
        )

        assertDate(nextRun, 2026, 7, 15)
    }

    @Test
    fun `yearly repeat moves to next year`() {
        val nextRun = RecurringTransactionSchedule.nextRunAfter(
            currentRunAt = millisOf(2026, 6, 15),
            storedRepeat = "Moi nam"
        )

        assertDate(nextRun, 2027, 6, 15)
    }

    @Test
    fun `due run calculation generates current due run and advances next run`() {
        val firstRun = millisOf(2026, 6, 15)
        val now = millisOf(2026, 6, 15)

        val calculation = RecurringTransactionSchedule.calculateDueRuns(
            nextRunAt = firstRun,
            lastGeneratedAt = null,
            now = now,
            storedRepeat = "Hang ngay",
            maxCatchUpRuns = 24
        )

        assertEquals(listOf(firstRun), calculation.generatedRunTimes)
        assertDate(calculation.nextRunAt, 2026, 6, 16)
        assertEquals(firstRun, calculation.lastGeneratedAt)
    }

    @Test
    fun `due run calculation does not generate duplicate when run was already generated`() {
        val firstRun = millisOf(2026, 6, 15)
        val now = millisOf(2026, 6, 15)

        val calculation = RecurringTransactionSchedule.calculateDueRuns(
            nextRunAt = firstRun,
            lastGeneratedAt = firstRun,
            now = now,
            storedRepeat = "Hang ngay",
            maxCatchUpRuns = 24
        )

        assertEquals(emptyList<Long>(), calculation.generatedRunTimes)
        assertDate(calculation.nextRunAt, 2026, 6, 16)
        assertEquals(firstRun, calculation.lastGeneratedAt)
    }

    @Test
    fun `due run calculation catches up missed daily runs`() {
        val firstRun = millisOf(2026, 6, 15)
        val secondRun = millisOf(2026, 6, 16)
        val thirdRun = millisOf(2026, 6, 17)
        val now = millisOf(2026, 6, 17)

        val calculation = RecurringTransactionSchedule.calculateDueRuns(
            nextRunAt = firstRun,
            lastGeneratedAt = null,
            now = now,
            storedRepeat = "Hang ngay",
            maxCatchUpRuns = 24
        )

        assertEquals(listOf(firstRun, secondRun, thirdRun), calculation.generatedRunTimes)
        assertDate(calculation.nextRunAt, 2026, 6, 18)
        assertEquals(thirdRun, calculation.lastGeneratedAt)
    }

    @Test
    fun `due run calculation respects catch up limit`() {
        val firstRun = millisOf(2026, 6, 15)
        val now = millisOf(2026, 6, 30)

        val calculation = RecurringTransactionSchedule.calculateDueRuns(
            nextRunAt = firstRun,
            lastGeneratedAt = null,
            now = now,
            storedRepeat = "Hang ngay",
            maxCatchUpRuns = 3
        )

        assertEquals(3, calculation.generatedRunTimes.size)
        assertEquals(firstRun, calculation.generatedRunTimes[0])
        assertDate(calculation.nextRunAt, 2026, 6, 18)
        assertEquals(millisOf(2026, 6, 17), calculation.lastGeneratedAt)
    }

    private fun millisOf(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun assertDate(actualMillis: Long, year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = actualMillis
        }

        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month - 1, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }
}
