package io.legado.app.ai.repair

import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 自定义 API 提供商实现
 * 支持国内大模型 API（如 Moonshot、文心一言、通义千问等）
 * 以及任何兼容 OpenAI API 格式的自定义服务
 */
class CustomProvider : AIProvider {

    override val name: String = AIProviderType.CUSTOM.displayName

    override val defaultModel: String = ""

    override val availableModels: List<String> = emptyList()

    override val requireApiKey: Boolean = true

    override fun getDefaultApiUrl(): String = ""

    override fun validateConfig(config: RepairConfig): Boolean {
        return config.apiKey.isNotBlank() && config.apiUrl.isNotBlank() && config.model.isNotBlank()
    }

    override fun buildRequestBody(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): String {
        val systemPrompt = config.systemPrompt ?: OpenAIProvider.DEFAULT_SYSTEM_PROMPT
        val userContent = buildString {
            appendLine("前文上下文:")
            appendLine(previousContext.takeLast(config.contextLength.coerceIn(1000, 8000)))
            appendLine("---")
            appendLine("当前段落:")
            append(currentParagraph)
        }

        return when (detectApiFormat(config.apiUrl)) {
            ApiFormat.OPENAI_COMPATIBLE -> buildOpenAIFormat(systemPrompt, userContent, config)
            ApiFormat.QIANWEN -> buildQianwenFormat(systemPrompt, userContent, config)
            else -> buildOpenAIFormat(systemPrompt, userContent, config)
        }
    }

    override fun parseResponse(responseBody: String): String? {
        return try {
            parseOpenAIFormat(responseBody)
                ?: parseQianwenFormat(responseBody)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun repairText(
        previousContext: String,
        currentParagraph: String,
        config: RepairConfig
    ): Result<String> {
        return try {
            if (!validateConfig(config)) {
                return Result.failure(IllegalArgumentException("Invalid configuration"))
            }
            val requestBody = buildRequestBody(previousContext, currentParagraph, config)
            Result.success(requestBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildRequest(config: RepairConfig, requestBody: String): Request {
        val builder = Request.Builder()
            .url(config.apiUrl)
            .addHeader("Content-Type", "application/json")

        if (detectApiFormat(config.apiUrl) != ApiFormat.WENXIN) {
            builder.addHeader("Authorization", "Bearer ${config.apiKey}")
        }

        return builder
            .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
    }

    private fun detectApiFormat(apiUrl: String): ApiFormat {
        return when {
            apiUrl.contains("qianwen") || apiUrl.contains("dashscope") -> ApiFormat.QIANWEN
            apiUrl.contains("wenxin") || apiUrl.contains("baidu") -> ApiFormat.WENXIN
            else -> ApiFormat.OPENAI_COMPATIBLE
        }
    }

    private fun buildOpenAIFormat(systemPrompt: String, userContent: String, config: RepairConfig): String {
        return GSON.toJson(
            mapOf(
                "model" to config.model,
                "messages" to listOf(
                    mapOf("role" to "system", "content" to systemPrompt),
                    mapOf("role" to "user", "content" to userContent)
                ),
                "temperature" to config.temperature.coerceIn(0.0f, 2.0f),
                "max_tokens" to config.maxTokens.coerceIn(100, 2000)
            )
        )
    }

    private fun buildQianwenFormat(systemPrompt: String, userContent: String, config: RepairConfig): String {
        return GSON.toJson(
            mapOf(
                "model" to config.model,
                "input" to mapOf(
                    "messages" to listOf(
                        mapOf("role" to "system", "content" to systemPrompt),
                        mapOf("role" to "user", "content" to userContent)
                    )
                ),
                "parameters" to mapOf(
                    "temperature" to config.temperature.coerceIn(0.0f, 2.0f),
                    "max_tokens" to config.maxTokens.coerceIn(100, 2000)
                )
            )
        )
    }

    private fun parseOpenAIFormat(responseBody: String): String? {
        val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
        val choices = map?.get("choices") as? List<*>
        val first = choices?.firstOrNull() as? Map<*, *>
        val message = first?.get("message") as? Map<*, *>
        return message?.get("content") as? String
    }

    private fun parseQianwenFormat(responseBody: String): String? {
        val map = GSON.fromJsonObject<Map<String, Any?>>(responseBody).getOrNull()
        val output = map?.get("output") as? Map<*, *>
        val choices = output?.get("choices") as? List<*>
        val first = choices?.firstOrNull() as? Map<*, *>
        val message = first?.get("message") as? Map<*, *>
        return message?.get("content") as? String
    }

    enum class ApiFormat {
        OPENAI_COMPATIBLE,
        QIANWEN,
        WENXIN
    }

    companion object {
        val PRESET_CONFIGS = mapOf(
            "moonshot" to PresetConfig(
                name = "Moonshot (月之暗面)",
                apiUrl = "https://api.moonshot.cn/v1/chat/completions",
                model = "moonshot-v1-8k",
                models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
            ),
            "qianwen" to PresetConfig(
                name = "通义千问",
                apiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                model = "qwen-turbo",
                models = listOf("qwen-turbo", "qwen-plus", "qwen-max")
            ),
            "wenxin" to PresetConfig(
                name = "文心一言",
                apiUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions",
                model = "ernie-bot",
                models = listOf("ernie-bot", "ernie-bot-turbo", "ernie-bot-4")
            ),
            "zhipu" to PresetConfig(
                name = "智谱 AI (GLM)",
                apiUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
                model = "glm-4",
                models = listOf("glm-4", "glm-4-flash", "glm-3-turbo")
            )
        )
    }

    data class PresetConfig(
        val name: String,
        val apiUrl: String,
        val model: String,
        val models: List<String>
    )
}
