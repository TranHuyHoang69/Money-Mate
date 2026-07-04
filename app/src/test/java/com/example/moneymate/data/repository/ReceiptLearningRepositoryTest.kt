package com.example.moneymate.data.repository

import com.example.moneymate.data.local.ReceiptLearningPatternDao
import com.example.moneymate.data.local.ReceiptLearningPatternEntity
import com.example.moneymate.domain.model.ReceiptScanResult
import com.example.moneymate.util.ReceiptFingerprintUtil
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptLearningRepositoryTest {
    @Test
    fun `applies learned amount and category for matching receipt fingerprint`() = runBlocking {
        val learnedRawText = """
            NHA THUOC LONG CHAU
            NGAY: 17-06-2026 12:10:52
            DON HANG: 3001987
            VIEN NGAM STREPSILS MAXPRO 2X8
            22,000
            SIRO HO BO PHE BOI MAU FORTE
            55,000
            Tien phai tra:
            77,000
        """.trimIndent()
        val merchantName = "NHA THUOC LONG CHAU"
        val fingerprint = ReceiptFingerprintUtil.create(learnedRawText, merchantName)
        val dao = FakeReceiptLearningPatternDao(
            patterns = mutableListOf(
                ReceiptLearningPatternEntity(
                    id = 1L,
                    merchantName = merchantName,
                    textFingerprint = fingerprint,
                    selectedAmount = 77_000.0,
                    selectedCategoryTitle = "Suc khoe",
                    usageCount = 3,
                    updatedAt = 1_000L
                )
            )
        )
        val repository = ReceiptLearningRepository(dao)

        val parsedResult = ReceiptScanResult(
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
            """.trimIndent(),
            amount = 18_006_928.0,
            amountCandidates = listOf(85_000.0, 25_000.0, 60_000.0),
            merchantName = merchantName,
            suggestedCategoryTitle = "An uong",
            confidence = 0.55f
        )

        val learnedResult = repository.applyLearnedSelection(parsedResult)

        assertEquals(77_000.0, learnedResult.amount ?: 0.0, 0.0)
        assertEquals("Suc khoe", learnedResult.suggestedCategoryTitle)
        assertEquals(77_000.0, learnedResult.amountCandidates.first(), 0.0)
        assertTrue(learnedResult.confidence >= 0.95f)
        assertTrue(learnedResult.isLearningApplied)
    }

    @Test
    fun `keeps parsed result when no learned pattern matches`() = runBlocking {
        val repository = ReceiptLearningRepository(FakeReceiptLearningPatternDao())
        val parsedResult = ReceiptScanResult(
            rawText = "Quan com ABC\nTong cong 45000",
            amount = 45_000.0,
            merchantName = "Quan com ABC",
            suggestedCategoryTitle = "An uong",
            confidence = 0.8f
        )

        val learnedResult = repository.applyLearnedSelection(parsedResult)

        assertEquals(parsedResult, learnedResult)
    }

    @Test
    fun `forgets learned pattern for current receipt fingerprint`() = runBlocking {
        val rawText = """
            NHA THUOC LONG CHAU
            NGAY: 17-06-2026
            SIRO HO BO PHE BOI MAU FORTE
            Tien phai tra:
            77,000
        """.trimIndent()
        val merchantName = "NHA THUOC LONG CHAU"
        val fingerprint = ReceiptFingerprintUtil.create(rawText, merchantName)
        val dao = FakeReceiptLearningPatternDao(
            patterns = mutableListOf(
                ReceiptLearningPatternEntity(
                    id = 1L,
                    merchantName = merchantName,
                    textFingerprint = fingerprint,
                    selectedAmount = 77_000.0,
                    selectedCategoryTitle = "Suc khoe",
                    usageCount = 1,
                    updatedAt = 1_000L
                )
            )
        )
        val repository = ReceiptLearningRepository(dao)

        val deleted = repository.forgetUserSelection(
            ReceiptScanResult(
                rawText = rawText,
                merchantName = merchantName,
                amount = 77_000.0
            )
        )

        assertTrue(deleted)
        assertEquals(null, dao.findByPattern(merchantName, fingerprint))
    }
}

private class FakeReceiptLearningPatternDao(
    private val patterns: MutableList<ReceiptLearningPatternEntity> = mutableListOf()
) : ReceiptLearningPatternDao {
    override suspend fun findByPattern(
        merchantName: String,
        textFingerprint: String
    ): ReceiptLearningPatternEntity? {
        return patterns.firstOrNull {
            it.merchantName == merchantName && it.textFingerprint == textFingerprint
        }
    }

    override suspend fun findBestByFingerprint(
        textFingerprint: String
    ): ReceiptLearningPatternEntity? {
        return patterns
            .filter { it.textFingerprint == textFingerprint }
            .maxWithOrNull(compareBy<ReceiptLearningPatternEntity> { it.usageCount }.thenBy { it.updatedAt })
    }

    override suspend fun insert(pattern: ReceiptLearningPatternEntity): Long {
        val nextId = (patterns.maxOfOrNull { it.id } ?: 0L) + 1L
        patterns += pattern.copy(id = nextId)
        return nextId
    }

    override suspend fun update(pattern: ReceiptLearningPatternEntity) {
        val index = patterns.indexOfFirst { it.id == pattern.id }
        if (index >= 0) {
            patterns[index] = pattern
        }
    }

    override suspend fun deleteByPattern(
        merchantName: String,
        textFingerprint: String
    ): Int {
        val beforeSize = patterns.size
        patterns.removeAll {
            it.merchantName == merchantName && it.textFingerprint == textFingerprint
        }
        return beforeSize - patterns.size
    }

    override suspend fun deleteByFingerprint(textFingerprint: String): Int {
        val beforeSize = patterns.size
        patterns.removeAll { it.textFingerprint == textFingerprint }
        return beforeSize - patterns.size
    }

    override suspend fun clearAll() {
        patterns.clear()
    }
}
