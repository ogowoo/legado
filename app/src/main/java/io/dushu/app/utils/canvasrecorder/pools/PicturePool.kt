package io.dushu.app.utils.canvasrecorder.pools

import android.graphics.Picture
import io.dushu.app.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
