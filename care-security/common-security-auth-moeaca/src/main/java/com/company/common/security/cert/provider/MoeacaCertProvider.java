package com.company.common.security.cert.provider;

import com.company.common.security.cert.CertExtensionUtils;
import com.company.common.security.cert.CertProvider;
import com.company.common.security.cert.CertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;

/**
 * MOEACA (工商憑證) certificate provider.
 * <p>
 * 從憑證的 Subject Directory Attributes extension 中提取統一編號。
 * <p>
 * OID: 2.16.886.1.100.2.101 — 統一編號
 * <p>
 * Fallback: 從自訂 extension OID 提取
 */
public class MoeacaCertProvider implements CertProvider {

    private static final Logger log = LoggerFactory.getLogger(MoeacaCertProvider.class);

    /** Subject Directory Attributes 內的統一編號 OID */
    private static final String MOEACA_BIZ_SDA_OID = "2.16.886.1.100.2.101";

    @Override
    public CertType getCertType() {
        return CertType.MOEACA;
    }

    @Override
    public String extractId(X509Certificate certificate) {
        // 嘗試從 Subject Directory Attributes (OID 2.5.29.9) 提取
        String id = CertExtensionUtils.extractSubjectDirectoryAttribute(certificate, MOEACA_BIZ_SDA_OID);
        if (id != null) {
            log.debug("MOEACA 統一編號 (from SDA): {}", id);
            return id;
        }

        // Fallback: 從自訂 extension OID 提取
        id = CertExtensionUtils.extractFromCustomExtension(certificate, MOEACA_BIZ_SDA_OID);
        if (id != null) {
            log.debug("MOEACA 統一編號 (from custom extension): {}", id);
            return id;
        }

        log.warn("無法從 MOEACA 憑證中提取統一編號");
        return null;
    }
}
