package com.example.cafeshopassignment.testutil

import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk

/**
 * Already-completed Play Services [Task]s. `kotlinx.coroutines.tasks.await()` returns (or
 * throws) immediately for completed tasks, so repositories can be tested without Firebase.
 */
fun <T> successfulTask(result: T): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { isSuccessful } returns true
        every { exception } returns null
        every { this@mockk.result } returns result
    }

fun <T> failedTask(error: Exception): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { isSuccessful } returns false
        every { exception } returns error
    }
