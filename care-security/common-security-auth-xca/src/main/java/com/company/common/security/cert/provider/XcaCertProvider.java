package com.company.common.security.cert.provider;

import com.company.common.security.cert.CertExtensionUtils;
import com.company.common.security.cert.CertProvider;
import com.company.common.security.cert.CertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;

/**
 * XCA (組織及團體憑證) certificate provider.
 * <p>
 * 從憑證的 Subject Directory Attributes extension 中提取組織代碼。
 * <p>
 * OID: 2.16.886.1.100.2.102 — 組織代碼（與 GCA 共用同一個 OID，
 * 但 {@link CertType#fromIssuer(String)} 會根據 Issuer DN 關鍵字區分）
 * <p>
 * Fallback: 從自訂 extension OID 提取
 */
public class XcaCertProvider implements CertProvider {

    private static final Logger log = LoggerFactory.getLogger(XcaCertProvider.class);

    /** Subject Directory Attributes 內的組織代碼 OID（與 GCA 共用） */
    private static final String XCA_ORG_SDA_OID = "2.16.886.1.100.2.102";

    @Override
    public CertType getCertType() {
        return CertType.XCA;
    }

    @Override
    public String extractId(X509Certificate certificate) {
        // 嘗試從 Subject Directory Attributes (OID 2.5.29.9) 提取
        String id = CertExtensionUtils.extractSubjectDirectoryAttribute(certificate, XCA_ORG_SDA_OID);
        if (id != null) {
            log.debug("XCA 組織代碼 (from SDA): {}", id);
            return id;
        }

        // Fallback: 從自訂 extension OID 提取
        id = CertExtensionUtils.extractFromCustomExtension(certificate, XCA_ORG_SDA_OID);
        if (id != null) {
            log.debug("XCA 組織代碼 (from custom extension): {}", id);
            return id;
        }

        log.warn("無法從 XCA 憑證中提取組織代碼");
        return null;
    }
}
