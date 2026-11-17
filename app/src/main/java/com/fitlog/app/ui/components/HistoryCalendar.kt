package com.fitlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.LogEntry
import com.fitlog.app.domain.Targets
import com.fitlog.app.domain.totalsOf
import com.fitlog.app.ui.theme.FitPalette
import com.kyant.capsule.ContinuousRoundedRectangle
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.icons.SfIcon
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private val weekdayLabels = listOf("日", "一", "二", "三", "四", "五", "六")

/**
 * 历史日历。
 *
 * 按月翻页，每一格显示当天摄入的热量；没有记录的日子只显示日期。
 * 点某一天由外部更新 `selected`，详情在 [DaySummaryCard] 里展示。
 */
@Composable
fun HistoryCalendarCard(
    month: YearMonth,
    selected: LocalDate,
    logs: Map<String, List<LogEntry>>,
    targetKcal: Int,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val accent = FitPalette.accent(colors.isDark)

    val leading = month.atDay(1).dayOfWeek.value % 7
    val dayCount = month.lengthOfMonth()
    val cells = buildList<LocalDate?> {
        repeat(leading) { add(null) }
        for (day in 1..dayCount) add(month.atDay(day))
        while (size % 7 != 0) add(null)
    }
    val weeks = cells.chunked(7)

    val recordedDays = (1..dayCount).count { logs[month.atDay(it).toString()].orEmpty().isNotEmpty() }
    val monthKcal = (1..dayCount).sumOf { day ->
        logs[month.atDay(day).toString()].orEmpty().sumOf { it.kcal }.roundToInt()
    }
    val averageKcal = if (recordedDays > 0) monthKcal / recordedDays else 0

    Column(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(colors.elevatedBackground)
            .padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${month.year}年${month.monthValue}月",
                style = IosTypography.title3,
                fontWeight = FontWeight(600),
                color = colors.content,
            )
            Spacer(Modifier.weight(1f))
            if (month != currentMonth) {
                Text(
                    "今天",
                    style = IosTypography.subheadline,
                    color = colors.accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onToday)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            MonthArrow("chevron.left", "上个月") { onMonthChange(month.minusMonths(1)) }
            Spacer(Modifier.width(4.dp))
            MonthArrow("chevron.right", "下个月") { onMonthChange(month.plusMonths(1)) }
        }

        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            weekdayLabels.forEach { label ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = IosTypography.caption1,
                        color = colors.tertiaryContent,
                    )
                }
            }
        }

        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        selected = selected,
                        today = today,
                        entries = date?.let { logs[it.toString()].orEmpty() }.orEmpty(),
                        targetKcal = targetKcal,
                        accent = accent,
                        onSelect = onSelect,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            when {
                recordedDays == 0 -> "这个月还没有记录"
                else -> "本月 $recordedDays 天有记录 · 日均 $averageKcal kcal"
            },
            style = IosTypography.footnote,
            color = colors.tertiaryContent,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun MonthArrow(
    systemName: String,
    description: String,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(colors.segmentedControlBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon(systemName, description, size = 14.dp, tint = colors.content)
    }
}

@Composable
private fun RowScope.DayCell(
    date: LocalDate?,
    selected: LocalDate,
    today: LocalDate,
    entries: List<LogEntry>,
    targetKcal: Int,
    accent: Color,
    onSelect: (LocalDate) -> Unit,
) {
    val colors = GlasmioTheme.colors
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .clip(ContinuousRoundedRectangle(12.dp))
            .then(
                if (date != null) Modifier.clickable { onSelect(date) } else Modifier,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (date == null) return@Box

        val isSelected = date == selected
        val isToday = date == today
        val isFuture = date > today
        val kcal = entries.sumOf { it.kcal }.roundToInt()
        val hasEntries = entries.isNotEmpty()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${date.dayOfMonth}",
                    style = IosTypography.body,
                    fontWeight = when {
                        isSelected || isToday -> FontWeight(600)
                        else -> FontWeight(400)
                    },
                    color = when {
                        isSelected -> FitPalette.onAccent(colors.isDark)
                        isToday -> accent
                        isFuture -> colors.tertiaryContent
                        else -> colors.content
                    },
                )
            }
            if (hasEntries) {
                Text(
                    "$kcal",
                    style = IosTypography.caption2,
                    fontSize = 9.5.sp,
                    color = if (kcal > targetKcal) colors.destructive else colors.secondaryContent,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 选中日期的当日营养汇总。
 *
 * 版式和今日页那张卡保持一致：左边四行数值、右边四圈同心环、底部余量胶囊，
 * 上面多一行「日期 + 今天 + 几条」的表头，下面多一行餐次分布。
 * 两处共用 [NutritionRingsBody]，改一处两边都跟着变。
 */
@Composable
fun DaySummaryCard(
    date: LocalDate,
    entries: List<LogEntry>,
    targets: Targets,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val totals = totalsOf(entries)
    val isToday = date == LocalDate.now()

    Column(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(colors.elevatedBackground)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${date.monthValue}月${date.dayOfMonth}日 · ${weekdayLabel(date)}",
                style = IosTypography.headline,
                color = colors.content,
            )
            if (isToday) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "今天",
                    style = IosTypography.caption2,
                    color = FitPalette.onAccent(colors.isDark),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FitPalette.accent(colors.isDark))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${entries.size} 条",
                style = IosTypography.footnote,
                color = colors.tertiaryContent,
            )
        }

        if (entries.isEmpty()) {
            EmptyHint(text = "这一天还没有记录", symbol = null)
        } else {
            Spacer(Modifier.height(14.dp))
            // 和今日页共用同一段「四圈环 + 四组数值」，这里不再另写餐次分布
            NutritionRingsBody(totals = totals, targets = targets)
        }
    }
}

private fun weekdayLabel(date: LocalDate): String =
    arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]