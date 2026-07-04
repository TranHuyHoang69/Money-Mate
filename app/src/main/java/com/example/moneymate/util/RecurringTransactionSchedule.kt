package com.example.moneymate.util

import java.util.Calendar

data class DueRunCalculation(
    val generatedRunTimes: List<Long>,
    val nextRunAt: Long,
    val lastGeneratedAt: Long?
)

object RecurringTransactionSchedule {
    fun resolveRepeat(storedRepeat: String): ReminderRepeat {
        val parsedRepeat = ReminderRepeat.fromStored(storedRepeat)
        return if (parsedRepeat == ReminderRepeat.ONCE) {
            ReminderRepeat.MONTHLY
        } else {
            parsedRepeat
        }
    }

    fun nextRunAfter(currentRunAt: Long, storedRepeat: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentRunAt
        }
        resolveRepeat(storedRepeat).addTo(calendar)
        return calendar.timeInMillis
    }

    fun calculateDueRuns(
        nextRunAt: Long,
        lastGeneratedAt: Long?,
        now: Long,
        storedRepeat: String,
        maxCatchUpRuns: Int
    ): DueRunCalculation {
        val generatedRunTimes = mutableListOf<Long>()
        var candidateRunAt = nextRunAt
        var updatedLastGeneratedAt = lastGeneratedAt
        var iterations = 0

        while (candidateRunAt <= now && iterations < maxCatchUpRuns) {
            if (updatedLastGeneratedAt != candidateRunAt) {
                generatedRunTimes += candidateRunAt
                updatedLastGeneratedAt = candidateRunAt
            }

            candidateRunAt = nextRunAfter(
                currentRunAt = candidateRunAt,
                storedRepeat = storedRepeat
            )
            iterations++
        }

        return DueRunCalculation(
            generatedRunTimes = generatedRunTimes,
            nextRunAt = candidateRunAt,
            lastGeneratedAt = updatedLastGeneratedAt
        )
    }
}
