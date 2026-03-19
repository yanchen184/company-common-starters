package com.company.common.signature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "common.signature")
public class SignatureProperties {

    /** 是否啟用簽名板功能。 */
    private boolean enabled = true;

    /** REST API 路徑前綴。 */
    private String apiPrefix = "/api/signatures";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }
}
