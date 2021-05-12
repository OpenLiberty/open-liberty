/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml20.fat.commonTest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonIOTools;
import com.ibm.ws.security.fat.common.CommonTest;
import com.ibm.ws.security.fat.common.ShibbolethHelpers;
import com.ibm.ws.security.fat.common.TestHelpers;
import com.ibm.ws.security.fat.common.ValidationData;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.apps.AppConstants;
import com.ibm.ws.security.fat.common.config.settings.BaseConfigSettings;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.MySkipRule;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.SAMLConfigSettings;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.common.apiservices.Bootstrap;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

public class SAMLCommonTest extends CommonTest {

    @ClassRule
    public static SAMLTestServer dummyServer = new SAMLTestServer();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    public static class skipIfExternalLDAP extends MySkipRule {
        @Override
        public Boolean callSpecificCheck() {
            Log.info(thisClass, "skipIfExternalLDAP", "Should we skip the test: " + usingExternalLDAPServer);
            if (usingExternalLDAPServer) {
                testSkipped();
            }
            return usingExternalLDAPServer;
        }

    }

    final static Class<?> thisClass = SAMLCommonTest.class;
    protected static TrustManager[] trustAllCerts = null;
    public static SAMLCommonTestTools cttools = new SAMLCommonTestTools();
    public static CommonIOTools cioTools = new CommonIOTools();
    public static SAMLCommonValidationTools validationTools = new SAMLCommonValidationTools();
    public static SAMLMessageTools msgUtils = new SAMLMessageTools();
    public ValidationData vData = new ValidationData(SAMLConstants.ALL_TEST_ACTIONS);
    public static ShibbolethHelpers shibbolethHelpers = new ShibbolethHelpers();
    public static SAMLTestServer testSAMLServer = null;
    public static SAMLTestServer testSAMLServer2 = null;
    public static SAMLTestServer testSAMLOIDCServer = null;
    public static SAMLTestServer testAppServer = null;
    public static SAMLTestServer testOIDCServer = null;
    public static SAMLTestServer testIDPServer = null;
    protected static String hostName = "localhost";
    protected static SAMLTestSettings testSettings = null;
    protected static SAMLConfigSettings samlConfigSettings = new SAMLConfigSettings();
    protected static SAMLCommonTestHelpers helpers = null;
    protected static String flowType;
    protected static Boolean copyMetaData = true;
    public static String spCfgNameExtension = "";
    //    public static String idpSupportedType = SAMLConstants.TFIM_TYPE;
    public static String idpSupportedType = SAMLConstants.SHIBBOLETH_TYPE;
    //	public static String idpSupportedType = SAMLConstants.ADFS_TYPE ;
    public static String[] validPortNumbers = SAMLConstants.SAML_SUPPORTED_PORTS;
    protected static String baseSamlServerConfig = null;
    protected static String baseSamlServer2Config = null;
    protected static String baseSamlOidcServerConfig = null;
    protected static String baseAppServerConfig = null;
    protected static String baseOidcServerConfig = null;
    protected static String chosenVersion = null;

    protected static final String IBMjdkBuilderAttribute = "org.apache.xerces.util.SecurityManager";
    protected static final String ORACLEjdkBuilderAttribute = "com.sun.org.apache.xerces.internal.util.SecurityManager";

    protected static List<SAMLTestServer> serverRefList = new ArrayList<SAMLTestServer>();
    protected static List<CommonLocalLDAPServerSuite> ldapRefList = new ArrayList<CommonLocalLDAPServerSuite>();
    protected static boolean cipherMayExceed128 = false;
    public static boolean usingExternalLDAPServer = false;

    @Rule
    public final TestName testName = new TestName();
    //    public String _testName = "";

    public static SAMLInstallUninstallUserFeature samlUserMappingUserFeature = null;

    // To be overridden by whoever needs to install user Feature
    public static void installUserFeature(SAMLTestServer aServer) throws Exception {
        if (samlUserMappingUserFeature != null) {
            samlUserMappingUserFeature.installUserFeature(aServer);
        } else {
            Log.info(thisClass, "installUserFeature", "No SAMLInstallUninstallUserFeature(No User Feature Installer)");
        }
    }; // install user feature in here

    // To be overridden by whoever needs to install user Feature
    public static void uninstallUserFeature() throws Exception {
        if (samlUserMappingUserFeature != null) {
            samlUserMappingUserFeature.uninstallUserFeature();
        } else {
            Log.info(thisClass, "uninstallUserFeature", "No SAMLInstallUninstallUserFeature(No User Feature Installer)");
        }
    }; // install user feature in here

    /**
     * initialize global variables - this shared class is used by all test classes within
     * a project. If something is set by one test class, the following test classes will
     * pick up that value - clean them out between test classes...
     */
    public static void initCommonVars() {
        testSAMLServer = null;
        testSAMLServer2 = null;
        testSAMLOIDCServer = null;
        testOIDCServer = null;
        testAppServer = null;
        testIDPServer = null;
        testSettings = null;
        samlConfigSettings = new SAMLConfigSettings();
        helpers = null;
        timeoutCounter = 0;
        //allowableTimeoutCount = 0;
        flowType = null;
        copyMetaData = true;
        spCfgNameExtension = "";
        idpSupportedType = SAMLConstants.SHIBBOLETH_TYPE;
        validPortNumbers = SAMLConstants.SAML_SUPPORTED_PORTS;
        baseSamlServerConfig = null;
        baseSamlServer2Config = null;
        baseSamlOidcServerConfig = null;
        baseAppServerConfig = null;
        baseOidcServerConfig = null;
        chosenVersion = null;
        cipherMayExceed128 = false;
        usingExternalLDAPServer = false;
    }

