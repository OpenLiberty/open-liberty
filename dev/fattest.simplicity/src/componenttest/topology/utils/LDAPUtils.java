/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class LDAPUtils {

    private static final Class<?> c = LDAPUtils.class;
    public static final boolean USE_LOCAL_LDAP_SERVER;

    public static String LDAP_SERVER_1_NAME;
    public static String LDAP_SERVER_2_NAME;
    // public static String LDAP_SERVER_3_NAME;
    public static String LDAP_SERVER_4_NAME;
    public static String LDAP_SERVER_5_NAME;
    public static String LDAP_SERVER_6_NAME;
    public static String LDAP_SERVER_7_NAME;
    public static String LDAP_SERVER_8_NAME;
//    public static String LDAP_SERVER_9_NAME;
    public static String LDAP_SERVER_10_NAME;
//    public static String LDAP_SERVER_11_NAME;
    public static String LDAP_SERVER_12_NAME;
    // public static String LDAP_SERVER_13_NAME;
    public static String LDAP_SERVER_1_PORT;
    public static String LDAP_SERVER_2_PORT;
    public static String LDAP_SERVER_2_SSL_PORT;
    //public static String LDAP_SERVER_3_PORT;
    //public static String LDAP_SERVER_3_SSL_PORT;
    public static String LDAP_SERVER_4_PORT;
    public static String LDAP_SERVER_4_SSL_PORT;
    public static String LDAP_SERVER_5_PORT;
    public static String LDAP_SERVER_6_PORT;
    public static String LDAP_SERVER_6_SSL_PORT;
    public static String LDAP_SERVER_7_PORT;
    public static String LDAP_SERVER_7_SSL_PORT;
    public static String LDAP_SERVER_8_PORT;
    public static String LDAP_SERVER_8_SSL_PORT;
//    public static String LDAP_SERVER_9_PORT;
    public static String LDAP_SERVER_10_PORT;
//    public static String LDAP_SERVER_11_PORT;
    public static String LDAP_SERVER_12_PORT;
    //public static String LDAP_SERVER_13_PORT;
    //public static String LDAP_SERVER_13_SSL_PORT;

    public static String LDAP_SERVER_2_BINDDN;
    public static String LDAP_SERVER_2_BINDPWD;
    //public static String LDAP_SERVER_3_BINDDN;
    //public static String LDAP_SERVER_3_BINDPWD;

    public static String LDAP_SERVER_4_BINDDN;
    public static String LDAP_SERVER_4_BINDPWD;
    public static String LDAP_SERVER_6_BINDDN;
    public static String LDAP_SERVER_6_BINDPWD;
    public static String LDAP_SERVER_7_BINDDN;
    public static String LDAP_SERVER_7_BINDPWD;
    public static String LDAP_SERVER_8_BINDDN;
    public static String LDAP_SERVER_8_BINDPWD;
    public static String LDAP_SERVER_10_BINDDN;
    public static String LDAP_SERVER_10_BINDPWD;
//    public static String LDAP_SERVER_11_BINDDN;
//    public static String LDAP_SERVER_11_BINDPWD;
    public static String LDAP_SERVER_12_BINDDN;
    public static String LDAP_SERVER_12_BINDPWD;

    //public static String LDAP_SERVER_13_BINDDN;
    //public static String LDAP_SERVER_13_BINDPWD;

    /** Service name for SVT's Active Directory servers. */
    private static final String CONSUL_LDAP_AD_SVT_SERVICE = "ldap-ad-svt";

    /** Service name for Continuous Delivery's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_CONTINUOUS_SERVICE = "ldap-ibm-continuous";

    /** Service name for FVT's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_SECURITY_FVT_SERVICE = "ldap-ibm-security-fvt";

    /** Service name for Security's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_SECURITY_SERVICE = "ldap-ibm-security";

    /** Service name for SVT's Oracle servers. Servers 3 and 13 */
    // private static final String CONSUL_LDAP_ORACLE_SVT_SERVICE = "ldap-oracle-svt";

    /** Key to retrieve LDAP port from Consul LDAP service. */
    private static final String CONSUL_LDAP_PORT_KEY = "ldapPort";

    /** Key to retrieve LDAPS port from Consul LDAP service. */
    private static final String CONSUL_LDAPS_PORT_KEY = "ldapsPort";

    /** Key to retrieve LDAP bind DN from Consul LDAP service. */
    private static final String CONSUL_BIND_DN_KEY = "bindDN";

    /** Key to retrieve LDAP bind password from Consul LDAP service. */
    private static final String CONSUL_BIND_PASSWORD_KEY = "bindPassword";

    /** Configuration properties for the remote LDAP servers. */
    private static final LdapServer[] remoteServers = new LdapServer[14];

    /** Configuration properties for the local LDAP servers. */
    private static final LdapServer[] localServers = new LdapServer[14];

    /** Did we fail to find the physical LDAP servers in Consul? */
    private static boolean CONSUL_LOOKUP_FAILED = false;

    static {

        /*
         * Determine whether we are running remote or locally.
         */
        boolean uselocalLDAP = isLocalLdapExpectedToBeUsed();

        /*
         * Get the remote LDAP configuration from Consul
         */
        if (!uselocalLDAP) {
            try {
                initializeRemoteLdapServers();
            } catch (Exception e) {
                /*
                 * Failed to get all the remote LDAP servers, so fail back to local LDAP.
                 */
                String os = System.getProperty("os.name").toLowerCase();
                if (os.startsWith("z/os")) {
                    Log.error(c, "<clinit>", e,
                              "*******REMOTE LDAP FAILED ON Z/OS RUN ******* Some failures setting up remote LDAPs, but local ApacheDS will not run on z/OS, doing remote anyway.");
                } else {

                    Log.error(c, "<clinit>", e, "Failed setting up remote LDAP servers. Failing back to local LDAP servers. " +
                                                "To run against the remote LDAP servers, ensure that the tests are being run " +
                                                "on the IBM network, that you have added your global IBM GHE access token " +
                                                "(global.ghe.access.token) to the user.build.properties file in your home directory " +
                                                "and that you are a member of the 'websphere' organization in IBM GHE.");
                    CONSUL_LOOKUP_FAILED = true;
                    uselocalLDAP = true;
                }
            }
        }

        USE_LOCAL_LDAP_SERVER = uselocalLDAP;
        initializeLocalLdapServers();
        setLdapServerVariables();

        logLdapServerInfo();

    }

    private static Boolean isLocalLdapExpectedToBeUsed() {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                String useInMemoryLdapString = System.getProperty("fat.test.really.use.local.ldap");
                boolean inMemoryLdap = false;
                if (useInMemoryLdapString != null) {
                    Log.info(c, "<clinit>", "fat.test.really.use.local.ldap=" + useInMemoryLdapString);
                    inMemoryLdap = Boolean.parseBoolean(useInMemoryLdapString);
                }
                return inMemoryLdap;
            }
        });
    }

    private static void initializeRemoteLdapServers() throws Exception {
        //Note: Servers 9 and 11 are dead

        /*
         * Request the remote LDAP server information from the Consul server
         */
        List<ExternalTestService> services = null;
        try {
            services = getLdapServices(2, CONSUL_LDAP_AD_SVT_SERVICE);

            remoteServers[2] = new LdapServer();
            remoteServers[2].serverName = services.get(0).getAddress();
            remoteServers[2].ldapPort = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[2].ldapsPort = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
            remoteServers[2].bindDn = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[2].bindPwd = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

            /* LDAP_SERVER_6 is dead, but was duplicate of LDAP_SERVER_2 */
            remoteServers[6] = new LdapServer();
            remoteServers[6].serverName = services.get(1).getAddress();
            remoteServers[6].ldapPort = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[6].ldapsPort = services.get(1).getProperties().get(CONSUL_LDAPS_PORT_KEY);
            remoteServers[6].bindDn = services.get(1).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[6].bindPwd = services.get(1).getProperties().get(CONSUL_BIND_PASSWORD_KEY);
        } finally {
            releaseServices(services);
        }

        services = null;
        try {
            services = getLdapServices(2, CONSUL_LDAP_IBM_CONTINUOUS_SERVICE);

            remoteServers[1] = new LdapServer();
            remoteServers[1].serverName = services.get(0).getAddress();
            remoteServers[1].ldapPort = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);

            remoteServers[5] = new LdapServer();
            remoteServers[5].serverName = services.get(1).getAddress();
            remoteServers[5].ldapPort = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
        } finally {
            releaseServices(services);
        }

        services = null;
        try {
            services = getLdapServices(3, CONSUL_LDAP_IBM_SECURITY_FVT_SERVICE);

            remoteServers[4] = new LdapServer();
            remoteServers[4].serverName = services.get(0).getAddress();
            remoteServers[4].ldapPort = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[4].ldapsPort = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
            remoteServers[4].bindDn = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[4].bindPwd = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

            remoteServers[7] = new LdapServer();
            remoteServers[7].serverName = services.get(1).getAddress();
            remoteServers[7].ldapPort = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[7].ldapsPort = services.get(1).getProperties().get(CONSUL_LDAPS_PORT_KEY);
            remoteServers[7].bindDn = services.get(1).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[7].bindPwd = services.get(1).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

            remoteServers[8] = new LdapServer();
            remoteServers[8].serverName = services.get(2).getAddress();
            remoteServers[8].ldapPort = services.get(2).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[8].ldapsPort = services.get(2).getProperties().get(CONSUL_LDAPS_PORT_KEY);
            remoteServers[8].bindDn = services.get(2).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[8].bindPwd = services.get(2).getProperties().get(CONSUL_BIND_PASSWORD_KEY);
        } finally {
            releaseServices(services);
        }

        services = null;
        try {
            services = getLdapServices(2, CONSUL_LDAP_IBM_SECURITY_SERVICE);

            remoteServers[10] = new LdapServer();
            remoteServers[10].serverName = services.get(0).getAddress();
            remoteServers[10].ldapPort = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[10].bindDn = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[10].bindPwd = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

            remoteServers[12] = new LdapServer();
            remoteServers[12].serverName = services.get(1).getAddress();
            remoteServers[12].ldapPort = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
            remoteServers[12].bindDn = services.get(1).getProperties().get(CONSUL_BIND_DN_KEY);
            remoteServers[12].bindPwd = services.get(1).getProperties().get(CONSUL_BIND_PASSWORD_KEY);
        } finally {
            releaseServices(services);
        }

    }

    private static void initializeLocalLdapServers() {

        /*
         * Read local LDAP ports from system properties
         */
        String LDAP_1_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_PORT=" + System.getProperty("ldap.1.port"));
                return System.getProperty("ldap.1.port");
            }
        });
        String LDAP_2_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_PORT=" + System.getProperty("ldap.2.port"));
                return System.getProperty("ldap.2.port");
            }
        });
        String LDAP_3_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_PORT=" + System.getProperty("ldap.3.port"));
                return System.getProperty("ldap.3.port");
            }
        });

        String LDAP_1_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_SSL_PORT=" + System.getProperty("ldap.1.ssl.port"));
                return System.getProperty("ldap.1.ssl.port");
            }
        });
        String LDAP_2_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_SSL_PORT=" + System.getProperty("ldap.2.ssl.port"));
                return System.getProperty("ldap.2.ssl.port");
            }
        });
        String LDAP_3_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_SSL_PORT=" + System.getProperty("ldap.3.ssl.port"));
                return System.getProperty("ldap.3.ssl.port");
            }
        });

        /*
         * Configure the local LDAP servers.
         */
        localServers[2] = new LdapServer();
        localServers[2].serverName = "localhost";
        localServers[2].ldapPort = LDAP_2_PORT;
        localServers[2].ldapsPort = LDAP_2_SSL_PORT;

        localServers[6] = new LdapServer();
        localServers[6].serverName = "localhost";
        localServers[6].ldapPort = LDAP_2_PORT;
        localServers[6].ldapsPort = LDAP_2_SSL_PORT;

        localServers[1] = new LdapServer();
        localServers[1].serverName = "localhost";
        localServers[1].ldapPort = LDAP_1_PORT;

        localServers[5] = new LdapServer();
        localServers[5].serverName = "localhost";
        localServers[5].ldapPort = LDAP_1_PORT;

        localServers[4] = new LdapServer();
        localServers[4].serverName = "localhost";
        localServers[4].ldapPort = LDAP_1_PORT;
        localServers[4].ldapsPort = LDAP_1_SSL_PORT;
        localServers[4].bindDn = "uid=admin,ou=system";
        localServers[4].bindPwd = "secret";

        localServers[7] = new LdapServer();
        localServers[7].serverName = "localhost";
        localServers[7].ldapPort = LDAP_1_PORT;
        localServers[7].ldapsPort = LDAP_1_SSL_PORT;
        localServers[7].bindDn = "uid=admin,ou=system";
        localServers[7].bindPwd = "secret";

        localServers[8] = new LdapServer();
        localServers[8].serverName = "localhost";
        localServers[8].ldapPort = LDAP_1_PORT;
        localServers[8].ldapsPort = LDAP_1_SSL_PORT;
        localServers[8].bindDn = "uid=admin,ou=system";
        localServers[8].bindPwd = "secret";

        localServers[10] = new LdapServer();
        localServers[10].serverName = "localhost";
        localServers[10].ldapPort = LDAP_1_PORT;
        localServers[10].bindDn = "uid=admin,ou=system";
        localServers[10].bindPwd = "secret";

        localServers[12] = new LdapServer();
        localServers[12].serverName = "localhost";
        localServers[12].ldapPort = LDAP_1_PORT;
        localServers[12].bindDn = "uid=admin,ou=system";
        localServers[12].bindPwd = "secret";

        localServers[3] = new LdapServer();
        localServers[3].serverName = "localhost";
        localServers[3].ldapPort = LDAP_3_PORT;
        localServers[3].ldapsPort = LDAP_3_SSL_PORT;

        localServers[13] = new LdapServer();
        localServers[13].serverName = "localhost";
        localServers[13].ldapPort = LDAP_3_PORT;
        localServers[13].ldapsPort = LDAP_3_SSL_PORT;
    }

    /*
     * Set the public LDAP server variables.
     *
     * I would really like to set this to the local servers when running remote, but they
     * don't start up before the LDAP test suites start checking the servers defined by
     * these variables to see if they are available. Additionally, the FAT suite can force
     * running with the remote servers even if USE_LOCAL_LDAP_SERVER==true.
     */
    private static void setLdapServerVariables() {
        Log.info(c, "<clinit>", "USE_LOCAL_LDAP_SERVER=" + USE_LOCAL_LDAP_SERVER);
        LDAP_SERVER_2_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[2] : remoteServers[2]).serverName;
        LDAP_SERVER_2_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[2] : remoteServers[2]).ldapPort;
        LDAP_SERVER_2_SSL_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[2] : remoteServers[2]).ldapsPort;
        LDAP_SERVER_2_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[2] : remoteServers[2]).bindDn;
        LDAP_SERVER_2_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[2] : remoteServers[2]).bindPwd;
        LDAP_SERVER_6_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[6] : remoteServers[6]).serverName;
        LDAP_SERVER_6_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[6] : remoteServers[6]).ldapPort;
        LDAP_SERVER_6_SSL_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[6] : remoteServers[6]).ldapsPort;
        LDAP_SERVER_6_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[6] : remoteServers[6]).bindDn;
        LDAP_SERVER_6_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[6] : remoteServers[6]).bindPwd;

        LDAP_SERVER_1_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[1] : remoteServers[1]).serverName;
        LDAP_SERVER_1_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[1] : remoteServers[1]).ldapPort;
        LDAP_SERVER_5_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[5] : remoteServers[5]).serverName;
        LDAP_SERVER_5_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[5] : remoteServers[5]).ldapPort;

        LDAP_SERVER_4_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[4] : remoteServers[4]).serverName;
        LDAP_SERVER_4_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[4] : remoteServers[4]).ldapPort;
        LDAP_SERVER_4_SSL_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[4] : remoteServers[4]).ldapsPort;
        LDAP_SERVER_4_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[4] : remoteServers[4]).bindDn;
        LDAP_SERVER_4_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[4] : remoteServers[4]).bindPwd;
        LDAP_SERVER_7_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[7] : remoteServers[7]).serverName;
        LDAP_SERVER_7_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[7] : remoteServers[7]).ldapPort;
        LDAP_SERVER_7_SSL_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[7] : remoteServers[7]).ldapsPort;
        LDAP_SERVER_7_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[7] : remoteServers[7]).bindDn;
        LDAP_SERVER_7_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[7] : remoteServers[7]).bindPwd;
        LDAP_SERVER_8_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[8] : remoteServers[8]).serverName;
        LDAP_SERVER_8_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[8] : remoteServers[8]).ldapPort;
        LDAP_SERVER_8_SSL_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[8] : remoteServers[8]).ldapsPort;
        LDAP_SERVER_8_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[8] : remoteServers[8]).bindDn;
        LDAP_SERVER_8_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[8] : remoteServers[8]).bindPwd;

        LDAP_SERVER_10_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[10] : remoteServers[10]).serverName;
        LDAP_SERVER_10_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[10] : remoteServers[10]).ldapPort;
        LDAP_SERVER_10_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[10] : remoteServers[10]).bindDn;
        LDAP_SERVER_10_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[10] : remoteServers[10]).bindPwd;
        LDAP_SERVER_12_NAME = (USE_LOCAL_LDAP_SERVER ? localServers[12] : remoteServers[12]).serverName;
        LDAP_SERVER_12_PORT = (USE_LOCAL_LDAP_SERVER ? localServers[12] : remoteServers[12]).ldapPort;
        LDAP_SERVER_12_BINDDN = (USE_LOCAL_LDAP_SERVER ? localServers[12] : remoteServers[12]).bindDn;
        LDAP_SERVER_12_BINDPWD = (USE_LOCAL_LDAP_SERVER ? localServers[12] : remoteServers[12]).bindPwd;

    }

    /*
     * Either the test was requested to run with local LDAP servers or we failed back from
     * the remote servers.
     */
    private static void logLdapServerInfo() {
        final String METHOD_NAME = "logLDAPServerInfo";
        Log.info(c, METHOD_NAME, "USE_LOCAL_LDAP_SERVER=" + USE_LOCAL_LDAP_SERVER);

        Log.info(c, METHOD_NAME, "Active Directory WAS SVT LDAP Servers");
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_2_NAME=" + LDAP_SERVER_2_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_2_PORT=" + LDAP_SERVER_2_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_2_SSL_PORT=" + LDAP_SERVER_2_SSL_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_6_NAME=" + LDAP_SERVER_6_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_6_PORT=" + LDAP_SERVER_6_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_6_SSL_PORT=" + LDAP_SERVER_6_SSL_PORT + '\n');

        Log.info(c, METHOD_NAME, "IBM Continuous Delivery LDAP Servers");
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_1_NAME=" + LDAP_SERVER_1_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_1_PORT=" + LDAP_SERVER_1_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_5_NAME=" + LDAP_SERVER_5_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_5_PORT=" + LDAP_SERVER_5_PORT + '\n');

        Log.info(c, METHOD_NAME, "IBM WAS Security FVT LDAP Servers");
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_4_NAME=" + LDAP_SERVER_4_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_4_PORT=" + LDAP_SERVER_4_PORT);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_4_SSL_PORT=" + LDAP_SERVER_4_SSL_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_7_NAME=" + LDAP_SERVER_7_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_7_PORT=" + LDAP_SERVER_7_PORT);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_7_SSL_PORT=" + LDAP_SERVER_7_SSL_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_8_NAME=" + LDAP_SERVER_8_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_8_PORT=" + LDAP_SERVER_8_PORT);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_8_SSL_PORT=" + LDAP_SERVER_8_SSL_PORT + '\n');

        Log.info(c, METHOD_NAME, "IBM WAS Security LDAP Servers");
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_10_NAME=" + LDAP_SERVER_10_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_10_PORT=" + LDAP_SERVER_10_PORT + '\n');
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_12_NAME=" + LDAP_SERVER_12_NAME);
        Log.info(c, METHOD_NAME, "           LDAP_SERVER_12_PORT=" + LDAP_SERVER_12_PORT + '\n');

        Log.info(c, METHOD_NAME, "Oracle WAS SVT LDAP Servers -- removed");

    }

    /**
     * Get a list of LDAP services from Consul.
     *
     * @param  count   The number of services requested. If unable to get unique 'count' instances,
     *                     the returned List will contain duplicate entries.
     * @param  service The service to return.
     * @return         A list of services returned. This list may return duplicates if unable to return enough
     *                 unique service instances.
     */
    private static List<ExternalTestService> getLdapServices(int count, String service) throws Exception {

        for (int requested = count; requested > 0; requested--) {
            try {
                List<ExternalTestService> services = new ArrayList<ExternalTestService>(ExternalTestService.getServices(requested, service));

                /*
                 * Remove services that are not available.
                 */
                Set<ExternalTestService> toRemove = new HashSet<ExternalTestService>();
                for (ExternalTestService serviceToPing : services) {
                    try {
                        if (!isLdapServerAvailable(serviceToPing.getAddress(),
                                                   serviceToPing.getProperties().get(CONSUL_LDAP_PORT_KEY), false,

                                                   serviceToPing.getProperties().get(CONSUL_BIND_DN_KEY),
                                                   serviceToPing.getProperties().get(CONSUL_BIND_PASSWORD_KEY))) {
                            Log.warning(c, "For service, " + service + ", Ldap at " + serviceToPing.getAddress() + " failed to ping");
                            toRemove.add(serviceToPing);
                        }
                    } catch (Exception e) {
                        Log.warning(c, "For service, " + service + ", Ldap at " + serviceToPing.getAddress() + " failed to ping with exception: " + e.getMessage());
                        toRemove.add(serviceToPing);
                    }
                }
                if (services.size() == toRemove.size()) {
                    throw new Exception("For service, " + service + ", no LDAPs responded to ping, follow regular failover path which will run local LDAP if possible.");
                } else if (!toRemove.isEmpty()) {
                    Log.warning(c, "For service, " + service + ", some LDAPS failed to ping, will remove from services list and run with good LDAPs only.");
                    services.removeAll(toRemove);
                }

                /*
                 * Copy unique instances to fill out the requested count of services.
                 */
                int idx = 0;
                int unique = services.size();
                while (services.size() < count) {
                    services.add(services.get(idx));
                    idx = idx % unique;
                }

                return services;
            } catch (IOException e) {
                /* Discontinue. The Consul server is not available. */
                throw e;
            } catch (Exception e) {
                /* Probably not enough services. Try again requesting less. */
                Log.warning(c, "Encountered error retrieving services for '" + service + "': " + e);
            }
        }

        throw new Exception("Couldn't find any available healthy services for service '" + service + "'.");
    }

    /**
     * Release a collection of Consul test services.
     *
     * @param services The services to release.
     */
    private static void releaseServices(Collection<ExternalTestService> services) {
        if (services != null) {
            for (ExternalTestService service : services) {
                service.release();
            }
        }
    }

    /**
     * Adds the following LDAP variables to the bootstrap.properties file for
     * use in server.xml:
     *
     * <ul>
     * <li>ldap.server.1.name - the host of the first LDAP server</li>
     * <li>ldap.server.1.port - the port of the first LDAP server</li>
     * <li>ldap.server.2.name - the host of the second LDAP server</li>
     * <li>ldap.server.2.port - the port of the second LDAP server</li>
     * <li>ldap.server.3.name - the host of the third LDAP server</li>
     * <li>ldap.server.3.port - the port of the third LDAP server</li>
     * <li>ldap.server.4.name - the host of the fourth LDAP server</li>
     * <li>ldap.server.4.port - the port of the fourth LDAP server</li>
     * <li>ldap.server.5.name - the host of the fifth LDAP server</li>
     * <li>ldap.server.5.port - the port of the fifth LDAP server</li>
     * <li>ldap.server.6.name - the host of the sixth LDAP server</li>
     * <li>ldap.server.6.port - the port of the sixth LDAP server</li>
     * <li>ldap.server.7.name - the host of the seventh LDAP server</li>
     * <li>ldap.server.7.port - the port of the seventh LDAP server</li>
     * <li>ldap.server.8.name - the host of the eighth LDAP server</li>
     * <li>ldap.server.8.port - the port of the eighth LDAP server</li>
     * <li>ldap.server.9.name - the host of the ninth LDAP server</li>
     * <li>ldap.server.9.port - the port of the ninth LDAP server</li>
     * <li>ldap.server.10.name - the host of the tenth LDAP server</li>
     * <li>ldap.server.10.port - the port of the tenth LDAP server</li>
     * <li>ldap.server.11.name - the host of the eleventh LDAP server</li>
     * <li>ldap.server.11.port - the port of the eleventh LDAP server</li>
     * <li>ldap.server.12.name - the host of the twelfth LDAP server</li>
     * <li>ldap.server.12.port - the port of the twelfth LDAP server</li>
     * <li>ldap.server.13.name - the host of the thirteen LDAP server</li>
     * <li>ldap.server.13.port - the port of the thirteen LDAP server</li>
     * </ul>
     *
     * @param  server
     *                       server for which bootstrap properties file needs updating with LDAP server host/ports
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server) throws Exception {
        addLDAPVariables(server, true);
    }

    /**
     * Adds LDAP variables for various servers and ports to the bootstrap.properties file for use in server.xml.
     *
     * @param  server
     * @param  isInMemoryAllowed If false, physical LDAP servers and ports will be used as the property values.
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server, boolean isInMemoryAllowed) throws Exception {
        String method = "addLDAPVariables";
        Log.entering(c, method, new Object[] { server, isInMemoryAllowed });

        Properties props = new Properties();

        // Get bootstrap properties file from server
        RemoteFile bootstrapPropFile = null;
        try {
            bootstrapPropFile = server.getServerBootstrapPropertiesFile();
        } catch (Exception e) {
            Log.error(c, method, e, "Error while getting the bootstrap properties file from liberty server");
            throw new Exception("Error while getting the bootstrap properties file from liberty server");
        }

        // Open the remote file for reading
        FileInputStream in = null;
        try {
            in = (FileInputStream) bootstrapPropFile.openForReading();
        } catch (Exception e) {
            Log.error(c, method, e, "Error while reading the remote bootstrap properties file");
            throw new Exception("Error while reading the remote bootstrap properties file");
        }

        // Load properties from bootstrap
        try {
            props.load(in);
        } catch (IOException e) {
            Log.error(c, method, e, "Error while loading properties from file input stream");
            throw new IOException("Error while loading properties from file input stream");
        } finally {
            //Close the input stream
            try {
                in.close();
            } catch (IOException e1) {
                Log.error(c, method, e1, "Error while closing the input stream");
                throw new IOException("Error while closing the input stream");
            }
        }

        // Open the remote file for writing with append as false
        FileOutputStream out = null;
        try {
            out = (FileOutputStream) bootstrapPropFile.openForWriting(false);
        } catch (Exception e) {
            Log.error(c, method, e, "Error while writing to remote bootstrap properties file");
            throw new Exception("Error while writing to remote bootstrap properties file");
        }

        Log.info(c, "addLDAPVariables", "USE_LOCAL_LDAP_SERVER=" + USE_LOCAL_LDAP_SERVER);
        Log.info(c, "addLDAPVariables", "isInMemoryAllowed=" + isInMemoryAllowed);

        /*
         * Create a Properties instance with the remote or the local server properties.
         */
        if (USE_LOCAL_LDAP_SERVER && isInMemoryAllowed) {
            Log.info(c, "addLDAPVariables", "Setting in-memory LDAP server properties");
        } else {
            /*
             * Check to see if we failed requesting the servers from Consul. If there was a failure, the output.txt log
             * will contain failure information.
             */
            if (CONSUL_LOOKUP_FAILED) {
                throw new Exception("Tests requested physical LDAP servers, but a failure was encountered retrieving them from "
                                    + "Consul. Check the output.txt file for more details.");
            }
            Log.info(c, "addLDAPVariables", "Setting physical LDAP server properties");
        }
        for (int idx = 1; idx < remoteServers.length; idx++) {
            setServerProperties(idx, props, isInMemoryAllowed);
        }

        // Write above LDAP variables to remote bootstrap properties file
        try {
            props.store(out, null);
            Log.info(c, method, "added ldap variables to bootstrap file");
        } catch (IOException e) {
            Log.error(c, method, e, "Error while reading the remote bootstrap properties file");
            throw new Exception("Error while reading the remote bootstrap properties file");
        } finally {
            //Close the output stream
            try {
                out.close();
                Log.info(c, method, "closed output stream");
            } catch (IOException e) {
                Log.error(c, method, e, "Error while closing the output stream");
                throw new IOException("Error while closing the output stream");
            }
        }
        Log.info(c, method, "about to exit routine");

        Log.exiting(c, method);
    }

    /**
     * Set the server bootstrap properties for a specified LDAP server.
     *
     * @param serverNumber      The LDAP server number.
     * @param props
     * @param isInMemoryAllowed
     */
    private static void setServerProperties(int serverNumber, Properties props, boolean isInMemoryAllowed) {
        /*
         * Determine whether we should use the local or remote server.
         */
        LdapServer server = (USE_LOCAL_LDAP_SERVER && isInMemoryAllowed) ? localServers[serverNumber] : remoteServers[serverNumber];

        if (server != null) {
            setProp(props, "ldap.server." + serverNumber + ".name", server.serverName);
            setProp(props, "ldap.server." + serverNumber + ".port", server.ldapPort);
            setProp(props, "ldap.server." + serverNumber + ".ssl.port", server.ldapsPort);
            setProp(props, "ldap.server." + serverNumber + ".bindDN", server.bindDn);
            setProp(props, "ldap.server." + serverNumber + ".bindPassword", server.bindPwd);
        }
    }

    /**
     * Set a property value in the Properties instance if the value is not null.
     *
     * @param  props The Properties instance.
     * @param  key   The key for the value.
     * @param  value The value to set.
     * @return       The previous value if it was set, null if it was not set.
     */
    private static Object setProp(Properties props, String key, String value) {
        // java.util.Properties does not allow null values, so only set the prop if value is non-null
        if (value == null) {
            return null;
        }

        Log.info(c, "setProp", "Adding server bootstrap property: " + key + "=" + value);
        return props.setProperty(key, value);
    }

    public static boolean isLdapServerAvailable(String hostname, String port) throws Exception {
        return isLdapServerAvailable(hostname, port, false);
    }

    public static boolean isLdapServerAvailable(String hostname, String port, boolean useSsl) throws Exception {
        return isLdapServerAvailable(hostname, port, false, null, null);
    }

    public static boolean isLdapServerAvailable(String hostname, String port, boolean useSsl, String bindDn, String bindPwd) throws Exception {
        String method = "isLdapServerAvaialble";
        Log.entering(c, method);

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + hostname + ":" + port);
        env.put(Context.SECURITY_AUTHENTICATION, "none");
        if (useSsl) {
            env.put(Context.PROVIDER_URL, "ldaps://" + hostname + ":" + port);
            if (bindDn != null && bindPwd != null) {
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, bindDn);
                env.put(Context.SECURITY_CREDENTIALS, bindPwd);
            }
            // Use a custom SSLSocketFactory class that doesn't check the remote server's signer certificate
            env.put("java.naming.ldap.factory.socket", CustomSSLSocketFactory.class.getName());
        }
        DirContext ctx = null;
        boolean isLdapServerAvailable = false;
        // Create initial context
        try {
            ctx = new InitialDirContext(env);
            Log.info(c, method, "Successfully created context to ldap with hostname " + hostname + " and port " + port);

            isLdapServerAvailable = true;
        } catch (CommunicationException e) {
            /*
             * Some of our older servers don't negotiate down from TLSv1.2/3, so try
             * again with TLSv1. Root failure in these cases is:
             * - java.net.SocketException: Connection or outbound has closed
             * - javax.net.ssl.SSLHandshakeException: Remote host terminated the handshake
             */
            try {
                Log.info(c, method, "CommunicationException. Using TLSv1 to retry creating context to " + hostname + " and port " + port);
                CustomSSLSocketFactory.setProtocol("TLSv1");
                ctx = new InitialDirContext(env);
                Log.info(c, method, "Successfully created context to ldap with hostname " + hostname + " and port " + port);

                isLdapServerAvailable = true;
            } catch (Exception e2) {
                /* Log original error. */
                Log.error(c, method, e, "Error while creating context to ldap with hostname " + hostname + " and port " + port);
                throw new Exception("Error while creating context to ldap with hostname " + hostname + " and port " + port);
            } finally {
                /* Don't forget to reset the protocol. */
                CustomSSLSocketFactory.resetProtocol();
            }
        } catch (Exception e) {
            Log.error(c, method, e, "Error while creating context to ldap with hostname " + hostname + " and port " + port);
            throw new Exception("Error while creating context to ldap with hostname " + hostname + " and port " + port);
        } finally {
            try {
                if (ctx != null) {
                    ctx.close();
                }
            } catch (NamingException e) {
                Log.error(c, method, e, "Error while closing context to ldap with hostname " + hostname + " and port " + port);
            }
        }
        return isLdapServerAvailable;

    }

    public static boolean areAnyLdapServersAvailable(HashMap<String, String> servers) throws Exception {
        Set<Entry<String, String>> hostsAndPorts = servers.entrySet();
        ArrayList<Entry<String, String>> failedServers = new ArrayList<Entry<String, String>>();
        boolean atLeastOneIsRunning = false;
        for (Entry<String, String> e : hostsAndPorts) {
            try {
                if (isLdapServerAvailable(e.getKey(), e.getValue())) {
                    atLeastOneIsRunning = true;
                }
            } catch (Exception e1) {
                failedServers.add(e);
            }
        }
        if (!atLeastOneIsRunning) {
            String exceptionMessage = "Error while creating context to ldap with hostname(s) ";

            for (Entry<String, String> e : failedServers) {
                exceptionMessage = exceptionMessage + e.getKey() + ":" + e.getValue() + ", ";
            }

            throw new Exception(exceptionMessage);
        }
        return atLeastOneIsRunning;
    }

    private static class LdapServer {
        String serverName = null;
        String ldapPort = null;
        String ldapsPort = null;
        String bindDn = null;
        String bindPwd = null;
    }
}
