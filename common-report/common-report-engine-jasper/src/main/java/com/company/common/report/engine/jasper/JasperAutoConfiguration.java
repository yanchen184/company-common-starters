package com.company.common.report.engine.jasper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * JasperReports 引擎自動配置
 *
 * <p>當 classpath 有 JasperReports 且 common.report.enabled=true 時自動註冊引擎。
 */
@AutoConfiguration
@ConditionalOnClass(name = "net.sf.jasperreports.engine.JasperReport")
@ConditionalOnProperty(prefix = "common.report", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JasperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JasperReportEngine jasperReportEngine() {
        return new JasperReportEngine();
    }
}