    /**
     * Sets up any configuration required for running the tests. - Starts the
     * server, which should start the applications in dropins or any other apps
     * specified in the server*.xml specified - Waits for the appropriate ports
     * - Waits for the test apps to start - It sets up the trust manager. -
     * Defines some global variables for use by the follow on tests...
     */
    public static SAMLTestServer commonSetUp(String requestedServer,
                                             String serverXML, String testType, String serverType,
                                             List<String> addtlApps, List<String> addtlMessages) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, serverType, addtlApps, addtlMessages, true, null, null);

    }

    public static SAMLTestServer commonSetUp(String requestedServer,
                                             String serverXML, String testType, String serverType,
                                             List<String> addtlApps, List<String> addtlMessages, String callbackHandler, String feature) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, serverType, addtlApps, addtlMessages, true, callbackHandler, feature);

    }

    public static SAMLTestServer commonSetUp(String requestedServer,
                                             String serverXML, String testType, String serverType,
                                             List<String> addtlApps, List<String> addtlMessages, Boolean checkForSecuityStart) throws Exception {

        return commonSetUp(requestedServer, serverXML, testType, serverType, addtlApps, addtlMessages, checkForSecuityStart, null, null);

    }

    public static SAMLTestServer commonSetUp(String requestedServer,
                                             String serverXML, String testType, String serverType,
                                             List<String> addtlApps, List<String> addtlMessages, Boolean checkForSecuityStart, String callbackHandler,
                                             String feature) throws Exception {

        String thisMethod = "commonSetUp";
        msgUtils.printMethodName(thisMethod);

        // common setup for the tests/clients
        TestHelpers.setupSSLClient();
        TestHelpers.initHttpUnitSettings();

        helpers = new SAMLCommonTestHelpers();

        testSettings = new SAMLTestSettings();
        if (serverType.contains("SAML")) {
            //            testSettings.selectIDPServer(idpSupportedType);
            if (testIDPServer != null) {
                testSettings.selectIDPServer(testIDPServer);
            }
        }

        timeoutCounter = 0;
        //        allowableTimeoutCount = 0;
        //		Integer defaultPort = null;
        String httpString = null;
        String httpsString = null;
        Log.info(thisClass, thisMethod, "requested server: " + requestedServer);
        SAMLTestServer aTestServer = null;
        try {
            String outputServerXml = null;
            if (serverXML.endsWith(BaseConfigSettings.BASE_CONFIG_SUFFIX)) {
                // Config file names with the ".base" suffix will have that suffix removed before being written
                Log.info(thisClass, thisMethod, "Removing \"" + BaseConfigSettings.BASE_CONFIG_SUFFIX + "\" suffix from the config file name");
                outputServerXml = serverXML.substring(0, serverXML.lastIndexOf(BaseConfigSettings.BASE_CONFIG_SUFFIX));
            }
            // The usable server xml should NOT be the one ending in ".base"
            String usableServerXml = (outputServerXml != null) ? outputServerXml : serverXML;

            if (callbackHandler == null) {
                aTestServer = new SAMLTestServer(requestedServer, usableServerXml, serverType);
            } else {
                Log.info(thisClass, "commonSetup", "callbackHandler: " + callbackHandler + " feature: " + feature);
                aTestServer = new SAMLTestServer(requestedServer, usableServerXml, serverType, callbackHandler, feature);
            }

            aTestServer.removeServerConfigFiles();
            aTestServer.setServerNameAndHostIp();

            if (!checkForSecuityStart) {
                aTestServer.setSkipSecurityReadyMsg();
            }

            List<String> messages = aTestServer.getDefaultStartMessages(serverType);
            if (addtlMessages != null && !addtlMessages.isEmpty()) {
                messages.addAll(addtlMessages);
            }

            //            List<String> checkApps = aTestServer.getDefaultTestApps(addtlApps);
            List<String> checkApps = aTestServer.getDefaultTestApps(null, requestedServer);
            for (String c : checkApps) {
                Log.info(thisClass, thisMethod, "Loop at after Default Apps: " + c);
            }
            if (addtlApps != null && !addtlApps.isEmpty()) {
                checkApps.addAll(addtlApps);
            }

            for (String c : checkApps) {
                // TODO ayoho: checkApps contains duplicate entries if getDefaultTestApps() doesn't do anything
                Log.info(thisClass, thisMethod, "Loop at end: " + c);
            }

            updateConfigFileWithDefaultSettings(aTestServer, serverXML, outputServerXml);

            Machine machine = aTestServer.getServer().getMachine();
            Bootstrap bootstrap = Bootstrap.getInstance();
            String z = LibertyFileManager.getInstallPath(bootstrap);

            Log.info(thisClass, thisMethod, "Install path is: " + z);
            Log.info(thisClass, thisMethod, "Temp is: " + machine.getTempDir().getAbsolutePath());
            //            machine.getTempDir().copyFromSource(srcFile, true, true) ;
            Log.info(thisClass, thisMethod, "Jar command is: " + aTestServer.getServer().getMachineJavaJarCommandPath());
            Log.info(thisClass, thisMethod, "Server install root is: " + aTestServer.getServer().getInstallRoot());

            if (serverType.equals(SAMLConstants.IDP_SERVER_TYPE)) {
                CommonLocalLDAPServerSuite one = new CommonLocalLDAPServerSuite();
                CommonLocalLDAPServerSuite two = new CommonLocalLDAPServerSuite();
                one.ldapSetUp();
                ldapRefList.add(one);
                two.ldapSetUp();
                ldapRefList.add(two);
                // we're having an issue with the in memory LDAP server on z/OS, added a method to see if it can accept requests,
                // if NOT, we'll use a "external" LDAP server (Shibboleth allows for failover to additional LDAP servers, but,
                // it doesn't allow different bindDN, bindPassword, ...)
                // this method will add properties to bootstrap.properties that will point to a hopefully working LDAP server
                //                int ldapPort = one.getLdapPort();
                //                int ldapSSLPort = one.getLdapSSLPort();
                //                Log.info(thisClass, "setupBeforeTest", "ldap Port in Common setup is: " + ldapPort);
                usingExternalLDAPServer = shibbolethHelpers.updateToUseExternalLDaPIfInMemoryIsBad(aTestServer, Integer.toString(one.getLdapPort()),
                                                                                                   Integer.toString(one.getLdapSSLPort()), Integer.toString(two.getLdapPort()),
                                                                                                   Integer.toString(two.getLdapSSLPort()));
                shibbolethHelpers.setShibbolethPropertiesForTestMachine(aTestServer);
            }

            switch (requestedServer) {
                case ("com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.sp"):
                case ("com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.config.sp"):
                    Log.info(thisClass, thisMethod, "in sp case");
                    transformApps(aTestServer.getServer(), "dropins/SAML_Demo.ear", "dropins/testmarker.war", "test-apps/samlclient.war", "test-apps/jaxrsclient.war");
                    break;
                case ("com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.rs"):
                case ("com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.config.rs"):
                    Log.info(thisClass, thisMethod, "in rs case");
                    transformApps(aTestServer.getServer(), "dropins/SAML_Demo.ear", "dropins/testmarker.war", "test-apps/samlclient.war", "test-apps/helloworld.war");
                    break;
                case ("com.ibm.ws.security.saml.sso-2.0_fat.jaxrs.merged_sp_rs"):
                    Log.info(thisClass, thisMethod, "in merged case");
                    transformApps(aTestServer.getServer(), "dropins/SAML_Demo.ear", "dropins/testmarker.war", "test-apps/samlclient.war", "test-apps/jaxrsclient.war",
                                  "test-apps/helloworld.war");
                    break;
                case ("com.ibm.ws.security.saml.sso_fat.logout"):
                case ("com.ibm.ws.security.saml.sso_fat.logout.server2"):
                    Log.info(thisClass, thisMethod, "in logout case");
                    transformApps(aTestServer.getServer(), "dropins/SAML_Demo.ear", "dropins/testmarker.war", "test-apps/samlclient.war", "test-apps/httpServletRequestApp.war");
                    break;
                case ("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth"):
                    transformApps(aTestServer.getServer(), "dropins/testmarker.war", "test-apps/idp.war");
                    break;
                default:
                    Log.info(thisClass, thisMethod, "in default case");
                    transformApps(aTestServer.getServer(), "dropins/SAML_Demo.ear", "dropins/testmarker.war", "test-apps/samlclient.war");
                    break;
            }

            Log.info(thisClass, thisMethod, "files: " + aTestServer.getServer().pathToAutoFVTTestFiles + "/buildWorkAround");
            if (LibertyFileManager.libertyFileExists(machine, aTestServer.getServer().pathToAutoFVTTestFiles + "/buildWorkAround")) {
                Log.info(thisClass, thisMethod, "Found buildWorkAround");
                List<String> mfFiles = aTestServer.getServer().listAutoFVTTestFiles(machine, "buildWorkAround/lib/features/", "mf");
                List<String> jarFiles = aTestServer.getServer().listAutoFVTTestFiles(machine, "buildWorkAround/lib/", "jar");

                for (String x : mfFiles) {
                    Log.info(thisClass, thisMethod, "Copying file: " + x);
                    aTestServer.getServer().copyFileToLibertyInstallRoot("lib/features", "buildWorkAround/lib/features/" + x);
                }
                for (String x : jarFiles) {
                    Log.info(thisClass, thisMethod, "Copying file: " + x);
                    aTestServer.getServer().copyFileToLibertyInstallRoot("lib", "buildWorkAround/lib/" + x);
                }
            }

            Log.info(thisClass, thisMethod, "calling LDAPUtil.addLDAPVariables", null);
            LDAPUtils.addLDAPVariables(aTestServer.getServer());
            Log.info(thisClass, thisMethod, "called LDAPUtil.addLDAPVariables", null);

            aTestServer.addIDPServerProp(testSettings.getIdpRoot());

            //TODO - chc - consolidate the next 2 chunks after OIDC is updated for shibboleth
            Log.info(thisClass, thisMethod, "Server type is: " + serverType);
            Log.info(thisClass, thisMethod, "Is testIDPServer already set: " + (testIDPServer != null));
            if (serverType.equals(SAMLConstants.SAML_SERVER_TYPE) && testIDPServer != null) {
                shibbolethHelpers.fixShibbolethInfoinSPServer(aTestServer, testIDPServer);
            }
            if (serverType.equals(SAMLConstants.SAML_APP_SERVER_TYPE) && testIDPServer != null) {
                shibbolethHelpers.fixShibbolethInfoinSPServer(aTestServer, testIDPServer);
            }

            // need idp ports for all servers
            if (testIDPServer != null) {
                aTestServer.addShibbolethProp("idpPort", testIDPServer.getHttpDefaultPort().toString());
                aTestServer.addShibbolethProp("idpSecurePort", testIDPServer.getHttpDefaultSecurePort().toString());
            }
            if (copyMetaData) {
                aTestServer.copyDefaultSPConfigFiles(testSettings.getIdpRoot());
            }
            copyMetaData = true;

            addToServerRefList(aTestServer);
            installUserFeature(aTestServer); // for installing user feature

            // choose if we test with servlet30 or servlet31 and copy the appropriate specific files to their generic name
            chooseServletLevel(aTestServer);

            //			Log.info(thisClass, thisMethod, "Choosing which feature group to use");
            //			String featureConfigFile = "saml_only_features_servlet30.xml";
            //			if (!System.getProperty("java.version").contains("1.6")) {
            //				String[] featureConfigFiles = new String[] { "saml_only_features_servlet30.xml", "saml_only_features_servlet31.xml" };
            //				featureConfigFile = chooseRandomEntry(featureConfigFiles);
            //			} else {
            //				Log.info(thisClass, thisMethod, "Running on JDK 6, so the servlet-3.0 feature group will be used");
            //			}
            //			Log.info(thisClass, thisMethod, "Feature group chosen: " + featureConfigFile);
            //			String importsDir = aTestServer.getServer().getServerRoot() + File.separator + "imports";
            //			LibertyFileManager.copyFileIntoLiberty(aTestServer.getServer().getMachine(), importsDir, "saml_only_features.xml", importsDir + File.separator + featureConfigFile);

            // start the server - if it fails to start the Junit flag is what causes the class to error immediately
            // SAML requires port 8020, so tell startServer to wait for it
            aTestServer.startServer(usableServerXml, null, checkApps, messages, SAMLConstants.JUNIT_REPORTING, new int[] { 8020 });

            httpString = aTestServer.getHttpString();
            httpsString = aTestServer.getHttpsString();
            // set the server's default ports - if the server created was saml server2, or an OP, RP, ... with
            // different default ports, it is up to the caller to reset the "Server" ports
            aTestServer.setServerHttpPort(aTestServer.getHttpDefaultPort());
            aTestServer.setServerHttpsPort(aTestServer.getHttpDefaultSecurePort());

            // disabled code as shibboleth is now local and we update the ports in both the SP and IDP with the real runtime ports
            // right now, our IDP's can only handle specific ports
            // (our partners have to be registered in an external server)
            // error out if we get any other ports - tests will fail for reasons that are
            // not always clear
            //            String currentHttpsPort = Integer.toString(aTestServer.getHttpDefaultSecurePort());
            //            if (!cttools.isInList(validPortNumbers, currentHttpsPort)) {
            //                helpers.logDebugInfo();
            //                throw new Exception("These tests use external IDP servers.  Those external servers have entries that include the https port used by the SP server on this test machine.  Therefore, these tests require the use of ports " + Arrays.toString(validPortNumbers) + " ONLY.  This execution is using port: " + currentHttpsPort);
            //            }

            testSettings.setDefaultTestSettings(testType, serverType, httpString.toString(), httpsString.toString());
            helpers.setServer(serverType, aTestServer);

        } catch (Exception e) {
            // if we fail setting up the server, we don't know how far it may have gotten
            // the server may be running - since we're not returning the TestServer object
            // to the caller the AfterClass method won't have the reference to use to stop the server
            // if we eat the failure, the tests will go on and fail and waste time instead of
            // erroring out...
            Log.error(thisClass, thisMethod, e);
            tearDownServer(aTestServer);
            throw e;
        }
        return aTestServer;

    }

    /**
     * Replaces all of the variables in the given config file with the default configuration settings.
     *
     * @param server
     * @param configFileName
     *            Config file name within the configs/ directory of the server.
     * @param configOutputName
     *            File name to which the result will be written, relative to the server's configs/ directory.
     *            If null or empty, this will be set to the value of {@code configFileName}.
     * @return The path to the resulting configuration file.
     */
    public static String updateConfigFileWithDefaultSettings(SAMLTestServer server, String configFileName, String configOutputName) {
        String method = "updateConfigFileWithDefaultSettings";

        Log.info(thisClass, method, "Updating default config settings for file: " + configFileName);

        return updateConfigFile(server, configFileName, samlConfigSettings, configOutputName);
    }

    /**
     * Replaces all of the variables specified by the provided configuration settings within the provided config file of the given
     * server.
     *
     * @param server
     * @param configFileName
     *            Config file name within the configs/ directory of the server.
     * @param configSettings
     * @param configOutputName
     *            File name to which the result will be written, relative to the server's configs/ directory.
     *            If null or empty, this will be set to the value of {@code configFileName}.
     * @return The path to the resulting configuration file.
     */
    public static String updateConfigFile(SAMLTestServer server, String configFileName, BaseConfigSettings configSettings, String configOutputName) {
        Map<String, String> replaceVals = configSettings.getConfigSettingsVariablesMap();
        return updateConfigFile(server, configFileName, replaceVals, configOutputName);
    }

    /**
     * Replaces all of the variables specified by the {@code replaceVals} map within the provided config file of the given
     * server.
     *
     * @param server
     * @param configFileName
     *            Config file name within the configs/ directory of the server.
     * @param replaceVals
     *            Maps variable names to the values to be used to replace them within the file.
     * @param configOutputName
     *            File name to which the result will be written, relative to the server's configs/ directory.
     *            If null or empty, this will be set to the value of {@code configFileName}.
     * @return The path to the resulting configuration file.
     */
    public static String updateConfigFile(SAMLTestServer server, String configFileName, Map<String, String> replaceVals, String configOutputName) {
        String method = "updateConfigFile";

        String configFilePath = server.createRuntimeConfigPath(configFileName);
        Log.info(thisClass, method, "Updating config settings for file: " + configFilePath);

        String configOutputPath = null;
        if (configOutputName != null && !configOutputName.isEmpty()) {
            configOutputPath = server.createRuntimeConfigPath(configOutputName);
        }

        boolean replaceSuccessful = cioTools.replaceStringsInFile(configFilePath, replaceVals, configOutputPath);
        if (!replaceSuccessful) {
            Log.warning(thisClass, "Did not successfully replace all strings in the given file");
        }
        return configOutputPath;
    }

    /**
     * Generic steps to invoke methods (in the correct ordering) to test with the SAML server
     *
     * @param testcase
     * @param wc
     * @param settings
     * @param testActions
     * @param expectations
     * @return
     * @throws Exception
     */

    // HTMLUNIT flows
    public Object genericSAML(String testcase, WebClient webClient, SAMLTestSettings settings,
                              String[] testActions, List<validationData> expectations) throws Exception {

        return genericSAML(testcase, webClient, settings, testActions, expectations, null);

    }

    public Object genericSAML(String testcase, WebClient webClient, SAMLTestSettings settings,
                              String[] testActions, List<validationData> expectations, Object somePage) throws Exception {

        if (webClient == null) {
            webClient = getAndSaveWebClient();
        }

        // WebResponse response = null;
        String thisMethod = "genericSAML";
        msgUtils.printMethodName(thisMethod);

        settings.printTestSettings();
        msgUtils.printExpectations(expectations);

        try {

            for (String entry : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + entry);
            }

            // TODO add back
            //  response = helpers.getIDPEndpoint (testcase, wc, settings, expectations) ;

            if (cttools.isInList(testActions, SAMLConstants.SAML_META_DATA_ENDPOINT)) {
                somePage = helpers.invokeSAMLMetaDataEndpoint(testcase, webClient, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST)) {
                somePage = helpers.buildPostIDPInitiatedRequest(testcase, webClient, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST)) {
                somePage = helpers.buildPostSolicitedSPInitiatedRequest(testcase, webClient, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.HANDLE_IDPCLIENT_JSP)) {
                somePage = helpers.handleIdpClientJsp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_IDP_JSP)) {
                somePage = helpers.processIdpJsp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.SLEEP_BEFORE_LOGIN)) {
                helpers.testSleep(settings.getSleepBeforeTokenUse());
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_IDP_REQUEST)) {
                somePage = helpers.processIDPRequest(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_IDP_CONTINUE)) {
                somePage = helpers.processIDPContinue(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PERFORM_IDP_LOGIN)) {
                somePage = helpers.performIDPLogin(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGIN_REQUEST)) {
                somePage = helpers.processLoginRequest(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGIN_CONTINUE)) {
                somePage = helpers.processLoginContinue(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            Object pageWithSAML = somePage;
            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE)) {
                somePage = helpers.invokeACSWithSAMLResponse(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES)) {
                somePage = helpers.invokeACSWithSAMLResponse(testcase, webClient, somePage, settings, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES,
                                                             true);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_BYPASS_APP)) {
                somePage = helpers.invokeACSWithSAMLResponse(testcase, webClient, somePage, settings, expectations, null, false);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN)) {
                somePage = helpers.invokeACSWithSAMLResponse(testcase, webClient, pageWithSAML, settings, expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_AGAIN, true);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_DEFAULT_APP)) {
                helpers.invokeDefaultApp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_DEFAULT_APP_SAME_CONVERSATION)) {
                helpers.invokeDefaultAppSameConversation(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ALTERNATE_APP_SAME_CONVERSATION)) {
                helpers.invokeAlternateAppSameConversation(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_DEFAULT_APP_NEW_CONVERSATION)) {
                helpers.invokeDefaultApp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ALTERNATE_APP_NEW_CONVERSATION)) {
                helpers.invokeAlternateApp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_ALTERNATE_APP)) {
                helpers.invokeAlternateApp(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_FORM_LOGIN)) {
                helpers.performFormLogin(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PERFORM_SP_LOGOUT)) {
                somePage = helpers.performSPLogout(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PERFORM_IDP_LOGOUT)) {
                somePage = helpers.performIDPLogout(testcase, webClient, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGOUT_REQUEST)) {
                somePage = helpers.processLogoutRequest(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGOUT_CONTINUE)) {
                somePage = helpers.processLogoutContinue(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }
            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGOUT_CONTINUE2)) {
                somePage = helpers.processContinue2(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES)) {
                somePage = helpers.processLogoutPropagateYes(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            //            if (cttools.isInList(testActions, SAMLConstants.PROCESS_EVENT_PROCEED)) {
            //                somePage = helpers.processEventProceed(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            //            }

            if (cttools.isInList(testActions, SAMLConstants.PROCESS_LOGOUT_REDIRECT)) {
                somePage = helpers.processLogoutRedirect(testcase, webClient, (HtmlPage) somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_JAXRS_GET)) {
                helpers.runGetMethod(testcase, somePage, settings, expectations);
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_JAXRS_GET_OVERRIDEAPP)) {
                helpers.runGetMethod(testcase, somePage, settings, expectations, settings.getSpDefaultApp());
            }

            if (cttools.isInList(testActions, SAMLConstants.INVOKE_JAXRS_GET_VIASERVICECLIENT)) {
                helpers.invokeSvcClient(testcase, webClient, somePage, settings, expectations);
            }
            //            return genericSAMLLogout(testcase, webClient, settings, testActions, expectations, somePage);
            return somePage;

        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }
    }

    public Object genericSAMLLogout(String testcase, WebClient webClient, SAMLTestSettings settings,
                                    String[] testActions, List<validationData> expectations, Object somePage) throws Exception {

        if (webClient == null) {
            webClient = getAndSaveWebClient();
        }

        // WebResponse response = null;
        String thisMethod = "genericSAMLLogout";
        msgUtils.printMethodName(thisMethod);

        settings.printTestSettings();
        msgUtils.printExpectations(expectations);

        try {

            for (String action : testActions) {
                Log.info(thisClass, testcase, "Action to be performed: " + action);

                switch (action) {
                    case SAMLConstants.PERFORM_SP_LOGOUT:
                        somePage = helpers.performSPLogout(testcase, webClient, somePage, settings, expectations);
                        break;
                    case SAMLConstants.PERFORM_IDP_LOGOUT:
                        somePage = helpers.performIDPLogout(testcase, webClient, somePage, settings, expectations);
                        break;
                    case SAMLConstants.PROCESS_LOGOUT_REQUEST:
                        somePage = helpers.processLogoutRequest(testcase, webClient, (HtmlPage) somePage, settings, expectations);
                        break;
                    case SAMLConstants.PROCESS_LOGOUT_CONTINUE:
                        somePage = helpers.processLogoutContinue(testcase, webClient, (HtmlPage) somePage, settings, expectations);
                        break;
                    case SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES:
                        somePage = helpers.processLogoutPropagateYes(testcase, webClient, (HtmlPage) somePage, settings, expectations);
                        break;
                    case SAMLConstants.PROCESS_LOGOUT_REDIRECT:
                        somePage = helpers.processLogoutRedirect(testcase, webClient, (HtmlPage) somePage, settings, expectations);
                        break;
                    default:
                        Log.info(thisClass, thisMethod, "Skipping action: " + action + " - there is no case to handle it");
                }

            }
        } catch (Exception e) {

            Log.error(thisClass, testcase, e, "Exception occurred");
            System.err.println("Exception: " + e);
            throw e;
        }

        return somePage;
    }

    protected static String[] standardFlow;
    protected static String[] justGetSAMLToken;
    protected static String[] reuseSAMLToken;
    protected static String[] standardFlowDefAppAgain;
    protected static String[] standardFlowDefAppAgainKeepingCookies;
    protected static String[] standardFlowAltAppAgain;
    protected static String[] standardFlowKeepingCookies;
    protected static String[] standardFlowUsingIDPCookieKeepingCookies;
    protected static String[] formLoginFlow;
    protected static String[] problemWithMetaDataTerminator;
    protected static String[] standardFlowExtendedKeepingCookies;
    //    protected static String[] loginLogoutFlow;
    //    protected static String[] justLogout;
    protected static String[] noLoginKeepingCookies;
    protected static String[] throughJAXRSGet;

    public static void setActionsForFlowType(String flowType) throws Exception {

        String thisMethod = "setActionsForFlowType";
        if (flowType == null) {
            Log.info(thisClass, thisMethod, "Flow wasn't set");
            return;
        }
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            spCfgNameExtension = "";
            Log.info(thisClass, thisMethod, "Settings values for IDP Initiated flow");
            standardFlow = SAMLConstants.IDP_INITIATED_FLOW;
            reuseSAMLToken = SAMLConstants.IDP_INITIATED_FLOW_AGAIN;
            justGetSAMLToken = SAMLConstants.IDP_INITIATED_GET_SAML_TOKEN;
            standardFlowDefAppAgain = SAMLConstants.IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN;
            standardFlowDefAppAgainKeepingCookies = SAMLConstants.IDP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES;
            standardFlowAltAppAgain = SAMLConstants.IDP_INITIATED_EXTENDED_FLOW;
            standardFlowKeepingCookies = SAMLConstants.IDP_INITIATED_FLOW_KEEPING_COOKIES;
            standardFlowUsingIDPCookieKeepingCookies = SAMLConstants.IDP_INITIATED_FLOW_KEEPING_COOKIES;
            formLoginFlow = SAMLConstants.IDP_INITIATED_NO_MATCH_FORMLOGIN;
            problemWithMetaDataTerminator = SAMLConstants.IDP_INITIATED_FLOW;
            standardFlowExtendedKeepingCookies = SAMLConstants.IDP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES;
            //            loginLogoutFlow = SAMLConstants.IDP_INITIATED_FLOW_LOGOUT;
            //            justLogout = SAMLConstants.IDP_INITIATED_LOGOUT;
            noLoginKeepingCookies = SAMLConstants.IDP_INITIATED_ACS_KEEPING_COOKIES;
            throughJAXRSGet = SAMLConstants.IDP_INITIATED_THROUGH_JAXRS_GET;
        }
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            spCfgNameExtension = "";
            Log.info(thisClass, thisMethod, "Settings values for Solicited SP Initiated flow");
            standardFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW;
            reuseSAMLToken = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_AGAIN;
            justGetSAMLToken = SAMLConstants.SOLICITED_SP_INITIATED_GET_SAML_RESPONSE;
            standardFlowDefAppAgain = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN;
            standardFlowDefAppAgainKeepingCookies = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES;
            standardFlowAltAppAgain = SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW;
            standardFlowKeepingCookies = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES;
            standardFlowUsingIDPCookieKeepingCookies = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_USING_IDP_KEEPING_COOKIES;
            formLoginFlow = SAMLConstants.SOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN;
            problemWithMetaDataTerminator = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            standardFlowExtendedKeepingCookies = SAMLConstants.SOLICITED_SP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES;
            //            loginLogoutFlow = SAMLConstants.SOLICITED_SP_INITIATED_FLOW_LOGOUT;
            //            justLogout = SAMLConstants.SP_INITIATED_LOGOUT;
            noLoginKeepingCookies = SAMLConstants.SOLICITED_SP_INITIATED_ACS_KEEPING_COOKIES;
            throughJAXRSGet = SAMLConstants.SOLICITED_SP_INITIATED_THROUGH_JAXRS_GET;
        }
        // The outward flow of the Unsolicited SP request is the same as the solicited
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            spCfgNameExtension = "_unsolicited";
            Log.info(thisClass, thisMethod, "Settings values for Unsolicited SP Initiated flow");
            standardFlow = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW;
            reuseSAMLToken = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_AGAIN;
            justGetSAMLToken = SAMLConstants.UNSOLICITED_SP_INITIATED_GET_SAML_TOKEN;
            standardFlowDefAppAgain = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN;
            standardFlowDefAppAgainKeepingCookies = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_INVOKE_DEF_APP_AGAIN_KEEPING_COOKIES;
            standardFlowAltAppAgain = SAMLConstants.UNSOLICITED_SP_INITIATED_EXTENDED_FLOW;
            standardFlowKeepingCookies = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_KEEPING_COOKIES;
            standardFlowUsingIDPCookieKeepingCookies = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_USING_IDP_KEEPING_COOKIES;
            formLoginFlow = SAMLConstants.UNSOLICITED_SP_INITIATED_NO_MATCH_FORMLOGIN;
            problemWithMetaDataTerminator = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_ONLY_SP;
            standardFlowExtendedKeepingCookies = SAMLConstants.UNSOLICITED_SP_INITIATED_EXTENDED_FLOW_KEEPING_COOKIES;
            //            loginLogoutFlow = SAMLConstants.UNSOLICITED_SP_INITIATED_FLOW_LOGOUT;
            //            justLogout = SAMLConstants.UNSOLICITED_SP_INITIATED_LOGOUT;
            noLoginKeepingCookies = SAMLConstants.UNSOLICITED_SP_INITIATED_ACS_KEEPING_COOKIES;
            throughJAXRSGet = SAMLConstants.UNSOLICITED_SP_INITIATED_THROUGH_JAXRS_GET;
        }
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

        Log.info(thisClass, "setTestName", "logging to primary Server");
        logTestCaseInServerSideLog("STARTING", testSAMLServer);
        // we have cases where the saml server and the app server are really the same server instance, so,
        //  make sure we have a unique server instance before logging the start msg
        if (testSAMLServer2 != testSAMLServer) {
            Log.info(thisClass, "setTestName", "logging to secondary Server");
            logTestCaseInServerSideLog("STARTING", testSAMLServer2);
        }
        if (testAppServer != testSAMLServer && testAppServer != testSAMLServer2) {
            Log.info(thisClass, "setTestName", "logging to Application Server");
            logTestCaseInServerSideLog("STARTING", testAppServer);
        }
        logTestCaseInServerSideLog("STARTING", testIDPServer);

    }

    /**
     * Clean up after running a test
     *
     * @throws Exception
     */
    @After
    public void endTest() throws Exception {

        try {

            // clean up webClients
            webClientTracker.closeAllWebClients();

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        try {

            endServer(testAppServer);
            endServer(testSAMLServer2);
            endServer(testSAMLServer);
            endServer(testIDPServer);

            if (testAppServer != testSAMLServer && testAppServer != testSAMLServer2) {
                logTestCaseInServerSideLog("ENDING", testAppServer);
            }
            if (testSAMLServer2 != testSAMLServer) {
                logTestCaseInServerSideLog("ENDING", testSAMLServer2);
            }
            logTestCaseInServerSideLog("ENDING", testSAMLServer);
            logTestCaseInServerSideLog("ENDING", testIDPServer);

            msgUtils.printMethodName(_testName, "Ending TEST ");
            System.out.println("----- End:  " + _testName + "   ----------------------------------------------------");

            //            helpers.turnHttpUnitRedirectOn();

        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

    }

    /**
     * Generic routine to end a server (just make sure the server exists before trying to end it)
     *
     * @param theServer
     * @throws Exception
     */
    private static void endServer(SAMLTestServer theServer) throws Exception {

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

        List<SAMLTestServer> newServerRefList = new ArrayList<SAMLTestServer>();
        List<CommonLocalLDAPServerSuite> newLdapRefList = new ArrayList<CommonLocalLDAPServerSuite>();

        Exception cumulativeException = null;
        int exceptionNum = 0;

        // had to move the timeoutChecker up as the passwordChecker (called durint teardown) will generate timeout msgs
        try {
            for (SAMLTestServer server : serverRefList) {
                addToAllowableTimeoutCount(server.getRetryTimeoutCount());
            }
            timeoutChecker();
        } catch (Exception e) {
            // Typically caused by an unexpected error message in the logs; should re-throw the exception
            e.printStackTrace(System.out);
            String prevExceptionMsg = (cumulativeException == null) ? "" : cumulativeException.getMessage();
            cumulativeException = new Exception(prevExceptionMsg + " [Exception #" + (++exceptionNum) + "]: " + e.getMessage() + "\n<br>");
        }

        for (SAMLTestServer server : serverRefList) {
            try {
                newServerRefList.add(server);
                tearDownServer(server);
            } catch (Exception e) {
                // Typically caused by an unexpected error message in the logs; should re-throw the exception
                e.printStackTrace(System.out);
                String prevExceptionMsg = (cumulativeException == null) ? "" : cumulativeException.getMessage();
                cumulativeException = new Exception(prevExceptionMsg + " [Exception #" + (++exceptionNum) + "]: " + e.getMessage() + "\n<br>");
            }
        }
        for (SAMLTestServer server : newServerRefList) {
            serverRefList.remove(server);
        }

        try {
            uninstallUserFeature();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        for (CommonLocalLDAPServerSuite ldapServer : ldapRefList) {
            newLdapRefList.add(ldapServer);
            ldapServer.ldapTearDown();
        }
        for (CommonLocalLDAPServerSuite ldapServer : newLdapRefList) {
            ldapRefList.remove(ldapServer);
        }

        initCommonVars();

        if (cumulativeException != null) {
            throw cumulativeException;
        }

    }

    /**
     * generic tear down of a server (makes sure the server exists first)
     *
     * @param theServer
     * @throws Exception
     */
    private static void tearDownServer(SAMLTestServer theServer) throws Exception {
        if (theServer != null) {
            // let the error get thrown and be caught by the caller
            passwordChecker(theServer);
            theServer.tearDownServer();
        }
    }

    protected void logTestCaseInServerSideLog(String action, SAMLTestServer server) throws Exception {
        String method = "logTestCaseInServerSideLog";
        try {
            if (server != null) {
                Log.info(thisClass, method, "server not null");
                WebConversation wc = new WebConversation();
                String markerUrl = server.getServerHttpString() + "/" + AppConstants.TESTMARKER_CONTEXT_ROOT + "/" + AppConstants.TESTMARKER_PATH;
                Log.info(thisClass, method, "marker url is: " + markerUrl);
                WebRequest request = new GetMethodWebRequest(markerUrl);
                request.setParameter("action", action);
                request.setParameter("testCaseName", _testName);
                wc.getResponse(request);
            } else {
                Log.info(thisClass, method, "server is null");
            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            Log.error(thisClass, method, e);
            e.printStackTrace();
        }

    }

    public void printTestTrace(String strMethod, String marker) throws Exception {
        System.err.flush();
        System.out.flush();
        Log.info(thisClass, strMethod, "     ************************ " + marker + " ***********************");

        logTestCaseInServerSideLog(marker, testSAMLServer);
        logTestCaseInServerSideLog(marker, testSAMLServer2);
        logTestCaseInServerSideLog(marker, testSAMLOIDCServer);
        logTestCaseInServerSideLog(marker, testOIDCServer);
        logTestCaseInServerSideLog(marker, testAppServer);
        logTestCaseInServerSideLog(marker, testIDPServer);

    }

    public String buildSPServerName(String origServerName) throws Exception {

        return origServerName.replace(".xml", spCfgNameExtension + ".xml");
    }

    // moved to common test tools
    //	/**
    //	 * Choose a random entry from the provided String array.
    //	 *
    //	 * @param entryArray
    //	 * @return
    //	 */
    //	public static String chooseRandomEntry(String[] entryArray) {
    //		String thisMethod = "chooseRandomEntry";
    //		Log.info(thisClass, thisMethod, "Determining which entry to select");
    //		Random rand = new Random();
    //		Integer num = rand.nextInt(1000);
    //		int div = num % entryArray.length;
    //		Log.info(thisClass, thisMethod, "Choosing entry from index: " + div);
    //
    //		String entry = entryArray[div];
    //		Log.info(thisClass, thisMethod, "Entry chosen: " + entry);
    //		return entry;
    //	}

    // randomly choose a flow type
    public static String chooseRandomFlow() {
        //		return chooseRandomEntry(new String[] { SAMLConstants.IDP_INITIATED, SAMLConstants.SOLICITED_SP_INITIATED, SAMLConstants.UNSOLICITED_SP_INITIATED });
        return cttools.chooseRandomEntry(new String[] { SAMLConstants.SOLICITED_SP_INITIATED });
    }

    public static void chooseServletLevel(SAMLTestServer theServer) throws Exception {

        String thisMethod = "chooseServletLevel";
        String[] servletRelatedFileNames = { "saml_only_features", "saml_rs_features", "saml_both_features" };
        Log.info(thisClass, thisMethod, "Choosing which feature group to use");
        if (chosenVersion == null) {
            chosenVersion = cttools.chooseRandomEntry(SAMLConstants.SERVLET_31, SAMLConstants.SERVLET_40);
        }
        Log.info(thisClass, thisMethod, "Feature group chosen: " + chosenVersion);
        String importsDir = theServer.getServer().getServerRoot() + File.separator + "imports";
        Machine machine = theServer.getServer().getMachine();
        for (String fileBase : servletRelatedFileNames) {
            String builtFilePath = importsDir + File.separator + fileBase + "_" + chosenVersion + ".xml";
            Log.info(thisClass, thisMethod, "builtFilePath:  " + builtFilePath);
            if (LibertyFileManager.libertyFileExists(machine, builtFilePath)) {
                LibertyFileManager.copyFileIntoLiberty(machine, importsDir, fileBase + ".xml", builtFilePath);
            }
        }

    }

    public static void startShibbolethApp(SAMLTestServer idpServer) throws Exception {

        shibbolethHelpers.startShibbolethApp(idpServer);

        RemoteFile idpProcessLogFile = null;

        // set flag indicating if encryption 192 and 256 tests can run
        for (int i = 0; i < 20; i++) {
            idpProcessLogFile = idpServer.getServer().getMatchingLogFile(SAMLConstants.IDP_PROCESS_LOG);
            if (idpProcessLogFile == null) {
                Log.info(thisClass, "startShibbolethApp", "idp-process.log reference is null");
                helpers.testSleep(5);
            } else {
                Log.info(thisClass, "startShibbolethApp", "found idp-process.log reference");
                // give it just a bit more time to log the entries we're looking for
                helpers.testSleep(5);
                break;
            }
        }
        if (idpProcessLogFile == null) {
            throw new Exception("Failed to find the " + SAMLConstants.IDP_PROCESS_LOG
                                + " file under the IdP server. The server may not have started correctly or took too long to start.");
        }
        cipherMayExceed128 = (idpServer.getServer().waitForStringInLog("exceeds Cipher max key length 128", 10 * 1000, idpProcessLogFile) == null);
        if (cipherMayExceed128) {
            addToAllowableTimeoutCount(1);
        }
    }

    public static void startSPWithIDPServer(String spServer, String spServerCfg, List<String> spExtraMsgs, List<String> spExtraApps, Boolean spCopyDataFlag) throws Exception {
        startSPWithIDPServer(spServer, spServerCfg, SAMLConstants.SAML_SERVER_TYPE, spExtraMsgs, spExtraApps, spCopyDataFlag, null, null, null);

    }

    public static void startSPWithIDPServer(String spServer, String spServerCfg, String serverType, List<String> spExtraMsgs, List<String> spExtraApps,
                                            Boolean spCopyDataFlag) throws Exception {
        startSPWithIDPServer(spServer, spServerCfg, serverType, spExtraMsgs, spExtraApps, spCopyDataFlag, null, null, null);
    }

    public static void startSPWithIDPServer(String spServer, String spServerCfg, String serverType, List<String> spExtraMsgs, List<String> spExtraApps, Boolean spCopyDataFlag,
                                            String callbackHandler, String feature) throws Exception {
        startSPWithIDPServer(spServer, spServerCfg, serverType, spExtraMsgs, spExtraApps, spCopyDataFlag, callbackHandler, feature, null);
    }

    public static void startSPWithIDPServer(String spServer, String spServerCfg, String serverType, List<String> spExtraMsgs, List<String> spExtraApps, Boolean spCopyDataFlag,
                                            String callbackHandler, String feature, ShibbolethHelpers.ShibbolethServerVars[] shibbolethVars) throws Exception {

        copyMetaData = false;
        testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.IDP_SERVER_TYPE, null, null,
                                    SAMLConstants.SKIP_CHECK_FOR_SECURITY_STARTED);
        copyMetaData = spCopyDataFlag;
        testSAMLServer = commonSetUp(spServer, spServerCfg, SAMLConstants.SAML_ONLY_SETUP, serverType, spExtraApps, spExtraMsgs, callbackHandler, feature);

        // now, we need to update the IDP files
        shibbolethHelpers.fixSPInfoInShibbolethServer(testSAMLServer, testIDPServer);
        if (shibbolethVars != null) {
            shibbolethHelpers.fixMiscVarsInShibbolethServer(testIDPServer, shibbolethVars);
        }
        shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
        // now, start the shibboleth app with the updated config info
        startShibbolethApp(testIDPServer);

        setActionsForFlowType(flowType);
    }

    public static void start2SPWithIDPServer(String spServer, String spServerCfg, String spServer2, String spServer2Cfg, String serverType, List<String> spExtraMsgs,
                                             List<String> spExtraApps, Boolean spCopyDataFlag, String callbackHandler, String feature) throws Exception {
        start2SPWithIDPServer(spServer, spServerCfg, spServer2, spServer2Cfg, serverType, spExtraMsgs, spExtraApps, spCopyDataFlag, callbackHandler, feature, null);
    }

    public static void start2SPWithIDPServer(String spServer, String spServerCfg, String spServer2, String spServer2Cfg, String serverType, List<String> spExtraMsgs,
                                             List<String> spExtraApps, Boolean spCopyDataFlag, String callbackHandler, String feature,
                                             ShibbolethHelpers.ShibbolethServerVars[] shibbolethVars) throws Exception {

        copyMetaData = false;
        testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", SAMLConstants.SAML_ONLY_SETUP, SAMLConstants.IDP_SERVER_TYPE, null, null,
                                    SAMLConstants.SKIP_CHECK_FOR_SECURITY_STARTED);
        copyMetaData = spCopyDataFlag;
        testSAMLServer = commonSetUp(spServer, spServerCfg, SAMLConstants.SAML_ONLY_SETUP, serverType, spExtraApps, spExtraMsgs, callbackHandler, feature);
        testSAMLServer2 = commonSetUp(spServer2, spServer2Cfg, SAMLConstants.SAML_ONLY_SETUP, serverType, spExtraApps, spExtraMsgs, callbackHandler, feature);

        fixServer2Ports(testSAMLServer2);

        // now, we need to update the IDP files
        shibbolethHelpers.fixSPInfoInShibbolethServer(testSAMLServer, testIDPServer);
        shibbolethHelpers.fixSecondSPInfoInShibbolethServer(testSAMLServer2, testIDPServer);
        if (shibbolethVars != null) {
            shibbolethHelpers.fixMiscVarsInShibbolethServer(testIDPServer, shibbolethVars);
        }
        shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
        // now, start the shibboleth app with the updated config info
        startShibbolethApp(testIDPServer);

        setActionsForFlowType(flowType);
    }

    public static void fixServer2Ports(SAMLTestServer testSAMLServer2) throws Exception {

        // we need to override the ports that are stored in the secondary server (first servers ports are set by default)
        testSAMLServer2.setServerHttpPort(testSAMLServer2.getServer().getHttpSecondaryPort());
        testSAMLServer2.setServerHttpsPort(testSAMLServer2.getServer().getHttpSecondarySecurePort());

    }

    /**
     * Add the newly added server to the list of server references
     *
     * @param server
     *            - server reference to add
     * @throws Exception
     */
    private static void addToServerRefList(SAMLTestServer server) throws Exception {

        serverRefList.add(server);

    }

    public static List<String> getDefaultSAMLStartMsgs() throws Exception {
        List<String> extraMsgs = new ArrayList<String>();
        getDefaultSAMLStartMsgs(extraMsgs);
        return extraMsgs;
    }

    public static List<String> getDefaultSAMLStartMsgs(List<String> extraMsgs) throws Exception {

        extraMsgs.add(SAMLMessageConstants.CWWKS5000I_SAML_CONFIG_PROCESSED);
        extraMsgs.add(SAMLMessageConstants.CWWKS5002I_SAML_SERVICE_ACTIVATED);

        return extraMsgs;
    }

    /**
     * JakartaEE9 transform a list of applications.
     *
     * @param myServer The server to transform the applications on.
     * @param apps The names of the applications to transform. Should include the path from the server root directory.
     */
    private static void transformApps(LibertyServer myServer, String... apps) {
        if (JakartaEE9Action.isActive()) {
            for (String app : apps) {
                Path someArchive = Paths.get(myServer.getServerRoot() + File.separatorChar + app);
                JakartaEE9Action.transformApp(someArchive);
            }
        }
    }

    public WebClient getAndSaveWebClient() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        webClientTracker.addWebClient(webClient);
        return webClient;
    }

    public WebClient getAndSaveWebClient(boolean override) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient(override);
        webClientTracker.addWebClient(webClient);
        return webClient;
    }
}
