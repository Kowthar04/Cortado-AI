package com.example.cafeshopassignment

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.cafeshopassignment.ui.auth.LoginEvent
import com.example.cafeshopassignment.ui.auth.LoginViewModel
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels { LoginViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.editTextUserName)
        val passwordInput = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.loginButton)

        findViewById<Button>(R.id.adminLoginButton).setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
        findViewById<Button>(R.id.registerRedirectButton).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        loginButton.setOnClickListener {
            viewModel.login(emailInput.text.toString(), passwordInput.text.toString(), requireAdmin = false)
        }

        collectWhileStarted(viewModel.isLoading) { loading -> loginButton.isEnabled = !loading }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                LoginEvent.LoggedInAsCustomer -> {
                    toast(R.string.login_success)
                    startActivity(Intent(this, MenuActivity::class.java))
                    finish()
                }
                LoginEvent.MissingFields -> toast(R.string.login_missing_fields)
                is LoginEvent.Failed -> toast(R.string.login_failed, event.detail.orEmpty())
                // Admin-only outcomes are not produced by the customer login.
                LoginEvent.LoggedInAsAdmin, LoginEvent.NotAnAdmin, LoginEvent.ProfileMissing -> Unit
            }
        }
    }
}
