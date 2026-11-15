package com.fitlog.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Dates {

    private val md = DateTimeFormatter.ofPattern("M月d日")

    fun today(): String = LocalDate.now().toString()

    fun parse(ymd: String): LocalDate =
        runCatching { LocalDate.parse(ymd) }.getOrElse { LocalDate.now() }

    fun shift(ymd: String, days: Long): String = parse(ymd).plusDays(days).toString()

    fun weekdayLabel(date: LocalDate): String {
        val names = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names[date.dayOfWeek.value - 1]
    }

    /** 今天 / 昨天 / 11月14日 周六 */
    fun human(ymd: String): String {
        val t = today()
        if (ymd == t) return "今天"
        if (ymd == shift(t, -1)) return "昨天"
        val d = parse(ymd)
        return md.format(d) + " " + weekdayLabel(d)
    }

    fun short(ymd: String): String = md.format(parse(ymd))

    /**
     * 回到前台时该把日期顺延到哪一天。
     *
     * 只有「当前还停留在上一轮的今天」才跟着系统日期走；用户手动翻到别的日期
     * （查看历史、补记前几天）时不能抢他的选择。返回 null 表示不用改。
     */
    fun rolledForward(current: String, lastKnownToday: String, today: String): String? =
        if (today != lastKnownToday && current == lastKnownToday) today else null
}
