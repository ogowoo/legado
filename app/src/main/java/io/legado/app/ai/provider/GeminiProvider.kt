package io.legado.app.ai.provider

import io.legado.app.ai.AIProvider
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

class GeminiProvider : AIProvider {
    
    override val name: String = "Gemini"
    override val defaultModel: String = "gemini-pro"
    
    override fun getApiUrl(customApiUrl: String?): String {
        return customApiUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    }
    
    override fun getHeaders(apiKey: String): Map<String, String> {
        return mapOf(
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
                "contents" to listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to AIProvider.SYSTEM_PROMPT + "\n\n" + 
                                AIProvider.buildUserPrompt(previousContext, paragraph))
                        )
                    )
                ),
                "generationConfig" to mapOf(
                    "temperature" to temperature,
                    "maxOutputTokens" to maxTokens
                )
            )
        )
    }
    
    override fun parseResponse(responseBody: String): String? {
        return try {
            val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
            val candidates = map?.get("candidates") as? List<*>
            val first = candidates?.firstOrNull() as? Map<*, *>
            val content = first?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val firstPart = parts?.firstOrNull() as? Map<*, *>
            firstPart?.get("text") as? String
        } catch (e: Exception) {
            null
        }
    }
}
