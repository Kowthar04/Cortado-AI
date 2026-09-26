package com.example.cafeshopassignment.util

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] but rethrows [CancellationException], so wrapping a suspend call never
 * swallows coroutine cancellation (e.g. when a ViewModel is cleared mid-request).
 */
suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
