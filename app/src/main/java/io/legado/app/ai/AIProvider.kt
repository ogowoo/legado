package io.legado.app.ai

/**
 * AI 提供商接口定义
 */
interface AIProvider {
    
    /**
     * 提供商名称
     */
    val name: String
    
    /**
     * 提供商 ID（用于配置存储）
     */
    val providerId: String
    
    /**
     * 默认 API 地址
     */
    val defaultApiUrl: String
    
    /**
     * 支持的模型列表
     */
    val supportedModels: List<AIModel>
    
    /**
     * 是否需要 API Key
     */
    val requireApiKey: Boolean
    
    /**
     * API Key 的名称提示（如 "API Key"、"AccessKey ID" 等）
     */
    val apiKeyHint: String
    
    /**
     * 是否支持自定义 API URL
     */
    val supportCustomUrl: Boolean
    
    /**
     * 构建请求体
     */
    fun buildRequestBody(
        systemPrompt: String,
        userContent: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): String
    
    /**
     * 构建请求头
     */
    fun buildHeaders(apiKey: String): Map<String, String>
    
    /**
     * 解析响应体，提取生成的文本
     */
    fun parseResponse(responseBody: String): String?
}

/**
 * AI 模型信息
 */
data class AIModel(
    val id: String,
    val displayName: String,
    val description: String = "",
    val isFree: Boolean = false,
    val isRecommended: Boolean = false
)
