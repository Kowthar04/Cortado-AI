package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cafeshopassignment.ui.auth.RegisterEvent
import com.example.cafeshopassignment.ui.auth.RegisterViewModel
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast

class RegisterActivity : AppCompatActivity() {
    private val viewModel: RegisterViewModel by viewModels { RegisterViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val firstnameInput = findViewById<EditText>(R.id.editTextFirstName)
        val surnameInput = findViewById<EditText>(R.id.editTextSurname)
        val emailInput = findViewById<EditText>(R.id.editTextEmail)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            viewModel.register(
                firstname = firstnameInput.text.toString(),
                surname = surnameInput.text.toString(),
                email = emailInput.text.toString(),
                password = passwordInput.text.toString(),
            )
        }
        findViewById<Button>(R.id.loginRedirectButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        collectWhileStarted(viewModel.isLoading) { loading -> registerButton.isEnabled = !loading }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                RegisterEvent.Registered -> {
                    toast(R.string.register_success)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                RegisterEvent.MissingFields -> toast(R.string.register_missing_fields)
                is RegisterEvent.Failed -> toast(R.string.register_failed, event.detail.orEmpty())
            }
        }
    }
}
