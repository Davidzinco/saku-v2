package com.saku.v2.platform.ewallet

import com.saku.v2.data.model.TransactionType

/**
 * Pure Kotlin parser for Indonesian e-wallet and mobile banking transaction notifications.
 * Designed for JVM unit testing without Android Context dependencies.
 */
object EWalletNotificationParser {

    data class ParsedNotification(
        val sourceApp: String,
        val suggestedAccountName: String?,
        val amount: Long,
        val type: TransactionType,
        val suggestedNote: String
    )

    private data class SupportedApp(
        val sourceApp: String,
        val suggestedAccountName: String
    )

    private val packageMap: Map<String, SupportedApp> = mapOf(
        "com.gojek.app" to SupportedApp("GoPay", "GoPay"),
        "com.gopay.app" to SupportedApp("GoPay", "GoPay"),
        "ovo.id" to SupportedApp("OVO", "OVO"),
        "id.dana" to SupportedApp("DANA", "DANA"),
        "com.shopee.id" to SupportedApp("ShopeePay", "ShopeePay"),
        "com.telkom.mwallet" to SupportedApp("LinkAja", "LinkAja"),
        "com.linkaja.android" to SupportedApp("LinkAja", "LinkAja"),
        "com.bca" to SupportedApp("BCA", "BCA"),
        "com.bca.mybca.omni.android" to SupportedApp("BCA", "BCA"),
        "id.co.bri.brimo" to SupportedApp("BRImo", "BRImo"),
        "id.bmri.livin" to SupportedApp("Livin' Mandiri", "Mandiri"),
        "id.co.bankbkemobile.digitalbank" to SupportedApp("SeaBank", "SeaBank"),
        "com.jago.digitalBanking" to SupportedApp("Jago", "Jago")
    )

    private val lowercasePackageMap: Map<String, SupportedApp> =
        packageMap.mapKeys { (pkg, _) -> pkg.lowercase() }

