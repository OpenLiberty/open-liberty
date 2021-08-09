/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.spnego.fat.config.CommonTestHelper;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.JDK11Expectations;
import com.ibm.ws.security.spnego.fat.config.JDK8Expectations;
import com.ibm.ws.security.spnego.fat.config.JDKExpectationTestClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.MsKdcHelper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.MessageBodyWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class SpnegoOIDCCommonTest extends AppPasswordsAndTokensCommonTest {

	private static final Class<?> c = SpnegoOIDCCommonTest.class;

	public static LibertyServer myServer;

	protected static BasicAuthClient myClient;
	protected static SSLBasicAuthClient mySslClient = null;
	protected static CommonTestHelper testHelper;
	protected static String spnegoTokenForTestClass = null;
	protected static String TARGET_SERVER = "";
	protected static boolean wasCommonTokenRefreshed = false;
	protected static JDKExpectationTestClass expectation;

	public static CommonValidationTools validationTools = new CommonValidationTools();

	protected static SpnegoCommonTestHelper helpers = new SpnegoCommonTestHelper();

	protected final static Krb5Helper krb5Helper = new Krb5Helper();
	protected static KdcHelper kdcHelper = null;

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

	}

	@Before
	public void preTestCheck() throws Exception {
		String thisMethod = "preTestCheck";
		Log.info(c, thisMethod, "Checking if new SPNEGO token should be created.");
		wasCommonTokenRefreshed = false;
		if (shouldCommonTokenBeRefreshed()) {
			createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
		}
	}
	
	public static String createSpnegoTokenForUser(String user, String password) throws Exception {
		Log.info(c, "createSpnegoTokenForUser", "^^^^Creating a new SPNEGO token for specific user: " + user +"^^^^");
		String spnegoToken = testHelper.createSpnegoToken(user, password, TARGET_SERVER,
				SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper);
		
		Log.info(c, "createSpnegoTokenForUser", "^^^^The token that was created looks like this: " + spnegoToken);
		return spnegoToken;
	}

	public static void createNewSpnegoToken(boolean setAsCommonSpnegoToken) throws Exception {
		createNewSpnegoToken(setAsCommonSpnegoToken, false);
	}

	/**
	 * Selects a user and creates a SPNEGO token for that user to be used by the
	 * tests in this class. If setAsCommonSpnegoToken is false, user1 will always be
	 * the user selected.
	 *
	 * @param setAsCommonSpnegoToken - Boolean indicating whether the newly created
	 *                               token should be set as the common SPNEGO token
	 *                               for all future tests and test classes.
	 *
	 * @param selectUser1            - disables the randomUser and instead creates a
	 *                               token using user1
	 * @throws Exception
	 */
	public static void createNewSpnegoToken(boolean setAsCommonSpnegoToken, boolean selectUser1) throws Exception {
		String thisMethod = "createNewSpnegoToken";
		Log.info(c, thisMethod, "^^^^Creating a new SPNEGO token^^^^");
		String user = InitClass.FIRST_USER;
		String password = InitClass.FIRST_USER_PWD;
		boolean isEmployee = true;
		boolean isManager = false;

		if (setAsCommonSpnegoToken) {
			Log.info(c, thisMethod,
					"The new SPNEGO token will be set as the common SPNEGO token for all future tests and test classes.");
			// Pick a user to use in creating the SPNEGO token; this allows for some desired
			// degree of randomness in the test cases
			Log.info(c, thisMethod, "Selecting user for whom the new common SPNEGO token will be created");
			if (selectUser1) {
				Log.info(c, thisMethod, "Selecting specified user: user1");
				InitClass.COMMON_TOKEN_USER = user;
			} else {
				InitClass.COMMON_TOKEN_USER = selectRandomUser();
			}
			setRolesForCommonUser(InitClass.COMMON_TOKEN_USER);

			user = InitClass.COMMON_TOKEN_USER;
			password = InitClass.COMMON_TOKEN_USER_PWD;
			isEmployee = InitClass.COMMON_TOKEN_USER_IS_EMPLOYEE;
			isManager = InitClass.COMMON_TOKEN_USER_IS_MANAGER;
		}

		Log.info(c, thisMethod, "SPNEGO token will be created for user: " + user + " (isEmployee=" + isEmployee
				+ ", isManager=" + isManager + ")");

		try {
			spnegoTokenForTestClass = testHelper.createSpnegoToken(user, password, TARGET_SERVER,
					SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper);
		} catch (Exception e) {
			String errorMsg = "Exception was caught while trying to create a SPNEGO token. Ensuing tests requiring use of this token might fail. "
					+ e.getMessage();
			Log.error(c, thisMethod, e, errorMsg);
			e.printStackTrace();
			throw (new Exception(errorMsg, e));
		}

		if (setAsCommonSpnegoToken) {
			InitClass.COMMON_TOKEN_CREATION_DATE = System.currentTimeMillis();
			DateFormat formatter = DateFormat.getTimeInstance(DateFormat.LONG);
			Log.info(c, thisMethod,
					"SPNEGO token created at " + formatter.format(new Date(InitClass.COMMON_TOKEN_CREATION_DATE))
							+ " and will be refreshed in " + (InitClass.TOKEN_REFRESH_LIFETIME_SECONDS / 60.0)
							+ " minute(s).");

			InitClass.COMMON_SPNEGO_TOKEN = spnegoTokenForTestClass;

			// Let any tests know that the token has been refreshed
			wasCommonTokenRefreshed = true;

			Log.info(c, thisMethod, "New common SPNEGO token: " + InitClass.COMMON_SPNEGO_TOKEN);
		}
	}

	private static String selectRandomUser() {
		List<String> users = new ArrayList<String>();
		users.add(InitClass.FIRST_USER);
		users.add(InitClass.SECOND_USER);
		return users.get((new Random().nextInt(users.size())));
	}

	private static void setRolesForCommonUser(String user) {
		if (user.equals(InitClass.FIRST_USER)) {
			InitClass.COMMON_TOKEN_USER_IS_EMPLOYEE = true;
			InitClass.COMMON_TOKEN_USER_IS_MANAGER = false;
			InitClass.COMMON_TOKEN_USER_PWD = InitClass.FIRST_USER_PWD;
		} else {
			InitClass.COMMON_TOKEN_USER_IS_EMPLOYEE = false;
			InitClass.COMMON_TOKEN_USER_IS_MANAGER = true;
			InitClass.COMMON_TOKEN_USER_PWD = InitClass.SECOND_USER_PWD;
		}
	}
	
	public static TestSettings addLocalhostToEndpoint(TestSettings updatedTestSettings) {
		
		return addLocalhostToEndpoint(updatedTestSettings, false);
	}
	
	public static TestSettings addLocalhostToEndpoint(TestSettings updatedTestSettings, boolean client) {
		Log.info(c, "addLocalhostToEndpoint", "Replacing localhost with the current server hostname....");
		updatedTestSettings.setClientRedirect(updatedTestSettings.getClientRedirect().replace("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setTokenEndpt(updatedTestSettings.getTokenEndpt().replaceAll("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setAuthorizeEndpt(updatedTestSettings.getAuthorizeEndpt().replaceAll("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setAppPasswordEndpt(updatedTestSettings.getAppPasswordsEndpt().replaceAll("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setRegistrationEndpt(updatedTestSettings.getRegistrationEndpt().replace("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setFirstClientURL(updatedTestSettings.getFirstClientURL().replace("localhost", testOPServer.getServerCanonicalHostname()));
        updatedTestSettings.setAppTokenEndpt(updatedTestSettings.getAppTokensEndpt().replace("localhost", testOPServer.getServerCanonicalHostname()));
        if(client) {
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace("localhost", testOPServer.getServerCanonicalHostname()));
        }
		return updatedTestSettings;
	}
    
	protected static TestServer commonSetUpRPServer(String requestedServer, String serverXML, String testType,
			List<String> addtlApps, boolean useDerby, List<String> addtlMessages, String targetUrl, String providerType)
			throws Exception {
		String thisMethod = "commonSetUpRPServer";
		Log.info(c, thisMethod, "***Starting testcase: " + requestedServer + "...");
		
		HashMap<String, String> boostraprops = null;
		addBootstrapProps(boostraprops);

		TestServer myRPserver = commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages,
				targetUrl, providerType, true, true);

		return myRPserver;
	}

	/**
	 * Sets up protected variables. No SPN, keytab file, or SPNEGO token will be
	 * created; the common SPN, keytab file, and SPNEGO token will be used.
	 *
	 * @param testServerName
	 * @param serverXml      - Server config file within the server's configs/
	 *                       directory to use. If null, a server.xml file is
	 *                       expected to be present in the server's root directory.
	 * @param checkApps      - List of apps to be validated as ready upon server
	 *                       start
	 * @param testProps      - Map of bootstrap property names and values to be set
	 *                       so they can be used in server configurations
	 * @param startServer    - Boolean indicating whether the server should be
	 *                       started once setup is complete
	 * @throws Exception
	 */

	protected static TestServer commonSetUpOPServer(String requestedServer, String serverXML, String testType,
			List<String> addtlApps, boolean useDerby, List<String> addtlMessages) throws Exception {
		return commonSetUpOPServer(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages,
				SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_CREATE_SSL_CLIENT,
				SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB, SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN,
				SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.USE_COMMON_KEYTAB);
	}

	/**
	 * Sets up protected variables. This method also determines whether an SPN,
	 * keytab, and/or initial SPNEGO token should be created, and whether the
	 * specified server should be started. It calls the constructor for the OIDC
	 * Code
	 *
	 * @param testServerName
	 * @param serverXml            - Server config file within the server's configs/
	 *                             directory to use. If null, a server.xml file is
	 *                             expected to be present in the server's root
	 *                             directory.
	 * @param checkApps            - List of apps to be validated as ready upon
	 *                             server start
	 * @param testProps            - Map of bootstrap property names and values to
	 *                             be set so they can be used in server
	 *                             configurations
	 * @param createSslClient
	 * @param createSpnAndKeytab
	 * @param spnRealm             - Realm to use for the SPN added to the keytab.
	 *                             If null, no realm will be appended to the SPN.
	 * @param createSpnegoToken
	 * @param setCommonSpnegoToken - Boolean indicating whether the new SPNEGO token
	 *                             (if one is created) should be set as the new
	 *                             common SPNEGO token for all future tests and test
	 *                             classes
	 * @param useCanonicalHostName
	 * @param copyCommonKeytab     - Boolean indicating whether the keytab file
	 *                             created during initial setup should be copied
	 *                             into this server's respective Kerberos resources
	 *                             directory
	 * @param startServer          - Boolean indicating whether the server should be
	 *                             started once setup is complete
	 * @throws Exception
	 */

	protected static TestServer commonSetUpOPServer(String requestedServer, String serverXML, String testType,
			List<String> addtlApps, boolean useDerby, List<String> addtlMessages, List<String> checkApps,
			Map<String, String> testProps, boolean createSslClient, boolean createSpnAndKeytab, String spnRealm,
			boolean createSpnegoToken, boolean setCommonSpnegoToken, boolean useCanonicalHostName,
			boolean copyCommonKeytab) throws Exception {

		String thisMethod = "commonSetUpOPServer";
		Log.info(c, thisMethod, "***Starting testcase: " + requestedServer + "...");

		// Add bootstrap properties
		addBootstrapProps(testProps);

		TestServer myOPserver = commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, addtlMessages,
				null, null, true, true);
		
		commonSpnegoConfigSetup(myOPserver.getServer(), requestedServer, serverXML, testProps, createSslClient, createSpnAndKeytab, spnRealm,
				createSpnegoToken, setCommonSpnegoToken, useCanonicalHostName, copyCommonKeytab);
		
		return myOPserver;

	}

	protected static TestServer commonSetUpRegistrationEP(String requestedServer, String serverXML, String testType,
			List<String> addtlApps, boolean useDerby, boolean useMongo, List<String> addtlMessages, String targetUrl,
			String providerType, boolean secStartMsg, boolean sslCheck, String tokenType, String certType,
			boolean reportViaJunit, Map<String, String> testProps, boolean createSslClient, boolean createSpnAndKeytab,
			String spnRealm, boolean createSpnegoToken, boolean setCommonSpnegoToken, boolean useCanonicalHostName,
			boolean copyCommonKeytab) throws Exception {
		
		String thisMethod = "commonSetUpRegistrationEP";
		Log.info(c, thisMethod, "***Starting testcase: " + requestedServer + "...");

		// Add bootstrap properties
		addBootstrapProps(testProps);

		TestServer myOPserver = commonSetUp(requestedServer, serverXML, testType, addtlApps, useDerby, useMongo,
				addtlMessages, targetUrl, providerType, secStartMsg, sslCheck, tokenType, certType, reportViaJunit);
		
		commonSpnegoConfigSetup(myOPserver.getServer(), requestedServer, serverXML, testProps, createSslClient, createSpnAndKeytab, spnRealm,
				createSpnegoToken, setCommonSpnegoToken, useCanonicalHostName, copyCommonKeytab);
		

		return myOPserver;

	}
	
	protected static void commonSpnegoConfigSetup(LibertyServer myServer, String requestedServer, String serverXML,
			Map<String, String> testProps, boolean createSslClient, boolean createSpnAndKeytab, String spnRealm,
			boolean createSpnegoToken, boolean setCommonSpnegoToken, boolean useCanonicalHostName,
			boolean copyCommonKeytab) throws Exception {
		commonSpnegoConfigSetup(myServer, requestedServer, serverXML,null, testProps, createSslClient, createSpnAndKeytab, spnRealm,
				createSpnegoToken, setCommonSpnegoToken, useCanonicalHostName, copyCommonKeytab, false);
	}
	
	
	
	protected static void commonSpnegoConfigSetup(LibertyServer server, String requestedServer, String serverXML,
			List<String> checkApps, Map<String, String> testProps, boolean createSslClient, boolean createSpnAndKeytab, String spnRealm,
			boolean createSpnegoToken, boolean setCommonSpnegoToken, boolean useCanonicalHostName,
			boolean copyCommonKeytab, boolean startServer) throws Exception {

		String thisMethod = "spnegoConfigSetup";
		Log.info(c, thisMethod, "***Starting testcase: " + requestedServer + "...");

		Log.info(c, thisMethod, "Adding SPNEGO Configuration to the OP server");
		 if (server != null) {
				myServer=server;
			}
		 else if (requestedServer != null) {
	            myServer= LibertyServerFactory.getLibertyServer(requestedServer);
	        }
		 else {
			 Exception e = new Exception(
						"There is no server specified, please specify one and try again.");
				Log.error(c, thisMethod, e);
				throw e;
		 }

		if (createSpnAndKeytab && copyCommonKeytab) {
			Exception e = new Exception(
					"The specified parameters for this method conflict with each other. The given parameters specify that an SPN and keytab file should be created, but also that the common keytab file should be used. These two files might not be identical, so it is unclear which keytab file should be used. Only one, or neither, of these parameters should be set to true.");
			Log.error(c, thisMethod, e);
			throw e;
		}

		
		testHelper = new CommonTestHelper(getMyServer(), myClient, mySslClient);
		createKrbConf(myServer);

		String hostName = testHelper.getTestSystemFullyQualifiedDomainName();
		int hostPort = myServer.getHttpDefaultPort();
		Log.info(c, thisMethod, "seeting up BasicauthClient with server " + hostName + "and port " + hostPort);

		myClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM,
				SPNEGOConstants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
		if (createSslClient) {
			mySslClient = new SSLBasicAuthClient(myServer);
		}

		// Copy in the new server config
		if (serverXML != null) {
			String config = testHelper.buildFullServerConfigPath(myServer, serverXML);
			testHelper.copyNewServerConfig(config);
			Log.info(c, thisMethod, "Using initial config: " + config);
		}

		if (createSpnAndKeytab) {
			try {
				Log.info(c, thisMethod, "Creating SPN and keytab");
				if (getKdcHelper() == null) {
					setKdcHelper(getKdcHelper(myServer));
				}
				getKdcHelper().createSpnAndKeytab(spnRealm, useCanonicalHostName, SPNEGOConstants.DEFAULT_CMD_ARGS);
			} catch (Exception e) {
				String message = e.getMessage();
				message = message.replace(InitClass.KDC_HOSTNAME, "InitClass.KDC_HOSTNAME");
        		message = message.replace(InitClass.KDC_USER, "InitClass.KDC_USER");
        		message = message.replace(InitClass.KDC_USER_PWD, "InitClass.KDC_USER_PWD");
				Log.info(c, thisMethod, "Got unexpected exception; no tests will be run: " + message);
				throw e;
			}
		}

		String fullyQualifiedDomainName = testHelper.getTestSystemFullyQualifiedDomainName();
		if (useCanonicalHostName) {
			Log.info(c, thisMethod, "Using the canonical host name in the target server SPN");
			TARGET_SERVER = fullyQualifiedDomainName;
		} else {
			Log.info(c, thisMethod, "Using the short host name in the target server SPN");
//			String shortHostName = getKdcHelper().getShortHostName(fullyQualifiedDomainName, true);
			TARGET_SERVER = InitClass.serverShortHostName;

		}

		if (createSpnegoToken || shouldCommonTokenBeRefreshed()) {
			// Only set this as the new common token if told to do so, or if the common
			// token needs to be refreshed
			boolean setAsCommonSpnegoToken = (createSpnegoToken) ? setCommonSpnegoToken : true;
			createNewSpnegoToken(setAsCommonSpnegoToken);
		}

		myServer.copyFileToLibertyInstallRoot("lib/features",
				"internalfeatures/securitylibertyinternals-1.0.mf");

		if (copyCommonKeytab) {
			Log.info(c, thisMethod, "Copying common keytab file into " + SPNEGOConstants.KRB_RESOURCE_LOCATION);
			// Liberty infrastructure already adds leading and trailing '/' characters when
			// copying
			String sanitizedKrbResourcePath = SPNEGOConstants.KRB_RESOURCE_LOCATION.substring(1,
					SPNEGOConstants.KRB_RESOURCE_LOCATION.length() - 1);
			myServer.copyFileToLibertyServerRoot(sanitizedKrbResourcePath,
					InitClass.KEYTAB_FILE_LOCATION);
		}
		
		if (startServer) {
			testHelper.startServer(serverXML, checkApps);
			// Wait for feature update to complete
			expectation.serverUpdate(getMyServer());
		}

	}
	
    protected static void createKrbConf() throws IOException {
        createKrbConf(getMyServer());
    }

    protected static void createKrbConf(LibertyServer testServer) throws IOException {
        String thisMethod = "createKrbConf";
        Log.info(c, thisMethod, "Creating krb.conf file inside the following path: " + testServer.getServerRoot() + SPNEGOConstants.SERVER_KRB5_CONFIG_FILE);
        FileOutputStream out = new FileOutputStream(testServer.getServerRoot() + SPNEGOConstants.SERVER_KRB5_CONFIG_FILE);
        out.write(InitClass.KRB5_CONF.getBytes());
        out.close();
    }

	private static HashMap<String, String> addBootstrapProps(Map<String, String> testProps)
			throws UnknownHostException {
		InetAddress inetAddr = java.net.InetAddress.getLocalHost();
		String serverHostName = inetAddr.getCanonicalHostName();
		HashMap<String, String> bootstrapProps = new HashMap<String, String>();
		bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_HOST_NAME, TARGET_SERVER);
		bootstrapProps.put("spnego.host.ipaddr", inetAddr.toString().split("/")[1]);
        bootstrapProps.put("kdc.hostname", InitClass.KDC_HOSTNAME);
        bootstrapProps.put("kdc.user.name", InitClass.KDC_USER);
        bootstrapProps.put("kdc.user.pwd", InitClass.KDC_USER_PWD);
        bootstrapProps.put("kdc.realm", InitClass.KDC_REALM);
        bootstrapProps.put("kdc.shortname", InitClass.KDC_HOST_SHORTNAME);
        bootstrapProps.put("first.user", InitClass.FIRST_USER);
        bootstrapProps.put("first.user.pwd", InitClass.FIRST_USER_PWD);
        bootstrapProps.put("second.user", InitClass.SECOND_USER);
        bootstrapProps.put("second.user.pwd", InitClass.SECOND_USER_PWD);
        bootstrapProps.put("z.user", InitClass.Z_USER);
        bootstrapProps.put("z.user.pwd", InitClass.Z_USER_PWD);
		if (testProps != null) {
			bootstrapProps.putAll(testProps);
		}

		setMiscBootstrapParms(bootstrapProps);

		return bootstrapProps;

	}

	/**
	 * Return the appropriate KdcHelper subtype for the corresponding KDC to be used
	 * by the tests.
	 *
	 * @param server
	 * @return
	 */
	private static KdcHelper getKdcHelper(LibertyServer server) {
		Log.info(c, "getKdcHelper", "Getting appropriate KdcHelper class");
		return new MsKdcHelper(getMyServer(), InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);
	}

	/**
	 * Determines whether the common SPNEGO token was created too far in the past to
	 * be usable in upcoming tests. If the JDK runtime is non-IBM JDK, always
	 * refresh the common token.
	 *
	 * @return
	 */
	protected static boolean shouldCommonTokenBeRefreshed() {
		String thisMethod = "shouldCommonTokenBeRefreshed";
		if (InitClass.OTHER_SUPPORT_JDKS) {
			return true;
		}
		long currentTime = System.currentTimeMillis();
		if (((currentTime - InitClass.COMMON_TOKEN_CREATION_DATE) / 1000) > InitClass.TOKEN_REFRESH_LIFETIME_SECONDS) {
			Log.info(c, thisMethod,
					"SPNEGO token lifetime has exceeded allowed time; recommend a new token should be created.");
			return true;
		}
		return false;
	}

	protected static String getCommonSpnegoToken() throws Exception {
		if (shouldCommonTokenBeRefreshed()) {
			createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
		}
		return InitClass.COMMON_SPNEGO_TOKEN;
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
		SpnegoOIDCCommonTest.kdcHelper = kdcHelper;
	}

	/**
	 * @return the myServer
	 */
	public static LibertyServer getMyServer() {
		return myServer;
	}

	/**
	 * Generic steps to invoke methods (in the correct ordering) to test with the RP
	 * and OP servers
	 *
	 * *************** NOTE *************** This method contains a subset of the
	 * function from the original genericRp. The methods that interact with SAML had
	 * to be modified to use HtmlUnit instead of HttpUnit when we switched to using
	 * Shibboleth. So, this method only processes a flow using SAMl and uses a
	 * WebClient instead of a WebConversation, ...
	 *
	 * @param testcase
	 * @param webClient
	 * @param settings
	 * @param testActions
	 * @param expectations
	 * @return
	 * @throws Exception
	 */
	@Override
	public Object genericRP(String testcase, WebClient webClient, TestSettings settings, String[] testActions,
			List<validationData> expectations) throws Exception {

		Object thePage = null;
		String thisMethod = "genericRP";
		msgUtils.printMethodName(thisMethod);

		List<endpointSettings> headers = null;

		settings.printTestSettings();
		msgUtils.printOAuthOidcExpectations(expectations);

		try {

			for (String entry : testActions) {
				Log.info(c, testcase, "Action to be performed: " + entry);
			}

			if (validationTools.isInList(testActions, SpnegoOIDCConstants.GET_SPNEGO_LOGIN_PAGE_METHOD)) {
				headers = eSettings.addEndpointSettingsIfNotNull(null, "Authorization",
						"Negotiate " + spnegoTokenForTestClass + " User-Agent: Firefox, Host: "
								+ testHelper.getTestSystemFullyQualifiedDomainName());
				thePage = helpers.getSpnegoLoginPage(testcase, webClient, settings, headers, expectations);
			}
			if (validationTools.isInList(testActions, SpnegoOIDCConstants.SEND_BAD_TOKEN)) {
				headers = eSettings.addEndpointSettingsIfNotNull(null, "Authorization",
						"Negotiate " + SpnegoOIDCConstants.SEND_BAD_TOKEN + " User-Agent: Firefox, Host: "
								+ testHelper.getTestSystemFullyQualifiedDomainName());
				thePage = helpers.getSpnegoLoginPage(testcase, webClient, settings, headers, expectations);
			}

			return thePage;

		} catch (Exception e) {

			Log.error(c, testcase, e, "Exception occurred");
			System.err.println("Exception: " + e);
			throw e;
		}
	}

	/**
	 * Generic steps to invoke and endpoint
	 *
	 * @param testcase
	 * @param webClient
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
	public Object invokeEndpointWithBody(String testcase, WebClient webClient, WebResponse inResponse, URL url,
			String method, String action, List<endpointSettings> parms, List<endpointSettings> headers,
			List<validationData> expectations, String source, String contentType) throws Exception {

		Object thePage = null;
		String thisMethod = "invokeEndpointWithBody";

		msgUtils.printMethodName(thisMethod);
		msgUtils.printOAuthOidcExpectations(expectations);

		try {
			com.gargoylesoftware.htmlunit.WebRequest requestSettings = null;
			String urlS = url.toString();

			if (method.equals(SpnegoOIDCConstants.POSTMETHOD)) {
				requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, contentType, null);
				requestSettings.setHttpMethod(HttpMethod.POST);

			} else {
				if (method.equals(SpnegoOIDCConstants.PUTMETHOD)) {
					requestSettings = new com.gargoylesoftware.htmlunit.WebRequest(url, contentType, null);
					requestSettings.setHttpMethod(HttpMethod.PUT);

				}
			}

			requestSettings.setRequestParameters(new ArrayList());
			helpers.setMarkToEndOfAllServersLogs();

			Log.info(c, thisMethod, "Endpoint URL: " + url.toString());

			if (parms != null) {
				for (endpointSettings parm : parms) {
					Log.info(c, thisMethod, "Setting request parameter:  key: " + parm.getKey() + " value: " + parm.getValue());
					requestSettings.getRequestParameters().add(new NameValuePair(parm.getKey(), parm.getValue()));
					webClient.addRequestHeader(parm.getKey(), parm.getValue());

				}
			} else {
				Log.info(c, thisMethod, "No parameters to set");
			}

			if (headers != null) {
				Map<String, String> headerParms = new HashMap<String, String>();
				for (endpointSettings header : headers) {
					Log.info(c, thisMethod,
							"Setting header field:  key: " + header.getKey() + " value: " + header.getValue());
					headerParms.put(header.getKey(), header.getValue());
					Log.info(c, thisMethod, "Setting header in the webclient field:  key: " + header.getKey()
							+ " value: " + header.getValue());
					webClient.addRequestHeader(header.getKey(), header.getValue());

				}
				requestSettings.setAdditionalHeaders(headerParms);
				webClient.addRequestHeader("Content-Type", "application/json");
				requestSettings.setRequestBody(source);
			} else {
				Log.info(c, thisMethod, "No header fields to add");
			}

			thePage = webClient.getPage(requestSettings);

			// make sure the page is processed before continuing
			Log.info(c, "waitBeforeContinuing", "Waiting for HtmlUnit to finish its business");
			webClient.waitForBackgroundJavaScriptStartingBefore(5000);
			webClient.waitForBackgroundJavaScript(5000);

			msgUtils.printResponseParts(thePage, thisMethod, "Invoke with Parms and Headers: ");

		} catch (HttpException e) {

			Log.info(c, thisMethod, "Exception message: " + e.getMessage());
			Log.info(c, thisMethod, "Exception Response: " + e.getResponseCode());
			Log.info(c, thisMethod, "Exception Stack: " + e.getStackTrace().toString());
			e.printStackTrace();
			Log.info(c, thisMethod, "Exception Response message" + e.getResponseMessage());
			Log.info(c, thisMethod, "Exception Cause: " + e.getCause());
			System.err.println("Exception: " + e);

			validationTools.validateException(expectations, action, e);

		} catch (Exception e) {

			Log.info(c, thisMethod, "Exception message: " + e.getMessage());
			Log.info(c, thisMethod, "Exception Stack: " + e.getStackTrace());
			Log.info(c, thisMethod, "Exception Response message" + e.getLocalizedMessage());
			Log.info(c, thisMethod, "Exception Cause: " + e.getCause());

			validationTools.validateException(expectations, action, e);

		}

		validationTools.validateResult(thePage, action, expectations, null);
		return thePage;
	}
	
	/**
     * Generic steps to invoke methods (in the correct ordering) to test with
     * the OP server
     *
     * @param testcase
     * @param webClient
     * @param settings
     * @param testActions
     * @param expectations
     * @return
     * @throws Exception
     */

	   public Object genericOP(String testcase, WebClient webClient, TestSettings settings, String[] testActions, List<validationData> expectations) throws Exception {

	        String thisMethod = "genericOP";
	        msgUtils.printMethodName(thisMethod);

	        settings.printTestSettings();
	        msgUtils.printOAuthOidcExpectations(expectations, testActions);

	        Object thePage = null;
	        List<endpointSettings> headers = null;
	        if(settings.getAdminUser() == InitClass.SECOND_USER) {
	        	  headers= eSettings.addEndpointSettingsIfNotNull(null, "Authorization",
	  					"Negotiate " + createSpnegoTokenForUser(settings.getAdminUser(), settings.getAdminPswd()) + " User-Agent: Firefox, Host: "
	  							+ testHelper.getTestSystemFullyQualifiedDomainName());
	        }
	        else if (settings.getAdminUser() == SpnegoOIDCConstants.NTLM_TOKEN){
	        	headers= eSettings.addEndpointSettingsIfNotNull(null,"Authorization",
	                    SpnegoOIDCConstants.SPNEGO_NEGOTIATE + "THIS+IS+A+BAD+TOKEN+THISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKENTHISISABADTOKEN"
	                    + " User-Agent: Firefox, Host: "+ testHelper.getTestSystemFullyQualifiedDomainName());
	        }
	        else {
	        headers= eSettings.addEndpointSettingsIfNotNull(null, "Authorization",
					"Negotiate " + createSpnegoTokenForUser(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD) + " User-Agent: Firefox, Host: "
							+ testHelper.getTestSystemFullyQualifiedDomainName());
	        }

	        try {

	            for (String entry : testActions) {
	                Log.info(c, testcase, "Action to be performed: " + entry);
	            }
	            
	            if (validationTools.isInList(testActions, SpnegoOIDCConstants.GET_SPNEGO_LOGIN_PAGE_METHOD)) {
					thePage = helpers.getSpnegoLoginPage(testcase, webClient, settings, headers, expectations);
				}

	            if (validationTools.isInList(testActions, Constants.INVOKE_OAUTH_CLIENT)) {
	                thePage = helpers.invokeFirstClient(testcase, webClient, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.SUBMIT_TO_AUTH_SERVER)) {
	                thePage = helpers.submitToAuthServer(testcase, webClient, thePage, settings, expectations, Constants.SUBMIT_TO_AUTH_SERVER);
	            }

	            if (validationTools.isInList(testActions, Constants.INVOKE_AUTH_ENDPOINT)) {
	                thePage = invokeAuthorizationEndpoint(testcase, webClient, thePage, headers, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.BUILD_POST_SP_INITIATED_REQUEST)) {
	                thePage = buildPostSolicitedSPInitiatedRequest(testcase, webClient, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.PERFORM_IDP_LOGIN)) {
	                thePage = helpers.performIDPLogin(testcase, webClient, (HtmlPage) thePage, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.PERFORM_LOGIN)) {
	                thePage = helpers.performLogin(testcase, webClient, thePage, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.INVOKE_ACS)) {
	                thePage = helpers.invokeACS(testcase, webClient, thePage, settings, expectations);
	            }
	            
	            if (validationTools.isInList(testActions, Constants.INVOKE_TOKEN_ENDPOINT)) {
	                thePage = invokeTokenEndpoint(testcase, webClient, thePage, settings, expectations);
	            }

	            if (validationTools.isInList(testActions, Constants.INVOKE_PROTECTED_RESOURCE)) {
	                thePage = helpers.invokeProtectedResource(testcase, webClient, thePage, settings, expectations);
	            }

	            return thePage;

	        } catch (Exception e) {

	            Log.error(c, testcase, e, "Exception occurred");
	            System.err.println("Exception: " + e);
	            throw e;
	        }
	    }
	   
	   public Object invokeAuthorizationEndpoint(String testCase, WebClient webClient, Object startPage, TestSettings settings, List<validationData> expectations) throws Exception {

	        return invokeAuthorizationEndpoint(testCase, webClient, startPage, null, settings, expectations, SpnegoOIDCConstants.USE_SPNEGO);

	    }
	   
	   public Object invokeAuthorizationEndpoint(String testCase, WebClient webClient, Object startPage, List<endpointSettings> headers, TestSettings settings, List<validationData> expectations) throws Exception {

	        return invokeAuthorizationEndpoint(testCase, webClient, startPage, headers, settings, expectations, SpnegoOIDCConstants.USE_SPNEGO);

	    }

	    public Object invokeAuthorizationEndpoint(String testCase, WebClient webClient, Object startPage,List<endpointSettings> headers, TestSettings settings, List<validationData> expectations, boolean useSpnego) throws Exception {

	        String thisMethod = "invokeAuthorizationEndpoint";
	        msgUtils.printMethodName(thisMethod);

		if (headers == null)
		{
			createNewSpnegoToken(SPNEGOConstants.SET_AS_COMMON_TOKEN);
			if (useSpnego == SpnegoOIDCConstants.USE_SPNEGO) {
				Log.info(c, thisMethod, "Building Spnego Token and adding it to the header");
				headers = eSettings.addEndpointSettingsIfNotNull(null, "Authorization",
						"Negotiate " + spnegoTokenForTestClass + " User-Agent: Firefox, Host: "
								+ testHelper.getTestSystemFullyQualifiedDomainName());
			}
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

	        Log.info(c, thisMethod, "WebClient isJavaScriptEnabled: " + webClient.getOptions().isJavaScriptEnabled());

	        Object response = genericInvokeEndpoint(testCase, webClient, startPage, settings.getAuthorizeEndpt(), Constants.GETMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers, expectations, settings);

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
	    public Object invokeAppPasswordsEndpoint_create(String testCase, WebClient webClient, TestSettings settings, String accessToken, String appName, String usedBy, String clientLocation, List<validationData> expectations) throws Exception {
	        String thisMethod = "invokeAppPasswordsEndpoint_create";
	        msgUtils.printMethodName(thisMethod);

	        Log.info(c, thisMethod, "Generating app-password for access_token: [" + accessToken + "] and app_name: [" + appName + "] using clientId: [" + settings.getClientID() + "] and clientSecret: [" + settings.getClientSecret() + "]");

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

	        Object response = genericInvokeEndpoint(_testName, webClient, null, settings.getAppPasswordsEndpt(), Constants.POSTMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_CREATE, parms, headers, expectations, settings);

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
	     * @param clientLocation
	     *            - flag indicating if clientid/clientSecret should be included in the header or as a parameter
	     * @param expectations
	     *            - the expected results/output from invoking the endpoint
	     * @throws Exception
	     *             - this method should NOT result in an exception - if one is thrown, the calling test case should report an
	     *             error
	     */
	    public void invokeAppPasswordsEndpoint_revoke(String testCase, WebClient webClient, TestSettings settings, String accessToken, String userId, String appId, String clientLocation, List<validationData> expectations) throws Exception {
	        String thisMethod = "invokeAppPasswordsEndpoint_revoke";
	        msgUtils.printMethodName(thisMethod);

	        Log.info(c, thisMethod, "Revoking app-passwords for access_token: [" + accessToken + "], user_id: [" + userId + "] and app_id: [" + appId + "] using clientId: [" + settings.getClientID() + "] and clientSecret: [" + settings.getClientSecret() + "]");

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

	        genericInvokeEndpoint(testCase, webClient, null, urlString, Constants.DELETEMETHOD, Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE, parms, headers, expectations, settings, false);

	    }
	

}