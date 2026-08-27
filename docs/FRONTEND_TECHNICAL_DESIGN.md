# MyGallery — Frontend Technical Design

**Version:** 0.2
**Status:** Draft
**Step:** 3A — Frontend Technical Design

**Changelog 0.2：** 修正 Random Hero 与 ISR 的缓存冲突（§5–§7）；修正 Server PhotoCard 与 Client Lightbox 的边界（§3、§5、§6、§12）；前端 DTO 对齐后端正式契约 `docs/DATA_MODEL.md` / `docs/TECHNICAL_DESIGN.md`（§7、§8、§9）。

---

## 1. Frontend Overview

MyGallery 正式前端是一个以摄影作品为核心的展示型网站，基于 Next.js 16 构建。它的职责单一：

> 通过 Spring Boot REST API 获取作品数据，以 `Modern Editorial Photography × Apple Minimalism × Analog Film Feeling` 的视觉语言呈现给访问者。

前端遵循 `Photography First` 原则：

- UI 退到背景，照片是交互中心
- Server Components 优先，客户端 JavaScript 最小化
- 不引入重型动画框架，优先 CSS 与浏览器原生 API
- `prototype/` 仅作为视觉参考，不复用其实现代码

**非目标（V1）：**

- 不做 Admin Dashboard UI
- 不做认证 / 登录
- 不直连数据库
- 不持有 Cloudinary API Secret

---

## 2. Technology Stack

| 领域 | 选型 | 说明 |
|------|------|------|
| Framework | Next.js 16 | App Router，RSC 优先 |
| UI | React | — |
| Language | TypeScript | 严格模式 |
| Router | Next.js App Router | 文件系统路由 |
| Styling | CSS Modules | 无 Tailwind |
| Images | 后端固定 variant URL（thumbnail / card / display） | 前端不自行构造 Cloudinary transformation；防 CLS |
| Deployment | Vercel | — |
| Repository | Monorepo | 前端根目录 `frontend/` |

**明确不引入（V1）：**

- Three.js / GSAP / Framer Motion
- Tailwind
- 全局状态管理库（Redux / Zustand 等，V1 无此需求）

---

## 3. Project Structure

```text
frontend/
├── app/
│   ├── layout.tsx                  # 根布局：字体、Header、Footer
│   ├── page.tsx                    # Homepage（Server Component）
│   ├── globals.css                 # Reset + Design Tokens + Typography
│   ├── about/
│   │   └── page.tsx                # About（Server Component）
│   ├── sitemap.ts                  # 站点地图
│   ├── robots.ts                   # robots.txt
│   └── icon.tsx                    # favicon
│
├── components/
│   ├── Header/
│   │   ├── Header.tsx
│   │   └── Header.module.css
│   ├── Hero/
│   │   ├── Hero.tsx                # Server：结构 + 确定性 fallback 渲染
│   │   ├── HeroPicker.tsx          # Client：水合后从 featured 池随机选择
│   │   └── Hero.module.css
│   ├── PhotoStream/
│   │   ├── PhotoStream.tsx         # Client：交互外壳
│   │   ├── PhotoStreamTrack.tsx    # Server：渲染照片列表
│   │   └── PhotoStream.module.css
│   ├── TimelineArchive/
│   │   ├── TimelineArchive.tsx     # Server：数据分组 + 结构
│   │   └── TimelineArchive.module.css
│   ├── TimelineSection/
│   │   ├── TimelineSection.tsx     # Server：Year → Month 渲染
│   │   └── TimelineSection.module.css
│   ├── PhotoCard/
│   │   ├── PhotoCard.tsx           # Server：单张照片 + 布局变体
│   │   └── PhotoCard.module.css
│   ├── Lightbox/
│   │   ├── Lightbox.tsx            # Client：全屏查看器
│   │   ├── LightboxProvider.tsx    # Client：全局 Lightbox 状态
│   │   ├── PhotoLightboxTrigger.tsx # Client：嵌入 PhotoCard 的最小点击触发器
│   │   └── Lightbox.module.css
│   └── Footer/
│       ├── Footer.tsx
│       └── Footer.module.css
│
├── lib/
│   ├── api/
│   │   ├── photos.ts               # getPhotos / getFeaturedPhotos / getPhoto
│   │   ├── archive.ts              # getArchive
│   │   └── client.ts               # fetch 封装（base URL、错误处理）
│   └── utils/
│       ├── groupByTime.ts          # Photo[] → ArchiveYear[]
│       └── layoutVariant.ts        # 布局变体分配
│
├── types/
│   └── photo.ts                    # Photo / PhotoSummary / ArchiveYear / ArchiveMonth
│
└── public/                         # 静态资源（og 默认图等）
```

