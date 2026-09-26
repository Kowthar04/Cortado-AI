package com.example.cafeshopassignment.ui.payment

enum class PaymentMethod(
    val label: String,
) {
    CARD("Credit/Debit Card"),
    GOOGLE_PAY("Google Pay"),
}

data class PaymentSummary(
    val subtotal: Double,
    val serviceFee: Double,
    val discount: Double,
    val promoApplied: Boolean,
) {
    val total: Double
        get() = (subtotal + serviceFee - discount).coerceAtLeast(0.0)
}

/** Pricing rules for checkout: flat service fee plus the "THIRSTY" 20%-off promo code. */
object PaymentCalculator {
    const val SERVICE_FEE = 0.50
    const val PROMO_CODE = "THIRSTY"
    const val PROMO_DISCOUNT_RATE = 0.20

    fun isValidPromo(code: String): Boolean = code.trim().equals(PROMO_CODE, ignoreCase = true)

    fun summarize(
        subtotal: Double,
        promoApplied: Boolean,
    ): PaymentSummary {
        val discount = if (promoApplied) (subtotal + SERVICE_FEE) * PROMO_DISCOUNT_RATE else 0.0
        return PaymentSummary(subtotal, SERVICE_FEE, discount, promoApplied)
    }
}

data class CardDetails(
    val number: String,
    val holderName: String,
    val expiry: String,
    val cvv: String,
)

enum class CardField { NUMBER, HOLDER_NAME, EXPIRY, CVV }

enum class CardError {
    INVALID_NUMBER,
    MISSING_NAME,
    EXPIRY_FORMAT,
    INVALID_MONTH,
    INVALID_CVV,
    ;

    val field: CardField
        get() =
            when (this) {
                INVALID_NUMBER -> CardField.NUMBER
                MISSING_NAME -> CardField.HOLDER_NAME
                EXPIRY_FORMAT, INVALID_MONTH -> CardField.EXPIRY
                INVALID_CVV -> CardField.CVV
            }
}

/** Basic client-side card form checks (this is a demo checkout; no card data leaves the device). */
object CardValidator {
    /** Returns the first problem found, or null if the details look valid. */
    fun validate(card: CardDetails): CardError? {
        val number = card.number.filterNot { it.isWhitespace() }
        val expiry = card.expiry.trim()
        val cvv = card.cvv.trim()
        return when {
            number.length !in 13..19 || !number.all { it.isDigit() } -> CardError.INVALID_NUMBER
            card.holderName.isBlank() -> CardError.MISSING_NAME
            expiry.length != 4 || !expiry.all { it.isDigit() } -> CardError.EXPIRY_FORMAT
            expiry.substring(0, 2).toInt() !in 1..12 -> CardError.INVALID_MONTH
            cvv.length !in 3..4 || !cvv.all { it.isDigit() } -> CardError.INVALID_CVV
            else -> null
        }
    }
}
