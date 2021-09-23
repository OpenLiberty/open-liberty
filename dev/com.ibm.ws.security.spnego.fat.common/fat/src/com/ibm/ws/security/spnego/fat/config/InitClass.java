/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.ExternalTestService;

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class InitClass {
    private static final Class<?> c = InitClass.class;
    /**
     * Active Directory appears to allow user names with a max of 20 characters. We add "_http" or
     * "_httpsn" to the end of the host name to create the user name, so the host name we use for
     * the user name must be 15 characters or less.
     */
    public static final int CANONICAL_HOST_NAME_CHAR_LIMIT = 13;

    public static String KDC_HOSTNAME = null;
    public static String KDC_HOST_SHORTNAME = null;
    public static String KDC_REALM = null;
    public static String KDC2_HOSTNAME = null;
    public static String KDC2_SHORTNAME = null;
    public static String KDC2_REALM = null;
    public static String KRB5_CONF = null;
    public static String KDCP_VAR = null;
    public static String KDCS_VAR = null;

    //KDC Users
    public static String KDC_USER = null;
    public static String KDC_USER_PWD = null;
    public static String KDC2_USER = null;
    public static String KDC2_USER_PWD = null;

    // User constants
    public static String FIRST_USER = null;
    public static String FIRST_USER_PWD = null;
    public static String FIRST_USER_KRB5_FQN = null;
    public static String FIRST_USER_KRB5_FQN_PWD = null;
    public static String SECOND_USER = null;
    public static String USER_PWD = null;
    public static String SECOND_USER_PWD = null;
    public static String SECOND_USER_KRB5_FQN = null;
    public static String SECOND_USER_KRB5_FQN_PWD = null;
    public static String Z_USER = null;
    public static String Z_USER_PWD = null;
    public static String FQN = null;

    public static String COMMON_TOKEN_USER = null;
    public static String COMMON_TOKEN_USER_PWD = null;
    public static boolean COMMON_TOKEN_USER_IS_EMPLOYEE = true;
    public static boolean COMMON_TOKEN_USER_IS_MANAGER = false;

    public static String COMMON_SPNEGO_TOKEN = null;
    public static String KEYTAB_FILE_LOCATION = null;
    public static long COMMON_TOKEN_CREATION_DATE = 0;
    public static final double TOKEN_REFRESH_LIFETIME_SECONDS = 180;
    public static boolean RUN_TESTS = true;
    public static boolean LOCALHOST_DEFAULT_IP_ADDRESS = true; //127.0.0.1       localhost

    public static boolean IBM_JDK_V8_LOWER = false;
    public static boolean OTHER_SUPPORT_JDKS = false;
    public static boolean SUN_ORACLE_JDK_V8_HIGHER = false;
    public static boolean IBM_HYBRID_JDK = false;

    //Properties to make sure we only send the scripts once
    public static boolean sendvbs = true;
    public static boolean needToPushaddSPNKeytab = true;
    public static boolean needToPushsetUserSPN = true;
    public static boolean needToPushwinSetSPN = true;
    public static boolean needToPushdeleteUserSPN = true;
    public static boolean isIBMJGSS = false;
    public static boolean isRndHostName = false;
    public static String rndHostName = null;

    private final static HashMap<String, String> libertyHostMap = new HashMap<String, String>();
    private final static HashMap<String, String> kdcHostMap = new HashMap<String, String>();
    public static String serverCanonicalHostName = null;
    public static String serverShortHostName = null;
    public static boolean randomizeHostName = false;

    public static void getKDCInfoFromConsul() throws Exception {
        String thisMethod = "getKDCInfoFromConsul";
        Log.info(c, thisMethod, "Getting KDCs from Consul.....");
        List<ExternalTestService> services = null;

        try {
            //obtaining kdcp and kdc_r information
            services = CommonTest.getKDCServices(2, SPNEGOConstants.KDC_HOST_FROM_CONSUL);
            KDC_HOSTNAME = services.get(0).getAddress();
            KDC_USER = services.get(0).getProperties().get(SPNEGOConstants.MS_KDC_USER_CONSUL);
            KDC_USER_PWD = services.get(0).getProperties().get(SPNEGOConstants.MS_KDC_USER_PASSWORD_CONSUL);
            KDC_REALM = services.get(0).getProperties().get(SPNEGOConstants.KDC_REALM_FROM_CONSUL);
            KDC_HOST_SHORTNAME = services.get(0).getProperties().get(SPNEGOConstants.KDC_SHORTNAME_FROM_CONSUL);
            KRB5_CONF = services.get(0).getProperties().get(SPNEGOConstants.KRB5_CONF_FROM_CONSUL);
            Z_USER = services.get(0).getProperties().get(SPNEGOConstants.Z_USER_FROM_CONSUL);
            FIRST_USER = services.get(0).getProperties().get(SPNEGOConstants.FIRST_USER_FROM_CONSUL);
            SECOND_USER = services.get(0).getProperties().get(SPNEGOConstants.SECOND_USER_FROM_CONSUL);
            USER_PWD = services.get(0).getProperties().get(SPNEGOConstants.USER_PWD_FROM_CONSUL);
            Z_USER_PWD = services.get(0).getProperties().get(SPNEGOConstants.USER0_PWD_FROM_CONSUL);

            ConnectionInfo connInfo = new ConnectionInfo(KDC_HOSTNAME, InitClass.KDC_USER, InitClass.KDC_USER_PWD);
            Machine kdcMachine = Machine.getMachine(connInfo);

            try {
                Log.info(c, thisMethod, "Testing connection to KDC: " + KDC_HOST_SHORTNAME);
                establishConnectionToKDC(thisMethod, kdcMachine);
            } catch (Exception e) {
                String failedKdcShortName = KDC_HOST_SHORTNAME;
                KDC_HOSTNAME = services.get(1).getAddress();
                KDC_USER = services.get(1).getProperties().get(SPNEGOConstants.MS_KDC_USER_CONSUL);
                KDC_USER_PWD = services.get(1).getProperties().get(SPNEGOConstants.MS_KDC_USER_PASSWORD_CONSUL);
                KDC_REALM = services.get(1).getProperties().get(SPNEGOConstants.KDC_REALM_FROM_CONSUL);
                KDC_HOST_SHORTNAME = services.get(1).getProperties().get(SPNEGOConstants.KDC_SHORTNAME_FROM_CONSUL);
                KRB5_CONF = services.get(1).getProperties().get(SPNEGOConstants.KRB5_CONF_FROM_CONSUL);
                Z_USER = services.get(1).getProperties().get(SPNEGOConstants.Z_USER_FROM_CONSUL);
                FIRST_USER = services.get(1).getProperties().get(SPNEGOConstants.FIRST_USER_FROM_CONSUL);
                SECOND_USER = services.get(1).getProperties().get(SPNEGOConstants.SECOND_USER_FROM_CONSUL);
                USER_PWD = services.get(1).getProperties().get(SPNEGOConstants.USER_PWD_FROM_CONSUL);
                Z_USER_PWD = services.get(1).getProperties().get(SPNEGOConstants.USER0_PWD_FROM_CONSUL);

                Log.info(c, thisMethod, "connection to " + failedKdcShortName + " failed. Attempting failover KDC: " + KDC_HOST_SHORTNAME);

                connInfo = new ConnectionInfo(KDC_HOSTNAME, InitClass.KDC_USER, InitClass.KDC_USER_PWD);
                kdcMachine = Machine.getMachine(connInfo);
                establishConnectionToKDC(thisMethod, kdcMachine);
            }

            KDCP_VAR = getKDCHostnameMask(KDC_HOSTNAME);

            //obtaining kdcs and kdcs_r information
            services = CommonTest.getKDCServices(1, SPNEGOConstants.KDC2_HOST_FROM_CONSUL);
            KDC2_HOSTNAME = services.get(0).getAddress();
            KDC2_USER = services.get(0).getProperties().get(SPNEGOConstants.MS_KDC_USER_REALM_2_CONSUL);
            KDC2_USER_PWD = services.get(0).getProperties().get(SPNEGOConstants.MS_KDC_PASSWORD_REALM_2_CONSUL);
            KDC2_REALM = services.get(0).getProperties().get(SPNEGOConstants.KDC2_REALM_FROM_CONSUL);

            KDCS_VAR = getKDCHostnameMask(KDC2_HOSTNAME);

            FIRST_USER_KRB5_FQN_PWD = USER_PWD;
            FIRST_USER_PWD = USER_PWD;
            SECOND_USER_PWD = USER_PWD;
            SECOND_USER_KRB5_FQN_PWD = USER_PWD;
            FQN = "@" + KDC_REALM;
            FIRST_USER_KRB5_FQN = FIRST_USER + FQN;
            SECOND_USER_KRB5_FQN = SECOND_USER + FQN;
            COMMON_TOKEN_USER = FIRST_USER;
            COMMON_TOKEN_USER_PWD = FIRST_USER_PWD;

            // get canonical and short host name
            getServerCanonicalHostName();
            getServerShortHostName();

        } catch (Exception e) {
            Log.info(c, thisMethod, "An Exception happened while getting the KDC from consul. TESTS WILL NOT RUN");
            e.printStackTrace();
            throw e;

        } finally {
            CommonTest.releaseServices(services);
        }

        Log.info(c, thisMethod, "We were able to retrieve the required information from consul, the test will now continue....");
        Log.info(c, thisMethod, "The following KDC's are being used: " + KDCP_VAR + " and " + KDCS_VAR + ".");

    }

    /**
     * @param thisMethod
     * @param kdcMachine
     * @throws Exception
     * @throws InterruptedException
     */
    private static void establishConnectionToKDC(String thisMethod, Machine kdcMachine) throws Exception, InterruptedException {
        for (int i = 1; i <= 3; i++) {
            try {
                SshClient sshClient = getSshClient();
                try {
                    getSshSession(sshClient, kdcMachine);
                } finally {
                    sshClient.stop();
                }
                Log.info(c, thisMethod, "KDC connection succeeded after " + i + " attempt(s)");
                break;
            } catch (Exception e) {
                if (i == 3) {
                    Log.info(c, thisMethod, "KDC connection still failed after retrying " + i + " attempts");
                    throw e;
                }
                Thread.sleep(5000);
            }
        }
    }

    public static String getKDCHostnameMask(String hostname) {
        String maskedHostname = "";
        if (hostname == null) {
            return "null";
        }

        if (kdcHostMap.get(hostname) == null) {
            if (hostname.contains("primary")) {
                maskedHostname = "kdcp";
            } else if (hostname.contains("secondary")) {
                maskedHostname = "kdcs";
            } else {
                //Should never reach here, but if it does, this will be useful to know.
                return "UNABLE TO GET KDC MASK: " + hostname;
            }

            if (hostname.contains("replica")) {
                maskedHostname += "_REP";
            }
            kdcHostMap.put(hostname, maskedHostname);
        } else {
            kdcHostMap.get(hostname);
        }

        return maskedHostname;
    }

    /**
     * Gets the fully qualified domain name (i.e. canonical host name) of the local host. If the code is not allowed to
     * know the hostname for this IP address, the textual representation of the IP address is returned.
     *
     * @return
     * @throws UnknownHostException
     */
    public static String getServerCanonicalHostName() throws UnknownHostException {
        String methodName = "getServerCanonicalHostName";
        InetAddress localHost = InetAddress.getLocalHost();
        String canonicalHostName = localHost.getCanonicalHostName();
        String ipAddress = localHost.getHostAddress();
        if (canonicalHostName.equals(ipAddress)) {
            throw new UnknownHostException("Can not resolve the hostname for IP address " + ipAddress +
                                           "\\n\\ SPNEGO FAT will fail. This is a machine set up issue with the host name. "
                                           + "\\n\\ This can be fixed by updating the hosts file or DNS server registration.");
        }
        if (canonicalHostName != null && canonicalHostName.length() > CANONICAL_HOST_NAME_CHAR_LIMIT) {
            Log.info(c, methodName, "Canonical host name [" + canonicalHostName + "] is longer than allowed character limit. Using a substring as the host name");
            String tmpHostLowerCase = canonicalHostName.toLowerCase();
            if (tmpHostLowerCase.contains("ebc")) {
                canonicalHostName = createRandomStringHostNameForEbc(canonicalHostName);
            } else {
                canonicalHostName = createRandomStringHostName(canonicalHostName);
            }
        }

        /*
         * If we can't resolve a canonical hostname other than localhost, we will have problems with
         * the input and output keytab being named the same. This in itself isn't a big deal,
         * but multiple hosts using the same keytab file on the shared remote KDCs will cause issues.
         *
         * Perhaps we could create a random hostname above, but I don't think this should be much
         * of an issue unless running on your local machine.
         *
         * This might be due to a DNS not being able to resolve the host name, or perhaps b/c of network
         * configuration on the local machine. For example on linux, /etc/hosts resolves 127.0.0.1 to
         * localhost before a FQDN.
         */
        if ("localhost".equalsIgnoreCase(canonicalHostName)) {
            throw new UnknownHostException("The canonical host name of " + canonicalHostName + " is not supported. Ensure that your host name is resolvable.");
        }

        serverCanonicalHostName = canonicalHostName;
        Log.info(c, methodName, "canonicalHostName: " + canonicalHostName);
        return canonicalHostName;
    }

    /**
     * EBC test machines have long host names, create a random string host name for EBC test machine.
     *
     * @param canonicalHostName
     * @return
     */
    protected static String createRandomStringHostNameForEbc(String canonicalHostName) {
        String methodName = "createRandomStringHostNameForEbc";
        rndHostName = libertyHostMap.get(canonicalHostName);
        if (rndHostName == null) {
            String prefix = "ebc_";
            String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder rdnString = new StringBuilder();
            Random rnd = new Random();
            while (rdnString.length() < CANONICAL_HOST_NAME_CHAR_LIMIT - prefix.length()) {
                int index = (int) (rnd.nextFloat() * chars.length());
                rdnString.append(chars.charAt(index));
            }
            rndHostName = prefix + rdnString.toString();
            libertyHostMap.put(canonicalHostName, rndHostName);
            isRndHostName = true;
        }
        Log.info(c, methodName, "EBC canonical hostname " + canonicalHostName + " mapped to the random generated hostname " + rndHostName);
        return rndHostName;
    }

    /**
     * Some test machines have long host names, create a random string host name for long named test machines.
     *
     * @param canonicalHostName
     * @return
     */
    protected static String createRandomStringHostName(String canonicalHostName) {
        String methodName = "createRandomStringHostName";
        rndHostName = libertyHostMap.get(canonicalHostName);
        if (rndHostName == null) {
            String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder rdnString = new StringBuilder();
            Random rnd = new Random();
            while (rdnString.length() < CANONICAL_HOST_NAME_CHAR_LIMIT) {
                int index = (int) (rnd.nextFloat() * chars.length());
                rdnString.append(chars.charAt(index));
            }
            rndHostName = rdnString.toString();
            libertyHostMap.put(canonicalHostName, rndHostName);
            isRndHostName = true;
        }
        Log.info(c, methodName, "Canonical hostname " + canonicalHostName + " mapped to the random generated hostname " + rndHostName);
        return rndHostName;
    }

    final static HashMap<String, String> hostMap = new HashMap<String, String>();

    /**
     * Returns the short host name from the canonical host name provided. If canonicalHostName does not include
     * "ibm.com", the same value provided for canonicalHostName is returned.
     *
     * @param canonicalHostName
     * @param issueMsg - Boolean indicating whether a message should be logged if the canonical host name does not
     *            include the IBM domain.
     * @return
     */
    public static String getShortHostName(String canonicalHostName, boolean issueMsg) {
        String methodName = "getShortHostName";
        String shortName = canonicalHostName;
        if (canonicalHostName == null) {
            Log.info(c, methodName, "canonicalHostName parameter is null, this is an invalid use of this method.");
            return null;
        } else if (canonicalHostName.contains(".")) {
            shortName = canonicalHostName.substring(0, canonicalHostName.indexOf("."));
        } else if (issueMsg) {
            Log.info(c, methodName, "Using a short host name; it may not work with SPNEGO");
        }
        serverShortHostName = shortName;
        Log.info(c, methodName, "shortHostName: " + shortName);
        return shortName;
    }

    /**
     * Gets the short host name of the local host. If the code is not allowed to know the host name for this IP address,
     * the textual representation of the IP address is returned.
     *
     * @return
     * @throws UnknownHostException
     */
    public static String getServerShortHostName() throws UnknownHostException {
        return getShortHostName(serverCanonicalHostName, true);
    }

    /**
     * Get a (started) SshClient.
     *
     * @return The SshClient.
     */
    protected static SshClient getSshClient() {
        SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        return sshClient;
    }

    /**
     * Get an SSH ClientSession to the specified machine.
     *
     * @param sshClient The SSH client.
     * @param machine The machine to connect to.
     * @return The session.
     * @throws IOException If there was an error getting an SSH session to the machine.
     */
    protected static ClientSession getSshSession(SshClient sshClient, Machine machine) throws IOException {
        ClientSession session = sshClient.connect(machine.getUsername(), machine.getHostname(), 22).verify(30, TimeUnit.SECONDS).getSession();
        session.addPasswordIdentity(machine.getPassword());
        session.auth().verify(30, TimeUnit.SECONDS).isSuccess();
        return session;
    }
}
