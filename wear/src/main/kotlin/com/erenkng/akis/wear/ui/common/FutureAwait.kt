package com.erenkng.akis.wear.ui.common

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** `ListenableFuture` için küçük `await` (kotlinx-coroutines-guava bağımlılığı eklemeden). */
suspend fun <T> ListenableFuture<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            try {
                continuation.resume(get())
            } catch (e: ExecutionException) {
                continuation.resumeWithException(e.cause ?: e)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        },
        Runnable::run,
    )
    continuation.invokeOnCancellation { cancel(false) }
}
