# Common Diagram Spring Boot Starter

通用圖表模組 — 支援多種圖表類型（簽名板、家系圖、自訂），JSON 存儲 + 版本歷史 + 附件管理

---

## 加入後你的專案自動獲得

| 功能 | 加入前 | 加入後 |
|------|--------|--------|
| 圖表存儲 | 自己寫 JSON 存 DB | `diagramService.save()` 一行搞定 |
| 多類型支援 | 每種圖表各寫一個模組 | `diagramType` 一個欄位區分所有類型 |
| 版本歷史 | 覆蓋更新，舊的不見 | 舊版整筆軟刪除，完整歷史可追溯 |
| 版本還原 | 自己寫 | `restoreVersion(id)` 一行搞定 |
| 附件管理 | 自己管理圖片 | 自動整合 attachment starter |
| 審計欄位 | 手動記錄 | 自動 createdBy / createdDate / lastModifiedBy / lastModifiedDate |

---

## 快速開始

### 1. 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-diagram-spring-boot-starter</artifactId>
</dependency>
```

> 需同時引入 `common-attachment-spring-boot-starter`（圖片由附件模組管理）。

### 2. 使用

```java
// 儲存簽名
diagramService.save(
    new DiagramSaveRequest("SIGNATURE", "ORDER", 123L, null, canvasJson),
    imageFile
);

// 儲存家系圖（帶版本名稱）
diagramService.save(
    new DiagramSaveRequest("FAMILY", "CASE", 456L, "第三次修改", familyJson),
    screenshotFile
);

// 查詢
DiagramResponse active = diagramService.findByOwner("SIGNATURE", "ORDER", 123L);

// 版本歷史（含已刪除的舊版）
List<DiagramResponse> history = diagramService.getHistory("FAMILY", "CASE", 456L);

// 還原舊版本
diagramService.restoreVersion(oldVersionId);
```

---

## 功能總覽

- **多類型支援** — `diagramType` 區分：SIGNATURE（簽名板）、FAMILY（家系圖）、任意自訂
- **JSON 存儲** — Canvas / D3 / 任何 JSON 格式的圖表資料
- **版本歷史** — 每次更新舊版整筆軟刪除，content + 附件都保留
- **版本還原** — `restoreVersion(id)` 將指定版本還原為 active
- **版本命名** — `name` 欄位，方便識別歷史版本
- **附件整合** — 圖片/截圖自動走 attachment starter，ownerType 前綴 `DIAGRAM_{type}_`
- **軟刪除** — 繼承 BaseEntity，同一 owner 可有多筆（active + 歷史）
- **條件啟用** — `@ConditionalOnBean(AttachmentService)` 確保附件模組存在

---

## 核心 API

### DiagramService

| 方法 | 回傳 | 說明 |
|------|------|------|
| `save(request, image)` | `DiagramResponse` | 儲存（舊版自動軟刪除 + 新建） |
| `findByOwner(type, ownerType, ownerId)` | `DiagramResponse` | 查詢 active 版本 |
| `getHistory(type, ownerType, ownerId)` | `List<DiagramResponse>` | 所有版本（含 deleted），按建立時間倒序 |
| `restoreVersion(id)` | `DiagramResponse` | 還原指定版本（軟刪除 current + restore target） |
| `delete(type, ownerType, ownerId)` | `void` | 軟刪除 active 版本 |
| `getAttachments(type, ownerType, ownerId)` | `List<AttachmentUploadResponse>` | 查詢附件列表 |

### REST API（opt-in）

需設定 `common.diagram.web.enabled=true`：

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/diagrams` | 儲存（multipart：diagramType + ownerType + ownerId + json + name + image） |
| GET | `/diagrams?diagramType=SIGNATURE&ownerType=ORDER&ownerId=1` | 查詢 active |
| GET | `/diagrams/history?diagramType=FAMILY&ownerType=CASE&ownerId=1` | 版本歷史 |
| POST | `/diagrams/{id}/restore` | 還原版本 |
| DELETE | `/diagrams?diagramType=SIGNATURE&ownerType=ORDER&ownerId=1` | 軟刪除 |

### DTO

```java
// 請求
record DiagramSaveRequest(
    String diagramType,    // SIGNATURE / FAMILY / 自訂
    String ownerType,
    Long ownerId,
    String name,           // 版本名稱（可選）
    String json            // 圖表 JSON
)

// 回應
record DiagramResponse(
    Long id,
    String diagramType,
    String ownerType,
    Long ownerId,
    String name,
    String content,
    Long attachmentId,
    boolean deleted,       // 用於歷史查詢
    String createdBy,
    LocalDateTime createdDate,
    String lastModifiedBy,
    LocalDateTime lastModifiedDate
)
```

### Entity

