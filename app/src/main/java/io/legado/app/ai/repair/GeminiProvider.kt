package io.legado.app.ai.repair

import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Google Gemini API 提供商实现
 */
class GeminiProvider : AIProvider {

    override val name: String = AIProviderType.GEMINI.displayName

    override val defaultModel: String = AIProviderType.GEMINI.defaultModel

    override val availableModels: List<String> = listOf(
        "gemini-pro",
        "gemini-1.5-pro",
        "gemini-1.5-flash"
    )

    override val requireApiKey: Boolean = true

    override fun getDefaultApiUrl(): String = AIProviderType.GEMINI.defaultApiUrl

    override fun validateConfig(config: RepairConfig): Boolean {
        return config.apiKey.isNotBlank()
    }

    override fun buildRequestBody(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): String {
        val systemPrompt = config.systemPrompt ?: OpenAIProvider.DEFAULT_SYSTEM_PROMPT
        val contextLength = config.contextLength.coerceIn(1000, 8000)

        return GSON.toJson(
            mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to systemPrompt)
                        )
                    ),
                    mapOf(
                        "role" to "model",
                        "parts" to listOf(
                            mapOf("text" to "我明白了，我会根据前文上下文修复当前段落中的错乱、错字、错序问题，只返回修正后的内容。")
                        )
                    ),
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf(
                                "text" to buildString {
                                    appendLine("前文上下文:")
                                    appendLine(previousContext.takeLast(contextLength))
                                    appendLine("---")
                                    appendLine("当前段落:")
                                    append(currentParagraph)
                                }
                            )
                        )
                    )
                ),
                "generationConfig" to mapOf(
                    "temperature" to config.temperature.coerceIn(0.0f, 2.0f),
                    "maxOutputTokens" to config.maxTokens.coerceIn(100, 2000)
                )
            )
        )
    }

    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
            val candidates = map?.get("candidates") as? List<*>
            val first = candidates?.firstOrNull() as? Map<*, *>
            val content = first?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            firstPart?.get("text") as? String
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun repairText(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): Result<String> {
        return try {
            if (!validateConfig(config)) {
                return Result.failure(IllegalArgumentException("Invalid configuration: API key is missing"))
            }

            val requestBody = buildRequestBody(previousContext, currentParagraph, config)
            Result.success(requestBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildRequest(config: RepairConfig, requestBody: String): Request {
        val model = config.model.takeIf { it.isNotBlank() } ?: defaultModel
        val baseUrl = config.apiUrl.takeIf { it.isNotBlank() } ?: getDefaultApiUrl()
        val apiUrl = baseUrl.replace("gemini-pro", model)

        return Request.Builder()
            .url("$apiUrl?key=${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }
}
