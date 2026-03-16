package com.company.common.security.autoconfigure;

import com.company.common.security.controller.OtpController;
import com.company.common.security.security.LoginAttemptService;
import com.company.common.security.service.OtpService;
import com.company.common.security.service.TotpService;
import com.company.common.security.repository.SaUserRepository;
import com.company.common.security.service.AuthService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for TOTP / OTP two-factor authentication.
 *
 * Activated when: care.security.otp.enabled=true
 */
@AutoConfiguration
@EnableConfigurationProperties(CareSecurityProperties.class)
@ConditionalOnProperty(prefix = "care.security.otp", name = "enabled", havingValue = "true")
public class OtpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TotpService totpService(CareSecurityProperties properties) {
        return new TotpService(properties.getOtp().getAllowedSkew());
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpService otpService(TotpService totpService, SaUserRepository saUserRepository,
                                  CareSecurityProperties properties) {
        return new OtpService(totpService, saUserRepository, properties.getOtp().getIssuer());
    }

    @Bean
    @ConditionalOnMissingBean
    public OtpController otpController(OtpService otpService, AuthService authService,
                                        LoginAttemptService loginAttemptService) {
        return new OtpController(otpService, authService, loginAttemptService);
    }
}
