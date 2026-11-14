package com.fitlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Profile
import com.fitlog.app.domain.Totals
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.ui.components.CalorieRingCard
import com.fitlog.app.ui.components.DaySummaryCard
import com.fitlog.app.ui.components.HistoryCalendarCard
import com.fitlog.app.ui.components.MacroCard
import com.fitlog.app.ui.components.WeightLineChart
import com.fitlog.app.ui.theme.FitLogTheme
import io.github.neoruaa.glasmio.core.GlasmioTheme
import java.time.LocalDate
import java.time.YearMonth

/**
 * Studio 里的组件预览。
 *
 * 只预览「不依赖玻璃采样」的零件：热量环、宏量卡、月历、当日详情、体重折线。
 * 整页（导航条、工具栏、弹层）必须有 [io.github.neoruaa.glasmio.glass.GlasmioBackdropHost]
 * 才能出效果，预览渲染不出来，请直接跑模拟器 / 真机看。
 */
private val demoProfile = Profile(weight = 70.2, height = 175.0, age = 28, activity = 1.55, goal = "cut")
private val demoTargets = targetsOf(demoProfile)
private val demoTotals = Totals(kcal = 880, protein = 83.1, carbs = 71.2, fat = 26.8)

private fun demoEntry(id: String, meal: String, kcal: Double, p: Double, c: Double, fat: Double) =
    LogEntry(id = id, meal = meal, kcal = kcal, p = p, c = c, fat = fat)

private val demoLogs: Map<String, List<LogEntry>> = mapOf(
    "2026-11-03" to listOf(demoEntry("a", "lunch", 640.0, 40.0, 60.0, 20.0)),
    "2026-11-05" to listOf(
        demoEntry("b", "breakfast", 320.0, 18.0, 30.0, 12.0),
        demoEntry("c", "dinner", 760.0, 45.0, 70.0, 28.0),
    ),
    "2026-11-09" to listOf(demoEntry("d", "lunch", 1850.0, 95.0, 180.0, 60.0)),
    "2026-11-11" to listOf(demoEntry("e", "dinner", 1180.0, 62.0, 120.0, 40.0)),
    "2026-11-12" to listOf(demoEntry("f", "snack", 210.0, 8.0, 26.0, 8.0)),
    "2026-11-14" to listOf(
        demoEntry("g", "breakfast", 249.0, 21.6, 7.2, 14.2),
        demoEntry("h", "lunch", 463.0, 53.3, 45.0, 6.2),
        demoEntry("i", "snack", 168.0, 8.2, 19.0, 6.4),
    ),
)

@Composable
private fun PreviewPage(dark: Boolean, content: @Composable () -> Unit) {
    FitLogTheme(dark = dark) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(GlasmioTheme.colors.groupedBackground)
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
        }
    }
}

@Preview(name = "热量环 · 浅色", showBackground = true)
@Composable
private fun CalorieRingLightPreview() {
    PreviewPage(dark = false) {
        CalorieRingCard(consumed = demoTotals.kcal, target = demoTargets.kcal)
    }
}

@Preview(name = "热量环 · 深色", showBackground = true)
@Composable
private fun CalorieRingDarkPreview() {
    PreviewPage(dark = true) {
        CalorieRingCard(consumed = demoTotals.kcal, target = demoTargets.kcal)
    }
}

@Preview(name = "宏量卡 · 浅色", showBackground = true)
@Composable
private fun MacroCardPreview() {
    PreviewPage(dark = false) {
        MacroCard(totals = demoTotals, targets = demoTargets)
    }
}

@Preview(name = "历史日历 · 浅色", showBackground = true)
@Composable
private fun CalendarPreview() {
    PreviewPage(dark = false) {
        HistoryCalendarCard(
            month = YearMonth.of(2026, 11),
            selected = LocalDate.of(2026, 11, 14),
            logs = demoLogs,
            targetKcal = demoTargets.kcal,
            onSelect = {},
            onMonthChange = {},
            onToday = {},
        )
    }
}

@Preview(name = "当日详情 · 深色", showBackground = true)
@Composable
private fun DaySummaryPreview() {
    PreviewPage(dark = true) {
        DaySummaryCard(
            date = LocalDate.of(2026, 11, 14),
            entries = demoLogs.getValue("2026-11-14"),
            targets = demoTargets,
        )
    }
}

@Preview(name = "体重折线 · 深色", showBackground = true)
@Composable
private fun WeightChartPreview() {
    PreviewPage(dark = true) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(GlasmioTheme.colors.elevatedBackground)
                .padding(16.dp),
        ) {
            WeightLineChart(points = listOf(71.8, 71.5, 71.6, 71.2, 71.0, 70.9, 70.8, 70.6, 70.5, 70.4, 70.2))
        }
    }
}
