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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.apps.AppConstants;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;

import componenttest.topology.impl.LibertyFileManager;

/**
 * This class contains tools used for SameSiteCookie tests
 *
 **/

public class SameSiteTestTools {

    public static Class<?> thisClass = SameSiteTestTools.class;

    public static ValidationData vData = new ValidationData();
    public static CommonValidationTools validationTools = new CommonValidationTools();

    public static final String SameSiteCookieKey = "mySameSiteCookie";
    public static final String SsoRequiresSSL = "mySsoRequiresSSL";
    public static final String HttpsRequiredKey = "myHttpsRequired";
    public static final String RedirectHostKey = "redirectHost";
    public static final String CookieSecureKey = "myCookieSecure";
    public static final String CookiesEnabledKey = "myCookiesEnabled";
    public static final String CookieHttpOnlyKey = "myCookieHttpOnly";
    public static final String AuthorizationHostKey = "authorizationHost";
    public static final String TokenHostKey = "tokenHost";
    public static final String IntrospectHostKey = "introspectHost";
    public static final String ValidationHostKey = "validationHost";
    //    public static final String UserKey = "testCaseUser";

    //    CommonTestHelpers helpers = new CommonTestHelpers();

    protected static List<String> configUpdateMsgs = Arrays.asList(MessageConstants.CWWKG0017I_CONFIG_UPDATE_COMPLETE);

    private TestServer testOPServer = null;
    private TestServer testRPServer = null;
    private TestServer genericTestServer = null;
    private List<TestServer> serverRefList = null;

    public SameSiteTestTools() {
    }

    // These tools are used by both oidc/oauth and social fats
    // We can't just extend the CommonTests class as too many classes that don't need these tools
    // will be polluted with them, so, keep a local instance of the server refs for use by the methods
    // in this class
    public SameSiteTestTools(TestServer opServer, TestServer rpServer, TestServer rsServer, List<TestServer> servers) {

        testOPServer = opServer;
        testRPServer = rpServer;
        genericTestServer = rsServer;
        serverRefList = servers;
    }

    /**
     * Update/Set config variables for a server and push the updates to the server.
     * Method waits for server to update or indicate that no update in needed
     *
     * @param server
     *            - ref to server that will be updated
     * @param valuesToSet
     *            - a map of the variables and their values to set
     * @throws Exception
     */
    public void updateServerSettings(TestServer server, Map<String, String> valuesToSet) throws Exception {

        String thisMethod = "updateServerSettings";

        // update the mark so that we look for the config update message only in the latest log entries
        // we had cases where we were finding the config update complete message from the previous config
        // we made requests using the old config (the new one wasn't ready) and were getting bad results.
        server.setMarkToEndOfLogs();

        ServerConfiguration config = server.getServer().getServerConfiguration();
        ConfigElementList<Variable> configVars = config.getVariables();

        for (Variable variableEntry : configVars) {
            Log.info(thisClass, thisMethod, "Already set configVar: " + variableEntry.getName() + " configVarValue: " + variableEntry.getValue());
        }

        for (Entry<String, String> variableEntry : valuesToSet.entrySet()) {
            addOrUpdateConfigVariable(configVars, variableEntry.getKey(), variableEntry.getValue());
        }

        server.getServer().updateServerConfiguration(config);
        server.getServer().waitForConfigUpdateInLogUsingMark(null);
        //        helpers.testSleep(5);
    }

    /**
     * Update a servers variable map with the key/value passed in.
     *
     * @param vars
     *            - map of existing variables
     * @param name
     *            - the key to add/update
     * @param value
     *            - the value for the key specified
     */
    protected void addOrUpdateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {

        Variable var = vars.getBy("name", name);
        if (var == null) {
            vars.add(new Variable(name, value));
        } else {
            var.setValue(value);
        }
    }

    // using this method to update multiple variables will result in multiple config updates
    // if you need to update multiple values, call updateServerSettings directly will all updates
    /**
     * Add/update one variable in a server config
     *
     * @param server
     *            - the server to update
     * @param key
     *            - The variable's key nane
     * @param value
     *            - the variable's value
     * @throws Exception
     */
    public void setOneVar(TestServer server, String key, String value) throws Exception {
        Map<String, String> vars = new HashMap<String, String>();
        vars.put(key, value);
        updateServerSettings(server, vars);
    }

