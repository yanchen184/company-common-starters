package com.company.common.security.autoconfigure;

import com.company.common.security.captcha.CaptchaController;
import com.company.common.security.captcha.CaptchaService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Auto-configuration for captcha (圖形驗證碼) support.
 *
 * Activated when: care.security.captcha.enabled=true
 */
@AutoConfiguration
@EnableConfigurationProperties(CareSecurityProperties.class)
@ConditionalOnProperty(prefix = "care.security.captcha", name = "enabled", havingValue = "true")
public class CaptchaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CaptchaService captchaService(RedisTemplate<String, Object> redisTemplate,
                                          CareSecurityProperties properties) {
        CareSecurityProperties.Captcha captcha = properties.getCaptcha();
        return new CaptchaService(redisTemplate, captcha.getLength(), captcha.getExpireSeconds());
    }

    @Bean
    @ConditionalOnMissingBean
    public CaptchaController captchaController(CaptchaService captchaService) {
        return new CaptchaController(captchaService);
    }
}
