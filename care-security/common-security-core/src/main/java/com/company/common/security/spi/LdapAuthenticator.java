package com.company.common.security.spi;

import java.util.Optional;

/**
 * SPI interface for LDAP authentication.
 * Implemented by {@code LdapAuthenticationProvider} in common-security-auth-ldap module.
 */
public interface LdapAuthenticator {

    /**
     * Authenticate user against LDAP.
     *
     * @param username the username
     * @param password the password
     * @return LdapUserInfo if authentication succeeds, empty if user not found or bad credentials
     */
    Optional<LdapUserResult> authenticate(String username, String password);

    /**
     * Test LDAP connection with service account.
     */
    boolean testConnection();

    /**
     * LDAP user information returned after successful authentication.
     */
    record LdapUserResult(String username, String displayName, String email) {}
}
