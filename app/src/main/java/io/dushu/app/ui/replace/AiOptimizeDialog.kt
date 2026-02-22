package io.dushu.app.ui.replace

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import io.dushu.app.R
import io.dushu.app.ai.RuleOptimizeService
import io.dushu.app.data.appDb
import io.dushu.app.databinding.DialogAiOptimizeBinding
import io.dushu.app.ui.widget.dialog.BaseAiDialog
import io.dushu.app.utils.toastOnUi
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI规则优化对话框
 * 分析现有规则，识别相似规则并合并
 */
class AiOptimizeDialog : BaseAiDialog(R.layout.dialog_ai_optimize) {

    private val binding by viewBinding(DialogAiOptimizeBinding::bind)
    
    override val progressBar: ProgressBar? by lazy { binding.progressBar }
    override val tvStatus: TextView? by lazy { binding.tvStatus }
    override val contentView: View? by lazy { binding.contentContainer }

    companion object {
        const val TAG = "AiOptimizeDialog"
    }

    override fun initView() {
        setupView()
        startOptimization()
    }
    
    private fun setupView() {
        showLoading(getString(R.string.ai_optimizing))
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
                    hideLoading()
                    showError("没有启用的规则需要优化")
                    binding.btnCancel.text = getString(R.string.close)
                }
                return@launch
            }
            
            // 调用AI分析
            val result = RuleOptimizeService.analyzeAndMergeRules(allRules)
            
            withContext(Dispatchers.Main) {
                hideLoading()
                
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
                    io.dushu.app.help.book.ContentProcessor.upReplaceRules()
                } else {
                    activity?.toastOnUi("没有需要保存的规则")
                }
                dismiss()
            }
        }
    }

    private var optimizationResult: RuleOptimizeService.OptimizeResult? = null
}
