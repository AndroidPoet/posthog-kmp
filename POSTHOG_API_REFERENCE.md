# PostHog API Reference for KMP SDK

> Compiled from PostHog Android SDK source (github.com/PostHog/posthog-android) and API documentation.
> Source of truth: the actual HTTP calls in `PostHogApi.kt` and the public interface in `PostHogInterface.kt`.

---

## 1. Base URLs & Authentication

### Hosts
| Region | Ingestion Host | Assets Host |
|--------|---------------|-------------|
| US (default) | `https://us.i.posthog.com` | `https://us-assets.i.posthog.com` |
| EU | `https://eu.i.posthog.com` | `https://eu-assets.i.posthog.com` |

### Authentication
- **Project API Key** (`apiKey`): Included in the JSON request body (NOT as a header). This is the public project key.
- **Personal API Key**: Used only for server-side local evaluation. Passed as `Authorization: Bearer <key>` header.
- **User-Agent**: `{sdkName}/{sdkVersion}` (e.g., `posthog-java/3.x.x`)
- **Content-Type**: `application/json; charset=utf-8`
- **Compression**: Gzip via `GzipRequestInterceptor` (request bodies are gzip-compressed)

---

## 2. HTTP Endpoints

### 2.1 POST `/batch` — Event Ingestion (Primary)

The main endpoint for sending events. All captured events (capture, identify, screen, alias, group, etc.) go through this endpoint.

**Request Body:**
```json
{
  "api_key": "phc_...",
  "batch": [
    {
      "event": "$pageview",
      "distinct_id": "user-123",
      "properties": {
        "$lib": "posthog-kotlin",
        "$lib_version": "1.0.0",
        "$os_name": "Android",
        "$os_version": "14",
        "$device_type": "Mobile",
        "$screen_name": "HomeScreen",
        "$set": { "email": "user@example.com" },
        "$set_once": { "first_seen": "2024-01-01" },
        "$groups": { "company": "acme-corp" },
        "$session_id": "uuid-v7",
        "$active_feature_flags": ["beta-feature"],
        "$feature/beta-feature": true,
        "$is_identified": true,
        "$process_person_profile": true
      },
      "timestamp": "2024-01-15T10:30:00.000Z",
      "uuid": "01234567-89ab-cdef-0123-456789abcdef"
    }
  ],
  "sent_at": "2024-01-15T10:30:01.000Z"
}
```

**Key Fields in Each Event:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `event` | String | Yes | Event name (e.g., `$identify`, `$screen`, `$set`, `$groupidentify`, `$create_alias`, `$feature_flag_called`, `$feature_view`, `$feature_interaction`, `$exception`, or custom) |
| `distinct_id` | String | Yes | Unique user identifier |
| `properties` | Map<String, Any> | No | All event properties (including special `$` prefixed ones) |
| `timestamp` | ISO 8601 Date | Yes | Auto-generated, UTC |
| `uuid` | UUID v7 | Yes | Auto-generated, used for deduplication |
| `api_key` | String | No | Only used for snapshot/replay events |

**Special Property Keys (set inside `properties`):**
| Property | Type | When Set |
|----------|------|----------|
| `$set` | Map<String, Any> | User properties to set (overwrite) |
| `$set_once` | Map<String, Any> | User properties to set only if not already set |
| `$groups` | Map<String, String> | Group memberships (type -> key) |
| `$session_id` | String (UUID) | Active session ID |
| `$window_id` | String (UUID) | Same as session_id (for replay) |
| `$is_identified` | Boolean | Whether user has been identified |
| `$process_person_profile` | Boolean | Whether to process person profile |
| `$lib` | String | SDK name |
| `$lib_version` | String | SDK version |
| `$os_name` | String | Operating system |
| `$os_version` | String | OS version |
| `$device_type` | String | Device type (Mobile, Tablet, TV, etc.) |
| `$screen_name` | String | Screen name (for `$screen` events) |
| `$active_feature_flags` | List<String> | Currently active feature flag keys |
| `$feature/{key}` | Any | Feature flag value per key |
| `$anon_distinct_id` | String | Previous anonymous ID (on `$identify`) |
| `$group_type` | String | Group type (on `$groupidentify`) |
| `$group_key` | String | Group key (on `$groupidentify`) |
| `$group_set` | Map<String, Any> | Group properties (on `$groupidentify`) |

**Response:** HTTP 200 on success. No meaningful response body.

**Error Handling:**
- `4xx`: Events are deleted (unrecoverable)
- `413`: Batch size is halved and retried
- `< 400` or network error: Retry with exponential backoff (5s increments, max 30s)

