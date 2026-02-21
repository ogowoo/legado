package io.legado.app.ai.providers

import io.legado.app.ai.AIModel
import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON

/**
 * DeepSeek 提供商
 * 官网：https://platform.deepseek.com/
 * 特点：价格便宜，中文表现好
 */
class DeepSeekProvider : AIProvider {
    
    override val name = "DeepSeek"
    override val providerId = "deepseek"
    override val defaultApiUrl = "https://api.deepseek.com/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "deepseek-chat", 
            "DeepSeek V3", 
            "通用对话模型，中文能力强，价格极低",
            isFree = false,
            isRecommended = true
        ),
        AIModel(
            "deepseek-reasoner", 
            "DeepSeek R1", 
            "推理模型，适合复杂任务",
            isFree = false
        )
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
