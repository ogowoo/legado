package io.dushu.app.ui.book.read

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import io.dushu.app.R
import io.dushu.app.ai.RuleGeneratorService
import io.dushu.app.ai.RuleGeneratorService.RuleType
import io.dushu.app.databinding.DialogAiRuleGeneratorBinding
import io.dushu.app.ui.widget.dialog.BaseAiDialog
import io.dushu.app.utils.toastOnUi
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI规则生成对话框
 * 在AI修正后询问用户是否生成替换规则
 */
class AiRuleGeneratorDialog : BaseAiDialog(R.layout.dialog_ai_rule_generator) {

    private val binding by viewBinding(DialogAiRuleGeneratorBinding::bind)
    
    private var originalText: String = ""
    private var repairedText: String = ""
    private var bookName: String = ""
    private var bookOrigin: String = ""
    
    override val progressBar: ProgressBar? by lazy { binding.progressBar }
    override val tvStatus: TextView? by lazy { binding.tvStatus }
    override val contentView: View? by lazy { binding.contentContainer }

    companion object {
        const val TAG = "AiRuleGeneratorDialog"
        private const val ARG_ORIGINAL_TEXT = "original_text"
        private const val ARG_REPAIRED_TEXT = "repaired_text"
        private const val ARG_BOOK_NAME = "book_name"
        private const val ARG_BOOK_ORIGIN = "book_origin"

        fun newInstance(
            originalText: String,
            repairedText: String,
            bookName: String,
            bookOrigin: String
        ): AiRuleGeneratorDialog {
            return AiRuleGeneratorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORIGINAL_TEXT, originalText)
                    putString(ARG_REPAIRED_TEXT, repairedText)
                    putString(ARG_BOOK_NAME, bookName)
                    putString(ARG_BOOK_ORIGIN, bookOrigin)
                }
            }
        }
    }

    override fun initView() {
        originalText = arguments?.getString(ARG_ORIGINAL_TEXT) ?: ""
        repairedText = arguments?.getString(ARG_REPAIRED_TEXT) ?: ""
        bookName = arguments?.getString(ARG_BOOK_NAME) ?: ""
        bookOrigin = arguments?.getString(ARG_BOOK_ORIGIN) ?: ""
        
        setupView()
        generateRules()
    }
    
    private fun setupView() {
        showLoading("正在分析文本差异，生成替换规则...")
        binding.rgRuleType.visibility = View.GONE
        binding.rgScope.visibility = View.GONE
        binding.btnSave.visibility = View.GONE
        binding.tvRules.visibility = View.GONE
        
        // 默认选中"通用规则"和"仅本书"
        binding.rbGeneralRule.isChecked = true
        binding.rbBookScope.isChecked = true
        
        // 规则类型切换时重新生成
        binding.rgRuleType.setOnCheckedChangeListener { _, _ ->
            generateRules()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveRules()
        }
    }

    private var currentRules: List<RuleGeneratorService.GeneratedReplaceRule> = emptyList()
    
    private fun generateRules() {
        // 清空当前规则
        currentRules = emptyList()
        
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在分析文本差异，生成替换规则..."
        binding.rgRuleType.visibility = View.GONE
        binding.rgScope.visibility = View.GONE
        binding.btnSave.visibility = View.GONE
        binding.tvRules.visibility = View.GONE
        
        val ruleType = if (binding.rbSpecificRule.isChecked) RuleType.SPECIFIC else RuleType.GENERAL
        
        lifecycleScope.launch(Dispatchers.IO) {
            val rules = RuleGeneratorService.generateRules(originalText, repairedText, ruleType)
            currentRules = rules
            
            withContext(Dispatchers.Main) {
                hideLoading()
                
                if (rules.isEmpty()) {
                    val typeText = if (ruleType == RuleType.SPECIFIC) "特定" else "通用"
                    binding.tvStatus.text = "未检测到可生成的${typeText}规则\n（差异可能是特定上下文导致的）"
                    binding.rgRuleType.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.GONE
                    binding.rgScope.visibility = View.GONE
                } else {
                    val typeText = if (ruleType == RuleType.SPECIFIC) "特定" else "通用"
                    binding.tvStatus.text = "已生成 ${rules.size} 条${typeText}替换规则"
                    binding.rgRuleType.visibility = View.VISIBLE
                    binding.rgScope.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                    binding.tvRules.visibility = View.VISIBLE
                    
                    // 显示生成的规则
                    val rulesText = StringBuilder()
                    rules.forEachIndexed { index, rule ->
                        rulesText.appendLine("${index + 1}. ${rule.name}")
                        rulesText.appendLine("   查找: ${rule.pattern}")
                        rulesText.appendLine("   替换: ${if (rule.replacement.isEmpty()) "(删除)" else rule.replacement}")
                        rulesText.appendLine()
                    }
                    binding.tvRules.text = rulesText.toString()
                }
            }
        }
    }

    private fun saveRules() {
        val scopeType = if (binding.rbSourceScope.isChecked) "source" else "book"
        val ruleType = if (binding.rbSpecificRule.isChecked) RuleType.SPECIFIC else RuleType.GENERAL
        
        lifecycleScope.launch(Dispatchers.IO) {
            val savedRules = RuleGeneratorService.saveRules(currentRules, bookName, bookOrigin, scopeType)
            
            withContext(Dispatchers.Main) {
                if (savedRules.isNotEmpty()) {
                    val scopeText = if (scopeType == "source") "整个书源" else "本书"
                    val typeText = if (ruleType == RuleType.SPECIFIC) "特定" else "通用"
                    activity?.toastOnUi(
                        "已保存 ${savedRules.size} 条${typeText}规则到替换净化，作用于$scopeText"
                    )
                    
                    // 通知替换规则已更新
                    io.dushu.app.help.book.ContentProcessor.upReplaceRules()
                } else {
                    activity?.toastOnUi("规则已存在或未生成有效规则")
                }
                dismiss()
            }
        }
    }
}
