package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow

/**
 * 「我的」一级页：只保留「目标」「设置」两个入口。
 *
 * 标题、图标、副标题、页脚全部去掉——这一页就是一张两行的列表，
 * 减少信息量，把细节交给二级页面。
 */
@Composable
fun MeScreen(
    listState: LazyListState,
    bottomPadding: Dp,
    onOpenTarget: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(GlasmioTheme.colors.groupedBackground),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 28.dp,
                bottom = bottomPadding + 24.dp,
            ),
        ) {
            item(key = "entries") {
                GlasmioGroupedList {
                    GlasmioListRow(
                        title = "目标",
                        showTopSeparator = false,
                        onClick = onOpenTarget,
                    )
                    GlasmioListRow(
                        title = "设置",
                        onClick = onOpenSettings,
                    )
                }
            }
        }
    }
}
