/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.commonTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.utils.Constants;

public class CommonAnnotatedSecurityTests extends CommonSecurityFat {

    protected static Class<?> thisClass = CommonAnnotatedSecurityTests.class;

    protected final TestActions actions = new TestActions();
    protected final TestValidationUtils validationUtils = new TestValidationUtils();

    protected static String opHttpBase = null;
    protected static String opHttpsBase = null;
    protected static String rpHttpBase = null;
    protected static String rpHttpsBase = null;

    public static void updateTrackers(LibertyServer opServer, LibertyServer rpServer, boolean serversAreReconfigured) throws Exception {

        // track the servers that we start so that they'll be cleaned up at the end of this classes execution, or if the tests fail out
        serverTracker.addServer(opServer);
        serverTracker.addServer(rpServer);
        if (!serversAreReconfigured) {
            // at the moment, none of the tests reconfigure the servers, so skip restoring them at the end of each test case.
            skipRestoreServerTracker.addServer(opServer);
            skipRestoreServerTracker.addServer(rpServer);
        }

    }

    public Expectations getGotToTheAppExpectations(String app, String url) throws Exception {
        Expectations expectations = getGotToTheAppExpectations(null, app, url);
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, Constants.BASE_SERVLET_MESSAGE
                                                                                                 + "SimpleServlet", "Did not land on the unprotected app."));
        return expectations;
    }

    public Expectations getGotToTheAppExpectations(String currentAction, String app, String url) throws Exception {
        Expectations expectations = CommonExpectations.successfullyReachedUrl(currentAction, url);
        expectations.addExpectation(new ResponseFullExpectation(currentAction, Constants.STRING_CONTAINS, Constants.BASE_SERVLET_MESSAGE
                                                                                                          + app, "Did not land on the unprotected app."));
        return expectations;

    }

    /**
     * Invoke the reqeusted app - and ensure that we landed on the login page (login.jsp)
     *
     * @param webClient the webClient to use to make the request
     * @param url the test requested url to attempt to access
     * @return the login page (that the next step will use to actually login)
     * @throws Exception
     */
    public Page invokeAppReturnLoginPage(WebClient webClient, String url) throws Exception {

        Page response = actions.invokeUrl(_testName, webClient, url);

        Expectations firstExpectations = CommonExpectations.successfullyReachedOidcLoginPage();

        validationUtils.validateResult(response, firstExpectations);

        return response;

    }

    public Page processLogin(Page response, String user, String pw, String app) throws Exception {

        Expectations currentExpectations = new Expectations();
        currentExpectations.addSuccessCodeForCurrentAction();
        currentExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "got here", "Did not land on the callback."));
        currentExpectations
                        .addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, "Callback: OpenIdContext: null", "The context was null and should not have been"));
        Log.info(thisClass, _testName, "TODO Need to add a check for the app: " + app);
        // TODO - update to look for the actual app instead of landing on the callback
        // Maybe call a method to compare values from the OpenIdContext Logger when it is called from the callback and when it is called from the test servlet
//        currentExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, app, "Did not land on the test app."));

        response = actions.doFormLogin(response, user, pw);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, currentExpectations);

        return response;
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the standard user/password and does not need the webClient
     * afterwards
     *
     * @param appRoot the root of the app to invoke
     * @param app the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(String appRoot, String app) throws Exception {
        WebClient webClient = getAndSaveWebClient();
        return runGoodEndToEndTest(webClient, appRoot, app);
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the standard user/password, but does need the webClient
     * afterwards
     *
     * @param webClient the webClient to use to process the requests
     * @param appRoot the root of the app to invoke
     * @param app the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app) throws Exception {

        return runGoodEndToEndTest(webClient, appRoot, app, Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to specify the user/password and does need access to the webClient
     * afterwards
     *
     * @param webClient the webClient to use to process the requests
     * @param appRoot the root of the app to invoke
     * @param app the name of the app to invoke
     * @param user the user to log in as
     * @param pw the password to use to log in
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app, String user, String pw) throws Exception {

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        response = processLogin(response, user, pw, app);

        // TODO - will we need to perform any other step?
        return response;
    }

    public static Map<String, Object> buildUpdatedConfigMap(LibertyServer opServer, LibertyServer rpServer, String appName, String configFileName,
                                                            Map<String, Object> overrideConfigSettings) throws Exception {

        String sourceConfigFile = "publish/shared/config/oidcClient/" + configFileName;
        Log.info(thisClass, "buildUpdatedConfigMap", "sourceConfigFile: " + sourceConfigFile);

        Map<String, Object> updatedMap = new HashMap<String, Object>();

        File cf = new File(sourceConfigFile);
        InputStream configFile = new FileInputStream(cf);
        if (configFile != null) {
            Log.info(thisClass, "deployConfigurableTestApps", "Loading config from: " + sourceConfigFile);
            Properties config = new Properties();
            config.load(configFile);
            for (Entry<Object, Object> entry : config.entrySet()) {
                updatedMap.put((String) entry.getKey(), fixConfigValue(opServer, rpServer, appName, entry.getValue()));
                Log.info(thisClass, "deployConfigurableTestApps", "key: " + entry.getKey() + " updatedValue: " + updatedMap.get(entry.getKey()));
            }
        }

        if (overrideConfigSettings != null) {
            for (Entry<String, Object> entry : overrideConfigSettings.entrySet()) {
                updatedMap.put(entry.getKey(), fixConfigValue(opServer, rpServer, appName, entry.getValue()));
            }
        }

        return updatedMap;
    }

    public static Object fixConfigValue(LibertyServer opServer, LibertyServer rpServer, String appName, Object value) throws Exception {

        Object newValue = null;
        if (value instanceof String) {
            newValue = ((String) value).replace("op_Port_op", Integer.toString(opServer.getBvtPort()))
                            .replace("op_SecurePort_op", Integer.toString(opServer.getBvtSecurePort()))
                            .replace("rp_Port_rp", Integer.toString(rpServer.getBvtPort()))
                            .replace("rp_SecurePort_rp", Integer.toString(rpServer.getBvtSecurePort()))
                            .replace("rp_AppName_rp", appName);
        }
        return newValue;
    }
}