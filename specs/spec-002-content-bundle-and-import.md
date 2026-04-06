# Spec 002：内容资源包与导入链路

## 1. 目标
建立第一版绘本内容的资源组织规范和导入流程，支撑：
- 书架展示
- 阅读页渲染
- 单词点读
- 整句播放
- 生词本

第一版不在 App 内直接解析 PDF / EPUB，而是将原始内容整理成内置资源包后打进安装包。

---

## 2. 已确认约束
- 内容来源：牛津树
- 当前仅自己小范围使用，不考虑商业化
- 原始内容形式：PDF / EPUB
- 第一版内容交付方式：内置在 App 包内
- 第一版绘本数量：3-5 本
- 中文辅助：只做单词释义，不做整页中文翻译
- 音频来源：第一版先用 TTS
- 内容整理方式：先手工，后续再逐步脚本化

---

## 3. 范围
### 3.1 本 spec 包含
- Book / Page / Word 的内容模型
- 资源目录规范
- PDF / EPUB 到资源包的整理流程
- 图片、文本、音频的命名规范
- TTS 音频生成边界
- App 内内容加载方式

### 3.2 本 spec 不包含
- App 内直接打开 PDF / EPUB
- 在线内容分发
- 内容后台 CMS
- 自动 OCR 高精度流程
- 商业版权管理
- 多语种扩展

---

## 4. 内容策略
### 4.1 第一版整体策略
第一版采用“离线整理 + App 内置”的方式：
1. 获取原始 PDF / EPUB
2. 在电脑端整理出页面图片、英文正文、单词释义、音频
3. 生成结构化索引文件
4. 打包进 Android assets

### 4.2 为什么不直接在 App 内读 PDF / EPUB
原因：
- 运行时复杂度高
- 页面图文对齐不稳定
- 不利于点词与点句播放
- 不利于后续资源精细控制

因此第一版明确采用结构化资源包方案。

---

## 5. 资源目录规范
建议目录：

```text
assets/
  books/
    oxford-tree-01/
      book.json
      cover.png
      pages/
        001.png
        002.png
        003.png
      audio/
        sentence_001.mp3
        sentence_002.mp3
        word_apple.mp3
        word_dog.mp3
      words.json
      pages.json
```

说明：
- 一本书一个目录
- 页面图片单独放 `pages/`
- 音频统一放 `audio/`
- 页面结构与单词结构用 json 描述

---

## 6. 数据模型

### 6.1 Book
```kotlin
data class Book(
    val id: String,
    val title: String,
    val level: String,
    val coverAsset: String,
    val pageCount: Int,
    val tags: List<String> = emptyList(),
    val enabled: Boolean = true,
)
```

### 6.2 BookPage
```kotlin
data class BookPage(
    val bookId: String,
    val pageNo: Int,
    val imageAsset: String,
    val englishText: String,
    val sentenceAudioAsset: String? = null,
    val words: List<PageWordRef> = emptyList(),
)
```

### 6.3 Word
```kotlin
data class Word(
    val id: String,
    val text: String,
    val meaningZh: String,
    val phonetic: String? = null,
    val audioAsset: String? = null,
)
```

### 6.4 PageWordRef
```kotlin
data class PageWordRef(
    val wordId: String,
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
)
```

说明：
- `PageWordRef` 用于把页面正文中的单词点击范围和词典实体关联起来
- 第一版尽量在内容整理阶段准备好索引，不在客户端动态猜测切词

---

## 7. JSON 文件建议

### 7.1 book.json
```json
{
  "id": "oxford-tree-01",
  "title": "The Apple",
  "level": "L1",
  "coverAsset": "cover.png",
  "pageCount": 12,
  "tags": ["daily-life", "family"],
  "enabled": true
}
```

### 7.2 pages.json
```json
[
  {
    "bookId": "oxford-tree-01",
    "pageNo": 1,
    "imageAsset": "pages/001.png",
    "englishText": "Dad has an apple.",
    "sentenceAudioAsset": "audio/sentence_001.mp3",
    "words": [
      { "wordId": "dad", "text": "Dad", "startIndex": 0, "endIndex": 3 },
      { "wordId": "apple", "text": "apple", "startIndex": 11, "endIndex": 16 }
    ]
  }
]
```

### 7.3 words.json
```json
[
  {
    "id": "apple",
    "text": "apple",
    "meaningZh": "苹果",
    "phonetic": "/ˈæpəl/",
    "audioAsset": "audio/word_apple.mp3"
  }
]
```

---

## 8. 内容整理流程

### Step 1：准备原始内容
输入：
- PDF / EPUB
- 书名
- 分级
- 封面图

### Step 2：拆分页面图片
产出：
- 每页一张图片
- 统一页码命名，例如 `001.png`

### Step 3：整理每页英文正文
要求：
- 手工校对
- 确保文本与页面对应
- 避免 OCR 错词直接进入 App

### Step 4：整理单词数据
产出：
- 单词文本
- 中文释义
- 可选音标
- 对应音频文件名

### Step 5：生成音频
第一版采用 TTS：
- 每页整句音频
- 单词音频

### Step 6：生成结构化文件
- `book.json`
- `pages.json`
- `words.json`

### Step 7：放入 assets 并验证
- 启动 App 检查书架是否可见
- 打开某书检查翻页、点词、播放是否正常

---

## 9. App 侧加载流程
### 9.1 启动时
1. 扫描 `assets/books/`
2. 读取每本书的 `book.json`
3. 建立书架索引

### 9.2 打开某本书时
1. 读取 `pages.json`
2. 读取 `words.json`
3. 构建页面数据
4. 进入阅读器

---

## 10. 验收标准
1. 至少能稳定加载 3 本以上绘本资源。
2. 每本书都能展示封面、标题、页数。
3. 阅读页能按页显示图片与英文正文。
4. 页面句子音频与单词音频路径可正确解析。
5. 单词点击能通过 `PageWordRef` 定位到释义数据。
6. 资源目录命名统一，不依赖硬编码散落在 UI 层。

---

## 11. 风险与后续扩展
### 11.1 风险
- PDF / EPUB 与实际页面图片未必天然对齐，需要人工校对。
- 若 TTS 质量不稳定，会直接影响第一版阅读体验。
- 若资源图片过大，会导致包体膨胀。
- 若单词索引不准确，点词体验会明显变差。

### 11.2 后续扩展方向
- 将手工整理流程逐步脚本化
- 引入离线资源生成工具
- 将内置资源迁移到服务端动态分发
- 扩展整页中文辅助或更多阅读元数据

---

## 12. 当前未决问题
以下问题尚未完全拍板，后续实现前需要再确认：
- TTS 使用哪一个具体 provider
- 页面文本提取是纯手工还是部分脚本辅助
- 内置 3-5 本时的包体大小上限控制策略
- 是否需要给内容资源增加版本号字段
