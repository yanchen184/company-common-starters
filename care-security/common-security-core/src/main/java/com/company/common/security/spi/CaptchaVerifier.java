package com.company.common.security.spi;

/**
 * SPI interface for CAPTCHA verification.
 * Implemented by {@code CaptchaService} in common-security-auth-captcha module.
 */
public interface CaptchaVerifier {

    /**
     * Verify the user's answer for a given captcha ID. Single-use: deletes after attempt.
     *
     * @param captchaId the unique captcha identifier
     * @param answer    the user's answer
     * @return true if the answer matches
     */
    boolean verifyCaptcha(String captchaId, String answer);
}
