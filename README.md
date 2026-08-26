# GitStore — GitHub 开源 App 聚合商店

一个面向 Android 的 **GitHub 开源应用聚合商店**（自用向，**minSdk 33 / Android 13+**，compile/target SDK 35）。发现/搜索热门开源 Android 应用，浏览最新 Release、下载 APK 并一键安装。主体 **Kotlin + Jetpack Compose**，设计遵循 **Material 3 Expressive**（动态配色 / Material You），下载界面清晰、使用明了。

## 技术栈
- **Kotlin 2.1** + **Jetpack Compose**（Material 3 Expressive / `MaterialExpressiveTheme`），compile SDK 35
- MVVM：`ViewModel` + `StateFlow`；协程 / Flow
- 网络：`Retrofit2` + `OkHttp` + `kotlinx-serialization`（GitHub REST API v3）
- 本地缓存：**Room 2.7**（KSP2 符号处理；浏览数据 1 小时 TTL，离线可浏览、显著降低 API 限流）
- F-Droid 数据源：`data/fdroid/FdroidIndexRepository` 拉取 `index-v2.json`，解析 `sourceCode` 为 GitHub `owner/repo`，动态扩充更新检测的包名映射
- 版本比较：**semver4j 4.2.1**（`util/VersionComparator`），正确处理 `1.10 > 1.9`、`v` 前缀剥离、预发布标签
- 图片：`Coil`；配置持久化：`DataStore Preferences`
- 下载：`DownloadManager`（进度 Flow + 通知）+ 可选多线程 `Range` 分块并发下载（AtomicLong 进度累加，无竞态）+ `FileProvider` 调起包安装器
- 新特性：`enableEdgeToEdge`、原生 `SplashScreen` API、Predictive Back、`PackageInfoFlags` 直用（minSdk 33 无版本守卫）

## 核心功能
- **主页（推荐 + 搜索 + 下拉刷新 + 动效）**：推荐分两块——「我 Star 的」（登录后，仅 Android/Kotlin 项目）优先 + 「热门推荐」（高 star 热门开源 App）；`SearchBar` + 300ms 防抖调用 GitHub Search API；分类 chip 行；`PullToRefreshBox` 下拉刷新；列表项入场 **stagger 淡入上移**动画。
- **下载与更新（合并页）**：下载任务队列（进度条 + 安装按钮）+ 自动检测已装开源 App 版本并提供一键更新；更新检测合并**内置 30+ 条映射表 + F-Droid 动态索引**，覆盖率大幅提升；版本比较经 semver4j 规范化。
- **设置**：镜像反代（默认 `ghfast.top`，可自定义）+ 多线程下载开关与线程数(2–8) + 通过 API 获取 release 链接开关 + 深色纯黑背景开关 + 动态配色 + GitHub PAT（仅本地）+ 关于（含缓存/F-Droid/semver 说明）；「我的（GitHub 账户）」作为设置子项内嵌。
- **首次使用向导**：4 页项目介绍 + 通知/安装未知来源权限请求，完成后不再出现（DataStore 标志位）。
- **加载占位**：全局统一"转圈圈 + 少女祈祷中…"。
- **底部导航动效**：3 底栏切换采用 `AnimatedContent` 共享轴/淡入淡出（`slideIn/Out + fade`），方向随目标顺序。

## 快速开始
1. 用 **Android Studio（2024.3+ / AGP 8.7+，内置 JDK 21）** 打开本目录（Gradle 会按需下载依赖，含 Room KSP 处理）。
2. 通过 **File → New → Image Asset** 生成自适应启动图标（`app/src/main/res/mipmap-*`），Manifest 已引用 `ic_launcher` / `ic_launcher_round`。
3. 运行 `:app` 到 API 33+ 设备/模拟器即可。

## 配置说明
- **镜像**：设置页下拉选择 `ghfast.top` / `ghproxy.com` / `ghproxy.net` 等，或「自定义…」填写任意主机（含 `https://`）；关闭总闸即直连。
- **多线程下载**：开启后按选定线程数并发分块下载再合并（AtomicLong 累加进度，无竞态）；关闭则用系统单连接 `DownloadManager`。
- **API release 回退**：Release 页面/资产 CDN 访问失败时开启，改由 GitHub REST API 获取资产直链（可再经镜像）后下载。
- **本地缓存**：Room（`gitstore-cache.db`），浏览/搜索/Starred/最新 Release 缓存 1 小时；可离线查看已加载内容，大幅缓解未认证 60 次/小时、PAT 5000 次/小时限流。
- **更新检测覆盖**：内置知名开源 App 映射表 + F-Droid `index-v2.json` 动态解析 `sourceCode` 为 GitHub 仓库，自动覆盖更多已装开源应用（F-Droid 网络不可用时静默降级到内置表）。
- **GitHub PAT（可选）**：设置页/账户子项填写 Personal Access Token，仅本地保存，用于拉取"我 Star 的 Android 应用"（Fine-grained PAT 授予 `repo:read` 即可）。
- 所有设置通过 `DataStore` 持久化（`com.example.githubappstore.data.settings.AppSettings`）。

## 目录结构
```
app/src/main/java/com/example/githubappstore/
├── GitHubAppStoreApp.kt        # Application + 依赖容器（含 Room DB / CachedGitHubRepository）
├── data/
│   ├── remote/GitHubApiService.kt   # GitHub REST API 接口
│   ├── model/GitHubModels.kt        # kotlinx-serialization 模型
│   ├── settings/AppSettings.kt      # DataStore 设置
│   ├── cache/                       # Room 缓存（CachedRepo/Release/Starred + DAO + DB）
│   └── fdroid/FdroidIndexRepository.kt  # F-Droid index-v2 → 包名↔GitHub 映射
├── domain/AppItem.kt            # AppItem / AppCategory / DownloadStatus
├── ui/
│   ├── MainActivity.kt GitStoreApp.kt   # 单 Activity + 3 底栏(AnimatedContent 切换)
│   ├── theme/                   # M3 Expressive 主题/typography/配色
│   ├── components/              # AppCard / InstallBottomSheet / LoadingPraying / StaggeredLazyColumn
│   ├── home/                    # 主页（推荐+搜索+分类+下拉刷新+入场动效）
│   ├── dlupdates/               # 下载与更新合并页
│   ├── settings/                # 设置页 + 内嵌 AccountSection
│   ├── onboarding/              # 首次使用向导
│   ├── downloads/               # DownloadViewModel
│   ├── updates/                 # UpdatesViewModel（已装应用更新检测，合并 F-Droid 映射）
│   └── account/                 # AccountViewModel（PAT 登录）
└── util/                        # ProxyUtils / ApkDownloader / MultiThreadDownloader / UpdateChecker / VersionComparator
```

## 数据来源与合规
- 仓库元数据与 Release/APK 资产均来自 **GitHub 公开 REST API**；F-Droid 索引来自 **f-droid.org 公开仓库**；本应用**不托管任何二进制文件**。
- 下载的 APK 来自各开源项目作者在其 GitHub Release 中发布的原始资产（经用户可选镜像前缀代理）。
- 镜像站为社区公益代理，可用性/速度可能变化，请勿将含凭据的请求经第三方代理。

## 参考
- Material Design 3 / M3 Expressive：<https://m3.material.io>
- Jetpack Compose Material 3：<https://developer.android.com/develop/ui/compose/designsystems/material3>
- GitHub REST API：<https://docs.github.com/en/rest>
- semver4j：<https://github.com/semver4j/semver4j>
- F-Droid 仓库索引：<https://f-droid.org/repo/index-v2.json>
