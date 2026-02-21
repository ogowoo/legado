package io.legado.app.ai

import io.legado.app.ai.providers.BaiduProvider
import io.legado.app.ai.providers.CustomOpenAIProvider
import io.legado.app.ai.providers.DashScopeProvider
import io.legado.app.ai.providers.DeepSeekProvider
import io.legado.app.ai.providers.NvidiaProvider
import io.legado.app.ai.providers.OpenAIProvider
import io.legado.app.ai.providers.ZhipuProvider

/**
 * AI 提供商工厂
 */
object AIProviderFactory {
    
    private val providers = listOf(
        OpenAIProvider(),
        DashScopeProvider(),      // 阿里云通义千问
        DeepSeekProvider(),       // DeepSeek
        ZhipuProvider(),          // 智谱 AI
        BaiduProvider(),          // 百度千帆
        NvidiaProvider(),         // NVIDIA NIM
        CustomOpenAIProvider()    // 自定义 OpenAI 兼容
    )
    
    /**
     * 获取所有可用的提供商
     */
    fun getAllProviders(): List<AIProvider> = providers
    
    /**
     * 根据 providerId 获取提供商
     */
    fun getProvider(providerId: String): AIProvider? {
        return providers.find { it.providerId == providerId }
    }
    
    /**
     * 获取默认提供商（OpenAI）
     */
    fun getDefaultProvider(): AIProvider = providers.first()
    
    /**
     * 获取推荐且有免费额度的模型列表
     * 用于默认推荐的提供商排序
     */
    fun getRecommendedProviders(): List<AIProvider> {
        return providers.sortedWith(compareByDescending<AIProvider> { provider ->
            // 优先推荐有免费模型的国内提供商
            provider.supportedModels.any { it.isFree }
        }.thenBy {
            // 然后按名称排序
            it.name
        })
    }
}