    /**
     * Create a map of the default settings for an OP server
     *
     * @return - map of default OP variables
     * @throws Exception
     */
    public Map<String, String> setDefaultOPConfigSettingsMap() throws Exception {

        Map<String, String> variablesToSet = new HashMap<String, String>();
        variablesToSet.put(CookieSecureKey, "true");
        variablesToSet.put(CookiesEnabledKey, "true");
        variablesToSet.put(CookieHttpOnlyKey, "true");
        variablesToSet.put(SameSiteCookieKey, Constants.SAMESITE_DISABLED);
        variablesToSet.put(SsoRequiresSSL, "false");
        variablesToSet.put(HttpsRequiredKey, "false");
        variablesToSet.put(RedirectHostKey, testRPServer.getServerHttpsString());

        return variablesToSet;

    }

    // updates the OP server with the default config values (could also say restores the default values)
    public Map<String, String> setOPConfigSettings() throws Exception {
        return setOPConfigSettings(new HashMap<String, String>());
    }

    /**
     * Creates a map of the default settings for the OP plus
     * any values to override or add to those settings
     *
     * @param updates
     *            - values to update/add
     * @return - updated map of variable settings
     * @throws Exception
     */
    public Map<String, String> setOPConfigSettings(Map<String, String> updates) throws Exception {

        Map<String, String> variables = setDefaultOPConfigSettingsMap();
        for (Entry<String, String> update : updates.entrySet()) {
            variables.put(update.getKey(), update.getValue());
        }
        updateServerSettings(testOPServer, variables);
        return variables;
    }

