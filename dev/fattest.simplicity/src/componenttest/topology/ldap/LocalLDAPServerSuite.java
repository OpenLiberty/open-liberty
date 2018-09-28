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
package componenttest.topology.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.LDAPUtils;

public class LocalLDAPServerSuite {

    private static final Class<?> c = LocalLDAPServerSuite.class;
    static LocalLdapServer tdsInstance = new LocalLdapServer("TDS");
    static LocalLdapServer adInstance = new LocalLdapServer("AD");
    static LocalLdapServer sunoneInstance = new LocalLdapServer("SunOne");

    private static final String KEY_DELIMITER = ":";
    private static final int KEY_SEGMENT_HOSTNAME = 0;
    private static final int KEY_SEGMENT_PORT = 1;
    private static final int KEY_SEGMENT_BIND_DN = 2;
    private static final int KEY_SEGMENT_BIND_PWD = 3;
    private static boolean isInMemoryAllowed = true;
    private static boolean throwNoPhysicalLDAPServerException = false;

    static HashMap<String, ArrayList<String>> testServers = new HashMap<String, ArrayList<String>>();
    static ArrayList<String> sslServers = new ArrayList<String>();

    @BeforeClass
    public static void setUp() throws Exception {
        String method = "setUp";
        Log.entering(c, method);
        System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification", "true");
        Log.info(c, method, "Endpoint identification was set to true");
        // Check if physical LDAP servers are up, if not then use in-memory LDAP
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER || !isInMemoryAllowed) {
            // Workaround property for OpenJDKs sun JNDI implementation
            boolean isLdapServersAvailable = false;
            if (testServers != null) {
                Set<String> primaryServers = testServers.keySet();
                if (!primaryServers.isEmpty()) {
                    isLdapServersAvailable = true;
                    for (String primaryServer : primaryServers) {

                        // Every primary server must be up, or at least one failover must be available for every primary server that is unavailable
                        String primaryHost = getHostname(primaryServer);
                        String primaryPort = getPort(primaryServer);
                        String primaryBindDn = getBindDn(primaryServer);
                        String primaryBindPwd = getBindPwd(primaryServer);
                        boolean checkFailover = false;
                        boolean useSsl = sslServers.contains(primaryServer);
                        try {
                            if (!LDAPUtils.isLdapServerAvailable(primaryHost, primaryPort, useSsl, primaryBindDn, primaryBindPwd)) {
                                checkFailover = true;
                            }
                        } catch (Exception e) {
                            Log.info(c, method, "Exception while checking availability of server " + primaryServer + ": " + e);
                            checkFailover = true;
                        }
                        if (checkFailover) {
                            // Primary LDAP server is unavailable; check if failover server is available instead
                            Log.info(c, method, "Server " + primaryServer + " was unavailable. Checking for available failovers.");
                            if (!isFailoverServerAvailable(primaryHost, primaryPort, primaryBindDn, primaryBindPwd)) {
                                isLdapServersAvailable = false;
                                break;
                            }
                        }
                    }
                }
            }

            Log.info(c, "<clinit>", "isLdapServersAvailable : " + isLdapServersAvailable);
            if (!isLdapServersAvailable && !("z/os".equalsIgnoreCase(System.getProperty("os.name")))) {
                if (!isInMemoryAllowed && throwNoPhysicalLDAPServerException) {
                    throw new Exception("No physical LDAP servers are available");
                }
                LDAPUtils.USE_LOCAL_LDAP_SERVER = true;
                Log.info(c, "<clinit>", "USE_LOCAL_LDAP_SERVER set to true as physical LDAP server(s) are not available");
            } else {
                Log.info(c, "<clinit>", "Using physical LDAP server");
            }
        }

        if (LDAPUtils.USE_LOCAL_LDAP_SERVER && isInMemoryAllowed) {
            Log.info(c, method, "Using in-memory LDAP");

            // Start all 3 instances of apache DS
            tdsInstance.start();
            adInstance.start();
            sunoneInstance.start();

        } else {
            Log.info(c, method, "Setup result: Either physical LDAP servers will be used or in-memory LDAP was requested but is not allowed");
        }

