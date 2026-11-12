package com.irv205.pokeapi.data.mock

import android.content.Intent
import com.irv205.pokeapi.BuildConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Overrides de debug para automatizacion E2E. Nunca se aplican en release.
 *
 * Uso con adb:
 * ```
 * adb shell am start -n com.irv205.pokeapi/.MainActivity \
 *   --es api_environment mock \
 *   --es mock_config_json "$(cat mocks.json)"
 * ```
 */
object DebugMockOverrides {

    const val EXTRA_ENVIRONMENT = "api_environment"
    const val EXTRA_MOCK_CONFIG_JSON = "mock_config_json"
    const val EXTRA_MOCK_CONFIG_JSON_BASE64 = "mock_config_json_base64"
    const val EXTRA_FALLBACK_TO_NETWORK = "mock_fallback_to_network"

    private val json = Json { ignoreUnknownKeys = true }

    fun applyFromIntent(
        intent: Intent?,
        environment: MutableEnvironmentProvider,
        config: InMemoryMockConfigProvider,
    ) {
        if (!BuildConfig.DEBUG || intent == null) return

        intent.getStringExtra(EXTRA_ENVIRONMENT)?.let { raw ->
            ApiEnvironment.entries
                .firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
                ?.let { environment.current = it }
        }

        val rawJson = intent.getStringExtra(EXTRA_MOCK_CONFIG_JSON)
            ?: intent.getStringExtra(EXTRA_MOCK_CONFIG_JSON_BASE64)?.let { encoded ->
                runCatching {
                    String(
                        android.util.Base64.decode(encoded, android.util.Base64.DEFAULT),
                        Charsets.UTF_8,
                    )
                }.getOrNull()
            }

        rawJson?.let { raw ->
            runCatching { json.decodeFromString<List<MockService>>(raw) }
                .onSuccess { services ->
                    if (services.isNotEmpty()) config.services = services
                }
        }

        if (intent.hasExtra(EXTRA_FALLBACK_TO_NETWORK)) {
            config.fallbackToNetwork = intent.getBooleanExtra(
                EXTRA_FALLBACK_TO_NETWORK,
                false,
            )
        }
    }
}
