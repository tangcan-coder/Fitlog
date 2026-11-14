# FitLog · 安卓版

自用的本地饮食记录 App。纯离线，数据只放在手机里，不联网、不登录、没有网络权限。

原生 Kotlin + Jetpack Compose 写的（不是网页套壳），界面是深色底 + 荧光绿 + 悬浮 Dock。

- **工程位置**：`D:\AndroidProjects\FitLog`
- **包名**：`com.fitlog.app`
- **当前版本**：1.0（versionCode 1）

---

## 一、环境

| 项 | 版本 / 位置 |
|---|---|
| Android Studio | `D:\android studio` |
| JDK | Studio 自带 JBR 25（`D:\android studio\jbr`） |
| Android SDK | `C:\Users\ASUS\AppData\Local\Android\Sdk`（写在 `local.properties`） |
| SDK Platform | API 37（Android 17） |
| build-tools | 36.0.0 |
| Gradle | 9.9.1（wrapper 自动下载，已配腾讯镜像） |
| AGP | 9.5.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| minSdk / targetSdk | 26 / 37 |

> **换电脑时要改一处**：`local.properties` 里的 `sdk.dir` 是绝对路径。
> 换机器直接删掉这个文件，Studio 打开工程时会自己重新生成。

---

## 二、第一次打开

1. Android Studio → **File → Open** → 选 `D:\AndroidProjects\FitLog`
   （选这个根目录，**不要**选里面的 `app`）
2. 等右下角 Gradle 同步跑完。首次要下依赖，约 5–10 分钟；之后打开是秒同步。
3. 顶部选设备：
   - **真机**：手机「设置 → 关于手机 → 连点版本号 7 次」→ 开发者选项 → USB 调试 → 插线并在手机上允许调试
   - **模拟器**：Device Manager → Create Device，选 API 37，建议 Graphics 设为 `Hardware - GLES 2.0`（软件渲染会非常慢）
4. 点绿色 ▶ 运行（`Shift + F10`）

---

## 三、怎么调试

### 1. 断点调试（最常用）

- 代码行号左侧点一下 → 出现红点就是断点
- 顶部把运行配置从 **Run** 切成 **Debug**（小虫子图标），或按 `Shift + F9`
- 停在断点时：
  - `F8` 单步跳过、`F7` 单步进入、`Shift + F8` 跳出
  - `Alt + F8` 打开表达式求值，可以现场改值、调函数
  - `Variables` 面板能展开 `data`、`ui` 这些状态对象看内部字段
- 应用已经在跑、想临时接入：**Run → Attach Debugger to Android Process** → 选 `com.fitlog.app`

**建议先在这几个地方下断点**，能把整条数据流串起来：

| 位置 | 看什么 |
|---|---|
| `AppViewModel.commitSelection` | 一顿饭入账时，份量怎么变成记录条目 |
| `AppViewModel.saveFoodForm` | 保存食物时怎么校验、id 怎么生成 |
| `AppRepository.mutate` | 每次写入前的完整 `AppData` 快照 |
| `domain/Calc.kt` 的 `targetsOf` | 目标热量是怎么算出来的 |

### 2. Compose 界面预览（改样式不用装手机）

`ui/Previews.kt` 里已经准备好 5 个预览：今日（有记录 / 空）、食物库、统计、我的，深色浅色都有。

- 打开这个文件，右上角切到 **Split** 或 **Design**，右侧就是模拟画面
- 改颜色、改间距，预览面板会实时刷新
- 预览用的是 `Sample` 里的假数据，**不会影响手机上的真实数据**
- 想加新预览：照抄一个 `@Preview` 函数改参数即可

弹层（新建食物 / 批量导入 / 资料）依赖 ViewModel，预览里看不到，这部分只能跑起来点。

### 3. 看界面层级和重组次数

**Tools → Layout Inspector**（应用运行时才能用）：

- 看每个 Composable 的边界、尺寸、实际生效的 modifier
- 勾上 **Show Recomposition Counts**：改一处数据后某个节点重组了几十次，就是那里该加 `remember` 了
- 颜色、字体、padding 到底被谁覆盖，在这里看比猜快

### 4. 看日志

`Logcat` 面板（底部工具栏那只猫）：

- 过滤栏输入 `package:com.fitlog.app`，只看这个应用
- 代码里目前没留日志。想临时加就写 `android.util.Log.d("FitLog", "xxx")`，然后在过滤栏输入 `tag:FitLog`
- 崩溃一定从这里看：搜 `FATAL EXCEPTION`，下面 `Caused by` 那行才是根因

### 5. 性能

**Run → Profiler**：CPU 看主线程卡在哪（Compose 卡顿多半是重组太多），Memory 看有没有泄漏。
注意 debug 包本身有额外开销，数字偏悲观，**要看真实表现请装 release 包**。

### 6. 清空数据重新来一遍

