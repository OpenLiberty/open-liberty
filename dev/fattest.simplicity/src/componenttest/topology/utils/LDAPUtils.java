/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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
    public static boolean USE_LOCAL_LDAP_SERVER;

    public static String LDAP_SERVER_1_NAME;
    public static String LDAP_SERVER_2_NAME;
    public static String LDAP_SERVER_3_NAME;
    public static String LDAP_SERVER_4_NAME;
    public static String LDAP_SERVER_5_NAME;
    public static String LDAP_SERVER_6_NAME;
    public static String LDAP_SERVER_7_NAME;
    public static String LDAP_SERVER_8_NAME;
//    public static String LDAP_SERVER_9_NAME;
    public static String LDAP_SERVER_10_NAME;
//    public static String LDAP_SERVER_11_NAME;
    public static String LDAP_SERVER_12_NAME;
    public static String LDAP_SERVER_13_NAME;
    public static String LDAP_SERVER_1_PORT;
    public static String LDAP_SERVER_2_PORT;
    public static String LDAP_SERVER_2_SSL_PORT;
    public static String LDAP_SERVER_3_PORT;
    public static String LDAP_SERVER_3_SSL_PORT;
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
    public static String LDAP_SERVER_13_PORT;
    public static String LDAP_SERVER_13_SSL_PORT;

    public static String LDAP_SERVER_2_BINDDN;
    public static String LDAP_SERVER_2_BINDPWD;
    public static String LDAP_SERVER_3_BINDDN;
    public static String LDAP_SERVER_3_BINDPWD;

    public static String LDAP_SERVER_4_BINDDN;
    public static String LDAP_SERVER_4_BINDPWD;
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

    public static String LDAP_SERVER_13_BINDDN;
    public static String LDAP_SERVER_13_BINDPWD;

    /** Service name for SVT's Active Directory servers. */
    private static final String CONSUL_LDAP_AD_SVT_SERVICE = "ldap-ad-svt";

    /** Service name for Continuous Delivery's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_CONTINUOUS_SERVICE = "ldap-ibm-continuous";

    /** Service name for FVT's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_SECURITY_FVT_SERVICE = "ldap-ibm-security-fvt";

    /** Service name for Security's IBM / Tivoli servers. */
    private static final String CONSUL_LDAP_IBM_SECURITY_SERVICE = "ldap-ibm-security";

    /** Service name for SVT's Oracle servers. */
    private static final String CONSUL_LDAP_ORACLE_SVT_SERVICE = "ldap-oracle-svt";

    /** Key to retrieve LDAP port from Consul LDAP service. */
    private static final String CONSUL_LDAP_PORT_KEY = "ldapPort";

    /** Key to retrieve LDAPS port from Consul LDAP service. */
    private static final String CONSUL_LDAPS_PORT_KEY = "ldapsPort";

    /** Key to retrieve LDAP bind DN from Consul LDAP service. */
    private static final String CONSUL_BIND_DN_KEY = "bindDN";

    /** Key to retrieve LDAP bind password from Consul LDAP service. */
    private static final String CONSUL_BIND_PASSWORD_KEY = "bindPassword";

    static {

        /*
         * Determine whether we are running remote or locally.
         */
        USE_LOCAL_LDAP_SERVER = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
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

        try {
            if (!USE_LOCAL_LDAP_SERVER) {
                // Note: Servers 9, and 11 are dead

                /*
                 * Request remote the LDAP server information from the Consul server.
                 */
                List<ExternalTestService> services = null;
                try {
                    services = getLdapServices(1, CONSUL_LDAP_AD_SVT_SERVICE);

                    LDAP_SERVER_2_NAME = services.get(0).getAddress();
                    LDAP_SERVER_2_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_2_SSL_PORT = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                    LDAP_SERVER_2_BINDDN = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_2_BINDPWD = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

                    /* LDAP_SERVER_6 is dead, but was duplicate of LDAP_SERVER_2 */
                    LDAP_SERVER_6_NAME = services.get(0).getAddress();
                    LDAP_SERVER_6_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_6_SSL_PORT = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                } finally {
                    releaseServices(services);
                }

                services = null;
                try {
                    services = getLdapServices(2, CONSUL_LDAP_IBM_CONTINUOUS_SERVICE);

                    LDAP_SERVER_1_NAME = services.get(0).getAddress();
                    LDAP_SERVER_1_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);

                    LDAP_SERVER_5_NAME = services.get(1).getAddress();
                    LDAP_SERVER_5_PORT = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
                } finally {
                    releaseServices(services);
                }

                services = null;
                try {
                    services = getLdapServices(3, CONSUL_LDAP_IBM_SECURITY_FVT_SERVICE);

                    LDAP_SERVER_4_NAME = services.get(0).getAddress();
                    LDAP_SERVER_4_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_4_SSL_PORT = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                    LDAP_SERVER_4_BINDDN = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_4_BINDPWD = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

                    LDAP_SERVER_7_NAME = services.get(1).getAddress();
                    LDAP_SERVER_7_PORT = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_7_SSL_PORT = services.get(1).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                    LDAP_SERVER_7_BINDDN = services.get(1).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_7_BINDPWD = services.get(1).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

                    LDAP_SERVER_8_NAME = services.get(2).getAddress();
                    LDAP_SERVER_8_PORT = services.get(2).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_8_SSL_PORT = services.get(2).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                    LDAP_SERVER_8_BINDDN = services.get(2).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_8_BINDPWD = services.get(2).getProperties().get(CONSUL_BIND_PASSWORD_KEY);
                } finally {
                    releaseServices(services);
                }

                services = null;
                try {
                    services = getLdapServices(2, CONSUL_LDAP_IBM_SECURITY_SERVICE);

                    LDAP_SERVER_10_NAME = services.get(0).getAddress();
                    LDAP_SERVER_10_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_10_BINDDN = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_10_BINDPWD = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

                    LDAP_SERVER_12_NAME = services.get(1).getAddress();
                    LDAP_SERVER_12_PORT = services.get(1).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_12_BINDDN = services.get(1).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_12_BINDPWD = services.get(1).getProperties().get(CONSUL_BIND_PASSWORD_KEY);
                } finally {
                    releaseServices(services);
                }

                services = null;
                try {
                    services = getLdapServices(1, CONSUL_LDAP_ORACLE_SVT_SERVICE);

                    LDAP_SERVER_3_NAME = services.get(0).getAddress();
                    LDAP_SERVER_3_PORT = services.get(0).getProperties().get(CONSUL_LDAP_PORT_KEY);
                    LDAP_SERVER_3_SSL_PORT = services.get(0).getProperties().get(CONSUL_LDAPS_PORT_KEY);
                    LDAP_SERVER_3_BINDDN = services.get(0).getProperties().get(CONSUL_BIND_DN_KEY);
                    LDAP_SERVER_3_BINDPWD = services.get(0).getProperties().get(CONSUL_BIND_PASSWORD_KEY);

                    /* Server 13 is dead. Reuse server 3. */
                    LDAP_SERVER_13_NAME = LDAP_SERVER_3_NAME;
                    LDAP_SERVER_13_PORT = LDAP_SERVER_3_PORT;
                    LDAP_SERVER_13_SSL_PORT = LDAP_SERVER_3_SSL_PORT;
                    LDAP_SERVER_13_BINDDN = LDAP_SERVER_3_BINDDN;
                    LDAP_SERVER_13_BINDPWD = LDAP_SERVER_3_BINDPWD;
                } finally {
                    releaseServices(services);
                }
            }
        } catch (Exception e) {
            /*
             * Failed to get all the remote LDAP servers, so fail back to local LDAP.
             */
            Log.error(c, "<clinit>", e, "Failed setting up remote LDAP servers. Failing back to local LDAP servers. " +
                                        "To run against the remote LDAP servers, ensure that the tests are being run " +
                                        "on the IBM network, that you have added your global IBM GHE access token " +
                                        "(global.ghe.access.token) to the user.build.properties file in your home directory " +
                                        "and that you are a member of the 'was-liberty' organization in IBM GHE.");
            USE_LOCAL_LDAP_SERVER = true;
        }

        /*
         * Either the test was requested to run with local LDAP servers or we failed back from
         * the remote servers.
         */
        Log.info(c, "<clinit>", "USE_LOCAL_LDAP_SERVER=" + USE_LOCAL_LDAP_SERVER);
        if (USE_LOCAL_LDAP_SERVER == false) {
            Log.info(c, "<clinit>", "Active Directory WAS SVT LDAP Servers");
            Log.info(c, "<clinit>", "           LDAP_SERVER_2_NAME=" + LDAP_SERVER_2_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_2_PORT=" + LDAP_SERVER_2_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_6_NAME=" + LDAP_SERVER_6_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_6_PORT=" + LDAP_SERVER_6_PORT + '\n');

            Log.info(c, "<clinit>", "IBM Continuous Delivery LDAP Servers");
            Log.info(c, "<clinit>", "           LDAP_SERVER_1_NAME=" + LDAP_SERVER_1_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_1_PORT=" + LDAP_SERVER_1_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_5_NAME=" + LDAP_SERVER_5_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_5_PORT=" + LDAP_SERVER_5_PORT + '\n');

            Log.info(c, "<clinit>", "IBM WAS Security FVT LDAP Servers");
            Log.info(c, "<clinit>", "           LDAP_SERVER_4_NAME=" + LDAP_SERVER_4_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_4_PORT=" + LDAP_SERVER_4_PORT);
            Log.info(c, "<clinit>", "           LDAP_SERVER_4_SSL_PORT=" + LDAP_SERVER_4_SSL_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_7_NAME=" + LDAP_SERVER_7_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_7_PORT=" + LDAP_SERVER_7_PORT);
            Log.info(c, "<clinit>", "           LDAP_SERVER_7_SSL_PORT=" + LDAP_SERVER_7_SSL_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_8_NAME=" + LDAP_SERVER_8_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_8_PORT=" + LDAP_SERVER_8_PORT);
            Log.info(c, "<clinit>", "           LDAP_SERVER_8_SSL_PORT=" + LDAP_SERVER_8_SSL_PORT + '\n');

            Log.info(c, "<clinit>", "IBM WAS Security LDAP Servers");
            Log.info(c, "<clinit>", "           LDAP_SERVER_10_NAME=" + LDAP_SERVER_10_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_10_PORT=" + LDAP_SERVER_10_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_12_NAME=" + LDAP_SERVER_12_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_12_PORT=" + LDAP_SERVER_12_PORT + '\n');

            Log.info(c, "<clinit>", "Oracle WAS SVT LDAP Servers");
            Log.info(c, "<clinit>", "           LDAP_SERVER_3_NAME=" + LDAP_SERVER_3_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_3_PORT=" + LDAP_SERVER_3_PORT);
            Log.info(c, "<clinit>", "           LDAP_SERVER_3_SSL_PORT=" + LDAP_SERVER_3_SSL_PORT + '\n');
            Log.info(c, "<clinit>", "           LDAP_SERVER_13_NAME=" + LDAP_SERVER_13_NAME);
            Log.info(c, "<clinit>", "           LDAP_SERVER_13_PORT=" + LDAP_SERVER_13_PORT);
            Log.info(c, "<clinit>", "           LDAP_SERVER_13_SSL_PORT=" + LDAP_SERVER_13_SSL_PORT + '\n');
        }
    }

    /**
     * Get a list of LDAP services from Consul.
     *
     * @param count   The number of services requested. If unable to get unique 'count' instances,
     *                    the returned List will contain duplicate entries.
     * @param service The service to return.
     * @return A list of services returned. This list may return duplicates if unable to return enough
     *         unique service instances.
     */
    private static List<ExternalTestService> getLdapServices(int count, String service) throws Exception {

        for (int requested = count; requested > 0; requested--) {
            try {
                List<ExternalTestService> services = new ArrayList<ExternalTestService>(ExternalTestService.getServices(requested, service));

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
     * @param server
     *                   server for which bootstrap properties file needs updating with LDAP server host/ports
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server) throws Exception {
        addLDAPVariables(server, true);
    }

    /**
     * Adds LDAP variables for various servers and ports to the bootstrap.properties file for use in server.xml.
     *
     * @param server
     * @param isInMemoryAllowed If false, physical LDAP servers and ports will be used as the property values.
     * @throws Exception
     */
    public static void addLDAPVariables(LibertyServer server, boolean isInMemoryAllowed) throws Exception {
        String method = "addLDAPVariables";
        Log.entering(c, method);

        // Read LDAP ports from system properties
        final String LDAP_1_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_PORT=" + System.getProperty("ldap.1.port"));
                return System.getProperty("ldap.1.port");
            }
        });
        final String LDAP_2_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_PORT=" + System.getProperty("ldap.2.port"));
                return System.getProperty("ldap.2.port");
            }
        });
        final String LDAP_3_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_PORT=" + System.getProperty("ldap.3.port"));
                return System.getProperty("ldap.3.port");
            }
        });

        final String LDAP_1_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_1_SSL_PORT=" + System.getProperty("ldap.1.ssl.port"));
                return System.getProperty("ldap.1.ssl.port");
            }
        });
        final String LDAP_2_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_2_SSL_PORT=" + System.getProperty("ldap.2.ssl.port"));
                return System.getProperty("ldap.2.ssl.port");
            }
        });
        final String LDAP_3_SSL_PORT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                Log.info(c, "<clinit>", "LDAP_3_SSL_PORT=" + System.getProperty("ldap.3.ssl.port"));
                return System.getProperty("ldap.3.ssl.port");
            }
        });

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

        if (USE_LOCAL_LDAP_SERVER && isInMemoryAllowed) {
            Log.info(c, "addLDAPVariables", "Setting in-memory LDAP server properties");
            // Add in-memory LDAP server host/ports. Host will be localhost an port changes on instances used
            props.setProperty("ldap.server.1.name", "localhost");
            props.setProperty("ldap.server.2.name", "localhost");
            props.setProperty("ldap.server.3.name", "localhost");
            props.setProperty("ldap.server.4.name", "localhost");
            props.setProperty("ldap.server.5.name", "localhost");
            props.setProperty("ldap.server.6.name", "localhost");
            props.setProperty("ldap.server.7.name", "localhost");
            props.setProperty("ldap.server.8.name", "localhost");
            props.setProperty("ldap.server.9.name", "localhost");
            props.setProperty("ldap.server.10.name", "localhost");
            props.setProperty("ldap.server.11.name", "localhost");
            props.setProperty("ldap.server.12.name", "localhost");
            props.setProperty("ldap.server.13.name", "localhost");

            props.setProperty("ldap.server.1.port", LDAP_1_PORT);
            props.setProperty("ldap.server.2.port", LDAP_2_PORT);
            props.setProperty("ldap.server.2.ssl.port", LDAP_2_SSL_PORT);
            props.setProperty("ldap.server.3.port", LDAP_3_PORT);
            props.setProperty("ldap.server.3.ssl.port", LDAP_3_SSL_PORT);
            props.setProperty("ldap.server.4.port", LDAP_1_PORT);
            props.setProperty("ldap.server.4.ssl.port", LDAP_1_SSL_PORT);
            props.setProperty("ldap.server.5.port", LDAP_1_PORT);
            props.setProperty("ldap.server.6.port", LDAP_2_PORT);
            props.setProperty("ldap.server.6.ssl.port", LDAP_2_SSL_PORT);
            props.setProperty("ldap.server.7.port", LDAP_1_PORT);
            props.setProperty("ldap.server.7.ssl.port", LDAP_1_SSL_PORT);
            props.setProperty("ldap.server.8.port", LDAP_1_PORT);
            props.setProperty("ldap.server.8.ssl.port", LDAP_1_SSL_PORT);
            props.setProperty("ldap.server.9.port", LDAP_1_PORT);
            props.setProperty("ldap.server.10.port", LDAP_1_PORT);
            props.setProperty("ldap.server.11.port", LDAP_1_PORT);
            props.setProperty("ldap.server.12.port", LDAP_1_PORT);
            props.setProperty("ldap.server.13.port", LDAP_3_PORT);
            props.setProperty("ldap.server.13.ssl.port", LDAP_3_SSL_PORT);

            props.setProperty("ldap.server.4.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.4.bindPassword", "secret");

            props.setProperty("ldap.server.7.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.7.bindPassword", "secret");

            props.setProperty("ldap.server.8.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.8.bindPassword", "secret");

            props.setProperty("ldap.server.10.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.10.bindPassword", "secret");

            props.setProperty("ldap.server.11.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.11.bindPassword", "secret");

            props.setProperty("ldap.server.12.bindDN", "uid=admin,ou=system");
            props.setProperty("ldap.server.12.bindPassword", "secret");

        } else {
            Log.info(c, "addLDAPVariables", "Setting physical LDAP server properties");

            // Add physical LDAP server host/ports
            props.setProperty("ldap.server.1.name", LDAP_SERVER_1_NAME);
            props.setProperty("ldap.server.2.name", LDAP_SERVER_2_NAME);
            props.setProperty("ldap.server.3.name", LDAP_SERVER_3_NAME);
            props.setProperty("ldap.server.4.name", LDAP_SERVER_4_NAME);
            props.setProperty("ldap.server.5.name", LDAP_SERVER_5_NAME);
            props.setProperty("ldap.server.6.name", LDAP_SERVER_6_NAME);
            props.setProperty("ldap.server.7.name", LDAP_SERVER_7_NAME);
            props.setProperty("ldap.server.8.name", LDAP_SERVER_8_NAME);
//        props.setProperty("ldap.server.9.name", LDAP_SERVER_9_NAME);
            props.setProperty("ldap.server.10.name", LDAP_SERVER_10_NAME);
//        props.setProperty("ldap.server.11.name", LDAP_SERVER_11_NAME);
            props.setProperty("ldap.server.12.name", LDAP_SERVER_12_NAME);
            props.setProperty("ldap.server.13.name", LDAP_SERVER_13_NAME);

            props.setProperty("ldap.server.1.port", LDAP_SERVER_1_PORT);
            props.setProperty("ldap.server.2.port", LDAP_SERVER_2_PORT);
            props.setProperty("ldap.server.2.ssl.port", LDAP_SERVER_2_SSL_PORT);
            props.setProperty("ldap.server.3.port", LDAP_SERVER_3_PORT);
            props.setProperty("ldap.server.3.ssl.port", LDAP_SERVER_3_SSL_PORT);
            props.setProperty("ldap.server.4.port", LDAP_SERVER_4_PORT);
            props.setProperty("ldap.server.4.ssl.port", LDAP_SERVER_4_SSL_PORT);
            props.setProperty("ldap.server.5.port", LDAP_SERVER_5_PORT);
            props.setProperty("ldap.server.6.port", LDAP_SERVER_6_PORT);
            props.setProperty("ldap.server.6.ssl.port", LDAP_SERVER_6_SSL_PORT);
            props.setProperty("ldap.server.7.port", LDAP_SERVER_7_PORT);
            props.setProperty("ldap.server.7.ssl.port", LDAP_SERVER_7_SSL_PORT);
            props.setProperty("ldap.server.8.port", LDAP_SERVER_8_PORT);
            props.setProperty("ldap.server.8.ssl.port", LDAP_SERVER_8_SSL_PORT);
//        props.setProperty("ldap.server.9.port", LDAP_SERVER_9_PORT);
            props.setProperty("ldap.server.10.port", LDAP_SERVER_10_PORT);
//        props.setProperty("ldap.server.11.port", LDAP_SERVER_11_PORT);
            props.setProperty("ldap.server.12.port", LDAP_SERVER_12_PORT);
            props.setProperty("ldap.server.13.port", LDAP_SERVER_13_PORT);
            props.setProperty("ldap.server.13.ssl.port", LDAP_SERVER_13_SSL_PORT);

            props.setProperty("ldap.server.4.bindDN", LDAP_SERVER_4_BINDDN);
            props.setProperty("ldap.server.4.bindPassword", LDAP_SERVER_4_BINDPWD);

            props.setProperty("ldap.server.7.bindDN", LDAP_SERVER_7_BINDDN);
            props.setProperty("ldap.server.7.bindPassword", LDAP_SERVER_7_BINDPWD);

            props.setProperty("ldap.server.8.bindDN", LDAP_SERVER_8_BINDDN);
            props.setProperty("ldap.server.8.bindPassword", LDAP_SERVER_8_BINDPWD);

            props.setProperty("ldap.server.10.bindDN", LDAP_SERVER_10_BINDDN);
            props.setProperty("ldap.server.10.bindPassword", LDAP_SERVER_10_BINDPWD);

//        props.setProperty("ldap.server.11.bindDN", LDAP_SERVER_11_BINDDN);
//        props.setProperty("ldap.server.11.bindPassword", LDAP_SERVER_11_BINDPWD);

            props.setProperty("ldap.server.12.bindDN", LDAP_SERVER_12_BINDDN);
            props.setProperty("ldap.server.12.bindPassword", LDAP_SERVER_12_BINDPWD);
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

}
