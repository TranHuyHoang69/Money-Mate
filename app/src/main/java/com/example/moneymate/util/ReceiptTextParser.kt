package com.example.moneymate.util

import com.example.moneymate.domain.model.ReceiptScanResult
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReceiptTextParser {
    private val priorityKeywords = listOf(
        "tong cong",
        "tong thanh toan",
        "tong tien thanh toan",
        "tong phai tra",
        "so tien thanh toan",
        "can thanh toan",
        "phai thanh toan",
        "tien phai tra",
        "thanh toan",
        "thanh tien",
        "tong tien",
        "phai tra",
        "grand total",
        "total due",
        "total payment",
        "total",
        "amount",
        "amount due",
        "payment amount",
        "payable amount",
        "payment"
    )

    private val payablePriorityKeywords = listOf(
        "tong cong",
        "tong thanh toan",
        "tong tien thanh toan",
        "tong phai tra",
        "so tien thanh toan",
        "can thanh toan",
        "phai thanh toan",
        "tien phai tra",
        "phai tra",
        "grand total",
        "total due",
        "total payment",
        "amount due",
        "payment amount",
        "payable amount"
    )

    private val ignoredAmountKeywords = listOf(
        "tien khach dua",
        "khach dua",
        "tien thoi",
        "tra lai",
        "tong tien giam",
        "tien giam",
        "giam gia",
        "khuyen mai",
        "voucher",
        "discount",
        "change",
        "cash",
        "received",
        "diem",
        "point"
    )

    private val merchantNoiseKeywords = listOf(
        "hoa don",
        "bien lai",
        "receipt",
        "invoice",
        "tax",
        "vat",
        "mst",
        "ma gd",
        "ma giao dich",
        "so hd",
        "hotline"
    )

    private val idLikeAmountKeywords = listOf(
        "ma hoa don",
        "ma hd",
        "so hoa don",
        "so hd",
        "ma gd",
        "ma giao dich",
        "order id",
        "don hang",
        "bill no",
        "receipt no",
        "invoice no",
        "serial",
        "mst",
        "tax code",
        "hotline",
        "hotllne",
        "dien thoai",
        "phone",
        "tel"
    )

    private val amountRegex = Regex(
        """(?<![A-Za-z0-9])([0-9OoIl]{1,3}(?:[.,\s][0-9OoIl]{3})+(?:[.,][0-9OoIl]{2})?|[0-9OoIl]{4,9})(?:\s*(?:₫|đ|d|vnd|vnđ|đồng|dong))?""",
        RegexOption.IGNORE_CASE
    )
    private val currencySuffixRegex = Regex("""(?:₫|đ|d|vnd|vnđ|đồng|dong)\s*$""", RegexOption.IGNORE_CASE)
    private val decimalSuffixRegex = Regex("""[.,]\d{2}\s*$""")
    private val dayFirstDateRegex = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")
    private val yearFirstDateRegex = Regex("""\b(\d{4})[./-](\d{1,2})[./-](\d{1,2})\b""")
    private val fuzzyDateRegex = Regex(
        """\b[0-9OoIlDS]{1,2}[./-][0-9OoIlDS]{1,2}[./-][0-9OoIlDS]{2,4}\b""",
        RegexOption.IGNORE_CASE
    )
    private val textualMonthDateRegex = Regex(
        """\b(\d{1,2})\s*(?:thang|thg|t)\.?\s*(\d{1,2})(?:\s*(?:nam)?\s*(\d{2,4}))?\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(
        rawText: String,
        availableCategories: List<String> = emptyList()
    ): ReceiptScanResult {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val ocrSummary = ReceiptOcrSummaryExtractor.extract(rawText)
        val amountSelection = parseAmount(lines)
        val amount = amountSelection?.value?.toDouble()
        val amountCandidateValues = parseAmountCandidateValues(lines)
        val amountCandidates = buildAmountCandidates(
            selectedAmount = amount,
            candidateValues = amountCandidateValues
        )
        val dateMillis = parseDateMillis(rawText)
        val merchantName = parseMerchantName(lines)
        val suggestedCategory = CategorySuggestionEngine.suggestCategory(
            text = listOfNotNull(merchantName, rawText).joinToString(" "),
            availableCategories = availableCategories
        )
        val confidence = calculateConfidence(amount, dateMillis, merchantName, rawText)
        val note = buildNote(merchantName, rawText)

        return ReceiptScanResult(
            rawText = rawText,
            amount = amount,
            amountCandidates = amountCandidates,
            dateMillis = dateMillis,
            merchantName = merchantName,
            suggestedCategoryTitle = suggestedCategory,
            note = note,
            confidence = confidence,
            isLongReceipt = ocrSummary.isLongReceipt,
            amountCandidateCount = amountCandidateValues.size,
            amountWarning = buildAmountWarning(
                isLongReceipt = ocrSummary.isLongReceipt,
                amountCandidateCount = amountCandidateValues.size
            ),
            amountSourceLine = amountSelection?.sourceLine,
            amountSourceReason = amountSelection?.reason
        )
    }

    private fun parseAmount(lines: List<String>): AmountSelection? {
        val indexedLines = lines.withIndex().toList()
        val candidateLines = indexedLines
            .filterNot { normalize(it.value).containsAny(ignoredAmountKeywords) }

        parsePriorityContextAmount(indexedLines, lines.size)?.let { return it }

        parseColumnarPriorityAmount(indexedLines, lines.size)?.let { return it }

        parseTailPriorityAmount(indexedLines, lines.size)?.let { return it }

        val fallbackAmounts = candidateLines
            .filterNot { line ->
                val normalized = normalize(line.value)
                containsDate(line.value) ||
                    normalized.containsAny(idLikeAmountKeywords)
            }
            .flatMap { extractAmountCandidates(it.value, it.index, lines.size) }

        return fallbackAmounts
            .preferTypicalAmounts()
            .maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenBy { it.value })
            ?.let {
                AmountSelection(
                    value = it.value,
                    sourceLine = indexedLines.getOrNull(it.lineIndex)?.value.orEmpty(),
                    reason = "fallback_candidate"
                )
            }
    }

    private fun parseTailPriorityAmount(
        lines: List<IndexedValue<String>>,
        lineCount: Int
    ): AmountSelection? {
        val hasPriorityLabel = lines.any { line ->
            isPayablePriorityLine(normalize(line.value))
        }
        if (!hasPriorityLabel) return null

        val tailStartIndex = (lines.size - TAIL_PRIORITY_SCAN_LINES).coerceAtLeast(0)
        val tailCandidates = lines
            .asSequence()
            .drop(tailStartIndex)
            .filterNot { line ->
                val normalized = normalize(line.value)
                normalized.containsAny(ignoredAmountKeywords) ||
                    normalized.containsAny(idLikeAmountKeywords) ||
                    containsDate(line.value)
            }
            .flatMap { line ->
                extractAmountCandidates(line.value, line.index, lineCount)
                    .map { candidate ->
                        ScoredAmountSelection(
                            selection = AmountSelection(
                                value = candidate.value,
                                sourceLine = line.value,
                                reason = "tail_priority_candidate"
                            ),
                            score = candidate.score + TAIL_PRIORITY_BONUS,
                            lineIndex = candidate.lineIndex
                        )
                    }
                    .asSequence()
            }
            .toList()

        return tailCandidates
            .filter { it.selection.value <= MAX_TYPICAL_RECEIPT_AMOUNT }
            .maxWithOrNull(
                compareBy<ScoredAmountSelection> { it.selection.value }
                    .thenBy { it.score }
                    .thenBy { it.lineIndex }
            )
            ?.selection
    }

    private fun parsePriorityContextAmount(
        lines: List<IndexedValue<String>>,
        lineCount: Int
    ): AmountSelection? {
        val selections = mutableListOf<ScoredAmountSelection>()

        lines.forEach { priorityLine ->
            val normalized = normalize(priorityLine.value)
            if (!isPayablePriorityLine(normalized)) return@forEach

            val isStrongPayableLabel = normalized.containsAny(payablePriorityKeywords)
            val labelScore = if (isStrongPayableLabel) {
                STRONG_PRIORITY_LABEL_SCORE
            } else {
                PRIORITY_LABEL_SCORE
            }

            extractAmountCandidates(priorityLine.value, priorityLine.index, lineCount)
                .preferTypicalAmounts()
                .forEach { candidate ->
                    selections += ScoredAmountSelection(
                        selection = AmountSelection(
                            value = candidate.value,
                            sourceLine = priorityLine.value,
                            reason = "same_line_priority"
                        ),
                        score = candidate.score + labelScore + SAME_LINE_PRIORITY_BONUS,
                        lineIndex = candidate.lineIndex
                    )
                }

            selections += findNearbyPriorityAmounts(
                lines = lines,
                priorityLine = priorityLine,
                lineCount = lineCount,
                labelScore = labelScore,
                searchBelow = true
            )

            if (isStrongPayableLabel) {
                selections += findNearbyPriorityAmounts(
                    lines = lines,
                    priorityLine = priorityLine,
                    lineCount = lineCount,
                    labelScore = labelScore,
                    searchBelow = false
                )
            }
        }

        return selections
            .maxWithOrNull(
                compareBy<ScoredAmountSelection> { it.score }
                    .thenBy { it.lineIndex }
                    .thenBy { it.selection.value }
            )
            ?.selection
    }

    private fun findNearbyPriorityAmounts(
        lines: List<IndexedValue<String>>,
        priorityLine: IndexedValue<String>,
        lineCount: Int,
        labelScore: Int,
        searchBelow: Boolean
    ): List<ScoredAmountSelection> {
        val maxOffset = if (searchBelow) PRIORITY_LOOKAHEAD_LINES else PRIORITY_LOOKBEHIND_LINES
        val results = mutableListOf<ScoredAmountSelection>()
        var sawIgnoredLabel = false

        for (offset in 1..maxOffset) {
            val targetIndex = if (searchBelow) {
                priorityLine.index + offset
            } else {
                priorityLine.index - offset
            }

            val line = lines.getOrNull(targetIndex) ?: continue
            val normalized = normalize(line.value)

            if (normalized.containsAny(idLikeAmountKeywords) || containsDate(line.value)) {
                continue
            }

            if (searchBelow && normalized.containsAny(ignoredAmountKeywords)) {
                sawIgnoredLabel = true
                continue
            }

            if (searchBelow && sawIgnoredLabel) continue

            val proximityBonus = if (searchBelow) {
                BELOW_PRIORITY_BONUS - (offset * PRIORITY_DISTANCE_PENALTY)
            } else {
                ABOVE_PRIORITY_BONUS - (offset * PRIORITY_DISTANCE_PENALTY)
            }

            extractAmountCandidates(line.value, line.index, lineCount)
                .preferTypicalAmounts()
                .forEach { candidate ->
                    results += ScoredAmountSelection(
                        selection = AmountSelection(
                            value = candidate.value,
                            sourceLine = line.value,
                            reason = if (searchBelow) {
                                "below_priority_keyword"
                            } else {
                                "above_priority_keyword"
                            }
                        ),
                        score = candidate.score + labelScore + proximityBonus,
                        lineIndex = candidate.lineIndex
                    )
                }
        }

        return results
    }

    private fun isPayablePriorityLine(normalizedLine: String): Boolean {
        return normalizedLine.containsAny(priorityKeywords) &&
            (!normalizedLine.containsAny(ignoredAmountKeywords) ||
                normalizedLine.containsAny(payablePriorityKeywords))
    }

    private fun parseAmountCandidateValues(lines: List<String>): List<Double> {
        return lines
            .withIndex()
            .filterNot { line ->
                val normalized = normalize(line.value)
                normalized.containsAny(ignoredAmountKeywords) ||
                    normalized.containsAny(idLikeAmountKeywords) ||
                    containsDate(line.value)
            }
            .flatMap { extractAmountCandidates(it.value, it.index, lines.size) }
            .preferTypicalAmounts()
            .map { it.value.toDouble() }
            .distinct()
            .sorted()
    }

    private fun buildAmountCandidates(
        selectedAmount: Double?,
        candidateValues: List<Double>
    ): List<Double> {
        return listOfNotNull(selectedAmount)
            .plus(candidateValues)
            .distinct()
            .take(MAX_AMOUNT_CANDIDATES)
    }

    private fun parseColumnarPriorityAmount(
        lines: List<IndexedValue<String>>,
        lineCount: Int
    ): AmountSelection? {
        val priorityLabelLines = lines.filter { line ->
            val normalized = normalize(line.value)
            normalized.containsAny(priorityKeywords) &&
                !normalized.containsAny(ignoredAmountKeywords) &&
                extractAmountCandidates(line.value, line.index, lineCount).isEmpty()
        }

        if (priorityLabelLines.isEmpty()) return null

        val preferredStartLine = priorityLabelLines
            .firstOrNull { normalize(it.value).containsAny(listOf("tien phai tra", "can thanh toan", "phai tra")) }
            ?: priorityLabelLines.first()

        findDirectColumnarAmount(
            lines = lines,
            preferredStartLine = preferredStartLine,
            lineCount = lineCount
        )?.let { return it }

        val fallbackCandidate = lines
            .asSequence()
            .filter { it.index > preferredStartLine.index }
            .take(COLUMNAR_AMOUNT_LOOKAHEAD_LINES)
            .filterNot { line ->
                val normalized = normalize(line.value)
                normalized.containsAny(ignoredAmountKeywords) ||
                    normalized.containsAny(idLikeAmountKeywords) ||
                    containsDate(line.value)
            }
            .flatMap { extractAmountCandidates(it.value, it.index, lineCount).asSequence() }
            .toList()
            .preferTypicalAmounts()
            .maxWithOrNull(compareBy<AmountCandidate> { it.value }.thenBy { it.lineIndex })
            ?: return null

        return AmountSelection(
            value = fallbackCandidate.value,
            sourceLine = lines.firstOrNull { it.index == fallbackCandidate.lineIndex }?.value.orEmpty(),
            reason = "columnar_priority"
        )
    }

    private fun findDirectColumnarAmount(
        lines: List<IndexedValue<String>>,
        preferredStartLine: IndexedValue<String>,
        lineCount: Int
    ): AmountSelection? {
        var sawIgnoredLabelBeforeAmount = false

        lines
            .asSequence()
            .filter { it.index > preferredStartLine.index }
            .take(DIRECT_COLUMNAR_AMOUNT_LOOKAHEAD_LINES)
            .forEach { line ->
                val normalized = normalize(line.value)
                if (normalized.containsAny(idLikeAmountKeywords) || containsDate(line.value)) {
                    return@forEach
                }

                val amounts = extractAmountCandidates(line.value, line.index, lineCount)
                if (normalized.containsAny(ignoredAmountKeywords)) {
                    sawIgnoredLabelBeforeAmount = true
                    return@forEach
                }

                if (amounts.isNotEmpty() && !sawIgnoredLabelBeforeAmount) {
                    val candidate = amounts
                        .preferTypicalAmounts()
                        .maxWithOrNull(compareBy<AmountCandidate> { it.score }.thenBy { it.value })
                        ?: return@forEach
                    return AmountSelection(
                        value = candidate.value,
                        sourceLine = line.value,
                        reason = "direct_columnar_priority"
                    )
                }
            }

        return null
    }

    private fun extractAmounts(line: String): List<Long> {
        return extractAmountCandidates(line, lineIndex = 0, lineCount = 1).map { it.value }
    }

    private fun extractAmountCandidates(
        line: String,
        lineIndex: Int,
        lineCount: Int
    ): List<AmountCandidate> {
        return amountRegex.findAll(line)
            .mapIndexedNotNull { amountIndex, match ->
                val value = parseAmountToken(match.value) ?: return@mapIndexedNotNull null
                if (value !in MIN_REASONABLE_AMOUNT..MAX_REASONABLE_AMOUNT) {
                    return@mapIndexedNotNull null
                }

                val normalized = normalize(line)
                var score = 0
                if (currencySuffixRegex.containsMatchIn(match.value)) score += 40
                if (match.range.last >= line.lastIndex - 3) score += 30
                if (value <= MAX_TYPICAL_RECEIPT_AMOUNT) score += 20 else score -= 50
                if (normalized.containsAny(priorityKeywords)) score += 50
                if (normalized.containsAny(idLikeAmountKeywords)) score -= 80
                score += calculateTailScore(lineIndex, lineCount)

                AmountCandidate(
                    value = value,
                    lineIndex = lineIndex,
                    amountIndex = amountIndex,
                    score = score
                )
            }
            .toList()
    }

    private fun parseAmountToken(rawToken: String): Long? {
        val withoutCurrency = currencySuffixRegex.replace(rawToken, "").trim()
        val normalizedToken = withoutCurrency
            .map { char ->
                when (char) {
                    'O', 'o' -> '0'
                    'I', 'i', 'l', 'L' -> '1'
                    else -> char
                }
            }
            .joinToString("")
            .replace("\\s+".toRegex(), " ")
            .trim()

        val withoutDecimalSuffix = decimalSuffixRegex.replace(normalizedToken, "")
        val digits = withoutDecimalSuffix.filter { it.isDigit() }
        return digits.toLongOrNull()
    }

    private fun calculateTailScore(lineIndex: Int, lineCount: Int): Int {
        if (lineCount <= 1) return 0
        val positionRatio = lineIndex.toDouble() / (lineCount - 1).coerceAtLeast(1)
        return when {
            positionRatio >= 0.85 -> 35
            positionRatio >= 0.70 -> 20
            else -> 0
        }
    }

    private fun List<AmountCandidate>.preferTypicalAmounts(): List<AmountCandidate> {
        val typical = filter { it.value <= MAX_TYPICAL_RECEIPT_AMOUNT }
        return typical.ifEmpty { this }
    }

    private fun parseDateMillis(rawText: String): Long? {
        val dayFirstDate = dayFirstDateRegex.findAll(rawText)
            .mapNotNull { match ->
                val day = match.groupValues[1].padStart(2, '0')
                val month = match.groupValues[2].padStart(2, '0')
                val rawYear = match.groupValues[3]
                val year = if (rawYear.length == 2) "20$rawYear" else rawYear
                parseDate("$day/$month/$year")
            }
            .firstOrNull()

        if (dayFirstDate != null) return dayFirstDate

        val yearFirstDate = yearFirstDateRegex.findAll(rawText)
            .mapNotNull { match ->
                val year = match.groupValues[1]
                val month = match.groupValues[2].padStart(2, '0')
                val day = match.groupValues[3].padStart(2, '0')
                parseDate("$day/$month/$year")
            }
            .firstOrNull()

        if (yearFirstDate != null) return yearFirstDate

        return textualMonthDateRegex.findAll(normalize(rawText))
            .mapNotNull { match ->
                val day = match.groupValues[1].padStart(2, '0')
                val month = match.groupValues[2].padStart(2, '0')
                val rawYear = match.groupValues[3]
                val year = when {
                    rawYear.isBlank() -> Calendar.getInstance().get(Calendar.YEAR).toString()
                    rawYear.length == 2 -> "20$rawYear"
                    else -> rawYear
                }
                parseDate("$day/$month/$year")
            }
            .firstOrNull()
    }

    private fun parseDate(value: String): Long? {
        return runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).apply {
                isLenient = false
            }.parse(value)?.time
        }.getOrNull()
    }

    private fun parseMerchantName(lines: List<String>): String? {
        return lines.firstOrNull { line ->
            val normalized = normalize(line)
            normalized.length >= 3 &&
                normalized.any { it.isLetter() } &&
                !normalized.containsAny(priorityKeywords) &&
                !normalized.containsAny(ignoredAmountKeywords) &&
                !normalized.containsAny(merchantNoiseKeywords) &&
                !containsDate(line) &&
                extractAmounts(line).isEmpty()
        }
    }

    private fun calculateConfidence(
        amount: Double?,
        dateMillis: Long?,
        merchantName: String?,
        rawText: String
    ): Float {
        var score = 0f
        if (amount != null) score += 0.50f
        if (dateMillis != null) score += 0.20f
        if (!merchantName.isNullOrBlank()) score += 0.15f
        if (normalize(rawText).containsAny(priorityKeywords)) score += 0.15f
        return score.coerceIn(0f, 1f)
    }

    private fun buildNote(merchantName: String?, rawText: String): String {
        return merchantName?.let { "Hóa đơn $it" }
            ?: rawText.lines().firstOrNull { it.isNotBlank() }?.take(MAX_NOTE_LENGTH)
            ?: "Hóa đơn OCR"
    }

    private fun buildAmountWarning(
        isLongReceipt: Boolean,
        amountCandidateCount: Int
    ): String? {
        return if (isLongReceipt && amountCandidateCount >= LONG_RECEIPT_WARNING_CANDIDATE_COUNT) {
            "Hóa đơn có nhiều số tiền, vui lòng kiểm tra lại tổng tiền trước khi lưu."
        } else {
            null
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { contains(it) }
    }

    private fun containsDate(value: String): Boolean {
        return dayFirstDateRegex.containsMatchIn(value) ||
            yearFirstDateRegex.containsMatchIn(value) ||
            fuzzyDateRegex.containsMatchIn(value) ||
            textualMonthDateRegex.containsMatchIn(normalize(value))
    }

    private fun normalize(value: String): String {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace('đ', 'd')
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private const val MIN_REASONABLE_AMOUNT = 1_000L
    private const val MAX_REASONABLE_AMOUNT = 100_000_000L
    private const val MAX_TYPICAL_RECEIPT_AMOUNT = 20_000_000L
    private const val COLUMNAR_AMOUNT_LOOKAHEAD_LINES = 60
    private const val DIRECT_COLUMNAR_AMOUNT_LOOKAHEAD_LINES = 5
    private const val PRIORITY_LOOKAHEAD_LINES = 5
    private const val PRIORITY_LOOKBEHIND_LINES = 2
    private const val TAIL_PRIORITY_SCAN_LINES = 30
    private const val MAX_AMOUNT_CANDIDATES = 8
    private const val MAX_NOTE_LENGTH = 60
    private const val LONG_RECEIPT_WARNING_CANDIDATE_COUNT = 5
    private const val STRONG_PRIORITY_LABEL_SCORE = 140
    private const val PRIORITY_LABEL_SCORE = 80
    private const val SAME_LINE_PRIORITY_BONUS = 80
    private const val BELOW_PRIORITY_BONUS = 70
    private const val ABOVE_PRIORITY_BONUS = 35
    private const val PRIORITY_DISTANCE_PENALTY = 10
    private const val TAIL_PRIORITY_BONUS = 45

    private data class AmountCandidate(
        val value: Long,
        val lineIndex: Int,
        val amountIndex: Int,
        val score: Int
    )

    private data class AmountSelection(
        val value: Long,
        val sourceLine: String,
        val reason: String
    )

    private data class ScoredAmountSelection(
        val selection: AmountSelection,
        val score: Int,
        val lineIndex: Int
    )
}
