package com.fitlog.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.domain.Dates
import com.fitlog.app.ui.components.FloatingDock
import com.fitlog.app.ui.screens.LibraryScreen
import com.fitlog.app.ui.screens.MeScreen
import com.fitlog.app.ui.screens.StatsScreen
import com.fitlog.app.ui.screens.TodayScreen
import com.fitlog.app.ui.sheets.SheetHost
import com.fitlog.app.ui.theme.FitTheme
import kotlinx.coroutines.delay

@Composable
fun FitLogApp(vm: AppViewModel) {
    val appState by vm.state.collectAsStateWithLifecycle()
    val data = appState.data
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = FitTheme.colors

    // 本地数据还没读出来时先不画，否则会闪一下「0 摄入 / 空食物库」的假数据。
    if (!appState.loaded) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.bg)
        )
        return
    }

    // 第一次打开时先把资料填了，否则算不出今天该吃多少。
    // 必须等本地数据真正读出来再判断，否则会用默认值误判成「没填过」。
    LaunchedEffect(appState.loaded, data.meta.onboarded) {
        if (appState.loaded && !data.meta.onboarded && ui.sheet == null) vm.openProfile()
    }

    val message = ui.message
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(1800)
            vm.consumeMessage()
        }
    }

    BackHandler(enabled = ui.sheet != null) { vm.closeSheet() }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
    ) {
        // 顶部一层极淡的氛围光
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(c.glow, Color.Transparent)
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            when (ui.tab) {
                Tab.TODAY -> TodayScreen(
                    data = data,
                    date = ui.date,
                    onShiftDate = { vm.shiftDate(it) },
                    onBackToToday = { vm.setDate(Dates.today()) },
                    onAddToMeal = { meal -> vm.openSelect(meal.id, ui.date) },
                    onEntryClick = { entry -> vm.openEntry(ui.date, entry) },
                    onCopyYesterday = { vm.copyYesterday() }
                )

                Tab.LIBRARY -> LibraryScreen(
                    foods = data.foods,
                    onEdit = { vm.openEditFood(it) },
                    onNew = { vm.openNewFood() },
                    onBatch = { vm.openBatch() }
                )

                Tab.STATS -> StatsScreen(
                    data = data,
                    onAddWeight = { vm.addWeight(it) }
                )

                Tab.ME -> MeScreen(
                    data = data,
                    buildExport = { vm.exportText() },
                    onExportDone = { vm.markBackedUp() },
                    onImport = { vm.importText(it) },
                    onEditProfile = { vm.openProfile() },
                    onClearAll = { vm.clearAll() }
                )
            }
        }

        // 底部渐隐，让内容在悬浮 Dock 下面自然淡出，而不是露出半行字
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(152.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to c.bg.copy(alpha = 0.92f),
                        1f to c.bg
                    )
                )
        )

        FloatingDock(
            current = ui.tab,
            onSelect = { vm.selectTab(it) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
                .navigationBarsPadding()
                .padding(bottom = 10.dp)
        )

        AnimatedVisibility(
            visible = message != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 86.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.surface2)
                    .border(1.dp, c.line2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(message?.text.orEmpty(), color = c.text, fontSize = 13.sp)
            }
        }

        SheetHost(
            sheet = ui.sheet,
            data = data,
            vm = vm,
            onDismiss = { vm.closeSheet() }
        )
    }
}
