package com.example.moneymate.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReceiptTextParserTest {
    @Test
    fun `parses vietnamese receipt total amount with dot separator`() {
        val result = ReceiptTextParser.parse(
            """
            Highlands Coffee
            Ngay 15/06/2026
            Cafe sua da 39.000d
            Tong cong 39.000d
            """.trimIndent()
        )

        assertEquals(39_000.0, result.amount)
        assertEquals("Highlands Coffee", result.merchantName)
        assertEquals("Cafe", result.suggestedCategoryTitle)
        assertDate(result.dateMillis, 2026, 6, 15)
        assertTrue(result.confidence >= 0.9f)
    }

    @Test
    fun `parses total amount with comma separator`() {
        val result = ReceiptTextParser.parse(
            """
            Grab
            Date 16-06-2026
            Amount 45,000 VND
            """.trimIndent()
        )

        assertEquals(45_000.0, result.amount)
        assertEquals("Di chuyển", result.suggestedCategoryTitle)
        assertDate(result.dateMillis, 2026, 6, 16)
    }

    @Test
    fun `parses plain amount without separators`() {
        val result = ReceiptTextParser.parse(
            """
            Quan an ABC
            Thanh toan 39000
            """.trimIndent()
        )

        assertEquals(39_000.0, result.amount)
        assertEquals("Ăn uống", result.suggestedCategoryTitle)
    }

    @Test
    fun `prioritizes total and ignores customer cash and change`() {
        val result = ReceiptTextParser.parse(
            """
            WinMart
            Tam tinh 120.000d
            Tong cong 95.000d
            Tien khach dua 100.000d
            Tien thoi 5.000d
            """.trimIndent()
        )

        assertEquals(95_000.0, result.amount)
        assertEquals("Mua sắm", result.suggestedCategoryTitle)
    }

    @Test
    fun `returns null date when receipt has no date`() {
        val result = ReceiptTextParser.parse(
            """
            Phuc Long
            Total 68000
            """.trimIndent()
        )

        assertEquals(68_000.0, result.amount)
        assertEquals(null, result.dateMillis)
        assertEquals("Cafe", result.suggestedCategoryTitle)
    }

    @Test
    fun `parses amount with space thousand separator and day dot month date`() {
        val result = ReceiptTextParser.parse(
            """
            Co.opmart
            Ngay 17.06.2026
            Can thanh toan 128 000 VND
            Voucher 20 000
            """.trimIndent()
        )

        assertEquals(128_000.0, result.amount)
        assertEquals("Mua sắm", result.suggestedCategoryTitle)
        assertDate(result.dateMillis, 2026, 6, 17)
    }

    @Test
    fun `parses year first date and ignores cash received`() {
        val result = ReceiptTextParser.parse(
            """
            Taxi ABC
            2026-06-18 20:30
            Phai tra 72.000d
            Cash received 100.000d
            Change 28.000d
            """.trimIndent()
        )

        assertEquals(72_000.0, result.amount)
        assertEquals("Di chuyển", result.suggestedCategoryTitle)
        assertDate(result.dateMillis, 2026, 6, 18)
    }

    @Test
    fun `does not use large transaction id as amount on priority line`() {
        val result = ReceiptTextParser.parse(
            """
            Highlands Coffee
            Ma GD 20260617 Tong cong 39.000d
            """.trimIndent()
        )

        assertEquals(39_000.0, result.amount)
        assertEquals("Cafe", result.suggestedCategoryTitle)
    }

    @Test
    fun `ignores invoice id line when no total keyword exists`() {
        val result = ReceiptTextParser.parse(
            """
            Quan an ABC
            Ma hoa don 20260617
            Com suon 45.000d
            """.trimIndent()
        )

        assertEquals(45_000.0, result.amount)
        assertEquals("Ăn uống", result.suggestedCategoryTitle)
    }

    @Test
    fun `parses columnar receipt total and ignores hotline order codes`() {
        val result = ReceiptTextParser.parse(
            """
            NHA THUOC LONG CHAU
            Hotllne: 18006928
            NGAY: 17-06-2026 12:10:52
            DON HANG: 3001987
            VIEN NGAM STREPSILS MAXPRO 2X8
            22,000
            SIRO HO BO PHE BOI MAU FORTE
            55,000
            Tong tien:
            Tong tien giam:
            VC:
            Thanh tien
            Tien phai tra:
            Tien khach dua:
            Tien thua:
            22,000
            81838156981781673001987
            55,000
            77,000
            77,000
            """.trimIndent()
        )

        assertEquals(77_000.0, result.amount)
        assertEquals(
            listOf(77_000.0, 22_000.0, 55_000.0),
            result.amountCandidates.take(3)
        )
        assertEquals("NHA THUOC LONG CHAU", result.merchantName)
        assertDate(result.dateMillis, 2026, 6, 17)
    }

    @Test
    fun `parses vietnamese textual date and dong symbol amount`() {
        val result = ReceiptTextParser.parse(
            """
            GS25 Viet Nam
            Ngay 18 tháng 06 năm 2026
            Banh mi 29.000 đồng
            Tong thanh toan
            29.000 ₫
            """.trimIndent()
        )

        assertEquals(29_000.0, result.amount)
        assertEquals("GS25 Viet Nam", result.merchantName)
        assertDate(result.dateMillis, 2026, 6, 18)
    }

    @Test
    fun `does not treat textual date year as amount candidate`() {
        val result = ReceiptTextParser.parse(
            """
            Phuc Long
            Ngay 19 thg 06 2026
            Tra sua 35.000d
            """.trimIndent()
        )

        assertEquals(35_000.0, result.amount)
        assertEquals(listOf(35_000.0), result.amountCandidates)
        assertDate(result.dateMillis, 2026, 6, 19)
    }

    @Test
    fun `parses payable amount from long receipt with many line items`() {
        val rawText = buildString {
            appendLine("WinMart")
            appendLine("Ngay 20/06/2026")
            for (index in 1..80) {
                appendLine("San pham $index ${index + 10}.000d")
            }
            appendLine("Tam tinh 1.800.000d")
            appendLine("Giam gia 100.000d")
            appendLine("Tien phai tra:")
            appendLine("1.700.000d")
            appendLine("Tien khach dua:")
            appendLine("2.000.000d")
            appendLine("Tien thoi:")
            appendLine("300.000d")
        }

        val result = ReceiptTextParser.parse(rawText)

        assertEquals(1_700_000.0, result.amount)
        assertTrue(result.isLongReceipt)
        assertTrue(result.amountCandidateCount >= 5)
        assertEquals(
            "Hóa đơn có nhiều số tiền, vui lòng kiểm tra lại tổng tiền trước khi lưu.",
            result.amountWarning
        )
    }

    @Test
    fun `does not choose customer cash or change as payable amount`() {
        val result = ReceiptTextParser.parse(
            """
            Quan an ABC
            Ngay 20/06/2026
            Tien phai tra:
            120.000d
            Tien khach dua:
            500.000d
            Tien thoi:
            380.000d
            """.trimIndent()
        )

        assertEquals(120_000.0, result.amount)
    }

    @Test
    fun `parses total amount with ocr zero mistakes on same priority line`() {
        val result = ReceiptTextParser.parse(
            """
            Cua hang ABC
            Tong thanh toan: 123.OOO d
            """.trimIndent()
        )

        assertEquals(123_000.0, result.amount)
        assertTrue(result.amountCandidates.contains(123_000.0))
        assertEquals("same_line_priority", result.amountSourceReason)
    }

    @Test
    fun `parses uppercase vietnamese payable total with million separator`() {
        val result = ReceiptTextParser.parse(
            """
            Sieu thi ABC
            TỔNG PHẢI THANH TOÁN 1.234.567 VNĐ
            """.trimIndent()
        )

        assertEquals(1_234_567.0, result.amount)
    }

    @Test
    fun `parses payment amount with decimal suffix`() {
        val result = ReceiptTextParser.parse(
            """
            Cua hang ABC
            Số tiền thanh toán 123.000,00
            """.trimIndent()
        )

        assertEquals(123_000.0, result.amount)
    }

    @Test
    fun `parses amount on line below priority keyword`() {
        val result = ReceiptTextParser.parse(
            """
            Cua hang ABC
            Amount Due
            250,000 VND
            """.trimIndent()
        )

        assertEquals(250_000.0, result.amount)
        assertEquals("below_priority_keyword", result.amountSourceReason)
    }

    @Test
    fun `parses amount when first digit is read as letter`() {
        val result = ReceiptTextParser.parse(
            """
            Cua hang ABC
            Total payment = I23.000
            """.trimIndent()
        )

        assertEquals(123_000.0, result.amount)
    }

    @Test
    fun `parses lets go restaurant receipt from real sample image`() {
        val result = ReceiptTextParser.parse(
            """
            Nhà Hàng Let's Go
            Đ/c: 100/15A Đường Trần Phú - Phường Lộc Thọ - Tp. Nha Trang
            Tel: 02583 524495 - Hot: 01699999346
            PHIẾU TẠM TÍNH
            Số HĐ: HD.4008
            10:11 07/05/2018
            Khu vực: Tầng Lầu
            Bàn: B28
            Giờ vào: 19:04
            06/05/2018
            Cá chim Kg 2 300.000 600.000
            Sườn ram mặn Dĩa 2 300.000 600.000
            Mực cơm xào hành cần Dĩa 2 300.000 600.000
            Bò xào hành cần Dĩa 2 250.000 500.000
            Khoai tây chiên Dĩa 2 150.000 300.000
            Mì xào hải sản Dĩa 1 250.000 250.000
            Chả giò Dĩa 1 340.000 340.000
            Cơm chiên hải sản Dĩa 2 250.000 500.000
            Rau muống xào tỏi Dĩa 4 100.000 400.000
            Khăn Lạnh Cái 20 3.000 60.000
            Rượu Ngũ Lượng Tửu nhỏ Chai 1 200.000 200.000
            Nghêu Kg 3 150.000 450.000
            Tôm hùm trung Con 2 165.000 330.000
            Tôm sú Kg 3 250.000 750.000
            Ốc ngón tay Kg 3 250.000 750.000
            Ốc Hương Kg 4 300.000 1.200.000
            Hàu Kg 2,8 100.000 280.000
            Bia Sài gòn chai Chai 48 15.000 720.000
            Nước Sanna Chai 4 10.000 40.000
            Tiger Lon 7 20.000 140.000
            Tổng cộng 115,8 9.010.000
            Thành tiền 9.010.000
            """.trimIndent()
        )

        assertEquals(9_010_000.0, result.amount)
        assertEquals("Nhà Hàng Let's Go", result.merchantName)
        assertEquals("Ăn uống", result.suggestedCategoryTitle)
        assertTrue(result.amountCandidates.contains(9_010_000.0))
    }

    @Test
    fun `parses lets go raw ocr when total label is separated from amount`() {
        val result = ReceiptTextParser.parse(
            """
            Đ/e: 100/1SA Dubng Trần Pho -Phường Lốc Tho
            -Tp. Nha Trang
            Te 02583 s24495 Hot: 01699999346
            PHTEU TAM TÍNH
            58 HĐ HD4008
            Khu vực Tang Lau
            Go vao: 19:04
            Nhà Hàng Let's Go
            D5/05/2018
            Thu ngân: thunganlanh
            Món
            Ca chm
            5uờn ram mah Dia
            Mực cơm xaol DIa
            hanh cần
            Bò xào hành Dia
            can
            sản
            Khoai tây chiérCia2
            M o ha sản Da
            Ché gio
            Cam chên hai Dia
            xo tó
            Khan Lanh
            Rau mubng Dia
            Rugu Ngũ
            Lượng Tửu
            nho
            Nghêu
            Tôm hùm
            trung
            Tôm sủ
            ĐVT SL
            Ốc ngón tay
            Kg 2
            de Hudng
            Hau
            chai
            Nưdc Sanna
            Da
            Tiger
            Tổng cộng
            2
            Chai1
            Kg
            1
            Kg3
            Con2
            ChaiI
            Ba Sài gòn Chai 48
            Lon
            3
            Kg 2,8
            4
            7
            10:11 07/0S/2018
            Ban:828
            Đ.GI
            300.000
            Cái 20 3.000 60.000
            300,000 600.000
            115,8
            300.000 600.000
            150.000
            T.liền
            250.000 S00.000
            600,000
            250.000 250.000
            340.000 340.000
            250.000 500.000
            100.000 400.000
            150,000
            165.000
            200.000 200.000
            250,000
            450.000
            330.000
            750,000
            250.000 750.000
            10.000
            300.000 1200.000
            100.000 280.000
            15.000 720.000
            40.000
            20.000 140.000
            9.010.000
            """.trimIndent()
        )

        assertEquals(9_010_000.0, result.amount)
        assertTrue(result.amountCandidates.contains(9_010_000.0))
        assertEquals("columnar_priority", result.amountSourceReason)
    }

    private fun assertDate(actualMillis: Long?, year: Int, month: Int, day: Int) {
        assertNotNull(actualMillis)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = actualMillis ?: 0L
        }
        assertEquals(year, calendar.get(Calendar.YEAR))
        assertEquals(month - 1, calendar.get(Calendar.MONTH))
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH))
    }
}