调试经常需要「回到刚装好的状态」，三种方式任选：

```bash
# 方式一：清空应用数据（最干净，等于刚安装）
adb shell pm clear com.fitlog.app

# 方式二：只删数据文件，保留应用
adb shell run-as com.fitlog.app rm -f files/datastore/fitlog.preferences_pb

# 方式三：直接看数据文件（就是一份 JSON，能肉眼核对）
adb exec-out run-as com.fitlog.app cat files/datastore/fitlog.preferences_pb
```

在 Studio 里也能看：**View → Tool Windows → Device Explorer** →
`data/data/com.fitlog.app/files/datastore/fitlog.preferences_pb`，右键 Save As 导出。

---

## 四、命令行也行

工程自带 Gradle Wrapper，不装 Gradle 也能跑：

```bat
cd /d D:\AndroidProjects\FitLog

rem 构建 debug 包，产物在 app\build\outputs\apk\debug\app-debug.apk
gradlew.bat assembleDebug

rem 直接装到已连接的设备
gradlew.bat installDebug

rem 跑单元测试
gradlew.bat testDebugUnitTest
```

用 Git Bash 就把 `gradlew.bat` 换成 `./gradlew`，并先指定 JDK：

```bash
export JAVA_HOME="/d/android studio/jbr"
```

---

## 五、目录结构

```
D:\AndroidProjects\FitLog\
├── settings.gradle.kts            工程名与模块声明
├── build.gradle.kts               根构建脚本（含 KGP 版本抬升，见第六节）
├── gradle.properties              Gradle 内存与缓存开关
├── local.properties               SDK 路径（本机专用，不进版本库）
├── .gitignore
├── gradle\
│   ├── libs.versions.toml         所有依赖版本集中在这里
│   └── wrapper\                   自带 Gradle 9.9.1
└── app\
    ├── build.gradle.kts           模块配置：SDK 版本、依赖
    ├── proguard-rules.pro
    └── src\
        ├── main\
        │   ├── AndroidManifest.xml
        │   ├── java\com\fitlog\app\
        │   │   ├── MainActivity.kt            入口，只负责挂载 Compose
        │   │   ├── data\
        │   │   │   ├── Models.kt              数据模型（字段与网页版 JSON 一致）
        │   │   │   ├── AppCodec.kt            AppData ↔ JSON，导入容错
        │   │   │   └── AppRepository.kt       DataStore 持久化，原子读改写
        │   │   ├── domain\                    纯逻辑，不依赖 Android，可直接单测
        │   │   │   ├── Calc.kt                TDEE / 三大宏量 / 汇总
        │   │   │   ├── Dates.kt               日期工具
        │   │   │   ├── Pinyin.kt              首字母检索（mf → 米饭）
        │   │   │   └── BatchImport.kt         批量粘贴解析
        │   │   └── ui\
        │   │       ├── AppViewModel.kt        唯一状态源，所有操作都走这里
        │   │       ├── FitLogApp.kt           骨架：背景 + 页面切换 + Dock + 弹层
        │   │       ├── Previews.kt            Studio 界面预览（★ 改样式先看这里）
        │   │       ├── theme\Theme.kt         深/浅色两套色板
        │   │       ├── components\            卡片、按钮、输入框、图表、图标（全自绘）
        │   │       ├── screens\               今日 / 选择食物 / 食物库 / 统计 / 我的
        │   │       └── sheets\                新建食物 / 编辑记录 / 批量导入 / 资料
        │   └── res\                           图标、主题、颜色
        └── test\java\com\fitlog\app\
            └── DomainTest.kt                  8 条单元测试
```

---

## 六、设计上的几个决定

- **没有用 Room。** 这个数据量（几百种食物、几年记录）一份 JSON 完全够，
  换来的是导出、导入、换机迁移都只是读写一个文件，调试时也能直接 `cat` 出来看。
- **没有引图标库和图表库。** Tab 图标、操作图标、柱状图、折线图全用 Canvas 画，
  少两个依赖，风格也统一。
- **状态只有一份。** `AppViewModel` 持有 `AppData` 和界面状态，Compose 单向订阅，
  不会出现「某个角落的数据忘了刷新」。
- **没有用 Hilt。** 就一个 `AppViewModel`，手动 `viewModelFactory` 注入 Repository，
  加个 DI 框架纯属给自己添麻烦。
- **与网页版数据互通。** 导出的 JSON 结构和之前那版网页 PWA 完全一致，
  浏览器里的备份可以直接「我的 → 导入恢复」，反过来也能拿到网页版用。

**关于 `build.gradle.kts` 里那行 `buildscript classpath`：**
AGP 9.0 起内置 Kotlin 支持，**不再需要** `org.jetbrains.kotlin.android` 插件；
但它内置的 KGP 只有 2.2.10，而依赖是按 2.4.x 编译的，元数据版本对不上就会报一屏
`Unresolved reference`。所以必须显式把 KGP 抬到 2.4.21。**这行别删。**

