package com.example.moneymate.util

import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

enum class ReminderRepeat {
    ONCE,
    DAILY,
    WEEKLY,
    EVERY_2_WEEKS,
    EVERY_4_WEEKS,
    MONTHLY,
    EVERY_2_MONTHS,
    QUARTERLY,
    EVERY_6_MONTHS,
    YEARLY;

    fun addTo(calendar: Calendar): Boolean {
        return when (this) {
            ONCE -> false
            DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                true
            }
            WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                true
            }
            EVERY_2_WEEKS -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 2)
                true
            }
            EVERY_4_WEEKS -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 4)
                true
            }
            MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                true
            }
            EVERY_2_MONTHS -> {
                calendar.add(Calendar.MONTH, 2)
                true
            }
            QUARTERLY -> {
                calendar.add(Calendar.MONTH, 3)
                true
            }
            EVERY_6_MONTHS -> {
                calendar.add(Calendar.MONTH, 6)
                true
            }
            YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
                true
            }
        }
    }

    companion object {
        fun fromStored(value: String?): ReminderRepeat {
            return when (normalize(value)) {
                "",
                "ONCE",
                "MOT LAN" -> ONCE

                "DAILY",
                "HANG NGAY" -> DAILY

                "WEEKLY",
                "HANG TUAN" -> WEEKLY

                "EVERY 2 WEEKS",
                "MOI 2 TUAN" -> EVERY_2_WEEKS

                "EVERY 4 WEEKS",
                "MOI 4 TUAN" -> EVERY_4_WEEKS

                "MONTHLY",
                "HANG THANG" -> MONTHLY

                "EVERY 2 MONTHS",
                "MOI 2 THANG" -> EVERY_2_MONTHS

                "QUARTERLY",
                "HANG QUY" -> QUARTERLY

                "EVERY 6 MONTHS",
                "MOI 6 THANG" -> EVERY_6_MONTHS

                "YEARLY",
                "EVERY YEAR",
                "MOI NAM" -> YEARLY

                else -> ONCE
            }
        }

        private fun normalize(value: String?): String {
            if (value.isNullOrBlank()) return ""
            return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .replace('_', ' ')
                .replace("\\s+".toRegex(), " ")
                .uppercase(Locale.ROOT)
                .trim()
        }
    }
}
