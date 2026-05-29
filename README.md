# UltimateShop 自用 Fork

> 这是 `UltimateShop` 的自用分支，主要用于我自己的构建、验证和维护。

## 这个 Fork 做了什么

- 把数学验证程序 `MathFunctionTest` 移到 `core/src/test/java`，只用于构建时验证，不会打进最终 JAR。
- 增加了 `:core:testMath` 任务，用来在构建过程中跑 EvalEx 和自定义 `SIGMA` 的表达式验证。
- 补充了 README 中的数学表达式说明，包含内置函数、布尔写法、占位符和自定义函数。

## 项目简介

**UltimateShop** 是一个适用于 Spigot / Paper 的商店插件，支持商品、价格、限购、冷却、菜单、条件和占位符等功能。

---

## 自定义物品识别

UltimateShop 使用基于 **NBT** 的物品识别方式，而不是简单地比较整件物品。

它支持和常见物品插件兼容，例如：

- MMOItems
- eco
- ItemsAdder
- Nexo
- Oraxen
- MythicMobs
- CraftEngine

即使物品被附魔、改名，或者被其它插件修改了 lore，插件通常也能继续正确识别。

---

## 菜单系统

UltimateShop 提供了完全可配置的菜单系统，支持：

- 自定义每个商店的布局
- 自动检查限购，减少误点
- 为按钮或商品设置自定义点击动作
- 添加带动作和条件的自定义按钮
- 商店内外都可以独立配置菜单
- 同一份配置可用于 Java 箱子界面和 Bedrock 表单界面

---

## 限购与冷却

插件支持配置：

- 全局买入限购
- 个人买入限购
- 全局卖出限购
- 个人卖出限购
- 全局买入冷却
- 个人买入冷却
- 全局卖出冷却
- 个人卖出冷却

重置方式包括：

- 每日 / 每周 / 每月重置
- 基于计时器的重置
- Cron 表达式重置
- 永不重置
- 通过其它插件占位符自定义重置

限购和冷却都支持数学表达式和 PlaceholderAPI 变量。

---

## 商品与价格

UltimateShop 采用商品和价格的多对多关系：

- 一个商品可以对应多个价格方案
- 一个价格规则也可以作用到多个商品

商品和价格都可以使用物品或货币来定义。支持：

- 多种经济插件
- 单个价格设置多个应用时间、规则和条件
- 数学运算和 PlaceholderAPI 变量
- 季节性或按时间变化的价格
- 折扣和随机商店

---

## 物品与经济格式

插件支持在以下位置使用物品和经济格式：

- 价格和商品
- 菜单和展示物品

经济格式支持：

- Vanilla XP 和 XP 等级
- 多种第三方经济插件

---

## 占位符

插件很多地方都能直接使用占位符，包括：

- 数学占位符
- Cron 占位符
- 随机占位符
- 条件占位符
- 语言占位符
- 比较占位符

---

## 数学表达式

UltimateShop 可以在商品数量、价格、限购、占位符等数值项中直接计算数学表达式。

`config.yml` 中相关配置如下：

```yaml
math:
  enabled: true
  scale: 2
  static-scale: false
```

当 `math.enabled` 为 `false` 时，数值会按普通数字读取；为 `true` 时，会通过 EvalEx 计算表达式，并按需要使用 `math.scale` 进行四舍五入。

### 基础写法

```text
1 + 1
(1 + 2) * 3
10 / 3
10 % 3
2 ^ 10
-5 + 8
2(3 + 4)
```

- 支持 `+`、`-`、`*`、`/`、`%`、`^`
- 支持括号控制优先级
- 支持一元 `+` 和 `-`
- 支持隐式乘法，比如 `2(3 + 4)` 等价于 `2 * (3 + 4)`
- 输出里可能出现科学计数法，例如 `1E+2` 表示 `100`

### 常量

```text
PI
E
TRUE
FALSE
NULL
```

指数写法请直接使用 `E ^ x`。

```text
E ^ 1
E ^ (-0.1 * 10)
1 - E ^ (-0.1 * 5)
```

当前版本里不要写 `EXP(x)`，它不是内置函数。

### 比较与布尔

```text
1 > 0
1 >= 1
1 < 2
1 <= 2
1 == 1
1 != 2
1 < 2 && 2 < 3
1 > 2 || 2 < 3
NOT(1 > 2)
IF(1 < 2, 100, 0)
IF(1 < 2 && 2 < 3, 400, 0)
```

布尔与或请用 `&&` 和 `||`，不要写成 `AND` / `OR` 中缀形式。

### 数字函数

