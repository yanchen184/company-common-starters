package com.company.common.security.autoconfigure;

import com.company.common.security.cert.provider.XcaCertProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for XCA (組織及團體憑證) authentication.
 * <p>
 * 預設不啟用，需在 application 設定 care.security.xca.enabled=true 才會生效。
 *
 * <pre>
 *   care:
 *     security:
 *       xca:
 *         enabled: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "care.security.xca", name = "enabled", matchIfMissing = false)
public class XcaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XcaCertProvider xcaCertProvider() {
        return new XcaCertProvider();
    }
}
