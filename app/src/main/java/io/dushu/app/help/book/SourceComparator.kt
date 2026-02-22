package io.dushu.app.help.book

import io.dushu.app.data.entities.Book
import io.dushu.app.data.entities.BookChapter
import io.dushu.app.data.entities.BookSource
import io.dushu.app.data.entities.SearchBook
import io.dushu.app.help.config.SourceConfig
import kotlin.math.max
import kotlin.math.min

/**
 * 书源评估对比器
 * 通过多维度评估选择最优书源
 */
object SourceComparator {

    /**
     * 书源评估维度权重
     */
    data class EvaluationWeights(
        val chapterCountWeight: Double = 0.25,      // 章节数量权重
        val contentCompletenessWeight: Double = 0.20, // 内容完整性权重
        val updateSpeedWeight: Double = 0.15,       // 更新速度权重
        val sourceQualityWeight: Double = 0.15,     // 书源质量评分权重
        val responseSpeedWeight: Double = 0.15,     // 响应速度权重
        val errorRateWeight: Double = 0.10          // 错误率权重（越低越好）
    )

    /**
     * 书源评估结果
     */
    data class SourceEvaluation(
        val searchBook: SearchBook,
        val book: Book?,
        val chapters: List<BookChapter>?,
        val chapterCountScore: Double,
        val completenessScore: Double,
        val updateSpeedScore: Double,
        val sourceQualityScore: Double,
        val responseSpeedScore: Double,
        val errorRateScore: Double,
        val totalScore: Double,
        val details: Map<String, String>
    )

    /**
     * 评估多个书源并返回排序后的结果
     */
    fun evaluateSources(
        searchBooks: List<SearchBook>,
        bookMap: Map<String, Book>,
        chapterMap: Map<String, List<BookChapter>>,
        sourceMap: Map<String, BookSource>,
        weights: EvaluationWeights = EvaluationWeights()
    ): List<SourceEvaluation> {
        if (searchBooks.isEmpty()) return emptyList()

        // 获取最大值用于归一化
        val maxChapterCount = searchBooks.map { it.chapterWordCount }.maxOrNull() ?: 1
        val maxWordCount = searchBooks.map { it.wordCount?.toIntOrNull() ?: 0 }.maxOrNull() ?: 1
        
        return searchBooks.map { searchBook ->
            val book = bookMap[searchBook.origin]
            val chapters = chapterMap[searchBook.origin]
            val source = sourceMap[searchBook.origin]
            
            evaluateSingleSource(
                searchBook, book, chapters, source,
                maxChapterCount, maxWordCount, weights
            )
        }.sortedByDescending { it.totalScore }
    }

    /**
     * 评估单个书源
     */
    private fun evaluateSingleSource(
        searchBook: SearchBook,
        book: Book?,
        chapters: List<BookChapter>?,
        source: BookSource?,
        maxChapterCount: Int,
        maxWordCount: Int,
        weights: EvaluationWeights
    ): SourceEvaluation {
        val details = mutableMapOf<String, String>()
        
        // 1. 章节数量评分 (0-100)
        val chapterCountScore = if (maxChapterCount > 0) {
            (searchBook.chapterWordCount.toDouble() / maxChapterCount * 100)
                .coerceIn(0.0, 100.0)
        } else 50.0
        details["章节数"] = "${searchBook.chapterWordCount}章"
        
        // 2. 内容完整性评分
        val completenessScore = calculateCompletenessScore(searchBook, book, chapters)
        details["完整性"] = when {
            completenessScore >= 80 -> "优秀"
            completenessScore >= 60 -> "良好"
            completenessScore >= 40 -> "一般"
            else -> "较差"
        }
        
        // 3. 更新速度评分 (根据最后更新时间)
        val updateSpeedScore = calculateUpdateSpeedScore(searchBook)
        details["更新速度"] = when {
            updateSpeedScore >= 80 -> "快"
            updateSpeedScore >= 60 -> "中等"
            else -> "较慢"
        }
        
        // 4. 书源质量评分 (来自 SourceConfig)
        val sourceQualityScore = SourceConfig.getSourceScore(searchBook.origin).toDouble()
            .coerceIn(0.0, 100.0)
        details["书源质量"] = "${sourceQualityScore.toInt()}分"
        
        // 5. 响应速度评分 (基于是否有章节缓存)
        val responseSpeedScore = if (chapters != null && chapters.isNotEmpty()) {
            100.0 // 有缓存，响应快
        } else {
            60.0 // 无缓存
        }
        details["响应速度"] = if (responseSpeedScore >= 80) "快" else "一般"
        
        // 6. 错误率评分 (基于书源历史表现)
        val errorRateScore = calculateErrorRateScore(source)
        details["稳定性"] = when {
            errorRateScore >= 80 -> "稳定"
            errorRateScore >= 60 -> "一般"
            else -> "较差"
        }
        
        // 计算总分
        val totalScore = 
            chapterCountScore * weights.chapterCountWeight +
            completenessScore * weights.contentCompletenessWeight +
            updateSpeedScore * weights.updateSpeedWeight +
            sourceQualityScore * weights.sourceQualityWeight +
            responseSpeedScore * weights.responseSpeedWeight +
            errorRateScore * weights.errorRateWeight
        
        return SourceEvaluation(
            searchBook = searchBook,
            book = book,
            chapters = chapters,
            chapterCountScore = chapterCountScore,
            completenessScore = completenessScore,
            updateSpeedScore = updateSpeedScore,
            sourceQualityScore = sourceQualityScore,
            responseSpeedScore = responseSpeedScore,
            errorRateScore = errorRateScore,
            totalScore = totalScore,
            details = details
        )
    }

