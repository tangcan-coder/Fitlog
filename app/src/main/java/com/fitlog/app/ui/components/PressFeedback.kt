package com.fitlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioListRow

/**
 * 按压反馈：按下时整体轻微缩小、松手弹回。
 *
 * 缩放只走 `graphicsLayer`（不影响布局），90ms 的短动画让手感「跟手」而不是慢半拍。
 * 幅度由调用方给：普通按钮 0.96、图标按钮 0.92、卡片 0.985。
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    scale: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val factor by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = factor
        scaleY = factor
    }
}

/**
 * 给「点击逻辑在别处」的容器（比如 Glasmio 的列表行）加按压反馈。
 *
 * 只观察手势、不消费事件：行自己的 clickable 照常收到点击，所以可以安全地叠在它外面。
 * 手指一滑动就取消按下态，滚动列表不会留下被缩小的行。
 */
fun Modifier.observePressScale(scale: Float = 0.985f): Modifier = composed {
    val pressed = remember { mutableStateOf(false) }
    val factor by animateFloatAsState(
        targetValue = if (pressed.value) scale else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "observePressScale",
    )
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            pressed.value = true
            try {
                waitForUpOrCancellation()
            } finally {
                pressed.value = false
            }
        }
    }.graphicsLayer {
        scaleX = factor
        scaleY = factor
    }
}

/**
 * 带按压反馈的列表行：外观和 GlasmioListRow 完全一致，
 * 只是可点击时整体轻微缩小。
 */
@Composable
fun FitListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    detail: String? = null,
    systemName: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showTopSeparator: Boolean = true,
    enabled: Boolean = true,
    showDisclosure: Boolean = onClick != null && trailing == null,
    titleColor: Color = Color.Unspecified,
    titleAlignment: TextAlign = TextAlign.Start,
) {
    GlasmioListRow(
        title = title,
        modifier = if (onClick != null) modifier.observePressScale() else modifier,
        subtitle = subtitle,
        detail = detail,
        systemName = systemName,
        leading = leading,
        onClick = onClick,
        trailing = trailing,
        showTopSeparator = showTopSeparator,
        enabled = enabled,
        showDisclosure = showDisclosure,
        titleColor = titleColor,
        titleAlignment = titleAlignment,
    )
}

/** 带按压反馈的玻璃图标按钮：按下缩到 0.92，其余参数原样透传。 */
@Composable
fun FitIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    GlasmioIconButton(
        onClick = onClick,
        modifier = modifier.observePressScale(0.92f),
        enabled = enabled,
        content = content,
    )
}
