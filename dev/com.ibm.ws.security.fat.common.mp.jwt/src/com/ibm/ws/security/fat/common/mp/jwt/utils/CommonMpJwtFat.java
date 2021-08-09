/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.simplicity.config.FeatureManager;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwtFatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.actions.MpJwtFatActions;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.topology.impl.LibertyServer;

public class CommonMpJwtFat extends CommonSecurityFat {

    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();
    protected final MpJwtFatActions actions = new MpJwtFatActions();
//    protected final static MPJwtAppSetupUtils setupUtils = new MPJwtAppSetupUtils();
    protected final static List<String> commonStartMsgs = CommonWaitForAppChecks.getSecurityReadyMsgs();

    public static enum ExpectedResult {
        GOOD, BAD
    };

    protected final String defaultUser = MPJwtFatConstants.TESTUSER;
    protected final String defaultPassword = MPJwtFatConstants.TESTUSERPWD;

    /**
     * Startup a Liberty Server with the JWT Builder enabled
     *
     * @param server - the server to startup
     * @param configFile - the config file to use when starting the server
     * @throws Exception
     */
    protected static void setUpAndStartBuilderServer(LibertyServer server, String configFile) throws Exception {
        setUpAndStartBuilderServer(server, configFile, false);
    }

    /**
     * Startup a Liberty Server with the JWT Builder enabled - assume that call won't be reconfiguring the builder
     * and pass "true" for skipRestoreBetweenTests
     *
     * @param server - the server to startup
     * @param configFile - the config file to use when starting the serever
     * @param jwtEnabled - flag indicating if jwt should be enabled (used to set a bootstrap property that the config will use)
     * @throws Exception
     */
    protected static void setUpAndStartBuilderServer(LibertyServer server, String configFile, boolean jwtEnabled) throws Exception {
        setUpAndStartBuilderServer(server, configFile, true, jwtEnabled);
    }

