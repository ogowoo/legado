package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.launch

/**
 * AI对话框基类
 * 统一AI相关对话框的样式和行为
 */
abstract class BaseAiDialog(@LayoutRes private val layoutId: Int) : DialogFragment(layoutId) {

    // 子类需要实现的视图引用
    protected abstract val progressBar: ProgressBar?
    protected abstract val tvStatus: TextView?
    protected abstract val contentView: View?

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.75f)
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
        view.setBackgroundColor(backgroundColor)
        
        // 检查AI配置
        if (!checkAiConfig()) {
            return
        }
        
        initView()
    }

    /**
     * 子类初始化视图
     */
    protected abstract fun initView()

    /**
     * 检查AI配置
     */
    protected open fun checkAiConfig(): Boolean {
        if (!AppConfig.aiContentRepairEnabled) {
            showError("请先启用并配置AI功能\n设置 → AI助手设置")
            return false
        }
        return true
    }

    /**
     * 显示加载状态
     */
    protected fun showLoading(message: String = "AI处理中...") {
        progressBar?.isVisible = true
        progressBar?.alpha = 0f
        progressBar?.animate()?.alpha(1f)?.setDuration(200)?.start()
        
        tvStatus?.text = message
        tvStatus?.isVisible = true
        
        contentView?.isVisible = false
    }

    /**
     * 隐藏加载状态
     */
    protected fun hideLoading() {
        progressBar?.isVisible = false
        tvStatus?.isVisible = false
        contentView?.isVisible = true
    }

    /**
     * 显示错误信息
     */
    protected fun showError(message: String) {
        lifecycleScope.launch {
            hideLoading()
            tvStatus?.text = message
            tvStatus?.isVisible = true
            tvStatus?.setTextColor(requireContext().getColor(R.color.error))
        }
    }

    /**
     * 显示成功状态
     */
    protected fun showSuccess(message: String = "处理完成") {
        tvStatus?.text = message
        tvStatus?.isVisible = true
        tvStatus?.setTextColor(requireContext().getColor(R.color.success))
    }

    /**
     * 统一的AI配置错误提示
     */
    protected fun showAiConfigError() {
        toastOnUi("AI功能未配置，请先设置API Key")
    }

    /**
     * 统一的网络错误提示
     */
    protected fun showNetworkError() {
        toastOnUi("网络请求失败，请检查网络连接")
    }

    /**
     * 统一的AI处理超时提示
     */
    protected fun showTimeoutError() {
        toastOnUi("AI处理超时，请稍后重试")
    }
}

/**
 * AI对话框状态
 */
sealed class AiDialogState {
    object Idle : AiDialogState()
    object Loading : AiDialogState()
    data class Success(val message: String) : AiDialogState()
    data class Error(val message: String) : AiDialogState()
}
