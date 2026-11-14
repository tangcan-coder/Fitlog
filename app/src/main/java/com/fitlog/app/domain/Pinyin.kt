package com.fitlog.app.domain

/**
 * 只有一份食物的中文名，不需要完整拼音库。
 * 这里用「常用汉字 → 声母」的对照表实现首字母检索：
 * 输入 mf 能搜到「米饭」，st 能搜到「水煮蛋」。
 */
object Pinyin {

    private val groups: Map<Char, String> = run {
        val map = HashMap<Char, String>()
        fun add(letter: Char, chars: String) {
            chars.forEach { map[it] = letter.toString() }
        }

        add('a', "阿")
        add('b', "白百包薄饼菠卜冰八拔拌爆杯板半便不北贝")
        add('c', "菜草橙茶肠翅醋葱糙炒春纯脆醇叉陈成粗蚕")
        add('d', "蛋豆大冬地带冻淀肚多丁点低稻雕")
        add('e', "二")
        add('f', "饭粉番肥蜂凤腐富份发脯方")
        add('g', "干甘果瓜骨糕桂锅高谷挂罐根鸽菇棍")
        add('h', "红黄花火河黑胡海荷滑烩茴合糊混禾核")
        add('j', "鸡酒酱姜橘韭蕉卷夹煎饺芥金精菌净九桔筋豇")
        add('k', "咖烤苦快葵口块筷宽空")
        add('l', "老冷栗莲绿萝辣酪梨荔粒炼零龙芦卤鹿螺烙里两励")
        add('m', "米面麦蘑木芒蜜梅母麻馒毛玫美磨末苗目明")
        add('n', "牛南奶年柠糯拿嫩泥酿内农女")
        add('o', "藕")
        add('p', "排皮苹葡片泡啤培配瓶平脯枇蟠")
        add('q', "青芹茄全清荞轻曲七千秋麒雀切")
        add('r', "肉乳日热仁软蓉润")
        add('s', "水生薯丝笋蒜酸食三沙山烧少十石士瘦蔬熟素四松酥舌神善")
        add('t', "土汤糖条头兔腿甜田调铁通团炭屠")
        add('w', "五味豌莴乌无丸微维尾温午王文窝")
        add('x', "香虾西小杏鲜咸稀细下仙现线心新星雪熏馅")
        add('y', "玉燕鸭鱼油圆腰椰洋芋盐芽腌羊野叶一银饮樱营硬优原月云")
        add('z', "紫猪蒸粥中炸竹枣子杂仔早粘蘸掌芝汁制质重煮主棕走足最")

        map
    }

    /** 取中文名中每个字的声母，非汉字直接跳过 */
    fun initials(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            groups[ch]?.let { sb.append(it) }
        }
        return sb.toString()
    }

    /** 搜索匹配：中文名包含、或首字母包含 */
    fun matches(name: String, initials: String, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        if (name.lowercase().contains(q)) return true
        return initials.lowercase().contains(q)
    }
}