    protected static void setUpAndStartBuilderServer(LibertyServer server, String configFile, boolean skipRestoreBetweenTests, boolean jwtEnabled) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, "oidcJWKEnabled", String.valueOf(jwtEnabled));
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MPJwtFatConstants.BVT_SERVER_2_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));
        if (skipRestoreBetweenTests) {
            skipRestoreServerTracker.addServer(server);
        }
        Log.info(thisClass, "setUpAndStartBuilderServer", server.getServerName() + " is ready to rock and roll!!!!!!");
    }

    public boolean isVersion12OrAbove(LibertyServer server) throws Exception {

        ServerConfiguration serverconfig = server.getServerConfiguration();
        FeatureManager fm = serverconfig.getFeatureManager();
        Set<String> features = fm.getFeatures();
        for (String feature : features) {
            if (feature.contains("mpJwt-")) {
                if (feature.contains("mpJwt-1.1")) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        // somehow feature not installed
        return false;
    }

    /**
     * This method determines which of the repeat actions that cause the test class to run
     * with different versions of the mpJwt feature is currently in use.
     * The getMostRecentRepeatAction method on RepeatTestFilter will return the latest repeat action, but
     * since the repeat actions can be nested, the mpJwt version action could be last, or first, or anywhere
     * between. We need to find the version in the list of all actions and return the appropriate string
     * to the caller who will then use that value to copy the feature files of the servers that the tests
     * will use...
     *
     * @param repeatActions
     * @return
     * @throws Exception
     */
    public static String setActionInstance(String repeatActions) throws Exception {

        String[] supportedActions = { MPJwtFatConstants.MP_JWT_11, MPJwtFatConstants.MP_JWT_12 };
        String currentAction = null;

        if (repeatActions != null) {
            for (String action : supportedActions) {
                if (repeatActions.contains(action)) {
                    return action;
                }
            }
        }
        return currentAction;

    }

    /*************************************/

    /**
     * Set good app check expectations - sets checks for good status code and for a message indicating what if any app class was invoked successfully
     *
     * @param theUrl - the url that the test invoked
     * @param appClass - the app class that should have been invoked
     * @return - newly created Expectations
     * @throws Exception
     */
    public Expectations goodAppExpectations(String theUrl, String appClass) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(theUrl));
        expectations.addExpectation(new ResponseFullExpectation(MPJwtFatConstants.STRING_CONTAINS, appClass, "Did not invoke the app " + appClass + "."));

        return expectations;
    }

    /**
     * Set bad app check expectations - sets checks for a 401 status code and the expected error message in the server's messages.log
     *
     * @param errorMessage - the error message to search for in the server's messages.log file
     * @return - newly created Expectations
     * @throws Exception
     */
    public Expectations badAppExpectations(String errorMessage) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ResponseMessageExpectation(MPJwtFatConstants.STRING_CONTAINS, errorMessage, "Did not find the error message: " + errorMessage));

        return expectations;
    }

    /**
     * Build the http app url
     *
     * @param theServer - The server where the app is running (used to get the port)
     * @param root - the root context of the app
     * @param app - the specific app to run
     * @return - returns the full url to invoke
     * @throws Exception
     */
    public String buildAppUrl(LibertyServer theServer, String root, String app) throws Exception {

        return SecurityFatHttpUtils.getServerUrlBase(theServer) + root + "/rest/" + app + "/" + MPJwtFatConstants.MPJWT_GENERIC_APP_NAME;

    }

    /**
     * Build the https app url
     *
     * @param theServer - The server where the app is running (used to get the port)
     * @param root - the root context of the app
     * @param app - the specific app to run
     * @return - returns the full url to invoke
     * @throws Exception
     */
    public String buildAppSecureUrl(LibertyServer theServer, String root, String app) throws Exception {

        return SecurityFatHttpUtils.getServerSecureUrlBase(theServer) + root + "/rest/" + app + "/" + MPJwtFatConstants.MPJWT_GENERIC_APP_NAME;

    }

    /**
     * Create the expectations for a good/successful test run. The App will log values for multiple data types obtained via different means. The app
     * will compare the values returned by each method to make sure that all of them return the same value for the same object.
     * This method will check to make sure that all of those checks worked.
     *
     * //TODO replace jwtTokenTools
     *
     * @param jwtTokenTools
     * @param theUrl - The test url that was invoked
     * @param testAppClass - the class of the test app invoked
     * @return - the expectations for a successful run
     * @throws Exception
     */
    public Expectations goodTestExpectations(JwtTokenForTest jwtTokenTools, String theUrl, String testAppClass) throws Exception {
        String AppFailedCheckMsg = "Values DO NOT Match --------";

        try {
            Expectations expectations = new Expectations();
            expectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, theUrl));
            expectations.addExpectation(new ResponseFullExpectation(MPJwtFatConstants.STRING_CONTAINS, testAppClass, "Did not invoke the app " + testAppClass + "."));
            expectations.addExpectation(new ResponseFullExpectation(MPJwtFatConstants.STRING_CONTAINS, testAppClass, "Did not invoke the app " + testAppClass + "."));
            expectations.addExpectation(new ResponseFullExpectation(MPJwtFatConstants.STRING_DOES_NOT_CONTAIN, AppFailedCheckMsg, "Response contained string \"" + AppFailedCheckMsg
                                                                                                                                  + "\" which indicates that injected claim values obtained via different means did NOT match"));
            expectations.addExpectations(addClaimExpectations(jwtTokenTools, testAppClass));

            return expectations;
        } catch (Exception e) {
            Log.info(thisClass, "goodTestExpectations", "Failed building expectations: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Adds expectations for specific claims that we'll find in the JWTs that we test with.
     * We check to see that the various forms of injection retrieve the claims properly
     * TODO - replace jwtTokenTools
     *
     * @param jwtTokenTools
     * @param testAppClass - the test class invoked
     * @return - returns the expectations for specific claims
     * @throws Exception
     */
    public Expectations addClaimExpectations(JwtTokenForTest jwtTokenTools, String testAppClass) throws Exception {
        try {
            Expectations expectations = new Expectations();
            if (!testAppClass.contains("ClaimInjection") || (testAppClass.contains("ClaimInjection") && testAppClass.contains("RequestScoped"))) {
                expectations.addExpectation(addApiOutputExpectation("getRawToken", MPJwtFatConstants.MP_JWT_TOKEN, null, jwtTokenTools.getJwtTokenString()));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getIssuer", MPJwtFatConstants.JWT_BUILDER_ISSUER, PayloadConstants.ISSUER));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getSubject", MPJwtFatConstants.JWT_BUILDER_SUBJECT, PayloadConstants.SUBJECT));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getTokenID", MPJwtFatConstants.JWT_BUILDER_JWTID, PayloadConstants.JWT_ID));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getExpirationTime", MPJwtFatConstants.JWT_BUILDER_EXPIRATION,
                                                                     PayloadConstants.EXPIRATION_TIME));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getIssuedAtTime", MPJwtFatConstants.JWT_BUILDER_ISSUED_AT, PayloadConstants.ISSUED_AT));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getAudience", MPJwtFatConstants.JWT_BUILDER_AUDIENCE, PayloadConstants.AUDIENCE));
                expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getGroups", MPJwtFatConstants.PAYLOAD_GROUPS, "groups"));
                // we won't have a list of claims to check for ClaimInjection, we don't use the api to retrieve the claims and there is no injected claim that lists all claims...
                if (!testAppClass.contains("ClaimInjection")) {
                    for (String key : jwtTokenTools.getPayloadClaims()) {
                        expectations.addExpectations(addApiOutputExpectation(jwtTokenTools, "getClaim", MPJwtFatConstants.JWT_BUILDER_CLAIM, key));
                    }
                }
            }

            return expectations;
        } catch (Exception e) {
            Log.info(thisClass, "addClaimExpectations", "Failed building expectations: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Adds the appropriate claim expectation for the requested claim.
     * Some claims will have a key:value and others will have key:<multivalue> and we'll need to build the
     * expectation properly. In some cases, we just want to check for the existence of the claim.
     * TODO - replace jwtTokenTools
     *
     * @param jwtTokenTools
     * @param jsonWebTokenApi -the jsonWebToken api (the runtime api method that returned the claim value)
     * @param claimIdentifier - the descriptive identifier for the claim
     * @param key - the claims key
     * @return - returns an expectation for the claim
     * @throws Exception
     */
    public Expectations addApiOutputExpectation(JwtTokenForTest jwtTokenTools, String jsonWebTokenApi, String claimIdentifier, String key) throws Exception {
        Expectations expectations = new Expectations();
        // syntax is a bit different for the "getClaim" results
        String passKeyName = null;
        if (jsonWebTokenApi.contains("getClaim")) {
            passKeyName = key;
        }
        List<String> values = jwtTokenTools.getElementValueAsListOfStrings(key);
//        Log.info(thisClass, "addApiOutputExpectations", "list of values is: " + values);
        if (!values.isEmpty()) {
            for (String value : values) {
//                Log.info(thisClass, "addApiOutputExpectations", "value: " + value);
                expectations.addExpectation(addApiOutputExpectation(jsonWebTokenApi, claimIdentifier, passKeyName, value));
            }
        } else {
            expectations.addExpectation(addApiOutputExpectation(jsonWebTokenApi, claimIdentifier, passKeyName, "null"));
        }

        return expectations;
    }

    /**
     * Create a response expectation for the key/value being checked
     *
     * @param api - The api that was used to retrieve the claim
     * @param claimIdentifier - A more descriptive identifier for the claim
     * @param key - the claim's key
     * @param value - the claims value
     * @return - returns a new Full Response expectation with a properly formatted string to look for the specified claim
     * @throws Exception
     */
    public ResponseFullExpectation addApiOutputExpectation(String api, String claimIdentifier, String key, String value) throws Exception {
        return new ResponseFullExpectation(MPJwtFatConstants.STRING_MATCHES, buildStringToCheck(claimIdentifier, key, value), "API " + api + " did NOT return the correct value ("
                                                                                                                              + value + ").");

    }

    /**
     * Build the string to search for (the test app should have logged this string if everything is working as it should)
     *
     * @param claimIdentifier - an identifier logged by the test app - could be the method used to obtain the key
     * @param key - the key to validate
     * @param value - the value to validate
     * @return - returns the string to look for in the output
     * @throws Exception
     */
    public String buildStringToCheck(String claimIdentifier, String key, String value) throws Exception {
        String builtString = claimIdentifier.trim();
        if (!claimIdentifier.contains(":")) {
            builtString = builtString + ":";
        }
        if (key != null) {
            builtString = builtString + " key: " + key + " value:.*" + value.replace("\"", "");
        } else {
            builtString = builtString + " " + value.replace("[", "\\[").replace("]", "\\]").replace("\"", "");
        }

        return builtString;

    }

    /**
     * Set expectations for tests that have bad issuers
     *
     * @return Expectations
     * @throws Exception
     */
    public Expectations setBadIssuerExpectations(LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the issuer is NOT valid."));

        return expectations;

    }

//    public boolean isA256GCMEnabled() throws Exception {
//
//    String thisMethod = "isA256GCMEnabled" ;
//    Provider[] providers = Security.getProviders();
//    algList = Security.getA
//    boolean found = false;
//    for (Provider provider : providers) {
//        String pName = provider.getName();
//        Log.info(thisClass, thisMethod, "Provider: " + pName);
//        if (pName.startsWith("IBMJSSE")) {
//            found = true;
//        }
//    }
//    return found ;

}
