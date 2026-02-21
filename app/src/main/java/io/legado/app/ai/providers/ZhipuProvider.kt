package io.legado.app.ai.providers

import io.legado.app.ai.AIModel
import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON

/**
 * 智谱 AI（GLM）提供商
 * 官网：https://open.bigmodel.cn/
 * 免费额度：新用户有 1000 万 Token 免费额度
 */
class ZhipuProvider : AIProvider {
    
    override val name = "智谱 AI (GLM)"
    override val providerId = "zhipu"
    override val defaultApiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "glm-4-flash", 
            "GLM-4 Flash", 
            "免费模型，速度快",
            isFree = true,
            isRecommended = true
        ),
        AIModel(
            "glm-4-plus", 
            "GLM-4 Plus", 
            "旗舰模型，能力强",
            isFree = true
        ),
        AIModel(
            "glm-4-air", 
            "GLM-4 Air", 
            "高性价比"
        ),
        AIModel(
            "glm-4-long", 
            "GLM-4 Long", 
            "长文本专用"
        )
    )
    
    override fun buildRequestBody(
        systemPrompt: String,
        userContent: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): String {
        // 智谱 AI 兼容 OpenAI 格式
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
