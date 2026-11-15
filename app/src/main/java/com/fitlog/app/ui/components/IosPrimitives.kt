package com.fitlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.data.Cats
import com.fitlog.app.domain.Targets
import com.fitlog.app.domain.Totals
import com.fitlog.app.ui.theme.FitPalette
import com.kyant.capsule.ContinuousRoundedRectangle
import com.kyant.shapes.Capsule
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.core.segmentedControlBackground
import io.github.neoruaa.glasmio.core.systemGray5
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.ui.glasmioGroupedListSeparator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/* ======================================================================
 * iOS 风格的基础零件：分组标题、搜索框、表单行、分段控件、按钮、图表。
 * 玻璃材质只交给 Glasmio 的导航栏 / 工具栏 / 弹层使用；页面内的卡片使用
 * iOS 分组列表的实色底，避免在自己的背景录制层里采样自己。
 * ==================================================================== */

private fun trim(value: Double): String {
    val r = (value * 10).roundToInt() / 10.0
    return if (r == floor(r)) r.toInt().toString() else r.toString()
}

fun formatGrams(value: Double): String = trim(value)

/** 分组标题：iOS 分组列表上方的小写灰字。 */
@Composable
fun IosSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = IosTypography.footnote, color = colors.secondaryContent)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** 分组列表下方的说明文字。传 [color] 可以把它当表单错误提示用。 */
@Composable
fun IosGroupFooter(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val colors = GlasmioTheme.colors
    Text(
        text,
        style = IosTypography.footnote,
        color = if (color == Color.Unspecified) colors.secondaryContent else color,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 7.dp),
    )
}

/** iOS 搜索框：胶囊灰底 + 放大镜 + 可清空。 */
@Composable
fun FitSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(Capsule())
            .background(colors.segmentedControlBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SfIcon("magnifyingglass", null, size = 17.dp, tint = colors.tertiaryContent)
        Spacer(Modifier.width(7.dp))
        // 同上：占位符放进 decorationBox，避免输入第一个字后输入框被重建、光标跳回开头。
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = IosTypography.body.copy(color = colors.content),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, style = IosTypography.body, color = colors.tertiaryContent, maxLines = 1)
                }
                innerTextField()
            },
        )
        if (value.isNotEmpty()) {
            SfIcon(
                "xmark.circle.fill",
                "清除",
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") },
                size = 17.dp,
                tint = colors.tertiaryContent,
            )
        }
    }
}

/** 分类筛选胶囊，横向可滚动。 */
@Composable
fun CategoryChips(
    items: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val dark = colors.isDark
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            val active = item == selected
            val background by animateColorAsState(
                targetValue = if (active) FitPalette.accent(dark) else colors.segmentedControlBackground,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipBackground",
            )
            Box(
                Modifier
                    .clip(Capsule())
                    .background(background)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    item,
                    style = IosTypography.subheadline,
                    fontWeight = if (active) FontWeight(600) else FontWeight(400),
                    color = if (active) FitPalette.onAccent(dark) else colors.content,
                )
            }
        }
    }
}

/** 表单行：左边标签、右边输入。放在 GlasmioGroupedList 里就是一堆 iOS 设置项。 */
@Composable
fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String = "",
    showTopSeparator: Boolean = true,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = IosTypography.body, color = colors.content)
        Spacer(Modifier.width(16.dp))
        // 占位符必须放在 decorationBox 里：如果它和输入框是兄弟节点，输入第一个字符后
        // 占位符消失会让输入框在组合树里换位置、被重建，光标回到 0，于是「23」会变成「32」。
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = IosTypography.body.copy(color = colors.content, textAlign = TextAlign.End),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = IosTypography.body,
                        color = colors.tertiaryContent,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            },
        )
        if (suffix.isNotEmpty()) {
            Spacer(Modifier.width(4.dp))
            Text(suffix, style = IosTypography.body, color = colors.tertiaryContent)
        }
    }
}