    /**
     * Create a map of the default settings for an RP server
     *
     * @return - map of default RP variables
     * @throws Exception
     */
    public Map<String, String> setDefaultRPConfigSettingsMap() throws Exception {

        Map<String, String> variablesToSet = new HashMap<String, String>();
        variablesToSet.put(CookieSecureKey, "true");
        variablesToSet.put(CookiesEnabledKey, "true");
        variablesToSet.put(CookieHttpOnlyKey, "true");
        variablesToSet.put(SameSiteCookieKey, Constants.SAMESITE_DISABLED);
        variablesToSet.put(SsoRequiresSSL, "false");
        variablesToSet.put(HttpsRequiredKey, "false");
        variablesToSet.put(AuthorizationHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(TokenHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(IntrospectHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(RedirectHostKey, testRPServer.getServerHttpsString());
        //        variablesToSet.put(RedirectHostKey, testRPServer.getServerHttpsCanonicalString());

        return variablesToSet;

    }

    // updates the RP server with the default config values (could also say restores the default values)
    public Map<String, String> setRPConfigSettings() throws Exception {
        return setRPConfigSettings(new HashMap<String, String>());
    }

    /**
     * Creates a map of the default settings for the RP plus
     * any values to override or add to those settings
     *
     * @param updates
     *            - values to update/add
     * @return - updated map of variable settings
     * @throws Exception
     */
    public Map<String, String> setRPConfigSettings(Map<String, String> updates) throws Exception {

        Map<String, String> variables = setDefaultRPConfigSettingsMap();
        for (Entry<String, String> update : updates.entrySet()) {
            variables.put(update.getKey(), update.getValue());
        }
        updateServerSettings(testRPServer, variables);
        return variables;
    }

    /**
     * Create a map of the default settings for an RS server
     *
     * @return - map of default RS variables
     * @throws Exception
     */
    public Map<String, String> setDefaultRSConfigSettingsMap() throws Exception {

        Map<String, String> variablesToSet = new HashMap<String, String>();
        variablesToSet.put(CookieSecureKey, "true");
        variablesToSet.put(CookiesEnabledKey, "true");
        variablesToSet.put(CookieHttpOnlyKey, "true");
        variablesToSet.put(SameSiteCookieKey, Constants.SAMESITE_DISABLED);
        variablesToSet.put(SsoRequiresSSL, "false");
        variablesToSet.put(HttpsRequiredKey, "false");
        variablesToSet.put(AuthorizationHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(TokenHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(IntrospectHostKey, testOPServer.getServerHttpsString());
        variablesToSet.put(RedirectHostKey, testRPServer.getServerHttpsString());
        variablesToSet.put(ValidationHostKey, testOPServer.getServerHttpsString());

        return variablesToSet;

    }

    // updates the RS server with the default config values (could also say restores the default values)
    public Map<String, String> setRSConfigSettings() throws Exception {
        return setRSConfigSettings(new HashMap<String, String>());
    }

    /**
     * Creates a map of the default settings for the RS plus
     * any values to override or add to those settings
     *
     * @param updates
     *            - values to update/add
     * @return - updated map of variable settings
     * @throws Exception
     */
    public Map<String, String> setRSConfigSettings(Map<String, String> updates) throws Exception {

        Map<String, String> variables = setDefaultRSConfigSettingsMap();
        for (Entry<String, String> update : updates.entrySet()) {
            variables.put(update.getKey(), update.getValue());
        }
        updateServerSettings(genericTestServer, variables);
        return variables;
    }

    /***********/

    /**
     * logs sub-test start/stop messages in each of the server logs
     *
     * @param action
     *            - start or stop action
     * @param msg
     *            - the message to log
     * @throws Exception
     */
    public void logStepInServerSideLogs(String action, String msg) throws Exception {

        for (TestServer server : serverRefList) {
            logStepInServerSideLog(action, msg, server);
        }
    }

    /**
     * Logs the sub-test message in the specified server's log
     *
     * @param action
     *            - Start or Stop string
     * @param msg
     *            - the message to record (generally info on which sub-test is to be logged)
     * @param server
     *            - the reference to the server to log in the information
     * @throws Exception
     */
    private void logStepInServerSideLog(String action, String msg, TestServer server) throws Exception {

        try {
            if (server != null) {
                Log.info(thisClass, "logTestCaseInServerSideLog", server.getServer().getServerName());
                WebConversation wc = new WebConversation();
                WebRequest request = new GetMethodWebRequest(server.getServerHttpString() + "/" + AppConstants.TESTMARKER_CONTEXT_ROOT + "/" + AppConstants.TESTMARKER_PATH);
                request.setParameter("action", action);
                request.setParameter("testCaseName", msg);
                wc.getResponse(request);
            }
        } catch (Exception e) {
            // just log the failure - we shouldn't allow a failure here to cause
            // a test case to fail.
            e.printStackTrace();
        }

    }

    /**
     * Backup/record the configuration for a specific sub-test
     *
     * @param testName
     *            - name of the test being run (will be the basis of the server file name)
     * @param baseNameToRemove
     *            - The part of the test case name to omit from the server configs filename (All test cases in a class typically
     *            start with the name of the class - to keep the filenames to a resonable lenght, we can remove that and just keep
     *            the unique part)
     * @param server
     *            - the server who's config is to be saved
     * @param testCaseExtension
     *            - a string to add to the server file name - should be the sub-test name
     * @throws Exception
     */
    public void backupConfig(String testName, String baseNameToRemove, TestServer server, String testCaseExtension) throws Exception {
        String thisMethod = "backupConfig";
        File testServerDir = server.getTestServerDir();
        String baseBackupConfigName = testName.replace(baseNameToRemove, "") + "_" + testCaseExtension.replace(" ", "_");
        String backupConfigName = baseBackupConfigName + "_server.xml";
        Log.info(thisClass, thisMethod, "Backing up server.xml for test: " + baseBackupConfigName + " to " + testServerDir.toString() + "/" + backupConfigName);
        LibertyFileManager.copyFileIntoLiberty(server.getServer().getMachine(), testServerDir.toString(), backupConfigName, server.getServerFileLoc() + "/server.xml");

    }

    /**
     * Copy a map
     *
     * @param mapToCopy
     *            - the map to make a copy of
     * @return - the copy of the map
     * @throws Exception
     */
    public Map<String, String> createOrRestoreConfigSettings(Map<String, String> mapToCopy) throws Exception {

        Map<String, String> newMap = new HashMap<String, String>();
        for (Entry<String, String> valueToCopy : mapToCopy.entrySet()) {
            newMap.put(valueToCopy.getKey(), valueToCopy.getValue());
        }
        return newMap;
    }

}
