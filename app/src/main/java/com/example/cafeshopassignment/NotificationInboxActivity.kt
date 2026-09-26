package com.example.cafeshopassignment

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.adapters.NotificationAdapter
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.notifications.NotificationInboxViewModel

class NotificationInboxActivity : AppCompatActivity() {
    private val viewModel: NotificationInboxViewModel by viewModels { NotificationInboxViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_inbox)

        val toolbar = findViewById<Toolbar>(R.id.adminToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val notificationAdapter = NotificationAdapter()
        findViewById<RecyclerView>(R.id.inboxRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@NotificationInboxActivity)
            adapter = notificationAdapter
        }

        // Live listener: attached while the inbox is visible, removed when it stops.
        collectWhileStarted(viewModel.uiState) { state ->
            notificationAdapter.submitList(state.notifications)
            when {
                state.notSignedIn -> toast(R.string.error_not_logged_in)
                state.error != null -> toast(R.string.inbox_load_failed, state.error)
            }
        }
    }
}
