package com.fitlog.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.fitlog.app.data.AppData
import com.fitlog.app.data.Food
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Meal
import com.fitlog.app.data.Meta
import com.fitlog.app.data.Profile
import com.fitlog.app.data.WeightLog
import com.fitlog.app.domain.Dates
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.ui.screens.LibraryScreen
import com.fitlog.app.ui.screens.MeScreen
import com.fitlog.app.ui.screens.StatsScreen
import com.fitlog.app.ui.screens.TodayScreen
import com.fitlog.app.ui.theme.FitLogTheme
import com.fitlog.app.ui.theme.FitTheme

/**
 * Android Studio 里的界面预览。
 *
 * 改配色、改间距时不用装到手机上看，右边 Preview 面板刷一下就行。
 * 这里用的是一份假的样本数据，跟手机上的真实数据完全无关。
 *
 * 预览里看不到「新建食物 / 批量导入」这类弹层 —— 它们依赖 ViewModel，
 * 想调那一层还是得跑起来点一下。
 */
private object Sample {

    private fun food(
        id: String, name: String, cat: String,
        kcal: Double, p: Double, c: Double, fat: Double, uses: Int
    ) = Food(
        id = id, name = name, cat = cat, unit = "份", gram = null,
        kcal = kcal, p = p, c = c, fat = fat,
        alias = Pinyin.initials(name), uses = uses,
        lastUsed = 0L, createdAt = 0L
    )

    private fun entry(
        id: String, foodId: String, name: String, qty: Double, meal: Meal,
        kcal: Double, p: Double, c: Double, fat: Double
    ) = LogEntry(
        id = id, foodId = foodId, name = name, unit = "份", qty = qty, meal = meal.id,
        kcal = kcal, p = p, c = c, fat = fat, ts = 0L
    )

    val today: String = Dates.today()

    val foods = listOf(
        food("f1", "白米饭", "主食", 230.0, 4.3, 48.0, 0.3, 42),
        food("f2", "水煮蛋", "蛋白", 78.0, 6.3, 0.6, 5.3, 31),
        food("f3", "鸡胸肉", "蛋白", 165.0, 31.0, 0.0, 3.6, 26),
        food("f4", "无糖豆浆", "饮品", 60.0, 5.2, 1.8, 2.5, 18),
        food("f5", "清炒西兰花", "蔬菜", 95.0, 4.1, 7.0, 5.2, 9)
    )

    val filled: AppData = AppData(
        foods = foods,
        logs = mapOf(
            today to listOf(
                entry("l1", "f1", "白米饭", 1.0, Meal.BREAKFAST, 230.0, 4.3, 48.0, 0.3),
                entry("l2", "f2", "水煮蛋", 2.0, Meal.BREAKFAST, 156.0, 12.6, 1.2, 10.6),
                entry("l3", "f3", "鸡胸肉", 1.0, Meal.LUNCH, 165.0, 31.0, 0.0, 3.6)
            )
        ),
        weights = listOf(
            WeightLog(Dates.shift(today, -4), 70.9),
            WeightLog(Dates.shift(today, -2), 70.6),
            WeightLog(today, 70.4)
        ),
        profile = Profile(),
        meta = Meta(onboarded = true)
    )

    val empty: AppData = AppData(profile = Profile(), meta = Meta(onboarded = true))
}

/** 套上和真机一样的背景与状态栏留白，免得预览和实际差一截。 */
@Composable
private fun Phone(content: @Composable () -> Unit) {
    FitLogTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(FitTheme.colors.bg)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) { content() }
        }
    }
}

@Preview(
    name = "今日 · 有记录（深色）",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewTodayFilled() {
    Phone {
        TodayScreen(
            data = Sample.filled,
            date = Sample.today,
            onShiftDate = {},
            onBackToToday = {},
            onAddToMeal = {},
            onEntryClick = {},
            onCopyYesterday = {}
        )
    }
}

@Preview(
    name = "今日 · 空（浅色）",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun PreviewTodayEmpty() {
    Phone {
        TodayScreen(
            data = Sample.empty,
            date = Sample.today,
            onShiftDate = {},
            onBackToToday = {},
            onAddToMeal = {},
            onEntryClick = {},
            onCopyYesterday = {}
        )
    }
}

@Preview(
    name = "食物库（深色）",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewLibrary() {
    Phone {
        LibraryScreen(
            foods = Sample.foods,
            onEdit = {},
            onNew = {},
            onBatch = {}
        )
    }
}

@Preview(
    name = "统计（深色）",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewStats() {
    Phone {
        StatsScreen(
            data = Sample.filled,
            onAddWeight = {}
        )
    }
}

@Preview(
    name = "我的（浅色）",
    widthDp = 393,
    heightDp = 852,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun PreviewMe() {
    Phone {
        MeScreen(
            data = Sample.filled,
            buildExport = { "" },
            onExportDone = {},
            onImport = {},
            onEditProfile = {},
            onClearAll = {}
        )
    }
}
