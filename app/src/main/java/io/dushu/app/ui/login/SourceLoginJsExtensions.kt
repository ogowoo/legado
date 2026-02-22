package io.dushu.app.ui.login

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.dushu.app.R
import io.dushu.app.constant.BookType
import io.dushu.app.constant.EventBus
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.BaseSource
import io.dushu.app.data.entities.Book
import io.dushu.app.data.entities.BookChapter
import io.dushu.app.data.entities.HttpTTS
import io.dushu.app.model.AudioPlay
import io.dushu.app.model.ReadAloud
import io.dushu.app.model.ReadBook
import io.dushu.app.model.VideoPlay
import io.dushu.app.model.analyzeRule.AnalyzeRule
import io.dushu.app.model.analyzeRule.AnalyzeRule.Companion.setChapter
import io.dushu.app.ui.rss.read.RssJsExtensions
import io.dushu.app.ui.widget.dialog.BottomWebViewDialog
import io.dushu.app.utils.FileUtils
import io.dushu.app.utils.postEvent
import io.dushu.app.utils.sendToClip
import io.dushu.app.utils.showDialogFragment
import io.dushu.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

@Suppress("unused")
class SourceLoginJsExtensions(
    activity: AppCompatActivity?, source: BaseSource?,
    private val bookType: Int = 0,
    callback: Callback? = null
) : RssJsExtensions(activity, source) {
    private val callbackRef: WeakReference<Callback> = WeakReference(callback)
    interface Callback {
        fun upUiData(data: Map<String, Any?>?)
        fun reUiView(deltaUp: Boolean = false)
    }

    fun upLoginData(data: Map<String, Any?>?) {
        callbackRef.get()?.upUiData(data)
    }

    @JvmOverloads
    fun reLoginView(deltaUp: Boolean = false) {
        callbackRef.get()?.reUiView(deltaUp)
    }

    fun refreshExplore() {
        callbackRef.get()?.reUiView()
    }

    fun refreshBookInfo() {
        postEvent(EventBus.REFRESH_BOOK_INFO, true)
    }

    fun refreshBookToc() {
        postEvent(EventBus.REFRESH_BOOK_TOC, true)
    }

    fun refreshContent() {
        postEvent(EventBus.REFRESH_BOOK_CONTENT, true)
    }

    fun copyText(text: String) {
        activityRef.get()?.sendToClip(text)
    }

    fun clearTtsCache() {
        if (getSource() !is HttpTTS) return
        val activity = activityRef.get() ?: return
        activity.lifecycleScope.launch(IO) {
            ReadAloud.upReadAloudClass()
            val ttsFolderPath = "${activity.cacheDir.absolutePath}${File.separator}httpTTS${File.separator}"
            FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
                FileUtils.delete(it.absolutePath)
            }
            activity.toastOnUi(R.string.clear_cache_success)
        }
    }

    @JvmOverloads
    fun showBrowser(url: String, html: String? = null, preloadJs: String? = null, config: String? = null) {
        val activity = activityRef.get() ?: return
        val source = getSource() ?: return
        activity.showDialogFragment(
            BottomWebViewDialog(
                source.getKey(),
                bookType,
                url,
                html,
                preloadJs,
                config
            )
        )
    }

    private val bookAndChapter by lazy {
        var book: Book? = null
        var chapter: BookChapter? = null
        when (bookType) {
            BookType.text -> {
                book = ReadBook.book?.also {
                    chapter = appDb.bookChapterDao.getChapter(
                        it.bookUrl,
                        ReadBook.durChapterIndex
                    )
                }
            }

            BookType.audio -> {
                book = AudioPlay.book
                chapter = AudioPlay.durChapter
            }

            BookType.video -> {
                book = VideoPlay.book
                chapter = VideoPlay.chapter
            }
        }
        Pair(book, chapter)
    }
    private val book: Book? get() = bookAndChapter.first
    private val chapter: BookChapter? get() = bookAndChapter.second

    override val analyzeRule by lazy {
        AnalyzeRule(book, source = getSource()).setChapter(chapter)
    }

}