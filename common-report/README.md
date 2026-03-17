# common-report-spring-boot-starter

Spring Boot 4.0 報表產製 Starter，提供多引擎支援（EasyExcel / JasperReports / xDocReport）、非同步產製、審計記錄，加依賴即可使用。

## 技術棧

- Spring Boot 4.0.3 / Java 21
- EasyExcel 4.0.1 + Apache POI 5.3.0
- Spring Data JPA（審計記錄）
- Spring Async（非同步產製）
- SpringDoc OpenAPI 3（Swagger UI）

## 模組結構

```
common-report/
├── common-report-core/                  # 核心：Entity / SPI / Service（純 library，無 web 依賴）
│   ├── entity/                          ReportItem, ReportLog, ReportLogBlob
│   ├── enums/                           ReportEngineType, ReportStatus, OutputFormat
│   ├── repository/                      JPA Repository
│   ├── service/                         ReportService, ReportLogService, ReportAsyncService
│   └── spi/                             ReportEngine(SPI), ReportContext, ReportResult
│
├── common-report-engine-easyexcel/      # EasyExcel 引擎（可選）
│   ├── EasyExcelReportEngine.java       implements ReportEngine
│   ├── CustomMergeStrategy.java         相同值自動垂直合併
│   └── EasyExcelAutoConfiguration.java  @ConditionalOnClass 自動裝配
│
├── common-report-engine-xdocreport/     # xDocReport 引擎（可選）
│   ├── XDocReportEngine.java            implements ReportEngine
│   └── XDocReportAutoConfiguration.java @ConditionalOnClass 自動裝配
│
├── common-report-autoconfigure/         # AutoConfiguration + Controller + DTO + Properties
│   ├── ReportAutoConfiguration.java     核心 Bean 註冊
│   ├── ReportAsyncConfiguration.java    @EnableAsync + ThreadPool
│   ├── ReportProperties.java            配置項
│   ├── ReportController.java            REST API
│   └── dto/                             ReportGenerateRequest, ReportStatusResponse
│
├── common-report-spring-boot-starter/   # Starter 空殼（只拉依賴）
│
└── common-report-test/                  # 整合測試（39 個，TDD Phase 1-6）
```

## 快速開始

### 1. 加入依賴

```xml
<!-- Starter 核心 -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-spring-boot-starter</artifactId>
</dependency>

<!-- 選擇需要的引擎（至少一個） -->
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-easyexcel</artifactId>
</dependency>
```

### 2. 零配置即可用

引入依賴後，以下功能自動生效：

- `ReportService` — 報表產製（依引擎類型自動派發）
- `ReportLogService` — 審計記錄（自動記錄每次產製）
- `ReportAsyncService` — 非同步產製
- `ReportController` — REST API（產製 / 狀態查詢 / 下載）

### 3. 在程式碼中使用

```java
@RestController
public class MyReportController {

    private final ReportService reportService;

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        // 1. 準備資料
        List<EmployeeData> data = employeeService.findAll();

        // 2. 建立 Context
        ReportContext context = ReportContext.builder()
                .engineType(ReportEngineType.EASYEXCEL)
                .outputFormat(OutputFormat.XLSX)
                .fileName("employees.xlsx")
                .data(data)
                .build();

        // 3. 產製
        ReportResult result = reportService.generate(context);

        // 4. 回傳檔案
        response.setContentType(result.getContentType());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + result.getFileName() + "\"");
        response.getOutputStream().write(result.getContent());
    }
}
```

## REST API

### 內建端點（由 ReportController 提供）

| 方法 | 路徑 | 說明 |
|------|------|------|
| POST | `/api/reports/generate` | 同步產製（直接回傳檔案） |
| POST | `/api/reports/generate-async` | 非同步產製（回傳 UUID） |
| GET | `/api/reports/status/{uuid}` | 查詢非同步狀態 |
| GET | `/api/reports/download/{uuid}` | 下載已完成的報表 |
| GET | `/api/reports/engines` | 列出可用引擎 |

