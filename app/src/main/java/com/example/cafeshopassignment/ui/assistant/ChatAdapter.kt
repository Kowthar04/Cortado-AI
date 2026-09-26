package com.example.cafeshopassignment.ui.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.models.ChatError
import com.example.cafeshopassignment.models.ChatMessage

/**
 * Two view types: user bubbles (right-aligned) and assistant bubbles (left-aligned). Error
 * replies render as a tinted assistant bubble; tapping one retries the request.
 */
class ChatAdapter(
    private val onRetry: (ChatMessage) -> Unit,
) : ListAdapter<ChatMessage, ChatAdapter.BubbleViewHolder>(DIFF) {
    class BubbleViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.chatBubbleText)
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position).role) {
            ChatMessage.Role.USER -> TYPE_USER
            ChatMessage.Role.ASSISTANT -> TYPE_ASSISTANT
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BubbleViewHolder {
        val layout = if (viewType == TYPE_USER) R.layout.item_chat_user else R.layout.item_chat_assistant
        return BubbleViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(
        holder: BubbleViewHolder,
        position: Int,
    ) {
        val message = getItem(position)
        val error = message.error
        if (error == null) {
            holder.text.text = message.content
            holder.text.setBackgroundResource(
                if (message.role == ChatMessage.Role.USER) R.drawable.bg_chat_bubble_user else R.drawable.bg_chat_bubble_assistant,
            )
            holder.text.setOnClickListener(null)
            holder.text.isClickable = false
        } else {
            val context = holder.text.context
            holder.text.text = context.getString(R.string.chat_error_with_retry, context.getString(errorText(error)))
            holder.text.setBackgroundResource(R.drawable.bg_chat_bubble_error)
            holder.text.setOnClickListener { onRetry(message) }
        }
    }

    private fun errorText(error: ChatError): Int =
        when (error) {
            ChatError.Network -> R.string.chat_error_network
            ChatError.Unauthorized -> R.string.chat_error_unauthorized
            ChatError.RateLimited -> R.string.chat_error_rate_limited
            is ChatError.Server -> R.string.chat_error_server
            ChatError.EmptyReply -> R.string.chat_error_empty
            is ChatError.Unknown -> R.string.chat_error_unknown
        }

    private companion object {
        const val TYPE_USER = 0
        const val TYPE_ASSISTANT = 1

        val DIFF =
            object : DiffUtil.ItemCallback<ChatMessage>() {
                override fun areItemsTheSame(
                    oldItem: ChatMessage,
                    newItem: ChatMessage,
                ) = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: ChatMessage,
                    newItem: ChatMessage,
                ) = oldItem == newItem
            }
    }
}