**与建议结构的差异说明：**

- `PhotoStream` 拆为「Server 渲染轨道 + Client 交互外壳」两层，避免整段照片列表被打包进客户端 JS
- `Hero` 拆为「Server 结构 + Client `HeroPicker`」两层：Server 渲染确定性 fallback 保证首屏不为空，`HeroPicker` 只在水合后做随机选择（详见 §7 Random Hero）
- `Lightbox` 增加 `LightboxProvider`（跨组件全局状态）与 `PhotoLightboxTrigger`（嵌入 Server PhotoCard 的最小 Client 点击边界，详见 §12）
- 不设独立 `styles/` 目录：`globals.css` 放 `app/` 下符合 Next.js 惯例，组件样式与组件共置（CSS Modules 的核心价值）

**克制原则：** 不拆 `Button`、`Caption`、`SectionHead` 等过细组件；相同样式通过 CSS Module 内的类复用解决。

---

## 4. Routing

第一版公开路由只有两条页面路由：

| 路由 | 页面 | 渲染策略 |
|------|------|----------|
| `/` | Homepage（Hero → Photo Stream → Timeline Archive） | Server，ISR |
| `/about` | About | Server，静态 |

**锚点导航（不产生新路由）：**

| 导航项 | 目标 |
|--------|------|
| INDEX | `/` 顶部 |
| ARCHIVE | `/#archive`（Homepage 的 Timeline Archive 区域） |
| ABOUT | `/about` |

**数据重新验证：** 作品数据通过 ISR（Incremental Static Regeneration）更新，例如 `revalidate = 3600`；未来 Admin 发布新照片后可通过 On-Demand Revalidation 立即刷新。具体机制在 Step 3B（后端设计）中联调确定。

**预留：** `/admin` 路由段未来存在，V1 不实现（见 §19）。

---

## 5. Component Architecture

组件树（Homepage）：

```text
RootLayout (Server)
├── Header (Server)
└── page.tsx (Server)
    ├── Hero (Server)                     # 结构 + 确定性 fallback
    │   └── HeroPicker (Client)           # 水合后随机选择（见 §7）
    ├── PhotoStream (Client 外壳)
    │   └── PhotoStreamTrack (Server children)
    │       └── PhotoCard × N (Server)
    │           └── PhotoLightboxTrigger (Client 叶子)
    ├── TimelineArchive (Server)
    │   └── TimelineSection × Year (Server)
    │       └── PhotoCard × N (Server, layoutVariant)
    │           └── PhotoLightboxTrigger (Client 叶子)
    ├── LightboxProvider (Client)
    │   └── Lightbox (Client, 按需渲染)
    └── Footer (Server)
```

**关键设计决策：**

1. **PhotoCard 保持 Server Component，不直接触碰 Client Context。** Server Component 不能使用 Client Context、不能绑定浏览器 `onClick`、不能调用 Client Provider state。因此点击行为由嵌入卡片内部的薄 Client 叶子 `PhotoLightboxTrigger` 承担：PhotoCard 负责图片 / caption / 变体布局（Server 渲染、hover 纯 CSS），Trigger 只接收打开 Lightbox 所需的最小数据并调用 `LightboxProvider`（详见 §12）。Timeline 与照片数据逻辑不因此变成 Client。
2. **Hero 随机是 presentation randomness，不是 data randomness。** Server 获取 featured 池并渲染确定性 fallback，`HeroPicker` 在水合后随机切换（详见 §7）。Hero / Homepage / `page.tsx` 均不因此变成 Client Component。
3. **数据分组在服务端完成。** Homepage 优先消费后端 `ArchiveResponse`（已分组）；降级时才由 `groupByTime.ts` 在服务端自行分组，客户端不重复计算。
4. **布局变体在渲染时确定。** `TimelineSection` 根据照片数量与 `orientation` 为每张卡片分配变体（见 §10），不依赖 CSS `nth-child` hack。
5. **Header / Footer 是 Server Component。** V1 无交互导航（无移动端汉堡菜单动画需求时），纯静态渲染。

---

## 6. Server vs Client Components

### Server Components（默认）

| 模块 | 理由 |
|------|------|
| `app/page.tsx` | 数据获取、结构组装 |
| `app/about/page.tsx` | 纯静态内容 |
| `Header` / `Footer` | 无交互 |
| `Hero` | 结构在服务端渲染，featured 池在服务端获取，确定性 fallback 直出 HTML |
| `PhotoStreamTrack` | 照片列表 HTML 由服务端输出 |
| `TimelineArchive` / `TimelineSection` | 分组、变体分配、渲染全部在服务端 |
| `PhotoCard` | 展示组件，hover 效果由纯 CSS 实现；不含 onClick / context |

