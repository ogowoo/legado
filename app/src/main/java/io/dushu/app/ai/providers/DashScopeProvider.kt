package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * 阿里云 DashScope（通义千问）提供商
 * 官网：https://dashscope.aliyun.com/
 * 免费额度：新用户有 100 万 Token 免费额度
 */
class DashScopeProvider : AIProvider {
    
    override val name = "阿里云 DashScope (通义千问)"
    override val providerId = "dashscope"
    override val defaultApiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key (sk-...)"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "qwen-turbo", 
            "通义千问 Turbo", 
            "速度快，价格便宜，有免费额度",
            isFree = true,
            isRecommended = true
        ),
        AIModel(
            "qwen-plus", 
            "通义千问 Plus", 
            "综合性能均衡，有免费额度",
            isFree = true
        ),
        AIModel(
            "qwen-max", 
            "通义千问 Max", 
            "最强模型，能力强",
            isFree = true
        ),
        AIModel(
            "qwen-coder-plus", 
            "通义千问 Coder", 
            "代码专用"
        )
    )
    
    override fun buildRequestBody(
        systemPrompt: String,
        userContent: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        // DashScope 兼容 OpenAI 格式
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
