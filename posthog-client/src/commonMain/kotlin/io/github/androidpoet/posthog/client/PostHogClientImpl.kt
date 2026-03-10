package io.github.androidpoet.posthog.client

import io.github.androidpoet.posthog.client.transport.HttpTransport
import io.github.androidpoet.posthog.core.models.AliasRequest
import io.github.androidpoet.posthog.core.models.BatchRequest
import io.github.androidpoet.posthog.core.models.CaptureRequest
import io.github.androidpoet.posthog.core.models.GroupRequest
import io.github.androidpoet.posthog.core.models.IdentifyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import io.github.androidpoet.posthog.core.result.PostHogResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.random.Random

/**
 * Default [PostHogClient] implementation that queues events in memory
 * and flushes them as batches to the PostHog `/batch` endpoint.
 */
internal class PostHogClientImpl(
    override val apiKey: String,
    override val config: PostHogConfig,
    private val transport: HttpTransport,
) : PostHogClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val queue = mutableListOf<CaptureRequest>()
    private val superProperties = mutableMapOf<String, JsonElement>()

    private var _distinctId: String = generateUUID()
    override val distinctId: String get() = _distinctId

    private var _optedOut: Boolean = config.optOut
    override val isOptedOut: Boolean get() = _optedOut

    private var flushJob: Job? = null

    init {
        startAutoFlush()
    }

    // ── Event capture ───────────────────────────────────────────────────

    override suspend fun capture(event: String, properties: JsonObject?) {
        if (_optedOut) return
        val merged = mergeProperties(properties)
        enqueue(
            CaptureRequest(
                apiKey = apiKey,
                event = event,
                distinctId = _distinctId,
                properties = merged,
                timestamp = currentTimestamp(),
            ),
        )
    }

    override suspend fun capture(event: String, properties: PropertiesBuilder.() -> Unit) {
        capture(event, PropertiesBuilder().apply(properties).build())
    }

    // ── User identification ─────────────────────────────────────────────

    override suspend fun identify(
        distinctId: String,
        userProperties: JsonObject?,
        userPropertiesSetOnce: JsonObject?,
    ) {
        if (_optedOut) return
        _distinctId = distinctId

        val request = IdentifyRequest(
            apiKey = apiKey,
            distinctId = distinctId,
            properties = mergeProperties(null),
            set = userProperties,
            setOnce = userPropertiesSetOnce,
        )
        val body = json.encodeToString(request)
        transport.post(url = "${config.host}/capture/", body = body)
    }

    // ── Alias ───────────────────────────────────────────────────────────

    override suspend fun alias(alias: String) {
        if (_optedOut) return
        val props = buildJsonObject {
            put("distinct_id", JsonPrimitive(_distinctId))
            put("alias", JsonPrimitive(alias))
        }
        val request = AliasRequest(
            apiKey = apiKey,
            distinctId = _distinctId,
            properties = props,
        )
        val body = json.encodeToString(request)
        transport.post(url = "${config.host}/capture/", body = body)
    }

    // ── Group ───────────────────────────────────────────────────────────

    override suspend fun group(
        groupType: String,
        groupKey: String,
        groupProperties: JsonObject?,
    ) {
        if (_optedOut) return
        val props = buildJsonObject {
            put("\$group_type", JsonPrimitive(groupType))
            put("\$group_key", JsonPrimitive(groupKey))
            groupProperties?.let {
                put("\$group_set", it)
            }
        }
        val request = GroupRequest(
            apiKey = apiKey,
            distinctId = _distinctId,
            properties = props,
        )
        val body = json.encodeToString(request)
        transport.post(url = "${config.host}/capture/", body = body)
    }

    // ── Super properties ────────────────────────────────────────────────

    override fun register(key: String, value: JsonElement) {
        superProperties[key] = value
    }

    override fun unregister(key: String) {
        superProperties.remove(key)
    }

    // ── Opt in/out ──────────────────────────────────────────────────────

    override fun optIn() {
        _optedOut = false
    }

    override fun optOut() {
        _optedOut = true
    }

    // ── Queue management ────────────────────────────────────────────────

    override suspend fun flush() {
        val batch = mutex.withLock {
            if (queue.isEmpty()) return
            val snapshot = queue.toList()
            queue.clear()
            snapshot
        }
        val request = BatchRequest(apiKey = apiKey, batch = batch)
        val body = json.encodeToString(request)
        transport.post(url = "${config.host}/batch/", body = body)
    }

    override fun reset() {
        _distinctId = generateUUID()
        superProperties.clear()
        queue.clear()
    }

    // ── Raw HTTP ──────────────────────────────────────────────────────

    override suspend fun post(url: String, body: String): PostHogResult<String> =
        transport.post(url = url, body = body)

    // ── Lifecycle ───────────────────────────────────────────────────────

    override fun close() {
        flushJob?.cancel()
        scope.launch { flush() }.invokeOnCompletion {
            transport.close()
            scope.cancel()
        }
    }

    // ── Internals ───────────────────────────────────────────────────────

    private suspend fun enqueue(event: CaptureRequest) {
        val shouldFlush = mutex.withLock {
            if (queue.size >= config.maxQueueSize) {
                queue.removeAt(0)
            }
            queue.add(event)
            queue.size >= config.flushAt
        }
        if (shouldFlush) {
            flush()
        }
    }

    private fun mergeProperties(custom: JsonObject?): JsonObject = buildJsonObject {
        superProperties.forEach { (k, v) -> put(k, v) }
        custom?.forEach { (k, v) -> put(k, v) }
    }

    private fun startAutoFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(config.flushIntervalMs)
                flush()
            }
        }
    }

    private fun currentTimestamp(): String {
        // ISO-8601 UTC timestamp via kotlinx-datetime would be ideal,
        // but to avoid an extra dependency we use epoch millis as a string.
        // Consumers can override via properties if needed.
        return kotlinx.datetime.Clock.System.now().toString()
    }
}

/**
 * Generates a RFC 4122 version 4 UUID using [Random].
 */
private fun generateUUID(): String {
    val bytes = Random.nextBytes(16)
    bytes[6] = (bytes[6].toInt() and 0x0f or 0x40).toByte()
    bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
    return buildString {
        bytes.forEachIndexed { i, b ->
            append(b.toUByte().toString(16).padStart(2, '0'))
            if (i == 3 || i == 5 || i == 7 || i == 9) append('-')
        }
    }
}
