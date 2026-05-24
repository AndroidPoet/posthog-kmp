package io.github.androidpoet.posthog.core

import io.github.androidpoet.posthog.core.result.PostHogError
import io.github.androidpoet.posthog.core.result.PostHogException
import io.github.androidpoet.posthog.core.result.PostHogResult
import io.github.androidpoet.posthog.core.result.flatMap
import io.github.androidpoet.posthog.core.result.getOrElse
import io.github.androidpoet.posthog.core.result.map
import io.github.androidpoet.posthog.core.result.onFailure
import io.github.androidpoet.posthog.core.result.onSuccess
import io.github.androidpoet.posthog.core.result.recover
import io.github.androidpoet.posthog.core.result.toKotlinResult
import io.github.androidpoet.posthog.core.result.toPostHogResult
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostHogResultTest {

    private val error = PostHogError(message = "not found", statusCode = 404)

    // ── Success ──────────────────────────────────────────────────────

    @Test
    fun test_success_holdsValue() {
        val result: PostHogResult<Int> = PostHogResult.Success(42)

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals(42, result.getOrNull())
        assertEquals(42, result.getOrThrow())
        assertNull(result.errorOrNull())
    }

    // ── Failure ──────────────────────────────────────────────────────

    @Test
    fun test_failure_holdsError() {
        val result: PostHogResult<Int> = PostHogResult.Failure(error)

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun test_failure_getOrThrowThrowsPostHogException() {
        val result: PostHogResult<Int> = PostHogResult.Failure(error)

        val exception = assertFailsWith<PostHogException> { result.getOrThrow() }
        assertEquals(error, exception.error)
    }

    // ── map ──────────────────────────────────────────────────────────

    @Test
    fun test_map_transformsSuccess() {
        val result = PostHogResult.Success(10).map { it * 2 }

        assertEquals(20, result.getOrNull())
    }

    @Test
    fun test_map_preservesFailure() {
        val result: PostHogResult<Int> = PostHogResult.Failure(error)
        val mapped = result.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertEquals(error, mapped.errorOrNull())
    }

    // ── flatMap ──────────────────────────────────────────────────────

    @Test
    fun test_flatMap_chainsSuccesses() {
        val result = PostHogResult.Success(5)
            .flatMap { PostHogResult.Success(it.toString()) }

        assertEquals("5", result.getOrNull())
    }

    @Test
    fun test_flatMap_shortCircuitsOnFailure() {
        val result: PostHogResult<Int> = PostHogResult.Failure(error)
        val chained = result.flatMap { PostHogResult.Success(it.toString()) }

        assertTrue(chained.isFailure)
    }

    // ── onSuccess / onFailure ────────────────────────────────────────

    @Test
    fun test_onSuccess_invokesForSuccess() {
        var captured = 0
        PostHogResult.Success(7).onSuccess { captured = it }

        assertEquals(7, captured)
    }

    @Test
    fun test_onFailure_invokesForFailure() {
        var captured: PostHogError? = null
        PostHogResult.Failure(error).onFailure { captured = it }

        assertEquals(error, captured)
    }

    // ── recover ──────────────────────────────────────────────────────

    @Test
    fun test_recover_convertsFailureToSuccess() {
        val result: PostHogResult<String> = PostHogResult.Failure(error)
            .recover { "default" }

        assertEquals("default", result.getOrNull())
    }

    @Test
    fun test_recover_leavesSuccessUntouched() {
        val result = PostHogResult.Success("original")
            .recover { "default" }

        assertEquals("original", result.getOrNull())
    }

    // ── getOrElse ────────────────────────────────────────────────────

    @Test
    fun test_getOrElse_returnsValueOnSuccess() {
        val value = PostHogResult.Success(99).getOrElse { -1 }

        assertEquals(99, value)
    }

    @Test
    fun test_getOrElse_returnsDefaultOnFailure() {
        val value: Int = PostHogResult.Failure(error).getOrElse { -1 }

        assertEquals(-1, value)
    }

    // ── catching ─────────────────────────────────────────────────────

    @Test
    fun test_catching_wrapsSuccessfulBlock() {
        val result = PostHogResult.catching { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun test_catching_wrapsPostHogException() {
        val result = PostHogResult.catching {
            throw PostHogException(error)
        }

        assertTrue(result.isFailure)
        assertEquals(error, result.errorOrNull())
    }

    @Test
    fun test_catching_wrapsGenericException() {
        val result = PostHogResult.catching<Int> {
            throw IllegalStateException("boom")
        }

        assertTrue(result.isFailure)
        assertEquals("boom", result.errorOrNull()?.message)
    }

    @Test
    fun test_catching_rethrowsCancellationException() {
        assertFailsWith<CancellationException> {
            PostHogResult.catching<Int> {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun test_toKotlinResult_successMapsToResultSuccess() {
        val kotlinResult = PostHogResult.Success(42).toKotlinResult()

        assertTrue(kotlinResult.isSuccess)
        assertEquals(42, kotlinResult.getOrNull())
    }

    @Test
    fun test_toKotlinResult_failureMapsToResultFailure() {
        val kotlinResult = PostHogResult.Failure(error).toKotlinResult()

        assertTrue(kotlinResult.isFailure)
        val exception = kotlinResult.exceptionOrNull()
        assertIs<PostHogException>(exception)
        assertEquals(error, exception.error)
    }

    @Test
    fun test_toPostHogResult_successMapsToPostHogSuccess() {
        val postHogResult = Result.success(7).toPostHogResult()

        assertTrue(postHogResult.isSuccess)
        assertEquals(7, postHogResult.getOrNull())
    }

    @Test
    fun test_toPostHogResult_failureMapsToPostHogFailure() {
        val postHogResult = Result.failure<Int>(IllegalStateException("boom")).toPostHogResult()

        assertTrue(postHogResult.isFailure)
        assertEquals("boom", postHogResult.errorOrNull()?.message)
    }

    @Test
    fun test_toPostHogResult_failureWithPostHogExceptionUnwrapsError() {
        val postHogResult = Result.failure<Int>(PostHogException(error)).toPostHogResult()

        assertTrue(postHogResult.isFailure)
        assertEquals(error, postHogResult.errorOrNull())
    }

    @Test
    fun test_toPostHogResult_rethrowsCancellationException() {
        assertFailsWith<CancellationException> {
            Result.failure<Int>(CancellationException("cancelled")).toPostHogResult()
        }
    }
}
