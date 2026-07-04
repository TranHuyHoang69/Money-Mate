package com.example.moneymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptFingerprintUtilTest {
    @Test
    fun `creates same fingerprint when receipt ids dates and amounts change`() {
        val first = ReceiptFingerprintUtil.create(
            rawText = """
                NHA THUOC LONG CHAU
                NGAY: 17-06-2026 12:10:52
                DON HANG: 3001987
                VIEN NGAM STREPSILS MAXPRO 2X8
                22,000
                SIRO HO BO PHE BOI MAU FORTE
                55,000
                Tien phai tra:
                77,000
                Hotline: 18006928
            """.trimIndent(),
            merchantName = "NHA THUOC LONG CHAU"
        )

        val second = ReceiptFingerprintUtil.create(
            rawText = """
                NHA THUOC LONG CHAU
                NGAY: 20-06-2026 15:45:11
                DON HANG: 9988776
                VIEN NGAM STREPSILS MAXPRO 2X8
                25,000
                SIRO HO BO PHE BOI MAU FORTE
                60,000
                Tien phai tra:
                85,000
                Hotline: 18006928
            """.trimIndent(),
            merchantName = "NHA THUOC LONG CHAU"
        )

        assertEquals(first, second)
        assertTrue(first.contains("long"))
        assertTrue(first.contains("chau"))
        assertTrue(first.contains("strepsils"))
        assertFalse(first.contains("3001987"))
        assertFalse(first.contains("77000"))
    }

    @Test
    fun `normalizes vietnamese accents in merchant and text`() {
        val fingerprint = ReceiptFingerprintUtil.create(
            rawText = """
                Nha thuoc Long Chau
                Thanh toan 39.000d
            """.trimIndent(),
            merchantName = "Nhà thuốc Long Châu"
        )

        assertTrue(fingerprint.contains("nha"))
        assertTrue(fingerprint.contains("thuoc"))
        assertTrue(fingerprint.contains("long"))
        assertTrue(fingerprint.contains("chau"))
    }

    @Test
    fun `creates same fingerprint when textual receipt date changes`() {
        val first = ReceiptFingerprintUtil.create(
            rawText = """
                GS25 Viet Nam
                Ngay 18 thang 06 nam 2026
                Tong thanh toan 29.000d
            """.trimIndent(),
            merchantName = "GS25 Viet Nam"
        )

        val second = ReceiptFingerprintUtil.create(
            rawText = """
                GS25 Viet Nam
                Ngay 19 thg 06 2026
                Tong thanh toan 31.000 ₫
            """.trimIndent(),
            merchantName = "GS25 Viet Nam"
        )

        assertEquals(first, second)
        assertFalse(first.contains("2026"))
        assertFalse(first.contains("29000"))
    }
}
