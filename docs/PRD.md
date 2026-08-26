# MyGallery — Product Requirements Document

**Version:** 0.1
**Status:** Draft

---

## 1. 产品定位

MyGallery 是一个以"时间"为核心组织方式的个人摄影作品网站。

它不是传统图库，也不是博客，而更像一本持续更新的线上摄影档案。

**核心视觉语言：**

`Modern Editorial Photography × Apple Minimalism × Analog Film Feeling`

---

## 2. Product Goals

- 展示个人摄影作品
- 让访问者长期浏览和关注新的摄影作品
- 通过时间线展示摄影风格和生活轨迹变化
- 照片优先，UI 尽可能退到背景
- 保持简洁、克制、有个人审美
- 后续增加照片时不需要修改页面结构
- 为未来后台上传、作品管理和图片保护能力预留空间

---

## 3. Target Users

### Primary User

- 朋友
- 同学
- 摄影爱好者
- 社交媒体关注者
- 潜在合作对象
- Portfolio Reviewer / 招聘方

**主要行为：**

- 浏览照片
- 查看最近作品
- 查看过去不同时间段作品
- 打开单张照片
- 查看作品信息
- 了解摄影者本人

### Secondary User

网站所有者本人。

**未来主要行为：**

- 添加照片
- 编辑 metadata
- 删除照片
- 设置 featured
- 设置 visibility
- 管理 Timeline

> 后台功能不属于当前第一版公开页面开发。

---

## 4. Core Information Architecture

网站以 `Photography by Time` 为主要内容组织方式。

**核心结构：**

```text
2026
├── August
├── July
└── June

2025
├── December
├── November
└── ...
```

**Category / Tag** 例如：

- Street
- Travel
- Landscape
- Portrait
- Film

未来可以存在，但不是主要导航结构。

---

## 5. Main Navigation

第一版公开导航：

- INDEX
- ARCHIVE
- ABOUT

CONTACT 可以放在 About 内，不一定作为一级导航。

**以下内容不要出现在公开导航：**

- Admin
- Upload
- Login
- Dashboard

---

## 6. Homepage

Homepage 是整个网站最重要的视觉入口。

**首页内容顺序：**

```text
Hero
↓
Horizontal Photo Stream
↓
Timeline Archive
```

### 6.1 Hero

包含：

- TOM RICK
- PHOTOGRAPHY
- 作品年份范围，例如 `2024 — 2026`
- 一张随机精选照片
- 简短的摄影档案描述

**Hero Photo 选取规则：**

- 从 `featured = true` 的作品中随机选择
- 刷新页面时可以出现不同 Featured Photo

---

## 7. Horizontal Photo Stream

Hero 下方展示 `Selected Frames`。

**要求：**

- 多张照片横向排列
- 有轻微流动 / 轨道感
- 支持触控板
- 支持鼠标滚轮
- 支持鼠标拖动
- 可以有非常缓慢的自动漂移
- 用户主动操作时优先服从用户输入
- 动效必须克制

---

## 8. Timeline Archive

Archive 是网站核心内容区域。

**数据结构：** `Year → Month → Photos`

**示例：**

```text
2026

AUGUST

[ Photo ]

        [ Photo ]

                  [ Photo ]

JULY

        [ Photo ]

[ Photo ]
```

- 不要使用传统"竖线 + 圆点"的程序员 Timeline
- 时间只作为章节标题

---

## 9. Archive Visual Layout

同一时间段内照片使用：

- Editorial layout
- Floating layout
- Asymmetric layout
- Large whitespace

**允许：**

- 不同图片尺寸
- 横图与竖图混排
- 不完全对齐
- 图片之间有大量留白

目标是摄影杂志感，而不是普通 Gallery Grid。

---

## 10. Image Hover

鼠标移动到照片：

- 当前图片轻微放大，推荐范围 `1.03–1.06`
- 可以让其他图片略微降低 opacity
- 可以显示 title / location
- 动效必须缓慢、克制

**禁止：**

- 夸张 3D
- 大幅旋转
- 弹跳
- 强烈动效

---

## 11. Photo Detail

第一版点击照片后使用 `Lightbox / Full-screen Viewer`，而不是立即为每张照片创建独立详情页。

**第一版显示：**

- Large Image
- Title
- Date
- Location
- Previous
- Next
- Close

**未来扩展：**

- Camera
- Lens
- Focal Length
- Aperture
- Shutter Speed
- ISO
- Description
- Tags

---

## 12. About

About 页面保持简单。

**内容：**

- 简短个人介绍
- Photography statement
- Location
- Social / Contact

不要把 About 做成复杂个人简历。

---

## 13. Responsive Design

| 平台 | 布局策略 |
|------|----------|
| Desktop | 使用更自由的 Floating Editorial Layout |
| Mobile | 使用更稳定的 Vertical Editorial Layout |

不要强行把复杂桌面错位布局原样复制到手机端。

---

## 14. Content Model

页面与作品数据必须完全分离。每张照片作为 Photo Object。

**第一版基础字段：**

```text
id
image
title
takenAt
year
month
location
orientation
featured
visibility
```

**未来字段：**

```text
camera
lens
aperture
shutterSpeed
iso
focalLength
tags
description
```

禁止把作品数据硬编码在 HTML 页面结构中。

---

## 15. Featured Logic

`featured = true` 可以用于：

- Homepage Hero
- Selected Frames
- Future Highlights
- Social Preview

Featured 不影响作品在 Timeline 中的位置。

---

## 16. Empty Collection Behaviour

- 如果某个月只有一张照片：正常展示，不重复照片填满页面
- 如果某一年没有作品：不显示该年份

---

## 17. Performance Goal

优先确保：

- 首屏快速加载
- Hero 尽快出现
- 其他图片 Lazy Load
- 页面布局不要因为图片加载产生严重跳动

具体 CDN / 图片压缩方案留到 Technical Design。

---

## 18. Privacy & Image Protection Goal

产品目标**不是**：完全阻止任何人复制公开图片。

**而是：**

- 不公开原始高分辨率照片
- 不暴露原始资产地址
- 提高批量抓取成本
- 控制后台权限
- 防止未经授权修改内容

技术实现方式留到 Step 3。

---

## 19. MVP Scope

第一版正式上线包含：

- Homepage
- Random Hero Photo
- Horizontal Photo Stream
- Timeline Archive
- Photo Hover
- Lightbox
- About
- Responsive Layout
- Dynamic Photo Data
- Basic SEO
- Custom Domain

---

## 20. Not MVP

第一版暂时不做：

- 用户注册
- 评论
- 点赞
- 收藏
- Social Feed
- 商城
- Public Download
- 搜索
- AI 推荐
- Public Upload
- 用户投稿
- 复杂分类系统

---

## 21. Future Features

**未来后台：**

```text
Admin
├── Upload Photo
├── Edit Metadata
├── Delete Photo
├── Featured
├── Visibility
└── Timeline Management
```

**未来可加入 EXIF 自动读取：**

- Camera
- Lens
- ISO
- Aperture
- Shutter Speed
- Date

---

## 22. Product Principle

所有设计和开发都遵循：`Photography First`

如果一个功能：

- 抢照片注意力
- 增加不必要 UI
- 明显降低加载速度
- 让网站像 SaaS
- 让网站像模板 Portfolio

**优先删除或简化。**

最终体验应该更像：一本持续更新的个人摄影杂志。
