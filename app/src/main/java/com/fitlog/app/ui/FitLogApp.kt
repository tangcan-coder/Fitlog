package com.fitlog.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.data.AppData
import com.fitlog.app.domain.Dates
import com.fitlog.app.ui.screens.LibraryScreen
import com.fitlog.app.ui.screens.MeScreen
import com.fitlog.app.ui.screens.StatsScreen
import com.fitlog.app.ui.screens.TodayScreen
import com.fitlog.app.ui.sheets.BackupDirPage
import com.fitlog.app.ui.sheets.SettingsPage
import com.fitlog.app.ui.sheets.TargetPage
import com.fitlog.app.ui.sheets.BatchSheet
import com.fitlog.app.ui.sheets.EntrySheet
import com.fitlog.app.ui.sheets.FoodFormSheet
import com.fitlog.app.ui.sheets.ProfileSheet
import com.fitlog.app.ui.sheets.SelectSheet
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

    // 挂后台过夜后回到前台，把「今天」对齐一次，否则早上的记录会落到昨天。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.syncDateToToday() }

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
    // 只订阅「是否有二级页面打开」这个布尔值：表单里打字不会牵动页面重组
    val pageOpen by vm.pageOpen.collectAsStateWithLifecycle()

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
                        listState = meList,
                        bottomPadding = bottomBarSpace,
                        onOpenTarget = { vm.openTarget() },
                        onOpenSettings = { vm.openSettings() },
                    )
                }

                // 所有弹层（选择食物 / 食物表单 / 记录份量 / 批量导入 / 资料）都改成
                // 盖在标签页上的二级页面：同一个窗口，没有 Dialog 窗口创建和跨窗口采样
                SheetPageArea(vm = vm, data = data)
            }
        },
        overlayContent = { backdrop ->
            if (pageOpen) {
                // 二级页面占满整屏，底栏让位；页面自己的顶栏负责返回/保存
            } else {
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
            }

            ToastHost(vm = vm, bottomOffset = bottomBarSpace)
        },
    )
}

/** 底部提示条：只在提示语变化时重组。带「撤销」这类动作时停留更久，给用户点的时间。 */
@Composable
private fun BoxScope.ToastHost(vm: AppViewModel, bottomOffset: Dp) {
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        delay(if (current.actionLabel != null) 4200 else 1800)
        vm.consumeMessage(current.id)
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomOffset + 8.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            Modifier
                .shadow(18.dp, Capsule())
                .clip(Capsule())
                .background(GlasmioTheme.colors.elevatedBackground)
                .border(0.5.dp, GlasmioTheme.colors.separator, Capsule())
                .padding(
                    start = 18.dp,
                    // 有动作时右侧留给可点的文字，裸提示保持对称留白
                    end = if (message?.actionLabel != null) 8.dp else 18.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message?.text.orEmpty(),
                style = IosTypography.subheadline,
                color = GlasmioTheme.colors.content,
            )
            val actionLabel = message?.actionLabel
            if (actionLabel != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    actionLabel,
                    style = IosTypography.subheadline,
                    fontWeight = FontWeight(600),
                    color = GlasmioTheme.colors.accent,
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable {
                            val current = message ?: return@clickable
                            current.onAction?.invoke()
                            vm.consumeMessage(current.id)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * 二级页面区域：所有弹层（选择食物 / 食物表单 / 记录份量 / 批量导入 / 资料）都渲染在这里。
 *
 * 和标签页在同一个窗口里，用一次水平推入代替 Dialog：没有窗口创建、没有跨窗口 backdrop 映射。
 * 各页面自己负责顶栏（[com.fitlog.app.ui.sheets.SheetPageScaffold]），这里只管进出场动画。
 */
@Composable
private fun SheetPageArea(vm: AppViewModel, data: AppData) {
    val sheet by vm.sheet.collectAsStateWithLifecycle()
    val backupDir by vm.backupDir.collectAsStateWithLifecycle()
    // 退场动画期间保留最后一份内容；打字时仍然用实时状态，避免输入被旧值冲回去
    var lastSheet by remember { mutableStateOf<Sheet?>(null) }
    LaunchedEffect(sheet) { if (sheet != null) lastSheet = sheet }
    val current = sheet ?: lastSheet ?: return

    BackHandler(enabled = sheet != null) { vm.closeSheet() }

    AnimatedVisibility(
        visible = sheet != null,
        enter = slideInHorizontally(
            animationSpec = tween(280, easing = FastOutSlowInEasing),
            initialOffsetX = { it },
        ) + fadeIn(tween(180)),
        exit = slideOutHorizontally(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            targetOffsetX = { it },
        ) + fadeOut(tween(160)),
    ) {
        when (current) {
            is Sheet.Select -> SelectSheet(
                state = current.state,
                foods = data.foods,
                visible = true,
                pageMode = true,
                onDismiss = { vm.closeSheet() },
                onQuery = vm::setSelectQuery,
                onCat = vm::setSelectCat,
                onQty = vm::changeQty,
                onCommit = vm::commitSelection,
                onNewFood = { vm.openNewFood(current.state) },
            )

            is Sheet.FoodForm -> FoodFormSheet(
                form = current,
                visible = true,
                pageMode = true,
                onDismiss = { vm.closeSheet() },
                onUpdate = { vm.updateFoodForm(it) },
                onBasis = { vm.setFoodBasis(it) },
                onSave = { vm.saveFoodForm() },
                onDelete = { vm.deleteFood(current.foodId.orEmpty()) },
            )

            is Sheet.Entry -> EntrySheet(
                sheet = current,
                visible = true,
                onDismiss = { vm.closeSheet() },
                onDelta = { vm.changeEntryQty(it) },
                onDelete = { vm.deleteEntry(current.date, current.entryId) },
            )

            Sheet.Batch -> BatchSheet(
                visible = true,
                pageMode = true,
                onDismiss = { vm.closeSheet() },
                onCommit = { vm.commitBatch(it) },
            )

            Sheet.Profile -> ProfileSheet(
                data = data,
                visible = true,
                pageMode = true,
                onDismiss = { vm.closeSheet() },
                onSave = { vm.saveProfile(it) },
                onInvalid = { vm.toast(it) },
            )

            Sheet.Target -> TargetPage(
                data = data,
                onDismiss = { vm.closeSheet() },
            )

            Sheet.Settings -> SettingsPage(
                data = data,
                backupDir = backupDir,
                onDismiss = { vm.closeSheet() },
                onEditProfile = { vm.openProfile() },
                onBackupDir = { vm.openBackupDir() },
                onExportToDir = { vm.exportToBackupDir() },
                buildExport = { vm.exportText() },
                onExportDone = { vm.markBackedUp() },
                onImport = { vm.importText(it) },
                onToast = { vm.toast(it) },
            )

            Sheet.BackupDir -> BackupDirPage(
                current = backupDir,
                onDismiss = { vm.closeSheet() },
                onPick = { vm.setBackupDir(it) },
                onClear = { vm.setBackupDir(null) },
                onToast = { vm.toast(it) },
            )
        }
    }
}