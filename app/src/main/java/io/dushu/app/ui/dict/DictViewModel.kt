package io.dushu.app.ui.dict

import android.app.Application
import io.dushu.app.base.BaseViewModel
import io.dushu.app.constant.AppLog
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.DictRule
import io.dushu.app.help.coroutine.Coroutine

class DictViewModel(application: Application) : BaseViewModel(application) {

    private var dictJob: Coroutine<String>? = null

    fun initData(onSuccess: (List<DictRule>) -> Unit) {
        execute {
            appDb.dictRuleDao.enabled
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun dict(
        dictRule: DictRule,
        word: String,
        onFinally: (String) -> Unit
    ) {
        dictJob?.cancel()
        dictJob = execute {
            dictRule.search(word)
        }.onSuccess {
            onFinally.invoke(it)
        }.onError {
            onFinally.invoke(it.localizedMessage ?: "ERROR")
        }
    }

    fun onButtonClick(
        dictRule: DictRule,
        name: String,
        click: String?
    ) {
        if (click.isNullOrBlank()) {
            return
        }
        execute {
            dictRule.buttonClick(name, click)
        }.onError {
            AppLog.put("$name button click error\n${it.localizedMessage}", it)
        }
    }

}