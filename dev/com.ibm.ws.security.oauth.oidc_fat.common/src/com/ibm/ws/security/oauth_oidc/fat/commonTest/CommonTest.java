/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.CommonIOTools;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.ShibbolethHelpers;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.apps.AppConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HeadMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.MessageBodyWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.LDAPUtils;
import org.apache.commons.io.IOUtils;

public class CommonTest extends com.ibm.ws.security.fat.common.CommonTest {

    public static UserFeatureInstaller userFeatureInstaller = null;
    public static TestHelpers testHelpers = new TestHelpers();
    public static ShibbolethHelpers shibbolethHelpers = new ShibbolethHelpers();

    protected static boolean useLdap = true;

    /**
     * Common access token can be used to avoid tests having to go through the
     * entire genericOP flow before calling the protected resource
     */
    protected static String commonPropagationToken = null;
    private static long propagationTokenCreationDate = 0;
    private static long testPropagationTokenLifetimeSeconds = 60;

    // MongoDB test support
    public static String MONGO_PROPS_FILE = "mongoDB.props"; // this name needs to match the one used in CustomStoreSample

    /**
     * Install new user features, to be overridden by whoever needs to install
     * user feature.
     *
     * @param aServer
     * @throws Exception
     */
    public static void installUserFeature(TestServer aServer) throws Exception {
        if (userFeatureInstaller != null) {
            userFeatureInstaller.installUserFeature(aServer);
        } else {
            Log.info(thisClass, "installUserFeature", "No InstallUninstallUserFeature(No User Feature Installer)");
        }
    };

    /**
     * Uninstall user features, to be overridden by whoever needs to uninstall
     * user feature.
     *
     * @throws Exception
     */
    public static void uninstallUserFeature() throws Exception {
        if (userFeatureInstaller != null) {
            userFeatureInstaller.uninstallUserFeature();
        } else {
            Log.info(thisClass, "uninstallUserFeature", "No InstallUninstallUserFeature(No User Feature Installer)");
        }
    };

    @ClassRule
    public static TestServer dummyServer = new TestServer();

    private final static Class<?> thisClass = CommonTest.class;
    protected static TrustManager[] trustAllCerts = null;
    public static CommonTestTools cttools = new CommonTestTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static CommonMessageTools msgUtils = new CommonMessageTools();
    public static ValidationData vData = new ValidationData();
    public static EndpointSettings eSettings = new EndpointSettings();
    public static CommonIOTools cioTools = new CommonIOTools();
    public static FillFormTools formTools = new FillFormTools();
    public static TestServer testOPServer = null;
    public static TestServer testRPServer = null;
    public static TestServer genericTestServer = null;
    public static TestServer testIDPServer = null;
    protected static String hostName = "localhost";
    protected static TestSettings testSettings = new TestSettings();
    protected static CommonTestHelpers helpers = null;
    protected static boolean OverrideRedirect = false;
    public static String[] validPortNumbers = null;
    protected static boolean pickAnIDP = false;
    protected static String[] GET_TOKEN_ACTIONS = Constants.BASIC_AUTHENTICATION_ACTIONS;
    protected static String[] GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
    protected static String LOGIN_USER = Constants.LOGIN_USER;
    protected static String PERFORM_LOGIN = Constants.PERFORM_LOGIN;
    protected static String[] RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
    protected static String[] RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
    private static HashMap<String, String> jwkValidationMap = null;
    private static HashMap<String, String> miscBootstrapParms = null;
    protected static boolean skipServerStart = false;
    protected static List<TestServer> serverRefList = new ArrayList<TestServer>();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    protected static final String IBMjdkBuilderAttribute = "org.apache.xerces.util.SecurityManager";
    protected static final String ORACLEjdkBuilderAttribute = "com.sun.org.apache.xerces.internal.util.SecurityManager";

    public static boolean usingExternalLDAPServer = false;
    public static String classOverrideValidationEndpointValue = null;

    @Rule
    public final TestName testName = new TestName();
    public static String _testName = "";

