# common-security-auth-moeaca

台灣工商憑證（MOEACA）認證模組 — 透過 `CertProvider` SPI 自動整合至 care-security 憑證認證流程。

---

## 功能總覽

- **統一編號提取** — 從 X.509 憑證的 OID `2.16.886.1.100.2.101` 提取統一編號（BID）
- **自動路由** — CertFactory 根據 Issuer DN（「工商憑證管理中心」）自動選擇此 Provider
- **共用認證流程** — 挑戰碼、PKCS#7 驗簽、憑證鏈驗證、OCSP/CRL 檢查皆由 cert-core + auth-moica 共用元件處理
- **零配置整合** — 加依賴 + 啟用即生效

---

## 啟用方式

### 1. 加入依賴

```xml
<dependency>
    <groupId>com.company.common</groupId>
    <artifactId>common-security-auth-moeaca</artifactId>
</dependency>
```

### 2. 設定 application.yml

```yaml
care:
  security:
    moeaca:
      enabled: true
```

啟用後自動註冊 `MoeacaCertProvider`，CertFactory 會自動發現並路由 MOEACA 憑證。

---

## 架構

```
使用者插入工商憑證 → 前端簽章
    ↓
POST /api/auth/cert/login（共用端點）
    ↓
Pkcs7Verifier 驗簽（cert-core 共用）
    ↓
CertFactory.identify() → 偵測 Issuer DN → 路由至 MoeacaCertProvider
    ↓
MoeacaCertProvider.extractId() → 提取統一編號
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
| `MoeacaCertProvider` | 實作 `CertProvider` SPI，提取 OID `2.16.886.1.100.2.101` 的統一編號 |
| `MoeacaAutoConfiguration` | Spring Boot 自動配置，條件：`care.security.moeaca.enabled=true` |

---

## 配置項

| 配置 | 預設值 | 說明 |
|------|--------|------|
| `care.security.moeaca.enabled` | `false` | 是否啟用 MOEACA 認證 |

> 憑證驗證相關配置（OCSP、CRL、中繼 CA 路徑等）統一由 `care.security.citizen-cert.*` 管理。

---

## 依賴關係

```
common-security-auth-moeaca
├── common-security-core        ← CareSecurityProperties
└── common-security-cert-core   ← CertProvider SPI、CertType、CertExtensionUtils
```

---

## 與其他憑證模組的關係

| 模組 | 憑證類型 | 識別碼 | OID |
|------|---------|--------|-----|
| auth-moica | 自然人憑證 | 身分證後4碼 | 2.16.886.1.100.2.51 |
| auth-gca | 政府機關憑證 | 機關代碼 | 2.16.886.1.100.2.102 |
| **auth-moeaca** | **工商憑證** | **統一編號** | **2.16.886.1.100.2.101** |
| auth-xca | 組織及團體憑證 | 組織代碼 | 2.16.886.1.100.2.102 |