### Client Components（`"use client"`，最小化）

| 模块 | 必须客户端的理由 |
|------|------------------|
| `HeroPicker` | 水合后从 featured 池随机选择一张照片（client-side presentation randomness） |
| `PhotoStream`（外壳） | wheel 事件转换、pointer drag、自动漂移、`prefers-reduced-motion` 检测 |
| `PhotoLightboxTrigger` | Server PhotoCard 无法绑定 onClick / 使用 Client Context，需要最小 Client 叶子转发点击到 `LightboxProvider` |
| `LightboxProvider` | 全局 Lightbox 开关状态、当前索引 |
| `Lightbox` | 键盘导航（←/→/Esc）、触摸 swipe、focus 管理、焦点归还 |

**边界原则：**

- `"use client"` 只标在叶子交互节点，**绝不在 `page.tsx` 或布局级标记**
- Client 外壳通过 `children` 接收 Server 渲染的内容（composition pattern），照片列表不进客户端 bundle
- Client 叶子（`HeroPicker` / `PhotoLightboxTrigger`）只接收可序列化的最小 props（如照片 id 列表 / 当前照片数据），不反向把数据逻辑拉进客户端
- hover 放大 / opacity 变化是 CSS `:hover`，不是 React state

---

## 7. Data Flow

```text
┌─────────────┐      ┌──────────────────────┐      ┌────────────┐
│  PostgreSQL  │ ◄─── │  Spring Boot REST API │ ◄─── │  Next.js   │
└─────────────┘      └──────────────────────┘      └────────────┘
                              │                         │
                              ▼                         ▼
                        Photo DTO (JSON)          React Components
                                                  (Server 渲染)
```

**约束：**

- 前端**只**通过 REST API 获取数据，绝不直连 PostgreSQL
- 前端**不持有** Cloudinary API Secret，不参与上传签名
- 图片 URL 由后端 DTO 直接给出（固定 variant：`thumbnailUrl` / `cardUrl` / `displayUrl`），前端不自行构造 transformation

**消费 API（契约以 `docs/TECHNICAL_DESIGN.md` §6.1 与 `docs/DATA_MODEL.md` §9–§10 为准）：**

| Endpoint | 参数 | 响应 | 前端用途 |
|----------|------|------|----------|
| `GET /api/v1/photos` | `year?`, `month?`, `order=newest\|oldest`, `page=0`, `size=24`（max 100） | `PhotoPageResponse`（`items` + 分页字段） | 通用列表；`lib/api` 适配层解包 `items`，组件只接收 `PhotoSummary[]` |
| `GET /api/v1/photos/featured` | `limit=12`（max 30），newest first | `PhotoSummaryResponse[]` | Hero 随机池（见下） |
| `GET /api/v1/photos/{id}` | — | `PhotoResponse` | 完整详情，Lightbox 未来扩展用 |
| `GET /api/v1/archive` | `year?` | `ArchiveResponse`（`years[]` 已按 Year → Month → Photos 分组） | **Homepage Timeline 的首选数据源** |

**数据获取位置：** 全部在 Server Component 内通过 `lib/api/` 的封装函数完成。V1 无客户端数据请求（无 SWR / React Query）。公开响应不含 `visibility` / `cloudinaryPublicId` / 原始 URL，前端模型也不定义这些字段。

**降级方案：** 若 `/api/v1/archive` 暂不可用，前端用 `lib/utils/groupByTime.ts` 在服务端自行分组——分组逻辑只写一次，两种来源都可用。

### Random Hero：与 ISR 解耦

**问题：** 若 Server Component 在页面生成时随机选择 Hero，ISR 缓存会让所有访问者在一段时间内看到同一张照片，不符合"不同访问 / 刷新出现不同照片"的产品期望。

**方案——client-side presentation randomness（而非 server-side data randomness）：**

```text
Server
  ↓ fetch GET /api/v1/photos/featured（featured 池，可随 ISR 缓存）
  ↓ 渲染确定性 fallback Hero（例如池中第一张，首屏不为空、无闪烁）
Client HeroPicker
  ↓ hydration 完成后，从 featuredPhotos props 中随机选择一张
  ↓ 若为 fallback 同一张则无操作；否则平滑切换图片与 caption
```