/** 只读键值行（标签 + 右侧值），用于「我的」页汇总。 */
@Composable
fun ValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showTopSeparator: Boolean = true,
    valueColor: Color = Color.Unspecified,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = IosTypography.body, color = colors.content)
        Spacer(Modifier.weight(1f).width(12.dp))
        Text(
            value,
            style = IosTypography.body,
            color = if (valueColor == Color.Unspecified) colors.secondaryContent else valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

/** iOS 分段控件（自绘）：灰底胶囊 + 白色滑块，切换时滑动。 */
@Composable
fun SegmentedRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    val colors = GlasmioTheme.colors
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height + 4.dp)
            .clip(Capsule())
            .background(colors.segmentedControlBackground),
    ) {
        val itemWidth = maxWidth / options.size
        val offset by animateDpAsState(
            targetValue = itemWidth * index,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            label = "segmentOffset",
        )
        Box(
            Modifier
                .offset(x = offset)
                .padding(2.dp)
                .width(itemWidth - 4.dp)
                .fillMaxHeight()
                .clip(Capsule())
                .background(colors.elevatedBackground),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEach { (value, label) ->
                val active = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(Capsule())
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = IosTypography.subheadline,
                        fontWeight = if (active) FontWeight(600) else FontWeight(400),
                        color = if (active) colors.content else colors.secondaryContent,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 主按钮：实色强调胶囊。 */
@Composable
fun PrimaryCapsule(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    destructive: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    val fill = when {
        !enabled -> colors.segmentedControlBackground
        destructive -> FitPalette.danger(colors.isDark)
        else -> colors.accent
    }
    val ink = when {
        !enabled -> colors.tertiaryContent
        destructive -> Color.White
        else -> FitPalette.onAccent(colors.isDark)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(Capsule())
            .background(fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = IosTypography.headline, fontWeight = FontWeight(600), color = ink)
    }
}

/** 文字按钮。 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val colors = GlasmioTheme.colors
    Text(
        text,
        style = IosTypography.body,
        color = when {
            !enabled -> colors.tertiaryContent
            destructive -> colors.destructive
            else -> colors.accent
        },
        modifier = modifier
            .clip(Capsule())
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/**
 * 把分组列表拆成独立 LazyColumn item 时，用来拼出「首行圆上角、末行圆下角」的形状。
 * 这样每一行都能被 LazyColumn 单独虚拟化，长列表不会整组一起组合。
 */
fun groupedRowShape(isFirst: Boolean, isLast: Boolean, radius: Dp = 26.dp): Shape =
    RoundedCornerShape(
        topStart = if (isFirst) radius else 0.dp,
        topEnd = if (isFirst) radius else 0.dp,
        bottomStart = if (isLast) radius else 0.dp,
        bottomEnd = if (isLast) radius else 0.dp,
    )

/** 食物分类的小图标块：灰底圆角方块 + emoji，iOS 列表里的 leading 位。 */@Composable
fun FoodEmojiBox(category: String, modifier: Modifier = Modifier) {
    val colors = GlasmioTheme.colors
    Box(
        modifier
            .size(30.dp)
            .clip(ContinuousRoundedRectangle(9.dp))
            .background(colors.systemGray5),
        contentAlignment = Alignment.Center,
    ) {
        Text(Cats.icon(category), fontSize = 15.sp)
    }
}

/** 分组列表标题右侧的小加号。 */@Composable
fun TinyAddButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Box(
        modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon("plus", contentDescription, size = 15.dp, tint = colors.accent)
    }
}

/** 数量步进胶囊：−  2  ＋。 */
@Composable
fun StepperPill(
    count: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .height(34.dp)
            .clip(Capsule())
            .background(colors.segmentedControlBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("minus", "减少一份", enabled = count > 0, onClick = onMinus)
        Text(
            "$count",
            style = IosTypography.subheadline,
            fontWeight = FontWeight(600),
            color = colors.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp),
        )
        StepperButton("plus", "增加一份", enabled = true, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(
    systemName: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = GlasmioTheme.colors
    Box(
        Modifier
            .size(width = 40.dp, height = 34.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SfIcon(
            systemName,
            description,
            size = 15.dp,
            tint = if (enabled) colors.content else colors.tertiaryContent.copy(alpha = 0.5f),
        )
    }
}

/* ------------------------------ 图表 ------------------------------ */

/** 热量环 + 剩余热量。 */
@Composable
fun CalorieRingCard(
    consumed: Int,
    target: Int,
    modifier: Modifier = Modifier,
    ringSize: Dp = 196.dp,
    stroke: Dp = 17.dp,
) {
    val colors = GlasmioTheme.colors
    val dark = colors.isDark
    val remaining = target - consumed
    val over = remaining < 0
    val progress = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f)
    val tint = if (over) FitPalette.danger(dark) else FitPalette.accent(dark)

    Box(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(28.dp))
            .background(colors.elevatedBackground)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val strokePx = stroke.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                drawArc(
                    color = colors.systemGray5,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                if (progress > 0.002f) {
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(tint.copy(alpha = 0.72f), tint),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    abs(remaining).toString(),
                    style = IosTypography.largeTitle,
                    fontWeight = FontWeight(700),
                    color = if (over) tint else colors.content,
                )
                Text(
                    if (over) "已超出目标" else "还可以吃",
                    style = IosTypography.subheadline,
                    color = if (over) tint else colors.secondaryContent,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "已摄入 $consumed · 目标 $target kcal",
                    style = IosTypography.caption2,
                    color = colors.tertiaryContent,
                )
            }
        }
    }
}

/** 蛋白 / 碳水 / 脂肪三栏。 */
@Composable
fun MacroCard(totals: Totals, targets: Targets, modifier: Modifier = Modifier) {
    val colors = GlasmioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(colors.elevatedBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MacroColumn("蛋白", totals.protein, targets.protein, FitPalette.protein, Modifier.weight(1f))
        MacroDivider()
        MacroColumn("碳水", totals.carbs, targets.carbs, FitPalette.carbs, Modifier.weight(1f))
        MacroDivider()
        MacroColumn("脂肪", totals.fat, targets.fat, FitPalette.fat, Modifier.weight(1f))
    }
}

@Composable
private fun MacroDivider() {
    val colors = GlasmioTheme.colors
    Box(
        Modifier
            .padding(horizontal = 10.dp)
            .width(1.dp)
            .height(42.dp)
            .background(colors.separator),
    )
}

@Composable
private fun MacroColumn(
    label: String,
    value: Double,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val frac = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    // 超出目标时整块变红，和热量环的「已超标」保持同一套语义。
    val over = target > 0 && value > target
    val barColor = if (over) FitPalette.danger(colors.isDark) else color
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(barColor),
            )
            Spacer(Modifier.width(5.dp))
            Text(label, style = IosTypography.footnote, color = colors.secondaryContent)
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatGrams(value),
                style = IosTypography.title3,
                fontWeight = FontWeight(600),
                color = if (over) barColor else colors.content,
            )
            Text(
                " / ${target}g",
                style = IosTypography.caption2,
                color = colors.tertiaryContent,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(Capsule())
                .background(colors.systemGray5),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clip(Capsule())
                    .background(barColor),
            )
        }
    }
}

