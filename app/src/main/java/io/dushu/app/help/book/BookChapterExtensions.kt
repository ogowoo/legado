@file:Suppress("unused")

package io.dushu.app.help.book

import io.dushu.app.data.entities.BookChapter
import io.dushu.app.help.RuleBigDataHelp.getDanmakuFile

fun BookChapter.getDanmaku(): Any? { //读取弹幕数据
    return variableMap["danmaku"] ?: getDanmakuFile(bookUrl, url)
}