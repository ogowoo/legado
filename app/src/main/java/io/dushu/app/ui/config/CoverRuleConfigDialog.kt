package io.dushu.app.ui.config

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.dushu.app.R
import io.dushu.app.base.BaseDialogFragment
import io.dushu.app.databinding.DialogCoverRuleConfigBinding
import io.dushu.app.lib.theme.primaryColor
import io.dushu.app.model.BookCover
import io.dushu.app.utils.GSON
import io.dushu.app.utils.setLayout
import io.dushu.app.utils.toastOnUi
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onClick

class CoverRuleConfigDialog : BaseDialogFragment(R.layout.dialog_cover_rule_config) {

    val binding by viewBinding(DialogCoverRuleConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        initData()
        binding.tvCancel.onClick {
            dismissAllowingStateLoss()
        }
        binding.tvOk.onClick {
            val enable = binding.cbEnable.isChecked
            val searchUrl = binding.editSearchUrl.text?.toString()
            val coverRule = binding.editCoverUrlRule.text?.toString()
            if (searchUrl.isNullOrBlank() || coverRule.isNullOrBlank()) {
                toastOnUi("搜索url和cover规则不能为空")
            } else {
                BookCover.CoverRule(enable, searchUrl, coverRule).let { config ->
                    BookCover.saveCoverRule(config)
                }
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.onClick {
            BookCover.delCoverRule()
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
        lifecycleScope.launch {
            val rule = withContext(IO) {
                BookCover.getCoverRule()
            }
            Log.e("coverRule", GSON.toJson(rule))
            binding.cbEnable.isChecked = rule.enable
            binding.editSearchUrl.setText(rule.searchUrl)
            binding.editCoverUrlRule.setText(rule.coverRule)
        }
    }

}