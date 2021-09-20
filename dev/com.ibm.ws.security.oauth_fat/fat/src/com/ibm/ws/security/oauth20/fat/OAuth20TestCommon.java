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
package com.ibm.ws.security.oauth20.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.LoggerFactory;

import com.ibm.websphere.simplicity.config.MongoDBElement;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.mongo.fat.MongoServerSelector;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.runtime.Network;

public class OAuth20TestCommon {

    public static String DERBY_STARTS_WITH = "com.ibm.ws.security.oauth-2.0.derby";
    public static String DERBY_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.derby.fat";
    public static String DERBY_STORE_SERVER_XOR = "com.ibm.ws.security.oauth-2.0.derby.xor.fat";
    public static String DERBY2_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.derby2.fat";
    public static String DERBY3_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.derby3.fat";
    public static String MONGO_STORE_FEATURE_SERVER = "com.ibm.ws.security.oauth-2.0.customstore.fat";
    public static String MONGO_STORE_FEATURE_SERVER_XOR = "com.ibm.ws.security.oauth-2.0.customstore.xor.fat";
    public static String MONGO_STORE_FEATURE_SERVER2 = "com.ibm.ws.security.oauth-2.0.customstore2.fat";
    public static String MONGO_STORE_FEATURE_SERVER3 = "com.ibm.ws.security.oauth-2.0.customstore3.fat";
    public static String MONGO_STORE_BELL_SERVER = "com.ibm.ws.security.oauth-2.0.customstore.bell.fat";
    public static String MONGO_STORE_BELL_SERVER2 = "com.ibm.ws.security.oauth-2.0.customstore2.bell.fat";
    public static String MONGO_STORE_BELL_SERVER3 = "com.ibm.ws.security.oauth-2.0.customstore3.bell.fat";
    public static String MONG_STARTS_WITH = "com.ibm.ws.security.oauth-2.0.customstore";
    public static String LOCAL_STORE_SERVER = "com.ibm.ws.security.oauth-2.0.fat";
    public static final String LTPA_TOKEN = "LtpaToken2";
    private static final Class<?> thisClass = OAuth20TestCommon.class;

    public static String MONGO_PROPS_FILE = "mongoDB.props"; // this name needs to match the one used in CustomStoreSample
    public static String mongoTableUid = "defaultUID";
    public boolean isRunningCustomStore = false;
    // These should match the strings used in the oAuth20MongoSetup servlet
    final static String DB_NAME = "dbName";
    final static String DB_HOST = "dbHost";
    final static String DB_PORT = "dbPort";
    final static String DB_PWD = "dbPwd";
    final static String DB_USER = "dbUser";
    static String dbInfo = "";

    private static MongodExecutable mongodExecutable = null;

    static {
        try {
            mongoTableUid = "_" + InetAddress.getLocalHost().getHostName() + "_" + new Random(System.currentTimeMillis()).nextLong();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            mongoTableUid = "localhost-" + System.nanoTime();
        }
    }

    protected static TrustManager tm = null;

    @Rule
    public TestName testName = new TestName();

    public static LibertyServer server = null;

//    static {
//        try {
//            TrustManager tm = new X509TrustManager() {
//                @Override
//                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {}
//
//                @Override
//                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {}
//
//                @Override
//                public X509Certificate[] getAcceptedIssuers() {
//                    return new X509Certificate[0];
//                }
//            };
//            SSLContext ctx = SSLContext.getInstance("TLS");
//            ctx.init(null, new TrustManager[] { tm }, null);
//            SSLContext.setDefault(ctx);
//        } catch (Exception e) {
//            Log.info(thisClass, "static initializer", "Unable to set default TrustManager", e);
//            throw new RuntimeException("Unable to set default TrustManager", e);
//        }
//    }

    public static String httpStart = null;
    public String httpsStart = null;
    public String firstClientUrl = null;
    public String firstClientUrlSSL = null;
    public String clientRedirect = null;
    public String authorizeEndpt = null;
    public String tokenEndpt = null;
    public String protectedResource = null;
    public String refreshTokUrl = null;
    public String clientName = null;
    public String clientID = null;
    public String adminUser = "testuser";
    public String adminPswd = "testuserpwd";
    public String recvAuthCode = "Received authorization code:";
    public String recvAccessToken = "Received from token endpoint: {\"access_token\":";
    public String appTitle = "Snoop Servlet";
    public String approvalForm = "javascript";
    public String autoauthz = "true";
    public String redirectAccessToken = "access_token=";
    public String loginPrompt = "Enter your username and password to login";
    public String clientSecret = "secret";
    public String snoopServlet = "Snoop Servlet";
    public String loginForm = "Enter your username and password to login";
    public String refreshToken = "Refresh Token: ";