- **缓存与随机展示解耦：** ISR 缓存的是"featured 池"这份数据与 fallback HTML；随机发生在每个访问者的浏览器里，不影响缓存语义
- **`HeroPicker` 是极轻 Client Component：** 只接收可序列化的 `featuredPhotos: PhotoSummary[]`，只负责初始化时的一次随机选择，不处理 hover、不处理路由
- **Hero / Homepage / `page.tsx` 均不因此变成 Client Component**；Timeline / Archive 继续使用 ISR 不受影响
- **空池降级：** featured 池为空时，fallback 回退到 archive 中最新一张照片

**加载与错误策略（V1 从简）：**

- API 不可用时渲染静态错误文案，不做骨架屏系统
- HeroPicker 随机切换使用 CSS opacity 过渡（≤ 600ms，遵循 `--ease-out`），不做复杂转场

---

## 8. TypeScript Models

`types/photo.ts` —— 与后端正式响应 DTO（`docs/DATA_MODEL.md` §9.2 / §9.3 / §10）一一对齐，数据库结构由后端决定：

```typescript
/** 后端给出的固定规格衍生图 URL 集（对应 PhotoSummaryResponse.image） */
export interface PhotoImage {
  thumbnailUrl: string;        // 极小预览 / 未来缩略图用途
  cardUrl: string;             // Timeline Archive / Horizontal Stream
  displayUrl: string;          // Hero / Lightbox
}

/** 列表、归档、Stream、Lightbox 导航使用的精简照片对象（对应 PhotoSummaryResponse） */
export interface PhotoSummary {
  id: string;                  // UUID
  title: string;
  takenAt: string;             // ISO 8601 日期，例如 "2026-08-14"
  year: number;                // 后端派生字段
  month: number;               // 1–12，后端派生字段
  location: string | null;
  orientation: "landscape" | "portrait" | "square";
  aspectRatio: number;         // width / height，后端派生，用于防 CLS 与布局变体
  featured: boolean;
  image: PhotoImage;
}

/** 完整照片对象（对应 PhotoResponse；详情 / 未来 EXIF 展示） */
export interface Photo extends PhotoSummary {
  description: string | null;
  camera: string | null;
  lens: string | null;
  focalLength: string | null;  // 后端已格式化，例如 "35 mm"
  aperture: string | null;     // 后端已格式化，例如 "f/2.8"
  shutterSpeed: string | null; // 后端已格式化，例如 "1/250 s"
  iso: number | null;
}

/** 时间归档（对应 ArchiveResponse；Year → Month → Photos） */
export interface ArchiveMonth {
  month: number;               // 1–12
  label: string;               // 后端给出，稳定大写英文月名，例如 "AUGUST"
  photoCount: number;
  photos: PhotoSummary[];
}

export interface ArchiveYear {
  year: number;
  photoCount: number;          // "N FRAMES" 角标
  months: ArchiveMonth[];      // 12 → 1 降序
}

export interface ArchiveResponse {
  years: ArchiveYear[];        // 年份降序；空年 / 空月由后端省略
}

/** GET /api/v1/photos 的分页信封（对应 PhotoPageResponse） */
export interface PhotoPage {
  items: PhotoSummary[];
  page: number;                // zero-based
  size: number;
  totalElements: number;
  totalPages: number;
}
```

**设计说明：**

- `image` 是后端契约的核心形状：不同视觉场景消费不同 variant（使用约定见 §9.2），前端**不再使用单一 `imageUrl`**
- `visibility` / `cloudinaryPublicId` / 原始 URL **不在前端模型中**——它们是后端内部字段，公开响应不返回
- `year` / `month` / `orientation` / `aspectRatio` 均为后端派生响应字段，前端不读图片文件、不自行计算
- 月份 `label` 由后端提供（语义值仍是数字 `month`），前端不做格式化
- EXIF 字段以后端格式化字符串传输（如 `"f/2.8"`），前端直接展示，不做单位换算
- 空值显式建模为 `| null`，与后端"nullable 显式返回 JSON null"的约定一致
- V1 无 `tags` 字段（后端明确不在 V1 建模）；未来扩展时随后端契约一并加入

---

## 9. Image Strategy

### 9.1 加载优先级

| 区域 | 策略 |
|------|------|
| Hero | `priority` + preload，首屏 LCP 元素 |
| Photo Stream | 首屏可视区内图片正常加载，可视区外 `loading="lazy"` |
| Timeline Archive | 全部 `loading="lazy"`，滚动进入视口再加载 |

### 9.2 Variant 使用约定与技术方案

后端 DTO 的 `image` 提供三档固定规格（对齐 `docs/TECHNICAL_DESIGN.md` §8.1），前端按视觉场景选择对应 variant：

