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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.apps.AppConstants;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
public class TestServer extends com.ibm.ws.security.fat.common.TestServer {

    private final Class<?> thisClass = TestServer.class;

    public static final String SERVER_TYPE_OP = "OP";
    public static final String SERVER_TYPE_RP = "RP";
    public static final String SERVER_TYPE_JWT_BUILDER = Constants.JWT_BUILDER;
    public static final String SERVER_TYPE_JWT_CONSUMER = Constants.JWT_CONSUMER;
    public static final String SERVER_TYPE_OTHER = "OTHER";
    public static final String SYS_PROP_RS_VALIDATION_TYPE = "rsValidationType";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME = "fat.server.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_CANONICAL_HOSTNAME = "fat.server.canonical.hostname";
    public static final String BOOTSTRAP_PROP_FAT_SERVER_HOSTIP = "fat.server.hostip";

    protected static CommonTestHelpers helpers = new CommonTestHelpers();;

    protected String thisServerType = SERVER_TYPE_OP;
    protected String rsValidationType = null;
    protected String providerApp = null;
    protected String providerRoot = null;
    protected boolean testUsesSsl = true;
    protected boolean isUsingDerby = false;
    protected boolean isUsingMongoDB = false;

    public TestServer() {
        String[] serverDirNames = getServerDirectoryNames();
        Log.info(thisClass, "TestServer-init", "server dirs:" + Arrays.toString(serverDirNames));
        server = chooseServerFromServerNameList(serverDirNames);
    }

    public TestServer(String requestedServer, String serverXML, String testType) {
        server = getLibertServerFromServerName(requestedServer);
        serverName = requestedServer;
        initializeServer(serverXML, testType);
    }

    void initializeServer(String serverXML, String testType) {
        setOriginalServerXmlName(serverXML);
        setServerTypeBasedOnTestType(testType);
        addHostNameAndAddrToBootstrap();
    }

    void setServerTypeBasedOnTestType(String testType) {
        if (Constants.OAUTH_OP.equals(testType) || Constants.OIDC_OP.equals(testType)) {
            setServerType(SERVER_TYPE_OP);
        } else if (Constants.OIDC_RP.equals(testType)) {
            setServerType(SERVER_TYPE_RP);
        } else if (Constants.JWT_BUILDER.equals(testType)) {
            setServerType(Constants.JWT_BUILDER);
        } else if (Constants.JWT_CONSUMER.equals(testType)) {
            setServerType(Constants.JWT_CONSUMER);
        } else if (Constants.IDP_SERVER_TYPE.equals(testType)) {
            setServerType(Constants.IDP_SERVER_TYPE);
        } else {
            setServerType(SERVER_TYPE_OTHER);
        }
        Log.info(thisClass, "setServerTypeBasedOnTestType", "ServerType: " + getServerType());
    }

    /** TODO *************************************** TestServer-isolated methods *****************************************/

    protected void setServerBootstrapUtils(ServerBootstrapUtils utils) {
        bootstrapUtils = utils;
    }

    @Override
    public void setServerType(String type) {
        thisServerType = type;
    }

    @Override
    public String getServerType() {
        return thisServerType;
    }

    public String getRSValidationType() {
        return rsValidationType;
    }

    public String getProviderApp() {
        return providerApp;
    }

    public void setTestDoesNotUseSsl() {
        testUsesSsl = false;
    }

    public void resetTestUsesSsl() {
        testUsesSsl = true;
    }

    /** TODO *************************************** Log-related methods *****************************************/

