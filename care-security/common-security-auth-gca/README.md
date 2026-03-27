# common-security-auth-gca

台灣政府機關憑證（GCA）認證模組 — 透過 `CertProvider` SPI 自動整合至 care-security 憑證認證流程。

---

## 功能總覽

- **政府機關代碼提取** — 從 X.509 憑證的 OID `2.16.886.1.100.2.102` 提取機關代碼
- **自動路由** — CertFactory 根據 Issuer DN（「政府憑證管理中心」）自動選擇此 Provider
- **共用認證流程** — 挑戰碼、PKCS#7 驗簽、憑證鏈驗證、OCSP/CRL 檢查皆由 cert-core + auth-moica 共用元件處理
- **零配置整合** — 加依賴 + 啟用即生效

---

## 啟用方式

### 1. 加入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-auth-gca</artifactId>
</dependency>
```

### 2. 設定 application.yml

```yaml
care:
  security:
    gca:
      enabled: true
```

啟用後自動註冊 `GcaCertProvider`，CertFactory 會自動發現並路由 GCA 憑證。

---

## 架構

```
使用者插入 GCA 憑證 → 前端簽章
    ↓
POST /api/auth/cert/login（共用端點）
    ↓
Pkcs7Verifier 驗簽（cert-core 共用）
    ↓
CertFactory.identify() → 偵測 Issuer DN → 路由至 GcaCertProvider
    ↓
GcaCertProvider.extractId() → 提取政府機關代碼
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
| `GcaCertProvider` | 實作 `CertProvider` SPI，提取 OID `2.16.886.1.100.2.102` 的政府機關代碼 |
| `GcaAutoConfiguration` | Spring Boot 自動配置，條件：`care.security.gca.enabled=true` |

---

## 配置項

| 配置 | 預設值 | 說明 |
|------|--------|------|
| `care.security.gca.enabled` | `false` | 是否啟用 GCA 認證 |

> 憑證驗證相關配置（OCSP、CRL、中繼 CA 路徑等）統一由 `care.security.citizen-cert.*` 管理。

---

## 依賴關係

```
common-security-auth-gca
├── common-security-core        ← CareSecurityProperties
└── common-security-cert-core   ← CertProvider SPI、CertType、CertExtensionUtils
```

---

## 與其他憑證模組的關係

| 模組 | 憑證類型 | 識別碼 | OID |
|------|---------|--------|-----|
| auth-moica | 自然人憑證 | 身分證後4碼 | 2.16.886.1.100.2.51 |
| **auth-gca** | **政府機關憑證** | **機關代碼** | **2.16.886.1.100.2.102** |
| auth-moeaca | 工商憑證 | 統一編號 | 2.16.886.1.100.2.101 |
| auth-xca | 組織及團體憑證 | 組織代碼 | 2.16.886.1.100.2.102 |

GCA 與 XCA 共用同一 OID，由 `CertType.fromIssuer()` 根據 Issuer DN 關鍵字區分。