| Variant | 上限 | 前端使用场景 |
|---------|------|--------------|
| `thumbnailUrl` | ~480px | 极小预览 / 未来缩略图用途（V1 公开页面基本不使用） |
| `cardUrl` | ~1280px | Timeline Archive 卡片、Horizontal Photo Stream |
| `displayUrl` | ~2048px | Hero、Lightbox 大图 |

**技术方案：**

- 直接使用后端给出的 variant URL；**不使用 `next/image` 的自定义 loader 去构造任意 Cloudinary 变换**（与后端 Strict Transformations 契约冲突）。需要框架优化时以 `next/image` + `unoptimized` 或原生 `<img>` 承载，语义等价
- `sizes` 概念仍成立：CSS 按组件实际渲染宽度精确控制展示尺寸（如 Hero `45vw`、Archive wide 变体 `46vw`），variant 分辨率冗余由后端规格保证
- 每张图片**必须**预留宽高空间：优先使用 `aspect-ratio` CSS 属性 + `width: 100%`，由 `PhotoSummary.aspectRatio` 驱动，杜绝 CLS

**前端禁止事项（安全契约）：**

- 不得自行构造任意 Cloudinary transformation URL
- 不得获取 Cloudinary API Secret / 上传签名
- 不得获取原始图 URL
- 不得获取 `cloudinaryPublicId`

### 9.3 裁切原则（尊重原始构图）

| 场景 | object-fit | 理由 |
|------|-----------|------|
| Timeline Archive | `contain` 语义（即：容器按 `aspectRatio` 生成，图片完整填充） | 尊重原始构图，不裁切 |
| Lightbox | 完整显示，最大高度约束 | 摄影作品必须看全 |
| Hero | 允许 `cover` 艺术裁切 | Hero 是视觉位，可为构图服务 |
| Photo Stream | 允许容器定高 + `cover` | 轨道需要统一节奏感 |

**Archive 不裁切的实现方式：** 容器宽度由布局变体决定，容器高度 = 宽度 ÷ `aspectRatio`，图片 `width/height: 100%` 自然填满——没有裁切，也没有留白边。

### 9.4 图片保护对齐 PRD

- 前端只渲染后端给出的固定 variant（最大 display 约 2048px 宽），原图为 Cloudinary private delivery，不进入公开 DTO
- 安全边界由后端 Strict Transformations + 固定 `MediaUrlFactory` 保证；前端不构造 URL，自然无法越界取图
- 已展示的衍生图可被复制是既定前提，保护目标是原图私有与分辨率受限，而非假装浏览器能藏住已渲染资源

---

## 10. Timeline Layout Strategy

### 10.1 问题

Prototype 用 `archive-card--0/1/2/3` 序号循环 + margin 偏移实现杂志感，本质是改良版 `nth-child`——照片数量变化时节奏不可控，也无法感知横竖图。

### 10.2 Layout Variant 系统

定义 5 个显式变体，每个变体是一组宽度 + 偏移规则：

| Variant | 宽度（Desktop） | 偏移 | 适用 |
|---------|----------------|------|------|
| `feature` | `min(52vw, 700px)` | 无 | 每月第一张或横图主打 |
| `wide` | `min(46vw, 620px)` | 无 | 横图 |
| `portrait` | `min(24vw, 340px)` | `margin-top: +12vh` | 竖图，下沉错位 |
| `medium` | `min(30vw, 430px)` | `margin-top: -4vh; margin-left: auto` | 通用，右对齐上浮 |
| `offset` | `min(36vw, 480px)` | `margin-top: +6vh` | 打破节奏用 |

### 10.3 分配算法（`lib/utils/layoutVariant.ts`）

在服务端为每个月的照片数组分配变体，规则：

1. 第一张 → `feature`（若当月仅一张，同样 `feature`，呼应 PRD 的 Empty Collection 规则）
2. 其余按 `orientation` 初选：`landscape → wide`、`portrait → portrait`、`square → medium`
3. 相邻两张同变体时，后者降级为 `offset`，保证节奏变化
4. 每 4 张注入一个 `medium`（右对齐上浮），形成非对称锚点

**输出是纯数据：**

```typescript
interface LayoutAssignment {
  photo: PhotoSummary;
  variant: "feature" | "wide" | "portrait" | "medium" | "offset";
}
```

**可维护性：** 变体语义化、与 CSS 类一一对应（`PhotoCard.module.css` 中 `.feature / .wide / .portrait / .medium / .offset`）；调整视觉只需改变体表或算法常量，不碰组件结构。Mobile 下所有变体统一塌缩为全宽纵向流（见 §14）。

