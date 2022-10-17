/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.commonTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.junit.Before;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Utils;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.OpenIdContextExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.ResponseValues;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletRequestExpectationHelpers;
import io.openliberty.security.jakartasec.fat.utils.WsSubjectExpectationHelpers;

public class CommonAnnotatedSecurityTests extends CommonSecurityFat {

    protected static Class<?> thisClass = CommonAnnotatedSecurityTests.class;

    protected final TestActions actions = new TestActions();
    protected final TestValidationUtils validationUtils = new TestValidationUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    protected static String opHttpBase = null;
    protected static String opHttpsBase = null;
    protected static String rpHttpBase = null;
    protected static String rpHttpsBase = null;

    protected static ResponseValues rspValues;

    public static RepeatTests createTokenTypeRepeats() {

        String accessTokenType = Utils.getRandomSelection(Constants.JWT_TOKEN_FORMAT, Constants.OPAQUE_TOKEN_FORMAT);

        Log.info(thisClass, "createRepeats", "Will be running tests using a/an " + accessTokenType + " access_token");
        // note:  using the method addRepeat below instead of adding test repeats in line to simplify hacking up the tests locally to only run one or 2 variations (all the calls are the same - dont' have to worry about using "with" vs "andWith")
        RepeatTests rTests = null;

        rTests = addRepeat(rTests, new SecurityTestRepeatAction(accessTokenType));

        return rTests;

    }

    public static RepeatTests addRepeat(RepeatTests rTests, SecurityTestRepeatAction currentRepeat) {
        if (rTests == null) {
            return RepeatTests.with(currentRepeat);
        } else {
            return rTests.andWith(currentRepeat);
        }
    }

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

    public static void setTokenTypeInBootstrap(LibertyServer server) throws Exception {

        setTokenTypeInBootstrap(server, null, null);
    }

    public static LibertyServer setTokenTypeInBootstrap(LibertyServer opServer, LibertyServer rpJwtServer, LibertyServer rpOpaqueServer) throws Exception {

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.JWT_TOKEN_FORMAT)) {
            bootstrapUtils.writeBootstrapProperty(opServer, "opTokenFormat", "jwt");
            return rpJwtServer;
        } else {
            bootstrapUtils.writeBootstrapProperty(opServer, "opTokenFormat", "opaque");
            return rpOpaqueServer;
        }
        // not testing with mpJwt at the moment
    }

    public Expectations getGotToTheAppExpectations(String app, String url) throws Exception {
        Expectations expectations = getGotToTheAppExpectations(null, app, url);
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, Constants.BASE_SERVLET_MESSAGE
                                                                                                 + "BaseServlet", "Did not land on the unprotected app."));
        return expectations;
    }

    public Expectations getGotToTheAppExpectations(String currentAction, String app, String url) throws Exception {
        Expectations expectations = CommonExpectations.successfullyReachedUrl(currentAction, url);
        expectations.addExpectation(new ResponseFullExpectation(currentAction, Constants.STRING_CONTAINS, Constants.BASE_SERVLET_MESSAGE
                                                                                                          + app, "Did not land on the unprotected app."));
        return expectations;

    }

    public static void initResponseValues() throws Exception {
        rspValues = new ResponseValues();
        rspValues.setIssuer(opHttpsBase + "/oidc/endpoint/OP1");
    }

    @Override
    @Before
    public void commonBeforeTest() {
        super.commonBeforeTest();
        try {
            initResponseValues();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Invoke the reqeusted app - and ensure that we landed on the login page (login.jsp)
     *
     * @param webClient
     *            the webClient to use to make the request
     * @param url
     *            the test requested url to attempt to access
     * @return the login page (that the next step will use to actually login)
     * @throws Exception
     */
    public Page invokeAppReturnLoginPage(WebClient webClient, String url) throws Exception {

        return invokeAppReturnLoginPage(webClient, url, null, null);

    }

    public Page invokeAppReturnLoginPage(WebClient webClient, String url, Map<String, String> headers, List<NameValuePair> parms) throws Exception {

        rspValues.setOriginalRequest(url);
        Page response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, url, parms, headers);

        Expectations firstExpectations = CommonExpectations.successfullyReachedOidcLoginPage();

        validationUtils.validateResult(response, firstExpectations);

        return response;

    }

    /**
     * Wrapper to invoke the app - used in cases where we don't want to do a full login flow - caller should validate the
     * response.
     * Use this instead of just actions.invokeUrl when you'll need the originalRequest set properly in rspValues
     *
     * @param webClient
     * @param url
     * @return
     * @throws Exception
     */
    public Page invokeApp(WebClient webClient, String url) throws Exception {

        rspValues.setOriginalRequest(url);
        Page response = actions.invokeUrl(_testName, webClient, url);

        return response;

    }

    public Page processLogin(Page response, String user, String pw, String app) throws Exception {

        return processLogin(response, user, pw, app, null, null);

    }

    public Page processLogin(Page response, String user, String pw, String app, Map<String, String> headers,
                             List<NameValuePair> parms) throws Exception {

        Expectations currentExpectations = getProcessLoginExpectations(app, headers, parms);

        response = actions.doFormLogin(response, user, pw);
        // confirm protected resource was accessed
        validationUtils.validateResult(response, currentExpectations);
//        validateTheSameContext(ServletMessageConstants.CALLBACK, response);
        validateTheSameContext(ServletMessageConstants.SERVLET, response);

        return response;
    }

    public Expectations getProcessLoginExpectations(String app) throws Exception {
        return getProcessLoginExpectations(app, null, null);
    }

    public Expectations getProcessLoginExpectations(String app, Map<String, String> headers, List<NameValuePair> parms) throws Exception {

        Expectations processLoginexpectations = new Expectations();
        processLoginexpectations.addSuccessCodeForCurrentAction();
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "got here servlet", "Did not land on the servlet."));
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_DOES_NOT_CONTAIN, ServletMessageConstants.SERVLET
                                                                                                                     + "OpenIdContext: null", "The context was null and should not have been"));

        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                             + ServletMessageConstants.BASE_SERVLET, "Did not land on the test app."));
        processLoginexpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, ServletMessageConstants.HELLO_MSG
                                                                                                             + app, "Did not land on the test app."));

