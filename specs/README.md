# 小学英语绘本阅读 App Specs

## 项目定位
面向小学学生的英语绘本阅读 App，第一版仅做 Android 端，技术栈使用 Jetpack Compose。

## 当前边界
- 仅 Android 端
- 仅 Compose
- 内容先内置在 App 包内
- 内容先使用牛津树，当前仅限自己小范围使用，不考虑商业化
- 目标是可上线 MVP，并先以内测包形式给少量用户使用
- 需求不明确时必须继续澄清，不默认扩展功能范围

## Spec 列表
1. `spec-001-auth-and-invite.md`
   - 手机号登录
   - 固定测试验证码
   - 邀请码注册
   - 登录态持久化

2. `spec-002-content-bundle-and-import.md`
   - 内容资源结构
   - PDF / EPUB 到内置资源包的整理流程
   - TTS 音频生成
   - 资源目录与数据模型

3. `spec-003-reader-and-vocabulary.md`
   - 书架即首页
   - 阅读器
   - 点词 / 点句播放
   - 生词本与阅读记录

4. `spec-004-app-architecture.md`
   - Compose 工程结构
   - Feature 分包
   - DataStore / Room / Assets 分层
   - Mock 到真实后端的切换边界

5. `spec-005-implementation-plan.md`
   - 实施阶段拆解
   - 里程碑与验收标准
   - Codex 协作粒度建议

## 建议开发顺序
1. spec-004 应用架构与工程结构
2. spec-001 账号与邀请注册
3. spec-002 内容模型与导入链路
4. spec-003 书架 / 阅读器 / 生词本
5. spec-005 实施计划与里程碑

## 协作规则
- 每个 spec 先定范围，再定页面、数据、交互、验收标准
- 当需求不明确时，要先提问，不替用户拍板
- 每个 spec 完成后再拆实现任务，而不是一开始直接生成全项目代码
