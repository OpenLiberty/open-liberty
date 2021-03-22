/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils;

import com.ibm.websphere.security.wim.ConfigConstants;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.ContextPool;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;

/**
 * Helper class for the Ldap Kerberos (GSSAPI) and bindAuthMechanims tests
 */
public class LdapKerberosUtils {

    private static final Class<?> c = LdapKerberosUtils.class;

    public static final String BASE_DN = "dc=example,dc=com";

    public static final String DOMAIN = "EXAMPLE.COM";

    public static final String BIND_PASSWORD = "max_secret";

    public static final String BIND_USER = "user17";

    public static final String BIND_PRINCIPAL_NAME = BIND_USER + "@" + DOMAIN;

    public static final String BIND_SIMPLE_DN = "uid=" + LdapKerberosUtils.BIND_USER + "," + BASE_DN;

    public static final String LDAP_TYPE = "Custom";

    public static final String HOSTNAME = "localhost";

    /**
     * Check if the test is running on Windows OS.
     *
     * @param methodName the name of the method being run.
     * @return True if the test is running on Windows.
     */
    public static boolean isWindows(String methodName) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            // windows not enforcing the setReadable/setWriteable
            Log.info(c, methodName,
                     "Skipping unreadable file tests on Windows: "
                                    + System.getProperty("os.name", "unknown"));
            return true;
        }
        return false;
    }

    /**
     * Get an LdapRegistry with a ticketCache defined and contextPool/caches disabled
     *
     * @param hostname
     * @param port
     * @param ticketCacheFile
     * @return
     */
    public static LdapRegistry getTicketCacheWithoutContextPool(String hostname, int port, String ticketCacheFile) {
        return getTicketCache(hostname, port, ticketCacheFile, true);
    }

    /**
     * Get an LdapRegistry with a ticketCache defined and contextPool/caches optionally disabled
     *
     * @param hostname
     * @param port
     * @param ticketCacheFile
     * @param disableCaches
     * @return
     */
    public static LdapRegistry getTicketCache(String hostname, int port, String ticketCacheFile, boolean disableCaches) {
        LdapRegistry ldap = new LdapRegistry();

        getBasicsLdapRegistry(ldap, hostname, port, disableCaches);
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        ldap.setKrb5Principal(BIND_PRINCIPAL_NAME);
        ldap.setKrb5TicketCache(ticketCacheFile);

        ldap.setJndiOutputEnabled(true);

        return ldap;

    }

    /**
     * Get a basic LdapRegistry with Kerberos and krb5Principal name, contextpool and caches disables
     *
     * @param hostname
     * @param port
     * @return
     */
    public static LdapRegistry getKrb5PrincipalNameWithoutContextPool(String hostname, int port) {
        return getKrb5PrincipalName(hostname, port, true);
    }

    /**
     * Get a basic LdapRegistry with Kerberos and krb5Principal name, contextpool and caches can be enabled or disabled
     *
     * @param hostname
     * @param port
     * @param disableCaches
     * @return
     */
    public static LdapRegistry getKrb5PrincipalName(String hostname, int port, boolean disableCaches) {
        LdapRegistry ldap = new LdapRegistry();
        getBasicsLdapRegistry(ldap, hostname, port, disableCaches);
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_BIND_AUTH_KRB5);
        ldap.setKrb5Principal(BIND_PRINCIPAL_NAME);

        return ldap;

    }

    /**
     * Get an LdapRegistry with simple bindAuthmech and Bind DN and Bind password with
     * disabled Context Pool/Caches
     *
     * @param hostname
     * @param port
     * @return
     */
    public static LdapRegistry getSimpleBind(String hostname, int port) {
        return getSimpleBind(hostname, port, true);
    }

    /**
     * Get an LdapRegistry with simple bindAuthmech and Bind DN and Bind password with
     * optionally disabled Context Pool/Caches
     *
     * @param hostname
     * @param port
     * @param disableCaches
     * @return
     */
    public static LdapRegistry getSimpleBind(String hostname, int port, boolean disableCaches) {
        LdapRegistry ldap = new LdapRegistry();

        getBasicsLdapRegistry(ldap, hostname, port, disableCaches);
        ldap.setBindAuthMechanism(ConfigConstants.CONFIG_AUTHENTICATION_TYPE_SIMPLE);
        ldap.setBindDN(BIND_SIMPLE_DN);
        ldap.setBindPassword(BIND_PASSWORD);

        return ldap;

    }

    /**
     * Disable the contextPool, search cache and attribute cache
     *
     * @param ldap
     */
    public static void disableCaches(LdapRegistry ldap) {
        ContextPool cp = new ContextPool();
        cp.setEnabled(false);
        ldap.setContextPool(cp);
        AttributesCache ac = new AttributesCache();
        ac.setEnabled(false);
        SearchResultsCache src = new SearchResultsCache();
        src.setEnabled(false);
        ldap.setLdapCache(new LdapCache(ac, src));

    }

    /**
     * Get a basic LdapRegistry with optionally disabled cache/contextPool, no bind credentials added
     *
     * @param ldap
     * @param hostname
     * @param port
     * @param disableCaches
     */
    public static void getBasicsLdapRegistry(LdapRegistry ldap, String hostname, int port, boolean disableCaches) {
        ldap.setId("LDAP1");
        ldap.setRealm("LDAPRealm");
        ldap.setHost(hostname);
        ldap.setPort(String.valueOf(port));
        ldap.setBaseDN(BASE_DN);
        ldap.setLdapType(LDAP_TYPE);

        // Force new connections for everything
        if (disableCaches) {
            disableCaches(ldap);
        }
    }

    /**
     * Create the LdapRegistry for an UnBoundID server. Search and attributes cache are disabled.
     *
     * @param port
     * @param baseDN
     * @return
     */
    public static LdapRegistry getUnboundIDRegistry(int port, String baseDN) {
        Log.info(c, "getUnboundIDRegistry", "Create unboundId LdapRegistry for " + baseDN);
        LdapRegistry ldap = new LdapRegistry();
        ldap.setId("UnboundIDLdap");
        ldap.setRealm("UnboundIDLdapRealm");
        ldap.setHost(HOSTNAME);
        ldap.setPort(String.valueOf(port));
        ldap.setIgnoreCase(true);
        ldap.setBaseDN(baseDN);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        ldap.setContextPool(new ContextPool(true, 1, 0, 3, "0s", "3000s"));
        AttributesCache ac = new AttributesCache();
        ac.setEnabled(false);
        SearchResultsCache src = new SearchResultsCache();
        src.setEnabled(false); // disable search cache so we can look up the same user over and over again
        ldap.setLdapCache(new LdapCache(ac, src));

        return ldap;
    }
}
