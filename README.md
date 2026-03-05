# Kiro-GUI

> 将 [Kiro](https://kiro.dev) AI 助手的核心能力集成到 JetBrains IDE 中。

[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Plugin-orange?logo=jetbrains)](https://plugins.jetbrains.com/)
[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-2023.3+-blue)](https://plugins.jetbrains.com/docs/intellij/welcome.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-green)](LICENSE)

Kiro-GUI 是一个开源的 JetBrains IDE 插件，让你在 IntelliJ IDEA、WebStorm、PyCharm、GoLand 等 IDE 中直接使用 Kiro AI 辅助开发功能，无需切换编辑器。

## 功能特性

### 💬 Chat 交互面板
- 侧边栏 AI 对话面板，支持拖拽、浮动和最小化
- Markdown 渲染、代码高亮、文件链接跳转
- 代码块一键复制或应用到编辑器
- 多轮对话，保持上下文连续性

### 📋 Spec 驱动开发
- 可视化管理 Spec 工作流（需求 → 设计 → 任务）
- Markdown 实时预览
- 任务列表复选框状态同步

### ⚡ Hook 自动化
- 基于文件保存、创建等事件自动触发操作
- Hook 管理界面，支持启用/禁用
- 执行结果通知

### 🎯 Steering 规则配置
- 可视化编辑 AI 行为规则
- 规则文件热重载，无需重启

### 🔗 编辑器上下文集成
- 右键菜单：解释代码、重构建议、生成测试
- 选中代码快捷发送到 Chat 面板
- 可自定义键盘快捷键

### 🔌 LSP 通信
- 标准 LSP 协议与 Kiro 后端通信
- 流式响应，逐步显示 AI 生成内容
- 自动重连机制（5 秒内重试，最多 3 次）

## 技术栈

| 层面 | 选型 |
|------|------|
| 语言 | Kotlin |
| 构建工具 | Gradle + Kotlin DSL |
| Gradle 插件 | IntelliJ Platform Gradle Plugin 2.x |
| UI 框架 | IntelliJ Platform UI (Swing) + JCEF |
| LSP 通信 | lsp4j |
| 异步处理 | Kotlin Coroutines |
| 序列化 | kotlinx.serialization |
| 测试 | JUnit 5 + IntelliJ Test Framework |
| JDK | 17+ |

## 兼容性

| IDE | 最低版本 |
|-----|---------|
| IntelliJ IDEA | 2023.3+ |
| WebStorm | 2023.3+ |
| PyCharm | 2023.3+ |
| GoLand | 2023.3+ |
| 其他 IntelliJ Platform IDE | 2023.3+ |

## 快速开始

### 前置条件

- JetBrains IDE 2023.3 或更高版本
- JDK 17+
- Kiro 账号

### 从源码构建

```bash
git clone https://github.com/Brainhu/Kiro-GUI.git
cd kiro-gui
./gradlew buildPlugin
```

### 安装插件

1. 打开 IDE → Settings → Plugins → ⚙️ → Install Plugin from Disk
2. 选择 `build/distributions/kiro-gui-*.zip`
3. 重启 IDE

### 首次使用

1. 安装后，右侧边栏会出现 Kiro-GUI 工具窗口
2. 点击登录，通过浏览器完成 OAuth 授权
3. 授权成功后即可开始使用

## 开发

### 项目结构

```
src/
├── main/
│   ├── kotlin/              # 插件源码
│   │   ├── chat/            # Chat 面板相关
│   │   ├── spec/            # Spec 管理器
│   │   ├── hooks/           # Hook 引擎
│   │   ├── steering/        # Steering 配置
│   │   ├── lsp/             # LSP 客户端
│   │   ├── auth/            # 认证模块
│   │   └── ui/              # 通用 UI 组件
│   └── resources/
│       └── META-INF/
│           └── plugin.xml   # 插件描述文件
└── test/                    # 测试代码
```

### 本地开发

```bash
# 启动带插件的 IDE 沙箱
./gradlew runIde

# 运行测试
./gradlew test

# 代码检查
./gradlew check
```

## 贡献

欢迎贡献代码。请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详情。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/your-feature`)
3. 提交更改 (`git commit -m 'Add some feature'`)
4. 推送到分支 (`git push origin feature/your-feature`)
5. 创建 Pull Request

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
