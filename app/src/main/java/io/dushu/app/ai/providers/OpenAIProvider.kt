package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * OpenAI 官方提供商
 */
class OpenAIProvider : AIProvider {
    
    override val name = "OpenAI"
    override val providerId = "openai"
    override val defaultApiUrl = "https://api.openai.com/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key (sk-...)"
    override val supportCustomUrl = true
    
    override val supportedModels = listOf(
        AIModel("gpt-3.5-turbo", "GPT-3.5 Turbo", "性价比高，适合文本校对", isRecommended = true),
        AIModel("gpt-4o-mini", "GPT-4o Mini", "更智能，价格适中"),
        AIModel("gpt-4o", "GPT-4o", "最智能，价格较高"),
        AIModel("gpt-4", "GPT-4", "经典大模型，能力强但价格高")
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
