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

class AdminLoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels { LoginViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val emailInput = findViewById<EditText>(R.id.adminEmailInput)
        val passwordInput = findViewById<EditText>(R.id.adminPasswordInput)
        val loginButton = findViewById<Button>(R.id.adminLoginButton)

        findViewById<Button>(R.id.backToUserLoginButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        loginButton.setOnClickListener {
            viewModel.login(emailInput.text.toString(), passwordInput.text.toString(), requireAdmin = true)
        }

        collectWhileStarted(viewModel.isLoading) { loading -> loginButton.isEnabled = !loading }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                LoginEvent.LoggedInAsAdmin -> {
                    toast(R.string.admin_login_success)
                    val intent =
                        Intent(this, AdminDashboardActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    startActivity(intent)
                }
                LoginEvent.MissingFields -> toast(R.string.login_missing_fields)
                LoginEvent.NotAnAdmin -> toast(R.string.admin_access_denied)
                LoginEvent.ProfileMissing -> toast(R.string.admin_profile_missing)
                is LoginEvent.Failed -> toast(R.string.login_failed, event.detail.orEmpty())
                LoginEvent.LoggedInAsCustomer -> Unit
            }
        }
    }
}
