package io.dushu.app.ui.dict.rule

import android.app.Application
import io.dushu.app.base.BaseViewModel
import io.dushu.app.constant.AppLog
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.DictRule
import io.dushu.app.help.DefaultData
import io.dushu.app.utils.toastOnUi

class DictRuleViewModel(application: Application) : BaseViewModel(application) {


    fun update(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.update(*dictRule)
        }.onError {
            val msg = "更新字典规则出错\n${it.localizedMessage}"
            AppLog.put(msg, it)
            context.toastOnUi(msg)
        }
    }

    fun delete(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.delete(*dictRule)
        }.onError {
            val msg = "删除字典规则出错\n${it.localizedMessage}"
            AppLog.put(msg, it)
            context.toastOnUi(msg)
        }
    }

    fun upSortNumber() {
        execute {
            val rules = appDb.dictRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.sortNumber = index + 1
            }
            appDb.dictRuleDao.insert(*rules.toTypedArray())
        }
    }

    fun enableSelection(vararg dictRule: DictRule) {
        execute {
            val array = dictRule.map { it.copy(enabled = true) }.toTypedArray()
            appDb.dictRuleDao.insert(*array)
        }
    }

    fun disableSelection(vararg dictRule: DictRule) {
        execute {
            val array = dictRule.map { it.copy(enabled = false) }.toTypedArray()
            appDb.dictRuleDao.insert(*array)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultDictRules()
        }
    }

}