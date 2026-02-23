package io.dushu.app.ai

import io.dushu.app.help.config.AppConfig
import io.dushu.app.help.http.okHttpClient
import io.dushu.app.utils.GSON
import io.dushu.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ContentRepairService {

    // 针对网文优化的默认系统提示词
    private const val DEFAULT_SYSTEM_PROMPT = """你是一个专业的网络小说文本校对助手。

任务：分析当前段落，如果发现错误则修复，如果内容正常则直接返回原文。

重要判断规则（按优先级）：
1. 【正常内容无需修改】如果段落是通顺、连贯的中文，没有乱码和明显错误，直接返回原文，不要做任何修改
2. 【非正文内容】如果是章节标题、作者的话、广告提示等非正文内容，直接返回原文
3. 【正常对话内容】如果是正常的人物对话（如"他说道："、"她问："等），直接返回原文

常见需要修复的错误类型：
1. OCR识别错误：如 "玫"误为"攻"、"的"误为"白勺"、数字"0"和字母"O"混淆
2. 乱码字符：如"锟斤拷"、"�"、"烫烫烫"等乱码符号
3. 缺字漏字：句子中有明显的字缺失（如"今天气很好"应为"今天天气很好"）
4. 错序错乱：语句顺序颠倒、字词位置明显错误
5. 错别字：明显的同音字、形近字错误（如"在"和"再"、"地"和"的"用错）
6. 特殊符号问题：多余或缺失的标点、不正常的符号

不需要修复的情况（直接返回原文）：
- 段落通顺、无语病、无乱码
- 正常的人物对话和心理描写
- 正常的场景描写和叙述
- 章节标题、卷名等非正文内容
- 没有明显错误的日常用语

网文特殊注意：
- 保留人物对话风格（如"说"、"道"、"问"等提示语）
- 保留网络流行语和特殊用语
- 保持段落原意不变，只修正明显错误
- 注意人名、地名的前后一致性

输出要求：
- 只返回修正后的段落内容（或原文）
- 不要添加任何解释、说明或"修正如下"等前缀
- 不要修改原文的叙述风格
- 【关键】如果段落无明显错误，必须直接返回原文，不得添加任何修改"""

    // 默认参数
    private const val DEFAULT_TEMPERATURE = 0.2
    private const val DEFAULT_MAX_TOKENS = 512
    private const val CONTEXT_LENGTH = 4000

    /**
     * 智能判断是否需要修复
     * 如果文本看起来已经正常，返回 false 跳过 AI 修复
     */
    fun shouldRepair(text: String): Boolean {
        // 如果文本为空或太短，不需要修复
        if (text.isBlank() || text.length < 3) return false
        
        // 检查是否包含明显的乱码特征
        val hasGarbledChars = text.contains(Regex("[锟斤拷烫屯枸獄絔瓠殸彃挍尠]|�|[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"))
        
        // 检查是否包含明显的OCR错误特征（数字和字母混淆等）
        val hasOcrErrors = text.contains(Regex("[0-9][a-zA-Z]|[a-zA-Z][0-9]")) && 
                          text.contains(Regex("[攻白勺]"))
        
        // 检查是否有明显的缺字（连续多个空格后紧跟标点）
        val hasMissingChars = text.contains(Regex("  +[，。！？、]"))
        
        // 检查是否有不正常的符号重复
        val hasSymbolErrors = text.contains(Regex("[。，]{2,}|[!！]{2,}|[?？]{2,}"))
        
        // 如果存在上述问题，需要修复
        if (hasGarbledChars || hasOcrErrors || hasMissingChars || hasSymbolErrors) {
            return true
        }
        
        // 检查文本连贯性：如果文本是通顺的中文（没有明显的乱码单词），可能不需要修复
        // 统计中文字符比例
        val chineseChars = text.filter { it in '\u4e00'..'\u9fff' }.length
        val totalChars = text.length
        val chineseRatio = chineseChars.toDouble() / totalChars
        
        // 如果中文字符占比很高（>70%）且没有上述错误特征，可能是正常文本
        if (chineseRatio > 0.7 && text.length > 10) {
            // 进一步检查是否有明显的语义断裂
            val hasSemanticBreaks = text.contains(Regex("[锟斤拷烫屯枸獄絔瓠殸彃挍尠]|�|\\b[a-zA-Z]{5,}\\b"))
            if (!hasSemanticBreaks) {
                // 可能是正常文本，但还是要让AI判断（因为可能有细微错误）
                // 返回 true 让AI来判断
                return true
            }
        }
        
        return true
    }

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
            }.takeIf { !it.isNullOrBlank() } ?: return@withContext paragraph

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