---

### 2.2 POST `/flags/?v=2` — Feature Flags Evaluation

Evaluates feature flags server-side for a given user.

**Request Body:**
```json
{
  "api_key": "phc_...",
  "distinct_id": "user-123",
  "timezone": "America/New_York",
  "$anon_distinct_id": "previous-anon-id",
  "groups": {
    "company": "acme-corp"
  },
  "person_properties": {
    "email": "user@example.com",
    "$app_version": "1.0.0",
    "$os_name": "Android"
  },
  "group_properties": {
    "company": {
      "plan": "enterprise"
    }
  },
  "evaluation_contexts": ["production", "mobile"]
}
```

**Request Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `api_key` | String | Yes | Project API key |
| `distinct_id` | String | Yes | User identifier |
| `timezone` | String | Yes | Auto-set from device timezone (IANA format) |
| `$anon_distinct_id` | String | No | Anonymous ID for linking |
| `groups` | Map<String, String> | No | Group memberships |
| `person_properties` | Map<String, Any?> | No | Properties for flag evaluation |
| `group_properties` | Map<String, Map<String, Any?>> | No | Group properties for evaluation |
| `evaluation_contexts` | List<String> | No | Tags to filter which flags to evaluate |

**Response Body (v1 — legacy):**
```json
{
  "errorsWhileComputingFlags": false,
  "featureFlags": {
    "beta-feature": true,
    "multivariate-test": "variant-a"
  },
  "featureFlagPayloads": {
    "beta-feature": "{\"discount\": 20}",
    "multivariate-test": "{\"color\": \"blue\"}"
  },
  "quotaLimited": [],
  "requestId": "req-abc-123",
  "evaluatedAt": 1705312200000
}
```

