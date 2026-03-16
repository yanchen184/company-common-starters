package com.company.common.security.spi;

import com.company.common.security.entity.SaUser;

/**
 * SPI interface for synchronizing LDAP user information to the local database.
 * Implemented by {@code LdapUserSyncService} in common-security-auth-ldap module.
 */
public interface LdapUserSyncer {

    /**
     * Sync LDAP user to local DB. Creates if not exists, updates if exists.
     *
     * @param ldapUser the LDAP user info from authentication
     * @return the local SaUser (created or updated)
     */
    SaUser syncUser(LdapAuthenticator.LdapUserResult ldapUser);
}
