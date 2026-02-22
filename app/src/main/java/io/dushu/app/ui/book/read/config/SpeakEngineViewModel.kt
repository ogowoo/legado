package io.dushu.app.ui.book.read.config

import android.app.Application
import android.speech.tts.TextToSpeech
import io.dushu.app.base.BaseViewModel
import io.dushu.app.help.DefaultData

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultHttpTTS()
        }
    }

}