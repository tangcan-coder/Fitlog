package com.fitlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextOverflow
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
            // 分隔线两端留白，不贴到卡片圆角上（和列表行保持一致）
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator, horizontalInset = 16.dp)
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
            // 分隔线两端留白，不贴到卡片圆角上（和列表行保持一致）
            .glasmioGroupedListSeparator(colors.separator, visible = showTopSeparator, horizontalInset = 16.dp)
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
                // 选中块用品牌绿：和主按钮、环形图、勾选同一套强调色
                .background(colors.accent),
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
                        color = if (active) FitPalette.onAccent(colors.isDark) else colors.secondaryContent,
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

/* ------------------------- 今日概览：同心圆环 ------------------------- */

/**
 * 今日营养概览：左边四圈同心环，右边四组「标签 + 数值」。
 *
 * 版式对齐健身 App 的活动圆环：环只表达比例，数值全部在环外、并且染成对应的环色，
 * 一眼就能把「哪一圈 = 哪一项」对上。最外圈是热量，往内依次是蛋白、碳水、脂肪，
 * 顺序和右边四行完全一致。
 *
 * 这里不再放「还可以吃多少」的余额胶囊：环和分数已经说明问题，
 * 多一个胶囊只会把卡片撑长。
 */
@Composable
fun NutritionRingsCard(
    totals: Totals,
    targets: Targets,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(26.dp))
            .background(GlasmioTheme.colors.elevatedBackground)
            .padding(16.dp),
    ) {
        NutritionRingsBody(totals = totals, targets = targets)
    }
}

/**
 * 「四圈环 + 四组数值」这一段，今日页和统计页的当日详情共用，
 * 保证两处是同一套东西，改一处两边都跟着变。
 */
@Composable
fun NutritionRingsBody(
    totals: Totals,
    targets: Targets,
    modifier: Modifier = Modifier,
    ringSize: Dp = 188.dp,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        MacroRings(
            kcal = ratioOf(totals.kcal.toDouble(), targets.kcal),
            protein = ratioOf(totals.protein, targets.protein),
            carbs = ratioOf(totals.carbs, targets.carbs),
            fat = ratioOf(totals.fat, targets.fat),
            overKcal = targets.kcal > 0 && totals.kcal > targets.kcal,
            overProtein = targets.protein > 0 && totals.protein > targets.protein,
            overCarbs = targets.carbs > 0 && totals.carbs > targets.carbs,
            overFat = targets.fat > 0 && totals.fat > targets.fat,
            modifier = Modifier.size(ringSize),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MetricLine(
                label = "热量",
                value = totals.kcal.toString(),
                target = targets.kcal,
                unit = "千卡",
                color = GlasmioTheme.colors.accent,
                over = targets.kcal > 0 && totals.kcal > targets.kcal,
            )
            MetricLine(
                label = "蛋白",
                value = formatGrams(totals.protein),
                target = targets.protein,
                unit = "克",
                color = FitPalette.protein,
                over = targets.protein > 0 && totals.protein > targets.protein,
            )
            MetricLine(
                label = "碳水",
                value = formatGrams(totals.carbs),
                target = targets.carbs,
                unit = "克",
                color = FitPalette.carbs,
                over = targets.carbs > 0 && totals.carbs > targets.carbs,
            )
            MetricLine(
                label = "脂肪",
                value = formatGrams(totals.fat),
                target = targets.fat,
                unit = "克",
                color = FitPalette.fat,
                over = targets.fat > 0 && totals.fat > targets.fat,
            )
        }
    }
}

private fun ratioOf(value: Double, target: Int): Float =
    if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)

/** 一组「小标签 + 大数字/目标」；数字用环色，超标时整组转红。 */
@Composable
private fun MetricLine(
    label: String,
    value: String,
    target: Int,
    unit: String,
    color: Color,
    over: Boolean,
) {
    val colors = GlasmioTheme.colors
    val tint = if (over) FitPalette.danger(colors.isDark) else color
    Column {
        // 单位跟着标签走：数值行只放「已吃/目标」，系统字体调大或屏幕窄时也不会把单位挤出边界
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = IosTypography.footnote,
                color = colors.secondaryContent,
                maxLines = 1,
            )
            Text(
                "（$unit）",
                style = IosTypography.caption2,
                color = colors.tertiaryContent,
                maxLines = 1,
            )
        }
        Text(
            "$value/$target",
            style = IosTypography.title3,
            fontWeight = FontWeight(600),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 四圈同心环：外圈热量、再蛋白、碳水、脂肪。
 *
 * 进度用动画推过去（切日期、记完一条都能看到环长出来），
 * 轨道和进度都是 Canvas 画的，不需要图片资源，也不走玻璃采样。
 */
@Composable
private fun MacroRings(
    kcal: Float,
    protein: Float,
    carbs: Float,
    fat: Float,
    overKcal: Boolean,
    overProtein: Boolean,
    overCarbs: Boolean,
    overFat: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = GlasmioTheme.colors
    val dark = colors.isDark
    val danger = FitPalette.danger(dark)
    val kcalProgress by animateFloatAsState(kcal, tween(700, easing = FastOutSlowInEasing), label = "kcalRing")
    val proteinProgress by animateFloatAsState(protein, tween(700, easing = FastOutSlowInEasing), label = "proteinRing")
    val carbsProgress by animateFloatAsState(carbs, tween(700, easing = FastOutSlowInEasing), label = "carbsRing")
    val fatProgress by animateFloatAsState(fat, tween(700, easing = FastOutSlowInEasing), label = "fatRing")
    val track = colors.systemGray5

    Canvas(modifier) {
        val stroke = 16.dp.toPx()
        // 圈与圈之间留一点缝：几个值都很小时（比如刚记录一项），
        // 贴在一起的圆头会糊成一块色斑，分开才能看出是四个环。
        val gap = 4.dp.toPx()
        val step = stroke + gap
        val outer = size.minDimension / 2f - stroke / 2f

        fun ring(progress: Float, color: Color, radius: Float) {
            val topLeft = Offset(center.x - radius, center.y - radius)
            val diameter = Size(radius * 2f, radius * 2f)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = diameter,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0.002f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = diameter,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        ring(kcalProgress, if (overKcal) danger else colors.accent, outer)
        ring(proteinProgress, if (overProtein) danger else FitPalette.protein, outer - step)
        ring(carbsProgress, if (overCarbs) danger else FitPalette.carbs, outer - step * 2f)
        ring(fatProgress, if (overFat) danger else FitPalette.fat, outer - step * 3f)
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