---

## 11. Photo Stream Interaction

### 11.1 架构

```text
PhotoStream.tsx          "use client" —— 交互外壳
└── children: PhotoStreamTrack (Server) —— 照片 HTML
```

外壳只持有一个 `<div ref>`，所有逻辑通过原生事件完成，照片列表零客户端 JS 成本。

### 11.2 交互矩阵

| 平台 | 输入 | 行为 |
|------|------|------|
| Desktop | 触控板横向滚动 | 原生 `scrollLeft`（不拦截 deltaX） |
| Desktop | 鼠标滚轮纵向 | `wheel` 事件中 `deltaY → scrollLeft`，`preventDefault` |
| Desktop | 鼠标拖拽 | pointerdown/move/up，`scrollLeft` 跟随位移 |
| Mobile | 触摸滑动 | 原生横向滚动（`overflow-x: auto` + `-webkit-overflow-scrolling`） |
| 任意 | 空闲 | `requestAnimationFrame` 以 ≤0.5px/帧自动漂移，到末尾回绕 |

### 11.3 用户优先原则

- 任何 wheel / pointerdown / touchstart → 立即停止漂移
- 停止交互约 2.5s 后恢复漂移
- 拖拽中不触发点击打开 Lightbox（位移阈值 >5px 判定为拖拽）

### 11.4 降级

- `prefers-reduced-motion: reduce` → 禁用自动漂移，仅保留手动滚动
- 滚动条视觉隐藏，但保留可滚动语义（`tabIndex` + 键盘左右键可作为未来增强，V1 非必须）

---

## 12. Lightbox Architecture

### 12.1 Server–Client 边界：PhotoLightboxTrigger

Server PhotoCard 不能绑定 `onClick`、不能使用 Client Context，因此点击打开 Lightbox 通过一个薄 Client 叶子组件完成：

```text
PhotoCard (Server)
   │  渲染 image / caption / metadata，hover 放大纯 CSS
   │
   └── PhotoLightboxTrigger ("use client")
             │  props：可序列化的最小数据（listPhotos 的 id 集 + 当前 index，
             │        或当前 PhotoSummary）
             │  onClick → 调用 LightboxProvider 的 open()
             ▼
       LightboxProvider ("use client", 挂在 Homepage 叶级)
             state: { photos: PhotoSummary[], index: number } | null
```

- **Trigger 只做最小必要交互：** 接收点击、转发给 Provider；不包含图片渲染、不做 hover、不做数据获取
- **PhotoCard / Timeline / Stream 保持 Server：** 不因点击行为把整段照片数据逻辑变成 Client Component；Trigger 是每张照片唯一的客户端 JS 成本（极薄）
- **Provider 状态模型：** `open(photos, index)` 携带**当前所在列表**（Stream 或某个月份组），Previous/Next 在该列表内循环；`null` 时不渲染 Lightbox DOM，零运行时成本
- 实现上 Trigger 也可渲染为卡片上方的透明 `<button>`（满足 §15 键盘可达与 focus 样式要求）

### 12.2 功能（对齐 PRD §11）

- Large Image（完整显示，不裁切）+ Title / Date / Location
- Previous / Next / Close

### 12.3 输入

| 平台 | 输入 |
|------|------|
| Desktop | ← / → 切换，Esc 关闭，点击遮罩关闭 |
| Mobile | 横向 swipe 切换，点 Close 关闭 |

### 12.4 滚动位置保持

- 打开时：`document.body` 加 `overflow: hidden`，**不修改 scrollTop**
- 关闭时：移除锁，浏览器自然停留在原滚动位置
- 不采用"记住位置再 scrollTo"方案，避免闪动

### 12.5 Focus 管理（Accessibility 要求）

- 打开：focus 移至 Lightbox 容器（`tabIndex={-1}`），focus trap 在内部
- 关闭：focus 归还到触发它的 `PhotoLightboxTrigger`
- `role="dialog"` + `aria-modal="true"` + `aria-label={photo.title}`

---

## 13. Styling Architecture

### 13.1 分层

```text
globals.css                  只放 Design Tokens + Reset + 基础排版
ComponentName.module.css     组件视觉全部在此
```

无 Tailwind，无 CSS-in-JS 运行时库。

### 13.2 globals.css Design Tokens

