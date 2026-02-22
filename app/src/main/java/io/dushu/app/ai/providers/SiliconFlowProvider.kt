package io.dushu.app.ai.providers

import io.dushu.app.ai.AIModel
import io.dushu.app.ai.AIProvider
import io.dushu.app.utils.GSON

/**
 * SiliconFlow (硅基流动) 提供商
 * 官网：https://siliconflow.cn/
 * 特点：国内平台，多种开源模型，有免费额度
 */
class SiliconFlowProvider : AIProvider {
    
    override val name = "SiliconFlow (硅基流动)"
    override val providerId = "siliconflow"
    override val defaultApiUrl = "https://api.siliconflow.cn/v1/chat/completions"
    override val requireApiKey = true
    override val apiKeyHint = "API Key (sk-...)"
    override val supportCustomUrl = false
    
    override val supportedModels = listOf(
        AIModel(
            "Qwen/Qwen2-7B-Instruct",
            "Qwen2 7B",
            "阿里通义千问，有免费额度",
            isFree = true,
            isRecommended = true
        ),
        AIModel(
            "Qwen/Qwen2-72B-Instruct",
            "Qwen2 72B",
            "阿里通义千问大模型",
            isFree = true
        ),
        AIModel(
            "Qwen/Qwen2.5-7B-Instruct",
            "Qwen2.5 7B",
            "阿里通义千问最新版",
            isFree = true
        ),
        AIModel(
            "meta-llama/Meta-Llama-3.1-8B-Instruct",
            "Llama 3.1 8B",
            "Meta开源模型",
            isFree = true
        ),
        AIModel(
            "meta-llama/Meta-Llama-3.1-70B-Instruct",
            "Llama 3.1 70B",
            "Meta开源大模型",
            isFree = true
        ),
        AIModel(
            "deepseek-ai/DeepSeek-V2-Chat",
            "DeepSeek V2 Chat",
            "深度求索对话模型",
            isFree = true
        ),
        AIModel(
            "deepseek-ai/DeepSeek-Coder-V2-Instruct",
            "DeepSeek Coder V2",
            "深度求索代码模型",
            isFree = true
        ),
        AIModel(
            "THUDM/glm-4-9b-chat",
            "GLM-4 9B",
            "智谱AI最新模型"
        ),
        AIModel(
            "01-ai/Yi-1.5-9B-Chat-16K",
            "Yi 1.5 9B",
            "零一万物模型"
        ),
        AIModel(
            "01-ai/Yi-1.5-34B-Chat-16K",
            "Yi 1.5 34B",
            "零一万物大模型"
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