/** 体重趋势折线。 */
@Composable
fun WeightLineChart(
    points: List<Double>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 108.dp,
) {
    val colors = GlasmioTheme.colors
    val accent = FitPalette.accent(colors.isDark)
    if (points.isEmpty()) return

    Canvas(
        modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        var lo = points.min()
        var hi = points.max()
        if (hi - lo < 1.0) {
            hi = lo + 1.0
            lo -= 0.5
        }
        val padTop = 10.dp.toPx()
        val padBottom = 10.dp.toPx()
        val innerH = size.height - padTop - padBottom
        val step = if (points.size == 1) 0f else size.width / (points.size - 1)

        fun px(i: Int): Float = if (points.size == 1) size.width / 2f else step * i
        fun py(v: Double): Float = padTop + (1 - (v - lo) / (hi - lo)).toFloat() * innerH

        val linePath = Path()
        points.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(px(i), py(v)) else linePath.lineTo(px(i), py(v))
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
                    listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0f)),
                    startY = padTop,
                    endY = size.height,
                ),
            )
        }

        drawPath(linePath, color = accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEachIndexed { i, v ->
            drawCircle(accent, radius = 3.dp.toPx(), center = Offset(px(i), py(v)))
        }
    }
}

/** 小块统计数字。 */
@Composable
fun StatTile(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    Column(
        modifier
            .clip(ContinuousRoundedRectangle(24.dp))
            .background(colors.elevatedBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = IosTypography.footnote, color = colors.secondaryContent)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = IosTypography.title1,
                fontWeight = FontWeight(600),
                color = colors.content,
            )
            Text(
                unit,
                style = IosTypography.caption2,
                color = colors.tertiaryContent,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

/** 空态提示。 */
@Composable
fun EmptyHint(
    text: String,
    modifier: Modifier = Modifier,
    symbol: String? = null,
) {
    val colors = GlasmioTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (symbol != null) {
            SfIcon(symbol, null, size = 26.dp, tint = colors.tertiaryContent)
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text,
            style = IosTypography.subheadline,
            color = colors.tertiaryContent,
            textAlign = TextAlign.Center,
        )
    }
}