```css
:root {
  /* color */
  --color-bg: #f5f3ee;
  --color-bg-soft: #efede7;
  --color-ink: #1a1a18;
  --color-ink-muted: #6f6c65;
  --color-ink-faint: #a8a49b;
  --color-rule: #d9d5cc;

  /* typography */
  --font-serif: "Didot", "Bodoni MT", Georgia, serif;
  --font-sans: "Helvetica Neue", Helvetica, Arial, sans-serif;
  --font-mono: "SF Mono", "Menlo", monospace;

  /* spacing */
  --space-page-x: 48px;
  --space-section-y: 14vh;
  --space-gap-grid: clamp(28px, 4vw, 64px);

  /* motion */
  --ease-out: cubic-bezier(.22, .61, .36, 1);
  --duration-slow: 1s;
  --duration-med: .5s;

  /* scale */
  --hover-scale: 1.045;
}
```

tokens 直接继承 Prototype 已验证的色板与动效曲线。

### 13.3 规则

- 组件样式只能用 Module 内的类 + `var(--*)`，不定义局部色值
- hover 放大 / 兄弟降透明等交互效果全部 CSS 实现
- `@media (prefers-reduced-motion: reduce)` 在 globals 统一关闭非必要过渡

---

## 14. Responsive Design

### 14.1 断点（克制，仅 3 档）

| 名称 | 范围 | 策略 |
|------|------|------|
| Mobile | `< 768px` | Vertical Editorial：所有布局变体塌缩为全宽纵向流，图片保持原 aspect ratio |
| Tablet | `768px – 1024px` | Floating 布局保留，变体宽度按 vw 自然收缩，偏移量减半 |
| Desktop | `> 1024px` | 完整 Floating Editorial Layout |

### 14.2 要点

- 不复刻桌面错位到手机端；Mobile 的美感来自**图片原始比例 + 稳定留白节奏**
- `--space-page-x` 在 Mobile 降为 `24px`
- Hero 在 Mobile 从双列 grid 塌缩为单列（标题 → 图片）
- Photo Stream 三端都保持横向，天然适配触摸

---

## 15. Accessibility

| 项目 | 方案 |
|------|------|
| alt text | `PhotoSummary.title`（无 title 时用 `"Untitled photograph, {location}, {year}"`） |
| 语义 HTML | header / main / section / figure / figcaption / footer / nav |
| 键盘导航 | 全站 Tab 可达；`PhotoLightboxTrigger` 以 `<button>` 语义承载（见 §12.1），原生支持 Enter / Space 触发 |
| focus state | 保留 `:focus-visible` 轮廓（定制为细线 ink 色，不用 outline: none） |
| Lightbox | focus trap + Esc + 焦点归还（见 §12.5） |
| reduced motion | 关闭自动漂移、入场动画、hover 放大（保留透明度提示） |
| 对比度 | `--color-ink` on `--color-bg` ≈ 14:1；muted 文本 ≥ 4.5:1 |
| 标题层级 | 每页唯一 h1（Hero），年号为 h2，月份为 h3 |

不为视觉极简牺牲以上任何一项。

---

## 16. Performance

### 16.1 图片（最大收益项）

- Hero `priority` 预加载（display variant）；其余全部 lazy（见 §9.1）
- `aspect-ratio` 预留空间，目标 CLS = 0
- 按场景选 variant：移动/卡片场景只下载 card（≤1280px），不下载 display（≤2048px）

### 16.2 JavaScript 最小化

- Server Components 为默认；客户端 JS 只来自 `HeroPicker` + `PhotoStream` 外壳 + `PhotoLightboxTrigger` × N + Lightbox（合计目标 < 15KB gzip，不含框架）
- App Router 天然 route-level code splitting：`/about` 几乎零 JS
- 无客户端数据请求库、无状态管理库、无动画框架

### 16.3 字体

- 通过 `next/font` 自托管一款展示衬线字体（Didot 为系统字体时零加载成本，备选自托管衬线用于非 macOS 设备）
- `font-display: swap`，正文字体使用系统栈零成本
- 只加载实际使用的字重

### 16.4 渲染

- ISR 静态生成，CDN 命中时 TTFB 极低
- 动画只用 `transform` / `opacity`（合成层属性），无 layout thrash

---

## 17. SEO

全部通过 Next.js Metadata API 实现，不引入额外 SEO 系统：

| 项目 | 方案 |
|------|------|
| title / description | `app/layout.tsx` 默认 metadata + 页面级覆盖 |
| Open Graph | metadata `openGraph` 字段；默认 og 图，未来可动态用 featured 照片 |
| Twitter Card | metadata `twitter` 字段，`summary_large_image` |
| Canonical | metadata `metadataBase` + `alternates.canonical` |
| Sitemap | `app/sitemap.ts`（V1 仅 `/` 与 `/about`，未来含归档锚点策略） |
| robots.txt | `app/robots.ts`，V1 全允许；`/admin` 未来 disallow |

