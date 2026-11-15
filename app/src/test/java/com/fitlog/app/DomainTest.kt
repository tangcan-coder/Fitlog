package com.fitlog.app

import com.fitlog.app.data.AppCodec
import com.fitlog.app.data.AppData
import com.fitlog.app.data.Food
import com.fitlog.app.data.LogEntry
import com.fitlog.app.data.Meta
import com.fitlog.app.data.Profile
import com.fitlog.app.data.WeightLog
import com.fitlog.app.domain.BatchImport
import com.fitlog.app.domain.Nutrients
import com.fitlog.app.domain.Pinyin
import com.fitlog.app.domain.kcalFromKj
import com.fitlog.app.domain.kjFromKcal
import com.fitlog.app.domain.kjText
import com.fitlog.app.domain.per100gFromServing
import com.fitlog.app.domain.servingFromPer100g
import com.fitlog.app.domain.targetsOf
import com.fitlog.app.domain.totalsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {

    @Test
    fun `减脂目标与网页版结果一致`() {
        val p = Profile(sex = "male", age = 28, height = 175.0, weight = 70.0, activity = 1.375, goal = "cut", rate = 0.5)
        val t = targetsOf(p)

        // BMR = 10*70 + 6.25*175 - 5*28 + 5 = 1658.75 -> 1659
        // TDEE = 1658.75 * 1.375 = 2280.8 -> 2281
        // 缺口 = 7700*0.5/7 = 550 -> (2280.8-550) 取整到十位 = 1730
        assertEquals(1659, t.bmr)
        assertEquals(2281, t.tdee)
        assertEquals(1730, t.kcal)
        assertEquals(140, t.protein)
        // 三大宏量加起来应当接近目标热量
        val sum = t.protein * 4 + t.carbs * 4 + t.fat * 9
        assertTrue("宏量合计 $sum 应接近 ${t.kcal}", Math.abs(sum - t.kcal) <= 20)
    }

    @Test
    fun `女性维持目标不会低于下限`() {
        val p = Profile(sex = "female", age = 60, height = 150.0, weight = 45.0, activity = 1.2, goal = "cut", rate = 0.75)
        assertTrue(targetsOf(p).kcal >= 1000)
    }

    @Test
    fun `拼音首字母能搜到食物`() {
        assertEquals("mf", Pinyin.initials("米饭"))
        assertEquals("szd", Pinyin.initials("水煮蛋"))
        assertTrue(Pinyin.matches("米饭", Pinyin.initials("米饭"), "mf"))
        assertTrue(Pinyin.matches("米饭", Pinyin.initials("米饭"), "米"))
        assertTrue(Pinyin.matches("米饭", Pinyin.initials("米饭"), "MF"))
        assertTrue(!Pinyin.matches("米饭", Pinyin.initials("米饭"), "jd"))
        assertTrue(Pinyin.matches("米饭", Pinyin.initials("米饭"), ""))
    }

    @Test
    fun `批量导入跳过无效行`() {
        val text = """
            白米饭 230 4.3 48 0.3
            水煮蛋 78
            # 这是注释
            坏行
            苹果 95 0.5 25 0.3
        """.trimIndent()

        val r = BatchImport.parse(text)
        assertEquals(3, r.foods.size)
        assertEquals(1, r.skipped)
        assertEquals("白米饭", r.foods[0].name)
        assertEquals(230.0, r.foods[0].kcal, 0.001)
        assertEquals(0.0, r.foods[1].p, 0.001)
    }

    @Test
    fun `合计按份数累加`() {
        val entries = listOf(
            LogEntry(name = "米饭", qty = 2.0, kcal = 460.0, p = 8.6, c = 96.0, fat = 0.6),
            LogEntry(name = "鸡蛋", qty = 1.0, kcal = 78.0, p = 6.3, c = 0.6, fat = 5.3)
        )
        val t = totalsOf(entries)
        assertEquals(538, t.kcal)
        assertEquals(14.9, t.protein, 0.001)
    }

    @Test
    fun `批量导入支持占位示例里的逗号写法`() {
        val text = """
            白米饭 230 4.3 48 0.3
            水煮蛋 78 6.3 0.6 5.3
            无糖豆浆 60 5.2 1.8 2.5
            苹果,95,0.5,25,0.3
        """.trimIndent()

        val r = BatchImport.parse(text)
        assertEquals(4, r.foods.size)
        assertEquals(0, r.skipped)
        assertEquals("苹果", r.foods[3].name)
        assertEquals(0.3, r.foods[3].fat, 0.001)
        // 默认落在「主食」，别名自动生成为拼音首字母，之后就能直接用 pg 搜到
        assertEquals("主食", r.foods[0].cat)
        assertEquals("份", r.foods[0].unit)
        assertEquals("pg", r.foods[3].alias)
    }

    @Test
    fun `备份 JSON 能原样往返`() {
        val data = AppData(
            foods = listOf(Food(id = "f1", name = "香菇滑鸡", kcal = 320.0, p = 28.0, c = 12.0, fat = 16.0)),
            logs = mapOf(
                "2026-11-14" to listOf(
                    LogEntry(id = "l1", foodId = "f1", name = "香菇滑鸡", qty = 2.0, kcal = 640.0)
                )
            ),
            weights = listOf(WeightLog("2026-11-14", 70.5)),
            profile = Profile(weight = 70.5),
            meta = Meta(onboarded = true)
        )

        val again = AppCodec.decode(AppCodec.encode(data))
        assertTrue("往返后不应为空", again != null)
        assertEquals(1, again!!.foods.size)
        assertEquals("香菇滑鸡", again.foods[0].name)
        assertEquals(640.0, again.logs["2026-11-14"]!![0].kcal, 0.001)
        assertEquals(70.5, again.weights[0].kg, 0.001)
        assertEquals(70.5, again.profile.weight, 0.001)
        assertTrue(again.meta.onboarded)
    }

    @Test
    fun `网页版空 gram 的备份也能导入`() {
        // 网页版把「没填克数」存成空串，直接解析会炸，所以要做归一化
        val web = """{"foods":[{"id":"a","name":"米饭","gram":"","kcal":230}],
            "logs":{},"weights":[],"profile":{"weight":70},"meta":{"onboarded":true}}"""
        val parsed = AppCodec.decode(web)
        assertTrue("空 gram 的旧备份应当能解析", parsed != null)
        assertEquals("米饭", parsed!!.foods[0].name)
        assertEquals(null, parsed.foods[0].gram)
    }

    @Test
    fun `每100克营养按一份克数折算`() {
        // 营养表：200 kcal / 蛋白 10 g / 碳水 20 g / 脂肪 5 g（每 100 g），这一份 150 g
        val per100 = Nutrients(kcal = 200.0, p = 10.0, c = 20.0, fat = 5.0)
        val one = servingFromPer100g(per100, 150.0)!!

        assertEquals(300.0, one.kcal, 0.001)
        assertEquals(15.0, one.p, 0.001)
        assertEquals(30.0, one.c, 0.001)
        assertEquals(7.5, one.fat, 0.001)
    }

    @Test
    fun `克数没填或非法时不折算`() {
        val per100 = Nutrients(kcal = 100.0)
        assertNull(servingFromPer100g(per100, null))
        assertNull(servingFromPer100g(per100, 0.0))
        assertNull(servingFromPer100g(per100, -5.0))
        assertNull(per100gFromServing(per100, null))
    }

    @Test
    fun `小数折算按四舍五入进位`() {
        // 0.3 g / 100 g × 150 g 应当正好是 0.45 g，但 double 会算成 0.44999999999999996
        val one = servingFromPer100g(Nutrients(fat = 0.3), 150.0)!!
        assertEquals(0.5, one.fat, 0.001)
    }

    @Test
    fun `能量按每千卡4184千焦折算`() {
        assertEquals(4.184, kjFromKcal(1.0), 0.0001)
        assertEquals(100.0, kcalFromKj(418.4), 0.0001)

        // 营养表：每 100 g 690 kJ，这一份 150 g -> 690/4.184*1.5 ≈ 247.4 kcal
        val per100 = Nutrients(kcal = kcalFromKj(690.0))
        assertEquals(247.4, servingFromPer100g(per100, 150.0)!!.kcal, 0.05)
    }

    @Test
    fun `能量输入框取整显示，回填不会掉成6899`() {
        assertEquals("690", kjFromKcal(kcalFromKj(690.0)).kjText())

        // 存档里只有一位小数的 kcal：690 kJ → 一份 247.4 kcal，反推回来仍应显示 690
        val per100Kcal = per100gFromServing(Nutrients(kcal = 247.4), 150.0)!!.kcal
        assertEquals("690", kjFromKcal(per100Kcal).kjText())
    }

    @Test
    fun `批量导入支持带kj的能量`() {
        // 带 kj 后缀按 kJ 换算；裸数字仍然是 kcal（否则旧文本会被读小四倍）
        val kjLine = BatchImport.parse("水煮蛋 326kj 6.3 0.6 5.3")
        assertEquals(1, kjLine.foods.size)
        assertEquals(77.9, kjLine.foods[0].kcal, 0.1)

        val kcalLine = BatchImport.parse("白米饭 230 4.3 48 0.3")
        assertEquals(230.0, kcalLine.foods[0].kcal, 0.001)
    }

    @Test
    fun `每份与每100克两个方向能往返`() {
        // 模拟真实包装：米饭每 100 g 116 kcal / 蛋白 2.6 / 碳水 25.9 / 脂肪 0.3，一份 150 g
        val per100 = Nutrients(kcal = 116.0, p = 2.6, c = 25.9, fat = 0.3)
        val one = servingFromPer100g(per100, 150.0)!!
        assertEquals(174.0, one.kcal, 0.001)

        // 编辑已有食物时要把「一份」反推回营养表的写法，再折回来不能走样
        val back = per100gFromServing(one, 150.0)!!
        assertEquals(116.0, back.kcal, 0.001)
        assertEquals(2.6, back.p, 0.001)
        // 只保留一位小数，碳水和脂肪容许 0.1 的误差
        assertEquals(25.9, back.c, 0.1)
        assertEquals(0.3, back.fat, 0.1)
        assertEquals(174.0, servingFromPer100g(back, 150.0)!!.kcal, 0.05)
    }
}
