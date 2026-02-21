package io.legado.app.ai

import io.legado.app.ai.provider.ClaudeProvider
import io.legado.app.ai.provider.CustomProvider
import io.legado.app.ai.provider.DeepSeekProvider
import io.legado.app.ai.provider.GeminiProvider
import io.legado.app.ai.provider.OpenAIProvider

/**
 * AI 提供商类型枚举
 */
enum class AIProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    GEMINI("Google Gemini"),
    CLAUDE("Anthropic Claude"),
    DEEPSEEK("DeepSeek"),
    CUSTOM("自定义 API");
    
    companion object {
        fun fromString(value: String): AIProviderType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OPENAI
        }
    }
}

/**
 * AI 提供商工厂
 */
object AIProviderFactory {
    
    fun create(providerType: AIProviderType): AIProvider {
        return when (providerType) {
            AIProviderType.OPENAI -> OpenAIProvider()
            AIProviderType.GEMINI -> GeminiProvider()
            AIProviderType.CLAUDE -> ClaudeProvider()
            AIProviderType.DEEPSEEK -> DeepSeekProvider()
            AIProviderType.CUSTOM -> CustomProvider()
        }
    }
    
    fun create(providerTypeString: String): AIProvider {
        return create(AIProviderType.fromString(providerTypeString))
    }
}
