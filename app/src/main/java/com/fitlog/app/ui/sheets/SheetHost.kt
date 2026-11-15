package com.fitlog.app.ui.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fitlog.app.data.AppData
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.Sheet
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
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(sheet) {
        if (sheet != null) {
            lastSheet = sheet
            visible = true
        } else if (lastSheet != null) {
            visible = false
            delay(420)
            lastSheet = null
        }
    }

    val current = sheet ?: lastSheet ?: return

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
