package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * NVIDIA NIM 提供商
 * 官网：https://build.nvidia.com/
 * 特点：支持多种开源大模型，性能强劲
 */
class NvidiaProvider : AIProvider {
    
    override val name = "NVIDIA NIM"
    override val providerId = "nvidia"
    override val defaultApiUrl = "https://integrate.api.nvidia.com/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key (nvapi-...)"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "meta/llama-3.1-8b-instruct",
            "Llama 3.1 8B Instruct",
            "Meta开源模型，速度快，性价比高",
            isFree = false,
            isRecommended = true
        ),
        AIModel(
            "meta/llama-3.1-70b-instruct",
            "Llama 3.1 70B Instruct",
            "Meta开源大模型，能力强",
            isFree = false
        ),
        AIModel(
            "meta/llama-3.1-405b-instruct",
            "Llama 3.1 405B Instruct",
            "Meta开源超大模型，最强性能",
            isFree = false
        ),
        AIModel(
            "mistralai/mixtral-8x7b-instruct-v0.1",
            "Mixtral 8x7B",
            "Mistral MoE模型，质量高",
            isFree = false
        ),
        AIModel(
            "mistralai/mixtral-8x22b-instruct-v0.1",
            "Mixtral 8x22B",
            "Mistral大MoE模型",
            isFree = false
        ),
        AIModel(
            "google/gemma-2-9b-it",
            "Gemma 2 9B",
            "Google轻量级模型"
        ),
        AIModel(
            "google/gemma-2-27b-it",
            "Gemma 2 27B",
            "Google大模型"
        ),
        AIModel(
            "microsoft/phi-3-mini-128k-instruct",
            "Phi-3 Mini",
            "微软轻量级模型，适合简单任务"
        ),
        AIModel(
            "microsoft/phi-3-medium-128k-instruct",
            "Phi-3 Medium",
            "微软中型模型"
        ),
        AIModel(
            "qwen/qwen2-7b-instruct",
            "Qwen2 7B",
            "阿里通义千问，中文能力强"
        ),
        AIModel(
            "qwen/qwen2-72b-instruct",
            "Qwen2 72B",
            "阿里通义千问大模型"
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