        Log.exiting(c, method);
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        String method = "tearDown";
        Log.entering(c, method);
        // Stop the 3 ApacheDS instances started above
        if (LDAPUtils.USE_LOCAL_LDAP_SERVER && isInMemoryAllowed) {
            Log.info(c, method, "Stopping in-memory LDAP instances");
            tdsInstance.stop();
            adInstance.stop();
            sunoneInstance.stop();
        }
        Log.exiting(c, method);
    }

    /**
     * Performs the usual setup, but only check the availability of the specified servers and failovers as needed.
     *
     * @param servers
     * @throws Exception
     */
    public static void setUpUsingServers(HashMap<String, ArrayList<String>> servers) throws Exception {
        setUpUsingServers(servers, true);
    }

    /**
     * Performs the usual setup, but only check the availability of the specified servers and failovers as needed.
     * If {@code isInMemoryLdapAllowed} is false, no in-memory LDAP server instances will be started.
     *
     * @param servers
     * @param isInMemoryLdapAllowed
     * @throws Exception
     */
    public static void setUpUsingServers(HashMap<String, ArrayList<String>> servers, boolean isInMemoryLdapAllowed) throws Exception {
        setUpUsingServers(servers, true, false);
    }

    /**
     * Performs the usual setup, but only check the availability of the specified servers and failovers as needed.
     * If {@code isInMemoryLdapAllowed} is false, no in-memory LDAP server instances will be started.
     *
     * @param servers
     * @param isInMemoryLdapAllowed
     * @throws Exception
     */
    public static void setUpUsingServers(HashMap<String, ArrayList<String>> servers, boolean isInMemoryLdapAllowed, boolean noPhysicalLDAPServerExceptionAllowed) throws Exception {

        String method = "setUpUsingServers";
        Log.entering(c, method);
        testServers = servers;
        isInMemoryAllowed = isInMemoryLdapAllowed;
        throwNoPhysicalLDAPServerException = noPhysicalLDAPServerExceptionAllowed;
        setUp();
        Log.exiting(c, method);
    }

    /**
     * Adds a primary test server and failover to the list of LDAP servers to be used during testing. If the primary server specified
     * is already included in {@code existingServerMap}, the failover server specified will be added to the list of failovers for that
     * primary server. This method assumes that neither the primary nor the failover server requires SSL.
     *
     * @param primaryHost
     * @param primaryPort
     * @param failoverHost
     * @param failoverPort
     * @param existingServerMap If {@code null}, a new server map will be created and returned by this method
     * @return
     */
    public static HashMap<String, ArrayList<String>> addTestServer(String primaryHost, String primaryPort, String failoverHost, String failoverPort,
                                                                   HashMap<String, ArrayList<String>> existingServerMap) {
        return addTestServer(primaryHost, primaryPort, false, null, null, failoverHost, failoverPort, false, null, null, existingServerMap);
    }

    /**
     * Adds a primary test server and failover to the list of LDAP servers to be used during testing. If the primary server specified
     * is already included in {@code existingServerMap}, the failover server specified will be added to the list of failovers for that
     * primary server.
     *
     * @param primaryHost
     * @param primaryPort
     * @param primaryUseSsl
     * @param failoverHost
     * @param failoverPort
     * @param failoverUseSsl
     * @param existingServerMap If {@code null}, a new server map will be created and returned by this method
     * @return
     */
    public static HashMap<String, ArrayList<String>> addTestServer(String primaryHost, String primaryPort, boolean primaryUseSsl, String primaryBindDn,
                                                                   String primaryBindPwd,
                                                                   String failoverHost, String failoverPort, boolean failoverUseSsl, String failoverBindDn,
                                                                   String failoverBindPwd,
                                                                   HashMap<String, ArrayList<String>> existingServerMap) {
        String method = "addFailoverServer";
        Log.entering(c, method);
        String primary = createKey(primaryHost, primaryPort, primaryBindDn, primaryBindPwd);
        if (primary == null) {
            Log.info(c, method, "Null/empty primary host or port specified, so no new primary or failover test server entry was created.");
            return existingServerMap;
        }
        String failover = createKey(failoverHost, failoverPort, failoverBindDn, failoverBindPwd);
        if (failover == null) {
            Log.info(c, method, "Null/empty failover host or port specified, so no failover server entry will be created for primary test server " + primary);
        }

        if (primaryUseSsl && !sslServers.contains(primary)) {
            Log.info(c, method, "Tracking new primary LDAP server that requires SSL: " + primary);
            sslServers.add(primary);
        }
        if (failoverUseSsl && !sslServers.contains(failover)) {
            Log.info(c, method, "Tracking new failover LDAP server that requires SSL: " + failover);
            sslServers.add(failover);
        }

        if (existingServerMap == null) {
            existingServerMap = new HashMap<String, ArrayList<String>>();
        }

        ArrayList<String> failoverList = null;
        if (existingServerMap.containsKey(primary)) {
            failoverList = existingServerMap.get(primary);
            if (failover != null && !failoverList.contains(failover)) {
                Log.info(c, method, "Adding failover server " + failover + " for primary server " + primary);
                failoverList.add(failover);
            } else {
                Log.info(c, method, "Specified failover server " + failover + " was null or was already specified as a failover for primary server " + primary);
            }
        } else {
            Log.info(c, method, "Adding primary server " + primary + ((failover == null) ? "" : " and failover server " + failover));
            failoverList = new ArrayList<String>();
            if (failover != null) {
                failoverList.add(failover);
            }
            existingServerMap.put(primary, failoverList);
        }

        Log.exiting(c, method);
        return existingServerMap;
    }

    /**
     * Check each of the failover servers specified for the given host and port and return true if there is an available
     * failover server.
     *
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    private static boolean isFailoverServerAvailable(String host, String port, String bindDn, String bindPwd) {
        String method = "isFailoverServerAvailable";
        Log.entering(c, method);

        String primary = createKey(host, port, bindDn, bindPwd);
        if (testServers != null && primary != null) {
            ArrayList<String> failovers = testServers.get(primary);
            if (failovers != null) {
                for (String failover : failovers) {

                    String serverHost = getHostname(failover);
                    String serverPort = getPort(failover);
                    String serverBindDn = getBindDn(failover);
                    String serverBindPwd = getBindPwd(failover);
                    try {
                        Log.info(c, method, "Checking availability of failover server " + failover);
                        boolean useSsl = sslServers.contains(failover);
                        if (LDAPUtils.isLdapServerAvailable(serverHost, serverPort, useSsl, serverBindDn, serverBindPwd)) {
                            Log.info(c, method, "Available failover server " + failover + " found for primary server " + primary);
                            Log.exiting(c, method);
                            return true;
                        }
                    } catch (Exception e) {
                        Log.info(c, method, "Error while checking availability of failover server " + failover + ": " + e);
                    }
                }
            }
        }
        Log.info(c, method, "No available failover server found for primary server " + primary);
        Log.exiting(c, method);
        return false;
    }

    private static String createKey(String host, String port, String bindDn, String bindPwd) {
        if (host == null || port == null || host.isEmpty() || port.isEmpty()) {
            return null;
        }
        if (bindDn == null) {
            bindDn = "null";
        }
        if (bindPwd == null) {
            bindPwd = "null";
        }
        return host + KEY_DELIMITER + port + KEY_DELIMITER + bindDn + KEY_DELIMITER + bindPwd;
    }

    private static String getHostname(String server) {
        return getKeyComponent(KEY_SEGMENT_HOSTNAME, server);
    }

    private static String getPort(String server) {
        return getKeyComponent(KEY_SEGMENT_PORT, server);
    }

    private static String getBindDn(String server) {
        return getKeyComponent(KEY_SEGMENT_BIND_DN, server);
    }

    private static String getBindPwd(String server) {
        return getKeyComponent(KEY_SEGMENT_BIND_PWD, server);
    }

    private static String getKeyComponent(int keySegment, String key) {
        String[] tokens = key.split(KEY_DELIMITER);
        if (tokens.length == 0 || keySegment >= tokens.length) {
            return key;
        } else {
            return tokens[keySegment];
        }
    }
}
