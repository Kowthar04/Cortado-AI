package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.example.cafeshopassignment.ui.common.toast
import com.example.cafeshopassignment.ui.notifications.SendNotificationEvent
import com.example.cafeshopassignment.ui.notifications.SendNotificationViewModel

class SendNotificationActivity : AppCompatActivity() {
    private val viewModel: SendNotificationViewModel by viewModels { SendNotificationViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_notification)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        val titleInput = findViewById<EditText>(R.id.notificationTitleInput)
        val messageInput = findViewById<EditText>(R.id.notificationMessageInput)
        val recipientInput = findViewById<EditText>(R.id.notificationRecipientInput)
        val sendToAllSwitch = findViewById<SwitchCompat>(R.id.sendToAllSwitch)
        val sendButton = findViewById<Button>(R.id.sendNotificationButton)

        sendToAllSwitch.setOnCheckedChangeListener { _, checked -> recipientInput.isEnabled = !checked }
        sendButton.setOnClickListener {
            viewModel.send(
                title = titleInput.text.toString(),
                message = messageInput.text.toString(),
                recipientId = recipientInput.text.toString(),
                sendToAll = sendToAllSwitch.isChecked,
            )
        }

        collectWhileStarted(viewModel.isSending) { sending -> sendButton.isEnabled = !sending }
        collectWhileStarted(viewModel.events) { event ->
            when (event) {
                SendNotificationEvent.MissingTitleOrMessage -> toast(R.string.send_missing_fields)
                SendNotificationEvent.MissingRecipient -> toast(R.string.send_missing_recipient)
                SendNotificationEvent.SentToUser -> {
                    toast(R.string.send_sent_to_user)
                    titleInput.text.clear()
                    messageInput.text.clear()
                }
                is SendNotificationEvent.SentToAll -> {
                    toast(R.string.send_sent_to_all, event.count)
                    finish()
                }
                is SendNotificationEvent.Failed -> toast(R.string.send_failed, event.detail.orEmpty())
            }
        }
    }
}
