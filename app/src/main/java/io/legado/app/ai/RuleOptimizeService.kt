package io.legado.app.ai

import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.GSON
import io.legado.app.help.http.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * AI规则优化服务
 * 分析现有替换规则，识别相似规则并合并，生成更通用的正则表达式
 */
object RuleOptimizeService {

    // 规则分析合并提示词
    private const val RULE_OPTIMIZE_PROMPT = """你是一个专业的正则表达式优化专家。

任务：分析提供的替换规则列表，识别相似或重复的规则，合并为更通用、更高效的正则表达式。

优化目标：
1. 合并相似规则：如 "【广告】" 和 "【推广】" 合并为 "【(?:广告|推广)】"
2. 提取共同模式：多个针对同一类问题的规则应该合并
3. 优化正则性能：避免回溯，使用字符类而非多选分支
4. 生成通用模式：能匹配同类问题，而不是过于具体

输入格式：
规则列表（JSON数组）：
[
  {"name": "规则1", "pattern": "【广告】", "replacement": ""},
  {"name": "规则2", "pattern": "【推广】", "replacement": ""},
  {"name": "规则3", "pattern": "本章未完，点击继续阅读", "replacement": ""}
]

输出要求：
1. 只返回JSON格式的优化结果，不要任何解释
2. 返回结构：
   {
     "mergedRules": [合并后的规则数组],
     "removedRules": [被合并的规则名称数组],
     "suggestions": [优化建议文本数组]
   }
3. mergedRules 格式与原规则相同
4. 如果无法优化，返回空对象 {}

示例输出：
{
  "mergedRules": [
    {
      "name": "过滤-广告推广标记",
      "pattern": "【(?:广告|推广|推荐)】",
      "replacement": ""
    },
    {
      "name": "过滤-本章未完提示",
      "pattern": "本章[\\s\\S]{0,30}?(?:未完|结束|待续)[\\s\\S]{0,30}?(?:点击|继续|阅读|下一页)[\\s\\S]{0,20}?[。！.]?",
      "replacement": ""
    }
  ],
  "removedRules": ["【广告】", "【推广】", "【推荐】", "本章未完"],
  "suggestions": ["将3个广告标记规则合并为1个", "优化本章未完提示为更通用的正则"]
}

重要提示：
- pattern 必须是有效的Java正则表达式
- 使用非捕获组 (?:...) 而非捕获组 (...)
- 适当使用量词限制，避免过度匹配
- replacement 保持不变（通常为空字符串）
- 规则名称应该简洁明了"""

    /**
     * 分析并优化规则
     * @param rules 需要分析的规则列表
     * @return 优化结果
     */
    suspend fun analyzeAndMergeRules(
        rules: List<ReplaceRule>
    ): OptimizeResult = withContext(Dispatchers.IO) {
        if (rules.isEmpty() || !AppConfig.aiContentRepairEnabled) {
            return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())
        }

        val provider = AIProviderFactory.getProvider(AppConfig.aiRepairProvider)
            ?: return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())

        val apiKey = AppConfig.aiRepairApiKey
        if (provider.requireApiKey && apiKey.isNullOrBlank()) {
            return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())
        }

        val apiUrl = if (provider.supportCustomUrl && !AppConfig.aiRepairApiUrl.isNullOrBlank()) {
            AppConfig.aiRepairApiUrl
        } else {
            provider.defaultApiUrl
        }.takeIf { !it.isNullOrBlank() } ?: return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())

        val model = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() }
            ?: provider.supportedModels.firstOrNull { it.isRecommended }?.id
            ?: provider.supportedModels.firstOrNull()?.id
            ?: return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())

        try {
            // 构建规则JSON
            val rulesJson = GSON.toJson(rules.map { 
                mapOf(
                    "name" to it.name,
                    "pattern" to it.pattern,
                    "replacement" to it.replacement
                )
            })

            val userContent = "请分析以下替换规则，识别相似规则并合并优化：\n\n$rulesJson"

            val payload = provider.buildRequestBody(
                systemPrompt = RULE_OPTIMIZE_PROMPT,
                userContent = userContent,
                model = model,
                temperature = 0.2,
                maxTokens = 4096
            )

            val headers = if (apiKey != null) {
                provider.buildHeaders(apiKey)
            } else {
                mapOf("Content-Type" to "application/json")
            }

            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))

            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string()
                ?: return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())

            val resultText = provider.parseResponse(responseBody)?.trim()
                ?: return@withContext OptimizeResult(emptyList(), emptyList(), emptyList())

            return@withContext parseOptimizeResult(resultText)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext OptimizeResult(emptyList(), emptyList(), listOf("分析失败: ${e.message}"))
        }
    }

    /**
     * 解析优化结果
     */
    private fun parseOptimizeResult(jsonText: String): OptimizeResult {
        return try {
            // 尝试直接解析
            GSON.fromJson(jsonText, OptimizeResult::class.java)
        } catch (e: Exception) {
            // 尝试从代码块中提取
            val jsonRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
            val match = jsonRegex.find(jsonText)
            if (match != null) {
                try {
                    GSON.fromJson(match.groupValues[1], OptimizeResult::class.java)
                } catch (_: Exception) {
                    OptimizeResult(emptyList(), emptyList(), emptyList())
                }
            } else {
                OptimizeResult(emptyList(), emptyList(), emptyList())
            }
        }
    }

    /**
     * 应用优化结果
     * @param result 优化结果
     * @param bookName 书籍名称（用于作用范围）
     * @param bookOrigin 书源（用于作用范围）
     * @return 成功保存的规则数量
     */
    fun applyOptimization(
        result: OptimizeResult,
        bookName: String = "",
        bookOrigin: String = ""
    ): Int {
        var savedCount = 0
        
        // 删除被合并的旧规则
        result.removedRules.forEach { ruleName ->
            appDb.replaceRuleDao.all.find { it.name == ruleName }?.let {
                appDb.replaceRuleDao.delete(it)
            }
        }
        
        // 保存合并后的新规则
        result.mergedRules.forEachIndexed { index, ruleData ->
            val replaceRule = ReplaceRule(
                name = ruleData.name,
                pattern = ruleData.pattern,
                replacement = ruleData.replacement,
                group = "AI优化合并",
                scope = if (bookName.isNotEmpty()) bookName else bookOrigin,
                scopeContent = true,
                isRegex = true,
                isEnabled = true,
                order = Int.MIN_VALUE + index
            )
            
            // 检查是否已存在相同规则
            val existing = appDb.replaceRuleDao.all.find { 
                it.pattern == replaceRule.pattern && it.scope == replaceRule.scope 
            }
            
            if (existing == null) {
                val id = appDb.replaceRuleDao.insert(replaceRule).firstOrNull() ?: 0
                if (id > 0) savedCount++
            }
        }
        
        return savedCount
    }

    /**
     * 优化结果数据类
     */
    data class OptimizeResult(
        val mergedRules: List<RuleData> = emptyList(),
        val removedRules: List<String> = emptyList(),
        val suggestions: List<String> = emptyList()
    )

    /**
     * 规则数据类（用于JSON解析）
     */
    data class RuleData(
        val name: String = "",
        val pattern: String = "",
        val replacement: String = ""
    )
}
