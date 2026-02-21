package io.legado.app.ai.provider

import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

class OpenAIProvider : AIProvider {
    
    override val name: String = "OpenAI"
    override val defaultModel: String = "gpt-3.5-turbo"
    
    override fun getApiUrl(customApiUrl: String?): String {
        return customApiUrl?.trim()?.takeIf { it.isNotEmpty() } 
            ?: "https://api.openai.com/v1/chat/completions"
    }
    
    override fun getHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
    }
    
    override fun buildRequestBody(
        previousContext: String,
        paragraph: String,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        return GSON.toJson(
            mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to AIProvider.SYSTEM_PROMPT),
                    mapOf(
                        "role" to "user",
                        "content" to AIProvider.buildUserPrompt(previousContext, paragraph)
                    )
                ),
                "temperature" to temperature,
                "max_tokens" to maxTokens
            )
        )
    }
    
    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
            val choices = map?.get("choices") as? List<*>
            val first = choices?.firstOrNull() as? Map<*, *>
            val message = first?.get("message") as? Map<*, *>
            message?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }
}