**Response Body (v4 — current, when `flags` field is present):**
```json
{
  "errorsWhileComputingFlags": false,
  "featureFlags": null,
  "featureFlagPayloads": null,
  "flags": {
    "beta-feature": {
      "key": "beta-feature",
      "enabled": true,
      "variant": null,
      "metadata": {
        "id": 42,
        "payload": "{\"discount\": 20}",
        "version": 3
      },
      "reason": {
        "code": "condition_match",
        "description": "Matched condition set 1",
        "condition_index": 0
      },
      "failed": false
    },
    "multivariate-test": {
      "key": "multivariate-test",
      "enabled": true,
      "variant": "variant-a",
      "metadata": {
        "id": 43,
        "payload": "{\"color\": \"blue\"}",
        "version": 1
      },
      "reason": null,
      "failed": false
    }
  },
  "sessionRecording": { ... },
  "surveys": [ ... ],
  "errorTracking": { ... },
  "capturePerformance": { ... },
  "quotaLimited": [],
  "requestId": "req-abc-123",
  "evaluatedAt": 1705312200000
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `errorsWhileComputingFlags` | Boolean | If true, some flags failed — SDK merges (upserts) instead of replacing |
| `featureFlags` | Map<String, Any>? | Flag key -> Boolean or String variant (v1 format) |
| `featureFlagPayloads` | Map<String, Any?>? | Flag key -> JSON string payload (v1 format) |
| `flags` | Map<String, FeatureFlag>? | v4 format with full flag details |
| `quotaLimited` | List<String>? | Features that are quota-limited (e.g., `["feature_flags"]`) |
| `requestId` | String? | Server request ID for debugging |
| `evaluatedAt` | Long? | Server timestamp of evaluation |
| `sessionRecording` | Any? | Session recording config (Boolean or Map) |
| `surveys` | Any? | Survey definitions |
| `errorTracking` | Any? | Error tracking config |
| `capturePerformance` | Any? | Performance capture config |

---

### 2.3 GET `/array/{apiKey}/config` — Remote Config

Fetches project-level remote configuration (session recording, surveys, feature flag existence check).

**Note:** Uses the **assets host** (e.g., `us-assets.i.posthog.com`) instead of the ingestion host.

**Request:** Simple GET with `User-Agent` and `Content-Type: application/json; charset=utf-8` headers.

**Response Body (inherits from `PostHogRemoteConfigResponse`):**
```json
{
  "sessionRecording": {
    "endpoint": "/s/",
    "linkedFlag": "recording-flag",
    "consoleLogRecordingEnabled": true,
    "sampleRate": "0.5"
  },
  "surveys": [
    { "id": "survey-1", "name": "NPS Survey", ... }
  ],
  "hasFeatureFlags": true,
  "errorTracking": {
    "autocaptureExceptions": true
  },
  "capturePerformance": {
    "network_timing": true
  }
}
```

`sessionRecording` can be `false` (disabled) or a Map with:
| Key | Type | Description |
|-----|------|-------------|
| `endpoint` | String | Snapshot ingestion endpoint (default `/s/`) |
| `linkedFlag` | String or Map | Feature flag that controls recording |
| `consoleLogRecordingEnabled` | Boolean | Whether to capture console logs |
| `sampleRate` | String/Number | 0.0-1.0, percentage of sessions to record |

---

### 2.4 POST `/s/` — Session Replay Snapshots

Used for session replay data. Events have `api_key` set on each event (not in a wrapper).

**Request Body:** JSON array of events (not wrapped in a batch object). No `sent_at`.
```json
[
  {
    "event": "$snapshot",
    "distinct_id": "user-123",
    "api_key": "phc_...",
    "properties": {
      "$session_id": "uuid",
      "$window_id": "uuid",
      "$snapshot_data": { ... }
    },
    "timestamp": "2024-01-15T10:30:00.000Z",
    "uuid": "..."
  }
]
```

---

### 2.5 GET `/api/feature_flag/local_evaluation/?token={apiKey}&send_cohorts` — Local Evaluation (Server-Side)

For server-side SDKs that evaluate feature flags locally without calling `/flags` per request.

**Headers:**
- `Authorization: Bearer {personalApiKey}`
- `If-None-Match: {etag}` (for conditional requests)

**Response:** Feature flag definitions for local evaluation. Returns `304 Not Modified` if unchanged.

---

## 3. Public SDK Methods to Implement

### 3.1 Core Lifecycle (`PostHogCoreInterface`)
```
setup(config)          — Initialize SDK with config
close()                — Shutdown SDK, stop queues
identify(distinctId, userProperties?, userPropertiesSetOnce?)  — Identify user
flush()                — Force flush event queue
optIn()                — Enable event capture
optOut()               — Disable event capture
isOptOut(): Boolean    — Check opt-out state
debug(enable)          — Toggle debug logging
```

### 3.2 Event Capture (`PostHogInterface`)
```
capture(event, distinctId?, properties?, userProperties?, userPropertiesSetOnce?, groups?, timestamp?)
captureException(throwable, properties?)
screen(screenTitle, properties?)
alias(alias)
captureFeatureView(flag, flagVariant?)
captureFeatureInteraction(flag, flagVariant?)
```

### 3.3 Feature Flags
```
reloadFeatureFlags(onFeatureFlags?)
isFeatureEnabled(key, defaultValue?, sendFeatureFlagEvent?): Boolean
getFeatureFlag(key, defaultValue?, sendFeatureFlagEvent?): Any?
getFeatureFlagPayload(key, defaultValue?): Any?
getFeatureFlagResult(key, sendFeatureFlagEvent?): FeatureFlagResult?
```

### 3.4 User & Group Management
```
group(type, key, groupProperties?)
register(key, value)           — Super properties (sent with every event)
unregister(key)                — Remove super property
distinctId(): String           — Get current distinct ID
reset()                        — Clear all cached state, new anonymous ID
setPersonProperties(userPropertiesToSet?, userPropertiesToSetOnce?)
```

### 3.5 Feature Flag Properties for Evaluation
```
setPersonPropertiesForFlags(userProperties, reloadFeatureFlags?)
resetPersonPropertiesForFlags(reloadFeatureFlags?)
setGroupPropertiesForFlags(type, groupProperties, reloadFeatureFlags?)
resetGroupPropertiesForFlags(type?, reloadFeatureFlags?)
```

### 3.6 Session Management
```
startSession()
endSession()
isSessionActive(): Boolean
getSessionId(): UUID?
```

---

## 4. Feature Flag Evaluation Logic

### Flow
1. On `setup()`: If `preloadFeatureFlags` is true (default), SDK calls remote config endpoint first
2. Remote config response has `hasFeatureFlags` — if true, SDK calls `/flags/?v=2`
3. Flags are cached in preferences (persisted across app launches)
4. On `identify()`: Flags are reloaded with the new `distinct_id` and `$anon_distinct_id`
5. On `reloadFeatureFlags()`: Manual reload triggered

### Response Processing (v4 with `flags` field)
- `flags` map is normalized: `featureFlags` = `flags.mapValues { it.variant ?: it.enabled }`
- `featureFlagPayloads` = `flags.mapValues { it.metadata.payload }`
- Payloads that are JSON strings are deserialized to their actual types

### Error Handling During Evaluation
- If `errorsWhileComputingFlags` is true:
  - v4: Filter out flags where `failed == true`
  - Merge (upsert) successful flags into cached flags (don't replace)
  - v1 (no `flags` field): Merge all returned flags into cache
- If `errorsWhileComputingFlags` is false: Replace all cached flags

### Quota Limiting
- If `quotaLimited` contains `"feature_flags"`, the response is ignored (cached values preserved)

### FeatureFlagResult Model
```
FeatureFlagResult(
  key: String,          // flag key
  enabled: Boolean,     // whether enabled
  variant: String?,     // variant name (null for boolean flags)
  payload: Any?         // deserialized payload
)
// value = variant ?: enabled  (convenience accessor)
```

### Feature Flag Called Event
- When `sendFeatureFlagEvent` is true (default from config), accessing a flag fires `$feature_flag_called`
- Properties: `$feature_flag` = key, `$feature_flag_response` = value
- Deduplicated via an LRU cache (default 1000 entries)

---

## 5. Event Batching Strategy

### Queue Architecture
- Events are serialized to individual files on disk (one file per event, named `{uuid}.event`)
- Files are stored in `{storagePrefix}/{apiKey}/` directory
- Separate queues for regular events (`/batch`) and replay events (`/s/`)

### Flush Triggers
| Trigger | Condition |
|---------|-----------|
| Threshold | Queue size >= `flushAt` (default: **20**) |
| Timer | Every `flushIntervalSeconds` (default: **30 seconds**) |
| Manual | `flush()` called explicitly |
| Fatal exception | Synchronous flush on fatal `$exception` events |

### Batch Limits
| Parameter | Default | Description |
|-----------|---------|-------------|
| `flushAt` | 20 | Minimum events before auto-flush |
| `maxBatchSize` | 50 | Maximum events per HTTP request |
| `maxQueueSize` | 1000 | Maximum events in queue (oldest dropped when exceeded) |
| `flushIntervalSeconds` | 30 | Timer-based flush interval |

### Retry Strategy
- On network error: Retry with backoff
- Backoff: `retryCount * 5 seconds`, capped at `30 seconds`
- On `413 Payload Too Large`: Halve `maxBatchSize` and `flushAt`, retry
- On `4xx` (other): Delete events (unrecoverable)
- On `< 400`: Retry (server issue)

### Request Format
- `sent_at` timestamp is set at send time (not capture time) to calculate clock drift
- Bodies are gzip-compressed
- Events preserve their original `timestamp` from capture time

---

## 6. User Identification Flow

### Identity Model
- **Anonymous ID**: UUID v7, generated on first access, stored in preferences
- **Distinct ID**: Defaults to anonymous ID until `identify()` is called
- **`$is_identified`**: Boolean flag, set to true after first `identify()` call

### `identify(distinctId, userProperties?, userPropertiesSetOnce?)` Flow
1. Validate `distinctId` is not blank
2. Check `personProfiles` mode allows identification
3. If `distinctId` differs from current AND user is not already identified:
   a. Set `isIdentified = true`
   b. Capture `$identify` event with `$anon_distinct_id` = previous anonymous ID
   c. Update `anonymousId` = previous distinct ID (for flag linking)
   d. Update `distinctId` = new distinct ID
   e. Set person properties for flags
   f. Reload feature flags (to re-evaluate with new identity)
4. If `distinctId` is same but properties changed: Capture `$set` event (deduplicated)
5. If `distinctId` is same and already identified: No-op

### `reset()` Flow
1. Generate new anonymous ID (UUID v7)
2. Clear distinct ID (falls back to new anonymous ID)
3. Set `isIdentified = false`
4. Clear all cached feature flags
5. Clear groups
6. End current session, start new session
7. Clear person processing state

### Person Profiles Mode (`PersonProfiles` enum)
| Mode | Behavior |
|------|----------|
| `ALWAYS` | Process person profiles for all events |
| `NEVER` | Never process person profiles (no merging on identify) |
| `IDENTIFIED_ONLY` (default) | Only process on `identify()`, `alias()`, `group()`, and events with explicit user properties |

---

## 7. Special Event Types

| Event Name | Constant | Trigger |
|------------|----------|---------|
| `$identify` | `IDENTIFY` | `identify()` with new distinct ID |
| `$set` | `SET` | `identify()` with same ID but new properties, or `setPersonProperties()` |
| `$screen` | `SCREEN` | `screen()` |
| `$groupidentify` | `GROUP_IDENTIFY` | `group()` |
| `$create_alias` | `CREATE_ALIAS` | `alias()` |
| `$feature_flag_called` | `FEATURE_FLAG_CALLED` | Feature flag accessed (auto) |
| `$feature_view` | `FEATURE_VIEW` | `captureFeatureView()` |
| `$feature_interaction` | `FEATURE_INTERACTION` | `captureFeatureInteraction()` |
| `$exception` | `EXCEPTION` | `captureException()` |
| `$snapshot` | `SNAPSHOT` | Session replay data |

---

## 8. Config Parameters Summary

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `apiKey` | String | — | Required. Project API key |
| `host` | String | `https://us.i.posthog.com` | API host |
| `debug` | Boolean | `false` | Enable debug logging |
| `optOut` | Boolean | `false` | Disable all capturing |
| `sendFeatureFlagEvent` | Boolean | `true` | Auto-send `$feature_flag_called` |
| `featureFlagCalledCacheSize` | Int | `1000` | LRU cache for dedup |
| `preloadFeatureFlags` | Boolean | `true` | Load flags on setup |
| `evaluationContexts` | List<String>? | `null` | Filter flags by context tags |
| `setDefaultPersonProperties` | Boolean | `true` | Auto-set device properties for flags |
| `flushAt` | Int | `20` | Events before auto-flush |
| `maxQueueSize` | Int | `1000` | Max queued events |
| `maxBatchSize` | Int | `50` | Max events per batch request |
| `flushIntervalSeconds` | Int | `30` | Timer flush interval |
| `personProfiles` | PersonProfiles | `IDENTIFIED_ONLY` | Person processing mode |
| `sessionReplay` | Boolean | `false` | Enable session replay |
| `reuseAnonymousId` | Boolean | `false` | Reuse anon ID across resets |
| `onFeatureFlags` | Callback? | `null` | Called when flags load |

