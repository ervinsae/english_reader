# English Reader App

面向小学生的英语绘本阅读 App。

## 当前状态
- 已完成 MVP 级 spec 文档
- 已完成 Android Compose 工程骨架
- 当前重点：按 spec 分阶段实现，而不是一次性生成完整 App

## 目录
- `specs/`：需求与架构文档
- `app/`：Android 应用代码
- `docs/book-ingestion.md`：PDF + MP3 书籍入库流程
- `docs/content-packages.md`：内容包规范、目录配置、导入方式
- `scripts/ingest_book.py`：从 PDF/MP3 生成本地书籍资源目录
- `tools/package_content_package.py`：标准内容包 zip 打包脚本

## 当前骨架包含
- Jetpack Compose
- Navigation Compose
- DataStore 登录态骨架
- Room 阅读进度 / 生词本骨架
- feature 分包结构

## 注意
当前机器未安装 `gradle` / `sdkmanager`，因此这次先手写了工程骨架文件。
如果要在本机直接命令行构建，后续需要补齐 Gradle Wrapper 或使用 Android Studio 导入项目。