### 非同步產製流程

```
1. POST /api/reports/generate-async
   → { "uuid": "abc-123", "status": "PENDING" }

2. GET /api/reports/status/abc-123
   → { "status": "PROCESSING" }

3. GET /api/reports/status/abc-123
   → { "status": "COMPLETED", "fileName": "report.xlsx" }

4. GET /api/reports/download/abc-123
   → 下載檔案
```

## SPI 擴展

### ReportEngine 介面

```java
public interface ReportEngine {
    ReportEngineType getType();
    ReportResult generate(ReportContext context);
    boolean supports(OutputFormat format);
}
```

要自訂引擎，實作此介面並註冊為 Spring Bean：

```java
@Component
public class MyCustomEngine implements ReportEngine {

    @Override
    public ReportEngineType getType() {
        return ReportEngineType.EXPORT; // 或自訂類型
    }

    @Override
    public ReportResult generate(ReportContext context) {
        // 自訂產製邏輯
    }

    @Override
    public boolean supports(OutputFormat format) {
        return format == OutputFormat.CSV;
    }
}
```

`ReportService` 會自動發現並路由到此引擎。

## 引擎對照

| 引擎模組 | ReportEngineType | 支援格式 | 適合場景 |
|---------|------------------|---------|---------|
| `common-report-engine-easyexcel` | `EASYEXCEL` | XLSX, XLS, CSV | 資料匯出、範本填充 |
| `common-report-engine-xdocreport` | `XDOCREPORT` | DOCX, ODT, PDF | Word 套表（合約、公文） |
| `common-report-engine-jasper` | `JASPER` | PDF, XLS, HTML | 複雜版面報表（待確認需求） |
| `common-report-engine-export` | `EXPORT` | XLSX, CSV, ODS, XML | 簡易列表匯出（待確認需求） |

## 資料庫 Entity

| Entity | Table | 說明 |
|--------|-------|------|
| `ReportItem` | REPORT_ITEM | 報表定義（名稱、範本路徑、引擎類型） |
| `ReportLog` | REPORT_LOG | 產製記錄（UUID、狀態、開始/結束時間） |
| `ReportLogBlob` | REPORT_LOG_BLOB | 產製檔案（BLOB 儲存） |

### 狀態機

```
PENDING → PROCESSING → COMPLETED
                    ↘ FAILED

終態（COMPLETED / FAILED）不可再轉移。
```

## 配置項

所有配置前綴：`common.report`

```yaml
common:
  report:
    enabled: true                    # 總開關（預設 true）

    storage:
      type: database                 # database | filesystem
      path: /tmp/reports             # filesystem 時的儲存路徑

    async:
      enabled: true                  # 啟用非同步產製
      core-pool-size: 2              # 核心執行緒數
      max-pool-size: 5               # 最大執行緒數
      queue-capacity: 100            # 佇列容量

    cleanup:
      enabled: false                 # 啟用定期清理舊記錄
      retention-days: 90             # 保留天數
```

## EasyExcel 引擎功能

### 資料寫入模式

直接傳入 `List<?>` 資料，EasyExcel 自動產生 Excel：

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .fileName("users.xlsx")
        .data(userList)  // List<UserExcelData>
        .build();
```

DTO 使用 EasyExcel 註解：

```java
public class UserExcelData {
    @ExcelProperty("姓名")
    @ColumnWidth(20)
    private String name;

    @ExcelProperty("Email")
    @ColumnWidth(30)
    private String email;
}
```

### 範本填充模式

提供 `.xlsx` 範本，用變數填充：

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.EASYEXCEL)
        .outputFormat(OutputFormat.XLSX)
        .templatePath("templates/monthly-report.xlsx")
        .parameter("title", "2026年3月報表")
        .parameter("date", LocalDate.now().toString())
        .data(detailList)
        .build();
```

### CustomMergeStrategy

相同值的儲存格自動垂直合併，適合分組報表：