---

## 9. Data Models for KMP Implementation

### PostHogEvent
```kotlin
data class PostHogEvent(
    val event: String,
    val distinctId: String,          // serialized as "distinct_id"
    val properties: MutableMap<String, Any>? = null,
    val timestamp: Instant = Clock.System.now(),
    val uuid: String = Uuid.random().toString(),  // UUID v7 preferred
)
```

### BatchRequest
```kotlin
data class BatchRequest(
    val apiKey: String,              // serialized as "api_key"
    val batch: List<PostHogEvent>,
    var sentAt: Instant? = null,     // serialized as "sent_at"
)
```

### FlagsRequest
```kotlin
data class FlagsRequest(
    val apiKey: String,              // "api_key"
    val distinctId: String,          // "distinct_id"
    val timezone: String,
    val anonDistinctId: String?,     // "$anon_distinct_id"
    val groups: Map<String, String>?,
    val personProperties: Map<String, Any?>?,   // "person_properties"
    val groupProperties: Map<String, Map<String, Any?>>?,  // "group_properties"
    val evaluationContexts: List<String>?,  // "evaluation_contexts"
)
```

### FlagsResponse
```kotlin
data class FlagsResponse(
    val errorsWhileComputingFlags: Boolean = false,
    val featureFlags: Map<String, Any>?,          // "featureFlags"
    val featureFlagPayloads: Map<String, Any?>?,  // "featureFlagPayloads"
    val flags: Map<String, FeatureFlag>?,
    val quotaLimited: List<String>?,
    val requestId: String?,
    val evaluatedAt: Long?,
    // Also extends remote config fields:
    val sessionRecording: Any? = false,
    val surveys: Any? = false,
    val hasFeatureFlags: Boolean? = false,
    val errorTracking: Any? = false,
    val capturePerformance: Any? = false,
)
```

### FeatureFlag (v4)
```kotlin
data class FeatureFlag(
    val key: String,
    val enabled: Boolean,
    val variant: String?,
    val metadata: FeatureFlagMetadata,
    val reason: EvaluationReason?,
    val failed: Boolean? = null,
)

data class FeatureFlagMetadata(
    val id: Int,
    val payload: String?,
    val version: Int,
)
```

### FeatureFlagResult (public API)
```kotlin
data class FeatureFlagResult(
    val key: String,
    val enabled: Boolean,
    val variant: String?,
    val payload: Any?,
) {
    val value: Any get() = variant ?: enabled
}
```

### PersonProfiles
```kotlin
enum class PersonProfiles {
    NEVER,
    ALWAYS,
    IDENTIFIED_ONLY,
}
```
