package com.fitlog.app.domain

/**
 * 营养换算：包装食品的营养表几乎都是「每 100 克」标注，而 App 内部统一按「一份」记账。
 *
 * 一份 150 g 的食物，营养表写蛋白 12 g / 100 g，实际就是 18 g。这两步换算都是纯算术，
 * 抽到这里既给表单的实时预览用，也能直接单测（不依赖 Android）。
 *
 * 数值统一保留一位小数：营养表本身精度就到 0.1，再细没有意义，
 * 还能避免折算回一份之后出现 173.99999 这种脏数据。
 */
data class Nutrients(
    val kcal: Double = 0.0,
    val p: Double = 0.0,
    val c: Double = 0.0,
    val fat: Double = 0.0
)

/** 每次记录都按「一份」折算，所以两个方向的换算都以一份的实际克数为基准。 */
fun Nutrients.scaled(factor: Double): Nutrients = Nutrients(
    kcal = kcal * factor,
    p = p * factor,
    c = c * factor,
    fat = fat * factor
)

/**
 * 保留一位小数。
 *
 * 这里加了一个极小量再取整：0.3 × 1.5 在 double 里其实是 0.44999999999999996，
 * 直接 round 会把「0.45」舍成 0.4，看起来就像算错了一档。补上 EPS 之后，
 * 真正等于 x.x5 的数值会按四舍五入进位（0.45 → 0.5），和手算结果一致。
 */
private const val EPS = 1e-9

private fun rounded1(value: Double): Double = kotlin.math.round(value * 10 + EPS) / 10.0

fun Nutrients.round1(): Nutrients = Nutrients(
    kcal = rounded1(kcal),
    p = rounded1(p),
    c = rounded1(c),
    fat = rounded1(fat)
)

/**
 * 每 100 g 的营养 × 这一份的克数 → 一份的营养。
 * 克数没填、或者填了 0 及负数时返回 null，由调用方提示用户去补。
 */
fun servingFromPer100g(per100g: Nutrients, gram: Double?): Nutrients? {
    if (gram == null || gram <= 0) return null
    return per100g.scaled(gram / 100.0).round1()
}

/**
 * 一份的营养 → 每 100 g。编辑已经有克数的食物时，把存档里的「每份值」反推回营养表的写法，
 * 这样用户看到的就是当初包装上那几行数字，而不是自己换算过的结果。
 */
fun per100gFromServing(serving: Nutrients, gram: Double?): Nutrients? {
    if (gram == null || gram <= 0) return null
    return serving.scaled(100.0 / gram).round1()
}
