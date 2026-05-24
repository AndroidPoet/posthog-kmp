package io.github.androidpoet.posthog.core.result

import kotlinx.coroutines.CancellationException

/**
 * A discriminated result type for PostHog operations.
 *
 * Every SDK call that can fail returns [PostHogResult] instead of throwing,
 * giving callers full control over error handling via [map], [flatMap],
 * [recover], and friends.
 */
public sealed interface PostHogResult<out T> {

    public data class Success<out T>(public val value: T) : PostHogResult<T>

    public data class Failure(public val error: PostHogError) : PostHogResult<Nothing>

    public val isSuccess: Boolean get() = this is Success
    public val isFailure: Boolean get() = this is Failure

    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.toException()
    }

    public fun errorOrNull(): PostHogError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    public companion object {
        /**
         * Executes [block] and wraps the outcome in a [PostHogResult].
         *
         * Any [PostHogException] is unwrapped back to its [PostHogError].
         * All other exceptions are wrapped with their message.
         */
        public inline fun <T> catching(block: () -> T): PostHogResult<T> =
            try {
                Success(block())
            } catch (e: PostHogException) {
                Failure(e.error)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                Failure(PostHogError(message = e.message ?: "Unknown error"))
            }
    }
}

// ── Extension functions ─────────────────────────────────────────────────

/**
 * Transforms the success value, leaving failures untouched.
 */
public inline fun <T, R> PostHogResult<T>.map(
    transform: (T) -> R,
): PostHogResult<R> = when (this) {
    is PostHogResult.Success -> PostHogResult.Success(transform(value))
    is PostHogResult.Failure -> this
}

/**
 * Transforms the success value into another [PostHogResult], flattening
 * the nesting.
 */
public inline fun <T, R> PostHogResult<T>.flatMap(
    transform: (T) -> PostHogResult<R>,
): PostHogResult<R> = when (this) {
    is PostHogResult.Success -> transform(value)
    is PostHogResult.Failure -> this
}

/**
 * Invokes [action] when the result is a success, returning `this` for chaining.
 */
public inline fun <T> PostHogResult<T>.onSuccess(
    action: (T) -> Unit,
): PostHogResult<T> = apply {
    if (this is PostHogResult.Success) action(value)
}

/**
 * Invokes [action] when the result is a failure, returning `this` for chaining.
 */
public inline fun <T> PostHogResult<T>.onFailure(
    action: (PostHogError) -> Unit,
): PostHogResult<T> = apply {
    if (this is PostHogResult.Failure) action(error)
}

/**
 * Attempts to recover from a failure by producing a new success value.
 */
public inline fun <T> PostHogResult<T>.recover(
    transform: (PostHogError) -> T,
): PostHogResult<T> = when (this) {
    is PostHogResult.Success -> this
    is PostHogResult.Failure -> PostHogResult.Success(transform(error))
}

/**
 * Returns the success value or the result of [defaultValue] on failure.
 */
public inline fun <T> PostHogResult<T>.getOrElse(
    defaultValue: (PostHogError) -> T,
): T = when (this) {
    is PostHogResult.Success -> value
    is PostHogResult.Failure -> defaultValue(error)
}

public fun <T> PostHogResult<T>.toKotlinResult(): Result<T> = when (this) {
    is PostHogResult.Success -> Result.success(value)
    is PostHogResult.Failure -> Result.failure(error.toException())
}

public inline fun <T> Result<T>.toPostHogResult(
    mapThrowable: (Throwable) -> PostHogError = { throwable ->
        val postHogException = throwable as? PostHogException
        postHogException?.error ?: PostHogError(message = throwable.message ?: "Unknown error")
    },
): PostHogResult<T> = fold(
    onSuccess = { PostHogResult.Success(it) },
    onFailure = { throwable ->
        if (throwable is CancellationException) throw throwable
        PostHogResult.Failure(mapThrowable(throwable))
    },
)
