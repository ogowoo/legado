package io.dushu.app.ui.ai

import android.os.Bundle
import androidx.activity.viewModels
import io.dushu.app.R
import io.dushu.app.base.VMBaseActivity
import io.dushu.app.constant.PreferKey
import io.dushu.app.databinding.ActivityAiCenterBinding
import io.dushu.app.help.config.AppConfig
import io.dushu.app.utils.putPrefBoolean
import splitties.init.appCtx
import io.dushu.app.ui.config.ConfigActivity
import io.dushu.app.ui.config.ConfigTag
import io.dushu.app.ui.replace.ReplaceRuleActivity
import io.dushu.app.utils.startActivity
import io.dushu.app.utils.toastOnUi
import io.dushu.app.utils.viewbindingdelegate.viewBinding

/**
 * AI助手中心
 * 集中展示和管理所有AI功能
 */
class AiCenterActivity : VMBaseActivity<ActivityAiCenterBinding, AiCenterViewModel>() {

    override val binding by viewBinding(ActivityAiCenterBinding::inflate)
    override val viewModel by viewModels<AiCenterViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
        // 标题栏返回按钮
        binding.titleBar.setNavigationOnClickListener {
            finish()
        }

        // AI功能总开关
        binding.switchAiEnable.isChecked = AppConfig.aiContentRepairEnabled
        binding.switchAiEnable.setOnCheckedChangeListener { _, isChecked ->
            appCtx.putPrefBoolean(PreferKey.aiContentRepair, isChecked)
            updateUiState(isChecked)
        }

        // AI内容修复卡片
        binding.cardAiRepair.setOnClickListener {
            if (!checkAiEnabled()) return@setOnClickListener
            toastOnUi("在阅读界面长按文字，选择\"AI修复\"使用")
        }

        // 替换规则助手卡片
        binding.cardAiRule.setOnClickListener {
            if (!checkAiEnabled()) return@setOnClickListener
            startActivity<ReplaceRuleActivity>()
        }

        // 正则生成卡片
        binding.cardAiRegex.setOnClickListener {
            if (!checkAiEnabled()) return@setOnClickListener
            toastOnUi("在替换规则编辑页面，点击AI按钮使用")
        }

        // 设置按钮
        binding.btnSettings.setOnClickListener {
            startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.AI_CONFIG)
            }
        }

        // 初始状态
        updateUiState(AppConfig.aiContentRepairEnabled)
    }

    private fun updateUiState(enabled: Boolean) {
        binding.cardAiRepair.alpha = if (enabled) 1.0f else 0.5f
        binding.cardAiRule.alpha = if (enabled) 1.0f else 0.5f
        binding.cardAiRegex.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun checkAiEnabled(): Boolean {
        if (!AppConfig.aiContentRepairEnabled) {
            toastOnUi("请先启用AI功能")
            return false
        }
        return true
    }

}
