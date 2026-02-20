package io.legado.app.ui.config

import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import io.legado.app.R
import io.legado.app.ai.repair.AIContentRepairService
import io.legado.app.ai.repair.AIProviderType
import io.legado.app.ai.repair.CustomProvider
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.prefs.NameListPreference
import io.legado.app.utils.launch
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI 内容修复设置界面
 */
class AIRepairConfigFragment : BasePreferenceFragment(R.xml.pref_config_ai_repair),
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindPreferenceListeners()
        updatePreferenceSummaries()
    }

    private fun bindPreferenceListeners() {
        findPreference<SwitchPreference>(PreferKey.aiContentRepair)?.onPreferenceChangeListener = this
        findPreference<NameListPreference>(PreferKey.aiRepairProviderType)?.onPreferenceChangeListener = this
        findPreference<EditTextPreference>(PreferKey.aiRepairApiKey)?.onPreferenceChangeListener = this
        findPreference<EditTextPreference>(PreferKey.aiRepairApiUrl)?.onPreferenceChangeListener = this
        findPreference<EditTextPreference>(PreferKey.aiRepairModel)?.onPreferenceChangeListener = this
        findPreference<SwitchPreference>(PreferKey.aiContentRepairCache)?.onPreferenceChangeListener = this
    }

    private fun updatePreferenceSummaries() {
        findPreference<EditTextPreference>(PreferKey.aiRepairApiKey)?.let {
            val apiKey = AppConfig.aiRepairApiKey
            it.summary = if (apiKey.isNullOrBlank()) {
                getString(R.string.ai_repair_api_key_summary)
            } else {
                "已配置: ${apiKey.take(8)}...${apiKey.takeLast(4)}"
            }
        }

        findPreference<EditTextPreference>(PreferKey.aiRepairModel)?.let {
            val model = AppConfig.aiRepairModel
            it.summary = if (model.isNullOrBlank()) {
                "使用默认模型"
            } else {
                "当前: $model"
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            PreferKey.aiRepairApiKey -> {
                val apiKey = newValue as? String
                preference.summary = if (apiKey.isNullOrBlank()) {
                    getString(R.string.ai_repair_api_key_summary)
                } else {
                    "已配置: ${apiKey.take(8)}...${apiKey.takeLast(4)}"
                }
            }
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        return true
    }

    override fun onResume() {
        super.onResume()
        updatePreferenceSummaries()
    }
}
