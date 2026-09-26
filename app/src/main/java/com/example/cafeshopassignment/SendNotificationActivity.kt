package com.example.cafeshopassignment

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class SendNotificationActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var titleInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var recipientInput: EditText
    private lateinit var sendButton: Button
    private lateinit var sendToAllSwitch: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_notification)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }

        db = FirebaseFirestore.getInstance()

        titleInput = findViewById(R.id.notificationTitleInput)
        messageInput = findViewById(R.id.notificationMessageInput)
        recipientInput = findViewById(R.id.notificationRecipientInput)
        sendButton = findViewById(R.id.sendNotificationButton)
        sendToAllSwitch = findViewById(R.id.sendToAllSwitch)

        sendButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val message = messageInput.text.toString().trim()
            val recipientId = recipientInput.text.toString().trim()
            val sendToAll = sendToAllSwitch.isChecked

            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in the title and message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sendToAll) {
                sendNotificationToAllUsers(title, message)
            } else {
                if (recipientId.isEmpty()) {
                    Toast.makeText(this, "Please enter recipient UID or enable 'Send to All'", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sendNotificationToUser(recipientId, title, message)
            }
        }
    }

    private fun sendNotificationToUser(
        recipientId: String,
        title: String,
        message: String,
    ) {
        val notificationData =
            hashMapOf(
                "title" to title,
                "message" to message,
                "recipientId" to recipientId,
                "notificationId" to UUID.randomUUID().toString(),
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp(),
            )

        db
            .collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Notification sent to user!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotificationToAllUsers(
        title: String,
        message: String,
    ) {
        db
            .collection("users")
            .get()
            .addOnSuccessListener { users ->
                for (user in users) {
                    val uid = user.id
                    sendNotificationToUser(uid, title, message)
                }

                Toast.makeText(this, "Promo sent to all users!", Toast.LENGTH_LONG).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to load users: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