```text
ABS(-5)
SQRT(16)
ROUND(3.14159, 2)
FLOOR(3.9)
CEILING(3.1)
LOG(E)
LOG10(100)
FACT(5)
MIN(3, 1, 4, 2)
MAX(3, 1, 4, 2)
SUM(1, 2, 3)
AVERAGE(1, 2, 3)
RANDOM()
COALESCE(NULL, 5)
SWITCH(2, 1, 100, 2, 200, 0)
```

`COALESCE` 会返回第一个非空值。`SWITCH(value, case1, result1, case2, result2, default)` 会返回匹配项或默认值。

### 三角函数

默认三角函数使用角度制：

```text
SIN(90)
COS(180)
TAN(45)
COT(45)
SEC(60)
CSC(30)
ASIN(1)
ACOS(-1)
ATAN(1)
ACOT(1)
ATAN2(1, 1)
```

弧度版本使用 `R` 后缀：

```text
SINR(1.5707963267948966)
COSR(3.141592653589793)
TANR(0.7853981633974483)
COTR(0.7853981633974483)
SECR(1.0471975511965976)
CSCR(0.5235987755982988)
ASINR(1)
ACOSR(-1)
ATANR(1)
ACOTR(1)
ATAN2R(1, 1)
RAD(180)
DEG(3.141592653589793)
```

双曲函数也可用：

```text
SINH(1)
COSH(1)
TANH(1)
ASINH(1)
ACOSH(2)
ATANH(0.5)
ACOTH(2)
COTH(1)
SECH(1)
CSCH(1)
```

### 占位符参与计算

PlaceholderAPI 的值会先被替换，再进行数学计算：

```yaml
amount: "%vault_eco_balance% * 0.05"
```

商品和价格里的动态内容也可以先带上内置占位符再计算：

```yaml
amount: "100 + {buy-times-player} * 5"
amount: "100 - {sell-times-server} * 2"
amount: "MAX(10, 100 + {buy-total-server} - {sell-total-server})"
```

常见使用占位符包括：

```text
{buy-times-player}
{sell-times-player}
{buy-times-server}
{sell-times-server}
{buy-total-player}
{sell-total-player}
{buy-total-server}
{sell-total-server}
```

文本展示里也可以直接用数学占位符：

```text
{math_1+1}
{math_ROUND(10/3, 2)}
{math_SIGMA(1, 10, "i")}
```

### 自定义函数 `SIGMA`

UltimateShop 自带一个自定义数学函数：

```text
SIGMA(start, end, "body")
```

`SIGMA` 会从 `start` 一直循环到 `end`，循环变量是 `i`，可在 `body` 里直接使用。

```text
SIGMA(1, 10, "i")
SIGMA(1, 10, "i * i")
SIGMA(1, 5, "i ^ 3")
SIGMA(1, 4, "FACT(i)")
SIGMA(1, 10, "IF(i % 2 == 0, i, 0)")
SIGMA(1, 5, "E ^ (-0.1 * i)")
```

示例结果：

```text
SIGMA(1, 10, "i") = 55
SIGMA(1, 10, "i * i") = 385
SIGMA(1, 5, "i ^ 3") = 225
SIGMA(5, 1, "i") = 0
```

如果在 YAML 里写 `SIGMA`，而 body 里又用了双引号，建议外层整段用单引号包起来：

```yaml
amount: 'SIGMA(1, 10, "i * i")'
```

`SIGMA` 单次最多允许 100000 次迭代，用于保护服务器性能。

---

## 动作与条件

UltimateShop 支持在以下场景触发动作和条件：

- 买入或卖出
- 失败动作
- 点击按钮
- 打开 / 关闭菜单

可用动作包括：

- 执行命令
- 生成实体
- 播放音效
- 传送玩家

可用条件包括：

- PlaceholderAPI
- 世界
- 生物群系
- 权限检查

---

## 高级功能

以下内容中有些是 PREMIUM 才能用的：

- MiniMessage 和 Legacy 颜色解析
- 语言消息支持 Action Bar / Title / Boss Bar / Sound
- 按玩家客户端语言显示对应文本
- 买更多菜单
- 快速卖出菜单
- 插件附魔支持
- 卖棒
- 自动卖箱
- 基岩菜单支持

---

## 购买与下载

如果你喜欢这个项目，也可以去原作者页面支持一下：

- [购买 PREMIUM](https://www.spigotmc.org/resources/ultimateshop-premium-menu-dynamic-price-limits-apply-settings-sell-all-and-more-1-17-1-20.113069/)
- [下载免费版](https://www.spigotmc.org/resources/ultimateshop-menus-limits-apply-settings-10-directly-hook-and-more-1-17-1-20.110601/)
