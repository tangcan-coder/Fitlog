package com.fitlog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.AppData
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Meal
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.pretty
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.domain.totalsOf
import com.fitlog.app.ui.components.NutritionRingsCard
import com.fitlog.app.ui.components.TinyAddButton
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.scaffold.GlasmioPinnedListPage
import io.github.neoruaa.glasmio.ui.GlasmioButton
import io.github.neoruaa.glasmio.ui.GlasmioButtonSize
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import kotlin.math.roundToInt

/**
 * 「今日」：iOS 健康风格的一页。
 * 热量环 + 三项宏量 + 四个餐次的分组列表，顶部导航栏负责切换日期。
 */
@Composable
fun TodayScreen(
    data: AppData,
    date: String,
    listState: LazyListState,
    bottomPadding: Dp,
    onShiftDate: (Long) -> Unit,
    onBackToToday: () -> Unit,
    onAddToMeal: (Meal) -> Unit,
    onEntryClick: (LogEntry) -> Unit,
    onCopyYesterday: () -> Unit,
) {
    val targets = targetsOf(data.profile)
    val entries = data.logs[date].orEmpty()
    val totals = totalsOf(entries)
    val isToday = date == Dates.today()

    GlasmioPinnedListPage(
        // 只保留紧凑的日期栏：大标题和副标题都去掉，日期放在顶栏里保证翻日期时看得见
        title = dateSubtitle(date),
        listState = listState,
        bottomPadding = bottomPadding,
        showsLargeTitle = false,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        actions = {
            GlasmioIconButton(onClick = { onShiftDate(-1) }) {
                SfIcon("chevron.left", "前一天", size = 17.dp)
            }
            GlasmioIconButton(onClick = { onShiftDate(1) }) {
                SfIcon("chevron.right", "后一天", size = 17.dp)
            }
            if (!isToday) {
                GlasmioButton(onClick = onBackToToday, size = GlasmioButtonSize.Small) {
                    Text("今天", style = IosTypography.subheadline)
                }
            }
        },
    ) {
        item(key = "nutrition") {
            // 热量和三项宏量合成一张卡：数值在左、三个同心环在右，文字不再压在环上
            NutritionRingsCard(totals = totals, targets = targets)
        }

        Meal.all.forEach { meal ->
            item(key = "meal-${meal.id}") {
                MealSection(
                    meal = meal,
                    entries = entries.filter { it.meal == meal.id },
                    onAdd = { onAddToMeal(meal) },
                    onEntryClick = onEntryClick,
                )
            }
        }

        item(key = "quick-actions") {
            GlasmioGroupedList {
                GlasmioListRow(
                    "复制昨天的记录",
                    systemName = "doc.on.doc",
                    showTopSeparator = false,
                    onClick = onCopyYesterday,
                )
                if (!isToday) {
                    GlasmioListRow(
                        "回到今天",
                        systemName = "calendar",
                        onClick = onBackToToday,
                    )
                }
            }
        }

    }
}

@Composable
private fun MealSection(
    meal: Meal,
    entries: List<LogEntry>,
    onAdd: () -> Unit,
    onEntryClick: (LogEntry) -> Unit,
) {
    val colors = GlasmioTheme.colors
    val mealKcal = entries.sumOf { it.kcal }.roundToInt()

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(meal.emoji, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(meal.label, style = IosTypography.footnote, color = colors.secondaryContent)
            Spacer(Modifier.width(8.dp))
            if (mealKcal > 0) {
                Text("$mealKcal kcal", style = IosTypography.footnote, color = colors.tertiaryContent)
            }
            Spacer(Modifier.weight(1f))
            TinyAddButton(contentDescription = "添加到${meal.label}", onClick = onAdd)
        }

        GlasmioGroupedList {
            if (entries.isEmpty()) {
                GlasmioListRow(
                    title = "添加食物",
                    systemName = "plus",
                    titleColor = colors.secondaryContent,
                    showTopSeparator = false,
                    showDisclosure = false,
                    onClick = onAdd,
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    GlasmioListRow(
                        title = entry.name,
                        subtitle = "${entry.qty.pretty()}${entry.unit} · 蛋白 ${entry.p.pretty()}g",
                        detail = "${entry.kcal.roundToInt()} kcal",
                        showTopSeparator = index > 0,
                        onClick = { onEntryClick(entry) },
                    )
                }
            }
        }
    }
}

private val Meal.emoji: String
    get() = when (this) {
        Meal.BREAKFAST -> "🥣"
        Meal.LUNCH -> "🍱"
        Meal.DINNER -> "🍲"
        Meal.SNACK -> "🍪"
    }

/** 11月14日 · 周六；不在今年时把年份也带上。 */
private fun dateSubtitle(date: String): String {
    val parsed = Dates.parse(date)
    val thisYear = Dates.parse(Dates.today()).year
    val prefix = if (parsed.year == thisYear) "" else "${parsed.year}年"
    return "${prefix}${parsed.monthValue}月${parsed.dayOfMonth}日 · ${Dates.weekdayLabel(parsed)}"
}