    private static String[] expectedMessages;

    private static boolean runRemote = true;

    static {
        /*
         * Local mongoDB does not run on z/OS, use remote
         */
        //isZOS = LibertyServerUtils.isZOS();
        if (runRemote) {
            Log.info(thisClass, "staticSetup", "Will connect to remote mongoDB server.");
        } else {
            Log.info(thisClass, "staticSetup", "Will start local mongoDB server.");
        }
    }

    @Before
    public void beforeTest() {
        /*
         * Reset the HttpUnit exceptions thrown to their default states.
         */
        HttpUnitOptions.setExceptionsThrownOnErrorStatus(true);
        HttpUnitOptions.setExceptionsThrownOnScriptError(true);
    }

    /**
     * Sets up any configuration required for running the OAuth tests.
     */
    public void commonSetup(String _server, String... expectedMsgs) throws Exception {
        final String thisMethod = "commonSetup";
        Log.info(thisClass, thisMethod, "Entry", _server);
        server = LibertyServerFactory.getLibertyServer(_server);

        switch (_server) {
            case "com.ibm.ws.security.oauth-2.0.customstore.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore2.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore3.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore.bell.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore2.bell.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore3.bell.fat":
            case "com.ibm.ws.security.oauth-2.0.customstore.xor.fat":
                FATSuite.transformApps(server, "apps/oAuth20MongoSetup.war", "dropins/com.ibm.oauth.test.client.ear", "dropins/OauthTaiDemo.ear");
                break;
            case "com.ibm.ws.security.oauth-2.0.derby.fat":
            case "com.ibm.ws.security.oauth-2.0.derby2.fat":
            case "com.ibm.ws.security.oauth-2.0.derby3.fat":
            case "com.ibm.ws.security.oauth-2.0.derby.xor.fat":
                FATSuite.transformApps(server, "dropins/com.ibm.oauth.test.client.ear", "dropins/oAuth20DerbySetup.war", "dropins/OauthTaiDemo.ear");
                break;
            default:
                FATSuite.transformApps(server, "dropins/com.ibm.oauth.test.client.ear", "dropins/OauthTaiDemo.ear");
        }

        // TODO see why we're getting CWWKE0700W and CWWKE0701E: Circular reference detected trying to get service
        if (expectedMsgs != null) {
            expectedMessages = new String[4 + expectedMsgs.length];
            expectedMessages[0] = "CWWKE0701E";
            expectedMessages[1] = "CWWKE0700W";
            expectedMessages[2] = "CWWKE1102W";
            expectedMessages[3] = "CWWKE1106W";
            System.arraycopy(expectedMsgs, 0, expectedMessages, 4, expectedMsgs.length);
        } else {
            expectedMessages = new String[4];
            expectedMessages[0] = "CWWKE0701E";
            expectedMessages[1] = "CWWKE0700W";
            expectedMessages[2] = "CWWKE1102W";
            expectedMessages[3] = "CWWKE1106W";
        }

        LDAPUtils.addLDAPVariables(server);

        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/securitylibertyinternals-1.0.mf");

        // Add special files for MongoDB/CustomStore config
        if (_server.equals(MONGO_STORE_FEATURE_SERVER) || _server.equals(MONGO_STORE_FEATURE_SERVER2) || _server.equals(MONGO_STORE_FEATURE_SERVER3)
            || _server.equals(MONGO_STORE_FEATURE_SERVER_XOR)) {
            Log.info(thisClass, thisMethod, "Add CustomStore user feature");
            isRunningCustomStore = true;
            server.installUserBundle("security.custom.store_1.0");
            server.installUserFeature("customStoreSample-1.0");
            setupMongoDBConfig(server);
        } else if (_server.equals(MONGO_STORE_BELL_SERVER) || _server.equals(MONGO_STORE_BELL_SERVER2) || _server.equals(MONGO_STORE_BELL_SERVER3)) {
            Log.info(thisClass, thisMethod, "Add CustomStore Bell");
            isRunningCustomStore = true;
            setupMongoDBConfig(server);
        }

        // start the test server and wait for it to complete starting
        server.startServer();
        server.waitForStringInLog("port " + server.getHttpDefaultPort());// com.ibm.oauth.test.client OauthTaiDemo
        assertNotNull("The application com.ibm.oauth.test.client failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*com.ibm.oauth.test.client"));
        assertNotNull("The application OauthTaiDemo failed to start",
                      server.waitForStringInLog("CWWKZ0001I.*OauthTaiDemo"));
        assertNotNull("The security service was not ready in time",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The TCP Channel defaultHttpEndpoint-ssl did not start",
                      server.waitForStringInLog("CWWKO0219I.*ssl"));
        if (_server.startsWith(DERBY_STARTS_WITH) || _server.startsWith(MONG_STARTS_WITH)) {
            // The OAuthConfigDerby app works for both types of stores (database and custom), didn't rename when MongoDB was added
            assertNotNull("Provider OAuthConfigDerby config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthConfigDerby"));
        } else {
            assertNotNull("Provider OAuthConfigSample config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthConfigSample"));
            assertNotNull("Provider OAuthConfigTai config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthConfigTai"));
            assertNotNull("Provider OAuthConfigPublic config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthConfigPublic"));
            assertNotNull("Provider OAuthConfigNoFilter config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthConfigNoFilter"));
            assertNotNull("Provider OAuthMediatorProvider config was not processed in time",
                          server.waitForStringInLog("OAuth provider OAuthMediatorProvider"));
        }
        assertNotNull("OAuth role configuration was not processed in time",
                      server.waitForStringInLog("OAuth roles configuration successfully processed"));

        printTestStart();
        init();
        return;
    }

    private void setupMongoDBConfig(LibertyServer server) throws Exception {
        String methodName = "setupMongoDBConfig";
        if (runRemote) {
            setupRemoteMongoDBConfig(server);
            return;
        }

        /*
         * Get the MongoDB connection properties. These are read in from the
         * mongoDB.props file.
         */
        String mongodbName = "oauthMongoDB";
        String mongodbHost = "localHost";
        int mongodbPort = Network.getFreeServerPort();

        Log.info(thisClass, methodName, "Populate mongo db props file for CustomStoreSample use.");
        File tmpFile = new File("lib/LibertyFATTestFiles/", MONGO_PROPS_FILE);
        tmpFile.getParentFile().mkdirs();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
            try {
                out.write("DBNAME:" + mongodbName);
                out.write("\nHOST:" + mongodbHost);
                out.write("\nPORT:" + mongodbPort);
            } finally {
                out.close();
            }

            server.copyFileToLibertyServerRoot(MONGO_PROPS_FILE);
        } catch (IllegalStateException e) {
            Log.info(thisClass, methodName, "Failed to create props file " + MONGO_PROPS_FILE);
            e.printStackTrace();
        } finally {
            tmpFile.delete();
        }

        /*
         * Startup a MondoDB instance.
         */
        Log.info(thisClass, methodName, "Start embedded mongoDB server.");
        RuntimeConfig runtimeConfig = Defaults.runtimeConfigFor(Command.MongoD, LoggerFactory.getLogger(thisClass.getName()))
                        .build();
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
        MongodConfig builder = MongodConfig.builder()
                        .version(Version.Main.PRODUCTION/* Version.V3_6_5 */)
                        .net(new Net(mongodbHost, mongodbPort, Network.localhostIsIPv6()))
                        .build();
        mongodExecutable = starter.prepare(builder);
        mongodExecutable.start();

        // build variables to send to the setup servlet
        dbInfo = "&" + DB_NAME + "=" + mongodbName + "&" + DB_HOST + "=" + mongodbHost + "&" + DB_PORT + "=" + mongodbPort;
    }

    private void setupRemoteMongoDBConfig(LibertyServer server) throws Exception {
        Log.info(thisClass, "setupRemoteMongoDBConfig", "Adding MongoDB info to server.xml");
        // gets a remote host we can use
        MongoServerSelector.assignMongoServers(server);

        assertEquals("Should have 1 mongoDB ref defined in server config", 1, server.getServerConfiguration().getMongoDBs().size());

        // now populate a properties file for the CustomStoreSample to use
        MongoDBElement mongoConfig = server.getServerConfiguration().getMongoDBs().get(0);

        Log.info(thisClass, "setupRemoteMongoDBConfig", "Populate mongo db props file for CustomStoreSample use.");
        File tmpFile = new File("lib/LibertyFATTestFiles/", MONGO_PROPS_FILE);
        tmpFile.getParentFile().mkdirs();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(tmpFile));
            try {
                out.write("DBNAME:" + mongoConfig.getDatabaseName());
                out.write("\nHOST:" + mongoConfig.getMongo().getHostNames());
                out.write("\nPWD:" + mongoConfig.getMongo().getPassword());
                out.write("\nPORT:" + mongoConfig.getMongo().getPortList()[0]);
                out.write("\nUSER:" + mongoConfig.getMongo().getUser());
                out.write("\nUID:" + mongoTableUid);
            } finally {
                out.close();
            }

            server.copyFileToLibertyServerRoot(MONGO_PROPS_FILE);
        } catch (IllegalStateException e) {
            Log.info(thisClass, "setupRemoteMongoDBConfig", "Failed to create props file " + MONGO_PROPS_FILE);
            e.printStackTrace();
        } finally {
            tmpFile.delete();
        }

        // build variables to send to the setup servlet
        dbInfo = "&" + DB_NAME + "=" + mongoConfig.getDatabaseName() + "&" + DB_HOST + "=" + mongoConfig.getMongo().getHostNames() + "&" + DB_PWD + "="
                 + mongoConfig.getMongo().getPassword() + "&" + DB_PORT + "=" + mongoConfig.getMongo().getPortList()[0] + "&" + DB_USER + "=" + mongoConfig.getMongo().getUser()
                 + "&uid=" + mongoTableUid;
    }

    @After
    public void testTearDown() throws Exception {
        printTestEnd();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (runRemote) {
            try {
                /**
                 * Clean up the remote mongoDB database.
                 */
                String urlString = httpStart + "/oAuth20MongoSetup?port=" + new Integer(server.getHttpDefaultPort());

                urlString = urlString + "&dropDB=true" + dbInfo;
                URL setupURL = new URL(urlString);
                Log.info(thisClass, "tearDown", "dropURL: " + setupURL);
                HttpURLConnection con = (HttpURLConnection) setupURL.openConnection();
                con.setDoInput(true);
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setRequestMethod("GET");
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();

                // Send output from servlet to console output
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                    Log.info(thisClass, "tearDown", line);
                }

                con.disconnect();
            } catch (Throwable e) {
                Log.info(thisClass, "tearDown", "Exception calling dropDB for mongoDB. If this is a Derby test, ignore this message." + e);
            }
        }

        try {
            server.deleteFileFromLibertyServerRoot(MONGO_PROPS_FILE);
        } catch (Exception e) {
            Log.info(thisClass, "tearDown", "Exception removing MONGO_PROPS_FILE. If this is a Derby test, ignore this message." + e);
        }

        try {
            if (server != null && server.isStarted()) {
                server.stopServer(expectedMessages);
            }

        } finally {
            if (mongodExecutable != null) {
                mongodExecutable.stop();
            }
        }
    }

    public String conditSet(String defValue, String specificValue) {

        // System.out.println("conditSet: " + defValue + " " + specificValue);
        if (specificValue != null) {
            return specificValue;
        }
        return defValue;
    }

    /* Set some default values - tests can override these as needed */
    public void init() throws Exception {
        try {
            // InetAddress localHost = InetAddress.getLocalHost();
            // Get IP Address
            // String ipAddress = localHost.getHostAddress();
            // String ipName = localHost.getCanonicalHostName();
            String ipName = "localhost";
            httpStart = "http://" + ipName + ":" + server.getHttpDefaultPort();
            httpsStart = "https://" + ipName + ":"
                         + server.getHttpDefaultSecurePort();
            firstClientUrl = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/client.jsp";
            firstClientUrlSSL = httpsStart + "/" + Constants.OAUTHCLIENT_APP + "/client.jsp";
            clientRedirect = httpStart + "/" + Constants.OAUTHCLIENT_APP + "/redirect.jsp";
            authorizeEndpt = httpsStart
                             + "/oauth2/endpoint/OAuthConfigSample/authorize";
            tokenEndpt = httpsStart
                         + "/oauth2/endpoint/OAuthConfigSample/token";
            protectedResource = httpsStart + "/oauth2tai/snoop";
            refreshTokUrl = httpsStart + "/" + Constants.OAUTHCLIENT_APP + "/refresh.jsp";
            clientName = "client01";
            clientID = "client01";

        } catch (Exception e) {

            System.out.println("Exception occurred:");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    public WebForm fillClientForm(WebForm form) {

        // Set username
        form.setParameter("user_name", this.clientName);

        // Set Client ID
        form.setParameter("client_id", this.clientID);

        // Set redirect URL
        form.setParameter("redirect_uri", this.clientRedirect);

        // Set authorize endpoint
        form.setParameter("authorize_endpoint", this.authorizeEndpt);

        // Set token endpoint
        form.setParameter("token_endpoint", this.tokenEndpt);

        // Set protected resource
        form.setParameter("resource_endpoint", this.protectedResource);

        // Set state
        form.setParameter("state", "Lvj9Z2l8jMSMrtWG1F3Z");

        // Set scope
        form.setParameter("scope", "scope1 scope2");

        // set autoauthz
        form.setParameter("autoauthz", this.autoauthz);

        return form;
    }

    public WebForm fillClientForm2(WebForm form) {

        if (this.adminUser != null) {
            // Set username
            form.setParameter("user_id", this.adminUser);
        }

        if (this.adminPswd != null) {
            // Set password
            form.setParameter("user_pass", this.adminPswd);
        }

        // Set Client ID
        form.setParameter("client_id", this.clientID);

        // Set Client secret
        form.setParameter("client_secret", this.clientSecret);

        // Set token endpoint
        form.setParameter("token_endpoint", this.tokenEndpt);

        // Set protected resource
        form.setParameter("resource_endpoint", this.protectedResource);

        // Set scope
        form.setParameter("scope", "scope1 scope2");

        // Set autoauthz
        form.setParameter("autoauthz", this.autoauthz);

        return form;
    }

    public WebForm fillLoginForm(WebForm form) {

        form.setParameter("j_username", this.adminUser);
        form.setParameter("j_password", this.adminPswd);
        return form;
    }

    public WebRequest fillAuthorizationForm(WebRequest form) {

        form.setParameter("auto", "true");
        form.setParameter("response_type", "token");
        form.setParameter("user_name", this.clientName);
        form.setParameter("client_id", this.clientID);
        form.setParameter("redirect_uri", this.clientRedirect);
        form.setParameter("authorize_endpoint", this.authorizeEndpt);
        form.setParameter("state", "Lvj9Z2l8jMSMrtWG1F3Z");
        form.setParameter("scope", "scope1 scope2");
        form.setParameter("autoauthz", this.autoauthz);
        return form;
    }

    public void setupDerbyEntries(String schemaName) throws Exception {

        Log.info(thisClass, "setupDerbyEntries", "Create DataBases through the server");
        String urlString = httpStart + "/oAuth20DerbySetup?port=" + new Integer(server.getHttpDefaultPort());
        setupInner(urlString, schemaName);
    }

    private void setupInner(String urlString, String schemaName) throws Exception {
        if (schemaName != null)
            urlString = urlString + "&schemaName=" + schemaName;
        URL setupURL = new URL(urlString);
        Log.info(thisClass, "setupDerbyEntries", "setupURL: " + setupURL);
        HttpURLConnection con = (HttpURLConnection) setupURL.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        String sep = System.getProperty("line.separator");
        StringBuilder lines = new StringBuilder();

        // Send output from servlet to console output
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            lines.append(line).append(sep);
            Log.info(thisClass, "runInJDBCFATServlet", line);
        }

        con.disconnect();
    }

    public void setupMongDBEntries(String schemaName) throws Exception {
        Log.info(thisClass, "setupMongDBEntries", "Create DataBases through the server");
        String urlString = httpStart + "/oAuth20MongoSetup?port=" + new Integer(server.getHttpDefaultPort()) + dbInfo;
        setupInner(urlString, schemaName);
    }

    public void printAllCookies(WebConversation wc) {

        String[] cookieNames = wc.getCookieNames();
        for (int i = 0; i < cookieNames.length; i++) {
            Log.info(thisClass, "printAllCookies", "Cookie: " + cookieNames[i]
                                                   + " Value: " + wc.getCookieValue(cookieNames[i]));
        }
        return;
    }

    public static void printTestStart() {

        String theLine = "----- Start: ----------------------------------------------------";
        System.out.println(theLine);
    }

    public static void printTestEnd() {

        System.out.flush();
        String theLine = "----- End: ------------------------------------------------------";
        System.out.println(theLine);
    }

    /**
     * Return true if the response has an LTPA token cookie
     *
     * @param response
     * @return
     */
    boolean hasLTPAToken(WebResponse response) {
        boolean hasLtpaToken = false;
        String[] cookies = response.getNewCookieNames();
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.equals(LTPA_TOKEN)) {
                    hasLtpaToken = true;
                    break;
                }
            }
        }
        return hasLtpaToken;
    }

    void sslSetup() throws Exception {

        String thisMethod = "sslSetup";
        try {
            tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs,
                                               String string) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            //SSLContext ctx = SSLContext.getInstance("TLS");
            SSLContext ctx;
            try {
                Log.info(thisClass, thisMethod, "SSLContext.getInstance(TLS)");
                ctx = SSLContext.getInstance("TLS");
            } catch (Exception e) {
                Log.info(thisClass, thisMethod, "SSLContext.getInstance(SSL)");
                ctx = SSLContext.getInstance("SSL");
            }
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLContext.setDefault(ctx);
            Log.info(thisClass, "static initializer",
                     "Set default TrustManager");
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            Log.info(thisClass, "static initializer",
                     "Unable to set default TrustManager", e);
            throw new RuntimeException("Unable to set default TrustManager", e);
        }

    }

    public String checkMongoEntry(String httpString, Integer defaultPort, String clientID) throws Exception {
        String urlString = httpString + "/oAuth20MongoSetup?port=" + defaultPort
                           + "&schemaName=OAuthDBSchema" + "&clientID=" + clientID + "&checkSecret=true" + dbInfo;
        return checkEntryInner(urlString);
    }

    public String checkMongoIteration(String httpString, Integer defaultPort, String clientID) throws Exception {
        String urlString = httpString + "/oAuth20MongoSetup?port=" + defaultPort
                           + "&schemaName=OAuthDBSchema" + "&clientID=" + clientID + "&checkIteration=true" + dbInfo;
        return checkEntryInner(urlString);
    }

    public String checkMongoAlgorithm(String httpString, Integer defaultPort, String clientID) throws Exception {
        String urlString = httpString + "/oAuth20MongoSetup?port=" + defaultPort
                           + "&schemaName=OAuthDBSchema" + "&clientID=" + clientID + "&checkAlgorithm=true" + dbInfo;
        return checkEntryInner(urlString);
    }

    public static String checkDerbyEntry(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {

        String urlString = httpString + "/oAuth20DerbySetup?port=" + defaultPort
                           + "&schemaName=testSchema1" + "&clientID=" + clientID + "&checkSecret=true" + "&compID=" + compID;
        return checkEntryInner(urlString);
    }

    public static String checkDerbyIteration(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {

        String urlString = httpString + "/oAuth20DerbySetup?port=" + defaultPort
                           + "&schemaName=testSchema1" + "&clientID=" + clientID + "&checkIteration=true" + "&compID=" + compID;
        return checkEntryInner(urlString);
    }

    public static String checkDerbyAlgorithm(String httpString, Integer defaultPort, String clientID, String compID) throws Exception {

        String urlString = httpString + "/oAuth20DerbySetup?port=" + defaultPort
                           + "&schemaName=testSchema1" + "&clientID=" + clientID + "&checkAlgorithm=true" + "&compID=" + compID;
        return checkEntryInner(urlString);
    }

    public static String checkEntryInner(String urlString) throws Exception {

        String METHOD = "checkEntryInnter";
        HttpURLConnection con = null;

        String msg = null;
        try {

            Log.info(thisClass, METHOD, "Check requested type");
            URL setupURL = new URL(urlString);
            Log.info(thisClass, METHOD, "setupURL: " + setupURL);

            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
                msg = line;
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;

        }

        return msg;

    }

}
