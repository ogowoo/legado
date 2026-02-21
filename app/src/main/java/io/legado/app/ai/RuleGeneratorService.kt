package io.legado.app.ai

import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import io.legado.app.help.http.okHttpClient

/**
 * AI规则生成服务
 * 分析原文和修正后的差异，自动生成替换净化规则
 */
object RuleGeneratorService {

    // 默认系统提示词 - 用于生成替换规则
    private const val DEFAULT_RULE_GENERATOR_PROMPT = """你是一个专业的文本替换规则生成助手。

任务：分析原文和修正后的文本差异，生成通用的正则表达式替换规则。

输入格式：
原文：[原始文本]
修正后：[修正后的文本]

输出要求：
1. 只返回JSON格式的规则数组，不要任何解释
2. 每个规则包含：name(规则名称), pattern(正则表达式), replacement(替换为)
3. 规则应该通用，能应用到类似的问题文本
4. 如果无法生成有效规则，返回空数组 []

示例输出格式：
[
  {
    "name": "过滤-本章未完提示",
    "pattern": "【.*?继续阅读.*?】",
    "replacement": ""
  },
  {
    "name": "修正-OCR错误",
    "pattern": "攻瑰",
    "replacement": "玫瑰"
  }
]

注意事项：
- pattern 必须是有效的正则表达式
- replacement 可以是空字符串表示删除
- 规则名称要简洁明了
- 优先生成通用规则，而非针对特定文本"""

    /**
     * 生成替换规则
     * @param originalText 原文
     * @param repairedText 修正后的文本
     * @return 生成的替换规则列表
     */
    suspend fun generateRules(
        originalText: String,
        repairedText: String
    ): List<GeneratedReplaceRule> = withContext(Dispatchers.IO) {
        // 检查功能是否启用
        if (!AppConfig.aiContentRepairEnabled) {
            return@withContext emptyList()
        }

        // 获取当前配置的提供商
        val provider = AIProviderFactory.getProvider(AppConfig.aiRepairProvider)
            ?: return@withContext emptyList()

        // 获取 API Key
        val apiKey = AppConfig.aiRepairApiKey
        if (provider.requireApiKey && apiKey.isNullOrBlank()) {
            return@withContext emptyList()
        }

        // 获取 API URL
        val apiUrl = if (provider.supportCustomUrl && !AppConfig.aiRepairApiUrl.isNullOrBlank()) {
            AppConfig.aiRepairApiUrl
        } else {
            provider.defaultApiUrl
        }.takeIf { !it.isNullOrBlank() } ?: return@withContext emptyList()

        // 获取模型
        val model = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() }
            ?: provider.supportedModels.firstOrNull { it.isRecommended }?.id
            ?: provider.supportedModels.firstOrNull()?.id
            ?: return@withContext emptyList()

        try {
            // 构建请求内容
            val userContent = buildString {
                appendLine("原文：")
                appendLine(originalText.take(3000)) // 限制长度避免token过多
                appendLine()
                appendLine("修正后：")
                appendLine(repairedText.take(3000))
            }

            // 构建请求体
            val payload = provider.buildRequestBody(
                systemPrompt = DEFAULT_RULE_GENERATOR_PROMPT,
                userContent = userContent,
                model = model,
                temperature = 0.1, // 低温度确保稳定的规则生成
                maxTokens = 2048
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

            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            // 发送请求
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()
                ?: return@withContext emptyList()

            // 解析响应
            val resultText = provider.parseResponse(responseBody)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return@withContext emptyList()

            // 解析JSON规则
            return@withContext parseGeneratedRules(resultText)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    /**
     * 解析生成的规则JSON
     */
    private fun parseGeneratedRules(jsonText: String): List<GeneratedReplaceRule> {
        return try {
            // 尝试直接解析
            GSON.fromJson(jsonText, Array<GeneratedReplaceRule>::class.java)?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            // 尝试从代码块中提取JSON
            val jsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
            val match = jsonRegex.find(jsonText)
            if (match != null) {
                try {
                    GSON.fromJson(match.groupValues[1], Array<GeneratedReplaceRule>::class.java)
                        ?.toList() ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    /**
     * 保存生成的规则到数据库
     * @param rules 生成的规则列表
     * @param bookName 书籍名称（用于规则命名）
     * @param bookOrigin 书源（用于作用范围）
     * @param scopeType 作用范围类型："book" - 仅本书, "source" - 整个书源
     */
    fun saveRules(
        rules: List<GeneratedReplaceRule>,
        bookName: String,
        bookOrigin: String,
        scopeType: String = "book"
    ): List<ReplaceRule> {
        val savedRules = mutableListOf<ReplaceRule>()
        
        val scope = when (scopeType) {
            "source" -> bookOrigin
            else -> bookName
        }
        
        val groupName = if (scopeType == "source") "AI生成-书源规则" else "AI生成-本书规则"

        rules.forEachIndexed { index, generatedRule ->
            val replaceRule = ReplaceRule(
                name = generatedRule.name,
                pattern = generatedRule.pattern,
                replacement = generatedRule.replacement,
                group = groupName,
                scope = scope,
                scopeContent = true,
                isRegex = true,
                isEnabled = true,
                order = Int.MIN_VALUE + index // 确保新规则排在前面
            )
            
            // 检查是否已存在相同规则
            val existing = appDb.replaceRuleDao.all.find { 
                it.pattern == replaceRule.pattern && it.scope == replaceRule.scope 
            }
            
            if (existing == null) {
                val id = appDb.replaceRuleDao.insert(replaceRule).firstOrNull() ?: 0
                if (id > 0) {
                    savedRules.add(replaceRule.copy(id = id))
                }
            }
        }
        
        return savedRules
    }

    /**
     * 生成的替换规则数据类
     */
    data class GeneratedReplaceRule(
        val name: String,
        val pattern: String,
        val replacement: String = ""
    )
}
