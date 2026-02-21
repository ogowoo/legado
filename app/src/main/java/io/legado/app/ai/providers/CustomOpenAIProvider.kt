package io.legado.app.ai.providers

import io.legado.app.ai.AIModel
import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON

/**
 * 自定义 OpenAI 兼容接口提供商
 * 用于支持各类第三方代理或自托管模型（如 Ollama、vLLM 等）
 */
class CustomOpenAIProvider : AIProvider {
    
    override val name = "自定义 OpenAI 兼容"
    override val providerId = "custom_openai"
    override val defaultApiUrl = ""
    override val requireApiKey = true
    override val apiKeyHint = "API Key（如不需要请填任意值）"
    override val supportCustomUrl = true
    
    // 自定义模型，用户可以输入任意模型名称
    override val supportedModels = listOf(
        AIModel("custom", "自定义模型", "请在下方输入模型名称", isRecommended = true)
    )
    
    override fun buildRequestBody(
        systemPrompt: String,
        userContent: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        return GSON.toJson(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userContent)
            ),
            "temperature" to temperature,
            "max_tokens" to maxTokens
        ))
    }
    
    override fun buildHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
    }
    
    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJson(responseBody, Map::class.java)
            val choices = map["choices"] as? List<*>
            val first = choices?.firstOrNull() as? Map<*, *>
            val message = first?.get("message") as? Map<*, *>
            message?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }
}
