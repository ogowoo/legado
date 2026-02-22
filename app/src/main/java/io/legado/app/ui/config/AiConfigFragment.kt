package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import io.legado.app.R
import io.legado.app.ai.AIProviderFactory
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.prefs.fragment.PreferenceFragment
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.lib.theme.primaryColor

/**
 * AI助手设置
 */
class AiConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_ai)
        initPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setEdgeEffectColor(primaryColor)
    }

    private fun initPreferences() {
        // 初始化AI提供商列表
        findPreference<ListPreference>(PreferKey.aiRepairProvider)?.let { pref ->
            val providers = AIProviderFactory.getAllProviders()
            pref.entries = providers.map { it.name }.toTypedArray()
            pref.entryValues = providers.map { it.providerId }.toTypedArray()
            pref.summary = providers.find { it.providerId == AppConfig.aiRepairProvider }?.name
                ?: providers.firstOrNull()?.name
        }

        // 更新API Key显示
        findPreference<EditTextPreference>(PreferKey.aiRepairApiKey)?.let { pref ->
            val apiKey = AppConfig.aiRepairApiKey
            pref.summary = if (apiKey.isNullOrBlank()) {
                getString(R.string.ai_repair_api_key_summary)
            } else {
                "*".repeat(apiKey.length)
            }
        }

        // 更新模型显示
        findPreference<EditTextPreference>(PreferKey.aiRepairModel)?.let { pref ->
            pref.summary = AppConfig.aiRepairModel?.takeIf { it.isNotBlank() }
                ?: getString(R.string.ai_repair_model_summary)
        }

        // 更新状态
        updateAiEnableStatus()
    }

    private fun updateAiEnableStatus() {
        val enabled = AppConfig.aiContentRepairEnabled
        val dependentKeys = arrayOf(
            PreferKey.aiRepairProvider,
            PreferKey.aiRepairApiKey,
            PreferKey.aiRepairApiUrl,
            PreferKey.aiRepairModel,
            PreferKey.aiRepairTemperature,
            PreferKey.aiRepairMaxTokens,
            PreferKey.aiRepairSystemPrompt,
            PreferKey.aiRepairMode,
            PreferKey.aiAutoSaveCache,
            PreferKey.aiPromptGenerateRules
        )
        
        dependentKeys.forEach { key ->
            findPreference<Preference>(key)?.isEnabled = enabled
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.aiContentRepairEnabled -> {
                updateAiEnableStatus()
            }
            PreferKey.aiRepairProvider -> {
                findPreference<ListPreference>(key)?.let { pref ->
                    val provider = AIProviderFactory.getProvider(pref.value)
                    pref.summary = provider?.name ?: pref.value
                }
            }
            PreferKey.aiRepairApiKey -> {
                findPreference<EditTextPreference>(key)?.let { pref ->
                    val value = pref.text
                    pref.summary = if (value.isNullOrBlank()) {
                        getString(R.string.ai_repair_api_key_summary)
                    } else {
                        "*".repeat(value.length)
                    }
                }
            }
            PreferKey.aiRepairModel -> {
                findPreference<EditTextPreference>(key)?.let { pref ->
                    pref.summary = pref.text?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.ai_repair_model_summary)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
            ?.unregisterOnSharedPreferenceChangeListener(this)
    }

}
