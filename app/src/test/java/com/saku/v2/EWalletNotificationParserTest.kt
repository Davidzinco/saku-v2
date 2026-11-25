package com.saku.v2

import com.saku.v2.data.model.TransactionType
import com.saku.v2.platform.ewallet.EWalletNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EWalletNotificationParserTest {

    @Test
    fun parses_gopay_expense_with_thousand_dots_and_trailing_balance() {
        val parsed = EWalletNotificationParser.parse(
            packageName = "com.gojek.app",
            title = "Pembayaran berhasil!",
            text = "Kamu bayar Rp 42.500 di Kopi Kenangan pakai GoPay. Sisa saldo Rp 637.500"
        )

        assertNotNull(parsed)
        assertEquals("GoPay", parsed!!.sourceApp)
        assertEquals("GoPay", parsed.suggestedAccountName)
        assertEquals(42_500L, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Kopi Kenangan", parsed.suggestedNote)
    }

    @Test
    fun parses_gopay_standalone_package_topup_as_income() {
        val parsed = EWalletNotificationParser.parse(
            packageName = "com.gopay.app",
            title = "Top Up Berhasil",
            text = "Top up GoPay sebesar Rp 500.000 dari BCA berhasil."
        )

        assertNotNull(parsed)
        assertEquals("GoPay", parsed!!.sourceApp)
        assertEquals(500_000L, parsed.amount)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertTrue(parsed.suggestedNote.contains("BCA"))

        val outgoingTopUp = EWalletNotificationParser.parse(
            packageName = "com.bca",
            title = "Transfer Berhasil",
            text = "Top up ke GoPay Rp 100.000 berhasil"
        )
        assertNotNull(outgoingTopUp)
        assertEquals("BCA", outgoingTopUp!!.sourceApp)
        assertEquals(100_000L, outgoingTopUp.amount)
        assertEquals(TransactionType.EXPENSE, outgoingTopUp.type)
    }

    @Test
    fun parses_ovo_qris_expense_with_indonesian_decimals() {
        val parsed = EWalletNotificationParser.parse(
            packageName = "ovo.id",
            title = "Pembayaran QRIS Berhasil",
            text = "Pembayaran QRIS sebesar Rp45.000,00 di Starbucks Reserve telah berhasil."
        )

        assertNotNull(parsed)
        assertEquals("OVO", parsed!!.sourceApp)
        assertEquals("OVO", parsed.suggestedAccountName)
        assertEquals(45_000L, parsed.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("QRIS Starbucks Reserve", parsed.suggestedNote)
    }

    @Test
    fun parses_dana_incoming_transfer_and_qris_income() {
        val transferIn = EWalletNotificationParser.parse(
            packageName = "id.dana",
            title = "Dana Masuk",
            text = "Kamu menerima transfer sebesar Rp 250.000 dari Andi Pratama"
        )
        assertNotNull(transferIn)
        assertEquals("DANA", transferIn!!.sourceApp)
        assertEquals(250_000L, transferIn.amount)
        assertEquals(TransactionType.INCOME, transferIn.type)
        assertEquals("Dari Andi Pratama", transferIn.suggestedNote)

        val qrisIn = EWalletNotificationParser.parse(
            packageName = "id.dana",
            title = "QRIS Diterima",
            text = "Kamu menerima pembayaran QRIS sebesar Rp 75.000 dari Warung Mba Siti"
        )
        assertNotNull(qrisIn)
        assertEquals(75_000L, qrisIn!!.amount)
        assertEquals(TransactionType.INCOME, qrisIn.type)
        assertEquals("QRIS masuk dari Warung Mba Siti", qrisIn.suggestedNote)
    }

    @Test
    fun parses_shopeepay_idr_comma_thousands_and_dot_decimals() {
        val plainIdr = EWalletNotificationParser.parse(
            packageName = "com.shopee.id",
            title = "Pembayaran Berhasil",
            text = "Pembayaran ShopeePay senilai IDR 125,000 di Indomaret berhasil"
        )
        assertNotNull(plainIdr)
        assertEquals("ShopeePay", plainIdr!!.sourceApp)
        assertEquals(125_000L, plainIdr.amount)
        assertEquals(TransactionType.EXPENSE, plainIdr.type)
        assertEquals("Indomaret", plainIdr.suggestedNote)

        val idrWithCents = EWalletNotificationParser.parse(
            packageName = "com.shopee.id",
            title = "Pembelian Berhasil",
            text = "Pembelian di Tokopedia sebesar IDR 1,250,000.00 telah berhasil"
        )
        assertNotNull(idrWithCents)
        assertEquals(1_250_000L, idrWithCents!!.amount)
        assertEquals(TransactionType.EXPENSE, idrWithCents.type)
        assertEquals("Tokopedia", idrWithCents.suggestedNote)
    }

    @Test
    fun parses_bca_and_mybca_qris_expense_and_transfer_income() {
        val bcaExpense = EWalletNotificationParser.parse(
            packageName = "com.bca",
            title = "Transaksi Berhasil",
            text = "Pembayaran QRIS berhasil ke WARUNG KOPI NUSANTARA sebesar Rp 28.000"
        )
        assertNotNull(bcaExpense)
        assertEquals("BCA", bcaExpense!!.sourceApp)
        assertEquals("BCA", bcaExpense.suggestedAccountName)
        assertEquals(28_000L, bcaExpense.amount)
        assertEquals(TransactionType.EXPENSE, bcaExpense.type)
        assertEquals("QRIS WARUNG KOPI NUSANTARA", bcaExpense.suggestedNote)

        val myBcaIncome = EWalletNotificationParser.parse(
            packageName = "com.bca.mybca.omni.android",
            title = "Mutasi Rekening",
            text = "Transfer masuk Rp 1.500.000,00 dari PT MAJU BERSAMA"
        )
        assertNotNull(myBcaIncome)
        assertEquals("BCA", myBcaIncome!!.sourceApp)
        assertEquals(1_500_000L, myBcaIncome.amount)
        assertEquals(TransactionType.INCOME, myBcaIncome.type)
    }

    @Test
    fun parses_additional_indonesian_digital_banks_and_ewallets() {
        val brimo = EWalletNotificationParser.parse(
            packageName = "id.co.bri.brimo",
            title = "BRImo",
            text = "Transaksi Debit Rp 180.000 di Pertamina SPBU berhasil"
        )
        assertNotNull(brimo)
        assertEquals("BRImo", brimo!!.sourceApp)
        assertEquals(TransactionType.EXPENSE, brimo.type)
        assertEquals(180_000L, brimo.amount)

        val livin = EWalletNotificationParser.parse(
            packageName = "id.bmri.livin",
            title = "Livin' Mandiri",
            text = "Transaksi Kredit sebesar Rp 2.000.000,00 ke rekening Anda"
        )
        assertNotNull(livin)
        assertEquals("Livin' Mandiri", livin!!.sourceApp)
        assertEquals("Mandiri", livin.suggestedAccountName)
        assertEquals(TransactionType.INCOME, livin.type)
        assertEquals(2_000_000L, livin.amount)

        val seabank = EWalletNotificationParser.parse(
            packageName = "id.co.bankbkemobile.digitalbank",
            title = "SeaBank",
            text = "Kamu telah kirim Rp 95.000 ke Rina Wijaya berhasil"
        )
        assertNotNull(seabank)
        assertEquals("SeaBank", seabank!!.sourceApp)
        assertEquals(TransactionType.EXPENSE, seabank.type)
        assertEquals(95_000L, seabank.amount)

        val jago = EWalletNotificationParser.parse(
            packageName = "com.jago.digitalBanking",
            title = "Kantong Jago",
            text = "Uang masuk Rp 350.000 dari Kantong Utama diterima"
        )
        assertNotNull(jago)
        assertEquals("Jago", jago!!.sourceApp)
        assertEquals(TransactionType.INCOME, jago.type)
        assertEquals(350_000L, jago.amount)

        val linkAja = EWalletNotificationParser.parse(
            packageName = "com.linkaja.android",
            title = "LinkAja",
            text = "Pembelian Token Listrik Rp 102.500 berhasil"
        )
        assertNotNull(linkAja)
        assertEquals("LinkAja", linkAja!!.sourceApp)
        assertEquals(TransactionType.EXPENSE, linkAja.type)
        assertEquals(102_500L, linkAja.amount)
    }

    @Test
    fun rejects_otp_verification_pin_and_secret_messages() {
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "com.gojek.app",
                title = "Kode OTP GoPay",
                text = "Gunakan OTP 849201 untuk transaksi Rp 150.000. Jangan bagikan ke siapapun."
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "id.dana",
                title = "Kode Verifikasi",
                text = "Kode verifikasi pembayaran Rp 45.000 adalah 112233. Bersifat rahasia."
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "com.bca",
                title = "Keamanan Akun",
                text = "Masukkan PIN m-BCA untuk menyelesaikan transfer Rp 500.000"
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "ovo.id",
                title = "Jangan Berikan Kode Rahasia",
                text = "Kode rahasia Anda 998877 untuk verifikasi transaksi Rp 20.000"
            )
        )
    }

    @Test
    fun rejects_generic_promo_cashback_spam_and_non_transaction_alerts() {
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "ovo.id",
                title = "Promo Spesial Akhir Pekan!",
                text = "Cashback hingga Rp 50.000 pakai QRIS OVO hari ini yuk!"
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "id.dana",
                title = "Voucher DANA Buat Kamu",
                text = "Nikmati diskon Rp 25.000 untuk bayar tagihan pertamamu!"
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "com.shopee.id",
                title = "Gratis Biaya Admin",
                text = "Promo Top Up ShopeePay s.d Rp 100.000 buruan klaim!"
            )
        )
        assertNull(
            EWalletNotificationParser.parse(
                packageName = "com.whatsapp",
                title = "Budi",
                text = "Pembayaran Rp 50.000 di warung sudah berhasil ya"
            )
        )
    }

    @Test
    fun accepts_real_payment_even_when_cashback_mention_follows() {
        val parsed = EWalletNotificationParser.parse(
            packageName = "com.gojek.app",
            title = "Pembayaran Berhasil",
            text = "Pembayaran di Kopi Kenangan sebesar Rp 35.000 berhasil! Dapatkan cashback Rp 5.000."
        )

        assertNotNull(parsed)
        assertEquals(35_000L, parsed!!.amount)
        assertEquals(TransactionType.EXPENSE, parsed.type)
        assertEquals("Kopi Kenangan", parsed.suggestedNote)
    }
}
