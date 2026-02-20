package io.legado.app.ai.repair

import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OpenAI API 提供商实现
 * 支持 OpenAI 及兼容 OpenAI API 格式的服务（如 DeepSeek、Moonshot 等）
 */
class OpenAIProvider(
    private val providerType: AIProviderType = AIProviderType.OPENAI
) : AIProvider {

    override val name: String = providerType.displayName

    override val defaultModel: String = providerType.defaultModel

    override val availableModels: List<String> = when (providerType) {
        AIProviderType.OPENAI -> listOf(
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",
            "gpt-4",
            "gpt-4-turbo",
            "gpt-4o",
            "gpt-4o-mini"
        )
        AIProviderType.DEEPSEEK -> listOf(
            "deepseek-chat",
            "deepseek-coder"
        )
        else -> listOf(defaultModel)
    }

    override val requireApiKey: Boolean = providerType.requireApiKey

    override fun getDefaultApiUrl(): String = providerType.defaultApiUrl

    override fun validateConfig(config: RepairConfig): Boolean {
        return config.apiKey.isNotBlank() && 
               (config.apiUrl.isNotBlank() || providerType.defaultApiUrl.isNotBlank())
    }

    override fun buildRequestBody(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): String {
        val systemPrompt = config.systemPrompt ?: DEFAULT_SYSTEM_PROMPT
        val userContent = buildUserContent(previousContext, currentParagraph, config)

        return GSON.toJson(
            mapOf(
                "model" to (config.model.takeIf { it.isNotBlank() } ?: defaultModel),
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userContent)
                ),
                "temperature" to config.temperature.coerceIn(0.0f, 2.0f),
                "max_tokens" to config.maxTokens.coerceIn(100, 2000)
            )
        )
    }

    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
            val choices = map?.get("choices") as? List<*>
            val first = choices?.firstOrNull() as? Map<*, *>
            val message = first?.get("message") as? Map<*, *>
            message?.get("content") as? String
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
                return Result.failure(IllegalArgumentException("Invalid configuration: API key or URL is missing"))
            }

            val requestBody = buildRequestBody(previousContext, currentParagraph, config)
            val apiUrl = config.apiUrl.takeIf { it.isNotBlank() } ?: getDefaultApiUrl()

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer ${config.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                .build()

            Result.success(requestBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildRequest(config: RepairConfig, requestBody: String): Request {
        val apiUrl = config.apiUrl.takeIf { it.isNotBlank() } ?: getDefaultApiUrl()
        return Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }

    private fun buildUserContent(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): String {
        val contextLength = config.contextLength.coerceIn(1000, 8000)
        return buildString {
            appendLine("前文上下文:")
            appendLine(previousContext.takeLast(contextLength))
            appendLine("---")
            appendLine("当前段落:")
            append(currentParagraph)
        }
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """你是一个专业的文本校对助手。你的任务是：
1. 根据前文上下文，修复当前段落中的错乱、错字、错序问题
2. 保持原文的语气和风格
3. 只返回修正后的段落内容，不要添加任何解释或说明
4. 如果文本没有问题，直接返回原文"""
    }
}
