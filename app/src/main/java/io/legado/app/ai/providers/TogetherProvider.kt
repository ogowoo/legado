package io.legado.app.ai.providers

import io.legado.app.ai.AIModel
import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON

/**
 * Together AI 提供商
 * 官网：https://api.together.xyz/
 * 特点：开源模型集合，有免费额度
 */
class TogetherProvider : AIProvider {
    
    override val name = "Together AI"
    override val providerId = "together"
    override val defaultApiUrl = "https://api.together.xyz/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "meta-llama/Meta-Llama-3.1-8B-Instruct-Turbo",
            "Llama 3.1 8B Turbo",
            "快速推理，性价比高",
            isFree = false,
            isRecommended = true
        ),
        AIModel(
            "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo",
            "Llama 3.1 70B Turbo",
            "能力强",
            isFree = false
        ),
        AIModel(
            "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo",
            "Llama 3.1 405B",
            "超大模型",
            isFree = false
        ),
        AIModel(
            "mistralai/Mixtral-8x7B-Instruct-v0.1",
            "Mixtral 8x7B",
            "MoE架构"
        ),
        AIModel(
            "mistralai/Mixtral-8x22B-Instruct-v0.1",
            "Mixtral 8x22B",
            "大MoE模型"
        ),
        AIModel(
            "google/gemma-2-9b-it",
            "Gemma 2 9B",
            "Google轻量模型"
        ),
        AIModel(
            "google/gemma-2-27b-it",
            "Gemma 2 27B",
            "Google大模型"
        ),
        AIModel(
            "Qwen/Qwen2-7B-Instruct",
            "Qwen2 7B",
            "阿里通义千问，中文好"
        ),
        AIModel(
            "Qwen/Qwen2-72B-Instruct",
            "Qwen2 72B",
            "阿里通义千问大模型"
        ),
        AIModel(
            "deepseek-ai/deepseek-coder-33b-instruct",
            "DeepSeek Coder 33B",
            "代码能力强"
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
