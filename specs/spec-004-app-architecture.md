# Spec 004：应用架构与工程结构

## 1. 目标
为第一版 Android MVP 定义一套足够稳、不过度设计、方便后续扩展的工程架构，支撑：
- 账号与邀请注册
- 本地内容资源加载
- 阅读器
- 生词本
- 阅读记录

该 spec 的重点不是炫技，而是保证第一版能稳定开发、测试和迭代。

---

## 2. 已确认约束
- 平台：仅 Android
- UI 技术：Jetpack Compose
- 第一版目标：可上线 MVP，先发内测包
- 内容第一版内置在 App 包内
- 账号第一版先用 mock / 测试态能力跑通
- 后续可能接正式短信、远程接口、内容分发

因此架构上需要满足两个条件：
1. 第一版实现简单、可快速交付
2. 后续切真实后端时，不推翻整体结构

---

## 3. 总体架构建议
推荐采用：

> **单应用 + 按 feature 分包 + MVVM + Repository + Local/Remote DataSource**

这是当前最适合你这个项目的方案。

原因：
- 对 Compose 友好
- 对 Codex 也友好，生成代码时边界清楚
- 第一版规模不大，没必要一上来做多模块巨型工程
- 后续如果项目变大，再从 feature package 平滑演进到真正多 module

---

## 4. 分层设计

### 4.1 UI 层
职责：
- Compose 页面
- 状态展示
- 用户交互
- 事件分发

包含：
- `Screen`
- `Composable`
- `UiState`
- `UiEvent`
- `UiAction`

### 4.2 Presentation 层
职责：
- ViewModel
- 页面状态转换
- 调用 UseCase / Repository
- 错误处理与加载态管理

### 4.3 Domain 层（轻量）
职责：
- 核心业务对象
- UseCase（只保留必要部分）

说明：
- 第一版不用为了“架构纯洁”把 domain 层做得太重
- 只在复杂逻辑出现时引入 UseCase
- 简单场景允许 ViewModel 直接依赖 Repository

### 4.4 Data 层
职责：
- Repository 实现
- Local DataSource
- Remote / Mock DataSource
- DTO / Entity 映射

---

## 5. 包结构建议
建议工程先采用单 module `app`，内部按 feature 分包。

```text
app/
  src/main/java/.../
    core/
      common/
      model/
      ui/
      navigation/
      datastore/
      database/
      audio/

    feature/
      auth/
        ui/
        data/
        domain/

      bookshelf/
        ui/
        data/
        domain/

      reader/
        ui/
        data/
        domain/

      vocabulary/
        ui/
        data/
        domain/

      profile/
        ui/

    app/
      MainActivity.kt
      App.kt
      AppNavHost.kt
```

---

## 6. 模块职责建议

### 6.1 `core/common`
放：
- Result 封装
- 通用扩展函数
- 常量
- Dispatcher / 时间工具

### 6.2 `core/model`
放：
- 通用数据模型
- 跨 feature 共用的领域对象

例如：
- `Book`
- `BookPage`
- `User`
- `ReadingProgress`
- `VocabularyItem`

### 6.3 `core/ui`
放：
- 通用 Compose 组件
- 主题
- 颜色、字体、间距
- 通用弹层组件

### 6.4 `core/navigation`
放：
- 路由定义
- NavHost 封装
- 路由参数处理

### 6.5 `core/datastore`
放：
- 登录态存储
- 用户偏好（后续）

### 6.6 `core/database`
放：
- Room Database
- DAO
- Entity

### 6.7 `core/audio`
放：
- 统一音频播放封装
- 句子音频 / 单词音频播放接口

---

## 7. Feature 设计

### 7.1 auth
负责：
- 手机号登录
- 邀请码注册
- 昵称补填
- 登录态读写

### 7.2 bookshelf
负责：
- 书架首页
- 最近阅读
- 绘本列表
- 进入阅读器

### 7.3 reader
负责：
- 页面渲染
- 翻页
- 单词点击
- 整句播放
- 阅读进度保存

### 7.4 vocabulary
负责：
- 生词本列表
- 生词收藏 / 删除
- 生词去重

### 7.5 profile
第一版很轻：
- 昵称展示
- 退出登录
- 可预留设置页

---

## 8. 数据源设计

## 8.1 Auth 数据源
第一版建议：
- `AuthRemoteDataSource` 先用 fake/mock 实现
- `SessionLocalDataSource` 用 DataStore

接口形态预留真实后端：
- `sendCode(phone)`
- `login(phone, code)`
- `verifyInvite(code)`
- `register(phone, inviteCode)`
- `updateNickname(nickname)`

## 8.2 Content 数据源
- `LocalBookPackageDataSource`
- 从 `filesDir/book-packages/installed/<book-id>/package/` 加载 `book.json / pages.json / words.json`
- `RemoteBookshelfManifestSource`
- 从远程 `bookshelf.json` 加载书架预览，点书时再按需下载内容包

## 8.3 Reader / Vocabulary 数据源
- `ReadingProgressLocalDataSource` -> Room
- `VocabularyLocalDataSource` -> Room

说明：
- 第一版不必把所有东西都做成远端 + 本地双写
- 先本地稳定，再扩展远端同步

---

## 9. 存储建议

### 9.1 DataStore
用于：
- token
- userId
- loginState
- nicknameCompleted

### 9.2 Room
用于：
- 阅读进度
- 生词本
- 可能的书籍索引缓存（如果后续需要）

### 9.3 Assets
用于：
- 绘本封面
- 页面图片
- 单词音频
- 句子音频
- 结构化内容 json

---

## 10. 导航结构建议

建议主路由：
- `Splash`
- `Login`
- `Invite`
- `Nickname`
- `Bookshelf`
- `Reader/{bookId}`
- `Vocabulary`
- `Profile`

说明：
- 第一版不用底部 Tab 也可以
- 书架是主入口
- 生词本从阅读页右上角进入

---

## 11. UI 状态管理建议
每个页面定义：
- `UiState`
- `UiAction`
- `UiEvent`

例如 Reader：
- `ReaderUiState`
- `ReaderUiAction`
- `ReaderUiEvent`

这样可以让 Codex 更容易按固定模式产出代码。

---

## 12. 错误处理建议
### 第一版原则
- 错误提示清楚
- 不做复杂错误系统
- 不要静默失败

### 建议处理类型
- 输入错误
- 邀请码无效
- 资源加载失败
- 音频播放失败
- 本地数据库读写失败

---

## 13. 测试建议
### 第一版至少覆盖
- 邀请码注册流程
- 登录态恢复
- 书架内容加载
- 阅读进度保存 / 恢复
- 生词本去重

### 测试类型建议
- ViewModel 单元测试
- Repository 单元测试
- 少量关键 UI 流程测试

---

## 14. 非功能性要求
- 冷启动尽量快
- 书架加载不依赖网络
- 阅读页翻页不卡顿
- 音频播放响应及时
- 页面状态可恢复

---

## 15. 验收标准
1. 项目按 feature 分包，职责清晰。
2. 登录态、阅读记录、生词本三类数据边界清楚。
3. 本地资源加载、DataStore、Room 三类存储职责不混乱。
4. 后续接正式后端时，不需要推翻 UI 层。
5. 第一版可以在不引入复杂多模块工程的前提下稳定开发。

---

## 16. 后续扩展方向
- 从单 module 逐步拆成多 module
- 增加远程 API 层
- 增加内容下载与版本管理
- 增加正式短信服务
- 增加用户体系和同步机制
