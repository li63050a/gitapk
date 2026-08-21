# Git 工具 (Git Tool) - Android APK

一个轻量级、现代化的 Android Git 命令行工具应用，让用户可以在手机上高效管理 Git 仓库。支持仓库克隆、提交历史查看、分支管理、远程操作、暂存管理、SSH 密钥管理、搜索、批量操作等核心功能，同时提供丰富的主题自定义选项。

---

## 目录

- [项目概述](#项目概述)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [构建说明](#构建说明)
- [UI 设计](#ui-设计)
- [数据持久化](#数据持久化)
- [Git 核心实现](#git-核心实现)
- [多语言支持](#多语言支持)
- [主题系统](#主题系统)
- [权限说明](#权限说明)
- [开发计划](#开发计划)

---

## 项目概述

### 产品名称
Git 工具 (Git Tool)

### 包名
`com.git.app`

### 版本
`0.0.0.2` (versionCode: 2)

### 目标平台
Android 6.0 (API 23) 及以上

### 应用定位
面向开发者和开发者爱好者的移动端 Git 管理工具，提供轻量、快速、易用的 Git 操作体验。无需依赖系统 Git，使用纯 Java 实现的 JGit 库作为底层引擎。

---

## 功能特性

### 核心功能

#### 1. 仓库管理
- **初始化仓库**：在本地目录创建新的 Git 仓库
- **克隆仓库**：从远程 URL 克隆仓库到本地指定路径
- **仓库扫描**：自动扫描指定目录下所有包含 `.git` 文件夹的 Git 仓库
- **仓库列表**：展示所有发现的仓库，按最后修改时间倒序排列
- **实时刷新**：首页下拉刷新按钮实时更新仓库列表

#### 2. 提交历史
- **提交列表**：获取并展示仓库的提交历史（默认最近 50 条）
- **提交详情**：点击提交展开查看完整 diff 和文件变更列表
- **路径筛选**：可指定仓库路径加载对应的提交历史
- **搜索提交**：支持按关键词搜索提交信息、作者、SHA

#### 3. 分支管理
- **分支列表**：获取并展示所有分支，当前分支高亮显示
- **切换分支**：一键切换至目标分支
- **创建分支**：输入分支名称创建新分支
- **删除分支**：删除非当前分支

#### 4. 远程操作
- **拉取 (Pull)**：从远程仓库拉取最新更改并合并
- **推送 (Push)**：将本地提交推送到远程仓库
- **获取 (Fetch)**：仅获取远程仓库信息，不合并
- **远程列表**：展示所有配置的远程仓库及其 URL

#### 5. 暂存管理
- **暂存文件**：将指定文件添加到暂存区
- **取消暂存**：将已暂存文件从暂存区移除
- **暂存全部**：一键暂存所有变更
- **变更列表**：展示已暂存、已修改、未跟踪的文件

#### 6. SSH 密钥管理
- **查看密钥**：列出本地 ~/.ssh 目录下的所有密钥
- **添加密钥**：手动添加新的 SSH 密钥
- **删除密钥**：删除指定 SSH 密钥
- **密钥信息**：显示密钥类型、创建时间、指纹

#### 7. 搜索功能
- **搜索提交**：按关键词搜索提交信息、作者、SHA
- **搜索文件**：按关键词搜索仓库中的文件

#### 8. 批量操作
- **批量拉取**：一次性从多个仓库拉取最新代码
- **批量推送**：一次性推送多个仓库的本地提交
- **批量获取**：一次性获取多个仓库的远程信息

#### 9. 设置与定制
- **主题模式**：跟随系统 / 浅色模式 / 深色模式
- **背景颜色**：7 种预设背景色（默认、浅灰、米色、浅蓝、浅绿、浅粉、深黑）
- **主题色**：8 种预设主题色（靛蓝、蓝色、青绿、红色、琥珀、紫色、森林绿、橙色）
- **语言切换**：简体中文（默认）/ English
- **自定义背景图**：支持从相册选择图片作为应用背景
- **关于页面**：展示应用版本信息

---

## 技术栈

### 开发语言
- **Kotlin 2.0.21**：主开发语言，利用协程、Flow 等现代特性

### UI 框架
- **Jetpack Compose**：声明式 UI 框架，Material Design 3 风格
- **Navigation Compose**：组件化导航管理

### 架构模式
- **MVVM**：Model-View-ViewModel 架构
  - **Model**：数据模型 (`git/GitModels.kt`)
  - **View**：UI 界面 (`ui/screen/*`)
  - **ViewModel**：业务逻辑 (`vm/*`)

### Git 引擎
- **JGit 6.9.0**：纯 Java 实现的 Git 库，无需系统 Git 依赖
  - 支持所有标准 Git 操作
  - 跨平台兼容性好
  - 适合移动端部署

### 数据持久化
- **DataStore Preferences**：Google 官方推荐的轻量级配置存储方案
  - 替代 SharedPreferences
  - 协程友好
  - 类型安全

### 异步处理
- **Kotlin Coroutines**：异步编程框架
  - `Dispatchers.IO` 用于网络/磁盘操作
  - `viewModelScope` 用于 ViewModel 生命周期管理
  - `LaunchedEffect` 用于 Composable 中的副作用处理

---

## 项目结构

```
git-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/git/
│   │   │   ├── GitApp.kt                    # Application 入口类
│   │   │   ├── MainActivity.kt              # 主 Activity，应用启动入口
│   │   │   ├── data/
│   │   │   │   ├── SettingsData.kt          # 配置数据模型和枚举
│   │   │   │   └── SettingsRepository.kt    # 设置数据存储层
│   │   │   ├── git/
│   │   │   │   ├── GitExecutor.kt           # Git 操作核心实现
│   │   │   │   └── GitModels.kt             # Git 数据模型
│   │   │   ├── ui/
│   │   │   │   ├── theme/
│   │   │   │   │   └── Theme.kt             # Material Theme 配置
│   │   │   │   ├── navigation/
│   │   │   │   │   └── GitNavGraph.kt       # 导航路由定义
│   │   │   │   ├── component/
│   │   │   │   │   └── GitBottomNavigation.kt # 底部导航栏组件
│   │   │   │   └── screen/
│   │   │   │       ├── home/
│   │   │   │       │   └── HomeScreen.kt    # 仓库列表页（含批量操作）
│   │   │   │       ├── commit/
│   │   │   │       │   └── CommitScreen.kt  # 提交历史页（含详情/搜索）
│   │   │   │       ├── branch/
│   │   │   │       │   └── BranchScreen.kt  # 分支管理页
│   │   │   │       ├── remote/
│   │   │   │       │   └── RemoteScreen.kt  # 远程操作页
│   │   │   │       ├── stage/
│   │   │   │       │   └── StageScreen.kt   # 暂存管理页
│   │   │   │       ├── ssh/
│   │   │   │       │   └── SSHScreen.kt     # SSH 密钥管理页
│   │   │   │       └── settings/
│   │   │   │           └── SettingsScreen.kt # 设置页面
│   │   │   └── vm/
│   │   │       ├── HomeViewModel.kt         # 首页 ViewModel
│   │   │       ├── CommitViewModel.kt       # 提交历史 ViewModel
│   │   │       ├── BranchViewModel.kt       # 分支管理 ViewModel
│   │   │       ├── RemoteViewModel.kt       # 远程操作 ViewModel
│   │   │       ├── StageViewModel.kt        # 暂存管理 ViewModel
│   │   │       └── SSHViewModel.kt          # SSH 密钥管理 ViewModel
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   │   ├── app_icon.jpg             # 应用图标（来自 2.jpg）
│   │   │   │   └── ic_launcher.xml          # 启动图标矢量图
│   │   │   ├── values/
│   │   │   │   ├── strings.xml              # 默认语言（简体中文）
│   │   │   │   ├── colors.xml               # 颜色定义
│   │   │   │   └── themes.xml               # 应用主题
│   │   │   ├── values-en/
│   │   │   │   └── strings.xml              # 英文资源
│   │   │   └── values-zh/
│   │   │       └── strings.xml              # 中文资源
│   │   └── AndroidManifest.xml              # 应用清单文件
│   ├── build.gradle.kts                     # App 模块构建配置
│   └── proguard-rules.pro                   # 混淆规则
├── build.gradle.kts                         # 项目级构建配置
├── settings.gradle.kts                      # Gradle 设置
├── gradle.properties                        # Gradle 属性
├── local.properties                         # 本地 SDK 路径
├── gradlew                                  # Gradle Wrapper 脚本
├── gradle/wrapper/
│   ├── gradle-wrapper.jar                   # Gradle Wrapper JAR
│   └── gradle-wrapper.properties            # Wrapper 配置
└── README.md                                # 项目说明文档
```

---

## 构建说明

### 环境要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17 (OpenJDK) | 编译和运行必需 |
| Android SDK | API 23+ | 最低支持版本 |
| Android SDK Build Tools | 34.0.0+ | 构建工具链 |
| Gradle | 8.9 | 构建系统 |
| Kotlin | 2.0.21 | 编程语言 |

### 构建命令

```bash
# 进入项目目录
cd /data/home/admin1/work/apk/app/git

# 设置环境变量
source /data/home/admin1/work/apk/env.sh

# 清理并构建 Debug APK
./gradlew clean assembleDebug

# 或直接用 gradle 命令（如果已配置 PATH）
gradle clean assembleDebug
```

### 输出位置

构建成功后，APK 文件位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## UI 设计

### 设计风格
- **Material Design 3**：采用最新的 Material You 设计语言
- **响应式布局**：适配不同屏幕尺寸
- **手势友好**：触摸目标符合人体工程学设计
- **7 个功能页面**：首页、提交、分支、远程、暂存、SSH、设置

### 主要页面

#### 首页 (HomeScreen)
- 展示扫描到的所有 Git 仓库列表
- 每个仓库卡片显示：图标、仓库名称、完整路径
- 右上角 FAB 按钮用于添加新仓库
- 支持按最后修改时间排序
- 支持批量操作（Pull/Push/Fetch 多个仓库）
- 支持实时刷新

#### 提交历史 (CommitScreen)
- 顶部路径输入框和加载按钮
- 提交列表，每条展示：
  - 短 SHA（7位）
  - 提交信息
  - 作者名称
  - 提交时间
- 点击提交展开查看详情（diff + 文件变更列表）
- 支持搜索提交（按关键词、作者、SHA）

#### 分支管理 (BranchScreen)
- 分支列表，当前分支高亮显示
- 每个分支显示：
  - 分支名称
  - "当前"标记
  - 切换/删除操作按钮
- 右上角按钮创建新分支

#### 远程操作 (RemoteScreen)
- 远程仓库列表展示
- 三个操作按钮：Pull、Push、Fetch
- 操作状态指示器（Loading 状态）
- 最后操作结果展示

#### 暂存管理 (StageScreen)
- 文件变更列表，分三组展示：
  - **Staged（已暂存）**：黄色高亮，支持取消暂存
  - **Modified（已修改）**：支持暂存操作
  - **Untracked（未跟踪）**：支持暂存操作
- 支持一键暂存全部
- 支持直接提交（填写提交信息）

#### SSH 密钥管理 (SSHScreen)
- 列出本地 ~/.ssh 目录下的所有密钥
- 每个密钥显示：名称、类型、创建时间、部分内容
- 支持添加新 SSH 密钥
- 支持删除指定密钥

#### 设置 (SettingsScreen)
- 主题模式选择（跟随系统/浅色/深色）
- 背景颜色选择（7种预设）
- 主题色选择（8种预设）
- 语言切换（简体中文/English）
- 自定义背景图片选择
- SSH 密钥管理入口
- 关于信息

### 配色方案

#### 背景颜色预设
| 名称 | 浅色 | 深色 |
|------|------|------|
| 默认 | #FAFAFA | #121212 |
| 浅灰 | #F3F3F2 | #1C1C1E |
| 米色 | #F8F3E9 | #1F1A12 |
| 浅蓝 | #EFF4FA | #101A2A |
| 浅绿 | #EDF5F0 | #0E1F18 |
| 浅粉 | #FAF0F2 | #231419 |
| 深黑 | #1E1E1E | #0A0A0A |

#### 主题色预设
| 名称 | 浅色 | 深色 |
|------|------|------|
| 靛蓝 | #4A5BD6 | #AEB8FF |
| 蓝色 | #1E6FD9 | #9FC7FF |
| 青绿 | #0F8771 | #8AD9C6 |
| 红色 | #D33A3A | #FFA6A6 |
| 琥珀 | #B87900 | #FFD78F |
| 紫色 | #8B3FD8 | #D6AFFF |
| 森林绿 | #2D7D46 | #7DD89A |
| 橙色 | #D97706 | #FDBA74 |

---

## 数据持久化

### 存储方案
使用 Android Jetpack DataStore Preferences 进行设置持久化。

### 存储项
| 设置项 | 键名 | 默认值 |
|--------|------|--------|
| 主题模式 | `theme_mode` | `system` |
| 背景颜色 | `bg_preset` | `default` |
| 主题色 | `accent_preset` | `indigo` |
| 语言 | `language` | `zh` |
| 自定义背景图 | `custom_bg` | null |

---

## Git 核心实现

### GitExecutor 类
位于 `git/GitExecutor.kt`，封装所有 Git 操作。

### 支持的操作

| 方法 | 功能 | 参数 | 返回值 |
|------|------|------|--------|
| `cloneRepository` | 克隆远程仓库 | URL, 本地路径 | GitResult<String> |
| `initRepository` | 初始化新仓库 | 本地路径 | GitResult<String> |
| `getCommits` | 获取提交历史 | 路径, 数量 | GitResult<List<Commit>> |
| `getCommitDetail` | 获取提交详情 | 路径, commitId | GitResult<CommitDetail> |
| `searchCommitsByMessage` | 搜索提交 | 路径, 关键词 | GitResult<List<Commit>> |
| `searchFiles` | 搜索文件 | 路径, 关键词 | GitResult<List<String>> |
| `getBranches` | 获取分支列表 | 路径 | GitResult<List<Branch>> |
| `getCurrentBranch` | 获取当前分支 | 路径 | GitResult<String> |
| `createBranch` | 创建新分支 | 路径, 分支名 | GitResult<Boolean> |
| `deleteBranch` | 删除分支 | 路径, 分支名 | GitResult<Boolean> |
| `checkoutBranch` | 切换分支 | 路径, 分支名 | GitResult<Boolean> |
| `addFile` | 添加文件到暂存区 | 路径, 文件路径 | GitResult<Boolean> |
| `addAll` | 添加所有变更 | 路径 | GitResult<Boolean> |
| `commit` | 提交变更 | 路径, 提交信息 | GitResult<String> |
| `getStatus` | 获取仓库状态 | 路径 | GitResult<RepoStatus> |
| `getStagedFiles` | 获取暂存文件列表 | 路径 | GitResult<List<String>> |
| `pull` | 拉取远程变更 | 路径 | GitResult<String> |
| `push` | 推送本地变更 | 路径 | GitResult<String> |
| `fetch` | 获取远程信息 | 路径 | GitResult<String> |
| `getRemotes` | 获取远程配置 | 路径 | GitResult<List<Remote>> |
| `getFileDiff` | 获取文件差异 | 路径, 文件 | GitResult<String> |
| `batchPull` | 批量拉取 | 仓库路径列表 | GitResult<List<BatchOperationResult>> |
| `batchPush` | 批量推送 | 仓库路径列表 | GitResult<List<BatchOperationResult>> |
| `batchFetch` | 批量获取 | 仓库路径列表 | GitResult<List<BatchOperationResult>> |
| `listSSHKeys` | 列出 SSH 密钥 | - | GitResult<List<SSHKey>> |
| `addSSHKey` | 添加 SSH 密钥 | 名称, 内容 | GitResult<Boolean> |
| `deleteSSHKey` | 删除 SSH 密钥 | 名称 | GitResult<Boolean> |

### 错误处理
所有方法返回统一的 `GitResult<T>` 包装类：
```kotlin
data class GitResult<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
```

---

## 多语言支持

### 支持的语言
| 语言 | 代码 | 资源目录 |
|------|------|---------|
| 简体中文 | zh | `values-zh/` |
| English | en | `values-en/` |

**默认语言：简体中文**

### 语言切换实现
```kotlin
fun applyLanguage(context: Context, language: AppLanguage) {
    val tag = language.tag
    val locale = if (tag != null) Locale(tag) else Locale.getDefault()
    val config = context.resources.configuration
    config.setLocale(locale)
    context.createConfigurationContext(config)
}
```

---

## 主题系统

### 主题模式
| 模式 | 说明 |
|------|------|
| SYSTEM | 跟随系统深浅色设置 |
| LIGHT | 强制浅色模式 |
| DARK | 强制深色模式 |

### 主题色生成
颜色方案根据背景色和主题色动态生成：
- Primary：主题色
- Background：背景色（深浅模式不同）
- Surface：基于背景混合的浅色变体
- Error：红色系

### 自定义背景图
用户可从相册选择图片作为应用背景，图片以 30% 透明度叠加在主题色之上。

---

## 权限说明

### 必要权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
用于 Git 远程仓库操作（克隆、推送、拉取等）。

### 存储权限
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"
    android:requestLegacyExternalStorage="true" />
```
用于扫描本地 Git 仓库和选择背景图片。

---

## 开发计划

### 已完成
- [x] 项目骨架搭建
- [x] Git 核心操作层
- [x] 首页仓库列表
- [x] 提交历史页面
- [x] 分支管理页面
- [x] 远程操作页面
- [x] 暂存管理页面
- [x] SSH 密钥管理页面
- [x] 设置页面
- [x] 多语言支持（中英文，默认中文）
- [x] 主题系统（背景色、主题色、自定义背景图）
- [x] 提交详情展开查看（含 diff）
- [x] 搜索功能（提交/文件）
- [x] 批量操作（多仓库 Pull/Push/Fetch）
- [x] 实时刷新
- [x] 应用图标
- [x] Debug APK 构建
- [x] Android 6-16 支持（minSdk 23）

---

## 构建信息

| 项目 | 值 |
|------|-----|
| 包名 | com.example.git |
| 最小 SDK | 23 (Android 6.0) |
| 目标 SDK | 34 (Android 14) |
| 编译 SDK | 34 (Android 14) |
| ABI 过滤 | arm64-v8a, armeabi-v7a, x86, x86_64 |
| APK 大小 | ~18 MB |
| 版本 | 0.0.0.2 |
| GitHub | https://github.com/li63050a/gitapk |

---

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。
