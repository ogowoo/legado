package io.legado.app.ai.provider

import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

class CustomProvider : AIProvider {
    
    override val name: String = "Custom"
    override val defaultModel: String = ""
    
    override fun getApiUrl(customApiUrl: String?): String {
        return customApiUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Custom API URL is required")
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
            if (choices != null) {
                val first = choices.firstOrNull() as? Map<*, *>
                val message = first?.get("message") as? Map<*, *>
                return message?.get("content") as? String
            }
            map?.get("content") as? String
        } catch (e: Exception) {
            null
        }
    }
}
