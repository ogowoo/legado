package io.dushu.app.ai

/**
 * 内容过滤器
 * 用于过滤无意义内容，如广告、提示信息等
 */
object ContentFilter {

    /**
     * 无意义内容匹配规则列表
     * 支持正则表达式
     */
    private val meaninglessPatterns = listOf(
        // 常见的小说网站提示
        Regex("【.*?继续阅读.*?】", RegexOption.IGNORE_CASE),
        Regex("【本章.*?未完.*?】", RegexOption.IGNORE_CASE),
        Regex("【.*?点击下一页.*?】", RegexOption.IGNORE_CASE),
        Regex("本章.*?未完.*?点击继续阅读", RegexOption.IGNORE_CASE),
        Regex("本章未完.*下一页", RegexOption.IGNORE_CASE),
        Regex("本章未完待续", RegexOption.IGNORE_CASE),
        Regex("点击继续阅读本章.*", RegexOption.IGNORE_CASE),
        Regex("点击下一页继续阅读", RegexOption.IGNORE_CASE),
        Regex("【请点击.*继续阅读】", RegexOption.IGNORE_CASE),
        Regex("【请.*点击.*下一.*】", RegexOption.IGNORE_CASE),
        
        // 各种变体
        Regex("本章.*未完.*下一.*", RegexOption.IGNORE_CASE),
        Regex("本章.*未完.*继续.*", RegexOption.IGNORE_CASE),
        Regex("未完待续.*", RegexOption.IGNORE_CASE),
        Regex("本章.*待续", RegexOption.IGNORE_CASE),
        
        // 常见广告提示
        Regex("【.*广告.*】", RegexOption.IGNORE_CASE),
        Regex("【.*推广.*】", RegexOption.IGNORE_CASE),
        Regex("【.*推荐.*】", RegexOption.IGNORE_CASE),
        
        // 手机站提示
        Regex("【手机.*站.*】", RegexOption.IGNORE_CASE),
        Regex("【移动.*版.*】", RegexOption.IGNORE_CASE),
        Regex("【.*wap.*】", RegexOption.IGNORE_CASE),
        
        // 通用无意义内容
        Regex("【重要通知.*】"),
        Regex("【系统.*提示.*】"),
        Regex("【作者.*说.*】"),
        
        // 纯符号分隔线（连续的特殊符号）
        Regex("^[-=*─—]{3,}$"),
        
        // 空括号
        Regex("【】"),
        Regex("（）"),
        Regex("\\[\\]"),
        
        // URL 和链接文本
        Regex("https?://\\S+"),
        Regex("www\\.\\S+"),
    )

    /**
     * 过滤无意义内容
     * @param content 原始内容
     * @return 过滤后的内容
     */
    fun filter(content: String): String {
        if (content.isBlank()) return content
        
        var filtered = content
        
        // 应用所有过滤规则
        for (pattern in meaninglessPatterns) {
            filtered = pattern.replace(filtered, "")
        }
        
        // 清理多余的空白字符
        filtered = filtered
            .replace(Regex("\n{3,}"), "\n\n")  // 超过2个换行符压缩为2个
            .replace(Regex(" {2,}"), " ")       // 多个空格压缩为1个
            .trim()
        
        return filtered
    }

    /**
     * 检查内容是否主要为无意义内容
     * @param content 要检查的内容
     * @return 如果超过50%的内容被过滤掉，返回 true
     */
    fun isMostlyMeaningless(content: String): Boolean {
        if (content.isBlank()) return false
        
        val filtered = filter(content)
        val originalLength = content.length
        val filteredLength = filtered.length
        
        // 如果被过滤掉的内容超过50%
        return (originalLength - filteredLength) > originalLength * 0.5
    }

    /**
     * 获取被过滤的内容列表
     * 用于调试，查看哪些内容被过滤掉了
     */
    fun getFilteredContent(content: String): List<String> {
        val filtered = mutableListOf<String>()
        
        for (pattern in meaninglessPatterns) {
            pattern.findAll(content).forEach { match ->
                filtered.add(match.value)
            }
        }
        
        return filtered.distinct()
    }
}
