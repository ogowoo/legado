package io.dushu.app.ui.rss.favorites


import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.dushu.app.R
import io.dushu.app.base.VMBaseFragment
import io.dushu.app.constant.AppLog
import io.dushu.app.data.appDb
import io.dushu.app.data.entities.RssStar
import io.dushu.app.databinding.FragmentRssArticlesBinding
import io.dushu.app.lib.dialogs.alert
import io.dushu.app.lib.theme.primaryColor
import io.dushu.app.ui.rss.read.ReadRss
import io.dushu.app.ui.widget.recycler.VerticalDivider
import io.dushu.app.utils.applyNavigationBarPadding
import io.dushu.app.utils.setEdgeEffectColor
import io.dushu.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssFavoritesFragment() : VMBaseFragment<RssFavoritesViewModel>(R.layout.fragment_rss_articles),
    RssFavoritesAdapter.CallBack {

    constructor(group: String) : this() {
        arguments = Bundle().apply {
            putString("group", group)
        }
    }

    private val binding by viewBinding(FragmentRssArticlesBinding::bind)
    override val viewModel by viewModels<RssFavoritesViewModel>()
    private val adapter: RssFavoritesAdapter by lazy {
        RssFavoritesAdapter(requireContext(), this@RssFavoritesFragment)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        loadArticles()
    }

    private fun initView() = binding.run {
        refreshLayout.isEnabled = false
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.layoutManager = run {
            recyclerView.addItemDecoration(VerticalDivider(requireContext()))
            LinearLayoutManager(requireContext())
        }
        recyclerView.adapter = adapter
        recyclerView.applyNavigationBarPadding()
    }

    private fun loadArticles() {
        lifecycleScope.launch {
            val group = arguments?.getString("group") ?: "默认分组"
            appDb.rssStarDao.flowByGroup(group).catch {
                AppLog.put("订阅文章界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        ReadRss.readRss(this, rssStar.toRssArticle())
    }

    override fun delStar(rssStar: RssStar) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n<" + rssStar.title + ">")
            noButton()
            yesButton {
                appDb.rssStarDao.delete(rssStar.origin, rssStar.link)
            }
        }
    }
}
