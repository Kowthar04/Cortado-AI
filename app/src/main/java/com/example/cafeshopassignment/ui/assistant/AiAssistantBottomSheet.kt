package com.example.cafeshopassignment.ui.assistant

import android.app.Dialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeshopassignment.R
import com.example.cafeshopassignment.ui.common.collectWhileStarted
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * "Ask AI" chat sheet: menu questions, recommendations, customisation help and order-status
 * queries, answered by the backend's Claude-powered /api/chat endpoint.
 */
class AiAssistantBottomSheet : BottomSheetDialogFragment() {
    // Activity-scoped so the conversation survives closing and reopening the sheet.
    private val viewModel: AiAssistantViewModel by activityViewModels { AiAssistantViewModel.Factory }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            // Keep the input row above the keyboard.
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_ai_assistant, container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        val messageList = view.findViewById<RecyclerView>(R.id.chatMessages)
        val emptyState = view.findViewById<View>(R.id.chatEmptyState)
        val input = view.findViewById<EditText>(R.id.chatInput)
        val sendButton = view.findViewById<ImageButton>(R.id.chatSendButton)
        val progress = view.findViewById<LinearProgressIndicator>(R.id.chatProgress)
        val typingText = view.findViewById<TextView>(R.id.chatTypingText)

        val chatAdapter = ChatAdapter(onRetry = viewModel::retry)
        messageList.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        messageList.adapter = chatAdapter

        fun submit() {
            val text = input.text.toString()
            if (text.isBlank() || viewModel.isLoading.value) return
            viewModel.send(text)
            input.text.clear()
        }

        sendButton.setOnClickListener { submit() }
        input.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnter) {
                submit()
                true
            } else {
                false
            }
        }
        input.doAfterTextChanged { sendButton.isEnabled = !it.isNullOrBlank() && !viewModel.isLoading.value }
        sendButton.isEnabled = false

        listOf(R.id.chipMenu, R.id.chipRecommend, R.id.chipCustomise, R.id.chipOrderStatus).forEach { chipId ->
            view.findViewById<Chip>(chipId).setOnClickListener { chip -> viewModel.send((chip as Chip).text.toString()) }
        }

        viewLifecycleOwner.collectWhileStarted(viewModel.messages) { messages ->
            emptyState.isVisible = messages.isEmpty()
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) messageList.scrollToPosition(messages.lastIndex)
            }
        }
        viewLifecycleOwner.collectWhileStarted(viewModel.isLoading) { loading ->
            progress.isVisible = loading
            typingText.isVisible = loading
            sendButton.isEnabled = !loading && !input.text.isNullOrBlank()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    companion object {
        const val TAG = "AiAssistantBottomSheet"

        /** Shows the sheet unless it is already showing (guards against double taps). */
        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                AiAssistantBottomSheet().show(fragmentManager, TAG)
            }
        }
    }
}
