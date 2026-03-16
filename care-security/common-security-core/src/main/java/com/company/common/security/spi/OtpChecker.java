package com.company.common.security.spi;

/**
 * SPI interface for OTP (One-Time Password) status checking.
 * Implemented by {@code OtpService} in common-security-auth-otp module.
 */
public interface OtpChecker {

    /**
     * Check if OTP is enabled for a user.
     *
     * @param username the username
     * @return true if OTP is enabled and configured
     */
    boolean isOtpEnabled(String username);
}
