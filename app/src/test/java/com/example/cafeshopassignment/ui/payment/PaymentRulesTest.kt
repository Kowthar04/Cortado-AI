package com.example.cafeshopassignment.ui.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentRulesTest {
    private val validCard = CardDetails(number = "4242 4242 4242 4242", holderName = "Sam Lee", expiry = "1229", cvv = "123")

    @Test
    fun `summary adds the service fee`() {
        val summary = PaymentCalculator.summarize(10.0, promoApplied = false)

        assertEquals(0.50, summary.serviceFee, 0.0)
        assertEquals(0.0, summary.discount, 0.0)
        assertEquals(10.50, summary.total, 0.0001)
    }

    @Test
    fun `promo takes 20 percent off subtotal plus fee`() {
        val summary = PaymentCalculator.summarize(10.0, promoApplied = true)

        assertEquals(2.10, summary.discount, 0.0001)
        assertEquals(8.40, summary.total, 0.0001)
    }

    @Test
    fun `promo code check is case and whitespace insensitive`() {
        assertTrue(PaymentCalculator.isValidPromo(" thirsty "))
        assertFalse(PaymentCalculator.isValidPromo("HUNGRY"))
    }

    @Test
    fun `valid card passes`() {
        assertNull(CardValidator.validate(validCard))
    }

    @Test
    fun `card checks report the first failing field`() {
        assertEquals(CardError.INVALID_NUMBER, CardValidator.validate(validCard.copy(number = "1234")))
        assertEquals(CardError.INVALID_NUMBER, CardValidator.validate(validCard.copy(number = "4242abcd42424242")))
        assertEquals(CardError.MISSING_NAME, CardValidator.validate(validCard.copy(holderName = " ")))
        assertEquals(CardError.EXPIRY_FORMAT, CardValidator.validate(validCard.copy(expiry = "12/29")))
        assertEquals(CardError.INVALID_MONTH, CardValidator.validate(validCard.copy(expiry = "1329")))
        assertEquals(CardError.INVALID_CVV, CardValidator.validate(validCard.copy(cvv = "12")))
        assertEquals(CardField.EXPIRY, CardError.INVALID_MONTH.field)
    }
}
