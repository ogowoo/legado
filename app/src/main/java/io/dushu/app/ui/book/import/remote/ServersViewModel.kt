package io.dushu.app.ui.book.import.remote

import android.app.Application
import io.dushu.app.base.BaseViewModel
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}