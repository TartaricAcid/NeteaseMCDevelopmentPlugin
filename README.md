<div align="center">

<img src="https://raw.githubusercontent.com/TartaricAcid/MCStudioHelper/main/src/main/resources/META-INF/pluginIcon.svg" alt="MC Studio Helper Logo" style="width: 240px; max-width: 100%;">

# MC Studio Helper（MC Studio 助手）

[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**为网易我的世界中国版提供组件开发与调试支持的 PyCharm 插件**

<p style="color: gray">注意：本插件与网易官方无任何从属关系，仅供个人学习与开发使用。</p>

[插件使用教程](./MC%20Studio%20Helper%20插件食用指北.pdf)

</div>

## 简短说明

- 本插件旨在简化在本地环境下为网易我的世界中国版编写、运行与调试扩展/组件的工作流。
- 当前为社区/个人维护的非官方工具，功能仍在持续开发中。

## 主要功能

- ✅ 在 PyCharm 中添加运行配置，直接运行开发端
    - ✅ 运行配置支持协同运行组件、设置世界规则等
    - ✅ 结合 LSP4IJ 插件和 MCPDB 程序，可实现断点调试功能
- ✅ 将开发端日志重定向到 PyCharm 的日志控制台并提供着色支持
    - ✅ 支持错误日志文件快速跳转等功能
- ✅ 在游戏中通过快捷键快速重载组件与世界（例如按键 R 或小键盘 0）
- 🚧 新建预设组件模板、代码补全与提示（画饼中）

## 鸣谢

感谢以下项目提供的支持与参考：

- Zero123 的 [MCDevTool](https://github.com/GitHub-Zero123/MCDevTool)：提供了思路与部分代码
- Dofes 的 [MCPDB](https://github.com/Dofes/mcpdb)：提供了启动器的 DAP 协议支持
- CloudburstMC 的 [NBT 解析库](https://github.com/CloudburstMC/NBT)：提供了基岩版 NBT 解析支持
- QuMod 开发者交流群：感谢各位朋友提供的帮助

## 许可证

本项目采用 MIT 许可证，详情请参阅 [LICENSE](LICENSE) 文件。

欢迎 Issues 与 PR 提交你的意见和贡献。