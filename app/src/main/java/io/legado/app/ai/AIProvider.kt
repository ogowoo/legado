package io.legado.app.ai

/**
 * AI 提供商接口
 */
interface AIProvider {
    
    /**
     * 提供商名称
     */
    val name: String
    
    /**
     * 默认模型
     */
    val defaultModel: String
    
    /**
     * 构建请求体
     */
    fun buildRequestBody(
        previousContext: String,
        paragraph: String,
        model: String,
        temperature: Float,
        maxTokens: Int
    ): String
    
    /**
     * 解析响应
     */
    fun parseResponse(responseBody: String): String?
    
    /**
     * 获取 API URL
     */
    fun getApiUrl(customApiUrl: String?): String
    
    /**
     * 获取请求头
     */
    fun getHeaders(apiKey: String): Map<String, String>
    
    companion object {
        /**
         * 系统提示词
         */
        const val SYSTEM_PROMPT = """你是一个专业的文本校对助手。收到前文上下文和当前段落后，修复当前段落里出现的错乱、错字、错序，使其成为通顺、连贯的中文段落。只返回修正后的段落内容，不要带额外说明。"""

        /**
         * 构建用户提示词
         */
        fun buildUserPrompt(previousContext: String, paragraph: String, contextLength: Int = 4000): String {
            return """前文上下文:
${previousContext.takeLast(contextLength)}
---
当前段落:
$paragraph"""
        }
    }
}
