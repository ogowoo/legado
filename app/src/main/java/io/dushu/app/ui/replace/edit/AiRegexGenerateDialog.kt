package io.dushu.app.ui.replace.edit

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import io.dushu.app.R
import io.dushu.app.ai.AIProviderFactory
import io.dushu.app.databinding.DialogAiRegexGenerateBinding
import io.dushu.app.help.config.AppConfig
import io.dushu.app.ui.widget.dialog.BaseAiDialog
import io.dushu.app.utils.toastOnUi
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import io.dushu.app.help.http.okHttpClient

/**
 * AI正则生成对话框
 * 根据文本示例生成对应的正则表达式
 */
class AiRegexGenerateDialog : BaseAiDialog(R.layout.dialog_ai_regex_generate) {

    private val binding by viewBinding(DialogAiRegexGenerateBinding::bind)
    
    override val progressBar: ProgressBar? by lazy { binding.progressBar }
    override val tvStatus: TextView? by lazy { binding.tvStatus }
    override val contentView: View? by lazy { binding.contentContainer }

    companion object {
        const val TAG = "AiRegexGenerateDialog"

        fun newInstance(): AiRegexGenerateDialog {
            return AiRegexGenerateDialog()
        }
    }

    interface CallBack {
        fun onRegexGenerated(pattern: String)
    }

    override fun initView() {
        setupView()
    }
    
    private fun setupView() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnGenerate.setOnClickListener {
            generateRegex()
        }

        binding.btnApply.setOnClickListener {
            val pattern = binding.etGeneratedPattern.text.toString().trim()
            if (pattern.isNotEmpty()) {
                (activity as? CallBack)?.onRegexGenerated(pattern)
                dismiss()
            } else {
                toastOnUi("请先生成正则表达式")
            }
        }

        // 显示提示
        binding.tvHint.text = """
            提示：
            1. 输入要匹配的文本示例
            2. 可输入多个示例，每行一个
            3. AI会根据示例生成通用正则表达式
            4. 支持中文、英文、数字、特殊符号等
            
            示例输入：
            本章未完，点击继续阅读
            本章未完，请点击下一页
        """.trimIndent()
    }

    private fun generateRegex() {
        val sampleText = binding.etSampleText.text.toString().trim()
        if (sampleText.isEmpty()) {
            toastOnUi("请输入要匹配的文本示例")
            return
        }

        // 检查 AI 是否配置
        if (!AppConfig.aiContentRepairEnabled) {
            toastOnUi("请先启用并配置 AI 内容修复功能")
            return
        }

        showLoading("正在生成正则表达式...")
        binding.btnGenerate.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val pattern = doGenerateRegex(sampleText)

            withContext(Dispatchers.Main) {
                hideLoading()
                binding.btnGenerate.isEnabled = true

                if (pattern != null) {
                    binding.etGeneratedPattern.setText(pattern)
                    binding.tvStatus.text = "生成成功！"
                    // 自动勾选使用正则
                    binding.cbIsRegex.isChecked = true
                } else {
                    showError("生成失败，请检查 AI 配置或重试")
                }
            }
        }
    }

    private suspend fun doGenerateRegex(sampleText: String): String? {
        val provider = AIProviderFactory.getProvider(AppConfig.aiRepairProvider)
            ?: return null

        val apiKey = AppConfig.aiRepairApiKey
        if (provider.requireApiKey && apiKey.isNullOrBlank()) {
            return null
        }

        val apiUrl = if (provider.supportCustomUrl && !AppConfig.aiRepairApiUrl.isNullOrBlank()) {
            AppConfig.aiRepairApiUrl
        } else {
            provider.defaultApiUrl
        }.takeIf { !it.isNullOrBlank() } ?: return null

        val model = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() }
            ?: provider.supportedModels.firstOrNull { it.isRecommended }?.id
            ?: provider.supportedModels.firstOrNull()?.id
            ?: return null

        return try {
            val systemPrompt = """你是一个正则表达式专家。

任务：根据用户提供的文本示例，生成能够匹配同类内容的 Java 正则表达式。

要求：
1. 生成的正则表达式必须在 Java 中有效
2. 正则应该足够通用，能匹配类似的文本，而不仅仅是完全相同的字符串
3. 对于变化的数字、字母、汉字等，使用适当的通配符
4. 对于固定部分，保持精确匹配
5. 如果示例中有重复模式，使用分组和量词
6. 只返回正则表达式本身，不要任何解释或代码块标记

输出格式：只返回正则表达式字符串，不要加任何其他内容。

示例：
输入："第1章"、"第12章"、"第123章"
输出：第\\d+章

输入："本章未完，点击继续阅读"
输出：本章未完.*?继续阅读"""

            val userContent = "请根据以下文本示例生成正则表达式：\n\n$sampleText"

            val payload = provider.buildRequestBody(
                systemPrompt = systemPrompt,
                userContent = userContent,
                model = model,
                temperature = 0.2,
                maxTokens = 512
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
            val responseBody = response.body?.string() ?: return null

            provider.parseResponse(responseBody)?.trim()?.takeIf { it.isNotBlank() }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getIsRegex(): Boolean {
        return binding.cbIsRegex.isChecked
    }
}
