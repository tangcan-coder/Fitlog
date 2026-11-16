package com.fitlog.app.ui.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fitlog.app.data.AppData
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.Sheet
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.LocalGlassAppearance
import kotlinx.coroutines.delay

/**
 * 弹层容器。
 *
 * Glasmio 的底部弹层是一个独立的窗口，退场动画期间必须留在组合树里，
 * 所以这里保留最后一次的弹层内容：`sheet` 先变成 null 触发关闭动画，
 * 动画走完再把节点移出组合树。
 */
@Composable
fun SheetHost(
    sheet: Sheet?,
    data: AppData,
    vm: AppViewModel,
    onDismiss: () -> Unit,
) {
    // 打开时用 vm 里的实时状态；只有在退场动画期间才用「最后一次内容」兜底。
    //
    // 之前这里把所有内容都指向 held 副本，而 held 是在 LaunchedEffect 里晚一拍更新的：
    // 每敲一个字都会先拿旧值重组一帧，输入框被外部值冲回去、光标跳回开头，
    // 于是输入 “23” 会显示成 “32”。
    var lastSheet by remember { mutableStateOf<Sheet?>(null) }

    LaunchedEffect(sheet) {
        if (sheet != null) {
            lastSheet = sheet
        } else if (lastSheet != null) {
            delay(SHEET_EXIT_ANIMATION_MS)
            lastSheet = null
        }
    }

    val current = sheet ?: lastSheet ?: return
    // 可见性直接由状态推导：以前还要在 LaunchedEffect 里再翻一次 visible，
    // 打开时会多走一轮「先以不可见组合、再改状态重组」的重组。
    val visible = sheet != null

    // 弹层本体铺的是一层不透明的 groupedBackground，玻璃材质其实被完全盖住，
    // 但每一帧仍要跑全屏采样 + 模糊/折射着色器——弹出时掉帧主要就掉在这里。
    // 所以只给弹层关掉背景采样（导航栏、工具栏的玻璃不受影响）。
    val sheetAppearance = remember { GlassAppearance(reduceTransparency = true) }

    CompositionLocalProvider(LocalGlassAppearance provides sheetAppearance) {
        when (current) {
            is Sheet.Select -> SelectSheet(
                state = current.state,
                foods = data.foods,
                visible = visible,
                onDismiss = onDismiss,
                onQuery = vm::setSelectQuery,
                onCat = vm::setSelectCat,
                onQty = vm::changeQty,
                onCommit = vm::commitSelection,
                onNewFood = { vm.openNewFood(current.state) },
            )

            is Sheet.FoodForm -> FoodFormSheet(
                form = current,
                visible = visible,
                onDismiss = onDismiss,
                onUpdate = { vm.updateFoodForm(it) },
                onBasis = { vm.setFoodBasis(it) },
                onSave = { vm.saveFoodForm() },
                onDelete = { vm.deleteFood(current.foodId.orEmpty()) },
            )

            is Sheet.Entry -> EntrySheet(
                sheet = current,
                visible = visible,
                onDismiss = onDismiss,
                onDelta = { vm.changeEntryQty(it) },
                onDelete = { vm.deleteEntry(current.date, current.entryId) },
            )

            Sheet.Batch -> BatchSheet(
                visible = visible,
                onDismiss = onDismiss,
                onCommit = { vm.commitBatch(it) },
            )

            Sheet.Profile -> ProfileSheet(
                data = data,
                visible = visible,
                onDismiss = onDismiss,
                onSave = { vm.saveProfile(it) },
                onInvalid = { vm.toast(it) },
            )
        }
    }
}

/** 退场动画时长：Glasmio 的弹层还在动画里时不能移出组合树，否则会闪一下空白 */
private const val SHEET_EXIT_ANIMATION_MS = 420L
