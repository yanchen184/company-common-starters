# common-security-auth-xca

台灣組織及團體憑證（XCA）認證模組 — 透過 `CertProvider` SPI 自動整合至 care-security 憑證認證流程。

---

## 功能總覽

- **組織代碼提取** — 從 X.509 憑證的 OID `2.16.886.1.100.2.102` 提取組織代碼
- **自動路由** — CertFactory 根據 Issuer DN（「組織及團體憑證管理中心」）自動選擇此 Provider
- **共用認證流程** — 挑戰碼、PKCS#7 驗簽、憑證鏈驗證、OCSP/CRL 檢查皆由 cert-core + auth-moica 共用元件處理
- **零配置整合** — 加依賴 + 啟用即生效

---

## 啟用方式

### 1. 加入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-auth-xca</artifactId>
</dependency>
```

### 2. 設定 application.yml

```yaml
care:
  security:
    xca:
      enabled: true
```

啟用後自動註冊 `XcaCertProvider`，CertFactory 會自動發現並路由 XCA 憑證。

---

## 架構

```
使用者插入組織及團體憑證 → 前端簽章
    ↓
POST /api/auth/cert/login（共用端點）
    ↓
Pkcs7Verifier 驗簽（cert-core 共用）
    ↓
CertFactory.identify() → 偵測 Issuer DN → 路由至 XcaCertProvider
    ↓
XcaCertProvider.extractId() → 提取組織代碼
    ↓
CertVerifier.fullVerify() → 憑證鏈 + OCSP + CRL（cert-core 共用）
    ↓
CitizenCertUserSyncService → 同步/建立使用者（auth-moica 共用）
    ↓
AuthService.completeCertLogin() → 簽發 JWT
```

---

## 核心類別

| 類別 | 說明 |
|------|------|
| `XcaCertProvider` | 實作 `CertProvider` SPI，提取 OID `2.16.886.1.100.2.102` 的組織代碼 |
| `XcaAutoConfiguration` | Spring Boot 自動配置，條件：`care.security.xca.enabled=true` |

---

## 配置項

| 配置 | 預設值 | 說明 |
|------|--------|------|
| `care.security.xca.enabled` | `false` | 是否啟用 XCA 認證 |

> 憑證驗證相關配置（OCSP、CRL、中繼 CA 路徑等）統一由 `care.security.citizen-cert.*` 管理。

---

## 依賴關係

```
common-security-auth-xca
├── common-security-core        ← CareSecurityProperties
└── common-security-cert-core   ← CertProvider SPI、CertType、CertExtensionUtils
```

---

## GCA 與 XCA 的 OID 共用說明

GCA 和 XCA 共用同一個 OID `2.16.886.1.100.2.102`，區分方式是 **Issuer DN**：

| 模組 | Issuer DN 關鍵字 |
|------|-----------------|
| GCA | 「政府憑證管理中心」 |
| XCA | 「組織及團體憑證管理中心」 |

`CertType.fromIssuer()` 會根據 Issuer DN 自動判斷，CertProvider 層面不需要額外處理。

---

## 與其他憑證模組的關係

| 模組 | 憑證類型 | 識別碼 | OID |
|------|---------|--------|-----|
| auth-moica | 自然人憑證 | 身分證後4碼 | 2.16.886.1.100.2.51 |
| auth-gca | 政府機關憑證 | 機關代碼 | 2.16.886.1.100.2.102 |
| auth-moeaca | 工商憑證 | 統一編號 | 2.16.886.1.100.2.101 |
| **auth-xca** | **組織及團體憑證** | **組織代碼** | **2.16.886.1.100.2.102** |
