package io.dushu.app.model

import android.content.Context
import io.dushu.app.constant.IntentAction
import io.dushu.app.service.DownloadService
import io.dushu.app.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}