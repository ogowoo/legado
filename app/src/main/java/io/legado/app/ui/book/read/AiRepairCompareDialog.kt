package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.ai.ContentRepairService
import io.legado.app.databinding.DialogAiRepairCompareBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI 内容修正对比弹窗
 * 长按后显示修正前后的对比
 */
class AiRepairCompareDialog : DialogFragment(R.layout.dialog_ai_repair_compare) {

    private val binding by viewBinding(DialogAiRepairCompareBinding::bind)
    private var originalText: String = ""
    private var contextText: String = ""

    companion object {
        const val TAG = "AiRepairCompareDialog"
        private const val ARG_ORIGINAL_TEXT = "original_text"
        private const val ARG_CONTEXT_TEXT = "context_text"

        fun newInstance(originalText: String, contextText: String): AiRepairCompareDialog {
            return AiRepairCompareDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORIGINAL_TEXT, originalText)
                    putString(ARG_CONTEXT_TEXT, contextText)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setBackgroundColor(backgroundColor)
        
        originalText = arguments?.getString(ARG_ORIGINAL_TEXT) ?: ""
        contextText = arguments?.getString(ARG_CONTEXT_TEXT) ?: ""
        
        initView()
        performAiRepair()
    }

    private fun initView() {
        binding.tvOriginal.text = originalText
        binding.progressBar.visibility = View.VISIBLE
        binding.tvRepaired.visibility = View.GONE
        binding.tvDiff.visibility = View.GONE
        
        binding.btnClose.setOnClickListener {
            dismiss()
        }
        
        binding.btnApply.setOnClickListener {
            // 通知外部应用修正
            val repaired = binding.tvRepaired.text.toString()
            (activity as? CallBack)?.onAiRepairApply(originalText, repaired)
            dismiss()
        }
        
        binding.btnGenerateRules.setOnClickListener {
            // 打开规则生成对话框
            val repaired = binding.tvRepaired.text.toString()
            (activity as? CallBack)?.onAiRepairGenerateRules(originalText, repaired)
            dismiss()
        }
        
        binding.btnApply.visibility = View.GONE
        binding.btnGenerateRules.visibility = View.GONE
    }

    private fun performAiRepair() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repairedText = ContentRepairService.repair(contextText, originalText)
                
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvRepaired.visibility = View.VISIBLE
                    binding.tvRepaired.text = repairedText
                    
                    // 计算差异
                    val diff = calculateDiff(originalText, repairedText)
                    if (diff.isNotEmpty()) {
                        binding.tvDiff.visibility = View.VISIBLE
                        binding.tvDiff.text = diff
                        binding.btnApply.visibility = View.VISIBLE
                        binding.btnGenerateRules.visibility = View.VISIBLE
                    } else {
                        binding.tvDiff.visibility = View.VISIBLE
                        binding.tvDiff.text = "【无需修正】文本没有错误"
                        binding.btnApply.visibility = View.GONE
                        binding.btnGenerateRules.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvRepaired.visibility = View.VISIBLE
                    binding.tvRepaired.text = "修正失败: ${e.message}"
                    binding.btnApply.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 计算文本差异
     */
    private fun calculateDiff(original: String, repaired: String): String {
        val differences = mutableListOf<String>()
        
        // 简单的逐字对比
        val maxLen = maxOf(original.length, repaired.length)
        var i = 0
        while (i < maxLen) {
            val origChar = original.getOrNull(i)
            val repairedChar = repaired.getOrNull(i)
            
            if (origChar != repairedChar) {
                if (origChar != null && repairedChar != null) {
                    differences.add("【$origChar】→【$repairedChar】")
                } else if (origChar != null) {
                    differences.add("删除【$origChar】")
                } else if (repairedChar != null) {
                    differences.add("添加【$repairedChar】")
                }
            }
            i++
        }
        
        return if (differences.isEmpty()) {
            ""
        } else {
            "修正位置: " + differences.take(5).joinToString(", ") +
            if (differences.size > 5) " 等${differences.size}处" else ""
        }
    }

    interface CallBack {
        /**
         * 应用AI修正
         */
        fun onAiRepairApply(originalText: String, repairedText: String)
        
        /**
         * 生成替换规则
         */
        fun onAiRepairGenerateRules(originalText: String, repairedText: String)
    }
}