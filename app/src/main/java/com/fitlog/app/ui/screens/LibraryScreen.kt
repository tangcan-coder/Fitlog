package com.fitlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.Cats
import com.fitlog.app.data.Food
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.domain.pretty
import com.fitlog.app.ui.components.CategoryChips
import com.fitlog.app.ui.components.EmptyHint
import com.fitlog.app.ui.components.FitSearchField
import com.fitlog.app.ui.components.IosGroupFooter
import com.fitlog.app.ui.components.IosSectionHeader
import com.fitlog.app.ui.components.PrimaryCapsule
import com.fitlog.app.ui.components.groupedRowShape
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.glasmioGroupedListBackground
import kotlin.math.roundToInt

private val SectionGap = 16.dp

/**
 * 「食物库」：搜索 + 分类胶囊 + iOS 分组列表。
 *
 * 列表行拆成 LazyColumn 的独立 item（组间距设为 0、每行自己拼圆角），
 * 这样食物很多时也只有屏幕上的几行会被组合/绘制，长列表不会拖慢滚动。
 */
@Composable
fun LibraryScreen(
    foods: List<Food>,
    listState: LazyListState,
    bottomPadding: Dp,
    onEdit: (String) -> Unit,
    onNew: () -> Unit,
    onBatch: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf(Cats.ALL) }

    val visible = foods
        .filter { cat == Cats.ALL || it.cat == cat }
        .filter { Pinyin.matches(it.name, it.alias, query) }
        .sortedWith(
            compareByDescending<Food> { it.uses }
                .thenByDescending { it.lastUsed }
                .thenBy { it.name },
        )

    GlasmioPinnedListPage(
        title = "",
        listState = listState,
        bottomPadding = bottomPadding,
        showsLargeTitle = false,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        actions = {
            GlasmioIconButton(onClick = onNew) {
                SfIcon("plus", "新建食物", size = 18.dp)
            }
        },
    ) {
        item(key = "filters") {
            Column(
                Modifier.padding(top = SectionGap),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FitSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "搜索 · 支持拼音首字母",
                )
                CategoryChips(
                    items = Cats.filters,
                    selected = cat,
                    onSelect = { cat = it },
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.padding(top = SectionGap)) {
                    EmptyHint(
                        text = if (foods.isEmpty()) {
                            "食物库还是空的\n把常吃的先录进来，以后点两下就能记录"
                        } else {
                            "没有匹配的食物"
                        },
                    )
                    if (foods.isEmpty()) {
                        PrimaryCapsule(
                            text = "新建第一个食物",
                            onClick = onNew,
                            modifier = Modifier.padding(horizontal = 48.dp),
                        )
                    }
                }
            }
        } else {
            itemsIndexed(visible, key = { _, food -> food.id }) { index, food ->
                GlasmioListRow(
                    title = food.name,
                    showTopSeparator = index > 0,
                    onClick = { onEdit(food.id) },
                    modifier = Modifier.glasmioGroupedListBackground(
                        groupedRowShape(
                            isFirst = index == 0,
                            isLast = index == visible.lastIndex,
                        ),
                    ),
                    leading = null,
                )
            }
        }

        item(key = "batch") {
            Column(Modifier.padding(top = SectionGap)) {
                IosSectionHeader("管理")
                GlasmioGroupedList {
                    GlasmioListRow(
                        "批量导入",
                        systemName = "arrow.down.circle",
                        showTopSeparator = false,
                        onClick = onBatch,
                    )
                }
            }
        }
    }
}