//            // check for the correct values from the callback in the server log
//            OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
//            WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.CALLBACK, rspValues);
        // check for the correct values from the servlet in the response
        OpenIdContextExpectationHelpers.getOpenIdContextExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        WsSubjectExpectationHelpers.getWsSubjectExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, rspValues);
        ServletRequestExpectationHelpers.getServletRequestExpectations(null, processLoginexpectations, ServletMessageConstants.SERVLET, headers, parms);

        return processLoginexpectations;
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the
     * standard user/password and does not need the webClient
     * afterwards
     *
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(String appRoot, String app) throws Exception {
        WebClient webClient = getAndSaveWebClient();
        return runGoodEndToEndTest(webClient, appRoot, app);
    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to use the
     * standard user/password, but does need the webClient
     * afterwards
     *
     * @param webClient
     *            the webClient to use to process the requests
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app) throws Exception {

        return runGoodEndToEndTest(webClient, appRoot, app, Constants.TESTUSER, Constants.TESTUSERPWD);

    }

    /**
     * Perform a good end-to-end run - make a general request to access a protected app. The calling test wants to specify the
     * user/password and does need access to the webClient
     * afterwards
     *
     * @param webClient
     *            the webClient to use to process the requests
     * @param appRoot
     *            the root of the app to invoke
     * @param app
     *            the name of the app to invoke
     * @param user
     *            the user to log in as
     * @param pw
     *            the password to use to log in
     * @return the web Page response - in case the caller needs to process it further
     * @throws Exception
     */
    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app, String user, String pw) throws Exception {

        return runGoodEndToEndTest(webClient, appRoot, app, user, pw, null, null);

    }

    public Page runGoodEndToEndTest(WebClient webClient, String appRoot, String app, String user, String pw, Map<String, String> headers,
                                    List<NameValuePair> parms) throws Exception {

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url, headers, parms);

        response = processLogin(response, user, pw, app, headers, parms);

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
            newValue = ((String) value).replace("op_Port_op",
                                                Integer.toString(opServer.getBvtPort())).replace("op_SecurePort_op",
                                                                                                 Integer.toString(opServer.getBvtSecurePort())).replace("rp_Port_rp",
                                                                                                                                                        Integer.toString(rpServer.getBvtPort())).replace("rp_SecurePort_rp",
                                                                                                                                                                                                         Integer.toString(rpServer.getBvtSecurePort())).replace("rp_AppName_rp",
                                                                                                                                                                                                                                                                appName);

        }
        return newValue;
    }

    public void validateNotTheSame(String instance, Page response1, Page response2) throws Exception {

        // TODO
//        String nonce1 = AutomationTools.getTokenFromResponse(response1, OpenIdContextExpectationHelpers.buildNonceString(instance));
//        String iat1 = AutomationTools.getTokenFromResponse(response1, OpenIdContextExpectationHelpers.buildIssuedAtTimeString(instance));
//        String nonce2 = AutomationTools.getTokenFromResponse(response2, OpenIdContextExpectationHelpers.buildNonceString(instance));
//        String iat2 = AutomationTools.getTokenFromResponse(response2, OpenIdContextExpectationHelpers.buildIssuedAtTimeString(instance));
//
//        Log.info(thisClass, "validateNotTheSame", "Nonces are: " + nonce1 + " and " + nonce2);
//        if (nonce1 == null || nonce2 == null) {
//            fail("Nonce value was null and should not have been");
//        }
//
//        if (!nonce1.equals(nonce2)) {
//            fail("The nonce was the same for two different application invocations and should NOT have been");
//        }
//
//        Log.info(thisClass, "validateNotTheSame", "iats are: " + iat1 + " and " + iat2);
//        if (iat1 == null || iat2 == null) {
//            fail("iat (issued at time) value was null and should not have been");
//        }
//
//        if (!iat1.equals(iat2)) {
//            fail("The iat was the same for two different application invocations and should NOT have been");
//        }

    }

    public void validateTheSameContext(String instance, Page response) throws Exception {

        String context = AutomationTools.getTokenFromResponse(response, OpenIdContextExpectationHelpers.buildContextString(instance));

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, "Private Credential: "
                                                                                                 + context, "Did not find the same OpenIdContext for both the WSSubject and the injected OpenIdContext."));

        validationUtils.validateResult(response, expectations);
    }
}
