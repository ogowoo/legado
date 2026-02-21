package io.legado.app.ui.book.read

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.ai.RuleGeneratorService
import io.legado.app.databinding.DialogAiRuleGeneratorBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI规则生成对话框
 * 在AI修正后询问用户是否生成替换规则
 */
class AiRuleGeneratorDialog : DialogFragment(R.layout.dialog_ai_rule_generator) {

    private val binding by viewBinding(DialogAiRuleGeneratorBinding::bind)
    
    private var originalText: String = ""
    private var repairedText: String = ""
    private var bookName: String = ""
    private var bookOrigin: String = ""

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

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.7f)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.setCanceledOnTouchOutside(false)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(backgroundColor)
        
        originalText = arguments?.getString(ARG_ORIGINAL_TEXT) ?: ""
        repairedText = arguments?.getString(ARG_REPAIRED_TEXT) ?: ""
        bookName = arguments?.getString(ARG_BOOK_NAME) ?: ""
        bookOrigin = arguments?.getString(ARG_BOOK_ORIGIN) ?: ""
        
        initView()
        generateRules()
    }

    private fun initView() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在分析文本差异，生成替换规则..."
        binding.rgScope.visibility = View.GONE
        binding.btnSave.visibility = View.GONE
        binding.tvRules.visibility = View.GONE
        
        // 默认选中"仅本书"
        binding.rbBookScope.isChecked = true
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnSave.setOnClickListener {
            saveRules()
        }
    }

    private fun generateRules() {
        lifecycleScope.launch(Dispatchers.IO) {
            val rules = RuleGeneratorService.generateRules(originalText, repairedText)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                
                if (rules.isEmpty()) {
                    binding.tvStatus.text = "未检测到可通用的替换规则\n（差异可能是特定上下文导致的，不适合生成通用规则）"
                    binding.btnSave.visibility = View.GONE
                    binding.rgScope.visibility = View.GONE
                } else {
                    binding.tvStatus.text = "已生成 ${rules.size} 条替换规则"
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
        
        lifecycleScope.launch(Dispatchers.IO) {
            val rules = RuleGeneratorService.generateRules(originalText, repairedText)
            val savedRules = RuleGeneratorService.saveRules(rules, bookName, bookOrigin, scopeType)
            
            withContext(Dispatchers.Main) {
                if (savedRules.isNotEmpty()) {
                    val scopeText = if (scopeType == "source") "整个书源" else "本书"
                    activity?.toastOnUi(
                        "已保存 ${savedRules.size} 条规则到替换净化，作用于$scopeText"
                    )
                    
                    // 通知替换规则已更新
                    io.legado.app.help.book.ContentProcessor.upReplaceRules()
                } else {
                    activity?.toastOnUi("规则已存在或未生成有效规则")
                }
                dismiss()
            }
        }
    }
}