    private val otpOrSensitiveRegex = Regex(
        pattern = """\b(otp|kode\s*otp|kode\s+verifikasi|verification\s+code|kode\s+keamanan|kode\s+rahasia|kode\s+konfirmasi|rahasia|m-?pin|pin|passcode|one[\s-]*time[\s-]*password|jangan\s+bagikan|jangan\s+berikan)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val promoSpamRegex = Regex(
        pattern = """\b(promo|diskon|cashback|voucher|kupon|hadiah|undian|menangkan|klaim|yuk|buruan|serbu|nikmati|penawaran|potongan|gratis\s+biaya|spesial|jangan\s+lewatkan|hingga\s+(?:rp|idr)|s\.?\s*d\.?|sampai\s+dengan)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val anyTxKeywordRegex = Regex(
        pattern = """\b(berhasil|sukses|selesai|bayar|pembayaran|pembelian|dibayar|terbayar|transfer|terima|menerima|diterima|masuk|kirim|terkirim|top\s*-?\s*up|qris|debit|terdebit|kredit|dikreditkan|senilai|sebesar|mutasi|transaksi)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val strongConfirmationRegex = Regex(
        pattern = """\b(berhasil|sukses|selesai|diterima|menerima|dibayar|terbayar|terkirim|terdebit|dikreditkan|senilai|sebesar|debit|kredit|transfer\s+masuk|saldo\s+masuk|mutasi)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val incomeStrongRegex = Regex(
        pattern = """\b(menerima|diterima|transfer\s+masuk|saldo\s+masuk|uang\s+masuk|dana\s+masuk|masuk\s+ke|kredit|dikreditkan|top\s*-?\s*up\s+berhasil)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val genericMasukRegex = Regex(
        pattern = """\bmasuk\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val topUpRegex = Regex(
        pattern = """\btop\s*-?\s*up\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val topUpOutgoingRegex = Regex(
        pattern = """\btop\s*-?\s*up\s+ke\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val expenseRegex = Regex(
        pattern = """\b(bayar|pembayaran|pembelian|dibayar|terbayar|qris|kirim|terkirim|debit|terdebit|berhasil\s+ke|transfer\s+ke|ditransfer\s+ke|belanja|tagihan)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val paymentSpendExplicitRegex = Regex(
        pattern = """\b(bayar|pembayaran|pembelian|dibayar|terbayar|debit|terdebit)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val amountRegex = Regex(
        pattern = """(?:rp\.?|idr)\s*([0-9]+(?:[.,][0-9]{3})*(?:[.,][0-9]{1,2})?|[0-9]+)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val nonTransactionAmountPrefixRegex = Regex(
        pattern = """(?:sisa\s+saldo|saldo\s+akhir|saldo\s+anda|cashback|diskon|hemat|potongan|hingga|maks\.?)\s*$""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val trailingDecimalRegex = Regex("""^(.+?)([.,])([0-9]{1,2})$""")

    private val merchantDiRegex = Regex(
        pattern = """\bdi\s+([A-Za-z0-9&'.,/\- ]{2,45}?)(?=\s+(?:berhasil|sukses|selesai|sebesar|senilai|pakai|menggunakan|via|dengan|pada|tgl|tanggal|rp\.?|idr|telah)\b|[,!.;]|$)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val recipientKeRegex = Regex(
        pattern = """\bke\s+([A-Za-z0-9&'.,/\- ]{2,45}?)(?=\s+(?:berhasil|sukses|selesai|sebesar|senilai|pakai|menggunakan|melalui|via|dengan|pada|tgl|tanggal|rp\.?|idr|telah)\b|[,!.;]|$)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val senderDariRegex = Regex(
        pattern = """\bdari\s+([A-Za-z0-9&'.,/\- ]{2,45}?)(?=\s+(?:berhasil|sukses|selesai|sebesar|senilai|melalui|via|dengan|pada|tgl|tanggal|rp\.?|idr|telah)\b|[,!.;]|$)""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val qrisRegex = Regex("""\bqris\b""", RegexOption.IGNORE_CASE)
    private val multiWhitespaceRegex = Regex("""\s+""")

    fun parse(packageName: String, title: String, text: String): ParsedNotification? {
        val app = packageMap[packageName] ?: lowercasePackageMap[packageName.lowercase()] ?: return null
        val combined = listOf(title.trim(), text.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" • ")
        if (combined.isBlank()) return null

        if (otpOrSensitiveRegex.containsMatchIn(combined)) {
            return null
        }

        if (!anyTxKeywordRegex.containsMatchIn(combined)) {
            return null
        }

        val isPromoLike = promoSpamRegex.containsMatchIn(combined)
        if (isPromoLike && !strongConfirmationRegex.containsMatchIn(combined)) {
            return null
        }

        val amount = extractPrimaryAmount(combined) ?: return null
        if (amount <= 0L) return null

        val type = resolveTransactionType(combined)
        val note = extractConciseNote(
            title = title.trim(),
            text = text.trim(),
            combined = combined,
            type = type,
            sourceApp = app.sourceApp
        )

        return ParsedNotification(
            sourceApp = app.sourceApp,
            suggestedAccountName = app.suggestedAccountName,
            amount = amount,
            type = type,
            suggestedNote = note
        )
    }

    private fun extractPrimaryAmount(text: String): Long? {
        val matches = amountRegex.findAll(text).toList()
        if (matches.isEmpty()) return null

        val preferred = matches.firstOrNull { match ->
            val prefixWindow = text.substring(0, match.range.first).takeLast(24)
            !nonTransactionAmountPrefixRegex.containsMatchIn(prefixWindow)
        } ?: matches.first()

        return normalizeAmount(preferred.groupValues[1])
    }

    internal fun normalizeAmount(rawNumber: String): Long? {
        val cleaned = rawNumber.trim().trimEnd('.', ',', '-')
        if (cleaned.isEmpty()) return null

        val decimalMatch = trailingDecimalRegex.matchEntire(cleaned)
        val integerPart = if (decimalMatch != null) {
            decimalMatch.groupValues[1]
        } else {
            cleaned
        }

        val digitsOnly = integerPart.replace(".", "").replace(",", "")
        return digitsOnly.toLongOrNull()
    }

    private fun resolveTransactionType(text: String): TransactionType {
        val hasStrongIncome = incomeStrongRegex.containsMatchIn(text) || genericMasukRegex.containsMatchIn(text)
        val hasTopUpIncome = topUpRegex.containsMatchIn(text) &&
            strongConfirmationRegex.containsMatchIn(text) &&
            !paymentSpendExplicitRegex.containsMatchIn(text) &&
            !topUpOutgoingRegex.containsMatchIn(text)

        if (hasStrongIncome || hasTopUpIncome) {
            return TransactionType.INCOME
        }

        if (expenseRegex.containsMatchIn(text)) {
            return TransactionType.EXPENSE
        }

        return TransactionType.EXPENSE
    }

    private fun extractConciseNote(
        title: String,
        text: String,
        combined: String,
        type: TransactionType,
        sourceApp: String
    ): String {
        val isQris = qrisRegex.containsMatchIn(combined)
        val isTopUp = topUpRegex.containsMatchIn(combined)

        val diTarget = merchantDiRegex.find(text)?.groupValues?.get(1)?.let(::cleanFragment)
            ?: merchantDiRegex.find(title)?.groupValues?.get(1)?.let(::cleanFragment)
        if (!diTarget.isNullOrBlank()) {
            return formatWithQrisPrefix(diTarget, isQris)
        }

        if (type == TransactionType.INCOME) {
            val dariTarget = senderDariRegex.find(text)?.groupValues?.get(1)?.let(::cleanFragment)
                ?: senderDariRegex.find(title)?.groupValues?.get(1)?.let(::cleanFragment)
            if (!dariTarget.isNullOrBlank()) {
                return when {
                    isTopUp -> "Top up dari $dariTarget"
                    isQris -> "QRIS masuk dari $dariTarget"
                    else -> "Dari $dariTarget"
                }.take(60)
            }
            if (isTopUp) return "Top up $sourceApp"
        }

        val keTarget = recipientKeRegex.find(text)?.groupValues?.get(1)?.let(::cleanFragment)
            ?: recipientKeRegex.find(title)?.groupValues?.get(1)?.let(::cleanFragment)
        if (!keTarget.isNullOrBlank() && !keTarget.equals("rekening anda", ignoreCase = true)) {
            return formatWithQrisPrefix(keTarget, isQris)
        }

        val base = text.ifBlank { title }
        val stripped = base
            .replace(amountRegex, " ")
            .replace(Regex("""\b(sebesar|senilai|telah|berhasil|sukses|menggunakan|pakai)\b""", RegexOption.IGNORE_CASE), " ")
            .let(::cleanFragment)

        return if (stripped.isNotBlank()) {
            stripped.take(60)
        } else if (isQris) {
            "Transaksi QRIS $sourceApp"
        } else {
            "Transaksi $sourceApp"
        }
    }

    private fun formatWithQrisPrefix(target: String, isQris: Boolean): String {
        val result = if (isQris && !target.contains("qris", ignoreCase = true)) {
            "QRIS $target"
        } else {
            target
        }
        return result.take(60)
    }

    private fun cleanFragment(raw: String): String {
        return raw
            .replace(multiWhitespaceRegex, " ")
            .trim()
            .trim('.', ',', '-', ':', '!', ';')
            .trim()
    }
}
