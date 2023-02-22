/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.mp.jwt.sharedTests;

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
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt21FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP21ConfigSettings;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MPJwtAppSetupUtils;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MpJwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;

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

@RunWith(FATRunner.class)
public class MPJwt21MPConfigTests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwt21MPConfigTests.class;

    @Server("com.ibm.ws.security.mp.jwt.2.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    protected final static MPJwtAppSetupUtils setupUtils = new MPJwtAppSetupUtils();

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
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, MP21ConfigSettings mpConfigSettings,
                                                        MPConfigLocation mpConfigLocation) throws Exception {

        setupBootstrapPropertiesForMPTests(server, MP21ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt21FatConstants.JWK_CERT));

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

    public static void setupMPConfig(LibertyServer theServer, MP21ConfigSettings mpConfigSettings, MPConfigLocation mpConfigLocation) throws Exception {

        // build fully resolved issuer
        mpConfigSettings.setIssuer(buildIssuer(jwtBuilderServer, mpConfigSettings.getIssuer()));
        // remove variables/dollar signs/... (need the server defined before we
        // can do this...
        mpConfigSettings.setPublicKeyLocation(resolvedJwksUri(jwtBuilderServer, mpConfigSettings.getPublicKeyLocation()));

        Log.info(thisClass, "setupMP21Config", "mpConfigLocation is set to: " + mpConfigLocation.toString());
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
                throw new Exception("Invalid MP Config location passed to setupMP21Config - tests do NOT understand " + mpConfigLocation);
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
    public static void setAlternateMP_ConfigProperties_InJvmOptions(LibertyServer theServer, MP21ConfigSettings mpConfigSettings) throws Exception {

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
    static void updateJvmOptionsFile(String jvmOptionsFile, MP21ConfigSettings mpConfigSettings) throws Exception {

        MPJwt12MPConfigTests.updateJvmOptionsFile(jvmOptionsFile, mpConfigSettings);

        HashMap<String, String> optionMap = new HashMap<String, String>();
        optionMap.put("xxx_tokenAge_xxx", Integer.toString(mpConfigSettings.getTokenAge()));
        optionMap.put("xxx_clockSkew_xxx", Integer.toString(mpConfigSettings.getClockSkew()));
        optionMap.put("xxx_decryptKeyAlg_xxx", mpConfigSettings.getKeyManagementKeyAlgorithm());

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
    public static void setAlternateMP_ConfigProperties_envVars(LibertyServer server, MP21ConfigSettings mpConfigSettings) throws Exception {
        setAlternateMP_ConfigProperties_envVars(new HashMap<String, String>(), server, mpConfigSettings);
    }

    public static void setAlternateMP_ConfigProperties_envVars(HashMap<String, String> envVars, LibertyServer server, MP21ConfigSettings mpConfigSettings) throws Exception {

        String tokenAge = Integer.toString(mpConfigSettings.getTokenAge());
        String clockSkew = Integer.toString(mpConfigSettings.getClockSkew());
        // some platforms do NOT support env vars with ".", so, we'll use
        // underscores "_" (our runtime allows either)
//        String TokenAgeName = "mp_jwt_verify_token_age";
        String tokenAgeName = MPJwt21FatConstants.TOKEN_AGE_KEY.replace(".", "_");
        String clockSkewName = MPJwt21FatConstants.CLOCK_SKEW_KEY.replace(".", "_");
        String keyMgmtKeyAlias = MPJwt21FatConstants.DECRYPT_KEY_ALG_KEY.replace(".", "_");

        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", tokenAgeName + "=" + tokenAge);
        envVars.put(tokenAgeName, tokenAge);
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", clockSkewName + "=" + clockSkew);
        envVars.put(clockSkewName, clockSkew);
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", keyMgmtKeyAlias + "=" + mpConfigSettings.getKeyManagementKeyAlgorithm());
        envVars.put(keyMgmtKeyAlias, mpConfigSettings.getKeyManagementKeyAlgorithm());

        MPJwt12MPConfigTests.setAlternateMP_ConfigProperties_envVars(envVars, server, mpConfigSettings);
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

    public static String buildMPConfigFileContent(MP21ConfigSettings mpConfigSettings, int rawTokenAge, int rawClockSkew, String decryptAlg) {

        return buildMPConfigFileContent(mpConfigSettings, rawTokenAge, rawClockSkew, decryptAlg, mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc());
    }

    public static String buildMPConfigFileContent(MP21ConfigSettings mpConfigSettings, int rawTokenAge, int rawClockSkew, String decryptAlg, String alg, String decryptKeyLoc) {

        String tokenAge = Integer.toString(rawTokenAge);
        String clockSkew = Integer.toString(rawClockSkew);

        Log.info(thisClass, "",
                 MPJwt21FatConstants.TOKEN_AGE_KEY + "=" + tokenAge + " " + MPJwt21FatConstants.CLOCK_SKEW_KEY + "=" + clockSkew + " " + MPJwt21FatConstants.DECRYPT_KEY_ALG_KEY
                                + "=" + decryptAlg);
        return MPJwt11MPConfigTests.buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), mpConfigSettings.getIssuer())
               + System.lineSeparator() + "mp.jwt.token.header=" + mpConfigSettings.getHeader() + System.lineSeparator()
               + "mp.jwt.token.cookie=" + mpConfigSettings.getCookie() + System.lineSeparator() + "mp.jwt.verify.audiences=" + mpConfigSettings.getAudience()
               + System.lineSeparator() + "mp.jwt.verify.publickey.algorithm="
               + alg + System.lineSeparator() + "mp.jwt.decrypt.key.location=" + decryptKeyLoc + System.lineSeparator()
               + MPJwt21FatConstants.TOKEN_AGE_KEY + "=" + tokenAge + System.lineSeparator()
               + MPJwt21FatConstants.CLOCK_SKEW_KEY + "=" + clockSkew + System.lineSeparator()
               + MPJwt21FatConstants.DECRYPT_KEY_ALG_KEY + "=" + decryptAlg + System.lineSeparator();

    }

    /**
     * Copy the primary wars (one for META-INF and one for WEB-INF testing) and
     * create new wars that contain updated microprofile-config.properties
     * files. This method creates many wars that will be used later to test both
     * good and bad values within the microprofile-config.properties files.
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings-
     *            a primary/default set of mp-config settings (the wars will be
     *            created with specific good or bad values)
     * @throws Exception
     */
    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MP21ConfigSettings mpConfigSettings) throws Exception {

        Log.info(thisClass, "2.1 deployRSServerMPConfigInAppApps", "2.1");

        try {

            // the microprofile-config.properties files will have xxx_<attr>_xxx
            // values that need to be replaced
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getTokenAge(), mpConfigSettings.getClockSkew(),
                                                                                        mpConfigSettings.getKeyManagementKeyAlgorithm()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getTokenAge(), mpConfigSettings.getClockSkew(),
                                                                                          mpConfigSettings.getKeyManagementKeyAlgorithm()));

        } catch (Exception e) {
            Log.info(thisClass, "deployRSServerMPConfigInAppApps", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    public static void deployRSServerMPConfigInAppVariationApps(LibertyServer server, MP21ConfigSettings mpConfigSettings) throws Exception {

        Log.info(thisClass, "1.2 deployRSServerMPConfigInAppHeaderApps", "1.2");
        String fileLoc = JwtKeyTools.getDefaultKeyFileLoc(server);

        try {
            // TokenAge and ClockSkew test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.DEFAULT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.DEFAULT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.LONG_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, 10000, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.LONG_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, 10000, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            // short token age needs a short clock skew also since the 2 will be used together to determine if the token is too old
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, 1, 1,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.SHORT_TOKEN_AGE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, 1, 1,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.DEFAULT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, 1, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.DEFAULT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, 1, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.LONG_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, 1, 10000,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.LONG_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, 1, 10000,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, 1, 1,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.SHORT_CLOCK_SKEW_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, 1, 1,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg));

            // KeyMgmtKeyAlg test apps
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.DEFAULT_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MP21ConfigSettings.DefaultKeyMgmtKeyAlg, MPJwt12FatConstants.SIGALG_RS256,
                                                                                        fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.DEFAULT_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MP21ConfigSettings.DefaultKeyMgmtKeyAlg, MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.MATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MPJwt12FatConstants.DEFAULT_KEY_MGMT_KEY_ALG, MPJwt12FatConstants.SIGALG_RS256,
                                                                                        fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.MATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MPJwt12FatConstants.DEFAULT_KEY_MGMT_KEY_ALG, MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                        MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256, MPJwt12FatConstants.SIGALG_RS256,
                                                                                        fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.MISMATCH_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                          MPJwt12FatConstants.KEY_MGMT_KEY_ALG_256, MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                        "SomeString", MPJwt12FatConstants.SIGALG_RS256,
                                                                                        fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt21FatConstants.INVALID_KEYMGMTKEYALG_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MP21ConfigSettings.DefaultTokenAge, MP21ConfigSettings.DefaultClockSkew,
                                                                                          "SomeString", MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    // something similar may be needed for additional 2.1 attrs
//    public static MP21ConfigSettings overrideSignerInMP21ConfigSettings(MP21ConfigSettings mpConfigSettings, LibertyServer server, String sigAlg) throws Exception {
//
//        Log.info(thisClass, "overrideSignerInMP21ConfigSettings", "sigAlg: " + sigAlg);
//        return new MP21ConfigSettings(mpConfigSettings.getPublicKeyLocation(), JwtKeyTools
//                        .getComplexPublicKeyForSigAlg(server, sigAlg), mpConfigSettings.getIssuer(), MPJwt21FatConstants.X509_CERT, mpConfigSettings
//                                        .getHeader(), mpConfigSettings
//                                                        .getCookie(), mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc());
//    }

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
    public void genericConfigTest(LibertyServer rs_server, String builder, int sleepTime, Expectations expectations) throws Exception {

        String thisMethod = "genericConfigTest";
        loggingUtils.printMethodName(thisMethod);

        for (TestApps app : setTestAppArray(rs_server)) {
            standard21TestFlow(builder, app.getUrl(), app.getClassName(), sleepTime, expectations);
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

    public void standard21TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standard21TestFlow(builder, server, rootContext, theApp, className, null);
    }

    public void standard21TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {
        String testUrl = buildAppUrl(server, rootContext, theApp);
        standard21TestFlow(builder, testUrl, className, 0, expectations);
    }

    public void standard21TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, int sleepTime,
                                   Expectations expectations) throws Exception {
        String testUrl = buildAppUrl(server, rootContext, theApp);
        standard21TestFlow(builder, testUrl, className, sleepTime, expectations);
    }

    public void standard21TestFlow(String builder, String testUrl, String className, int sleepTime, Expectations expectations) throws Exception {

        String builtToken = getToken(builder);

        Thread.sleep(sleepTime * 1000);

        useToken(builtToken, testUrl, className, expectations);
    }

    public void useToken(String builtToken, String testUrl, String className, Expectations expectations) throws Exception {

        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }

        Page response = null;

        response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);

        validationUtils.validateResult(response, expectations);
        actions.destroyWebClient(webClient);

    }

    public String getToken(String builder) throws Exception {
        if (builder == null || builder.equals("")) {
            builder = MPJwt21FatConstants.SIGALG_RS256;
        }
        return actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, builder);

    }

    /**
     * Set expectations for tests that have bad Audiences values
     *
     * @return Expectations
     * @throws Exception
     */
    public Expectations setShortTokenAgeExpectations(LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6067E_ELAPSED_TOKEN_AGE, "Message log did not contain an error indicating that too much time had elapsed to use the token."));

        return expectations;

    }

    public Expectations setShortTokenAgeExpectationsVerbose(LibertyServer server) throws Exception {

        Expectations expectations = setShortTokenAgeExpectations(server);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem creating a JWT."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Message log did not contain an error indicating that the token could not be processed."));

        return expectations;

    }

    public Expectations setEncryptAlgMismatchExpectations(LibertyServer server, String alg) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6069E_MISMATCH_ENCRYP_ALG + ".*" + JwtConstants.DEFAULT_KEY_MGMT_KEY_ALG + ".*"
                                                                         + alg, "Message log did not contain an error indicating that the JWE could not be validated because it was not encrypted using the "
                                                                                + alg + " algorithm."));

        return expectations;

    }

    public Expectations setOnlyJWSAcceptedExpectations(LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Message log did not contain an error indicating a problem authenticating the request using the provided token."));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6063E_JWS_REQUIRED_BUT_TOKEN_NOT_JWS, "Message log did not contain an error indicating that only a JWS token is accepted (not a JWE)"));

        return expectations;

    }

}
