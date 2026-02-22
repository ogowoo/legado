package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * Groq 提供商
 * 官网：https://console.groq.com/
 * 特点：速度极快，有免费额度（每月限制）
 */
class GroqProvider : AIProvider {
    
    override val name = "Groq"
    override val providerId = "groq"
    override val defaultApiUrl = "https://api.groq.com/openai/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key (gsk_...)"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "llama-3.1-8b-instant",
            "Llama 3.1 8B Instant",
            "超快响应，有免费额度",
            isFree = true,
            isRecommended = true
        ),
        AIModel(
            "llama-3.1-70b-versatile",
            "Llama 3.1 70B Versatile",
            "能力强，有免费额度",
            isFree = true
        ),
        AIModel(
            "llama3-8b-8192",
            "Llama 3 8B",
            "快速响应，有免费额度",
            isFree = true
        ),
        AIModel(
            "llama3-70b-8192",
            "Llama 3 70B",
            "能力强，有免费额度",
            isFree = true
        ),
        AIModel(
            "mixtral-8x7b-32768",
            "Mixtral 8x7B",
            "MoE架构，有免费额度",
            isFree = true
        ),
        AIModel(
            "gemma-7b-it",
            "Gemma 7B",
            "Google轻量模型，有免费额度",
            isFree = true
        ),
        AIModel(
            "gemma2-9b-it",
            "Gemma 2 9B",
            "Google新一代模型，有免费额度",
            isFree = true
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
            "max_tokens" to maxTokens,
            "stream" to false
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
