/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt12.fat.sharedTests;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwt12FatConstants;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwtMPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;
import com.ibm.ws.security.mp.jwt12.fat.utils.MP12ConfigSettings;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a common test class that will test the alternate placement of mp jwt
 * config settings. They can be found in multiple places within the application,
 * or can be set as system or environment variables. The extending test classes
 * will do one of the following: - request use of an app that has placement of
 * MPConfig in "resources/META-INF/microprofile-config.properties" - request use
 * of an app that has placement of MPConfig in
 * "resources/WEB-INF/classes/META-INF/microprofile-config.properties" - request
 * use of a server that has placement of MPConfig in jvm.options - setup
 * MPConfig as environment variables
 *
 **/

@SuppressWarnings("restriction")
@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwt12MPConfigTests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwt12MPConfigTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat.builder")
    public static LibertyServer jwtBuilderServer;

    protected static final String BothHeaderGood = "HeaderGood";
    protected static final String BothCookieGood = "CookieGood";

    /********************************************
     * helper methods
     **************************************/

    /**
     * Setup apps, System properties or environment variables needed for the
     * tests. Return a list of apps that we need to wait for at startup.
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings
     *            - the settings values to use
     * @param mpConfigLocation
     *            - where to put the settings (system properties, env vars, or
     *            in apps)
     * @return
     * @throws Exception
     */

    // Need to pass in the resource server reference as we can be using one
    // of several
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, MP12ConfigSettings mpConfigSettings,
                                                        MPConfigLocation mpConfigLocation) throws Exception {

        setupBootstrapPropertiesForMPTests(server, MP12ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MpJwtFatConstants.JWK_CERT));

        setupMPConfig(server, mpConfigSettings, mpConfigLocation);

        startRSServerForMPTests(server, configFile);

    }

    /**
     * Setup the system properties, environment variables or
     * microprofile-config.properties in the test apps as appropriate. When
     * testing with System properties, we will update the jvm.options file and
     * install an app with no microprofile-config.properties file When testing
     * with env variables, we will set those in the server environment and
     * install an app with no microprofile-config.properties file When testing
     * with microprofile-config.properties in the app, we will create multiple
     * apps with a variety of settings within the microprofile-config.properties
     * file within the app. We'll also create apps with the
     * microprofile-config.properties in the META-INF directory and the
     * WEB-INF/classes/META-INF directory.
     *
     * @param theServer
     *            - the server to install the apps on and set the system
     *            properties or env variables for
     * @param mpConfigSettings
     *            - The microprofile settings to put into the various locations
     * @param mpConfigLocation
     *            - where this test instance would like the MPConfig settings
     *            (system properties, environment variables, or
     *            microprofile-config.properties in apps)
     * @throws Exception
     */

    public static void setupMPConfig(LibertyServer theServer, MP12ConfigSettings mpConfigSettings, MPConfigLocation mpConfigLocation) throws Exception {

        // build fully resolved issuer
        mpConfigSettings.setIssuer(buildIssuer(jwtBuilderServer, mpConfigSettings.getIssuer()));
        // remove variables/dollar signs/... (need the server defined before we
        // can do this...
        mpConfigSettings.setPublicKeyLocation(resolvedJwksUri(jwtBuilderServer, mpConfigSettings.getPublicKeyLocation()));

        Log.info(thisClass, "setupMP12Config", "mpConfigLocation is set to: " + mpConfigLocation.toString());
        switch (mpConfigLocation) {
            case SYSTEM_PROP:
                // if we're testing system properties, we'll need to update the
                // values in the jvm.options file (if the file exists, update it)
                setAlternateMP_ConfigProperties_InJvmOptions(theServer, mpConfigSettings);
                setupUtils.deployRSServerNoMPConfigInAppApp(theServer);
                break;
            case ENV_VAR:
                // if we're testing env variables, we'll need to set environment
                // variables
                setAlternateMP_ConfigProperties_envVars(theServer, mpConfigSettings);
                setupUtils.deployRSServerNoMPConfigInAppApp(theServer);
                break;
            case IN_APP:
                deployRSServerMPConfigInAppApps(theServer, mpConfigSettings);
                break;
            default:
                throw new Exception("Invalid MP Config location passed to setupMP12Config - tests do NOT understand " + mpConfigLocation);
        }

    }

    /**
     * Sets system properties before the server is started
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings
     *            - the mp-config settings values
     * @throws Exception
     */
    public static void setAlternateMP_ConfigProperties_InJvmOptions(LibertyServer theServer, MP12ConfigSettings mpConfigSettings) throws Exception {

        String jvmFile = theServer.getServerRoot() + "/jvm.options";
        updateJvmOptionsFile(jvmFile, mpConfigSettings);

    }

    /**
     * Update the values in the jvm.options file of the server
     *
     * @param jvmOptionsFile
     *            - the file to update
     * @param publicKey
     *            - the publicKey value to update
     * @param keyLoc
     *            - the keyLocation value to update
     * @param issuer
     *            - the issuer value to update
     */
    static void updateJvmOptionsFile(String jvmOptionsFile, MP12ConfigSettings mpConfigSettings) throws Exception {

        MPJwt11MPConfigTests.updateJvmOptionsFile(jvmOptionsFile, mpConfigSettings);

        HashMap<String, String> optionMap = new HashMap<String, String>();
        optionMap.put("xxx_header_xxx", mpConfigSettings.getHeader());
        optionMap.put("xxx_cookie_xxx", mpConfigSettings.getCookie());
        optionMap.put("xxx_audience_xxx", mpConfigSettings.getAudience());
        optionMap.put("xxx_algorithm_xxx", mpConfigSettings.getAlgorithm());
        Log.info(thisClass, "updateJvmOptionsFiles", "ALG: " + mpConfigSettings.getAlgorithm());

        CommonIOUtils cioTools = new CommonIOUtils();
        if (cioTools.replaceStringsInFile(jvmOptionsFile, optionMap)) {
            return;
        }
        throw new Exception("Failure updating the jvm.options file - tests will NOT behave as expected - exiting");

    }

    /**
     * Sets/creates environment variables
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings
     *            - the mp-config settings values
     * @throws Exception
     */
    public static void setAlternateMP_ConfigProperties_envVars(LibertyServer server, MP12ConfigSettings mpConfigSettings) throws Exception {

        // some platforms do NOT support env vars with ".", so, we'll use
        // underscores "_" (our runtime allows either)
        String HeaderName = "mp_jwt_token_header";
        String CookieName = "mp_jwt_token_cookie";
        String AudienceName = "mp_jwt_verify_audiences";
        String AlgorithmName = "mp_jwt_verify_publickey_algorithm";

        HashMap<String, String> envVars = new HashMap<String, String>();
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", HeaderName + "=" + mpConfigSettings.getHeader());
        envVars.put(HeaderName, mpConfigSettings.getHeader());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", CookieName + "=" + mpConfigSettings.getCookie());
        envVars.put(CookieName, mpConfigSettings.getCookie());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", AudienceName + "=" + mpConfigSettings.getAudience());
        envVars.put(AudienceName, mpConfigSettings.getAudience());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", AlgorithmName + "=" + mpConfigSettings.getAlgorithm());
        envVars.put(AlgorithmName, mpConfigSettings.getAlgorithm());

        MPJwt11MPConfigTests.setAlternateMP_ConfigProperties_envVars(envVars, server, mpConfigSettings);
    }

    /**
     * Sets the MPConfig content for the microprofile-config.properties file
     *
     * @param publicKey
     *            - public Key value to add to properties file
     * @param publicKeyLocation
     *            - public key location value to add to the properties file
     * @param issuer
     *            - issuer value to add to the properties file
     * @return - return the microprofile-config.properties file content
     */
    public static String buildMPConfigFileContent(MP12ConfigSettings mpConfigSettings, String header, String cookie, String audience, String algorithm) {
        Log.info(thisClass, "",
                 "mp.jwt.token.header=" + header + " mp.jwt.token.cookie=" + cookie + " mp.jwt.verify.audiences=" + audience + " mp.jwt.verify.publickey.algorithm=" + algorithm);
        return MPJwt11MPConfigTests.buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), mpConfigSettings.getIssuer())
               + System.lineSeparator() + "mp.jwt.token.header=" + header + System.lineSeparator()
               + "mp.jwt.token.cookie=" + cookie + System.lineSeparator() + "mp.jwt.verify.audiences=" + audience + System.lineSeparator() + "mp.jwt.verify.publickey.algorithm="
               + algorithm;

    }

    /**
     * Copy the master wars (one for META-INF and one for WEB-INF testing) and
     * create new wars that contain updated microprofile-config.properties
     * files. This method creates many wars that will be used later to test both
     * good and bad values within the microprofile-config.properties files.
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings-
     *            a master/default set of mp-config settings (the wars will be
     *            created with specific good or bad values)
     * @throws Exception
     */
    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MP12ConfigSettings mpConfigSettings) throws Exception {

        Log.info(thisClass, "1.2 deployRSServerMPConfigInAppApps", "1.2");

        try {

            // the microprofile-config.properties files will have xxx_<attr>_xxx
            // values that need to be replaced
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getHeader(), mpConfigSettings.getCookie(),
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getHeader(), mpConfigSettings.getCookie(),
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    public static void deployRSServerMPConfigInAppHeaderApps(LibertyServer server, MP12ConfigSettings mpConfigSettings) throws Exception {

        Log.info(thisClass, "1.2 deployRSServerMPConfigInAppHeaderApps", "1.2");

        try {
            // Header and cookie test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, MP12ConfigSettings.DefaultCookieName,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, MP12ConfigSettings.DefaultCookieName,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, "OtherCookieName",
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.COOKIE, "OtherCookieName",
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, "badHeader", MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, "badHeader", mpConfigSettings.getCookie(),
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));

            // audiences test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        "BadAudience", mpConfigSettings.getAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          "BadAudience", mpConfigSettings.getAlgorithm()));

            // algorithm test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_RS256));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_RS256));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_ES256));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MpJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_ES256));
            // let's create an app with mp config properties that over ride the 1.1 and 1.2 alg settings
            MP12ConfigSettings overrideMpConfigSettings = new MP12ConfigSettings(mpConfigSettings.getPublicKeyLocation(), MP12ConfigSettings
                            .getComplexKey(server, MP12ConfigSettings.es256PubKey), mpConfigSettings
                                            .getIssuer(), MpJwt12FatConstants.X509_CERT, mpConfigSettings
                                                            .getHeader(), mpConfigSettings.getCookie(), mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm());
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(overrideMpConfigSettings, MpJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_ES256));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(overrideMpConfigSettings, MpJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MpJwt12FatConstants.SIGALG_ES256));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    public Map<String, String> addToMap(Map<String, String> currentMap, String name, String value) throws Exception {

        if (currentMap == null) {
            currentMap = new HashMap<String, String>();
        }
        currentMap.put(name, value);
        return currentMap;
    }

    public Page invokeUrlWithOtherHeader(WebClient webClient, String url, String location, String name, String value) throws Exception {

        Map<String, String> header = addToMap(null, location, name + " " + value);

        return actions.invokeUrlWithParametersAndHeaders(_testName, webClient, url, null, header);

    }

    /**
     * All of the tests in this class follow the same flow. The differences between them are which builder they use to create a
     * token, the config they use in the resource server
     * and then whether they expect a failure (mainly due to a mis-match between the token and the servers config).
     * We'll put the common steps in this method so we're not duplicating steps/code over and over.
     *
     * @param builtToken
     *            - the token built to reflect the goal of the calling test
     * @param expectations
     *            - the expected behavior that we need to validate
     * @throws Exception
     */
    public void genericConfigTest(LibertyServer rs_server, String builder, String location, String name, Expectations expectations) throws Exception {

        String thisMethod = "genericConfigTest";
        loggingUtils.printMethodName(thisMethod);

        for (TestApps app : setTestAppArray(rs_server)) {
            standard12TestFlow(builder, app.getUrl(), app.getClassName(), location, name, expectations);
        }
    }

    /**
     * This method performs all of the steps needed for each test case. Good
     * flow and bad flow tests all follow the same steps. The good tests
     * expect
     * output from the test app. The bad tests expect bad status and error
     * messages. The actual steps are all the same.
     *
     * @param rootContext
     *            - root context of the app to invoke
     * @param theApp
     *            - the app name to invoke
     * @param className
     *            - the className to validate in the test output (how we check
     *            that we got where we should have gotten)
     * @param expectations
     *            - null when running a good test, the expectations to check in
     *            the case of a bad/negative test
     * @throws Exception
     */

    public void standard12TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, String location, String name) throws Exception {
        standard12TestFlow(builder, server, rootContext, theApp, className, location, name, null);
    }

    public void standard12TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, String location, String name,
                                   Expectations expectations) throws Exception {
        String testUrl = buildAppUrl(server, rootContext, theApp);
        standard12TestFlow(builder, testUrl, className, location, name, expectations);
    }

    public void standard12TestFlow(String builder, String testUrl, String className, String location, String name, Expectations expectations) throws Exception {

        if (builder == null || builder.equals("")) {
            builder = MpJwt12FatConstants.SIGALG_RS256;
        }
        String builtToken = actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, builder);

        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }

        Page response = null;
        if (MpJwt12FatConstants.AUTHORIZATION.equals(location)) {
            if (MpJwt12FatConstants.TOKEN_TYPE_BEARER.equals(name)) {
                response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
            } else {
                response = invokeUrlWithOtherHeader(webClient, testUrl, location, name, builtToken);
            }
        } else {
            if (MpJwt12FatConstants.COOKIE.equals(location)) {
                response = actions.invokeUrlWithCookie(_testName, testUrl, name, builtToken);
            } else {
                // if we haven't requested Authorization, or Cookie, we want to test passing both - but, there can
                // be 2 varieties of both - 1) header has good value and cookie is bad and 2) header is bad and cookie is good
                // Headers and cookies are added as additional headers, so create a map of "headers" and pass that on the request of the url.
                if (BothHeaderGood.equals(location)) {
                    Map<String, String> headers = addToMap(null, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER + " " + builtToken);
                    headers = addToMap(headers, MpJwt12FatConstants.COOKIE, name + "=badTokenString");
                    response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, testUrl, null, headers);
                } else {
                    if (BothCookieGood.equals(location)) {
                        Map<String, String> headers = addToMap(null, MpJwt12FatConstants.AUTHORIZATION, MpJwt12FatConstants.TOKEN_TYPE_BEARER + " " + "badTokenString");
                        headers = addToMap(headers, MpJwt12FatConstants.COOKIE, name + "=" + builtToken);
                        response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, testUrl, null, headers);
                    } else {
                        throw new Exception("Test code does not understand request");
                    }
                }
            }
        }

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Set expectations for tests that have bad Header values
     *
     * @return Expectations
     * @throws Exception
     */
    public Expectations setBadHeaderValueExpectations(LibertyServer server, String testUrl, String className) throws Exception {

        Expectations expectations = setGoodAppExpectations(testUrl, className);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5528W_BAD_HEADER_VALUE_IN_MP_CONFIG, "Message.log did not contain an error indicating a problem with the value specified for mp.jwt.token.header in the microprofile-config.properties file."));

        return expectations;

    }

    /**
     * Set expectations for tests that have bad Audiences values
     *
     * @return Expectations
     * @throws Exception
     */
    public Expectations setBadAudiencesExpectations(LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6023E_AUDIENCE_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the audience is NOT valid."));

        return expectations;

    }

