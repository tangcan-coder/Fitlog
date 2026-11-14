package com.fitlog.app.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fitlog.app.data.AppData
import com.fitlog.app.ui.AppViewModel
import com.fitlog.app.ui.Sheet
import com.fitlog.app.ui.components.PrimaryButton
import com.fitlog.app.ui.screens.FoodSelectScreen
import com.fitlog.app.ui.theme.FitTheme
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

/**
 * 弹层容器：全屏的「选择食物」和底部卡片式的表单都从这里进。
 * 保留最后一次内容，这样退场动画期间不会闪成空白。
 */
@Composable
fun SheetHost(
    sheet: Sheet?,
    data: AppData,
    vm: AppViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var last by remember { mutableStateOf<Sheet?>(null) }
    LaunchedEffect(sheet) { if (sheet != null) last = sheet }
    val current = sheet ?: last

    // 关键：弹层完全关闭后必须把节点移出组合树。
    // 只要节点还在，即使不可见也会拦截底部 Dock 的点击。
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = sheet != null
    if (!visibleState.currentState && !visibleState.targetState) return

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier.zIndex(3f),
        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut()
    ) {
        if (current == null) return@AnimatedVisibility

        Box(Modifier.fillMaxSize()) {
            when (current) {
                is Sheet.Select -> FoodSelectScreen(
                    state = current.state,
                    foods = data.foods,
                    onQuery = vm::setSelectQuery,
                    onCat = vm::setSelectCat,
                    onQty = vm::changeQty,
                    onCommit = vm::commitSelection,
                    onClose = onDismiss,
                    onNewFood = { vm.openNewFood(current.state) }
                )

                is Sheet.FoodForm -> FoodFormSheet(current, vm, onDismiss)
                is Sheet.Entry -> EntrySheet(current, vm, onDismiss)
                Sheet.Batch -> BatchSheet(vm, onDismiss)
                Sheet.Profile -> ProfileSheet(data, vm, onDismiss)
            }
        }
    }
}

@Composable
fun SheetScrim(onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss)
    )
}

@Composable
fun BottomSheetCard(content: @Composable ColumnScope.() -> Unit) {
    val c = FitTheme.colors
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(c.surface)
            .navigationBarsPadding(),
        content = content
    )
}

@Composable
fun SheetHeader(
    title: String,
    onCancel: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    cancelLabel: String = "取消"
) {
    val c = FitTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            cancelLabel,
            color = c.text2,
            fontSize = 14.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Spacer(Modifier.weight(1f))
        Text(title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        PrimaryButton(confirmLabel, onClick = onConfirm)
    }
}

@Composable
fun SheetBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)
    ) {
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun FieldLabel(text: String) {
    val c = FitTheme.colors
    Text(
        text,
        color = c.text3,
        fontSize = 11.5.sp,
        modifier = Modifier.padding(start = 2.dp, bottom = 7.dp)
    )
}

@Composable
fun SegmentedRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) c.accent else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) c.accentInk else c.text2,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))

@Composable
fun HSpace(w: Int) = Spacer(Modifier.width(w.dp))
