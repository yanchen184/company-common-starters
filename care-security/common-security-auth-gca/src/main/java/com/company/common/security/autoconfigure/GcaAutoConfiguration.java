package com.company.common.security.autoconfigure;

import com.company.common.security.cert.provider.GcaCertProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for GCA (政府機關憑證) authentication.
 * <p>
 * 預設不啟用，需在 application 設定 care.security.gca.enabled=true 才會生效。
 *
 * <pre>
 *   care:
 *     security:
 *       gca:
 *         enabled: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "care.security.gca", name = "enabled", matchIfMissing = false)
public class GcaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GcaCertProvider gcaCertProvider() {
        return new GcaCertProvider();
    }
}
