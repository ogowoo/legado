package io.legado.app.ai

import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ContentRepairService {

    // 默认系统提示词
    private const val DEFAULT_SYSTEM_PROMPT = "你是一个专业的文本校对助手，收到前文上下文和当前段落后，修复当前段落里出现的错乱、错字、错序，使其成为通顺、连贯的中文段落。只返回修正后的段落内容，不要带额外说明。"

    // 默认参数
    private const val DEFAULT_TEMPERATURE = 0.2
    private const val DEFAULT_MAX_TOKENS = 512
    private const val CONTEXT_LENGTH = 4000

    suspend fun repair(previousContext: String, paragraph: String): String =
        withContext(Dispatchers.IO) {
            // 检查功能是否启用
            if (!AppConfig.aiContentRepairEnabled) {
                return@withContext paragraph
            }

            // 获取当前配置的提供商
            val provider = AIProviderFactory.getProvider(AppConfig.aiRepairProvider)
                ?: return@withContext paragraph

            // 获取 API Key
            val apiKey = AppConfig.aiRepairApiKey
            if (provider.requireApiKey && apiKey.isNullOrBlank()) {
                return@withContext paragraph
            }

            // 获取 API URL（自定义或默认）
            val apiUrl = if (provider.supportCustomUrl && !AppConfig.aiRepairApiUrl.isNullOrBlank()) {
                AppConfig.aiRepairApiUrl
            } else {
                provider.defaultApiUrl
            }.takeIf { it.isNotBlank() } ?: return@withContext paragraph

            // 获取模型
            val model = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() }
                ?: provider.supportedModels.firstOrNull { it.isRecommended }?.id
                ?: provider.supportedModels.firstOrNull()?.id
                ?: return@withContext paragraph

            // 获取温度参数
            val temperature = AppConfig.aiRepairTemperature?.toDoubleOrNull() 
                ?: DEFAULT_TEMPERATURE

            // 获取最大 Token 数
            val maxTokens = AppConfig.aiRepairMaxTokens?.toIntOrNull() 
                ?: DEFAULT_MAX_TOKENS

            // 获取自定义系统提示词
            val systemPrompt = AppConfig.aiRepairSystemPrompt?.takeIf { it.isNotBlank() }
                ?: DEFAULT_SYSTEM_PROMPT

            try {
                // 构建请求内容
                val userContent = buildUserContent(previousContext, paragraph)

                // 构建请求体
                val payload = provider.buildRequestBody(
                    systemPrompt = systemPrompt,
                    userContent = userContent,
                    model = model,
                    temperature = temperature,
                    maxTokens = maxTokens
                )

                // 构建请求头
                val headers = if (apiKey != null) {
                    provider.buildHeaders(apiKey)
                } else {
                    mapOf("Content-Type" to "application/json")
                }

                // 构建请求
                val requestBuilder = Request.Builder()
                    .url(apiUrl)
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))

                // 添加请求头
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                // 发送请求
                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() 
                    ?: return@withContext paragraph

                // 解析响应
                val repairedText = provider.parseResponse(responseBody)
                
                return@withContext repairedText?.trim()?.takeIf { it.isNotBlank() } ?: paragraph

            } catch (e: Throwable) {
                e.printStackTrace()
                return@withContext paragraph
            }
        }

    /**
     * 构建用户提示内容
     */
    private fun buildUserContent(previousContext: String, paragraph: String): String {
        return buildString {
            appendLine("前文上下文:")
            appendLine(previousContext.takeLast(CONTEXT_LENGTH))
            appendLine("---")
            appendLine("当前段落:")
            append(paragraph)
        }
    }
}
