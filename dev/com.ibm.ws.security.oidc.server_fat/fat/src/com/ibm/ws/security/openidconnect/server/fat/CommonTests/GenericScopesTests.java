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

package com.ibm.ws.security.openidconnect.server.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test class for scope. Tests scope handling within the OP.
 * These tests are run for both web and implicit clients.
 * They run with both oauth and oidc. The extending class will set the test
 * setting appropriatly for the flow/provider being tested.
 * The extending class specifies the action/flow.
 * 
 * @author chrisc
 * 
 */
public class GenericScopesTests extends CommonTest {

    private static final Class<?> thisClass = GenericScopesTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static String targetProvider = null;
    protected static String[] goodActions = null;
    protected static String[] emptyScopeActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    // the next few variables need to be kept in sync with what's in the server.xml
    //protected static final String client01String = "openid scope1 scope2" ;
    protected static final String client01String = "ALL_SCOPES";
    protected static final String client02String = "openid scope1 scope2";
    protected static final String client03String = "";
    protected static final String client04sString = null;

    /**
     * Updates the test setting with values needed for an implicit flow (called
     * by the extending classes)
     * 
     * @return - returns updated test settings
     * @throws Exception
     */
    protected static TestSettings setImplicitClientDefaultSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setNonce(Constants.DEFAULT_NONCE);
        updatedTestSettings.setFlowType(Constants.IMPLICIT_FLOW);
        return updatedTestSettings;

    }

    protected static TestSettings update2LegScopeSettings(TestSettings inSettings, String fType) {

        TestSettings updatedTestSettings = inSettings.copyTestSettings();
        String clCfgScopes = null;
        Log.info(thisClass, "update2LegScopeSettings", "Using client: " + inSettings.getClientID());
        if (inSettings.getClientID().equals("client01")) {
            clCfgScopes = client01String;
        } else {
            if (inSettings.getClientID().equals("client02")) {
                clCfgScopes = client02String;
            } else {
                if (inSettings.getClientID().equals("client03")) {
                    clCfgScopes = client03String;
                } else {
                    if (inSettings.getClientID().equals("client04s")) {
                        Log.info(thisClass, "update2LegScopeSettings", "see client04s");
                        clCfgScopes = client04sString;
                    }
                }
            }
        }
        // expected scopes for implicit and authorization client flow is just the common scopes
        // between the request and the server config
        String newScopes = "";

        if ((clCfgScopes == null || clCfgScopes.isEmpty())) {
            if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
                Log.info(thisClass, "update2LegScopeSettings", "Null/Empty client scopes: set empty scope");
                updatedTestSettings.setScope("");
            } else {
                Log.info(thisClass, "update2LegScopeSettings", "Null/Empty client scopes: set requested scopes");
                updatedTestSettings.setScope(inSettings.getScope());
            }
        } else {
            if (clCfgScopes.equals("ALL_SCOPES")) {
                Log.info(thisClass, "update2LegScopeSettings", "ALL_SCOPES client scopes: set requested scopes");
                updatedTestSettings.setScope(inSettings.getScope());
            } else {
                if (eSettings.getProviderType().equals(Constants.OIDC_OP) ||
                        ((fType.equals(Constants.WEB_CLIENT_FLOW) || fType.equals(Constants.IMPLICIT_FLOW)) && eSettings.getProviderType().equals(Constants.OAUTH_OP))) {
                    for (String cfgScope : clCfgScopes.split(" ")) {
                        Log.info(thisClass, "update2LegScopeSettings", "cfg scope: " + cfgScope);
                        Log.info(thisClass, "update2LegScopeSettings", "request scopes: " + inSettings.getScope());
                        if (inSettings.getScope().contains(cfgScope)) {
                            if (newScopes.isEmpty()) {
                                newScopes = cfgScope;
                            } else {
                                newScopes = newScopes + " " + cfgScope;
                            }
                        }
                    }
                } else {
                    newScopes = inSettings.getScope();
                }
                Log.info(thisClass, "update2LegScopeSettings", "new scope: " + newScopes);
                updatedTestSettings.setScope(newScopes);
            }
        }

        updatedTestSettings.printTestSettings();
        return updatedTestSettings;
    }

    /**
     * TestDescription:
     * 
     * This test has autoAuth enabled for the client and no scopes specified in the server config
     * It verifies that the access_token lists the scopes that the client passes in for OAuth request.
     * It also shows that we can get to the protected resource.
     * OIDC request fails in client credential/password flows because there is no common scope between registered (null) and
     * requested scopes (openid scope1 scope2).
     * 
     */
    @AllowedFFDC(value = { "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException" })
    @Test
    public void testScope_NoScopesInConfig() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client04s", null, targetProvider, targetProvider + "Scopes", null);
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        // expect good results - no scope in the server.xml is specified, 
        // so the scope on the request is the scope that we get in the response - this is true for OAuth. OIDC behaves differently. 

        List<validationData> expectations = null;
        if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            actions = emptyScopeActions;
            overrideRedirect();
            if ((flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW) || flowType.equals(Constants.PASSWORD_FLOW))) {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "\":\"" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "\"" + Constants.ERROR_RESPONSE_DESCRIPTION + "\":\"" + "CWOAU0064E");
            } else {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_SERVER);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "=" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_DESCRIPTION + "=" + "CWOAU0068E");
            }
        } else {
            expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);
        }

        updatedTestSettings.setFlowType(flowType);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has autoAuth enabled for the client and no scopes specified in the server config
     * It verifies that the access_token lists the scopes that the client passes in for OAuth/OIDC request.
     * It also shows that we can get to the protected resource.
     * 
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_ALL_SCOPES_ScopesInConfig() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, null, null, targetProvider, targetProvider + "Scopes", "openid scope1 scope2");
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        // expect good results - all scope in the server.xml is specified, 
        // so the scope on the request is the scope that we get in the response - this is true for OAuth & OIDC 

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);

        updatedTestSettings.setFlowType(flowType);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    // with no scopes configured in the server, it shoulnd't matter how many we specify in the request
    // so, the next 2 tests really only makes sense when scopes are set in the server
    //	public void testScope_NoScopesInConfig_reducedClientScopes() throws Exception {
    //	public void testScope_NoScopesInConfig_increaseClientScopes() throws Exception {

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and no scopes specifed in the server config
     * It verifies that we do not get an id_token if openid is NOT specified in the scope of the request.
     * OAuth requests succeed.
     * OIDC request fails because there is no common scope between registered (null) and requested scopes (scope1 scope2).
     * Failures for authorization_code/implicit flows and client_credential/password flows differ as different endpoints
     * are used and the msgs are slightly differnet
     * 
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_NoScopesInConfig_openidNotInScope() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client04s", null, targetProvider, targetProvider + "Scopes", "scope1 scope2");
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        // expect good results - no scope in the server.xml is specified, 
        // so the scope on the request is the scope that we get in the response - this is true for OAuth. OIDC behaves differently. 

        List<validationData> expectations = null;
        if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            actions = emptyScopeActions;
            overrideRedirect();
            if ((flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW) || flowType.equals(Constants.PASSWORD_FLOW))) {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "\":\"" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "\"" + Constants.ERROR_RESPONSE_DESCRIPTION + "\":\"" + "CWOAU0064E");
                actions = emptyScopeActions;
            } else {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_SERVER);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "=" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_DESCRIPTION + "=" + "CWOAU0068E");
            }
        }
        else {
            expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);
        }

        updatedTestSettings.setFlowType(flowType);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and no scopes specifed in the server config
     * It verifies that we do not get an id_token if openid is NOT specified in the scope of the request.
     * OAuth requests succeed.
     * When there is an empty or missing registered scope, then user is presented with the consent form
     * with all the scopes specified in the request and will have a choice to select the scope. So whatever scope set is left
     * after the consent is treated as
     * a resultant scope in these flows.
     * 
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_ALL_SCOPES_ScopesInConfig_openidNotInScope() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, null, null, targetProvider, targetProvider + "Scopes", "scope1 scope2");
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        // expect good results - no scope in the server.xml is specified, 
        // so the scope on the request is the scope that we get in the response - this is true for OAuth. OIDC behaves differently. 

        List<validationData> expectations = null;
        expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);

        updatedTestSettings.setFlowType(flowType);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    // testScope_NoScopesInConfig_noScopesInCommon - doesn't make sense - nothing in the server config to compare against

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and our normal tests scopes specifed in the server config
     * (openid scope1 scope2)
     * The scopes in the request matches what is in the server
     * It verifies that the set of scopes passed in is what is in the access_token
     * It also shows that we can get to the protected resource
     * 
     * @throws Exception
     */
    @Test
    public void testScope_NormalScopesInConfig() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", null);

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, update2LegScopeSettings(updatedTestSettings, flowType), _testName, flowType);

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and our normal tests scopes specifed in the server config
     * (openid scope1 scope2)
     * It verifies that a subset of the server scopes passed in on the request is what is in the access_token
     * It also shows that we can get to the protected resource
     * 
     * @throws Exception
     */
    @Test
    public void testScope_NormalScopesInConfig_reducedClientScopes() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "openid scope1");

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, update2LegScopeSettings(updatedTestSettings, flowType), _testName, flowType);

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and our normal tests scopes specifed in the server config
     * (openid scope1 scope2)
     * It verifies that if extra scopes are passed in on the request, the only scopes in the access_token will be those in the
     * server
     * It also shows that we can get to the protected resource
     * 
     * @throws Exception
     */
    @Test
    public void testScope_NormalScopesInConfig_increaseClientScopes() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "openid scope1 scope2 scope3");

        //		TestSettings updatedTestSettings2 = updatedTestSettings.copyTestSettings(); ;
        //		updatedTestSettings2.setScope(authHelpers.fixScope(eSettings, updatedTestSettings, "openid scope1 scope2")) ;

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, update2LegScopeSettings(updatedTestSettings, flowType), _testName, flowType);

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and "openid scope1 scope2" scopes specifed in the server config
     * It verifies that we do NOT get an id_token if openid is NOT specified in the scope of the request
     * It also shows that we can get to the protected resource
     * 
     * @throws Exception
     */
    @Test
    public void testScope_NormalScopesInConfig_openidNotInScope() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "scope1 scope2");

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, update2LegScopeSettings(updatedTestSettings, flowType), _testName, flowType);

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and "openid scope1 scope2" scopes specified in the server configuration
     * The request has a different set of scopes specified - with no scopes in common with the server
     * Test verifies that we get an exception and no access_token, nor id_token
     * OIDC request receives InvalidScopeException. OAuth request behavior is not changed in this case.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_NormalScopesInConfig_noScopesInCommon() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "scope3 scope4");
        List<validationData> expectations = null;
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);
        } else if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            actions = emptyScopeActions;
            //overrideRedirect() ;
            if ((flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW) || flowType.equals(Constants.PASSWORD_FLOW))) {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "\":\"" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "\"" + Constants.ERROR_RESPONSE_DESCRIPTION + "\":\"" + "CWOAU0064E");

            } else {
                expectations = vData.addSuccessStatusCodes();
                //expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.REDIRECT_STATUS)  ;
                expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "=" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "&" + Constants.ERROR_RESPONSE_DESCRIPTION + "=" + "CWOAU0064E");
            }
        }

        updatedTestSettings.setFlowType(flowType);

        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and "openid scope1 scope2" scopes specified in the server config
     * The request specified "OPENID SCOPE1 SCOPE2" and since scope is case SENSITIVE there will be no scopes in common with the
     * server
     * Test verifies that we get an exception and no access_token, nor id_token
     * OIDC request receives InvalidScopeException. OAuth request succeeds (no behavior change).
     * 
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_NormalScopesInConfig_upperCaseScope() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "OPENID SCOPE1 SCOPE2");
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;

        List<validationData> expectations = null;

        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);
        } else if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            actions = emptyScopeActions;
            //			overrideRedirect() ;
            if ((flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW) || flowType.equals(Constants.PASSWORD_FLOW))) {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "\":\"" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "\"" + Constants.ERROR_RESPONSE_DESCRIPTION + "\":\"" + "CWOAU0064E");
            } else {
                expectations = vData.addSuccessStatusCodes();
                //				expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.REDIRECT_STATUS)  ;
                expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "=" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "&" + Constants.ERROR_RESPONSE_DESCRIPTION + "=" + "CWOAU0064E");
            }
        }
        updatedTestSettings.setFlowType(flowType);
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has authoAuth enabled for the client and "openid scope1 scope2" scopes specifed in the server config
     * The request specified "OPENID SCOPE1 SCOPE2" and since scope is case SENSITIVE there ill be no scopes in common with the
     * server
     * Test verifies that we get an exception and no access_token, nor id_token
     * 
     * @throws Exception
     */

    //@Test
    public void testScope_NormalScopesInConfig_upperCaseScope_ORIGINAL() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client02", null, targetProvider, targetProvider + "Scopes", "OPENID SCOPE1 SCOPE2");

        //List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);
        //expectations = vData.addResponseStatusExpectation(expectations, Constants.PERFORM_LOGIN, Constants.FORBIDDEN_STATUS);

        //		TestSettings updatedTestSettings2 = updatedTestSettings.copyTestSettings(); ;
        //		updatedTestSettings2.setScope(authHelpers.fixScope(eSettings, updatedTestSettings, "")) ;

        List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, update2LegScopeSettings(updatedTestSettings, flowType), _testName, flowType);

        // update once 135670 is completed
        //        // check for failure msgs
        //		List<validationData> expectations = authHelpers.setGoodAuthExpectations(eSettings, updatedTestSettings, _testName, flowType) ;

        genericOP(_testName, wc, updatedTestSettings, goodActions, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test has autoAuth enabled for the client and no scopes specified in the server config
     * It verifies that the access_token lists the scopes that the client passes in for OAuth request.
     * It also shows that we can get to the protected resource.
     * OIDC request fails in client credential/password flows because there is no common scope between registered ("") and
     * requested scopes (openid scope1 scope2).
     * OAUth request behavior is same as before.
     * 
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidScopeException")
    @Test
    public void testScope_EmptyScopesInConfig() throws Exception {

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, "client03", null, targetProvider, targetProvider + "Scopes", null);
        TestSettings settings = update2LegScopeSettings(updatedTestSettings, flowType);
        String[] actions = goodActions;
        // expect good results - the scope with value "" in the server.xml should be treated as if there was NO scope set
        // so the scope on the request is the scope that we get in the response - this is true for OAuth. OIDC behaves different. 

        List<validationData> expectations = null;
        if (eSettings.getProviderType().equals(Constants.OIDC_OP)) {
            actions = emptyScopeActions;
            overrideRedirect();
            if ((flowType.equals(Constants.CLIENT_CREDENTIAL_FLOW) || flowType.equals(Constants.PASSWORD_FLOW))) {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "\":\"" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, "\"" + Constants.ERROR_RESPONSE_DESCRIPTION + "\":\"" + "CWOAU0064E");

            } else {
                expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_SERVER);
                expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.REDIRECT_STATUS);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_PARM + "=" + Constants.ERROR_CODE_INVALID_SCOPE);
                expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS,
                        "Did not get failure due to invalid scope", null, Constants.ERROR_RESPONSE_DESCRIPTION + "=" + "CWOAU0068E");
            }
        }
        else {
            expectations = authHelpers.setGoodAuthExpectations(eSettings, settings, _testName, flowType);
        }

        updatedTestSettings.setFlowType(flowType);
        WebConversation wc = new WebConversation();
        genericOP(_testName, wc, updatedTestSettings, actions, expectations);

    }

}