    /**
     * Sets up any configuration required for running the tests. - Starts the
     * server, which should start the applications in dropins or any other apps
     * specified in the server*.xml specified - Waits for the appropriate ports
     * - Waits for the test apps to start - It sets up the trust manager. -
     * Defines some global variables for use by the follow on tests...
     */
    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby,
                                         List<String> addtlMessages) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages, null, null, true, true);
    }

    /**
     * Sets up any configuration required for running the tests. - Starts the
     * server, which should start the applications in dropins or any other apps
     * specified in the server*.xml specified - Waits for the appropriate ports
     * - Waits for the test apps to start - It sets up the trust manager. -
     * Defines some global variables for use by the follow on tests...
     */
    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, List<String> addtlMessages,
                                         boolean secStartMsg, boolean sslCheck) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages, null, null, secStartMsg, sslCheck);
    }

    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, List<String> addtlMessages,
                                         String targetUrl, String providerType) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages, targetUrl, providerType, true, true);
    }

    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, List<String> addtlMessages,
                                         String targetUrl, String providerType, boolean secStartMsg, boolean sslCheck) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages, targetUrl, providerType, secStartMsg, sslCheck, Constants.ACCESS_TOKEN_KEY,
                           Constants.X509_CERT);
    }

    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, List<String> addtlMessages,
                                         String targetUrl, String providerType, boolean secStartMsg, boolean sslCheck, String tokenType, String certType) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages, targetUrl, providerType, secStartMsg, sslCheck, tokenType, certType,
                           Constants.JUNIT_REPORTING);
    }

    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, List<String> addtlMessages,
                                         String targetUrl, String providerType, boolean secStartMsg, boolean sslCheck, String tokenType, String certType,
                                         boolean reportViaJunit) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, false, addtlMessages, targetUrl, providerType, secStartMsg, sslCheck, tokenType, certType,
                           reportViaJunit);

    }

    public static TestServer commonSetUp(String requestedServer, String serverXML, String testType, List<String> addtlApps, boolean useDerby, boolean useMongo,
                                         List<String> addtlMessages, String targetUrl, String providerType, boolean secStartMsg, boolean sslCheck, String tokenType,
                                         String certType, boolean reportViaJunit) throws Exception {

        Integer defaultPort = null;
        String httpString = null;
        String httpsString = null;
        String thisMethod = "commonSetUp";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "OS: " + System.getProperty("os.name"));
        Log.info(thisClass, thisMethod, "requested server: " + requestedServer);
        TestServer aTestServer = null;

        // Reset the common access token to help avoid using the same token
        // across test classes
        commonPropagationToken = null;

        try {
            helpers = new CommonTestHelpers();
            aTestServer = new TestServer(requestedServer, serverXML, testType);
            aTestServer.removeServerConfigFiles();
            List<String> messages = aTestServer.getDefaultStartMessages(testType);
            if (addtlMessages != null && !addtlMessages.isEmpty()) {
                messages.addAll(addtlMessages);
            }

            /*
             * Transform EE 9 applications.
             */
            transformApps(aTestServer);

            List<String> checkApps = aTestServer.getDefaultTestApps(testType);
            for (String c : checkApps) {
                Log.info(thisClass, thisMethod, "Loop at after Default Apps: " + c);
            }
            if (addtlApps != null && !addtlApps.isEmpty()) {
                checkApps.addAll(addtlApps);
            }

            for (String c : checkApps) {
                Log.info(thisClass, thisMethod, "Loop at end: " + c);
            }

            // libertyinternals-1.0.mf is not needed for RP tests
            if (!testType.equals(Constants.OIDC_RP)) {
                if (LibertyFileManager.libertyFileExists(aTestServer.getServer().getMachine(), aTestServer.getServer().pathToAutoFVTTestFiles
                                                                                               + "/internalFeatures/securitylibertyinternals-1.0.mf")) {
                    aTestServer.getServer().copyFileToLibertyInstallRoot("lib/features", "internalFeatures/securitylibertyinternals-1.0.mf");
                }
            }
            if (!secStartMsg) {
                aTestServer.setSkipSecurityReadyMsg();
            }

            // set properties in bootstrap.properties on every setup - not only when we're switching/altering the token type
            if (testType.equals(Constants.GENERIC_SERVER) || testType.equals(Constants.OIDC_RP)) {
                aTestServer.addValidationEndpointProps(providerType, tokenType, certType, jwkValidationMap, classOverrideValidationEndpointValue);
            } else {
                aTestServer.addOPJaxRSProps(providerType, tokenType, certType);
            }

            if (providerType != null) { // and not ISAM ????
                aTestServer.addProviderProps(providerType);
            }

            if (miscBootstrapParms != null) {
                aTestServer.addMiscBootstrapParms(miscBootstrapParms);
            }

            if (testType.equals(Constants.IDP_SERVER_TYPE)) {
                // we're having an issue with the in memory LDAP server on z/OS, added a method to see if it can accept requests,
                // if NOT, we'll use a "external" LDAP server (Shibboleth allows for failover to additional LDAP servers, but,
                // it doesn't allow different bindDN, bindPassword, ...)
                // this method will add properties to bootstrap.properties that will point to a hopefully working LDAP server
                usingExternalLDAPServer = shibbolethHelpers.updateToUseExternalLDaPIfInMemoryIsBad(aTestServer);
                shibbolethHelpers.setShibbolethPropertiesForTestMachine(aTestServer);
            }

            // if we need an IDP and we've already started one
            if (pickAnIDP && testIDPServer != null) {
                setSelectedTfimServer(aTestServer, testIDPServer);
                //                aTestServer.addMiscBootstrapParms(mapFromArrays({"",""}, {"",""}));
                Map<String, String> parmMap = new HashMap<String, String>();
                parmMap.put("idpPort", testIDPServer.getHttpDefaultPort().toString());
                parmMap.put("idpSecurePort", testIDPServer.getHttpDefaultSecurePort().toString());
                aTestServer.addMiscBootstrapParms(parmMap);
                pickAnIDP = false;
            }

            // if test class is using LDAP, or we're configuring an IDP server, we'll need the ldap vars in bootstrap.properties
            if (useLdap || testType.equals(Constants.IDP_SERVER_TYPE)) {
                Log.info(thisClass, thisMethod, "calling LDAPUtil.addLDAPVariables", null);
                LDAPUtils.addLDAPVariables(aTestServer.getServer());
                Log.info(thisClass, thisMethod, "called LDAPUtil.addLDAPVariables", null);
            }
            if (!useDerby && useMongo && !(serverXML.contains("Bell") || serverXML.contains("bell"))) {
                Log.info(thisClass, "commonSetUp", "Add CustomStore user feature");
                aTestServer.getServer().installUserBundle("security.custom.store_1.0");
                aTestServer.getServer().installUserFeature("customStoreSample-1.0");
            }

            addToServerRefList(aTestServer);
            installUserFeature(aTestServer); // for installing user feature
            if (skipServerStart) {
                Log.info(thisClass, "setupBeforeTest", "/****************************** SKIP ACTUAL SERVER START ********************************/");
                // reset for the next server
                skipServerStart = false;
            } else {
                if (sslCheck) {
                    aTestServer.startServer(serverXML, checkApps, messages, null, reportViaJunit);
                } else {
                    aTestServer.startServer(serverXML, checkApps, messages, "skip", reportViaJunit);
                }
            }

            boolean overrideForConsul = false;
            if (!useDerby && useMongo && JavaInfo.JAVA_VERSION == 7) { // Added Java 7 back in, need to do this to connect to Consul for MongoDB
                overrideForConsul = true;
            }

            setupSSLClient(overrideForConsul);

            /*
             * reconfigExpectations = new FATDataHelpers().addExpectation(null,
             * null, Constants.MESSAGES_LOG, null, null,
             * Constants.MSG_OPENID_CONFIG_MODIFIED);
             */
            initHttpUnitSettings();

            defaultPort = aTestServer.getHttpDefaultPort();
            httpString = aTestServer.getHttpString();
            httpsString = aTestServer.getHttpsString();

            aTestServer.setServerHttpPort(aTestServer.getHttpDefaultPort());
            aTestServer.setServerHttpsPort(aTestServer.getHttpDefaultSecurePort());

            // right now, our IDP's can only handle specific ports
            // (our partners have to be registered in an external server)
            // error out if we get any other ports - tests will fail for reasons
            // that are
            // not always clear
            //            if (validPortNumbers != null) {
            //                String currentHttpsPort = Integer.toString(aTestServer.getHttpDefaultSecurePort());
            //                if (!validationTools.isInList(validPortNumbers, currentHttpsPort)) {
            //                    testHelpers.logDebugInfo();
            //                    throw new Exception("These tests use external IDP servers.  Those external servers have entries that include the https port used by the SP server on this test machine.  Therefore, these tests require the use of ports " + Arrays.toString(validPortNumbers) + " ONLY.  This execution is using port: " + currentHttpsPort);
            //                }
            //            }

            setActionsForServerType();

            // define the testSettings that are appropriate for the test type
            // either setup OAuth, or OpenID Connect
            if (testType.equals(Constants.OAUTH_OP)) {
                testSettings.setDefaultOAuthOPTestSettings(httpString, httpsString);
                eSettings.setOAUTHOPTestType();
                //                helpers.setOPServer(aTestServer);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    testSettings.setDefaultOIDCOPTestSettings(httpString, httpsString);
                    eSettings.setOIDCOPTestType();
                    //                    helpers.setOPServer(aTestServer);
                } else {
                    if (testType.equals(Constants.OIDC_RP)) {
                        testSettings.setDefaultRPTestSettings(httpString, httpsString, targetUrl, providerType);
                        // eSettings.setOAUTHTestType() ;
                        //                        helpers.setRPServer(aTestServer);
                    } else {
                        if (testType.equals(Constants.JWT_CONSUMER)) {
                            testSettings.setDefaultJwtConsumerTestSettings(httpString, httpsString);
                            //                            helpers.setGenericTestServer(aTestServer);
                        } else {
                            if (testType.equals(Constants.GENERIC_SERVER)) {
                                // testSettings.setDefaultRPTestSettings(httpString,
                                // httpsString, targetUrl, providerType);
                                testSettings.setDefaultGenericTestSettings(httpString, httpsString);
                                //                                helpers.setGenericTestServer(aTestServer);
                                setActionsForServerType(providerType, tokenType);
                            } else {
                                if (testType.equals(Constants.IDP_SERVER_TYPE)) {
                                    // nothing???
                                } else {
                                    throw new RuntimeException("Unknown testType:" + testType + " Can not determine defaults to set");
                                }
                            }
                        }
                    }
                }
            }

            if (useDerby) {
                DerbyUtils.setupDerbyEntries(httpString, defaultPort);
                aTestServer.setUsingDerby(true);
            } else if (useMongo) {
                setupMongoDBConfig(aTestServer, httpString, defaultPort);
                aTestServer.setUsingMongoDB(true);
            }
            if (useDerby && useMongo) { // while not ideal, setting two variables seemed to have the lowest code churn when the MongoDB option was added.
                Log.info(thisClass, "setupBeforeTest", "Both useDerby and useMongo were set to true. Derby will be used.");
            }
        } catch (Exception e) {
            // if we fail setting up the server, we don't know how far it may
            // have gotten
            // the server may be running - since we're not returning the
            // TestServer object
            // to the caller the AfterClass method won't have the reference to
            // use to stop the server
            // if we eat the failure, the tests will go on and fail and waste
            // time instead of
            // erroring out...
            try {
                // don't update the allowed timeout count for retrys - we're failing and exiting - report as many failures as possible
                tearDownServer(aTestServer);
            } catch (Exception e2) {
                e2.printStackTrace(System.out);
            }
            throw e;
        }

        // initialize flags for conditional ignores and set other token specific
        // values
        if (tokenType.equals(Constants.JWT_TOKEN) || tokenType.equals(Constants.MP_JWT_TOKEN) || tokenType.equals(Constants.BUILT_JWT_TOKEN)) {
            Log.info(thisClass, "setupBeforeTest", "Fix settings for JWT_TOKEN");
            RSSkipIfJWTToken.useJWT = true;
            RSSkipIfAccessToken.useAccessToken = false;
            testSettings = helpers.fixProviderInUrls(testSettings, "providers", "endpoint");
        } else {
            Log.info(thisClass, "setupBeforeTest", "Fix settings for ACCESS_TOKEN");
            RSSkipIfJWTToken.useJWT = false;
            RSSkipIfAccessToken.useAccessToken = true;
        }
        testSettings.setRsTokenType(tokenType);
        Log.info(thisClass, "setupBeforeTest", "RSTokenType: " + testSettings.getRsTokenType());

        if (certType == null) {
            testSettings.setRsCertType(Constants.X509_CERT);
        } else {
            testSettings.setRsCertType(certType);
            if (certType.equals(Constants.X509_CERT)) {
                testSettings.setSignatureAlg(Constants.SIGALG_HS256);
            } else {
                testSettings.setSignatureAlg(Constants.SIGALG_RS256);
            }
        }
        if (testSettings.getRsCertType().equals(Constants.JWK_CERT)) {
            testSettings.setSignatureAlg(Constants.SIGALG_RS256);
        }

        return aTestServer;

    }

    /**
     * Add the newly added server to the list of server references
     *
     * @param server
     *            - server reference to add
     * @throws Exception
     */
    private static void addToServerRefList(TestServer server) throws Exception {

        serverRefList.add(server);
        helpers.addToServerRefList(server);

    }

    /**
     * Perform setup for testing with SSL connections: TrustManager, hostname
     * verifier, ...
     */
    private static void setupSSLClient(boolean overrideForConsul) {

        String thisMethod = "setupSSLCLient";

        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "Setting up global trust");

        /*
         * TODO BEGIN DELETE
         *
         * This block of code was added to support DSA keys until they are replaced
         * in our tests.
         */
        String protocols = "SSLv3,TLSv1";
        if (JavaInfo.JAVA_VERSION >= 8 || overrideForConsul)
            protocols += ",TLSv1.1,TLSv1.2";

        System.setProperty("com.ibm.jsse2.disableSSLv3", "false");
        System.setProperty("https.protocols", protocols);
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        Log.info(thisClass, "enableSSLv3", "Enabled SSLv3.  https.protocols=" + protocols);
        /*
         * TODO END DELETE
         */

        try {
            KeyManager keyManagers[] = null;

            // if the System.Properties already set up the keystore, initialize
            // it
            String ksPath = System.getProperty("javax.net.ssl.keyStore");
            if (ksPath != null && ksPath.length() > 0) {
                String ksPassword = System
                                .getProperty("javax.net.ssl.keyStorePassword");
                String ksType = System
                                .getProperty("javax.net.ssl.keyStoreType");
                Log.info(thisClass, "setup Keymanager", "ksPath=" + ksPath + " ksPassword=" + ksPassword + " ksType=" + ksType);
                if (ksPassword != null && ksType != null) {
                    KeyManagerFactory kmFactory = KeyManagerFactory
                                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());

                    File ksFile = new File(ksPath);
                    KeyStore keyStore = KeyStore.getInstance(ksType);
                    FileInputStream ksStream = new FileInputStream(ksFile);
                    keyStore.load(ksStream, ksPassword.toCharArray());

                    kmFactory.init(keyStore, ksPassword.toCharArray());
                    keyManagers = kmFactory.getKeyManagers();
                }
            }

            // Create a trust manager that does not validate certificate chains
            /* */
            trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    Log.info(thisClass, "checkClientTrusted", "In server trust check");
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    Log.info(thisClass, "checkServerTrusted", "In server trust check");
                }
            } };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                            .setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            @SuppressWarnings("unused")
            HostnameVerifier allHostsValid = new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            /* setup jdk ssl */
            Log.info(thisClass, thisMethod, "Setting trustStore to " + Constants.JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStore", Constants.JKS_LOCATION);
            System.setProperty("javax.net.ssl.trustStorePassword",
                               // "changeit");
                               "LibertyClient");
            System.setProperty("javax.net.debug", "ssl");
            Log.info(thisClass, thisMethod, "javax.net.debug is set to: " + System.getProperty("javax.net.debug"));

        } catch (Exception e) {
            Log.info(thisClass, "static initializer", "Unable to set default TrustManager", e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        } finally {
            System.setProperty("javax.net.ssl.keyStore", ""); // reset the
                                                              // System
                                                              // property to
                                                              // empty string
                                                              // on keyStore
                                                              // settings for
                                                              // next test
                                                              // suite
        }

    }

    /**
     * Sets httpunit options that all the tests need
     *
     * @param providerType
     *            - The type of provider that this test instance will use
     */
    private static void initHttpUnitSettings() {

        String thisMethod = "initHttpUnitSettings";
        msgUtils.printMethodName(thisMethod);

        /*
         * We need to make sure that scripting is enabled, but that we don't let
         * httpunit try to parse the java script embedded in the forms - that
         * doesn't seem to work all that well
         */
        HttpUnitOptions.setScriptingEnabled(true);
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(false);
        HttpUnitOptions.setLoggingHttpHeaders(true);

    }

    /**
     * 1) This method is to allow junit testing client to provide
     * clientCertificateAuthentication during SSL handshake Since the
     * certAuthentication attribute in configuration is implemented with default
     * value set to false, the clientCertificate is ignored. And the test
     * results are the same as before in theory. 2) Once the ssl in the client
     * initialized in a single test class, we got into troubles to update its
     * settings again
     */
    protected static void setupSSLClientKeyStore(String keyFile, String password, String type) {
        String thisMethod = "setupSSLCLientKeyStore";
        /* setup jdk ssl */
        Log.info(thisClass, thisMethod, "Setting keyStore to " + keyFile);
        System.setProperty("javax.net.ssl.keyStore", keyFile);
        System.setProperty("javax.net.ssl.keyStorePassword", password);
        System.setProperty("javax.net.ssl.keyStoreType", type);
    }

    /**
     * Set up the local ACTION variables based on the server type The defaults
     * are the steps/actions used with oidc/oauth, but some tests can run with
     * those servers, or an ISAM server and for ISAM, the steps/actions will be
     * different - this method lets us override the defaults
     *
     */
    public static void setActionsForServerType() throws Exception {
        setActionsForServerType(Constants.OIDC_OP, Constants.ACCESS_TOKEN_KEY);
    }

    public static void setActionsForServerType(String serverType, String tokenType) throws Exception {

        // actions are
        if (serverType != null && serverType.equals(Constants.ISAM_OP)) {
            GET_TOKEN_ACTIONS = Constants.BASIC_CREATE_JWT_TOKEN_ACTIONS;
            GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_WITH_JWT_ACTIONS;
            RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_JWT_RS_PROTECTED_RESOURCE_ACTIONS;
            LOGIN_USER = Constants.LOGIN_USER;
            PERFORM_LOGIN = Constants.PERFORM_ISAM_LOGIN;
            RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_JWT_RS_PROTECTED_RESOURCE_ACTIONS;
        } else {
            GET_TOKEN_ACTIONS = Constants.BASIC_AUTHENTICATION_ACTIONS;
            RS_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
            LOGIN_USER = Constants.LOGIN_USER;
            PERFORM_LOGIN = Constants.PERFORM_LOGIN;
            // when the token type is JWT, we can't invoke the app without using
            // the RS server, so skip those steps
            if (tokenType.equals(Constants.ACCESS_TOKEN_KEY)) {
                GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_PROTECTED_RESOURCE_ACTIONS;
                RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
            } else {
                GET_TOKEN_AND_INVOKE_APP_ACTIONs = Constants.BASIC_AUTHENTICATION_ACTIONS;
                RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;
            }
        }

    }

    /**
     * Generic steps to invoke methods (in the correct ordering) to test with
     * the OP server
     *
     * @param testcase
     * @param wc
     * @param settings
     * @param testActions
     * @param expectations
     * @return
     * @throws Exception
     */

    public WebResponse genericOP(String testcase, WebConversation wc, TestSettings settings, String[] testActions, List<validationData> expectations) throws Exception {

        return genericOP(testcase, wc, settings, testActions, expectations, null, null);

    }

    public WebResponse genericOP(String testcase, WebConversation wc, TestSettings settings, String[] testActions, List<validationData> expectations, WebResponse response,
                                 String idToken) throws Exception {

        // WebResponse response = null;
        String thisMethod = "genericOP";
        msgUtils.printMethodName(thisMethod);

        //        settings.printTestSettings();
        if (settings != null && settings.getAllowPrint()) {
            msgUtils.printOAuthOidcExpectations(expectations, testActions);
        }

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_OAUTH_CLIENT)) {
                response = helpers.invokeFirstClient(testcase, wc, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER)) {
                response = helpers.submitToAuthServer(testcase, wc, response, settings, expectations, Constants.SUBMIT_TO_AUTH_SERVER);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_ENDPOINT)) {
                response = invokeAuthorizationEndpoint(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.BUILD_POST_SP_INITIATED_REQUEST)) {
                response = buildPostSolicitedSPInitiatedRequest(testcase, wc, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH)) {
                response = invokeAuthorizationEndpoint(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH);
            }

            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER_FOR_TOKEN)) {
                response = helpers.submitToAuthServerForToken(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH)) {
                response = helpers.submitToAuthServer(testcase, wc, response, settings, expectations, Constants.SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_SERVER)) {
                response = helpers.invokeAuthServer(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER);
            }
            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH)) {
                response = helpers.invokeAuthServer(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH);
            }

            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN)) {
                response = helpers.performIDPLogin(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.PERFORM_LOGIN)) {
                response = helpers.performLogin(testcase, wc, response, settings, expectations);
            }
            if (validationTools.isInList(testActions, Constants.PERFORM_ISAM_LOGIN)) {
                response = helpers.performISAMLogin(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_ACS)) {
                response = helpers.invokeACS(testcase, wc, response, settings, expectations);
            }

            String logoutIdToken = null;
            // if we're going to be calling logout later, we need to save off
            // the idToken
            // it won't be in the response after we invoke the protected
            // resource
            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                // If we have a response, get id token from response
                if (response != null) {
                    logoutIdToken = validationTools.getIDToken(settings, response);
                } else {
                    Log.info(thisClass, thisMethod, "using supplied id_token");
                    logoutIdToken = idToken;
                }
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT)) {
                response = invokeTokenEndpoint(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT_CL_CRED)) {
                response = invokeTokenEndpoint_clientCredentials(testcase, wc, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT_PASSWORD)) {
                response = invokeTokenEndpoint_password(testcase, wc, settings, expectations);
            }

            WebResponse beforeResourceResponse = response;
            if (validationTools.isInList(testActions, Constants.INVOKE_PROTECTED_RESOURCE)) {
                response = helpers.invokeProtectedResource(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_RS_PROTECTED_RESOURCE)) {
                response = helpers.invokeRsProtectedResource(testcase, wc, beforeResourceResponse, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                response = helpers.processEndSession(testcase, wc, logoutIdToken, settings, expectations);
            }

            return response;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    public Object genericOP(String testcase, WebClient webClient, TestSettings settings, String[] testActions, List<validationData> expectations) throws Exception {
        return genericOP(testcase, webClient, settings, testActions, expectations, null, null);
    }

    public Object genericOP(String testcase, WebClient webClient, TestSettings settings, String[] testActions, List<validationData> expectations, Object response,
                            String idToken) throws Exception {

        //        return genericOP(testcase, webClient, settings, testActions, expectations, null, null);
        //
        //    }
        //
        //    public WebResponse genericOP(String testcase, WebClient webClient, TestSettings settings, String[] testActions, List<validationData> expectations, WebResponse response, String idToken) throws Exception {

        // WebResponse response = null;
        String thisMethod = "genericOP";
        msgUtils.printMethodName(thisMethod);

        //        settings.printTestSettings();
        msgUtils.printOAuthOidcExpectations(expectations, testActions);

        Object thePage = null;

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_OAUTH_CLIENT)) {
                thePage = helpers.invokeFirstClient(testcase, webClient, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER)) {
                thePage = helpers.submitToAuthServer(testcase, webClient, thePage, settings, expectations, Constants.SUBMIT_TO_AUTH_SERVER);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_ENDPOINT)) {
                thePage = invokeAuthorizationEndpoint(testcase, webClient, thePage, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.BUILD_POST_SP_INITIATED_REQUEST)) {
                thePage = buildPostSolicitedSPInitiatedRequest(testcase, webClient, settings, expectations);
            }

            //            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_ENDPOINT_WITH_BASIC_AUTH)) {
            //                response = invokeAuthorizationEndpoint(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH);
            //            }
            //
            //            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER_FOR_TOKEN)) {
            //                response = helpers.submitToAuthServerForToken(testcase, wc, response, settings, expectations);
            //            }
            //
            //            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH)) {
            //                response = helpers.submitToAuthServer(testcase, wc, response, settings, expectations, Constants.SUBMIT_TO_AUTH_SERVER_WITH_BASIC_AUTH);
            //            }
            //
            //            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_SERVER)) {
            //                response = helpers.invokeAuthServer(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER);
            //            }
            //            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH)) {
            //                response = helpers.invokeAuthServer(testcase, wc, response, settings, expectations, Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH);
            //            }

            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN)) {
                thePage = helpers.performIDPLogin(testcase, webClient, (HtmlPage) thePage, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.PERFORM_LOGIN)) {
                thePage = helpers.performLogin(testcase, webClient, thePage, settings, expectations);
            }
            //            if (validationTools.isInList(testActions, Constants.PERFORM_ISAM_LOGIN)) {
            //                response = helpers.performISAMLogin(testcase, wc, response, settings, expectations);
            //            }

            if (validationTools.isInList(testActions, Constants.INVOKE_ACS)) {
                thePage = helpers.invokeACS(testcase, webClient, thePage, settings, expectations);
            }

            String logoutIdToken = null;
            // if we're going to be calling logout later, we need to save off
            // the idToken
            // it won't be in the response after we invoke the protected
            // resource
            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                // If we have a response, get id token from response
                if (response != null) {
                    logoutIdToken = validationTools.getIDToken(settings, response);
                } else {
                    Log.info(thisClass, thisMethod, "using supplied id_token");
                    logoutIdToken = idToken;
                }
            }
            //
            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT)) {
                thePage = invokeTokenEndpoint(testcase, webClient, thePage, settings, expectations);
            }

            //            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT_CL_CRED)) {
            //                response = invokeTokenEndpoint_clientCredentials(testcase, wc, settings, expectations);
            //            }
            //
            //            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT_PASSWORD)) {
            //                response = invokeTokenEndpoint_password(testcase, wc, settings, expectations);
            //            }
            //
            Object beforePage = thePage;
            if (validationTools.isInList(testActions, Constants.INVOKE_PROTECTED_RESOURCE)) {
                thePage = helpers.invokeProtectedResource(testcase, webClient, thePage, settings, expectations);
            }

            //            if (validationTools.isInList(testActions, Constants.INVOKE_RS_PROTECTED_RESOURCE)) {
            //                response = helpers.invokeRsProtectedResource(testcase, wc, beforeResourceResponse, settings, expectations);
            //            }
            //
            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                response = helpers.processLogout(testcase, webClient, logoutIdToken, settings, expectations);
            }

            return thePage;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    /**
     * Generic steps to invoke methods (in the correct ordering) to test with
     * the RP and OP servers
     *
     * @param testcase
     * @param wc
     * @param settings
     * @param testActions
     * @param expectations
     * @return
     * @throws Exception
     */
    public WebResponse genericRP(String testcase, WebConversation wc, TestSettings settings, String[] testActions, List<validationData> expectations) throws Exception {

        WebResponse response = null;
        String thisMethod = "genericRP";
        msgUtils.printMethodName(thisMethod);

        //        settings.printTestSettings();
        msgUtils.printOAuthOidcExpectations(expectations);

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            if (validationTools.isInList(testActions, Constants.GET_LOGIN_PAGE)) {
                response = helpers.getLoginPage(testcase, wc, settings, expectations);
            }
            if (validationTools.isInList(testActions, Constants.POST_LOGIN_PAGE)) {
                response = helpers.postLoginPage(testcase, wc, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.SPECIFY_PROVIDER)) {
                response = helpers.processOpenIdForm(response, wc, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGIN_USER)) {
                response = helpers.processProviderLoginForm(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN)) {
                response = helpers.performIDPLogin(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_ACS)) {
                response = helpers.invokeACS(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.GET_RP_CONSENT)) {
                response = helpers.processRPConsent(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGIN_AGAIN)) {
                response = helpers.processRPRequestAgain(testcase, wc, response, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                response = helpers.processEndSession(testcase, wc, validationTools.getIDToken(settings, response), settings, expectations);
            }

            return response;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    /**
     * Generic steps to invoke methods (in the correct ordering) to test with
     * the RP and OP servers
     *
     * *************** NOTE ***************
     * This method contains a subset of the function from the original genericRp. The methods that interact with SAML had to be
     * modified to use HtmlUnit instead of HttpUnit
     * when we switched to using Shibboleth. So, this method only processes a flow using SAMl and uses a WebClient instead of a
     * WebConversation, ...
     *
     * @param testcase
     * @param wc
     * @param settings
     * @param testActions
     * @param expectations
     * @return
     * @throws Exception
     */
    public Object genericRP(String testcase, WebClient webClient, TestSettings settings, String[] testActions, List<validationData> expectations) throws Exception {

        Object thePage = null;
        String thisMethod = "genericRP";
        msgUtils.printMethodName(thisMethod);

        //        settings.printTestSettings();
        msgUtils.printOAuthOidcExpectations(expectations);

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            if (validationTools.isInList(testActions, Constants.GET_LOGIN_PAGE)) {
                thePage = helpers.getLoginPage(testcase, webClient, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.POST_LOGIN_PAGE)) {
                thePage = helpers.postLoginPage(testcase, webClient, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGIN_USER)) {
                thePage = helpers.processProviderLoginForm(testcase, webClient, (HtmlPage) thePage, Constants.LOGIN_USER, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN)) {
                thePage = helpers.performIDPLogin(testcase, webClient, (HtmlPage) thePage, settings, expectations);
            }
            // with java script enabled for negative samesite tests, we run into an OOM issue with htmlunit
            // disable javascript for this step (caller indicates by specifying this step)
            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN_JAVASCRIPT_DISABLED)) {
                boolean origSetting = webClient.getOptions().isJavaScriptEnabled();
                webClient.getOptions().setJavaScriptEnabled(false);
                thePage = helpers.performIDPLogin(testcase, webClient, (HtmlPage) thePage, settings, expectations);
                webClient.getOptions().setJavaScriptEnabled(origSetting);
            }

            if (validationTools.isInList(testActions, Constants.INVOKE_ACS)) {
                thePage = helpers.invokeACS(testcase, webClient, thePage, settings, expectations);
            }

            if (validationTools.isInList(testActions, Constants.LOGOUT)) {
                thePage = helpers.processLogout(testcase, webClient, validationTools.getIDToken(settings, thePage), settings, expectations);
            }

            return thePage;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    /**
     * Generic steps to invoke and endpoint
     *
     * @param testcase
     * @param wc
     * @param inResponse
     * @param url
     * @param method
     * @param action
     * @param parms
     * @param headers
     * @param expectations
     * @param settings
     *            - optional
     * @param skipTestCase
     *            - optional
     * @return
     * @throws Exception
     */
    /*
     * test settings and skiptestcase not specified - pass in the defaults of
     * null and false
     */
    public WebResponse genericInvokeEndpoint(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                             List<endpointSettings> headers, List<validationData> expectations) throws Exception {

        return genericInvokeEndpoint(testcase, wc, inResponse, url, method, action, parms, headers, expectations, null, false);
    }

    /*
     * test settings passed in, but skiptestcase not specified - pass in the
     * default of false
     */
    public WebResponse genericInvokeEndpoint(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                             List<endpointSettings> headers, List<validationData> expectations, TestSettings settings) throws Exception {

        return genericInvokeEndpoint(testcase, wc, inResponse, url, method, action, parms, headers, expectations, settings, false);
    }

    /* test settings not specified - pass in the default of null */
    public WebResponse genericInvokeEndpoint(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                             List<endpointSettings> headers, List<validationData> expectations, boolean skipTestCase) throws Exception {

        return genericInvokeEndpoint(testcase, wc, inResponse, url, method, action, parms, headers, expectations, null, skipTestCase);
    }

    public WebResponse genericInvokeEndpoint(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                             List<endpointSettings> headers, List<validationData> expectations, TestSettings settings, boolean skipTestCase) throws Exception {

        WebRequest request = null;
        WebResponse response = null;
        String thisMethod = "genericInvokeEndpoint";

        msgUtils.printMethodName(thisMethod);
        if (settings != null && settings.getAllowPrint()) {
            settings.printTestSettings();
            msgUtils.printOAuthOidcExpectations(expectations);
        }

        try {

            // WebConversation wc = new WebConversation();

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + url);

            if (method.equals(Constants.GETMETHOD)) {
                request = new GetMethodWebRequest(url);
            } else {
                if (method.equals(Constants.POSTMETHOD)) {
                    request = new PostMethodWebRequest(url);
                } else {
                    if (method.equals(Constants.HEADMETHOD)) {
                        request = new HeadMethodWebRequest(url);
                    }
                }
            }

            if (parms != null) {
                for (endpointSettings parm : parms) {
                    if (parm.value != null) {
                        Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                        request.setParameter(parm.key, parm.value);
                    }
                }
                if (skipTestCase) {
                    Log.info(thisClass, thisMethod, "NOT sending testCase name as a parm");
                } else {
                    Log.info(thisClass, thisMethod, "Setting request parameter:  key: testCase value: " + testcase);
                    request.setParameter("testCase", testcase);
                }
            } else {
                Log.info(thisClass, thisMethod, "No parameters to set");
            }

            if (headers != null) {
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                    request.setHeaderField(header.key, header.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }
            Log.info(thisClass, thisMethod, "Making request: " + request.toString());
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Invoke with Parms and Headers: ");
        } catch (HttpException e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception response code: " + e.getResponseCode());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getResponseMessage());
            Log.info(thisClass, thisMethod, "Exception Cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception stack: " + e.getStackTrace());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getLocalizedMessage());
            Log.info(thisClass, thisMethod, "Exception cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        }

        validationTools.validateResult(response, action, expectations, settings);
        return response;
    }

    public Object genericInvokeEndpoint(String testcase, WebClient webClient, Object startPage, String url, String method, String action, List<endpointSettings> parms,
                                        List<endpointSettings> headers, List<validationData> expectations, TestSettings settings) throws Exception {

        return genericInvokeEndpoint(testcase, webClient, startPage, url, method, action, parms, headers, expectations, settings, false);
    }

    /* test settings not specified - pass in the default of null */
    public Object genericInvokeEndpoint(String testcase, WebClient webClient, Object startPage, String url, String method, String action, List<endpointSettings> parms,
                                        List<endpointSettings> headers, List<validationData> expectations, boolean skipTestCase) throws Exception {

        return genericInvokeEndpoint(testcase, webClient, startPage, url, method, action, parms, headers, expectations, null, skipTestCase);
    }

    public Object genericInvokeEndpoint(String testcase, WebClient webClient, Object startPage, String inUrl, String method, String action, List<endpointSettings> parms,
                                        List<endpointSettings> headers, List<validationData> expectations, TestSettings settings, boolean skipTestCase) throws Exception {

        String thisMethod = "genericInvokeEndpoint";
        com.gargoylesoftware.htmlunit.WebRequest requestSettings = null;
        Page thePage = null;

        msgUtils.printMethodName(thisMethod);
        if (settings != null && settings.getAllowPrint()) {
            settings.printTestSettings();
            msgUtils.printOAuthOidcExpectations(expectations);
        }

        try {

            // WebConversation wc = new WebConversation();

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + inUrl);
            URL url = AutomationTools.getNewUrl(inUrl);

            HttpMethod selectedMethod = null;

            switch (method) {
                case Constants.DELETEMETHOD:
                    selectedMethod = HttpMethod.DELETE;
                    break;
                case Constants.GETMETHOD:
                    selectedMethod = HttpMethod.GET;
                    break;
                case Constants.HEADMETHOD:
                    selectedMethod = HttpMethod.HEAD;
                    break;
                case Constants.OPTIONSMETHOD:
                    selectedMethod = HttpMethod.OPTIONS;
                    break;
                case Constants.PATCHMETHOD:
                    selectedMethod = HttpMethod.PATCH;
                    break;
                case Constants.POSTMETHOD:
                    selectedMethod = HttpMethod.POST;
                    break;
                case Constants.PUTMETHOD:
                    selectedMethod = HttpMethod.PUT;
                    break;
                case Constants.TRACEMETHOD:
                    selectedMethod = HttpMethod.TRACE;
                    break;
                default:
                    selectedMethod = HttpMethod.GET;
                    break;
            }

            requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, selectedMethod);

            if (parms != null) {
                ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
                for (endpointSettings parm : parms) {
                    if (parm.value != null) {
                        Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                        params.add(new NameValuePair(parm.key, parm.value));
                    }
                }
                if (skipTestCase) {
                    Log.info(thisClass, thisMethod, "NOT sending testCase name as a parm");
                } else {
                    Log.info(thisClass, thisMethod, "Setting request parameter:  key: testCase value: " + testcase);
                    params.add(new NameValuePair("testCase", testcase));
                }
                requestSettings.setRequestParameters(params);
            } else {
                Log.info(thisClass, thisMethod, "No parameters to set");
            }

            if (headers != null) {
                Map<String, String> headerParms = new HashMap<String, String>();
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                    headerParms.put(header.key, header.value);
                }
                requestSettings.setAdditionalHeaders(headerParms);
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }

            Log.info(thisClass, thisMethod, "isRedirectEnabled: " + webClient.getOptions().isRedirectEnabled());
            Log.info(thisClass, thisMethod, "isJavaScriptEnabled: " + webClient.getOptions().isJavaScriptEnabled());
            Log.info(thisClass, thisMethod, "isDoNotTrackEnabled: " + webClient.getOptions().isDoNotTrackEnabled());
            webClient.waitForBackgroundJavaScript(2000);
            webClient.setJavaScriptTimeout(10000);

            Log.info(thisClass, thisMethod, "Request: " + requestSettings.toString());
            //            thePage = webClient.getCurrentWindow().getEnclosedPage();
            thePage = webClient.getPage(requestSettings);

            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, thisMethod, "Invoke with Parms and Headers: ");
        } catch (HttpException e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception response code: " + e.getResponseCode());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getResponseMessage());
            Log.info(thisClass, thisMethod, "Exception Cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception stack: " + e.getStackTrace());
            Log.info(thisClass, thisMethod, "Exception response message: " + e.getLocalizedMessage());
            Log.info(thisClass, thisMethod, "Exception cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        }

        validationTools.validateResult(thePage, action, expectations, settings);
        return thePage;
    }

    /**
     * Generic steps to invoke and endpoint
     *
     * @param testcase
     * @param wc
     * @param inResponse
     * @param url
     * @param action
     * @param parms
     * @param headers
     * @param expectations
     * @param requestBody
     * @param contentType
     * @return
     * @throws Exception
     */
    public WebResponse invokeEndpointWithBody(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                              List<endpointSettings> headers, List<validationData> expectations, InputStream source, String contentType) throws Exception {

        MessageBodyWebRequest request = null;
        if (method.equals(Constants.POSTMETHOD)) {
            request = new PostMethodWebRequest(url, source, contentType);
        } else {
            if (method.equals(Constants.PUTMETHOD)) {
                request = new PutMethodWebRequest(url, source, contentType);
            }
        }
        WebResponse response = null;
        String thisMethod = "invokeEndpointWithBody";

        msgUtils.printMethodName(thisMethod);
        msgUtils.printOAuthOidcExpectations(expectations);
        Log.info(thisClass, thisMethod, "source: " + source);
        String s = IOUtils.toString(source, StandardCharsets.UTF_8);
        Log.info(thisClass, thisMethod, "source: " + s);

        try {

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + url);

            if (parms != null) {
                for (endpointSettings parm : parms) {
                    Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                    request.setParameter(parm.key, parm.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No parameters to set");
            }

            if (headers != null) {
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                    request.setHeaderField(header.key, header.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }

            msgUtils.printRequestParts(request, thisMethod, "outgoing " + method + " type request");
            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Invoke with Parms and Headers: ");

        } catch (HttpException e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception Response: " + e.getResponseCode());
            Log.info(thisClass, thisMethod, "Exception Stack: " + e.getStackTrace().toString());
            e.printStackTrace();
            Log.info(thisClass, thisMethod, "Exception Response message" + e.getResponseMessage());
            Log.info(thisClass, thisMethod, "Exception Cause: " + e.getCause());
            System.err.println("Exception: " + e);

            validationTools.validateException(expectations, action, e);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception Stack: " + e.getStackTrace());
            Log.info(thisClass, thisMethod, "Exception Response message" + e.getLocalizedMessage());
            Log.info(thisClass, thisMethod, "Exception Cause: " + e.getCause());

            validationTools.validateException(expectations, action, e);

        }

        validationTools.validateResult(response, action, expectations, null);
        return response;
    }

    /**
     * Generic routine to invoke and process a form
     *
     * @param testcase
     * @param wc
     * @param inResponse
     * @param url
     * @param method
     * @param action
     * @param parms
     * @param expectations
     * @return
     * @throws Exception
     */
    public WebResponse genericInvokeForm(String testcase, WebConversation wc, WebResponse inResponse, String url, String method, String action, List<endpointSettings> parms,
                                         List<validationData> expectations) throws Exception {
        return genericInvokeForm(testcase, wc, inResponse, null, url, method, action, parms, expectations);

    }

    public WebResponse genericInvokeForm(String testcase, WebConversation wc, WebResponse inResponse, TestSettings settings, String url, String method, String action,
                                         List<endpointSettings> parms, List<validationData> expectations) throws Exception {

        helpers.setMarkToEndOfAllServersLogs();

        WebRequest request = null;
        WebResponse response = null;
        String thisMethod = "genericInvokeForm";

        msgUtils.printMethodName(thisMethod);
        if (settings != null && settings.getAllowPrint()) {
            msgUtils.printOAuthOidcExpectations(expectations);
        }

        try {

            // WebConversation wc = new WebConversation();

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + url);

            if (method.equals(Constants.GETMETHOD)) {
                request = new GetMethodWebRequest(url);
            } else {
                if (method.equals(Constants.POSTMETHOD)) {
                    request = new PostMethodWebRequest(url);
                }
            }

            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, thisMethod, "Response from Form getResponse: ");

            // Process form only if we have a form in the response
            WebForm[] forms = response.getForms();

            if (forms.length >= 1) {

                // WebForm form = response.getForms()[0];
                WebForm form = forms[0];
                if (parms != null) {
                    for (endpointSettings parm : parms) {
                        Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                        form.setParameter(parm.key, parm.value);
                    }
                } else {
                    Log.info(thisClass, thisMethod, "No parameters to set");
                }

                // specify the null, 0,0 to work around a bug in httpunit where
                // it does a double submit under the covers
                response = form.submit(null, 0, 0);
                msgUtils.printAllCookies(wc);
                msgUtils.printResponseParts(response, thisMethod, "Response from Form Submit: ");
            }

            validationTools.validateResult(response, action, expectations, settings);

            return response;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }

    }

    public Object genericInvokeForm(String testcase, WebClient webClient, Object inResponse, String url, String method, String action, List<endpointSettings> parms,
                                    List<validationData> expectations) throws Exception {
        return genericInvokeForm(testcase, webClient, inResponse, null, url, method, action, parms, expectations);

    }

    public Object genericInvokeForm(String testcase, WebClient webClient, Object inResponse, TestSettings settings, String inUrl, String method, String action,
                                    List<endpointSettings> parms, List<validationData> expectations) throws Exception {

        helpers.setMarkToEndOfAllServersLogs();

        com.gargoylesoftware.htmlunit.WebRequest requestSettings = null;
        Page thePage = null;

        String thisMethod = "genericInvokeForm";

        msgUtils.printMethodName(thisMethod);
        if (settings != null && settings.getAllowPrint()) {
            msgUtils.printOAuthOidcExpectations(expectations);
        }

        msgUtils.printAllCookies(webClient);

        try {

            helpers.setMarkToEndOfAllServersLogs();

            Log.info(thisClass, thisMethod, "Endpoint URL: " + inUrl);

            URL url = AutomationTools.getNewUrl(inUrl);

            if (method.equals(Constants.GETMETHOD)) {
                requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);
            } else {
                if (method.equals(Constants.POSTMETHOD)) {
                    requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.POST);
                }
            }

            thePage = webClient.getPage(requestSettings);
            msgUtils.printResponseParts(thePage, thisMethod, "Response from Form getResponse: ");

            // Process form only if we have a form in the response
            List<HtmlForm> forms = ((HtmlPage) thePage).getForms();

            if (forms.size() >= 1) {

                final HtmlForm form = forms.get(0);

                if (parms != null) {
                    for (endpointSettings parm : parms) {
                        Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                        formTools.setAttr(form, parm.key, parm.value);
                    }
                } else {
                    Log.info(thisClass, thisMethod, "No parameters to set");
                }

                HtmlButton button1 = form.getButtonByName("submit");
                thePage = button1.click();
                msgUtils.printAllCookies(webClient);
                msgUtils.printResponseParts(thePage, thisMethod, "Response from Form Submit: ");
            }

            validationTools.validateResult(thePage, action, expectations, settings);

            return thePage;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }

    }

    /**
     * Goes through the genericOP flow if the common propagation token should be
     * refreshed or if the common token is null. This is intended to be called
     * by a @Before method in test classes that wish to use this functionality.
     * This allows individual tests that simply need a propagation token to
     * invoke the protected resource to use a valid token that has already
     * obtained. Individual tests then do not need to go through the entire
     * genericOP flow.
     */
    public void createCommonPropagationToken() {
        String method = "createCommonPropagationToken";
        if (shouldCommonTokenBeRefreshed() || commonPropagationToken == null) {
            Log.info(thisClass, method, "Obtaining a new common propagation token" + ((commonPropagationToken == null) ? " (common token was null)" : ""));

            try {
                WebResponse response = genericOP(_testName, new WebConversation(), testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, vData.addSuccessStatusCodes());
                // commonPropagationToken =
                // validationTools.getTokenFromResponse(response,
                // Constants.ACCESS_TOKEN_KEY);
                commonPropagationToken = validationTools.getTokenForType(testSettings, response);

                propagationTokenCreationDate = System.currentTimeMillis();
                DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
                Log.info(thisClass, method, "Common propagation token (" + commonPropagationToken + ") created at " + formatter.format(new Date(propagationTokenCreationDate))
                                            + " and will be refreshed in " + testPropagationTokenLifetimeSeconds + " second(s).");

            } catch (Exception e) {
                Log.error(thisClass, method, e, "Failed to obtain a common propagation token. Tests using the common token may fail.");
            }
        }
    }

    /**
     * Returns true if the difference between the current time and the common
     * token creation time exceeds the lifetime we specify in
     * testAccessTokenLifetimeSeconds.
     *
     * @return
     */
    private boolean shouldCommonTokenBeRefreshed() {
        String method = "shouldCommonTokenBeRefreshed";
        long currentTime = System.currentTimeMillis();
        if (((currentTime - propagationTokenCreationDate) / 1000) > testPropagationTokenLifetimeSeconds) {
            Log.info(thisClass, method, "Common token lifetime has exceeded allowed time; recommend a new token should be created.");
            return true;
        }
        return false;
    }

    /**
     * setup before running a test
     *
     * @throws Exception
     */
    @Before
    public void setTestName() throws Exception {
        _testName = testName.getMethodName();
        System.out.println("----- Start:  " + _testName + "   ----------------------------------------------------");
        msgUtils.printMethodName(_testName, "Starting TEST ");

        logTestCaseInServerSideLogs("STARTING");

    }

    /**
     * Clean up after running a test
     *
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @After
    public void endTest() throws Exception {

        try {
            logTestCaseInServerSideLogs("ENDING");
            for (TestServer server : serverRefList) {
                endServer(server);
            }

            msgUtils.printMethodName(_testName, "Ending TEST ");
            System.out.println("----- End:  " + _testName + "   ----------------------------------------------------");

            // if test case over write the autoredirect, reset it!
            if (OverrideRedirect) {
                HttpUnitOptions.setAutoRedirect(true);
                OverrideRedirect = false;
            }
            validPortNumbers = null;

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Generic routine to end a server (just make sure the server exists before
     * trying to end it)
     *
     * @param theServer
     * @throws Exception
     */
    private static void endServer(TestServer theServer) throws Exception {

        if (theServer != null) {
            theServer.endTestServer();
        }
    }

    /**
     * Clean up at the end of the test class
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        msgUtils.printMethodName("tearDown");
        List<TestServer> newServerRefList = new ArrayList<TestServer>();

        Exception cumulativeException = null;
        int exceptionNum = 0;
        boolean ranMongoTeardown = false;
        for (TestServer server : serverRefList) {
            if (!ranMongoTeardown && server.isUsingMongoDB) {
                ranMongoTeardown = mongoDBTeardownCleanup(server, cumulativeException, exceptionNum);
            }
            try {
                // update the allowedTimeout count to account for msgs issued during start retries
                addToAllowableTimeoutCount(server.getRetryTimeoutCount());
                tearDownServer(server);
                newServerRefList.add(server);
            } catch (Exception e) {
                // Typically caused by an unexpected error message in the logs; should re-throw the exception
                e.printStackTrace(System.out);
                String prevExceptionMsg = (cumulativeException == null) ? "" : cumulativeException.getMessage();
                cumulativeException = new Exception(prevExceptionMsg + " [Exception #" + (++exceptionNum) + "]: " + e.getMessage() + "\n<br>");
            }
        }
        if (cumulativeException != null) {
            throw cumulativeException;
        }

        for (TestServer server : newServerRefList) {
            serverRefList.remove(server);
            helpers.removeFromServerRefList(server);
        }

        try {
            uninstallUserFeature();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        timeoutChecker();

    }

    private static boolean mongoDBTeardownCleanup(TestServer server, Exception cumulativeException, int exceptionNum) {
        try {

            Log.info(thisClass, "mongoDBTeardownCleanup", "cleanupMongoDBEntries");
            MongoDBUtils.cleanupMongoDBEntries(server.getHttpString(), server.getHttpDefaultPort());

            try {
                Log.info(thisClass, "mongoDBTeardownCleanup", "delete mongo props file " + MONGO_PROPS_FILE);
                server.getServer().deleteFileFromLibertyServerRoot(MONGO_PROPS_FILE);
            } catch (Exception e) {
                Log.info(thisClass, "mongoDBTeardownCleanup", "Exception removing MONGO_PROPS_FILE. If this is a Derby test, ignore this message." + e);
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace(System.out);
            String prevExceptionMsg = (cumulativeException == null) ? "" : cumulativeException.getMessage();
            cumulativeException = new Exception(prevExceptionMsg + " [Exception #" + (++exceptionNum) + "]: " + e.getMessage() + "\n<br>");
        }
        return false;
    }

    /**
     * generic tear down of a server (makes sure the server exists first)
     *
     * @param theServer
     * @throws Exception
     */
    private static void tearDownServer(TestServer theServer) throws Exception {
        if (theServer != null) {
            theServer.tearDownServer();
        }
    }

    // ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Generic steps to invoke and endpoint
     *
     * @param testcase
     * @param wc
     * @param inResponse
     * @param url
     * @param method
     * @param action
     * @param parms
     * @param headers
     * @param expectations
     * @return
     * @throws Exception
     *
     *             WebResponse response =
     *             genericInvokeEndpointWithHttpUrlConn(_testName, connection,
     *             null, testSettings.getUserinfoEndpt(), Constants.POSTMETHOD,
     *             Constants.INVOKE_USERINFO_ENDPOINT, parms, null,
     *             expectations);
     **/
    public void genericInvokeEndpointWithHttpUrlConn(String testcase, String response, String url, String method, String action, List<endpointSettings> parms,
                                                     List<endpointSettings> headers, List<validationData> expectations) throws Exception {

        String thisMethod = "genericInvokeEndpointWithHttpUrlConn";

        msgUtils.printMethodName(thisMethod);
        msgUtils.printOAuthOidcExpectations(expectations);

        try {
            helpers.setMarkToEndOfAllServersLogs();

            String builtParms = buildParmString(parms);
            Log.info(thisClass, thisMethod, "request string: " + builtParms);

            String updatedUrl = url;
            // Delete doesn't support output - have to build the url with the query string included
            if ((Constants.DELETEMETHOD.equals(method) || Constants.PUTMETHOD.equals(method)) && builtParms != null) {
                updatedUrl = url + "?" + builtParms;
            }
            Log.info(thisClass, thisMethod, "Endpoint URL: " + updatedUrl);

            HttpURLConnection connection = prepareConnection(updatedUrl, method);

            if (headers != null) {
                for (endpointSettings header : headers) {
                    Log.info(thisClass, thisMethod, "Setting header field:  key: " + header.key + " value: " + header.value);
                    connection.setRequestProperty(header.key, header.value);
                }
            } else {
                Log.info(thisClass, thisMethod, "No header fields to add");
            }

            if ((parms != null) && (method != "GET")) {
                writeToConnection(connection, builtParms);
            } else {
                Log.info(thisClass, thisMethod, "No parameters to set");
            }

            connect(connection);
            response = processResponse(connection);

            validationTools.validateHttpUrlConnResult(connection, action, expectations, null, response);

        } catch (Exception e) {

            Log.info(thisClass, thisMethod, "Exception message: " + e.getMessage());
            Log.info(thisClass, thisMethod, "Exception Stack: " + e.getStackTrace());
            Log.info(thisClass, thisMethod, "Exception Response message" + e.getLocalizedMessage());

            validationTools.validateException(expectations, Constants.INVOKE_PROTECTED_RESOURCE, e);

        }

    }

    protected String buildParmString(List<endpointSettings> parms) throws Exception {

        String thisMethod = "buildParmString";
        if (parms != null) {
            StringBuilder result = new StringBuilder();
            boolean firstParm = true;
            for (endpointSettings parm : parms) {
                if (firstParm) {
                    firstParm = false;
                } else {
                    result.append("&");
                }
                Log.info(thisClass, thisMethod, "Setting request parameter:  key: " + parm.key + " value: " + parm.value);
                result.append(URLEncoder.encode(parm.key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(parm.value, "UTF-8"));
            }
            return result.toString();
        } else {
            return null;
        }

    }

    protected HttpURLConnection prepareConnection(String rawUrl, String method) throws Exception {
        String thisMethod = "HttpURLConnection";
        URL url = AutomationTools.getNewUrl(rawUrl);
        Log.info(thisClass, thisMethod, "HttpURLConnection URL is set to: " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Log.info(thisClass, thisMethod, "HttpURLConnection successfully opened the connection to URL " + url);
        connection.setRequestMethod(method);
        Log.info(thisClass, thisMethod, "HttpURLConnection set request method to " + method);
        connection.setConnectTimeout(120000); // 2 minutes
        Log.info(thisClass, thisMethod, "HttpURLConnection set connect timeout to 2 min " + method);
        connection.setReadTimeout(120000); // 2 minutes
        Log.info(thisClass, thisMethod, "HttpURLConnection set read timeout to 2 min " + method);
        // connection.setInstanceFollowRedirects(true); // allow redirect
        // Log.info(thisClass, thisMethod,
        // "HttpURLConnection setInstanceFollowRedirects is set to true");
        connection.setDoInput(true);
        if (method != "GET") {
            connection.setDoOutput(true);
        }
        if (method == Constants.PUTMETHOD) {
            connection.addRequestProperty("Content-Type", "application/json");
        }
        return connection;
    }

    protected void connect(HttpURLConnection connection) throws IOException {
        String thisMethod = "connect";
        connection.connect();
        Log.info(thisClass, thisMethod, "HttpURLConnection successfully completed connection.connect");
    }

    protected void writeToConnection(HttpURLConnection connection, String message) throws IOException {
        String thisMethod = "writeToConnection";
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(message);
        writer.flush();
        writer.close();
        Log.info(thisClass, thisMethod, "HttpURLConnection successfully completed write to connection");
    }

    protected String processResponse(HttpURLConnection connection) throws IOException, UnsupportedEncodingException {
        String thisMethod = "processResponse";
        InputStream responseStream = null;
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   Start Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

        int statusCode = connection.getResponseCode();
        Log.info(thisClass, thisMethod, "Response (StatusCode):  " + statusCode);

        String url = connection.getURL().toString();
        Log.info(thisClass, thisMethod, "Response (Url):  " + url);

        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            String value = header.getValue().toString().replaceAll("^\\[|\\]$", "");
            Log.info(thisClass, thisMethod, "Response (Header): " + header.getKey() + ": " + value);
        }

        String message = connection.getResponseMessage();
        Log.info(thisClass, thisMethod, "Response (Message):  " + message);

        if (statusCode == 200) {
            responseStream = connection.getInputStream();
        } else {
            responseStream = connection.getErrorStream();
        }
        if (responseStream != null) {
            final char[] buffer = new char[1024];
            StringBuffer sb2 = new StringBuffer();
            InputStreamReader reader = new InputStreamReader(responseStream, "UTF-8");
            int bytesRead;
            do {
                Log.info(thisClass, thisMethod, "HttpUrlConnection buffer.length  " + buffer.length);
                bytesRead = reader.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    sb2.append(buffer, 0, bytesRead);
                }
            } while (bytesRead >= 0);
            reader.close();
            String resultResource = new String(sb2.toString().trim());
            Log.info(thisClass, thisMethod, "Response (Full): " + resultResource);
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

            return resultResource;
        } else {
            Log.info(thisClass, thisMethod, "Response (Full):  No response body");
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@   End Response Content @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            return "Response (Full):  No response body";
        }

    }

    /**
     * Invoke the authorization endpoint directly instead of using client.jsp
     * First, we need to build up the list of parm needed for the endpoint -
     * those come from our settings
     *
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeAuthorizationEndpoint(String testCase, WebConversation wc, WebResponse inResponse, TestSettings settings,
                                                   List<validationData> expectations) throws Exception {

        return invokeAuthorizationEndpoint(testCase, wc, inResponse, settings, expectations, Constants.INVOKE_AUTH_SERVER);

    }

    public WebResponse invokeAuthorizationEndpoint(String testCase, WebConversation wc, WebResponse inResponse, TestSettings settings, List<validationData> expectations,
                                                   String basicAuth) throws Exception {

        String thisMethod = "invokeAuthorizationEndpoint";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        if (basicAuth.equals(Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH)) {
            Log.info(thisClass, thisMethod, "Building basic auth with user: " + settings.getAdminUser() + " and password: " + settings.getAdminPswd());
            headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getAdminUser(), settings.getAdminPswd()));
        }
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "response_type", "code");
        if (settings.getLoginPrompt() != null) {
            parms = eSettings.addEndpointSettings(parms, "prompt", settings.getLoginPrompt());
        }
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "autoauthz", "true");
        parms = eSettings.addEndpointSettings(parms, "state", settings.getState());

        if (settings.getCodeChallenge() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_challenge", settings.getCodeChallenge());
        }
        if (settings.getCodeChallengeMethod() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_challenge_method", settings.getCodeChallengeMethod());
        }

        WebResponse response = genericInvokeEndpoint(testCase, wc, inResponse, settings.getAuthorizeEndpt(), Constants.GETMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers,
                                                     expectations, settings);;
        return response;
    }

    public Object invokeAuthorizationEndpoint(String testCase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        return invokeAuthorizationEndpoint(testCase, webClient, startPage, settings, expectations, Constants.INVOKE_AUTH_SERVER);

    }

    public Object invokeAuthorizationEndpoint(String testCase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations,
                                              String basicAuth) throws Exception {

        String thisMethod = "invokeAuthorizationEndpoint";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        if (basicAuth.equals(Constants.INVOKE_AUTH_SERVER_WITH_BASIC_AUTH)) {
            Log.info(thisClass, thisMethod, "Building basic auth with user: " + settings.getAdminUser() + " and password: " + settings.getAdminPswd());
            headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getAdminUser(), settings.getAdminPswd()));
        }
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "response_type", "code");
        if (settings.getLoginPrompt() != null) {
            parms = eSettings.addEndpointSettings(parms, "prompt", settings.getLoginPrompt());
        }
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "autoauthz", "true");
        parms = eSettings.addEndpointSettings(parms, "state", settings.getState());

        if (settings.getCodeChallenge() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_challenge", settings.getCodeChallenge());
        }
        if (settings.getCodeChallengeMethod() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_challenge_method", settings.getCodeChallengeMethod());
        }

        String cookieHack = null;
        for (endpointSettings parm : parms) {
            if (cookieHack == null) {
                cookieHack = "?";
            } else {
                cookieHack = cookieHack + "&";
            }
            cookieHack = cookieHack + parm.getKey() + "=" + parm.getValue();
        }
        cookieHack = URLEncoder.encode(cookieHack, "UTF-8");

        Log.info(thisClass, thisMethod, "WebClient isJavaScriptEnabled: " + webClient.getOptions().isJavaScriptEnabled());

        Object response = genericInvokeEndpoint(testCase, webClient, startPage, settings.getAuthorizeEndpt(), Constants.GETMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers,
                                                expectations, settings);

        Set<Cookie> cookies = webClient.getCookieManager().getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith("WASSamlReq_")) {
                //prepend the url to the hacked string, then encode it (finally remove the original and add the new value
                cookieHack = cookie.getValue() + cookieHack;
                Cookie updatedCookie = new Cookie(cookie.getDomain(), cookie.getName(), cookieHack);
                webClient.getCookieManager().removeCookie(cookie);
                webClient.getCookieManager().addCookie(updatedCookie);
            }
        }
        msgUtils.printAllCookies(webClient);
        return response;
    }

    /**
     * Invoke the token endpoint directly instead of using client.jsp First, we
     * need to build up the list of parm needed for the endpoint - those come
     * from our settings
     *
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeTokenEndpoint(String testCase, WebConversation wc, WebResponse inResponse, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "grant_type", "authorization_code");
        parms = eSettings.addEndpointSettings(parms, "code", validationTools.getValueFromResponseFull(inResponse, "Received authorization code: "));
        if (settings.getCodeVerifier() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_verifier", settings.getCodeVerifier());
        }
        WebResponse response = genericInvokeEndpoint(_testName, wc, inResponse, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers,
                                                     expectations, settings);

        return response;
    }

    public Object invokeTokenEndpoint(String testCase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "grant_type", "authorization_code");
        parms = eSettings.addEndpointSettings(parms, "code", validationTools.getValueFromResponseFull(startPage, "Received authorization code: "));
        if (settings.getCodeVerifier() != null) {
            parms = eSettings.addEndpointSettings(parms, "code_verifier", settings.getCodeVerifier());
        }
        Object response = genericInvokeEndpoint(_testName, webClient, startPage, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers,
                                                expectations, settings);

        return response;
    }

    /**
     * Invoke the token endpoint directly instead of using client.jsp First, we
     * need to build up the list of parm needed for the endpoint - those come
     * from our settings
     *
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeTokenEndpoint_clientCredentials(String testCase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint_clientCredentials";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        // Log.info(thisClass, thisMethod, "Building basic auth with user: " +
        // settings.getClientID() + " and password: " +
        // settings.getClientSecret());
        // headers = eSettings.addEndpointSettings(null, "Authorization",
        // cttools.buildBasicAuthCred(settings.getClientID(),
        // settings.getClientSecret()));

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "grant_type", "client_credentials");
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers,
                                                     expectations, settings);

        return response;
    }

    /**
     * Invoke the token endpoint directly instead of using client.jsp First, we
     * need to build up the list of parm needed for the endpoint - those come
     * from our settings
     *
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeTokenEndpoint_password(String testCase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint_password";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        Log.info(thisClass, thisMethod, "Building basic auth with user: " + settings.getClientID() + " and password: " + settings.getClientSecret());
        headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));

        Log.info(thisClass, thisMethod, "Setting resource owner " + settings.getAdminUser() + " and password: " + settings.getAdminPswd());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "username", settings.getAdminUser());
        parms = eSettings.addEndpointSettings(parms, "password", settings.getAdminPswd());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "grant_type", Constants.PASSWORD_GRANT_TYPE);
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers,
                                                     expectations, settings);

        return response;
    }

    /**
     * Invoke the app-passwords endpoint method to create an app-password
     * This method requests that the clientId/clientSecret be passed in the header
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to use to generate the app-password
     * @param appName
     *            - the app name to register the app-password with
     * @param usedBy
     *            - the client_d that will be allowed to use the tokens created from the app-password created
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the app-password, or error/failure information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppPasswordsEndpoint_create(String testCase, WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy,
                                                         List<validationData> expectations) throws Exception {
        return invokeAppPasswordsEndpoint_create(testCase, wc, settings, accessToken, appName, usedBy, Constants.HEADER, expectations);
    }

    /**
     * Invoke the app-passwords endpoint method to create an app-password
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to use to generate the app-password
     * @param appName
     *            - the app name to register the app-password with
     * @param usedBy
     *            - the client_d that will be allowed to use the tokens created from the app-password created
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the app-password, or error/failure information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppPasswordsEndpoint_create(String testCase, WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy,
                                                         String clientLocation, List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppPasswordsEndpoint_create";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Generating app-password for access_token: [" + accessToken + "] and app_name: [" + appName + "] using clientId: [" + settings.getClientID()
                                        + "] and clientSecret: [" + settings.getClientSecret() + "]");

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = null;
        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);

        parms = eSettings.addEndpointSettingsIfNotNull(parms, "app_name", appName);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "used_by", usedBy);
        if (clientLocation.equals(Constants.HEADER)) {
            headers = eSettings.addEndpointSettings(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        }

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAppPasswordsEndpt(), Constants.POSTMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE,
                                                     parms, headers, expectations, settings);

        return response;
    }

    /**
     * Invoke the app-passwords endpoint method to list all app-passwords
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to list
     * @param userId
     *            - the user id to list app-passwords for
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the list of app-password info, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppPasswordsEndpoint_list(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId,
                                                       List<validationData> expectations) throws Exception {
        return invokeAppPasswordsEndpoint_list(testCase, wc, settings, accessToken, userId, Constants.HEADER, expectations);
    }

    /**
     * Invoke the app-passwords endpoint method to list all app-passwords
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to list
     * @param userId
     *            - the user id to list app-passwords for
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the list of app-password info, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppPasswordsEndpoint_list(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String clientLocation,
                                                       List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppPasswordsEndpoint_list";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Listing app-passwords for access_token: [" + accessToken + "] and user_id: [" + userId + "] using clientId: [" + settings.getClientID()
                                        + "] and clientSecret: [" + settings.getClientSecret() + "]");

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = null;

        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);

        // pass the name of the user to perform the action on - if omitted, the list will be for the user in the access_token
        // passing the user id allows an admin to list some other user
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "user_id", userId);
        if (Constants.HEADER.equals(clientLocation)) {
            headers = eSettings.addEndpointSettings(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        }

        boolean origScripting = HttpUnitOptions.isScriptingEnabled();
        HttpUnitOptions.setScriptingEnabled(false);
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAppPasswordsEndpt(), Constants.GETMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_LIST, parms,
                                                     headers, expectations, settings);
        HttpUnitOptions.setScriptingEnabled(origScripting);

        return response;
    }

    /**
     * Invoke the app-passwords endpoint method to revoke app-passwords
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to revoke
     * @param userId
     *            - the user id to revoke app-passwords for
     * @param appId
     *            - the specific app_id to revoke
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public void invokeAppPasswordsEndpoint_revoke(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String appId,
                                                  List<validationData> expectations) throws Exception {
        invokeAppPasswordsEndpoint_revoke(testCase, wc, settings, accessToken, userId, appId, Constants.HEADER, expectations);
    }

    /**
     * Invoke the app-passwords endpoint method to revoke app-passwords
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to revoke
     * @param userId
     *            - the user id to revoke app-passwords for
     * @param appId
     *            - the specific app_id to revoke
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public void invokeAppPasswordsEndpoint_revoke(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String appId,
                                                  String clientLocation, List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppPasswordsEndpoint_revoke";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Revoking app-passwords for access_token: [" + accessToken + "], user_id: [" + userId + "] and app_id: [" + appId + "] using clientId: ["
                                        + settings.getClientID() + "] and clientSecret: [" + settings.getClientSecret() + "]");

        String urlString = null;
        if (appId == null) {
            urlString = settings.getAppPasswordsEndpt();
        } else {
            urlString = settings.getAppPasswordsEndpt() + "/" + appId;
        }

        List<endpointSettings> parms = null;
        List<endpointSettings> headers = null;

        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);
        if (clientLocation.equals(Constants.HEADER)) {
            headers = eSettings.addEndpointSettingsIfNotNull(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "client_secret", settings.getClientSecret());
        }
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "user_id", userId);

        // RTC 274642 using htmlunit is resulting in an OOM - even with javascript disabled - it's failing in:
        //        java.lang.OutOfMemoryError: Failed to create a thread: retVal -1073741830, errno 132 (0x84), errno2 -1055784930 (0xffffffffc112001e)
        //        at java.lang.Thread.start(Thread.java:982)
        //        at com.gargoylesoftware.htmlunit.javascript.background.DefaultJavaScriptExecutor.startThreadIfNeeded(DefaultJavaScriptExecutor.java:63)
        //        at com.gargoylesoftware.htmlunit.javascript.background.DefaultJavaScriptExecutor.addWindow(DefaultJavaScriptExecutor.java:191)
        //        at com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine.registerWindowAndMaybeStartEventLoop(JavaScriptEngine.java:629)
        //        at com.gargoylesoftware.htmlunit.WebClient.getPage(WebClient.java:357)
        //        at com.gargoylesoftware.htmlunit.WebClient.getPage(WebClient.java:433)
        //        at com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest.genericInvokeEndpoint(CommonTest.java:1280)
        //        WebClient webClient = CommonTestHelpers.getWebClient(true);
        //        webClient.getOptions().setJavaScriptEnabled(false);
        //        genericInvokeEndpoint(testCase, webClient, null, urlString, Constants.DELETEMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE, parms, headers, expectations, settings, false);
        genericInvokeEndpointWithHttpUrlConn(testCase, null, urlString, Constants.DELETEMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE, parms, headers, expectations);

    }

    public void addNameValuePairIfSet(ArrayList<NameValuePair> params, String key, String value) throws Exception {
        if (params == null) {
            params = new ArrayList<NameValuePair>();
        }
        if (value != null) {
            params.add(new NameValuePair(key, value));
        }
    }

    /**
     * Invoke the app-tokens endpoint method to create an app-token
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to use to generate the app-token
     * @param appName
     *            - the app name to register the app-token with
     * @param usedBy
     *            - used_by value to include in the request. The request parameter will be omitted if this value is null.
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the app-token, or error/failure information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppTokensEndpoint_create(String testCase, WebConversation wc, TestSettings settings, String accessToken, String appName, String usedBy,
                                                      String clientLocation, List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppTokensEndpoint_create";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Generating app-token for access_token: [" + accessToken + "] and app_name: [" + appName + "] using clientId: [" + settings.getClientID()
                                        + "] and clientSecret: [" + settings.getClientSecret() + "]");

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = null;
        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);

        parms = eSettings.addEndpointSettingsIfNotNull(parms, "app_name", appName);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "used_by", usedBy);
        if (clientLocation.equals(Constants.HEADER)) {
            headers = eSettings.addEndpointSettings(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        }

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAppTokensEndpt(), Constants.POSTMETHOD, Constants.INVOKE_APP_TOKENS_ENDPOINT_CREATE, parms,
                                                     headers, expectations, settings);

        return response;
    }

    /**
     * Invoke the app-tokens endpoint method to list all app-tokens
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to list
     * @param userId
     *            - the user id to list app-tokens for
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the list of app-token info, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppTokensEndpoint_list(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId,
                                                    List<validationData> expectations) throws Exception {
        return invokeAppTokensEndpoint_list(testCase, wc, settings, accessToken, userId, Constants.HEADER, expectations);
    }

    /**
     * Invoke the app-tokens endpoint method to list all app-tokens
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to list
     * @param userId
     *            - the user id to list app-tokens for
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could contain the list of app-token info, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public WebResponse invokeAppTokensEndpoint_list(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String clientLocation,
                                                    List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppTokensEndpoint_list";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Listing app-tokens for access_token: [" + accessToken + "] and user_id: [" + userId + "] using clientId: [" + settings.getClientID()
                                        + "] and clientSecret: [" + settings.getClientSecret() + "]");

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = null;

        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);
        // pass the name of the user to perform the action on - if omitted, the list will be for the user in the access_token
        // passing the user id allows an admin to list some other user
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "user_id", userId);
        if (clientLocation.equals(Constants.HEADER)) {
            headers = eSettings.addEndpointSettings(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        }

        boolean origScripting = HttpUnitOptions.isScriptingEnabled();
        HttpUnitOptions.setScriptingEnabled(false);
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAppTokensEndpt(), Constants.GETMETHOD, Constants.INVOKE_APP_TOKENS_ENDPOINT_LIST, parms,
                                                     headers, expectations, settings);
        HttpUnitOptions.setScriptingEnabled(origScripting);

        return response;
    }

    /**
     * Invoke the app-tokens endpoint method to revoke app-tokens
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to revoke
     * @param userId
     *            - the user id to revoke app-tokens for
     * @param appId
     *            - the specific app_id to revoke
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could be an empty response, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public void invokeAppTokensEndpoint_revoke(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String tokenId,
                                               List<validationData> expectations) throws Exception {
        invokeAppTokensEndpoint_revoke(testCase, wc, settings, accessToken, userId, tokenId, Constants.HEADER, expectations);
    }

    /**
     * Invoke the app-tokens endpoint method to revoke app-tokens
     *
     * @param testCase
     *            - the testCase name
     * @param wc
     *            - the conversation
     * @param settings
     *            - current testSettings
     * @param accessToken
     *            - the access_token to a) provide authorization and 2) possibly the user_id to revoke
     * @param userId
     *            - the user id to revoke app-tokens for
     * @param appId
     *            - the specific app_id to revoke
     * @param clientLocation
     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
     * @param expectations
     *            - the expected results/output from invoking the endpoint
     * @return - returns the response from the invocation (could be an empty response, or error/failure
     *         information
     * @throws Exception
     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
     *             error
     */
    public void invokeAppTokensEndpoint_revoke(String testCase, WebConversation wc, TestSettings settings, String accessToken, String userId, String tokenId, String clientLocation,
                                               List<validationData> expectations) throws Exception {
        String thisMethod = "invokeAppTokensEndpoint_revoke";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "Revoking app-tokens for access_token: [" + accessToken + "], user_id: [" + userId + "] and token_id: [" + tokenId + "] using clientId: ["
                                        + settings.getClientID() + "] and clientSecret: [" + settings.getClientSecret() + "]");

        String urlString = null;
        if (tokenId == null) {
            urlString = settings.getAppTokensEndpt();
        } else {
            urlString = settings.getAppTokensEndpt() + "/" + tokenId;
        }

        List<endpointSettings> parms = null;
        List<endpointSettings> headers = null;

        headers = eSettings.addEndpointSettingsIfNotNull(headers, Constants.ACCESS_TOKEN_KEY, accessToken);
        if (clientLocation.equals(Constants.HEADER)) {
            headers = eSettings.addEndpointSettingsIfNotNull(headers, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        } else {
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "client_id", settings.getClientID());
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "client_secret", settings.getClientSecret());
        }
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "user_id", userId);

        // RTC 274642 using htmlunit is resulting in an OOM - even with javascript disabled - it's failing in:
        //        java.lang.OutOfMemoryError: Failed to create a thread: retVal -1073741830, errno 132 (0x84), errno2 -1055784930 (0xffffffffc112001e)
        //        at java.lang.Thread.start(Thread.java:982)
        //        at com.gargoylesoftware.htmlunit.javascript.background.DefaultJavaScriptExecutor.startThreadIfNeeded(DefaultJavaScriptExecutor.java:63)
        //        at com.gargoylesoftware.htmlunit.javascript.background.DefaultJavaScriptExecutor.addWindow(DefaultJavaScriptExecutor.java:191)
        //        at com.gargoylesoftware.htmlunit.javascript.JavaScriptEngine.registerWindowAndMaybeStartEventLoop(JavaScriptEngine.java:629)
        //        at com.gargoylesoftware.htmlunit.WebClient.getPage(WebClient.java:357)
        //        at com.gargoylesoftware.htmlunit.WebClient.getPage(WebClient.java:433)
        //        at com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest.genericInvokeEndpoint(CommonTest.java:1280)
        //        WebClient webClient = CommonTestHelpers.getWebClient(true);
        //        webClient.getOptions().setJavaScriptEnabled(false);
        //        genericInvokeEndpoint(testCase, webClient, null, urlString, Constants.DELETEMETHOD, Constants.INVOKE_APP_TOKENS_ENDPOINT_REVOKE, parms, headers, expectations, settings, false);
        genericInvokeEndpointWithHttpUrlConn(testCase, null, urlString, Constants.DELETEMETHOD, Constants.INVOKE_APP_TOKENS_ENDPOINT_REVOKE, parms, headers, expectations);

    }

    /**
     * Invoke the token endpoint directly instead of using client.jsp First, we
     * need to build up the list of parm needed for the endpoint - those come
     * from our settings
     *
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeGenericForm_refreshToken(String testCase, WebConversation wc, TestSettings settings, String originalRefreshToken,
                                                      List<validationData> expectations) throws Exception {

        String thisMethod = "invokeGenericForm_refreshToken";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> parms = setRefreshTokenParms(settings, originalRefreshToken);

        // Constants.REFRESH_TOKEN_GRANT_TYPE);
        WebResponse response = genericInvokeForm(_testName, wc, null, settings, settings.getRefreshTokUrl(), Constants.GETMETHOD, Constants.INVOKE_REFRESH_ENDPOINT, parms,
                                                 expectations);

        return response;
    }

    public List<endpointSettings> setRefreshTokenParms(TestSettings settings, String originalRefreshToken) throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "refresh_token", originalRefreshToken);
        parms = eSettings.addEndpointSettings(parms, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        // parms = eSettings.addEndpointSettings(parms, "grant_type",
        return parms;
    }

    public Object invokeGenericForm_refreshToken(String testCase, WebClient webClient, TestSettings settings, String originalRefreshToken,
                                                 List<validationData> expectations) throws Exception {

        String thisMethod = "invokeGenericForm_refreshToken";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> parms = setRefreshTokenParms(settings, originalRefreshToken);

        // Constants.REFRESH_TOKEN_GRANT_TYPE);
        Object response = genericInvokeForm(_testName, webClient, null, settings, settings.getRefreshTokUrl(), Constants.POSTMETHOD, Constants.INVOKE_REFRESH_ENDPOINT, parms,
                                            expectations);

        return response;
    }

    public WebResponse invokeGenericForm_revokeToken(String testCase, WebConversation wc, TestSettings settings, String accessToken,
                                                     List<validationData> expectations) throws Exception {

        String thisMethod = "invokeGenericForm_revokeToken";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "token", accessToken);
        return genericInvokeEndpoint(_testName, wc, null, settings.getRevocationEndpt(), Constants.POSTMETHOD, Constants.INVOKE_REVOKE_ENDPOINT, parms, headers, expectations);

    }

    @SuppressWarnings("deprecation")
    public void overrideRedirect() {

        OverrideRedirect = true;
        HttpUnitOptions.setAutoRedirect(false);

    }

    /*
     * method determines if remote port is in use - if it is, remote
     * application/service should be available - ie: tfim server, ...
     */
    public static boolean isRemoteServerPortInUse(String serverName, String serverPort) throws Exception {

        String thisMethod = "isRemoteServerPortInUse";
        if (serverName != null) {
            try {
                InetAddress ia = InetAddress.getByName(serverName);
                Socket s = new Socket(ia, Integer.parseInt(serverPort));
                Log.info(thisClass, thisMethod, "Server is listening on port " + serverPort + " of " + serverName);
                s.close();
                return true;
            } catch (IOException ex) {
                // The remote host is not listening on this port
                Log.info(thisClass, thisMethod, "Server is not listening on port " + serverPort + " of " + serverName);
                return false;
            }
        } else {
            Log.info(thisClass, thisMethod, "Server name specified was null");
            return false;
        }

    }

    /**
     * Determines if the remote server is up and whether a request for the
     * specified URL returns a 200 status code.
     *
     * @param hostName
     * @param port
     * @param urlContextPath
     * @param isSecure
     */
    public static boolean isRemoteServerAndUrlAvailable(String hostName, int port, String urlContextPath, boolean isSecure) {
        String method = "isRemoteServerAndUrlAvailable";

        return true;
        //        try {
        //            Log.info(thisClass, method, "Creating socket for host " + hostName + " and port " + port);
        //            Socket s = new Socket(hostName, port);
        //            if (s.isConnected()) {
        //                try {
        //                    WebConversation wc = new WebConversation();
        //                    String url = hostName + ":" + port + (urlContextPath == null ? "" : urlContextPath);
        //                    if (isSecure) {
        //                        url = "https://" + url;
        //                    } else {
        //                        url = "http://" + url;
        //                    }
        //                    WebRequest request = new PostMethodWebRequest(url, true);
        //                    Log.info(thisClass, method, "Getting response for post request: " + url);
        //                    WebResponse response = wc.getResponse(request);
        //                    if (response.getResponseCode() == HttpServletResponse.SC_OK) {
        //                        Log.info(thisClass, method, "Remote server " + hostName + " and URL " + urlContextPath + " is up and responding to requests.");
        //                        return true;
        //                    }
        //                    Log.info(thisClass, method, "Remote server " + hostName + " is up, but did not receive a 200 status code for URL " + urlContextPath + ". Instead got " + response.getResponseCode());
        //                    return false;
        //                } catch (SAXException e) {
        //                    throw new IOException(e);
        //                } catch (HttpNotFoundException e) {
        //                    Log.info(thisClass, method, "Remote server " + hostName + " considered not available because of exception: " + e);
        //                }
        //            }
        //        } catch (IOException e) {
        //            Log.info(thisClass, method, "Caught exception attempting to contact " + hostName + " and URL " + urlContextPath + ": " + e);
        //        }
        //        Log.info(thisClass, method, "Remote server " + hostName + " is down.");
        //        return false;
    }

    protected void logTestCaseInServerSideLogs(String action) throws Exception {

        for (TestServer server : serverRefList) {
            logTestCaseInServerSideLog(action, server);
        }
    }

    private void logTestCaseInServerSideLog(String action, TestServer server) throws Exception {

        try {
            if (server != null) {
                Log.info(thisClass, "logTestCaseInServerSideLog", server.getServer().getServerName());
                WebConversation wc = new WebConversation();
                WebRequest request = new GetMethodWebRequest(server.getServerHttpString() + "/" + AppConstants.TESTMARKER_CONTEXT_ROOT + "/" + AppConstants.TESTMARKER_PATH);
                request.setParameter("action", action);
                request.setParameter("testCaseName", _testName);
                wc.getResponse(request);
            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            e.printStackTrace();
        }

    }

    public WebResponse buildPostSolicitedSPInitiatedRequest(String testcase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "buildPostSolicitedSPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);
        WebResponse response = null;

        try {

            String builtTarget = null;

            builtTarget = settings.getProtectedResource();

            WebRequest request = new GetMethodWebRequest(builtTarget);

            Log.info(thisClass, thisMethod, "Returned request is: " + request.toString());

            // msgUtils.printRequestParts(request, testcase,
            // "Outgoing request");
            // // wc.getClientProperties().setAutoRedirect(false);

            response = wc.getResponse(request);
            msgUtils.printResponseParts(response, testcase, "returned response");
            msgUtils.printAllCookies(wc);
            // validationTools.setServers(testSAMLServer, testSAMLOIDCServer,
            // testOIDCServer);
            validationTools.validateResult(response, Constants.BUILD_POST_SP_INITIATED_REQUEST, expectations, settings);

            // String id = getValueFromCookieInConversation(wc,
            // "SAML20RelayState");
            // String id = getValueFromCookieInConversation(wc,
            // "SAML20RelayState");
            //
            // //.replaceAll("sp_initial_", "");
            // Log.info(thisClass, thisMethod, "relay state: " + id);
            //
            // settings.setSamlTokenValidationData(settings.getSamlTokenValidationData().getNameId(),
            // settings.getSamlTokenValidationData().getIssuer(), id,
            // settings.getSamlTokenValidationData().getMessageID(),
            // settings.getSamlTokenValidationData().getEncryptionKeyUser());
        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.BUILD_POST_SP_INITIATED_REQUEST, e);
        }
        return response;
    }

    public Object buildPostSolicitedSPInitiatedRequest(String testcase, WebClient webClient, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "buildPostSolicitedSPInitiatedRequest";
        msgUtils.printMethodName(thisMethod);
        Object thePage = null;

        try {

            String builtTarget = null;

            builtTarget = settings.getProtectedResource();

            URL url = AutomationTools.getNewUrl(builtTarget);
            com.gargoylesoftware.htmlunit.WebRequest request = new com.gargoylesoftware.htmlunit.WebRequest(url, HttpMethod.GET);

            Log.info(thisClass, thisMethod, "Returned request is: " + request.toString());

            thePage = webClient.getPage(request);
            msgUtils.printAllCookies(webClient);
            msgUtils.printResponseParts(thePage, testcase, "returned response");
            // validationTools.setServers(testSAMLServer, testSAMLOIDCServer,
            // testOIDCServer);
            validationTools.validateResult(thePage, Constants.BUILD_POST_SP_INITIATED_REQUEST, expectations, settings);

        } catch (Exception e) {
            Log.error(thisClass, testcase, e, "Exception occurred in " + thisMethod);
            System.err.println("Exception: " + e);
            validationTools.validateException(expectations, Constants.BUILD_POST_SP_INITIATED_REQUEST, e);
        }
        return thePage;
    }

    /**
     * Asserts that the TFIM server used as the IDP is up and responding to
     * requests.
     */
    public static void setSelectedTfimServer(TestServer server, TestServer idpServer) throws Exception {

        String[] chosen = selectIDPServer();
        if (chosen != null) {
            String shortName = chosen[0];
            if (shortName != null) {
                shortName = shortName.split("\\.")[0];
            }

            addIDPServerProp(server, shortName);
            shibbolethHelpers.fixShibbolethInfoinSPServer(server, idpServer);

            server.copyDefaultSPConfigFiles(shortName);
        }
        // for ()
        // assertTrue("Remote server " + idpTfimHostName +
        // " is not available, consequently any tests of " +
        // thisClass.getSimpleName() + " class won't run.",
        // isRemoteServerAndUrlAvailable(idpTfimHostName, idpTfimPortHttp,
        // idpTfimApp, false));
    }

    /**
     * Takes an array of server:port strings and randomly chooses one - checks
     * if it is listening on the port specified. If it is, it returns that
     * information. If not, it creates a new list containing all OTHER server
     * and calls itself again. Hopefully randomly finding a server that is
     * active. If no servers are active then it throws an exception - which
     * should cause the test class to fail.
     *
     * @param serverList
     * @return
     * @throws Exception
     */
    public static String[] getServerFromList(String[] serverList) throws Exception {

        Log.info(thisClass, "getServerFromList", "serverList: " + Arrays.toString(serverList));
        String thisMethod = "getServerFromList";
        if (serverList == null) {
            throw new Exception("No server from the list appears to be active - Test class is terminating");
        }
        int numServers = serverList.length;
        if (numServers == 0) {
            throw new Exception("None of the servers in the list appear to be active - Test class is terminating");
        }

        Log.info(thisClass, thisMethod, "Choosing server from a list of " + numServers + " servers.");
        Log.info(thisClass, thisMethod, "Selection will be made from: " + Arrays.toString(serverList));

        Random rand = new Random();
        Integer num = rand.nextInt(1000);

        // create array for next round of checking if needed
        String[] nextServerList = new String[numServers - 1];
        int div = num % numServers;
        Log.info(thisClass, thisMethod, "Checking server from array list location: " + div);

        int j = 0;
        for (int i = 0; i < numServers; i++) {
            if (div != i) {
                // This server wasn't selected to be checked - add to array for
                // next round of checking if needed
                // debug msg:
                // System.out.println("Server doesn't match - div is: " + div +
                // " i is: " + i) ;
                nextServerList[j] = serverList[i];
                j++;
            } else {
                // debug msg: System.out.println("Server matches - div is: " +
                // div + " i is: " + i) ;
                if (serverList[i] == null || serverList[i].length() == 0) {
                    Log.info(thisClass, thisMethod, "The server selected is not set - skipping.  (position " + i + " in the server array)");
                } else {
                    String serverName = serverList[i].split(":")[0];
                    String serverPort = serverList[i].split(":")[1];
                    String serverSecurePort = serverList[i].split(":")[2];
                    //                    if (isRemoteServerPortInUse(serverName, serverPort)) {
                    //                        // non-secure port available, now check ssl
                    //                        if (isRemoteServerPortInUse(serverName, serverSecurePort)) {
                    return new String[] { serverName, serverPort, serverSecurePort };
                    //                        }
                    //                    }
                }
            }

        }
        return getServerFromList(nextServerList);
    }

    public static String[] selectIDPServer() throws Exception {

        String idpTfimApp = "/sps/login.jsp";

        Log.info(thisClass, "selectIDPServer", "starting");

        String[] filteredIDPServerList = Constants.SHIBBOLETH_SERVER_LIST;
        String[] chosen_IDP_server = null;
        while ((chosen_IDP_server == null) && (filteredIDPServerList != null)) {
            chosen_IDP_server = getServerFromList(filteredIDPServerList);
            // if login page accessable - we've found a good derver
            if (isRemoteServerAndUrlAvailable(chosen_IDP_server[0], Integer.valueOf(chosen_IDP_server[1]), idpTfimApp, false)) {
                break;
            } else {
                // server's IDP function is NOT working - skip it
                filteredIDPServerList = removeChosenFromFilteredIDPServerList(filteredIDPServerList, chosen_IDP_server);
                chosen_IDP_server = null;
            }
        }

        if (chosen_IDP_server == null) {
            //            throw new Exception("No TFIM server appears to be active - Test class is terminating");
            throw new Exception("No Shibboleth server appears to be active - Test class is terminating");
        }
        Log.info(thisClass, "selectIDPServer", "Using Server: " + chosen_IDP_server[0]);
        // setServerIndex(determineServerIndex(chosen_IDP_server)) ;
        // Log.info(thisClass, "selectTFIMIDPServer",
        // getSelectedIDPServerName()) ;
        // setIdpRoot(getShortName(getSelectedIDPServerName())) ;

        return chosen_IDP_server;
    }

    public static String[] removeChosenFromFilteredIDPServerList(String[] origList, String[] chosen_IDP_server) throws Exception {

        ArrayList<String> newServerList = new ArrayList<String>();

        int numServers = origList.length;
        if (numServers == 0) {
            throw new Exception("None of the servers in the list appear to be active - Test class is terminating");
        }

        for (int i = 0; i < numServers; i++) {
            // String shortName = getShortName(origList[i]) ;

            Log.info(thisClass, "removeChosenFromFilteredIDPServerList", "Orig list: " + origList[i]);
            Log.info(thisClass, "removeChosenFromFilteredIDPServerList", "Chosen Server" + chosen_IDP_server[0]);
            if (!origList[i].startsWith(chosen_IDP_server[0])) {
                newServerList.add(origList[i]);
            }

        }
        return newServerList.toArray(new String[newServerList.size()]);
    }

    public static void addIDPServerProp(TestServer server, String tfimServerName) throws Exception {
        String thisMethod = "addIDPServerProp";
        // create proprty to point to the correct TFIM IDP server
        String newPropString = "tfimIdpServer=" + tfimServerName;
        // add property to bootstrap.properties
        String bootProps = server.getServerFileLoc() + "/bootstrap.properties";
        Log.info(thisClass, thisMethod, "tfimIdpProp File: " + bootProps);
        // append to bootstrap.properties
        FileWriter writer = new FileWriter(bootProps, true);
        writer.append(System.getProperty("line.separator"));
        writer.append(newPropString);
        writer.append(System.getProperty("line.separator"));
        writer.close();

    }

    public static void setJWKValidationMap(HashMap<String, String> validationEndpoints) {
        jwkValidationMap = new HashMap<String, String>(validationEndpoints);
    }

    public static void setMiscBootstrapParms(HashMap<String, String> inMiscBootstrapParms) {
        miscBootstrapParms = new HashMap<String, String>(inMiscBootstrapParms);
    }

    private static void setupMongoDBConfig(TestServer aTestServer, String httpString, Integer defaultPort) {
        String methodName = "setupMongoDBConfig";
        Log.info(thisClass, methodName, "Setup for mongoDB");
        String mongoTableUid = "defaultUID";
        try {
            mongoTableUid = "_" + InetAddress.getLocalHost().getHostName() + "_" + new Random(System.currentTimeMillis()).nextLong();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            mongoTableUid = "localhost-" + System.nanoTime();
        }

        try {
            MongoDBUtils.startMongoDB(aTestServer.getServer(), MONGO_PROPS_FILE, mongoTableUid);
            MongoDBUtils.setupMongoDBEntries(httpString, defaultPort, mongoTableUid);
        } catch (Exception e) {
            Log.error(thisClass, methodName, e, "Exception setting up MongoDB, CustomStore tests may fail.");

        }
    }

    static protected void setForClientSecretHash() {
        testSettings.setHttpString(testOPServer.getHttpString()); // need this information to talk to the Derby or MongoDB servlet
        testSettings.setHttpPort(testOPServer.getServerHttpPort());
        testSettings.setHashed(true);
    }

    /**
     * JakartaEE9 transform applications for a specified server.
     *
     * @param serverName The server to transform the applications on.
     */
    private static void transformApps(TestServer server) {
        if (JakartaEE9Action.isActive()) {
            LibertyServer myServer = server.getServer();
            switch (server.getServer().getServerName()) {
                /******************************************************************
                 *
                 * com.ibm.ws.security.social_fat.LibertOP servers
                 *
                 ******************************************************************/
                case "com.ibm.ws.security.social_fat.LibertyOP.op":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    break;
                case "com.ibm.ws.security.social_fat.LibertyOP.social":
                case "com.ibm.ws.security.social_fat.LibertyOP.socialDisc":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "helloworld.war"));
                    break;

                /******************************************************************
                 *
                 * com.ibm.ws.security.oidc.client_fat servers
                 *
                 ******************************************************************/
                case "com.ibm.ws.security.openidconnect.client-1.0_fat.op":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    break;
                case "com.ibm.ws.security.openidconnect.client-1.0_fat.rp":
                case "com.ibm.ws.security.openidconnect.client-1.0_fat.rpd":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "formlogin.war"));
                    break;
                case "com.ibm.ws.security.openidconnect.client-1.0_fat.rs":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "helloworld.war"));
                    break;

                /******************************************************************
                 *
                 * com.ibm.ws.security.oidc.server_fat servers
                 *
                 ******************************************************************/
                case "com.ibm.ws.security.openidconnect.server-1.0_fat":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oAuth20DerbySetup.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oauthclient.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oauthtaidemo.ear"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "oAuth20MongoSetup.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "testMediator.jar"));
                    break;
                case "com.ibm.ws.security.openidconnect.server-1.0_fat.cert":
                case "com.ibm.ws.security.openidconnect.server-1.0_fat.cert_required":
                case "com.ibm.ws.security.openidconnect.server-1.0_fat.pwdTest":
                case "com.ibm.ws.security.openidconnect.server-1.0_fat.tai":
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oAuth20DerbySetup.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oauthclient.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "oauthtaidemo.ear"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "dropins" + File.separatorChar + "testmarker.war"));
                    JakartaEE9Action.transformApp(Paths.get(myServer.getServerRoot() + File.separatorChar + "test-apps" + File.separatorChar + "oAuth20MongoSetup.war"));
                    break;
            }
        }
    }
}