---

## 18. Prototype Migration Strategy

### 18.1 保留的视觉思想（直接从 Prototype 继承）

| 元素 | 继承方式 |
|------|----------|
| Hero typography（超大衬线、RICK 错位缩进） | tokens + Hero.module.css 直接复刻 |
| 色彩系统（米白 bg / 三级墨色 / 细分隔线） | 已进入 globals.css tokens |
| 动效曲线 `cubic-bezier(.22,.61,.36,1)` + 1s 时长 | token `--ease-out` |
| hover scale 1.045 + 兄弟降 opacity | PhotoCard / Stream 的 CSS |
| Photo Stream 漂移节奏（≤0.5px/帧、2.5s 恢复） | §11 交互参数 |
| Archive 错位编辑思想 | 抽象为 §10 Layout Variant 系统 |
| 图片轻饱和处理 `saturate(.92) contrast(1.02)` | PhotoCard 基础样式 |

### 18.2 重新工程化的部分（不复制 Prototype 代码）

| 部分 | Prototype 做法 | 正式版做法 |
|------|----------------|-----------|
| Data loading | `mockPhotos.js` 全局常量 | Server Component 内 fetch REST API + ISR |
| DOM 渲染 | `innerHTML` 字符串拼接 | React 组件 + TypeScript 类型约束 |
| Archive 分组 | 客户端运行时分组 | 服务端分组（优先消费 `ArchiveResponse`，降级 `groupByTime.ts`） |
| 布局变体 | `archive-card--0/1/2/3` 序号循环 | 语义化 Layout Variant 分配算法（§10） |
| 图片 | 原生 `<img>`，无尺寸预留 | 后端固定 variant URL + aspect-ratio 预留（§9） |
| Responsive | 单一 860px 断点 | 3 档断点策略（§14） |
| Accessibility | 未考虑 | §15 全量方案 |
| Lightbox | 不存在 | §12 新架构（Server PhotoCard + Client Trigger） |
| Hero 随机 | 客户端 `Math.random()`（有闪烁、无缓存意识） | 服务端确定性 fallback 直出 + `HeroPicker` 水合后随机（client-side presentation randomness，与 ISR 解耦，§7） |

### 18.3 Prototype 的处理

- `prototype/` **保留不删**，作为视觉回归对照（视觉走查时双开对比）
- 正式开发中任何视觉争议以 Prototype 为准绳
- 不将 Prototype 的 JS 复制进 `frontend/`

---

## 19. Future Admin Considerations

本轮**不设计** Admin UI、不实现登录，仅在前端架构上预留：

- **路由预留：** `/admin` 段未来挂在 `app/admin/` 下，与公开站共享 layout 体系但使用独立 layout 文件（公开站 Header/Footer 不适用于 Admin）
- **SEO 隔离：** `robots.ts` 中对 `/admin` disallow
- **API 预留：** 后端已保留 `/api/v1/admin/*`（session / photos / uploads signature，见 `docs/TECHNICAL_DESIGN.md` §6.2），届时浏览器请求使用 `credentials: "include"` + CSRF 握手，V1 前端不写任何相关代码
- **类型预留：** `visibility` / `cloudinaryPublicId` 由后端 `AdminPhotoResponse` 管理，公开类型中不存在；Admin 类型在未来单独定义，不污染公开 `types/photo.ts`
- **组件隔离：** Admin 组件放 `components/admin/`，不与展示组件混用——Admin 允许是"实用 UI"，公开站必须 `Photography First`
- **认证边界：** 认证由后端 Session 方案决定，前端届时在 `app/admin/layout.tsx` 做守卫，V1 不预设方案

---

## 附录：与 PRD 的对照

| PRD 章节 | 本文档落点 |
|----------|-----------|
| §6 Homepage 结构 | §4 Routing、§5 Component Architecture |
| §7 Horizontal Photo Stream | §11 Photo Stream Interaction |
| §8–9 Timeline Archive / Layout | §10 Timeline Layout Strategy |
| §10 Image Hover | §13 Styling（CSS 实现） |
| §11 Photo Detail (Lightbox) | §12 Lightbox Architecture |
| §13 Responsive | §14 Responsive Design |
| §14 Content Model | §8 TypeScript Models |
| §17 Performance Goal | §9 Image Strategy、§16 Performance |
| §18 Privacy & Image Protection | §9.4 |
| §21 Future Admin | §19 Future Admin Considerations |
