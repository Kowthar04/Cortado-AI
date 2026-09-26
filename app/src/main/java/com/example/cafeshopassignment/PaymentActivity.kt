package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.formatPrice
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.payment.CardDetails
import com.example.cafeshopassignment.ui.payment.CardError
import com.example.cafeshopassignment.ui.payment.CardField
import com.example.cafeshopassignment.ui.payment.PaymentEvent
import com.example.cafeshopassignment.ui.payment.PaymentMethod
import com.example.cafeshopassignment.ui.payment.PaymentUiState
import com.example.cafeshopassignment.ui.payment.PaymentViewModel
import com.google.android.material.card.MaterialCardView

class PaymentActivity : AppCompatActivity() {
    private val viewModel: PaymentViewModel by viewModels { PaymentViewModel.Factory }

    private lateinit var cardPaymentRadio: RadioButton
    private lateinit var googlePayRadio: RadioButton
    private lateinit var cardDetailsSection: View
    private lateinit var cardNumberInput: EditText
    private lateinit var cardholderNameInput: EditText
    private lateinit var expiryDateInput: EditText
    private lateinit var cvvInput: EditText
    private lateinit var subtotalText: TextView
    private lateinit var serviceFeeText: TextView
    private lateinit var totalText: TextView
    private lateinit var payNowButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        subtotalText = findViewById(R.id.subtotalAmount)
        serviceFeeText = findViewById(R.id.serviceFeeAmount)
        totalText = findViewById(R.id.totalAmount)
        payNowButton = findViewById(R.id.payNowButton)
        cardPaymentRadio = findViewById(R.id.cardPaymentRadio)
        googlePayRadio = findViewById(R.id.googlePayRadio)
        cardDetailsSection = findViewById(R.id.cardDetailsSection)
        cardNumberInput = findViewById(R.id.cardNumberInput)
        cardholderNameInput = findViewById(R.id.cardholderNameInput)
        expiryDateInput = findViewById(R.id.expiryDateInput)
        cvvInput = findViewById(R.id.cvvInput)
        val promoCodeInput = findViewById<EditText>(R.id.promoCodeInput)

        findViewById<Button>(R.id.applyPromoButton).setOnClickListener {
            viewModel.applyPromo(promoCodeInput.text.toString())
        }
        findViewById<MaterialCardView>(R.id.cardPaymentOption).setOnClickListener {
            viewModel.selectMethod(PaymentMethod.CARD)
        }
        findViewById<MaterialCardView>(R.id.googlePayOption).setOnClickListener {
            viewModel.selectMethod(PaymentMethod.GOOGLE_PAY)
        }
        cardPaymentRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.selectMethod(PaymentMethod.CARD)
        }
        googlePayRadio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.selectMethod(PaymentMethod.GOOGLE_PAY)
        }
        payNowButton.setOnClickListener {
            viewModel.pay(
                CardDetails(
                    number = cardNumberInput.text.toString(),
                    holderName = cardholderNameInput.text.toString(),
                    expiry = expiryDateInput.text.toString(),
                    cvv = cvvInput.text.toString(),
                ),
            )
        }

        collectWhileStarted(viewModel.uiState, ::render)
        collectWhileStarted(viewModel.events, ::handleEvent)
    }

    private fun render(state: PaymentUiState) {
        subtotalText.text = formatPrice(state.summary.subtotal)
        serviceFeeText.text = formatPrice(state.summary.serviceFee)
        totalText.text = formatPrice(state.summary.total)
        cardPaymentRadio.isChecked = state.method == PaymentMethod.CARD
        googlePayRadio.isChecked = state.method == PaymentMethod.GOOGLE_PAY
        cardDetailsSection.isVisible = state.method == PaymentMethod.CARD
        payNowButton.isEnabled = !state.isProcessing
        payNowButton.text = getString(if (state.isProcessing) R.string.payment_processing else R.string.payment_pay_now)
    }

    private fun handleEvent(event: PaymentEvent) {
        when (event) {
            PaymentEvent.PromoCodeEmpty -> toast(R.string.payment_promo_empty)
            PaymentEvent.PromoApplied -> toast(R.string.payment_promo_applied)
            PaymentEvent.PromoInvalid -> toast(R.string.payment_promo_invalid)
            PaymentEvent.SelectPaymentMethod -> toast(R.string.payment_select_method)
            is PaymentEvent.InvalidCard -> showCardError(event.error)
            PaymentEvent.CartEmpty -> toast(R.string.cart_empty)
            PaymentEvent.NotLoggedIn -> toast(R.string.error_not_logged_in)
            is PaymentEvent.Failed -> toast(R.string.payment_failed, event.detail.orEmpty())
            is PaymentEvent.OrderPlaced -> {
                val intent =
                    Intent(this, OrderConfirmationActivity::class.java).apply {
                        putExtra(OrderConfirmationActivity.EXTRA_ORDER_TOTAL, event.total)
                        putExtra(OrderConfirmationActivity.EXTRA_ORDER_ID, event.orderId)
                    }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showCardError(error: CardError) {
        val message =
            getString(
                when (error) {
                    CardError.INVALID_NUMBER -> R.string.card_error_number
                    CardError.MISSING_NAME -> R.string.card_error_name
                    CardError.EXPIRY_FORMAT -> R.string.card_error_expiry_format
                    CardError.INVALID_MONTH -> R.string.card_error_month
                    CardError.INVALID_CVV -> R.string.card_error_cvv
                },
            )
        val field =
            when (error.field) {
                CardField.NUMBER -> cardNumberInput
                CardField.HOLDER_NAME -> cardholderNameInput
                CardField.EXPIRY -> expiryDateInput
                CardField.CVV -> cvvInput
            }
        field.error = message
        field.requestFocus()
    }
}
