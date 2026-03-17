package com.company.common.report.engine.xdocreport;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * xDocReport 引擎自動配置
 *
 * <p>當 classpath 有 xDocReport 且 common.report.enabled=true 時自動註冊引擎。
 */
@AutoConfiguration
@ConditionalOnClass(name = "fr.opensagres.xdocreport.document.IXDocReport")
@ConditionalOnProperty(prefix = "common.report", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XDocReportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XDocReportEngine xDocReportEngine() {
        return new XDocReportEngine();
    }
}
