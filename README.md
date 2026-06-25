# 陶离猩球 - Android App

实时监测付费动态，自动记录消费并进行分类统计。

## 功能

- 🔔 **自动监测付费**：通过 NotificationListenerService 监听支付宝、微信支付、银行短信等付款通知
- 💰 **手动记录**：点击首页浮动按钮手动记录消费
- 📊 **统计总览**：日/月/年维度查看各类型消费汇总，饼图展示
- 📝 **消费记录**：按月查看历史记录，支持删除
- ⚙️ **类型管理**：自定义消费类型（饭菜钱、路程篇等）

## 构建步骤

### 前置要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- JDK 17

### 编译
1. 用 Android Studio 打开 `TaoliXingqiu` 目录
2. 等待 Gradle 同步完成
3. 点击 Run > Run 'app' 安装到手机

### 首次使用
1. 打开 APP 后会提示开启"通知监听"权限
2. 进入系统设置 → 通知使用权 → 找到"陶离猩球"并开启
3. 之后支付宝/微信支付等付款通知会自动被检测

## 项目结构

```
app/src/main/java/com/taoli/xingqiu/
├── MainActivity.kt          # 主界面，底部导航，付款弹窗
├── model/
│   └── Record.kt            # 消费记录数据模型
├── data/
│   └── DatabaseHelper.kt    # SQLite 数据库操作
├── service/
│   ├── PaymentNotificationService.kt  # 通知监听服务
│   └── SmsReceiver.kt                 # 短信监听广播
├── ui/
│   ├── OverviewFragment.kt  # 总览页面（统计 + 饼图）
│   ├── RecordsFragment.kt   # 记录页面（历史列表）
│   ├── CategoryFragment.kt  # 类型管理页面
│   └── PaymentDialog.kt     # 付款弹窗辅助类
└── view/
    └── PieChartView.kt      # 自定义饼图 View
```

## 技术栈

- Kotlin
- Material Design 3
- SQLite (SQLiteOpenHelper)
- NotificationListenerService
- ViewBinding
- RecyclerView
- FlexboxLayout
- Custom View (PieChartView)
