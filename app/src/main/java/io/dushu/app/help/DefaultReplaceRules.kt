package io.dushu.app.help

import io.dushu.app.data.appDb
import io.dushu.app.data.entities.ReplaceRule

/**
 * 默认替换规则管理器
 * 提供无意义内容过滤等常用规则的预置导入
 */
object DefaultReplaceRules {

    const val GROUP_AI_FILTER = "AI智能过滤"
    const val KEY_IMPORTED = "defaultReplaceRulesImported"

    /**
     * 无意义内容过滤规则列表
     */
    val aiFilterRules = listOf(
        ReplaceRule(
            name = "过滤-本章未完提示",
            pattern = "【.*?继续阅读.*?】|【本章.*?未完.*?】|【.*?点击下一页.*?】|本章.*?未完.*?点击继续阅读|本章未完.*下一页|本章未完待续|点击继续阅读本章.*|点击下一页继续阅读|【请点击.*继续阅读】|【请.*点击.*下一.*】",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-未完待续变体",
            pattern = "本章.*未完.*下一.*|本章.*未完.*继续.*|未完待续.*|本章.*待续",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-广告推广",
            pattern = "【.*广告.*】|【.*推广.*】|【.*推荐.*】",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-手机站提示",
            pattern = "【手机.*站.*】|【移动.*版.*】|【.*wap.*】",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-系统通知",
            pattern = "【重要通知.*】|【系统.*提示.*】|【作者.*说.*】",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-纯符号分隔线",
            pattern = "^[\\-=*─—]{3,}$",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-空括号",
            pattern = "【】|（）|\\[\\]",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-网址链接",
            pattern = "https?://\\S+|www\\.\\S+",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-小说网站通用提示",
            pattern = "『继续阅读』|『下一章』|『上一章』|点击阅读全文|查看完整章节|以下内容需要VIP|开通VIP继续阅读|登录后阅读|注册后继续阅读",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "过滤-阅读APP提示",
            pattern = "本章由.*提供|技术支持.*|内容来源于.*",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "修正-多余空行",
            pattern = "\\n{3,}",
            replacement = "\\n\\n",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        ),
        ReplaceRule(
            name = "修正-行首行尾空格",
            pattern = "^[\\s]+|[\\s]+$",
            replacement = "",
            group = GROUP_AI_FILTER,
            isRegex = true,
            scopeContent = true
        )
    )

    /**
     * 检查是否已经导入过默认规则
     */
    fun isImported(): Boolean {
        return appDb.replaceRuleDao.findEnabledByContentScope("", "")
            .any { it.group == GROUP_AI_FILTER }
    }

    /**
     * 导入默认的无意义内容过滤规则
     * @param force 是否强制重新导入（会删除旧规则）
     */
    fun importAiFilterRules(force: Boolean = false) {
        if (force) {
            // 删除旧的 AI 过滤规则
            appDb.replaceRuleDao.deleteByGroup(GROUP_AI_FILTER)
        }
        
        // 插入新规则
        aiFilterRules.forEach { rule ->
            appDb.replaceRuleDao.insert(rule)
        }
    }

    /**
     * 获取 AI 过滤规则数量
     */
    fun getAiFilterRuleCount(): Int {
        return appDb.replaceRuleDao.getByGroup(GROUP_AI_FILTER).size
    }

    /**
     * 启用/禁用 AI 过滤规则组
     */
    fun setAiFilterEnabled(enabled: Boolean) {
        appDb.replaceRuleDao.updateEnableByGroup(GROUP_AI_FILTER, enabled)
    }

    /**
     * 检查 AI 过滤规则组是否启用
     */
    fun isAiFilterEnabled(): Boolean {
        return appDb.replaceRuleDao.findEnabledByContentScope("", "")
            .any { it.group == GROUP_AI_FILTER && it.isEnabled }
    }
}
