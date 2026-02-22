package io.dushu.app.lib.mobi.decompress

interface Decompressor {

    fun decompress(data: ByteArray): ByteArray

}
