package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * 百度千帆（文心一言）提供商
 * 官网：https://qianfan.baidu.com/
 * 免费额度：有免费额度赠送
 * 
 * 注意：百度 API 使用 AK/SK 鉴权，不是简单的 API Key
 */
class BaiduProvider : AIProvider {
    
    override val name = "百度千帆 (文心一言)"
    override val providerId = "baidu"
    override val defaultApiUrl = "https://qianfan.baidubce.com/v2/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "Access Token (需先获取)"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "ernie-speed-128k", 
            "ERNIE Speed 128K", 
            "轻量级模型，有免费额度",
            isFree = true,
            isRecommended = true
        ),
        AIModel(
            "ernie-lite-8k", 
            "ERNIE Lite 8K", 
            "轻量级模型，有免费额度",
            isFree = true
        ),
        AIModel(
            "ernie-4.0-turbo-8k", 
            "ERNIE 4.0 Turbo", 
            "旗舰模型"
        ),
        AIModel(
            "ernie-3.5-128k", 
            "ERNIE 3.5 128K", 
            "经典模型"
        )
    )
    
    override fun buildRequestBody(
        systemPrompt: String,
        userContent: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        // 百度兼容 OpenAI 格式
        return GSON.toJson(mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userContent)
            ),
            "temperature" to temperature,
            "max_output_tokens" to maxTokens
        ))
    }
    
    override fun buildHeaders(apiKey: String): Map<String, String> {
        // 百度使用 Bearer Token 鉴权
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