//    /**
//     * Set expectations for tests that have bad Audiences values
//     *
//     * @return Expectations
//     * @throws Exception
//     */
//    public Expectations setBadAlgorithmExpectations(LibertyServer server) throws Exception {
//
//        Expectations expectations = new Expectations();
//        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
//
////        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
////        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6023E_AUDIENCE_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the audience is NOT valid."));
//
//        return expectations;
//
//    }

//
//    /**
//     * Set expectations for tests that have bad Decryption key location values
//     *
//     * @return Expectations
//     * @throws Exception
//     */
//    public Expectations setBadDecryptionKeyLocationExpectations(LibertyServer server) throws Exception {
//
//        Expectations expectations = new Expectations();
//        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
//
//        // TODO - fix when we have the correct messages
//        //expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
//        //expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED, "Messagelog did not contain an exception indicating that the issuer is NOT valid."));
//
//        return expectations;
//
//    }

    /**
     * Set expectations for tests that have bad Signature Algorithms
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setBadEncryptExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);

        //TODO - correct failure msgs....
//        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
//        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an exception indicating that the Signature Algorithm is NOT valid."));

        return expectations;

    }

    /**
     * Runtime finds the Authorization header, but the name within the token is not recognized
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setMissingTokenBadNameExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an error indicating a problem processing the request."));

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain and exception indicating that the JWT feature cannot authenticate the request."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an exception indicating that the Signature Algorithm is NOT valid."));

        return expectations;

    }

    /**
     * Set expectations for tests that are missing the token
     *
     * @param server - server whose logs will be searched
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setMissingTokenExpectations(LibertyServer server) throws Exception {

        Expectations expectations = badAppExpectations(MpJwt12FatConstants.UNAUTHORIZED_MESSAGE);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5522E_MPJWT_TOKEN_NOT_FOUND, "Messagelog did not contain an error indicating that the JWT token was not found."));

        return expectations;

    }
}
