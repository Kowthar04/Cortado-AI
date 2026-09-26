package com.example.cafeshopassignment.ui.common

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Collects [flow] only while this owner is at least STARTED. Collection is cancelled in
 * onStop, which also cancels any upstream Firestore snapshot listener.
 */
fun <T> LifecycleOwner.collectWhileStarted(
    flow: Flow<T>,
    action: suspend (T) -> Unit,
) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

fun Context.toast(message: CharSequence) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.toast(
    @StringRes resId: Int,
    vararg args: Any,
) {
    toast(getString(resId, *args))
}

/** Formats an amount as pounds, e.g. "£3.50". */
fun formatPrice(amount: Double): String = String.format(Locale.UK, "£%.2f", amount)
