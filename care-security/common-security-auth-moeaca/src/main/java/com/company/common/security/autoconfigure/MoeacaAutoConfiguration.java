package com.company.common.security.autoconfigure;

import com.company.common.security.cert.provider.MoeacaCertProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for MOEACA (工商憑證) authentication.
 * <p>
 * 預設不啟用，需在 application 設定 care.security.moeaca.enabled=true 才會生效。
 *
 * <pre>
 *   care:
 *     security:
 *       moeaca:
 *         enabled: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "care.security.moeaca", name = "enabled", matchIfMissing = false)
public class MoeacaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MoeacaCertProvider moeacaCertProvider() {
        return new MoeacaCertProvider();
    }
}