    /**
     * 计算内容完整性评分
     */
    private fun calculateCompletenessScore(
        searchBook: SearchBook,
        book: Book?,
        chapters: List<BookChapter>?
    ): Double {
        var score = 50.0 // 基础分
        
        // 有简介加分
        if (!searchBook.intro.isNullOrBlank() && searchBook.intro!!.length > 50) {
            score += 15
        }
        
        // 有封面加分
        if (!searchBook.coverUrl.isNullOrBlank()) {
            score += 10
        }
        
        // 有章节信息加分
        if (chapters != null && chapters.isNotEmpty()) {
            score += 15
            // 章节数量越多越完整
            if (chapters.size > 100) score += 5
            if (chapters.size > 500) score += 5
        }
        
        // 有字数信息加分
        val wordCountValue = searchBook.wordCount?.toIntOrNull() ?: 0
        if (wordCountValue > 0) {
            score += 10
        }
        
        return score.coerceIn(0.0, 100.0)
    }

    /**
     * 计算更新速度评分
     */
    private fun calculateUpdateSpeedScore(searchBook: SearchBook): Double {
        val latestChapter = searchBook.latestChapterTitle ?: return 50.0
        
        // 检查是否包含最新章节标识
        return when {
            latestChapter.contains("最新") || 
            latestChapter.contains("连载") -> 90.0
            latestChapter.contains("完结") || 
            latestChapter.contains("完本") -> 85.0
            searchBook.chapterWordCount > 1000 -> 80.0 // 章节多通常更新较快
            else -> 60.0
        }
    }

    /**
     * 计算错误率评分 (稳定性)
     */
    private fun calculateErrorRateScore(source: BookSource?): Double {
        if (source == null) return 50.0
        
        // 根据书源类型和响应历史判断
        return when {
            // 官方源通常更稳定
            source.bookSourceName.contains("官方") || 
            source.bookSourceName.contains("正版") -> 90.0
            // 大型网站通常较稳定
            source.bookSourceName.contains("起点") ||
            source.bookSourceName.contains("纵横") ||
            source.bookSourceName.contains("晋江") -> 85.0
            // 默认分数
            else -> 70.0
        }
    }

    /**
     * 获取最优书源
     */
    fun getBestSource(
        searchBooks: List<SearchBook>,
        bookMap: Map<String, Book>,
        chapterMap: Map<String, List<BookChapter>>,
        sourceMap: Map<String, BookSource>
    ): SourceEvaluation? {
        if (searchBooks.isEmpty()) return null
        val evaluations = evaluateSources(searchBooks, bookMap, chapterMap, sourceMap)
        return evaluations.firstOrNull()
    }

    /**
     * 格式化评估结果为可读文本
     */
    fun formatEvaluation(evaluation: SourceEvaluation): String {
        val sb = StringBuilder()
        sb.appendLine("书源: ${evaluation.searchBook.originName}")
        sb.appendLine("总分: ${evaluation.totalScore.toInt()}分")
        sb.appendLine("各项评分:")
        sb.appendLine("  • 章节数量: ${evaluation.chapterCountScore.toInt()}分")
        sb.appendLine("  • 内容完整: ${evaluation.completenessScore.toInt()}分")
        sb.appendLine("  • 更新速度: ${evaluation.updateSpeedScore.toInt()}分")
        sb.appendLine("  • 书源质量: ${evaluation.sourceQualityScore.toInt()}分")
        sb.appendLine("  • 响应速度: ${evaluation.responseSpeedScore.toInt()}分")
        sb.appendLine("  • 稳定性: ${evaluation.errorRateScore.toInt()}分")
        sb.appendLine("详细信息:")
        evaluation.details.forEach { (key, value) ->
            sb.appendLine("  • $key: $value")
        }
        return sb.toString()
    }
}
