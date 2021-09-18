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
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.Oid;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Spnego;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTestHelper;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.JDK11Expectations;
import com.ibm.ws.security.spnego.fat.config.JDK8Expectations;
import com.ibm.ws.security.spnego.fat.config.JDKExpectationTestClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestService;

public class ApacheKDCCommonTest {
    private static final Class<?> c = ApacheKDCCommonTest.class;

    public static LibertyServer myServer;
    public static BasicAuthClient myClient;
    protected static SSLBasicAuthClient mySslClient = null;
    protected static CommonTestHelper testHelper;
    protected static String spnegoTokenForTestClass = null;
    protected static String TARGET_SERVER = "";
    protected static boolean wasCommonTokenRefreshed = false;
    protected static JDKExpectationTestClass expectation;
    protected static ServerConfiguration emptyConfiguration = null;

    protected final static Krb5Helper krb5Helper = new Krb5Helper();

    protected static KdcHelper kdcHelper = null;

    public static final String HOSTNAME = "localhost";

    public static final boolean ENABLE_INFO_LOGGING = true;

    protected static String keytabFile = null;

    protected static String configFile = null;

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void preClassCheck() {
        String thisMethod = "preClassCheck";

        Log.info(c, thisMethod, "Checking the assumption that the tests for this class should be run.");
        Assume.assumeTrue(InitClass.RUN_TESTS);
        if (InitClass.OTHER_SUPPORT_JDKS && !InitClass.IBM_HYBRID_JDK) {
            expectation = new JDK11Expectations();
            Log.info(c, thisMethod, "Using JDK 11 Expectations.");
        } else {
            expectation = new JDK8Expectations();
            Log.info(c, thisMethod, "Using IBM hybrid or IBM JDK 8 or lower Expectations.");
        }

        configFile = ApacheKDCforSPNEGO.getDefaultConfigFile();
        assertNotNull("ConfigFile is null", configFile);
        Log.info(c, "setUp", "Config file: " + configFile);

        keytabFile = ApacheKDCforSPNEGO.getLibertyServerSPNKeytabFile();
        assertNotNull("Keytab is null", keytabFile);
        Log.info(c, "setUp", "Keytab file: " + keytabFile);

        InitClass.COMMON_TOKEN_USER = ApacheKDCforSPNEGO.COMMON_TOKEN_USER;
        InitClass.FQN = "@" + ApacheKDCforSPNEGO.DOMAIN;

        InitClass.SECOND_USER = ApacheKDCforSPNEGO.KRB5_USER2;
        InitClass.SECOND_USER_PWD = ApacheKDCforSPNEGO.KRB5_USER2_PWD;

        //must set to avoid NPE from re-using existing helper functions
        InitClass.KDCP_VAR = InitClass.KDC_HOSTNAME;
        InitClass.KDCS_VAR = InitClass.KDC2_HOSTNAME;

    }

    //@Before
    public void preTestCheck() throws Exception {
        String thisMethod = "preTestCheck";
        Log.info(c, thisMethod, "Checking if new SPNEGO token should be created.");
        wasCommonTokenRefreshed = false;

        if (shouldCommonTokenBeRefreshed()) {
            Log.info(c, thisMethod, "Creating new SPNEGO token.");
            createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
        } else {
            Log.info(c, thisMethod, "re-using existing SPNEGO token.");
        }
    }

    public void spnegoTestSetupChecks() throws Exception {
        preTestCheck();
        setDefaultSpnegoServerConfig();
    }

    public static void createNewSpnegoToken(boolean setAsCommonSpnegoToken) throws Exception {
        createNewSpnegoToken(setAsCommonSpnegoToken, true);
    }

    /**
     * Selects a user and creates a SPNEGO token for that user to be used by the tests in this class. If
     * setAsCommonSpnegoToken is false, user1 will always be the user selected.
     *
     * @param setAsCommonSpnegoToken - Boolean indicating whether the newly created token should be set as the common
     *            SPNEGO token for all future tests and test classes.
     *
     * @param selectUser1 - disables the randomUser and instead creates a token using user1
     * @throws Exception
     */
    public static void createNewSpnegoToken(boolean setAsCommonSpnegoToken, boolean selectUser1) throws Exception {
        String thisMethod = "createNewSpnegoToken";
        Log.info(c, thisMethod, "****Creating a new SPNEGO token");

        boolean isEmployee = true;
        boolean isManager = false;

        if (setAsCommonSpnegoToken) {
            Log.info(c, thisMethod, "The new SPNEGO token will be set as the common SPNEGO token for all future tests and test classes.");

            setRolesForCommonUser(ApacheKDCforSPNEGO.KRB5_USER1);

            isEmployee = ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_EMPLOYEE;
            isManager = ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_MANAGER;
        }

        Log.info(c, thisMethod, "SPNEGO token will be created for user: " + ApacheKDCforSPNEGO.KRB5_USER1 + " (isEmployee=" + isEmployee + ", isManager=" + isManager + ")");

        createSpnegoToken(thisMethod, ApacheKDCforSPNEGO.KRB5_USER1, ApacheKDCforSPNEGO.KRB5_USER1_PWD);

        if (setAsCommonSpnegoToken) {
            ApacheKDCforSPNEGO.COMMON_TOKEN_CREATION_DATE = System.currentTimeMillis();
            DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
            Log.info(c, thisMethod, "SPNEGO token created at " + formatter.format(new Date(ApacheKDCforSPNEGO.COMMON_TOKEN_CREATION_DATE)) + " and will be refreshed in "
                                    + (ApacheKDCforSPNEGO.TOKEN_REFRESH_LIFETIME_SECONDS / 60.0) + " minute(s).");

            ApacheKDCforSPNEGO.COMMON_SPNEGO_TOKEN = spnegoTokenForTestClass;

            // Let any tests know that the token has been refreshed
            wasCommonTokenRefreshed = true;

            Log.info(c, thisMethod, "New common SPNEGO token: " + ApacheKDCforSPNEGO.COMMON_SPNEGO_TOKEN);
        }
    }

