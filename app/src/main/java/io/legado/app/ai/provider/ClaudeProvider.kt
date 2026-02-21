package io.legado.app.ai.provider

import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

class ClaudeProvider : AIProvider {
    
    override val name: String = "Claude"
    override val defaultModel: String = "claude-3-haiku-20240307"
    
    override fun getApiUrl(customApiUrl: String?): String {
        return customApiUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: "https://api.anthropic.com/v1/messages"
    }
    
    override fun getHeaders(apiKey: String): Map<String, String> {
        return mapOf(
            "x-api-key" to apiKey,
            "Content-Type" to "application/json",
            "anthropic-version" to "2023-06-01"
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
                "max_tokens" to maxTokens,
                "temperature" to temperature,
                "system" to AIProvider.SYSTEM_PROMPT,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to AIProvider.buildUserPrompt(previousContext, paragraph)
                    )
                )
            )
        )
    }
    
    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
            val content = map?.get("content") as? List<*>
            val first = content?.firstOrNull() as? Map<*, *>
            first?.get("text") as? String
        } catch (e: Exception) {
            null
        }
    }
}
