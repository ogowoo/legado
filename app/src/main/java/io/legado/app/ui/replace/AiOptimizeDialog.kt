package io.legado.app.ui.replace

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.ai.RuleOptimizeService
import io.legado.app.data.appDb
import io.legado.app.databinding.DialogAiOptimizeBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI规则优化对话框
 * 分析现有规则，识别相似规则并合并
 */
class AiOptimizeDialog : DialogFragment(R.layout.dialog_ai_optimize) {

    private val binding by viewBinding(DialogAiOptimizeBinding::bind)

    companion object {
        const val TAG = "AiOptimizeDialog"
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
        
        initView()
        startOptimization()
    }

    private fun initView() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.ai_optimizing)
        binding.tvResult.visibility = View.GONE
        binding.btnApply.visibility = View.GONE
        binding.btnCancel.visibility = View.VISIBLE
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        binding.btnApply.setOnClickListener {
            applyOptimization()
        }
    }

    private fun startOptimization() {
        lifecycleScope.launch(Dispatchers.IO) {
            // 获取所有启用的规则
            val allRules = appDb.replaceRuleDao.allEnabled
            
            if (allRules.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "没有启用的规则需要优化"
                    binding.btnCancel.text = getString(R.string.close)
                }
                return@launch
            }
            
            // 调用AI分析
            val result = RuleOptimizeService.analyzeAndMergeRules(allRules)
            
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                
                if (result.mergedRules.isEmpty()) {
                    binding.tvStatus.text = getString(R.string.ai_optimize_no_change)
                    binding.btnCancel.text = getString(R.string.close)
                } else {
                    binding.tvStatus.text = getString(
                        R.string.ai_optimize_complete,
                        result.mergedRules.size
                    )
                    binding.tvResult.visibility = View.VISIBLE
                    binding.btnApply.visibility = View.VISIBLE
                    
                    // 显示优化结果
                    val resultText = StringBuilder()
                    resultText.appendLine("发现 ${result.removedRules.size} 条规则可合并：")
                    result.removedRules.forEach { name ->
                        resultText.appendLine("  • $name")
                    }
                    resultText.appendLine()
                    resultText.appendLine("生成 ${result.mergedRules.size} 条优化规则：")
                    result.mergedRules.forEachIndexed { index, rule ->
                        resultText.appendLine("${index + 1}. ${rule.name}")
                        resultText.appendLine("   模式: ${rule.pattern}")
                    }
                    
                    if (result.suggestions.isNotEmpty()) {
                        resultText.appendLine()
                        resultText.appendLine("优化建议：")
                        result.suggestions.forEach { suggestion ->
                            resultText.appendLine("  • $suggestion")
                        }
                    }
                    
                    binding.tvResult.text = resultText.toString()
                }
            }
            
            // 保存结果供后续使用
            optimizationResult = result
        }
    }

    private fun applyOptimization() {
        val result = optimizationResult ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            val savedCount = RuleOptimizeService.applyOptimization(result)
            
            withContext(Dispatchers.Main) {
                if (savedCount > 0) {
                    activity?.toastOnUi("已成功应用优化，保存了 $savedCount 条规则")
                    // 通知替换规则已更新
                    io.legado.app.help.book.ContentProcessor.upReplaceRules()
                } else {
                    activity?.toastOnUi("没有需要保存的规则")
                }
                dismiss()
            }
        }
    }

    private var optimizationResult: RuleOptimizeService.OptimizeResult? = null
}
