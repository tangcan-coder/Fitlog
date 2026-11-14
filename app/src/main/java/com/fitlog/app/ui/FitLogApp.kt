package com.fitlog.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.ui.screens.LibraryScreen
import com.fitlog.app.ui.screens.MeScreen
import com.fitlog.app.ui.screens.StatsScreen
import com.fitlog.app.ui.screens.TodayScreen
import com.fitlog.app.ui.sheets.SheetHost
import com.fitlog.app.ui.theme.FitLogTheme
import com.kyant.shapes.Capsule
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.GlasmioBackdropHost
import io.github.neoruaa.glasmio.icons.GlasmioIconProvider
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.scaffold.glasmioNavigationScrollHint
import io.github.neoruaa.glasmio.scaffold.rememberGlasmioNavigationScrollState
import io.github.neoruaa.glasmio.ui.CompactBottomControlSize
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import io.github.neoruaa.glasmio.ui.NavigationBarSurfaceHeight
import kotlinx.coroutines.delay

/**
 * 应用根节点。
 *
 * 结构就是 Glasmio 推荐的两层：`sampledContent` 是整页内容（被录制为玻璃的采样源），
 * `overlayContent` 是悬浮在页面之上的玻璃底栏、提示条和弹层。
 */
@Composable
fun FitLogApp(vm: AppViewModel) {
    val appState by vm.state.collectAsStateWithLifecycle()
    val dark = isSystemInDarkTheme()
    // 稳定的图标默认色，避免每次重组都换一个 lambda、把所有图标一起重组。
    val iconTint: @Composable () -> Color = remember { { LocalContentColor.current } }

    FitLogTheme(dark) {
        GlasmioIconProvider(resolver = FitIconResolver, defaultTint = iconTint) {
            if (!appState.loaded) {
                Box(Modifier.fillMaxSize().background(GlasmioTheme.colors.groupedBackground))
            } else {
                FitLogContent(vm = vm, data = appState.data)
            }
        }
    }
}

@Composable
private fun FitLogContent(vm: AppViewModel, data: AppData) {
    // 这里只订阅 tab 和日期：弹层、提示语各自在下面的子组合里订阅自己的流，
    // 这样在弹层里打字不会让整页（和底下的玻璃采样层）跟着重组。
    val tab by vm.tab.collectAsStateWithLifecycle()
    val date by vm.date.collectAsStateWithLifecycle()

    // 第一次打开时先把资料填了，否则算不出今天该吃多少。每次启动只自动弹一次。
    var onboardingPrompted by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(data.meta.onboarded) {
        if (!data.meta.onboarded && !onboardingPrompted) {
            onboardingPrompted = true
            vm.openProfile()
        }
    }

    val todayList = rememberLazyListState()
    val libraryList = rememberLazyListState()
    val statsList = rememberLazyListState()
    val meList = rememberLazyListState()
    val currentList = when (tab) {
        Tab.TODAY -> todayList
        Tab.LIBRARY -> libraryList
        Tab.STATS -> statsList
        Tab.ME -> meList
    }
    val navigationScroll = rememberGlasmioNavigationScrollState(currentList)

    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 底栏自身高度 + 外边距 + 系统手势区，页面底部要让出这么多。
    val bottomBarSpace = NavigationBarSurfaceHeight + 16.dp + navigationInset

    val tabs = remember(tab) {
        listOf(
            GlasmioTabItem(
                key = Tab.TODAY,
                label = Tab.TODAY.label,
                symbol = if (tab == Tab.TODAY) SfSymbol.HouseFilled else SfSymbol.House,
            ),
            GlasmioTabItem(Tab.LIBRARY, Tab.LIBRARY.label, SfSymbol.Layers),
            GlasmioTabItem(Tab.STATS, Tab.STATS.label, SfSymbol.Waveform),
            GlasmioTabItem(Tab.ME, Tab.ME.label, SfSymbol.PersonFilled),
        )
    }
    val selectTab: (Tab) -> Unit = remember(vm) { { vm.selectTab(it) } }
    val expandBar: () -> Unit = remember(navigationScroll) { { navigationScroll.expand() } }

    GlasmioBackdropHost(
        modifier = Modifier.fillMaxSize(),
        sampledContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(navigationScroll.nestedScrollConnection),
            ) {
                when (tab) {
                    Tab.TODAY -> TodayScreen(
                        data = data,
                        date = date,
                        listState = todayList,
                        bottomPadding = bottomBarSpace,
                        onShiftDate = { vm.shiftDate(it) },
                        onBackToToday = { vm.setDate(Dates.today()) },
                        onAddToMeal = { meal -> vm.openSelect(meal.id, date) },
                        onEntryClick = { entry -> vm.openEntry(date, entry) },
                        onCopyYesterday = { vm.copyYesterday() },
                    )

                    Tab.LIBRARY -> LibraryScreen(
                        foods = data.foods,
                        listState = libraryList,
                        bottomPadding = bottomBarSpace,
                        onEdit = { vm.openEditFood(it) },
                        onNew = { vm.openNewFood() },
                        onBatch = { vm.openBatch() },
                    )

                    Tab.STATS -> StatsScreen(
                        data = data,
                        listState = statsList,
                        bottomPadding = bottomBarSpace,
                        onAddWeight = { vm.addWeight(it) },
                        onInvalidWeight = { vm.toast("体重填 20–300 kg") },
                    )

                    Tab.ME -> MeScreen(
                        data = data,
                        listState = meList,
                        bottomPadding = bottomBarSpace,
                        buildExport = { vm.exportText() },
                        onExportDone = { vm.markBackedUp() },
                        onImport = { vm.importText(it) },
                        onEditProfile = { vm.openProfile() },
                        onClearAll = { vm.clearAll() },
                        onToast = { vm.toast(it) },
                    )
                }
            }
        },
        overlayContent = { backdrop ->
            GlasmioNavigationBar(
                items = tabs,
                selectedKey = tab,
                onSelected = selectTab,
                onExpand = expandBar,
                compactProgress = navigationScroll.compactProgress,
                compactSize = CompactBottomControlSize,
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
                    .glasmioNavigationScrollHint(navigationScroll.scrollHintProgress)
                    .navigationBarsPadding(),
            )

            ToastHost(vm = vm, bottomOffset = bottomBarSpace)
            SheetArea(vm = vm, data = data)
        },
    )
}

/** 底部提示条：只在提示语变化时重组。 */
@Composable
private fun BoxScope.ToastHost(vm: AppViewModel, bottomOffset: Dp) {
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(1800)
            vm.consumeMessage()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomOffset + 8.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            Modifier
                .shadow(18.dp, Capsule())
                .clip(Capsule())
                .background(GlasmioTheme.colors.elevatedBackground)
                .border(0.5.dp, GlasmioTheme.colors.separator, Capsule())
                .padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Text(
                message?.text.orEmpty(),
                style = IosTypography.subheadline,
                color = GlasmioTheme.colors.content,
            )
        }
    }
}

/** 弹层区域：只在弹层状态变化时重组，不牵动页面。 */
@Composable
private fun SheetArea(vm: AppViewModel, data: AppData) {
    val sheet by vm.sheet.collectAsStateWithLifecycle()

    BackHandler(enabled = sheet != null) { vm.closeSheet() }

    SheetHost(
        sheet = sheet,
        data = data,
        vm = vm,
        onDismiss = { vm.closeSheet() },
    )
}