```
| 部門   | 姓名   | 薪資   |
|--------|--------|--------|
| 研發部 | 王小明 | 65,000 |
|        | 李小華 | 58,000 |  ← 「研發部」自動合併
|        | 劉建宏 | 72,000 |
| 業務部 | 陳大文 | 70,000 |
|        | 林志偉 | 62,000 |  ← 「業務部」自動合併
```

## xDocReport 引擎功能

用 Word 範本（`.docx`）+ Velocity 變數替換，產出 Word 或 PDF。適合合約、公文、通知書。

### 引入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-report-engine-xdocreport</artifactId>
</dependency>
```

### Word 範本

建立 `.docx` 範本，用 Velocity 語法寫變數：

```
合約書

甲方：$clientName
日期：$contractDate

明細：
#foreach($item in $items)
  - $item.name：$item.amount 元
#end
```

### 使用方式

```java
ReportContext context = ReportContext.builder()
        .engineType(ReportEngineType.XDOCREPORT)
        .outputFormat(OutputFormat.DOCX)   // 或 OutputFormat.PDF
        .templatePath("templates/contract.docx")
        .parameter("clientName", "王小明")
        .parameter("contractDate", "2026-03-17")
        .data(itemList)                    // 範本裡用 $items 存取
        .fileName("合約書.docx")
        .build();

ReportResult result = reportService.generate(context);
```

### 支援格式

| OutputFormat | 說明 |
|-------------|------|
| `DOCX` | 直接輸出 Word（不轉換） |
| `PDF` | Word → PDF（需要 xdocreport converter） |
| `ODT` | OpenDocument Text |

## 注意事項

### @EnableJpaRepositories

由於 `@EnableJpaRepositories` 是覆蓋式的，如果使用方自己也有 JPA Repository，需要在 `@SpringBootApplication` 上加：

```java
@SpringBootApplication
@EnableJpaRepositories(basePackages = "your.app.repository")
public class MyApplication { }
```

Report starter 的 entity/repository 會由 `ReportAutoConfiguration` 自動掃描，不需要手動加。

### 檔案下載與 GlobalResponseAdvice

如果同時使用 `common-response-spring-boot-starter`，檔案下載端點需要用 `void` + `HttpServletResponse` 寫出，避免 `GlobalResponseAdvice` 把 `byte[]` 包裝成 `ApiResponse`：

```java
// 正確 — 用 HttpServletResponse 直接寫出
@GetMapping("/download")
public void download(HttpServletResponse response) { ... }

// 錯誤 — 會被 GlobalResponseAdvice 包裝導致 ClassCastException
@GetMapping("/download")
public ResponseEntity<byte[]> download() { ... }
```

或在 `common.response.exclude-paths` 排除下載路徑：

```yaml
common:
  response:
    exclude-paths:
      - /api/reports/download/**
```

## 測試

39 個整合測試，TDD 規格文件風格：

| Phase | 測試內容 | 數量 |
|-------|---------|------|
| 1 | Entity / Repository | 7 |
| 2 | ReportService（引擎派發 + 驗證） | 6 |
| 3 | ReportLogService（狀態機 + 審計） | 10 |
| 4 | EasyExcelReportEngine | 7 |
| 5 | ReportController（HTTP API） | 4 |
| 6 | AutoConfiguration（Bean 載入） | 5 |

```bash
cd common-report
mvn test -pl common-report-test
```

## 設計決策

| 決策 | 理由 |
|------|------|
| core 不依賴 web | 可在 batch job 中使用，不強制帶入 web 層 |
| SPI + 策略模式 | 引擎可插拔，加依賴自動註冊 |
| Builder 模式 (ReportContext) | 參數多且可選，Builder 比 constructor 清晰 |
| @Version 樂觀鎖 | 非同步場景防止併發修改 ReportLog |
| 路徑遍歷保護 | templatePath 禁止 `..` 和絕對路徑 |
| 狀態機驗證 | 終態不可再轉移，防止資料不一致 |
