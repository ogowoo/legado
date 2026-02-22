package io.dushu.app.ui.main.explore

import android.app.Application
import io.dushu.app.base.BaseViewModel
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.BookSourcePart
import io.dushu.app.help.config.SourceConfig
import io.dushu.app.help.source.SourceHelp

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

}