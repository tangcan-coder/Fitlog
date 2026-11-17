package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow

/**
 * 「我的」一级页：只保留「目标」「设置」两个入口。
 *
 * 原来的目标数字、食物库规模、记录天数、备份/恢复、清空数据都收进这两个入口：
 * 目标页只读展示当前目标，设置页放资料修改和备份相关操作。
 */
@Composable
fun MeScreen(
    data: AppData,
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenTarget: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val targets = targetsOf(data.profile)

    GlasmioPinnedListPage(
        title = "我的",
        subtitle = "目标与设置",
        listState = listState,
        bottomPadding = bottomPadding,
    ) {
        item(key = "entries") {
            GlasmioGroupedList {
                GlasmioListRow(
                    title = "目标",
                    subtitle = "每日 ${targets.kcal} kcal · 蛋白 ${targets.protein} g",
                    showTopSeparator = false,
                    onClick = onOpenTarget,
                    leading = { EmojiTile("🎯") },
                )
                GlasmioListRow(
                    title = "设置",
                    subtitle = "资料、备份与恢复",
                    onClick = onOpenSettings,
                    leading = { EmojiTile("⚙️") },
                )
            }
        }

        item(key = "footer") {
            Spacer(Modifier.height(8.dp))
            IosGroupFooter("FitLog · 本地版，数据不上传。")
        }
    }
}

/** 列表行左侧的小图标底：和食物库的分类图标保持同一种尺寸和圆角。 */
@Composable
private fun EmojiTile(emoji: String) {
    Box(
        Modifier
            .size(30.dp)
            .clip(ContinuousRoundedRectangle(9.dp))
            .background(GlasmioTheme.colors.systemGray5),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 15.sp)
    }
}
