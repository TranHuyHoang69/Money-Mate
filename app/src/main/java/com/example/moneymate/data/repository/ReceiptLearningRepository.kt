package com.example.moneymate.data.repository

import com.example.moneymate.data.local.ReceiptLearningPatternDao
import com.example.moneymate.data.local.ReceiptLearningPatternEntity
import com.example.moneymate.domain.model.ReceiptScanResult
import com.example.moneymate.util.ReceiptFingerprintUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptLearningRepository @Inject constructor(
    private val receiptLearningPatternDao: ReceiptLearningPatternDao
) {
    suspend fun applyLearnedSelection(result: ReceiptScanResult): ReceiptScanResult {
        val learnedPattern = findMatchingPattern(result) ?: return result
        val learnedCandidates = (
            listOf(learnedPattern.selectedAmount) +
                result.amountCandidates +
                listOfNotNull(result.amount)
            )
            .distinct()
            .take(MAX_AMOUNT_CANDIDATES)

        return result.copy(
            amount = learnedPattern.selectedAmount,
            amountCandidates = learnedCandidates,
            suggestedCategoryTitle = learnedPattern.selectedCategoryTitle,
            confidence = maxOf(result.confidence, LEARNED_CONFIDENCE),
            isLearningApplied = true
        )
    }

    suspend fun saveUserSelection(result: ReceiptScanResult) {
        val amount = result.amount ?: return
        val merchantName = result.merchantName?.trim().orEmpty()
        if (merchantName.isBlank()) return

        val fingerprint = ReceiptFingerprintUtil.create(
            rawText = result.rawText,
            merchantName = merchantName
        )
        if (fingerprint.isBlank()) return

        val now = System.currentTimeMillis()
        val existing = receiptLearningPatternDao.findByPattern(
            merchantName = merchantName,
            textFingerprint = fingerprint
        )

        if (existing == null) {
            receiptLearningPatternDao.insert(
                ReceiptLearningPatternEntity(
                    merchantName = merchantName,
                    textFingerprint = fingerprint,
                    selectedAmount = amount,
                    selectedCategoryTitle = result.suggestedCategoryTitle,
                    usageCount = 1,
                    updatedAt = now
                )
            )
        } else {
            receiptLearningPatternDao.update(
                existing.copy(
                    selectedAmount = amount,
                    selectedCategoryTitle = result.suggestedCategoryTitle,
                    usageCount = existing.usageCount + 1,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun forgetUserSelection(result: ReceiptScanResult): Boolean {
        val merchantName = result.merchantName?.trim().orEmpty()
        if (merchantName.isBlank()) return false

        val fingerprint = ReceiptFingerprintUtil.create(
            rawText = result.rawText,
            merchantName = merchantName
        )
        if (fingerprint.isBlank()) return false

        val exactDeleted = receiptLearningPatternDao.deleteByPattern(
            merchantName = merchantName,
            textFingerprint = fingerprint
        )
        val fallbackDeleted = if (exactDeleted == 0) {
            receiptLearningPatternDao.deleteByFingerprint(fingerprint)
        } else {
            0
        }

        return exactDeleted + fallbackDeleted > 0
    }

    private suspend fun findMatchingPattern(
        result: ReceiptScanResult
    ): ReceiptLearningPatternEntity? {
        val merchantName = result.merchantName?.trim().orEmpty()
        if (merchantName.isBlank()) return null

        val fingerprint = ReceiptFingerprintUtil.create(
            rawText = result.rawText,
            merchantName = merchantName
        )
        if (fingerprint.isBlank()) return null

        return receiptLearningPatternDao.findByPattern(
            merchantName = merchantName,
            textFingerprint = fingerprint
        ) ?: receiptLearningPatternDao.findBestByFingerprint(fingerprint)
    }

    private companion object {
        const val LEARNED_CONFIDENCE = 0.95f
        const val MAX_AMOUNT_CANDIDATES = 8
    }
}
