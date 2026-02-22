package io.dushu.app.ai

import io.dushu.app.data.appDb
import io.dushu.app.data.entities.ReplaceRule
import io.dushu.app.help.config.AppConfig
import io.dushu.app.utils.GSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import io.dushu.app.help.http.okHttpClient

/**
 * AI规则生成服务
 * 分析原文和修正后的差异，自动生成替换净化规则
 */
object RuleGeneratorService {

    // 默认系统提示词 - 用于生成替换规则
    private const val DEFAULT_RULE_GENERATOR_PROMPT = """你是一个专业的网络小说文本清洗专家。

任务：分析原文和修正后的文本差异，识别并生成用于删除"无意义内容"的替换规则。

重点识别以下类型的无意义内容：
1. 网站引流提示："本章未完，点击继续阅读"、"请点击下一页"、"查看完整章节"等
2. 广告推广标记："【广告】"、"【推广】"、"【VIP章节】"等
3. 平台提示信息："手机站"、"移动版"、"APP阅读更流畅"等
4. 系统通知："重要通知"、"系统提示"、"作者有话说"等（除非是正文中的对话）
5. URL链接：http://、https://、www.开头的网址
6. 重复分隔符：连续的特殊符号如"-----"、"====="等

输入格式：
原文：[原始文本]
修正后：[修正后的文本]

输出要求：
1. 只返回JSON格式的规则数组，不要任何解释文字
2. 每个规则包含：name(规则名称), pattern(正则表达式), replacement(替换为)
3. pattern必须使用正则表达式，能匹配同类内容
4. replacement为空字符串表示删除该内容
5. 如果无法生成有效规则，返回空数组 []

示例输出格式：
[
  {
    "name": "过滤-本章未完提示",
    "pattern": "【?本章[\\s\\S]{0,20}?(?:继续阅读|点击阅读|查看完整|下一页)[\\s\\S]{0,20}?】?",
    "replacement": ""
  },
  {
    "name": "过滤-网站引流",
    "pattern": "(?:点击|请|立即).*?(?:继续阅读|阅读全文|查看完整|下载APP|打开APP)[\\s\\S]{0,10}?[。！.]?",
    "replacement": ""
  },
  {
    "name": "过滤-广告标记",
    "pattern": "【(?:广告|推广|推荐|VIP|订阅).*?】",
    "replacement": ""
  },
  {
    "name": "过滤-网址链接",
    "pattern": "https?://[\\w./?=&-]+",
    "replacement": ""
  },
  {
    "name": "修正-OCR错字",
    "pattern": "攻瑰",
    "replacement": "玫瑰"
  }
]

重要提示：
- 规则必须通用，能匹配同类问题，而不是只针对特定文本
- 对于"本章未完"这类提示，使用非贪婪匹配.*?并限制长度{0,20}
- 优先识别并删除无意义的网站提示和广告内容
- pattern 必须是有效的Java正则表达式
- 规则名称格式：类型-具体描述（如：过滤-XXX、修正-XXX）"""

    /**
     * 规则生成类型
     */
    enum class RuleType {
        GENERAL,  // 通用规则 - 使用正则表达式匹配同类内容
        SPECIFIC  // 特定规则 - 精确匹配特定文本
    }

    /**
     * 生成替换规则
     * @param originalText 原文
     * @param repairedText 修正后的文本
     * @param ruleType 规则类型：通用规则或特定规则
     * @return 生成的替换规则列表
     */
    suspend fun generateRules(
        originalText: String,
        repairedText: String,
        ruleType: RuleType = RuleType.GENERAL
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
            // 根据规则类型选择提示词
            val systemPrompt = when (ruleType) {
                RuleType.GENERAL -> DEFAULT_RULE_GENERATOR_PROMPT
                RuleType.SPECIFIC -> SPECIFIC_RULE_GENERATOR_PROMPT
            }

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
                systemPrompt = systemPrompt,
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
            val rules = parseGeneratedRules(resultText)
            
            // 特定规则模式：设置isRegex为false
            return@withContext if (ruleType == RuleType.SPECIFIC) {
                rules.map { it.copy(isRegex = false) }
            } else {
                rules
            }

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
                isRegex = generatedRule.isRegex, // 使用规则中的isRegex设置
                isEnabled = true,
                order = Int.MIN_VALUE + index // 确保新规则排在前面
            )
            
            // 检查是否已存在相同规则（考虑isRegex）
            val existing = appDb.replaceRuleDao.all.find { 
                it.pattern == replaceRule.pattern && 
                it.scope == replaceRule.scope &&
                it.isRegex == replaceRule.isRegex
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

    // 特定规则生成提示词 - 用于生成精确匹配的替换规则
    private const val SPECIFIC_RULE_GENERATOR_PROMPT = """你是一个专业的网络小说文本清洗专家。

任务：分析原文和修正后的文本差异，识别被删除或修改的具体内容，生成精确的特定替换规则。

重点识别以下类型的内容：
1. 网站引流提示："本章未完，点击继续阅读"、"请点击下一页"等（精确保留原文）
2. 广告推广标记："【广告】"、"【推广】"等具体标记
3. 平台提示信息："手机站"、"移动版"、"APP阅读更流畅"等
4. 系统通知："重要通知"、"系统提示"等具体文本
5. URL链接：具体的http://、https://链接
6. 特定错字：原文中的错别字

输入格式：
原文：[原始文本]
修正后：[修正后的文本]

输出要求：
1. 只返回JSON格式的规则数组，不要任何解释文字
2. 每个规则包含：name(规则名称), pattern(精确匹配文本), replacement(替换为，通常为空)
3. pattern必须是原文中实际存在的精确文本（不是正则表达式）
4. replacement为空字符串表示删除该内容
5. 如果无法生成有效规则，返回空数组 []

示例输出格式：
[
  {
    "name": "删除-本章未完提示",
    "pattern": "本章未完，点击继续阅读",
    "replacement": ""
  },
  {
    "name": "删除-广告标记",
    "pattern": "【广告】",
    "replacement": ""
  },
  {
    "name": "修正-OCR错字",
    "pattern": "攻瑰",
    "replacement": "玫瑰"
  }
]

重要提示：
- pattern必须是原文中的精确文本，不要使用通配符或正则表达式
- 优先识别并删除无意义的网站提示和广告内容
- 对于修改的内容，记录原文和修正后的文本"""

    /**
     * 生成的替换规则数据类
     */
    data class GeneratedReplaceRule(
        val name: String,
        val pattern: String,
        val replacement: String = "",
        val isRegex: Boolean = true
    )
}