```
DiagramEntity extends BaseEntity
├── (繼承) deleted, version, createdDate, lastModifiedDate, createdBy, lastModifiedBy
├── id (Long, IDENTITY)
├── diagramType (String)  ← SIGNATURE / FAMILY / 自訂
├── ownerType (String)
├── ownerId (Long)
├── name (String)         ← 版本名稱
├── content (NVARCHAR(MAX)) ← 圖表 JSON
└── attachmentId (Long)   ← 關聯附件 ID
```

---

## 使用情境

### 簽名板

```java
// diagramType = "SIGNATURE"
diagramService.save(
    new DiagramSaveRequest("SIGNATURE", "CONTRACT", 1L, null, canvasJson),
    pngScreenshot
);
```

### 家系圖

```java
// diagramType = "FAMILY"
diagramService.save(
    new DiagramSaveRequest("FAMILY", "PATIENT", 100L, "2026-03 更新", familyTreeJson),
    familyTreeScreenshot
);

// 查歷史
List<DiagramResponse> history = diagramService.getHistory("FAMILY", "PATIENT", 100L);

// 還原到某個版本
diagramService.restoreVersion(history.get(2).id());
```

### 自訂圖表

```java
// diagramType = 任意字串
diagramService.save(
    new DiagramSaveRequest("FLOWCHART", "PROJECT", 50L, "v2", flowchartJson),
    null  // 不一定要有圖片
);
```

---

## 配置

```yaml
common:
  diagram:
    web:
      enabled: false       # 是否自動註冊 REST controller（預設 false）
```

---

## 設計決策

| 決策 | 原因 |
|------|------|
| 泛化為 `diagramType` 而非各做一個 starter | 簽名板和家系圖 90% 邏輯重複，差別只是 JSON 內容 |
| 軟刪除舊版 + 新建 | 保留完整版本歷史，不覆蓋（簽名板曾經因覆蓋導致舊簽名不見） |
| `restoreVersion` 先軟刪除 active 再 restore target | 確保同一 owner 同時只有一筆 active |
| 附件 ownerType 用 `DIAGRAM_{type}_{ownerType}` | 不同圖表類型的附件不會混在一起 |
| `name` 欄位 | 家系圖需要版本命名（如「2026-03 更新」），簽名板可留空 |
| Controller 預設關閉 | 避免不需要 REST API 的專案意外暴露端點 |

---

## 依賴關係

```
common-diagram-spring-boot-starter
├── common-jpa-spring-boot-starter          ← BaseEntity + SoftDeleteRepository
├── common-attachment-spring-boot-starter   ← 圖片附件管理
├── spring-boot-starter-data-jpa            ← (provided)
└── spring-boot-starter-web                 ← (provided)
```

---

## 專案結構

```
business/common-signature-spring-boot-starter/   ← 目錄名暫保留
├── pom.xml                                       (artifactId = common-diagram-spring-boot-starter)
└── src/
    ├── main/java/com/company/common/diagram/
    │   ├── config/
    │   │   ├── DiagramAutoConfiguration.java     # @AutoConfiguration
    │   │   └── DiagramProperties.java            # common.diagram.*
    │   ├── dto/
    │   │   ├── DiagramSaveRequest.java           # record
    │   │   └── DiagramResponse.java              # record（含 deleted 欄位）
    │   ├── entity/
    │   │   └── DiagramEntity.java                # diagramType + ownerType + ownerId + name + content
    │   ├── repository/
    │   │   └── DiagramRepository.java            # findActiveDiagram / findAllVersions / findActiveByOwner
    │   ├── service/
    │   │   └── DiagramService.java               # save / findByOwner / getHistory / restoreVersion / delete
    │   └── web/
    │       └── DiagramController.java            # REST API（opt-in）
    ├── main/resources/META-INF/spring/
    │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/java/.../DiagramServiceTest.java     # 16 個測試
```

---

## 技術規格

| 項目 | 值 |
|------|-----|
| Java | 21 |
| Spring Boot | 4.0.3 |
| 資料庫表 | `DIAGRAM` |
| 主鍵策略 | `IDENTITY` |
| JSON 欄位 | `NVARCHAR(MAX)` |
| 索引 | `DIAGRAM_TYPE + OWNER_TYPE + OWNER_ID` |
| 軟刪除 | 繼承 `BaseEntity.deleted` |
| 測試 | 16 個（Mockito 單元測試） |

---

## 版本

### 1.0.0

- 初始版本（common-signature-starter）：Canvas JSON 簽名、REST API、附件整合

### 1.1.0

- 泛化為 common-diagram-starter
- 新增 `diagramType`（SIGNATURE / FAMILY / 自訂）
- 新增 `name`（版本名稱）
- 新增 `getHistory()`（含 deleted 歷史）
- 新增 `restoreVersion()`（版本還原）
- 重簽/重畫改為軟刪除 + 新建（不再覆蓋）
- 16 個單元測試