    /**
     * @param thisMethod
     * @param user
     * @param password
     * @throws Exception
     * @throws InterruptedException
     */
    private static void createSpnegoToken(String thisMethod, String user, String password) throws Exception, InterruptedException {
        for (int i = 1; i <= 3; i++) {
            try {
                spnegoTokenForTestClass = createToken(user, password, TARGET_SERVER, null, null, "krb5ConfPath", krb5Helper, true, Krb5Helper.SPNEGO_MECH_OID, null);
                break;
            } catch (LoginException e) {
                if (i == 3) {
                    Log.info(c, thisMethod, "Login still failed after " + i + " attempts");
                    String errorMsg = "Exception was caught while trying to create a SPNEGO token. Due to LoginException: " + e.getMessage();
                    Log.error(c, thisMethod, e, errorMsg);
                    e.printStackTrace();
                    throw (new Exception(errorMsg, e));
                }
                Thread.sleep(2000);
            } catch (Exception e2) {
                String errorMsg = "Exception was caught while trying to create a SPNEGO token. Ensuing tests requiring use of this token might fail. " + e2.getMessage();
                Log.error(c, thisMethod, e2, errorMsg);
                e2.printStackTrace();
                throw (new Exception(errorMsg, e2));
            }
        }
    }

    public static String createToken(String username, String password, String targetServer, String realm, String kdcHostName, String krb5ConfPath,
                                     Krb5Helper krb5Helper, boolean includeClientGSSCredentialInSubject, Oid mechOid, String jaasLoginContextEntry) throws Exception {
        String method = "createToken";
        String targetSpn = ApacheKDCforSPNEGO.SPN;
        Log.info(c, method, "Target SPN: " + targetSpn);

        Subject subject = null;
        String krbName = username;
        if (realm != null) {
            krbName = username + "@" + realm;
            subject = krb5Helper.kerberosLogin(myServer, username, password, null, realm, kdcHostName, jaasLoginContextEntry);
        } else {
            subject = krb5Helper.kerberosLogin(myServer, username, password, configFile, null, null, null);
        }

        return krb5Helper.createToken(subject, krbName, targetSpn, includeClientGSSCredentialInSubject, mechOid);
    }

    private static String selectRandomUser() {
        List<String> users = new ArrayList<String>();
        users.add(ApacheKDCforSPNEGO.KRB5_USER1);
        users.add(ApacheKDCforSPNEGO.KRB5_USER2);
        return users.get((new Random().nextInt(users.size())));
    }

    private static void setRolesForCommonUser(String user) {
        if (user.equals(ApacheKDCforSPNEGO.KRB5_USER1)) {
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_EMPLOYEE = true;
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_MANAGER = false;
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_PWD = ApacheKDCforSPNEGO.KRB5_USER1_PWD;
        } else {
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_EMPLOYEE = false;
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_MANAGER = true;
            ApacheKDCforSPNEGO.COMMON_TOKEN_USER_PWD = ApacheKDCforSPNEGO.KRB5_USER2_PWD;
        }
    }

    /**
     * Sets up protected variables, starts the server, and waits for it to be ready. A server.xml file is expected to
     * be present in the server's root directory. No SPN, keytab file, or SPNEGO token will be created; the common SPN,
     * keytab file, and SPNEGO token will be used. No apps will be validated as ready, and no additional bootstrap
     * properties will be set.
     *
     * @param testServerName
     * @throws Exception
     */
    public static void commonSetUp(String testServerName) throws Exception {
        commonSetUp(testServerName, null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS);
    }

    /**
     * Sets up protected variables. A server.xml file is expected to be present in the server's root directory. No SPN,
     * keytab file, or SPNEGO token will be created; the common SPN, keytab file, and SPNEGO token will be used. No
     * apps will be validated as ready, and no additional bootstrap properties will be set.
     *
     * @param testServerName
     * @param startServer - Boolean indicating whether the server should be started once setup is complete
     * @throws Exception
     */
    public static void commonSetUp(String testServerName, boolean startServer) throws Exception {
        commonSetUp(testServerName, null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, startServer);
    }

