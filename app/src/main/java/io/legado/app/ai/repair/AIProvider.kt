package io.legado.app.ai.repair

/**
 * AI提供商接口
 * 定义了AI文本修复服务的基本操作
 */
interface AIProvider {

    /**
     * 提供商名称
     */
    val name: String

    /**
     * 默认模型名称
     */
    val defaultModel: String

    /**
     * 可用的模型列表
     */
    val availableModels: List<String>

    /**
     * 是否需要API Key
     */
    val requireApiKey: Boolean

    /**
     * 修复文本
     * 
     * @param previousContext 前文上下文
     * @param currentParagraph 当前需要修复的段落
     * @param config 修复配置
     * @return 修复后的文本
     */
    suspend fun repairText(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): Result<String>

    /**
     * 验证配置是否有效
     * 
     * @param config 修复配置
     * @return 验证结果
     */
    fun validateConfig(config: RepairConfig): Boolean

    /**
     * 获取提供商的默认API地址
     */
    fun getDefaultApiUrl(): String

    /**
     * 构建请求体
     */
    fun buildRequestBody(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): String

    /**
     * 解析响应体
     */
    fun parseResponse(responseBody: String): String?
}

/**
 * 修复配置数据类
 */
data class RepairConfig(
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",
    val temperature: Float = 0.2f,
    val maxTokens: Int = 512,
    val contextLength: Int = 4000,
    val systemPrompt: String? = null,
    val timeoutMs: Long = 30000L,
    val retryCount: Int = 2,
    val retryDelayMs: Long = 1000L
)

/**
 * AI提供商类型枚举
 */
enum class AIProviderType(
    val displayName: String,
    val defaultModel: String,
    val defaultApiUrl: String,
    val requireApiKey: Boolean = true
) {
    OPENAI(
        displayName = "OpenAI",
        defaultModel = "gpt-3.5-turbo",
        defaultApiUrl = "https://api.openai.com/v1/chat/completions"
    ),
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-pro",
        defaultApiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    ),
    CLAUDE(
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-haiku-20240307",
        defaultApiUrl = "https://api.anthropic.com/v1/messages"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultModel = "deepseek-chat",
        defaultApiUrl = "https://api.deepseek.com/v1/chat/completions"
    ),
    CUSTOM(
        displayName = "自定义 API",
        defaultModel = "",
        defaultApiUrl = ""
    );

    companion object {
        fun fromName(name: String): AIProviderType {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: OPENAI
        }
    }
}

/**
 * 修复结果密封类
 */
sealed class RepairResult {
    data class Success(val repairedText: String) : RepairResult()
    data class Error(val exception: Throwable, val message: String) : RepairResult()
    data class Cached(val repairedText: String) : RepairResult()
}