---

## 七、功能

| 模块 | 内容 |
|---|---|
| 今日 | 热量环、可吃余量、蛋白/碳水/脂肪三张卡、四餐分栏、前后翻日期、复制昨天 |
| 记录 | 搜索（中文 / 拼音首字母）、分类筛选、份量步进、一顿汇总后一次入账 |
| 食物库 | 新建/编辑/删除、七个分类、按使用频次排序、批量粘贴导入 |
| 统计 | 近 7 天柱状图 + 目标虚线、日均摄入、达标天数、体重折线 |
| 我的 | 目标与代谢数据、导出/导入备份、清空数据 |
| 体验 | 深/浅色跟随系统、底部悬浮 Dock、键盘弹起时表单自动上移 |

### 用起来要知道的三件事

1. **食物库是空的，得自己建。** 这是当初刻意不做预置数据的结果——
   只录自己真吃的东西，选的时候才快。
2. **营养值按「1 份」记**（一碗 / 一个 / 一杯），不按 100g。
   热量必填，蛋白/碳水/脂肪可以先空着，以后补。
3. **数据只在这台手机上。** 卸载应用会一起删掉，记得「我的 → 导出备份」，
   文件存到你选的位置（网盘目录最稳）。

---

## 八、已知限制 / 以后想加

- 没做三餐定时提醒。要加的话是 `WorkManager` + 通知权限，半小时的活。
- 没做「我的套餐」（一组常吃食物一键记）。用一段时间觉得需要再加。
- 没做 release 签名配置。现在只有 debug 包，自己用够了；要正式分发再生成 keystore。

---

## 九、验证记录

跑在 `Medium_Phone_API_37`（1080×2400 @420dpi）上，逐项点过：

| 验证项 | 结果 |
|---|---|
| 新建食物 → 保存 | 库里 1 种，320 kcal / 蛋白 28，提示「已保存」 |
| 早餐加 2 份 → 记录 | 环上「已摄入 640 · 目标 1740」剩余 1100；蛋白 56/141、碳水 24/168、脂肪 32/56 |
| 首页卡片 | 早餐显示「Xianggu · 2份 · 640 kcal」，空餐次显示「还没有记录」 |
| 统计 | 柱子按 640/1740 比例，虚线为目标；日均 640 kcal · 1 天；达标 0/7 |
| 记体重 70.5 | 折线出单点，起止都标 70.5 kg；资料体重同步 → 目标 1730 变 1740 |
| 导出备份 | 系统保存框默认名 `fitlog-2026-11-14.json`，JSON 结构与网页版一致 |
| 清空 → 导入恢复 | 清空后 `foods:[]`；导入同一文件后完整还原，「上次备份」刷新 |
| 冷启动持久化 | `force-stop` 后重启，食物/记录/体重/资料全在 |
| 深浅色 | 两套色板都正常，状态栏图标随之反色 |

单元测试 8 条全过（`gradlew testDebugUnitTest`）：热量目标与网页版一致、女性目标下限、
拼音检索、批量导入（含逗号写法与默认分类/拼音别名）、份数累加、备份 JSON 往返、
网页版空 `gram` 的旧备份可导入。

---

## 十、踩过的坑

都是实际遇到过的，记下来省得以后重复排查：

**1. 模拟器慢是正常的，别怀疑代码。**
默认模拟器用 SwiftShader 软件渲染（没 GPU 加速），冷启动能到一分多钟，
连续快速按键甚至会触发「Input dispatching timed out」。
真机是秒开。想让模拟器快点：AVD 设置里 Graphics 改 `Hardware - GLES 2.0`。

**2. 改完代码一片红 `Unresolved reference`。**
先看 `build.gradle.kts` 里那行 KGP 抬升还在不在（见第六节），
再 **Build → Clean Project** 后重新同步。

**3. Gradle 同步卡住 / 下载超时。**
`gradle/wrapper/gradle-wrapper.properties` 的 `distributionUrl` 已经指向腾讯镜像：

```
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-9.9.1-bin.zip
```

依赖仓库是 `google()` + `mavenCentral()`，国内偶尔慢，重试通常就好。

**4. 首帧闪一下「0 摄入」的假数据？**
已经在 `FitLogApp.kt` 开头加了门禁：数据没读出来只画背景色。要是又出现，
说明 `AppState.loaded` 的判断被改动了。

**5. 用 adb 点界面点不准。**
别靠截图目测坐标，用无障碍层的真实边界：

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb exec-out cat /sdcard/ui.xml
```

Git Bash 下要加 `MSYS_NO_PATHCONV=1`，否则 `/sdcard/...` 会被改写成 Windows 路径。
另外 dump 失败时不会报错，会留下**上一次的旧文件**，容易读到过期界面。
