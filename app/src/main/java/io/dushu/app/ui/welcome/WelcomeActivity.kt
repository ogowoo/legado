package io.dushu.app.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.postDelayed
import io.dushu.app.base.BaseActivity
import io.dushu.app.constant.PreferKey
import io.dushu.app.constant.Theme
import io.dushu.app.data.appDb
import io.dushu.app.databinding.ActivityWelcomeBinding
import io.dushu.app.help.config.AppConfig
import io.dushu.app.help.config.ThemeConfig
import io.dushu.app.lib.theme.accentColor
import io.dushu.app.lib.theme.backgroundColor
import io.dushu.app.ui.book.read.ReadBookActivity
import io.dushu.app.ui.main.MainActivity
import io.dushu.app.utils.BitmapUtils
import io.dushu.app.utils.fullScreen
import io.dushu.app.utils.getPrefBoolean
import io.dushu.app.utils.getPrefInt
import io.dushu.app.utils.getPrefString
import io.dushu.app.utils.setStatusBarColorAuto
import io.dushu.app.utils.startActivity
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import io.dushu.app.utils.visible
import io.dushu.app.utils.windowSize

open class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {

    override val binding by viewBinding(ActivityWelcomeBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT != 0) {
            // 避免从桌面启动程序后，会重新实例化入口类的activity
            finish()
        } else {
            // 直接跳转到主界面，不显示闪屏
            startMainActivity()
        }
    }

    override fun setupSystemBar() {
        fullScreen()
        setStatusBarColorAuto(backgroundColor, true, fullScreen)
        upNavigationBarColor()
    }

    override fun upBackgroundImage() {
        if (getPrefBoolean(PreferKey.customWelcome)) {
            kotlin.runCatching {
                when (ThemeConfig.getTheme()) {
                    Theme.Dark -> {
                        getPrefString(PreferKey.welcomeImageDark)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowTextDark)
                        binding.ivBook.visible(AppConfig.welcomeShowIconDark)
                        binding.tvGzh.visible(AppConfig.welcomeShowTextDark)
                        return
                    }
                    else -> {
                        getPrefString(PreferKey.welcomeImage)?.let { path ->
                            if (path.endsWith(".9.png")) {
                                BitmapUtils.decodeNinePatchDrawable(path)?.let {
                                    window.decorView.background = it
                                }
                            } else {
                                val size = windowManager.windowSize
                                BitmapUtils.decodeBitmap(path, size.widthPixels, size.heightPixels)?.let {
                                    window.decorView.background = it.toDrawable(resources)
                                }
                            }
                        }
                        binding.tvLegado.visible(AppConfig.welcomeShowText)
                        binding.ivBook.visible(AppConfig.welcomeShowIcon)
                        binding.tvGzh.visible(AppConfig.welcomeShowText)
                        return
                    }
                }
            }
        }
        super.upBackgroundImage()
    }

    private fun startMainActivity() {
        startActivity<MainActivity>()
        if (getPrefBoolean(PreferKey.defaultToRead) && appDb.bookDao.lastReadBook != null) {
            startActivity<ReadBookActivity>()
        }
        finish()
    }

}

class Launcher1 : WelcomeActivity()
class Launcher2 : WelcomeActivity()
class Launcher3 : WelcomeActivity()
class Launcher4 : WelcomeActivity()
class Launcher5 : WelcomeActivity()
class Launcher6 : WelcomeActivity()
class Launcher7 : WelcomeActivity()