    /**
     * Searches for a message string in the specified server log
     *
     * @param expected
     *            - a validationMsg type to search (contains the log to search and the string to search for)
     * @throws exception
     */
    public void waitForValueInServerLog(validationData expected) throws Exception {
        // TODO - same as superclass, but validationData class type is different
        String thisMethod = "waitForValueInServerLog";
        if (expected == null) {
            throw new Exception("Cannot search for expected value in server log: The provided expectation is null!");
        }
        try {
            Log.info(thisClass, thisMethod, "checkType is: " + expected.getCheckType());

            String logName = getGenericLogName(expected.getWhere());
            String expectedValue = expected.getValidationValue();
            Log.info(thisClass, thisMethod, "Searching for [" + expectedValue + "] in " + logName);

            String searchResult = server.waitForStringInLogUsingMark(expectedValue, server.getMatchingLogFile(logName));
            msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg() + " Was expecting to find [" + expectedValue + "] in " + logName + ", but did not find it there!",
                                      searchResult != null);
            Log.info(thisClass, thisMethod, "Found message: " + expectedValue);

        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, thisMethod, e, "Failure searching for string [" + expected.getValidationValue() + "] in " + expected.getWhere());
            throw e;
        }
    }

    /** TODO *************************************** Server operations *****************************************/

    /**
     * Reconfigures the running server, copies a new server config and then waits for all expected messages (indicating that the
     * server configuration was updated).
     *
     * @param reportViaJunit
     *            - boolean indicating if failures should be reported via junit, or if we should just log a message
     * @param startMessages
     *            - any additional messages that we should wait for in the logs
     * @throws exception
     */
    public void reconfigServer(String newServerConfigFile, String testName, boolean reportViaJunit, List<String> startMessages) throws Exception {
        String thisMethod = "reconfigServer";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "************** Starting server.xml update for: " + testName);
        try {
            reconfigServer(newServerConfigFile, testName, startMessages, Constants.DO_NO_RESTART_SERVER, reportViaJunit);

            if (isUsingDerby) {
                setUpDerbyEntries();
            }

            Log.info(thisClass, thisMethod, "************** Completed server.xml update: " + testName);
            System.err.flush();

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw ex;
        }
    }

    /**
     * Starts the specified server and waits for all required msgs to appear in the log
     *
     * @param checkApps
     *            - list of applications that should should be waited on during a server start
     * @param startMessages
     *            - any additional messages that we should wait for in the logs
     * @param reportViaJunit
     *            - boolean indicating if failures should be reported via junit, or if we should just log a message
     */
    public void startServer(String serverXML, List<String> checkApps, List<String> startMessages, String testName, boolean reportViaJunit) throws Exception {
        startServer(serverXML, testName, checkApps, startMessages, reportViaJunit, null);
    }

    @Override
    protected List<String> addMiscStartMessages(List<String> startMessages, List<String> checkApps, String testName) {
        startMessages = super.addMiscStartMessages(startMessages, checkApps, testName);

        if (!isInvalidSSLConfigTest(testName) && testUsesSsl) {
            startMessages.add("CWWKO0219I.*ssl");
        }
        return startMessages;
    }

    /**
     * Checks if the test case that is being executed is one with an invalid SSL configuration.
     **/
    boolean isInvalidSSLConfigTest(String testName) {
        if (testName != null &&
            ((testName.equals("skip")) ||
             (testName.equals("OidcClientTestLDAPRegistryHttpsRequiredNoSSLConfig")) ||
             (testName.equals("OidcClientTestLDAPRegistryHttpsRequiredInvalidSSLConfig")))) {
            return true;
        }
        return false;
    }

    /** TODO *************************************** Bootstrap utils *****************************************/

    void addHostNameAndAddrToBootstrap() {
        String thisMethod = "addHostNameAndAddrToBootstrap";
        try {
            setServerNameAndHostIp();
            bootstrapUtils.writeBootstrapProperty(this, BOOTSTRAP_PROP_FAT_SERVER_CANONICAL_HOSTNAME, serverCanonicalHostName);
            bootstrapUtils.writeBootstrapProperty(this, BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, serverHostName);
            bootstrapUtils.writeBootstrapProperty(this, BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, serverHostIp);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, thisMethod, "Setup failed to add host info to bootstrap.properties");
        }
    }

    public void addMiscBootstrapParms(Map<String, String> miscParms) throws Exception {
        bootstrapUtils.writeSecureBootstrapProperties(this, miscParms);
    }

    /** TODO *************************************** SAML *****************************************/

    public void copyDefaultSPConfigFiles(String chosenSAMLServer) throws Exception {
        Log.info(thisClass, "copyDefaultSPConfigFiles", "Copying serversettings/SAMLServerFiles/" + chosenSAMLServer + "/idpMetadata.xml to resources/security/");
        Log.info(thisClass, "copyDefaultSPConfigFiles", "Server loc: " + server.getServerConfigurationPath());
        server.copyFileToLibertyServerRoot("resources/security/", "serversettings/SAMLServerFiles/" + chosenSAMLServer + "/idpMetadata.xml");
    }

    /** TODO *************************************** Derby *****************************************/

    public void setUsingDerby(boolean trueFalse) {
        isUsingDerby = trueFalse;
    }

    public void setUsingMongoDB(boolean trueFalse) {
        isUsingMongoDB = trueFalse;
    }

    public boolean isUsingMongoDB() {
        return isUsingMongoDB;
    }

    public boolean isUsingDerby() {
        return isUsingDerby;
    }

    public void setUpDerbyEntries() throws Exception {
        DerbyUtils.setupDerbyEntries(getHttpString(), getHttpDefaultPort());
    }

    /** TODO *************************************** Methods with mixed relation *****************************************/

    @Override
    public Integer getHttpDefaultPort() {

        try {
            Log.info(thisClass, "getHttpDefaultPort", "ServerName: " + server.getServerName());

            ServerConfiguration serverConfig = server.getServerConfiguration();
            HttpEndpoint serverEndpoints = serverConfig.getHttpEndpoints().getById("defaultHttpEndpoint");
            String port = serverEndpoints.getHttpPort().replace("${bvt.prop.", "").replace("}", "");
            Integer portNum = Integer.getInteger(port);
            return portNum;

        } catch (Exception e) {
            Log.error(thisClass, "failed getting port - will use the default", e);
        }
        return server.getHttpDefaultPort();
    }

    @Override
    public Integer getHttpDefaultSecurePort() {

        try {

            Log.info(thisClass, "getHttpDefaultPort", "ServerName: " + server.getServerName());

            ServerConfiguration serverConfig = server.getServerConfiguration();
            HttpEndpoint serverEndpoints = serverConfig.getHttpEndpoints().getById("defaultHttpEndpoint");
            String port = serverEndpoints.getHttpsPort().replace("${bvt.prop.", "").replace("}", "");
            Integer portNum = Integer.getInteger(port);
            return portNum;

        } catch (Exception e) {
            Log.error(thisClass, "failed getting port - will use the default", e);
        }
        return server.getHttpDefaultSecurePort();
    }

    public List<String> getDefaultStartMessages(String testType) {
        List<String> startMsgs = new ArrayList<String>();
        if (Constants.OAUTH_OP.equals(testType)) {
            startMsgs.add("OAuth roles configuration successfully processed");
        }
        return startMsgs;
    }

    public List<String> getDefaultTestApps(String testType) {
        List<String> testApps = new ArrayList<String>();
        testApps.add(AppConstants.TESTMARKER_CONTEXT_ROOT);
        return testApps;
    }

    /**
     * Adds the application name and/or application start message to the provided lists for the specified application. The name of
     * the application might vary depending on the server type. If only the app list or message list needs to be updated, a null
     * value can be provided for the other list argument.
     *
     * @param appList
     *            Existing list of application names expected to be checked for.
     * @param msgList
     *            Existing list of messages expected to be checked for.
     */
    public static void addTestApp(List<String> appList, List<String> msgList, String appToAdd, String testType) {

        if (appToAdd.equals(Constants.OP_CLIENT_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCLIENT_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCLIENT_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCLIENT_START_APP);
                    addToMyList(msgList, Constants.OIDC_APP_START_MSG + Constants.OIDCCLIENT_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_TAI_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGTAI_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGTAI_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGTAI_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGTAI_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_DERBY_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGDERBY_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGDERBY_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGDERBY_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGDERBY_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_NOFILTER_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGNOFILTER_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGNOFILTER_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGNOFILTER_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGNOFILTER_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_SAMPLE_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGSAMPLE_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGSAMPLE_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGSAMPLE_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGSAMPLE_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_MEDIATOR_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGMEDIATOR_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGMEDIATOR_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGMEDIATOR_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGMEDIATOR_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_PUBLIC_APP)) {
            if (testType.equals(Constants.OAUTH_OP)) {
                addToMyList(appList, Constants.OAUTHCONFIGPUBLIC_START_APP);
                addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGPUBLIC_APP);
            } else {
                if (testType.equals(Constants.OIDC_OP)) {
                    addToMyList(appList, Constants.OIDCCONFIGPUBLIC_START_APP);
                    addToMyList(msgList, Constants.OAUTH_APP_START_MSG + Constants.OAUTHCONFIGPUBLIC_APP);
                }
            }
        }
        if (appToAdd.equals(Constants.OP_TESTMARKER_APP)) {
            addToMyList(appList, Constants.TESTMARKER_START_APP);
        }

    }

    public void addValidationEndpointProps(String opServerType, String tokenType, String certType, Map<String, String> jwkValidationMap, String callingClassValidationEndpointOverrideValue) throws Exception {
        String thisMethod = "addValidationEndpointProps";
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, thisMethod, "tokenType: " + tokenType);

        setRsValidationType(opServerType, tokenType, callingClassValidationEndpointOverrideValue);

        Map<String, String> bootPropsMap = getValidationEndpointBootstrapProps(certType);
        bootstrapUtils.writeBootstrapProperties(this, bootPropsMap);

        writeJwkValidationBootstrapProperties(certType, jwkValidationMap);
    }

    void setRsValidationType(String opServerType, String tokenType, String callingClassValidationEndpointOverrideValue) throws Exception {
        String thisMethod = "setRsValidationType";

        rsValidationType = getRsValidationTypeRequiredForSettings(tokenType, opServerType);
        if (rsValidationType != null) {
            return;
        }
        // some of the test classes have been updated to run twice (once with userinfo and once with introspect), they'll
        // pass in the type that they want - for now, we'll override the env var with what the class specifies it needs
        rsValidationType = Utils.getEnvVar(SYS_PROP_RS_VALIDATION_TYPE);
        if (callingClassValidationEndpointOverrideValue != null) {
            rsValidationType = callingClassValidationEndpointOverrideValue;
        }
        if (rsValidationType == null || rsValidationType.equals("")) {
            rsValidationType = Utils.getRandomSelection(Constants.INTROSPECTION_ENDPOINT, Constants.USERINFO_ENDPOINT);
        } else {
            Log.info(thisClass, thisMethod, "Using caller override value of: " + rsValidationType);
        }
    }

    /**
     * Returns the RS validation type that must be used if the provided token type or OP server type are set to specific values.
     * For example, JWT token types must use local validation and pure OAuth OPs must use introspection.
     */
    String getRsValidationTypeRequiredForSettings(String tokenType, String opServerType) {
        if (tokenType != null && (tokenType.contains(Constants.JWT_TOKEN) || tokenType.contains(Constants.MP_JWT_TOKEN))) {
            return Constants.LOCAL_VALIDATION_METHOD;
        }
        if (Constants.OAUTH_OP.equals(opServerType)) {
            // when the OP is running with OAUTH only, we can't use the userinfo endpoint, have to use introspect
            return Constants.INTROSPECTION_ENDPOINT;
        }
        return null;
    }

    Map<String, String> getValidationEndpointBootstrapProps(String certType) {
        Map<String, String> bootPropsMap = new HashMap<String, String>();
        // use 2 different variables just in case the endpoint or type names change sometime
        bootPropsMap.put(Constants.BOOT_PROP_RS_VALIDATION_ENDPOINT, rsValidationType);
        bootPropsMap.put(Constants.BOOT_PROP_RS_VALIDATION_TYPE, rsValidationType);
        bootPropsMap.put(Constants.BOOT_PROP_OIDC_SIG_ALG, Constants.SIGALG_HS256);
        if (Constants.USERINFO_ENDPOINT.contains(rsValidationType)) {
            bootPropsMap.put(Constants.BOOT_PROP_USERAPI_TYPE, "basic");
        } else {
            bootPropsMap.put(Constants.BOOT_PROP_USERAPI_TYPE, Constants.INTROSPECTION_ENDPOINT);
        }
        bootPropsMap.put(Constants.BOOT_PROP_OIDC_JWK_VALIDATION_URL, "");
        bootPropsMap.put(Constants.BOOT_PROP_OIDC_JWK_VALIDATION_URL_2, "");
        if (Constants.JWK_CERT.equals(certType)) {
            bootPropsMap.put(Constants.BOOT_PROP_OIDC_SIG_ALG, Constants.SIGALG_RS256);
            bootPropsMap.put(Constants.BOOT_PROP_OIDC_JWK_VALIDATION_URL, "\"https://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTPS_DEFAULT + "}/${" + Constants.BOOT_PROP_PROVIDER_ROOT + "}/endpoint/${" + Constants.BOOT_PROP_PROVIDER_SAMPLE + "}/jwk\"");
            bootPropsMap.put(Constants.BOOT_PROP_OIDC_JWK_VALIDATION_URL_2, "\"https://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTPS_DEFAULT + "}/${" + Constants.BOOT_PROP_PROVIDER_ROOT + "}/endpoint/${" + Constants.BOOT_PROP_PROVIDER_SAMPLE + "}2/jwk\"");
        }
        return bootPropsMap;
    }

    void writeJwkValidationBootstrapProperties(String certType, Map<String, String> jwkValidationMap) throws Exception {
        if (jwkValidationMap == null) {
            return;
        }
        for (Map.Entry<String, String> entry : jwkValidationMap.entrySet()) {
            String propValue = "\"\"";
            if (Constants.JWK_CERT.equals(certType)) {
                propValue = "\"https://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTPS_DEFAULT + "}/${" + Constants.BOOT_PROP_PROVIDER_ROOT + "}/endpoint/" + entry.getValue() + "/jwk\"";
            }
            bootstrapUtils.writeBootstrapProperty(this, entry.getKey(), propValue);
        }
    }

    public void addOPJaxRSProps(String opServerType, String tokenType, String certType) throws Exception {
        String thisMethod = "addOPJaxRSProps";
        msgUtils.printMethodName(thisMethod);

        Log.info(thisClass, thisMethod, "opServerType: " + opServerType);
        Log.info(thisClass, thisMethod, "tokenType: " + tokenType);
        Log.info(thisClass, thisMethod, "certType: " + certType);

        if (Constants.ISAM_OP.equals(opServerType)) {
            // if using an external ISAM server, we don't need to set any properties for the OP
            return;
        }

        Map<String, String> bootProps = getOpJaxRsBootstrapProperties(tokenType, certType);
        bootstrapUtils.writeBootstrapProperties(this, bootProps);
    }

    Map<String, String> getOpJaxRsBootstrapProperties(String tokenType, String certType) {
        Map<String, String> bootProps = new HashMap<String, String>();
        bootProps.putAll(getOpJaxRsTokenFormatBootstrapProps(tokenType));
        bootProps.putAll(getOpJaxRsJwkAndSigAlgBootstrapProps(certType));
        return bootProps;
    }

    Map<String, String> getOpJaxRsTokenFormatBootstrapProps(String tokenType) {
        Map<String, String> bootProps = new HashMap<String, String>();
        if (Constants.JWT_TOKEN.equals(tokenType)) {
            bootProps.put(Constants.BOOT_PROP_OIDC_CREATE_JWT, "true");
            bootProps.put(Constants.BOOT_PROP_OIDC_TOKEN_FORMAT, Constants.JWT_TOKEN_FORMAT);
        } else {
            if (Constants.MP_JWT_TOKEN.equals(tokenType)) {
                bootProps.put(Constants.BOOT_PROP_OIDC_CREATE_JWT, "true");
                bootProps.put(Constants.BOOT_PROP_OIDC_TOKEN_FORMAT, Constants.MP_JWT_TOKEN_FORMAT);
            } else {
                bootProps.put(Constants.BOOT_PROP_OIDC_CREATE_JWT, "false");
                bootProps.put(Constants.BOOT_PROP_OIDC_TOKEN_FORMAT, Constants.OPAQUE_TOKEN_FORMAT);
            }
        }
        return bootProps;
    }

    Map<String, String> getOpJaxRsJwkAndSigAlgBootstrapProps(String certType) {
        Map<String, String> bootProps = new HashMap<String, String>();
        if (Constants.JWK_CERT.equals(certType)) {
            bootProps.put(Constants.BOOT_PROP_OIDC_JWK_ENABLED, "true");
            bootProps.put(Constants.BOOT_PROP_OIDC_SIG_ALG, Constants.SIGALG_RS256);
        } else {
            bootProps.put(Constants.BOOT_PROP_OIDC_JWK_ENABLED, "false");
            bootProps.put(Constants.BOOT_PROP_OIDC_SIG_ALG, Constants.SIGALG_HS256);
        }
        return bootProps;
    }

    public void addProviderProps(String opServerType) throws Exception {
        String thisMethod = "addProviderProps";
        msgUtils.printMethodName(thisMethod);

        if (Constants.OAUTH_OP.equals(opServerType)) {
            providerApp = Constants.OAUTHCONFIGSAMPLE_APP;
            providerRoot = Constants.OAUTH_ROOT;
        } else {
            providerApp = Constants.OIDCCONFIGSAMPLE_APP;
            providerRoot = Constants.OIDC_ROOT;
        }

        //        // shouldn't need the root or sample name for the IDP
        //        // Set the proerties used in the shibboleth config itself
        //        if (Constants.IDP_SERVER_TYPE.equals(opServerType)) {
        //            String curDir = new File(".").getAbsoluteFile().getCanonicalPath().replace("\\", "/") + "/shibboleth-idp";
        //            Log.info(thisClass, thisMethod, "Current Dir: " + curDir);
        //            bootstrapUtils.writeBootstrapProperty(this, "idp.home", curDir);
        //            String logLoc = getServer().getLogsRoot().replace("\\", "/");
        //            bootstrapUtils.writeBootstrapProperty(this, "was.idp.logs", logLoc);
        //        } else {
        bootstrapUtils.writeBootstrapProperty(this, Constants.BOOT_PROP_PROVIDER_SAMPLE, providerApp);
        bootstrapUtils.writeBootstrapProperty(this, Constants.BOOT_PROP_PROVIDER_ROOT, providerRoot);
        //        }
    }

    /** TODO *************************************** General utils *****************************************/

    public static void addToMyList(List<String> theList, String theString) {
        if (theList == null) {
            theList = new ArrayList<String>();
        }
        theList.add(theString);
    }

}