    /**
     * Sets up protected variables, starts the server, and waits for it to be ready. No SPN, keytab file, or SPNEGO
     * token will be created; the common SPN, keytab file, and SPNEGO token will be used.
     *
     * @param testServerName
     * @param serverXml - Server config file within the server's configs/ directory to use. If null, a server.xml file
     *            is expected to be present in the server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param testProps - Map of bootstrap property names and values to be set so they can be used in server
     *            configurations
     * @throws Exception
     */
    public static void commonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps) throws Exception {
        commonSetUp(testServerName, serverXml, checkApps, testProps, SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                    SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME,
                    SPNEGOConstants.DONT_USE_COMMON_KEYTAB, SPNEGOConstants.START_SERVER);
    }

    /**
     * Sets up protected variables. No SPN, keytab file, or SPNEGO token will be created; the common SPN, keytab file,
     * and SPNEGO token will be used.
     *
     * @param testServerName
     * @param serverXml - Server config file within the server's configs/ directory to use. If null, a server.xml file
     *            is expected to be present in the server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param testProps - Map of bootstrap property names and values to be set so they can be used in server
     *            configurations
     * @param startServer - Boolean indicating whether the server should be started once setup is complete
     * @throws Exception
     */
    public static void commonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps, boolean startServer) throws Exception {
        commonSetUp(testServerName, serverXml, checkApps, testProps, SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                    SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME,
                    SPNEGOConstants.USE_COMMON_KEYTAB, startServer);
    }

    /**
     * Sets up protected variables. This method also determines whether an SPN, keytab, and/or initial SPNEGO token
     * should be created, and whether the specified server should be started.
     *
     * @param testServerName
     * @param serverXml - Server config file within the server's configs/ directory to use. If null, a server.xml file
     *            is expected to be present in the server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param testProps - Map of bootstrap property names and values to be set so they can be used in server
     *            configurations
     * @param createSslClient
     * @param createSpnAndKeytab
     * @param spnRealm - Realm to use for the SPN added to the keytab. If null, no realm will be appended to the SPN.
     * @param createSpnegoToken
     * @param setCommonSpnegoToken - Boolean indicating whether the new SPNEGO token (if one is created) should be set
     *            as the new common SPNEGO token for all future tests and test classes
     * @param useCanonicalHostName
     * @param copyCommonKeytab - Boolean indicating whether the keytab file created during initial setup should be
     *            copied into this server's respective Kerberos resources directory
     * @param startServer - Boolean indicating whether the server should be started once setup is complete
     * @throws Exception
     */
    public static void commonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                   boolean createSslClient, boolean createSpnAndKeytab, String spnRealm, boolean createSpnegoToken,
                                   boolean setCommonSpnegoToken, boolean useCanonicalHostName, boolean copyCommonKeytab, boolean startServer) throws Exception {
        String thisMethod = "commonSetUp";
        Log.info(c, thisMethod, "***Starting testcase: " + testServerName + "...");
        Log.info(c, thisMethod, "Performing common setup");

        if (createSpnAndKeytab && copyCommonKeytab) {
            Exception e = new Exception("The specified parameters for this method conflict with each other. The given parameters specify that an SPN and keytab file should be created, but also that the common keytab file should be used. These two files might not be identical, so it is unclear which keytab file should be used. Only one, or neither, of these parameters should be set to true.");
            Log.error(c, thisMethod, e);
            throw e;
        }

        if (testServerName != null) {
            setMyServer(LibertyServerFactory.getLibertyServer(testServerName));
        }

        if (emptyConfiguration == null) {
            emptyConfiguration = myServer.getServerConfiguration();
        }

        testHelper = new CommonTestHelper(getMyServer(), myClient, mySslClient);
        testHelper.setShutdownMessages("CWWKS9127W");

        //createKrbConf();

        //String hostName = testHelper.getTestSystemFullyQualifiedDomainName();
        String hostName = HOSTNAME;
        int hostPort = getMyServer().getHttpDefaultPort();
        Log.info(c, thisMethod, "setting up BasicauthClient with server " + hostName + " and port " + hostPort);

        //myClient = new BasicAuthClient(hostName, hostPort, BasicAuthClient.DEFAULT_REALM, Constants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        myClient = new BasicAuthClient(getMyServer(), BasicAuthClient.DEFAULT_REALM, SPNEGOConstants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        if (createSslClient) {
            mySslClient = new SSLBasicAuthClient(getMyServer());
        }

        // Copy in the new server config
        if (serverXml != null) {
            String config = testHelper.buildFullServerConfigPath(getMyServer(), serverXml);
            testHelper.copyNewServerConfig(config);
            Log.info(c, thisMethod, "Using initial config: " + config);
        }

        /*
         * Configure the KdcHelper appropriately.
         */

        /*
         * if (getKdcHelper() == null) {
         * setKdcHelper(getKdcHelper(getMyServer()));
         * } else {
         * getKdcHelper().server = getMyServer();
         * }
         */

        if (createSpnAndKeytab) {
            try {
                Log.info(c, thisMethod, "Creating SPN and keytab");
                getKdcHelper().createSpnAndKeytab(spnRealm, useCanonicalHostName, SPNEGOConstants.DEFAULT_CMD_ARGS);
            } catch (Exception e) {
                Log.info(c, thisMethod, "Got unexpected exception; no tests will be run: " + e.getMessage());
                throw e;
            }
        }

        //String fullyQualifiedDomainName = testHelper.getTestSystemFullyQualifiedDomainName();
        //String fullyQualifiedDomainName = InitClass.getServerCanonicalHostName();
        if (useCanonicalHostName) {
            Log.info(c, thisMethod, "Using the canonical host name in the target server SPN: " + "rndhostname");
            //TARGET_SERVER = fullyQualifiedDomainName;
            TARGET_SERVER = ApacheKDCforSPNEGO.canonicalHostname;
        } else {
            Log.info(c, thisMethod, "Using the short host name in the target server SPN");
//            String shortHostName = getKdcHelper().getShortHostName(fullyQualifiedDomainName, true);
            TARGET_SERVER = ApacheKDCforSPNEGO.serverShortHostName;

        }

        if (createSpnegoToken) {
            // Only set this as the new common token if told to do so, or if the common token needs to be refreshed
            boolean setAsCommonSpnegoToken = (createSpnegoToken) ? setCommonSpnegoToken : true;
            createNewSpnegoToken(setAsCommonSpnegoToken, true);
        }

        // Add bootstrap properties
        String inetAddr = java.net.InetAddress.getLocalHost().getHostAddress();
        Map<String, String> bootstrapProps = new HashMap<String, String>();
        bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
        bootstrapProps.put("spnego.host.ipaddr", inetAddr);
        //addConsulBootStrappingProps(bootstrapProps);
        if (testProps != null) {
            bootstrapProps.putAll(testProps);
        }
        testHelper.addBootstrapProperties(getMyServer(), bootstrapProps);

        getMyServer().copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        if (copyCommonKeytab) {
            Log.info(c, thisMethod, "Copying common keytab file into " + SPNEGOConstants.KRB_RESOURCE_LOCATION);
            // Liberty infrastructure already adds leading and trailing '/' characters when copying
            //String sanitizedKrbResourcePath = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1, SPNEGOConstants.KRB_RESOURCE_LOCATION.length() - 1);
            //getMyServer().copyFileToLibertyServerRoot(sanitizedKrbResourcePath, ApacheDSandKDCforSPNEGO.KEYTAB_FILE_LOCATION);
        }

        if (startServer) {
            testHelper.startServer(serverXml, checkApps);

            // Wait for feature update to complete

            expectation.serverUpdate(getMyServer());
        }
    }

    public static void spnegoTokencommonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                              boolean createSslClient, boolean createSpnAndKeytab, String spnRealm, boolean createSpnegoToken,
                                              boolean setCommonSpnegoToken, boolean useCanonicalHostName, boolean copyCommonKeytab, boolean startServer,
                                              String servletName, String rootContext) throws Exception {
        spnegoTokencommonSetUp(testServerName, serverXml, checkApps, testProps,
                               createSslClient, createSpnAndKeytab, spnRealm, createSpnegoToken,
                               setCommonSpnegoToken, useCanonicalHostName, copyCommonKeytab, startServer,
                               servletName, rootContext, false);

    }

    /**
     * Sets up protected variables. This method also determines whether an SPN, keytab, and/or initial SPNEGO token
     * should be created, and whether the specified server should be started.
     *
     * @param testServerName
     * @param serverXml - Server config file within the server's configs/ directory to use. If null, a server.xml file
     *            is expected to be present in the server's root directory.
     * @param checkApps - List of apps to be validated as ready upon server start
     * @param testProps - Map of bootstrap property names and values to be set so they can be used in server
     *            configurations
     * @param createSslClient
     * @param createSpnAndKeytab
     * @param spnRealm - Realm to use for the SPN added to the keytab. If null, no realm will be appended to the SPN.
     * @param createSpnegoToken
     * @param setCommonSpnegoToken - Boolean indicating whether the new SPNEGO token (if one is created) should be set
     *            as the new common SPNEGO token for all future tests and test classes
     * @param useCanonicalHostName
     * @param copyCommonKeytab - Boolean indicating whether the keytab file created during initial setup should be
     *            copied into this server's respective Kerberos resources directory
     * @param startServer - Boolean indicating whether the server should be started once setup is complete
     * @param servletName - name of servlet (other than SimpleServlet) to be invoked
     * @param rootContext - the root context of servlet to be invoked
     * @param useUser1 - use only user1 when creating spnego token.
     * @throws Exception
     */
    public static void spnegoTokencommonSetUp(String testServerName, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                              boolean createSslClient, boolean createSpnAndKeytab, String spnRealm, boolean createSpnegoToken,
                                              boolean setCommonSpnegoToken, boolean useCanonicalHostName, boolean copyCommonKeytab, boolean startServer,
                                              String servletName, String rootContext, boolean useUser1) throws Exception {
        String thisMethod = "spnegoTokencommonSetUp";
        Log.info(c, thisMethod, "***Starting testcase: " + testServerName + "...");
        Log.info(c, thisMethod, "Performing SPNEGO token common setup");

        if (createSpnAndKeytab && copyCommonKeytab) {
            Exception e = new Exception("The specified parameters for this method conflict with each other. The given parameters specify that an SPN and keytab file should be created, but also that the common keytab file should be used. These two files might not be identical, so it is unclear which keytab file should be used. Only one, or neither, of these parameters should be set to true.");
            Log.error(c, thisMethod, e);
            throw e;
        }

        if (testServerName != null) {
            setMyServer(LibertyServerFactory.getLibertyServer(testServerName));
        }
        String hostName = testHelper.getTestSystemFullyQualifiedDomainName();
        int hostPort = getMyServer().getHttpDefaultPort();
        // using actual hostname and port instead of localhost and port
        myClient = new BasicAuthClient(hostName, hostPort, BasicAuthClient.DEFAULT_REALM, servletName, rootContext);
        if (createSslClient) {
            mySslClient = new SSLBasicAuthClient(getMyServer());
        }
        testHelper = new CommonTestHelper(getMyServer(), myClient, mySslClient);

        // Copy in the new server config
        if (serverXml != null) {
            String config = testHelper.buildFullServerConfigPath(getMyServer(), serverXml);
            testHelper.copyNewServerConfig(config);
            Log.info(c, thisMethod, "Using initial config: " + config);
        }

        String fullyQualifiedDomainName = testHelper.getTestSystemFullyQualifiedDomainName();
        if (useCanonicalHostName) {
            Log.info(c, thisMethod, "Using the canonical host name in the target server SPN");
            TARGET_SERVER = fullyQualifiedDomainName;
        } else {
            Log.info(c, thisMethod, "Using the short host name in the target server SPN");
            //String shortHostName = getKdcHelper().getShortHostName(fullyQualifiedDomainName, true);
            TARGET_SERVER = ApacheKDCforSPNEGO.serverShortHostName;
        }

        if (createSpnegoToken || shouldCommonTokenBeRefreshed()) {
            // Only set this as the new common token if told to do so, or if the common token needs to be refreshed
            boolean setAsCommonSpnegoToken = (createSpnegoToken) ? setCommonSpnegoToken : true;
            createNewSpnegoToken(setAsCommonSpnegoToken, useUser1);
        }

        // Add bootstrap properties
        Map<String, String> bootstrapProps = new HashMap<String, String>();
        bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
        //addConsulBootStrappingProps(bootstrapProps);
        if (testProps != null) {
            bootstrapProps.putAll(testProps);
        }
        testHelper.addBootstrapProperties(getMyServer(), bootstrapProps);

        getMyServer().copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        if (copyCommonKeytab) {
            Log.info(c, thisMethod, "Copying common keytab file into " + SPNEGOConstants.KRB_RESOURCE_LOCATION);
            // Liberty infrastructure already adds leading and trailing '/' characters when copying
            String sanitizedKrbResourcePath = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1, SPNEGOConstants.KRB_RESOURCE_LOCATION.length() - 1);
            getMyServer().copyFileToLibertyServerRoot(sanitizedKrbResourcePath, ApacheKDCforSPNEGO.KEYTAB_FILE_LOCATION);
        }

        if (startServer) {
            testHelper.startServer(serverXml, checkApps);

            // Wait for feature update to complete
            expectation.serverUpdate(getMyServer());
        }
    }

    /**
     * Sets up the specified server without using any of the protected static variables in this class. This should facilitate setting
     * up servers without stepping on the feet of other test classes that extend this class. Bootstrap properties are set, the common
     * keytab file is optionally pulled in, and the server is optionally started.
     *
     * @param testServer
     * @param serverXml
     * @param checkApps
     * @param testProps
     * @param copyCommonKeytab
     * @param startServer
     * @throws Exception
     */
    public static void setupServer(LibertyServer testServer, String serverXml, List<String> checkApps, Map<String, String> testProps,
                                   boolean copyCommonKeytab, boolean startServer) throws Exception {
        String thisMethod = "setupServer";
        Log.info(c, thisMethod, "Setting up server");

        CommonTestHelper localTestHelper = new CommonTestHelper(testServer, null, null);

        // Copy in the new server config
        if (serverXml != null) {
            String config = localTestHelper.buildFullServerConfigPath(testServer, serverXml);
            localTestHelper.copyNewServerConfig(config);
            Log.info(c, thisMethod, "Using initial config: " + config);
        }

        // Add bootstrap properties
        String inetAddr = java.net.InetAddress.getLocalHost().getHostAddress();
        Map<String, String> bootstrapProps = new HashMap<String, String>();
        bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
        bootstrapProps.put("spnego.host.ipaddr", inetAddr);
        //addConsulBootStrappingProps(bootstrapProps);
        if (testProps != null) {
            bootstrapProps.putAll(testProps);
        }
        localTestHelper.addBootstrapProperties(testServer, bootstrapProps);

        testServer.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");

        if (copyCommonKeytab) {
            Log.info(c, thisMethod, "Copying common keytab file into " + SPNEGOConstants.KRB_RESOURCE_LOCATION);
            // Liberty infrastructure already adds leading and trailing '/' characters when copying
            //String sanitizedKrbResourcePath = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1, SPNEGOConstants.KRB_RESOURCE_LOCATION.length() - 1);
            //testServer.copyFileToLibertyServerRoot(sanitizedKrbResourcePath, ApacheDSandKDCforSPNEGO.KEYTAB_FILE_LOCATION);
        }

        if (startServer) {
            localTestHelper.startServer(serverXml, checkApps);

            // Wait for feature update to complete

            expectation.serverUpdate(getMyServer());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "commonTearDown", "Common tear down");
        if (getMyServer() != null) {
            String[] msgs;
            try {
                msgs = testHelper.getShutdownMessages();
            } catch (Exception e) {
                Log.info(c, "commonTearDown", "testHelper.getShutdownMessages() threw an exception...");
                getMyServer().stopServer();
                return;
            }
            if (msgs != null && msgs.length > 0) {
                getMyServer().stopServer(msgs);
            } else {
                getMyServer().stopServer();
            }
        }
    }

    /**
     * Return the appropriate KdcHelper subtype for the corresponding KDC to be used by the tests.
     *
     * @param server
     * @return
     */
    /*
     * private static KdcHelper getKdcHelper(LibertyServer server) {
     * Log.info(c, "getKdcHelper", "Getting appropriate KdcHelper class");
     * return new LocalKdcHelper(getMyServer(), ApacheDSandKDCforSPNEGO.KDC_USER, ApacheDSandKDCforSPNEGO.KDC_USER_PWD, ApacheDSandKDCforSPNEGO.KDC_REALM);
     * }
     */

    /**
     * Determines whether the common SPNEGO token was created too far in the past to be usable in upcoming tests.
     * If the JDK runtime is non-IBM JDK, always refresh the common token.
     *
     * @return
     */
    protected static boolean shouldCommonTokenBeRefreshed() {
        String thisMethod = "shouldCommonTokenBeRefreshed";
        if (ApacheKDCforSPNEGO.OTHER_SUPPORT_JDKS) {
            return true;
        }
        long currentTime = System.currentTimeMillis();
        if (((currentTime - ApacheKDCforSPNEGO.COMMON_TOKEN_CREATION_DATE) / 1000) > ApacheKDCforSPNEGO.TOKEN_REFRESH_LIFETIME_SECONDS) {
            Log.info(c, thisMethod, "SPNEGO token lifetime has exceeded allowed time; recommend a new token should be created.");
            return true;
        }
        return false;
    }

    /**
     * Performs a call to the specified servlet that is expected to be successful using the headers provided. The
     * response received is then verified and checked to make sure the subject returned contains the user provided, as
     * well as the appropriate security roles for the user.
     *
     * @param servlet
     * @param headers
     * @param user
     * @param isEmployee
     * @param isManager
     * @return
     */
    public String successfulServletCall(String servlet, Map<String, String> headers, String user, boolean isEmployee, boolean isManager) {
        String response = myClient.accessProtectedServletWithValidHeaders(servlet, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

        return checkExpectation(user, isEmployee, isManager, response);
    }

    public String successfulServletCall(String servlet, Map<String, String> headers, String user, boolean isEmployee, boolean isManager, boolean handleSSOCookie) {
        String response = myClient.accessProtectedServletWithValidHeaders(servlet, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, handleSSOCookie);

        return checkExpectation(user, isEmployee, isManager, response);
    }

    public String successfulServletCall(String servlet, Map<String, String> headers, String user, boolean isEmployee, boolean isManager, boolean handleSSOCookie,
                                        String dumpSSOCookieName) {
        String response = myClient.accessProtectedServletWithValidHeaders(servlet, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, handleSSOCookie, dumpSSOCookieName);

        return checkExpectation(user, isEmployee, isManager, response);
    }

    /**
     * @param user
     * @param isEmployee
     * @param isManager
     * @param response
     * @return
     */
    private String checkExpectation(String user, boolean isEmployee, boolean isManager, String response) {
        expectation.successfulServletResponse(response, myClient, user, isEmployee, isManager);

        myClient.resetClientState();
        return response;
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be successful using the headers provided. The response
     * received is then verified against the user selected to be used in creating the common SPNEGO token and checked
     * for the presence of GSS credentials.
     *
     * @param headers
     * @return
     */
    public String successfulSpnegoServletCall(Map<String, String> headers) {
        return successfulSpnegoServletCall(headers, ApacheKDCforSPNEGO.COMMON_TOKEN_USER, ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_EMPLOYEE,
                                           ApacheKDCforSPNEGO.COMMON_TOKEN_USER_IS_MANAGER);
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be successful using the headers provided. The response
     * received is then verified and checked to make sure the subject returned contains the user provided, as well as
     * the appropriate security roles for the user. The response is also checked for the presence of GSS credentials
     * for the specified user.
     *
     * @param headers
     * @param user
     * @param isEmployee
     * @param isManager
     * @return
     */
    public String successfulSpnegoServletCall(Map<String, String> headers, String user, boolean isEmployee, boolean isManager) {
        return successfulSpnegoServletCall(headers, user, isEmployee, isManager, true);
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be successful using the headers provided. The response
     * received is then verified and checked to make sure the subject returned contains the user provided, as well as
     * the appropriate security roles for the user. The response is also checked for the presence of GSS credentials
     * for the specified user.
     *
     * @param headers
     * @param user
     * @param isEmployee
     * @param isManager
     * @return
     */
    public String successfulSpnegoServletCall(Map<String, String> headers, String user, boolean isEmployee, boolean isManager, boolean areGSSCredPresent) {
        String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, user, isEmployee, isManager);

        expectation.successfulExpectationsSpnegoServletCall(response, user, areGSSCredPresent);

        return response;
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to contain mapped user credentials. The WSPrincipal value
     * contained in the response is checked to make sure it contains the mapped user as well as the appropriate
     * security roles for the mapped user. The response is also checked for the presence of GSS credentials for the
     * original user specified.
     *
     * @param spnegoTokenUser
     * @param spnegoTokenUserPwd
     * @param mapToUser
     * @param isMappedUserEmployee
     * @param isMappedUserManager
     * @return
     * @throws Exception
     */
    public String successfulSpnegoServletCallForMappedUser(String spnegoTokenUser, String spnegoTokenUserPwd,
                                                           String mapToUser, boolean isMappedUserEmployee, boolean isMappedUserManager) throws Exception {
        String spnegoToken = ApacheKDCforSPNEGO.COMMON_SPNEGO_TOKEN;
        // We might already have a SPNEGO token, so only create a new token if given a user different from the one used to create the common token
        if (spnegoTokenUser != ApacheKDCforSPNEGO.COMMON_TOKEN_USER) {
            createSpnegoToken("successfulSpnegoServletCallForMappedUser", spnegoTokenUser, spnegoTokenUserPwd);
            spnegoToken = spnegoTokenForTestClass;
        }

        Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);

        // SPNEGO servlet call should be verified against the mapped user and the mapped user's roles
        String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, mapToUser, isMappedUserEmployee, isMappedUserManager);

        expectation.successfulExpectationsSpnegoServletCallForMappedUser(response, spnegoTokenUser);

        return response;
    }

    /**
     * Performs a call to the SPNEGO servlet from an SSL client that is expected to be successful using the headers
     * provided. The response received is then verified against the user selected to be used in creating the common
     * SPNEGO token and checked for the presence of GSS credentials.
     *
     * @param headers
     * @return
     */
    public String successfulSpnegoServletCallSSLClient(Map<String, String> headers) {
        if (mySslClient == null) {
            Log.info(c, name.getMethodName(), "Creating an SSL client for accessing the protected resource");
            mySslClient = new SSLBasicAuthClient(getMyServer());
        }

        Log.info(c, name.getMethodName(), "Accessing the protected resource using an SSL client");
        String response = mySslClient.accessProtectedServletWithValidHeaders(SPNEGOConstants.SIMPLE_SERVLET, headers);
        expectation.successfulSpnegoServletCallSSLClient(response, mySslClient);
        mySslClient.resetClientState();
        return response;
    }

    /**
     * Performs a call to the specified servlet that is expected to be unsuccessful using the headers provided.
     *
     * @param servlet
     * @param headers
     * @param ignoreErrorContent - If true, the response received is expected to be null
     * @param expectedStatusCode
     * @return
     */
    public String unsuccessfulServletCall(String servlet, Map<String, String> headers, boolean ignoreErrorContent, int expectedStatusCode) {
        String response = myClient.accessProtectedServletWithInvalidHeaders(servlet, headers, ignoreErrorContent, expectedStatusCode);

        if (ignoreErrorContent) {
            assertNull("Expected response to be null, but content was found.", response);
        }

        myClient.resetClientState();
        return response;
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be unsuccessful using the headers provided, resulting
     * in a 401. The response received is expected to be null due to the unauthorized request.
     *
     * @param headers
     * @return
     */
    public String unsuccessfulSpnegoServletCall(Map<String, String> headers) {
        return unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.IGNORE_ERROR_CONTENT);
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be unsuccessful using the headers provided, resulting
     * in a 401.
     *
     * @param headers
     * @param ignoreErrorContent - If true, the response received is expected to be null. Otherwise, the response
     *            received is verified as unsuccessful and checked for the absence of GSS credentials.
     * @return
     */
    public String unsuccessfulSpnegoServletCall(Map<String, String> headers, boolean ignoreErrorContent) {
        return unsuccessfulSpnegoServletCall(headers, ignoreErrorContent, 401);
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be unsuccessful using the headers provided.
     *
     * @param headers
     * @param ignoreErrorContent - If true, the response received is expected to be null. Otherwise, the response
     *            received is verified as unsuccessful and checked for the absence of GSS credentials.
     * @param expectedStatusCode
     * @return
     */
    public String unsuccessfulSpnegoServletCall(Map<String, String> headers, boolean ignoreErrorContent, int expectedStatusCode) {
        String response = unsuccessfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, ignoreErrorContent, expectedStatusCode);

        if (!ignoreErrorContent) {
            expectation.unsuccesfulSpnegoServletCall(response, SPNEGOConstants.OWNER_STRING);
        }

        return response;
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be successful using the common valid SPNEGO token. The
     * response received is then verified and checked for the presence of GSS credentials.
     *
     * @return
     * @throws Exception
     */
    public String commonSuccessfulSpnegoServletCall() throws Exception {
        return successfulSpnegoServletCall(createCommonHeaders());
    }

    /**
     * Performs a call to the SPNEGO servlet from an SSL client that is expected to be successful using the common
     * valid SPNEGO token. The response received is then verified and checked for the presence of GSS credentials.
     *
     * @return
     * @throws Exception
     */
    public String commonSuccessfulSpnegoServletCallSSLClient() throws Exception {
        return successfulSpnegoServletCallSSLClient(createCommonHeaders());
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be unsuccessful using the common valid SPNEGO token,
     * resulting in a 401. The response received is expected to be null due to the unauthorized request.
     *
     * @return
     * @throws Exception
     */
    public String commonUnsuccessfulSpnegoServletCall() throws Exception {
        return commonUnsuccessfulSpnegoServletCall(401);
    }

    /**
     * Performs a call to the SPNEGO servlet that is expected to be unsuccessful using the common valid SPNEGO token.
     * The response received is expected to be null due to the unauthorized request.
     *
     * @param expectedStatusCode
     * @return
     * @throws Exception
     */
    public String commonUnsuccessfulSpnegoServletCall(int expectedStatusCode) throws Exception {
        return unsuccessfulSpnegoServletCall(createCommonHeaders(), SPNEGOConstants.IGNORE_ERROR_CONTENT, expectedStatusCode);
    }

    /**
     * Creates and returns a map of headers using the SPNEGO token created for this test class in the Authorization
     * header, Firefox as the User-Agent header value, TARGET_SERVER as the Host header value, and null as the remote
     * address header value.
     *
     * @return
     */
    protected Map<String, String> createHeaders() {
        return testHelper.setTestHeaders("Negotiate " + spnegoTokenForTestClass, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
    }

    /**
     * Creates and returns a map of headers using the common SPNEGO token in the Authorization header, Firefox as the
     * User-Agent header value, TARGET_SERVER as the Host header value, and null as the remote address header value.
     *
     * @return
     * @throws Exception
     */
    protected Map<String, String> createCommonHeaders() throws Exception {
        return testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
    }

    protected static String getCommonSPNEGOToken() throws Exception {
        /*
         * if (shouldCommonTokenBeRefreshed()) {
         * createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
         * }
         */
        return ApacheKDCforSPNEGO.COMMON_SPNEGO_TOKEN;
    }

    /**
     * @return the kdcHelper
     */
    public static KdcHelper getKdcHelper() {
        return kdcHelper;
    }

    /**
     * @param kdcHelper the kdcHelper to set
     */
    public static void setKdcHelper(KdcHelper kdcHelper) {
        ApacheKDCCommonTest.kdcHelper = kdcHelper;
    }

    /**
     * @return the myServer
     */
    public static LibertyServer getMyServer() {
        return myServer;
    }

    /**
     * @param myServer the myServer to set
     */
    public static void setMyServer(LibertyServer myServer) {
        ApacheKDCCommonTest.myServer = myServer;
    }

    /**
     * Update the server to have a populated Spnego config element with the SPN, krb5Keytab and krb5Config attributes.
     *
     * @param newServer
     * @return
     */
    public Spnego addDefaultSpnegoConfigElement(ServerConfiguration newServer) {
        return setSpnegoConfigElement(newServer, configFile, keytabFile, ApacheKDCforSPNEGO.SPN, "false", null, null, null);
    }

    protected Spnego setDefaultSpnegoConfigValues(Spnego spnego) throws Exception {
        spnego.krb5Config = configFile;
        spnego.krb5Keytab = keytabFile;
        spnego.servicePrincipalNames = ApacheKDCforSPNEGO.SPN;
        spnego.canonicalHostName = "false";
        spnego.skipForUnprotectedURI = null;
        spnego.spnegoAuthenticationErrorPageURL = null;
        spnego.spnegoNotSupportedErrorPageURL = null;
        spnego.ntlmTokenReceivedErrorPageURL = null;
        return spnego;
    }

    /**
     * @param newServer - server configuration
     * @param authErrorPage - Spnego Authentication ErrorPage URL
     * @param useCanonicalHost
     * @param spn - krb5 servicePrincipalName
     * @param keytab - krb5Keytab
     * @param config - krb5Config
     * @return
     */
    public Spnego setSpnegoConfigElement(ServerConfiguration newServer, String config, String keytab, String spn, String useCanonicalHost, String authErrorPage,
                                         String notSupportedErrorPage, String ntlmErrorPage) {
        Spnego spnego = newServer.getSpnego();
        spnego.krb5Config = config;
        spnego.krb5Keytab = keytab;
        spnego.servicePrincipalNames = spn;
        spnego.canonicalHostName = "false";
        spnego.spnegoAuthenticationErrorPageURL = authErrorPage;
        spnego.spnegoNotSupportedErrorPageURL = notSupportedErrorPage;
        spnego.ntlmTokenReceivedErrorPageURL = ntlmErrorPage;

        return spnego;
    }

    protected void setDefaultSpnegoServerConfig() throws Exception {
        setDefaultSpnegoServerConfig(false);
    }

    protected void setDefaultSpnegoServerConfig(boolean enableInfoLogging) throws Exception {
        setSpnegoServerConfig(configFile, keytabFile, ApacheKDCforSPNEGO.SPN, "false", null, null, null, false);
    }

    protected void setSpnegoServerConfig(String config, String keytab, String spn, String useCanonicalHost, String authErrorPage, String notSupportedErrorPage,
                                         String ntlmErrorPage, boolean enableInfoLogging) throws Exception {
        if (!enableInfoLogging) {
            ApacheKDCCommonTest.setGlobalLoggingLevel(Level.WARNING);
        }
        ServerConfiguration newServerConfig = emptyConfiguration.clone();
        Spnego spnego = setSpnegoConfigElement(newServerConfig, config, keytab, spn, useCanonicalHost, authErrorPage, notSupportedErrorPage, ntlmErrorPage);

        updateConfigDynamically(myServer, newServerConfig, false);
        ApacheKDCCommonTest.setGlobalLoggingLevel(Level.INFO);

        Log.info(c, "setSpnegoServerConfig", spnego.toString());
        Log.info(c, "setSpnegoServerConfig", "================== Spnego Config is Set  ==================");

    }

    /**
     * Release a collection of Consul test services.
     *
     * @param services The services to release.
     */
    public static void releaseServices(Collection<ExternalTestService> services) {
        if (services != null) {
            for (ExternalTestService service : services) {
                service.release();
            }
        }
    }

    public static void setGlobalLoggingLevel(Level level) {
        Logger root = Logger.getLogger("");
        root.setLevel(level);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }
    }

    /**
     * This method will the reset the log and trace marks for log and trace searches, update the
     * configuration and then wait for the server to re-initialize. Optionally it will then wait for the application to start.
     *
     * @param server The server to update.
     * @param config The configuration to use.
     * @param waitForAppToStart Wait for the application to start.
     * @throws Exception If there was an issue updating the server configuration.
     */
    public static void updateConfigDynamically(LibertyServer server, ServerConfiguration config, boolean waitForAppToStart) throws Exception {
        resetMarksInLogs(server);
        server.updateServerConfiguration(config);
        //CWWKG0017I: The server configuration was successfully updated in {0} seconds.
        //CWWKG0018I: The server configuration was not updated. No functional changes were detected.
        server.waitForStringInLogUsingMark("CWWKG001[7-8]I");
        if (waitForAppToStart) {
            server.waitForStringInLogUsingMark("CWWKZ0003I"); //CWWKZ0003I: The application userRegistry updated in 0.020 seconds.
        }
    }

    /**
     * Reset the marks in all Liberty logs.
     *
     * @param server The server for the logs to reset the marks.
     * @throws Exception If there was an error resetting the marks.
     */
    public static void resetMarksInLogs(LibertyServer server) throws Exception {
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        server.setMarkToEndOfLog(server.getMostRecentTraceFile());
    }

}
