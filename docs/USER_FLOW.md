# MyGallery — User Flow

**Version:** 0.1
**Status:** Draft

---

## 1. Main Visitor Flow

```text
User opens website
        │
        ▼
Homepage
        │
        ├── Random Hero Photo
        ├── Horizontal Photo Stream
        └── Explore Archive
        │
        ▼
Timeline Archive
        │
        ├── 2026
        │    ├── August
        │    ├── July
        │    └── June
        │
        ├── 2025
        └── 2024
        │
        ▼
Select Photo
        │
        ▼
Lightbox
        │
        ├── Previous
        ├── Next
        ├── View Metadata
        └── Close
             │
             ▼
Return to previous Archive position
```

---

## 2. Homepage Flow

```text
Website Load
      │
      ▼
Select random Featured Photo
      │
      ▼
Display Hero
      │
      ▼
User Scroll
      │
      ▼
Selected Frames
      │
      ├── Horizontal Scroll
      ├── Drag
      └── Click Photo
             │
             ▼
          Lightbox
```

**继续向下：**

```text
Selected Frames
      │
      ▼
Timeline Archive
```

---

## 3. Archive Flow

```text
ARCHIVE
   │
   ▼
Newest Year
2026
   │
   ├── AUGUST
   │    ├── Photo
   │    ├── Photo
   │    └── Photo
   │
   ├── JULY
   └── JUNE
   │
   ▼
2025
   │
   ▼
2024
```

**默认排序：** `Newest → Oldest`

- 年份：`2026 → 2025 → 2024`
- 月份：`December → January`

---

## 4. Timeline Navigation

未来增强功能：

```text
2026
2025
2024
2023
```

点击年份后平滑滚动至对应位置。

> 该功能不是第一版必须功能。

---

## 5. Photo Hover Flow

```text
Cursor enters Photo
        │
        ▼
Photo slightly scales
        │
        ├── Title appears
        └── Location appears
        │
        ▼
Click
        │
        ▼
Open Lightbox
```

---

## 6. Lightbox Flow

```text
Open Photo
     │
     ▼
Large Image
     │
     ├── Title
     ├── Location
     ├── Date
     ├── Previous
     ├── Next
     └── Close
```

**Desktop：**

- Left Arrow
- Right Arrow
- ESC

**Mobile：**

- Swipe Left
- Swipe Right
- Tap Close

---

## 7. About Flow

```text
Homepage
   │
   ▼
ABOUT
   │
   ├── Short Biography
   ├── Photography Statement
   ├── Location
   └── Social / Contact
```

用户可以随时返回：

- INDEX
- ARCHIVE

---

## 8. Navigation

```text
TOM RICK

INDEX
ARCHIVE
ABOUT
```

| 导航项 | 行为 |
|--------|------|
| INDEX | 回到首页顶部 |
| ARCHIVE | 进入 / 滚动到摄影时间档案 |
| ABOUT | 进入个人简介 |

---

## 9. Mobile Flow

信息结构保持一致：

```text
Hero
 ↓
Selected Frames
 ↓
Archive
 ↓
Photo Viewer
 ↓
About
```

**区别：**

| 平台 | 布局 |
|------|------|
| Desktop | Floating Editorial Layout |
| Mobile | Vertical Editorial Layout |

---

## 10. Future Admin Flow

未来私有后台：

```text
Private Admin URL
       │
       ▼
Authentication
       │
       ▼
Dashboard
       │
       ├── Upload Photo
       ├── Edit Photo
       ├── Delete Photo
       ├── Featured
       └── Visibility
```

**Upload Flow：**

```text
Select Photo
      │
      ▼
Upload
      │
      ▼
Read Metadata
      │
      ▼
Edit Metadata
      │
      ▼
Publish
      │
      ▼
Photo automatically appears
in the correct Timeline position
```

---

## 11. Core User Experience

最终访问体验尽可能保持为：

```text
Open
 ↓
Look
 ↓
Scroll
 ↓
Discover
 ↓
Open Photo
 ↓
Continue
```

- 用户不需要学习网站怎么使用
- UI 应该尽可能退到背景
- 照片始终是交互中心
