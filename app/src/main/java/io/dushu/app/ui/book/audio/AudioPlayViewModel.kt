package io.dushu.app.ui.book.audio

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.dushu.app.R
import io.dushu.app.base.BaseViewModel
import io.dushu.app.constant.AppLog
import io.dushu.app.constant.BookType
import io.dushu.app.constant.EventBus
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.Book
import io.dushu.app.data.entities.BookChapter
import io.dushu.app.data.entities.BookSource
import io.dushu.app.help.book.getBookSource
import io.dushu.app.help.book.removeType
import io.dushu.app.help.book.simulatedTotalChapterNum
import io.dushu.app.model.AudioPlay
import io.dushu.app.model.webBook.WebBook
import io.dushu.app.utils.postEvent
import io.dushu.app.utils.toastOnUi

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {
    val titleData = MutableLiveData<String>()
    val coverData = MutableLiveData<String>()
    val customBtnListData = MutableLiveData<Boolean>()

    fun initData(intent: Intent, success: (() -> Unit)) = AudioPlay.apply {
        execute {
            inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            val bookUrl = intent.getStringExtra("bookUrl") ?: book?.bookUrl ?: return@execute
            val targetBook = appDb.bookDao.getBook(bookUrl) ?: run {
                inBookshelf = false
                book?.also { appDb.bookDao.insert(it) } ?: return@execute
            }
            initBook(targetBook)
        }.onSuccess {
            success.invoke()
        }.onFinally {
            saveRead(true)
        }
    }

    private suspend fun initBook(book: Book) {
        val isSameBook = AudioPlay.book?.bookUrl == book.bookUrl
        if (isSameBook) {
            AudioPlay.upData(book)
        } else {
            AudioPlay.resetData(book)
        }
        customBtnListData.postValue(AudioPlay.bookSource?.customButton == true)
        titleData.postValue(book.name)
        coverData.postValue(book.getDisplayCover())
        if (book.tocUrl.isEmpty() && !loadBookInfo(book)) {
            return
        }
        if (AudioPlay.chapterSize == 0 && !loadChapterList(book)) {
            return
        }
    }

    private suspend fun loadBookInfo(book: Book): Boolean {
        val bookSource = AudioPlay.bookSource ?: return true
        try {
            WebBook.getBookInfoAwait(bookSource, book)
            return true
        } catch (e: Exception) {
            AppLog.put("详情页出错: ${e.localizedMessage}", e, true)
            return false
        }
    }

    private suspend fun loadChapterList(book: Book): Boolean {
        val bookSource = AudioPlay.bookSource ?: return true
        try {
            val oldBook = book.copy()
            val cList = WebBook.getChapterListAwait(bookSource, book).getOrThrow()
            if (oldBook.bookUrl == book.bookUrl) {
                appDb.bookDao.update(book)
            } else {
                appDb.bookDao.replace(oldBook, book)
            }
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookChapterDao.insert(*cList.toTypedArray())
            AudioPlay.chapterSize = cList.size
            AudioPlay.simulatedChapterSize = book.simulatedTotalChapterNum()
            AudioPlay.upDurChapter()
            return true
        } catch (_: Exception) {
            context.toastOnUi(R.string.error_load_toc)
            return false
        }
    }

    fun upSource() {
        execute {
            val book = AudioPlay.book ?: return@execute
            AudioPlay.bookSource = book.getBookSource()?.also{
                customBtnListData.postValue(it.customButton)
            }
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            AudioPlay.book?.delete()
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter()
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}