package com.company.common.security.cert.provider;

import com.company.common.security.cert.CertExtensionUtils;
import com.company.common.security.cert.CertProvider;
import com.company.common.security.cert.CertType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;

/**
 * GCA (政府機關憑證) certificate provider.
 * <p>
 * 從憑證的 Subject Directory Attributes extension 中提取政府機關代碼。
 * <p>
 * OID: 2.16.886.1.100.2.102 — 政府機關代碼
 * <p>
 * Fallback: 從自訂 extension OID 提取
 */
public class GcaCertProvider implements CertProvider {

    private static final Logger log = LoggerFactory.getLogger(GcaCertProvider.class);

    /** Subject Directory Attributes 內的政府機關代碼 OID */
    private static final String GCA_ORG_SDA_OID = "2.16.886.1.100.2.102";

    @Override
    public CertType getCertType() {
        return CertType.GCA;
    }

    @Override
    public String extractId(X509Certificate certificate) {
        // 嘗試從 Subject Directory Attributes (OID 2.5.29.9) 提取
        String id = CertExtensionUtils.extractSubjectDirectoryAttribute(certificate, GCA_ORG_SDA_OID);
        if (id != null) {
            log.debug("GCA 政府機關代碼 (from SDA): {}", id);
            return id;
        }

        // Fallback: 從自訂 extension OID 提取
        id = CertExtensionUtils.extractFromCustomExtension(certificate, GCA_ORG_SDA_OID);
        if (id != null) {
            log.debug("GCA 政府機關代碼 (from custom extension): {}", id);
            return id;
        }

        log.warn("無法從 GCA 憑證中提取政府機關代碼");
        return null;
    }
}
