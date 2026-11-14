package com.fitlog.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.ui.Tab
import com.fitlog.app.ui.theme.FitTheme
import kotlin.math.max
import kotlin.math.min

/* ================= 容器与文字 ================= */

@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = FitTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(24.dp))
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, trailing: String? = null, modifier: Modifier = Modifier) {
    val c = FitTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = c.text2, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = c.text3, fontSize = 11.5.sp)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    val c = FitTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = c.text3, fontSize = 12.sp)
        }
        trailing()
    }
}

/* ================= 按钮 ================= */

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = FitTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (enabled) c.accent else c.surface2)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) c.accentInk else c.text3,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    icon: IconKind? = null,
    onClick: () -> Unit
) {
    val c = FitTheme.colors
    val fg = color ?: c.text2
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(c.tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            FitIcon(icon, fg, size = 16.dp, strokeWidth = 1.8.dp)
            Spacer(Modifier.width(7.dp))
        }
        Text(text, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CircleIconButton(
    icon: IconKind,
    modifier: Modifier = Modifier,
    background: Color? = null,
    tint: Color? = null,
    size: Dp = 38.dp,
    onClick: () -> Unit
) {
    val c = FitTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: c.tint)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        FitIcon(icon, tint ?: c.text2, size = size * 0.45f, strokeWidth = 2.dp)
    }
}

/* ================= 输入 ================= */

@Composable
fun FitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    minHeight: Dp = 48.dp
) {
    val c = FitTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = !multiline,
        textStyle = TextStyle(color = c.text, fontSize = 15.sp),
        cursorBrush = SolidColor(c.accentText),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(c.surface2)
                    .padding(horizontal = 14.dp, vertical = if (multiline) 14.dp else 13.dp),
                contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = c.text3, fontSize = 15.sp)
                }
                inner()
            }
        }
    )
}

/* ================= 热量环 ================= */

@Composable
fun CalorieRing(
    consumed: Int,
    target: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 208.dp,
    strokeWidth: Dp = 14.dp
) {
    val c = FitTheme.colors
    val remaining = target - consumed
    val over = remaining < 0
    val frac = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f)
    val ringColor = if (over) c.danger else c.accent

    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = c.tint,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            if (frac > 0.001f) {
                drawArc(
                    color = ringColor.copy(alpha = 0.20f),
                    startAngle = -90f,
                    sweepAngle = 360f * frac,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw * 2.1f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * frac,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = kotlin.math.abs(remaining).toString(),
                color = if (over) c.danger else c.text,
                fontSize = 45.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp
            )
            Text(
                text = if (over) "已经超出" else "还可以吃",
                color = if (over) c.danger else c.text2,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "已摄入 $consumed · 目标 $target",
                color = c.text3,
                fontSize = 11.sp
            )
        }
    }
}

/* ================= 宏量卡片 ================= */

@Composable
fun MacroCard(
    label: String,
    value: Double,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val frac = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .border(1.dp, c.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp)
    ) {
        Text(label, color = c.text3, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formatG(value),
                color = c.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(" / $target", color = c.text3, fontSize = 10.5.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.tint)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

private fun formatG(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}

/* ================= 7 天柱状图 ================= */

@Composable
fun WeekBarChart(
    values: List<Int>,
    target: Int,
    labels: List<String>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 116.dp
) {
    val c = FitTheme.colors
    val maxValue = max(target, (values.maxOrNull() ?: 0)).coerceAtLeast(1)

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - target.toFloat() / maxValue)
                drawLine(
                    color = c.danger.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEachIndexed { _, v ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (v > 0) {
                            val frac = (v.toFloat() / maxValue).coerceIn(0.03f, 1f)
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                if (v > target) c.danger else c.accent,
                                                (if (v > target) c.danger else c.accent).copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { lab ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(lab, color = c.text3, fontSize = 10.5.sp)
                }
            }
        }
    }
}

/* ================= 体重折线 ================= */

@Composable
fun WeightLineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 104.dp
) {
    val c = FitTheme.colors
    if (points.isEmpty()) return

    Canvas(
        modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        var lo = points.min()
        var hi = points.max()
        if (hi - lo < 1.0) {
            hi = lo + 1.0
            lo -= 0.5
        }
        val padTop = 10.dp.toPx()
        val padBottom = 8.dp.toPx()
        val innerH = size.height - padTop - padBottom
        val step = if (points.size == 1) 0f else size.width / (points.size - 1)

        fun px(i: Int): Float = if (points.size == 1) size.width / 2f else step * i
        fun py(v: Double): Float = padTop + (1 - (v - lo) / (hi - lo)).toFloat() * innerH

        val linePath = Path()
        points.forEachIndexed { i, v ->
            val x = px(i)
            val y = py(v)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        if (points.size > 1) {
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fillPath,
                Brush.verticalGradient(
                    listOf(c.accent.copy(alpha = 0.22f), c.accent.copy(alpha = 0f)),
                    startY = padTop,
                    endY = size.height
                )
            )
        }

        drawPath(
            linePath,
            color = c.accentText,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEachIndexed { i, v ->
            drawCircle(c.accentText, radius = 3.dp.toPx(), center = Offset(px(i), py(v)))
        }
    }
}

/* ================= 底部悬浮 Dock ================= */

@Composable
fun FloatingDock(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = FitTheme.colors
    val items = listOf(
        Tab.TODAY to IconKind.Today,
        Tab.LIBRARY to IconKind.Library,
        Tab.STATS to IconKind.Stats,
        Tab.ME to IconKind.Me
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(24.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(28.dp))
            .background(c.glass)
            .border(1.dp, c.line2, RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (tab, icon) ->
            val active = tab == current
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .then(if (active) Modifier.background(c.accent) else Modifier)
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FitIcon(
                        icon,
                        if (active) c.accentInk else c.text3,
                        size = 19.dp,
                        strokeWidth = if (active) 2.1.dp else 1.9.dp
                    )
                    if (active) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tab.label,
                            color = c.accentInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/* ================= 空状态 ================= */

@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = FitTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = c.text3, fontSize = 13.sp, lineHeight = 20.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            PrimaryButton(actionLabel, onClick = onAction)
        }
    }
}

/* ================= 小圆点 ================= */

@Composable
fun Dot(color: Color, size: Dp = 6.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

internal fun Double.safeDiv(other: Double): Double = if (other == 0.0) 0.0 else this / other

internal fun clampPct(v: Float): Float = min(1f, max(0f, v))
