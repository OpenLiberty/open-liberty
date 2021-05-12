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
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.mp.jwt.MPJwt12FatConstants;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MP12ConfigSettings;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MPJwtAppSetupUtils;
import com.ibm.ws.security.fat.common.mp.jwt.utils.MpJwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;

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

@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwt12MPConfigTests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwt12MPConfigTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.2.fat.builder")
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
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, MP12ConfigSettings mpConfigSettings,
                                                        MPConfigLocation mpConfigLocation) throws Exception {

        setupBootstrapPropertiesForMPTests(server, MP12ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MPJwt12FatConstants.JWK_CERT));

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
        optionMap.put("xxx_decryptKeyLoc_xxx", mpConfigSettings.getDecryptKeyLoc());
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
        String DecryptKeyName = "mp_jwt_decrypt_key_location";

        HashMap<String, String> envVars = new HashMap<String, String>();
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", HeaderName + "=" + mpConfigSettings.getHeader());
        envVars.put(HeaderName, mpConfigSettings.getHeader());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", CookieName + "=" + mpConfigSettings.getCookie());
        envVars.put(CookieName, mpConfigSettings.getCookie());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", AudienceName + "=" + mpConfigSettings.getAudience());
        envVars.put(AudienceName, mpConfigSettings.getAudience());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", AlgorithmName + "=" + mpConfigSettings.getAlgorithm());
        envVars.put(AlgorithmName, mpConfigSettings.getAlgorithm());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", DecryptKeyName + "=" + mpConfigSettings.getDecryptKeyLoc());
        envVars.put(DecryptKeyName, mpConfigSettings.getDecryptKeyLoc());
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
    public static String buildMPConfigFileContent(MP12ConfigSettings mpConfigSettings, String header, String cookie, String audience, String algorithm, String decryptKeyLoc) {
        Log.info(thisClass, "",
                 "mp.jwt.token.header=" + header + " mp.jwt.token.cookie=" + cookie + " mp.jwt.verify.audiences=" + audience + " mp.jwt.verify.publickey.algorithm=" + algorithm
                                + " mp.jwt.decrypt.key.location=" + decryptKeyLoc);
        return MPJwt11MPConfigTests.buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), mpConfigSettings.getIssuer())
               + System.lineSeparator() + "mp.jwt.token.header=" + header + System.lineSeparator()
               + "mp.jwt.token.cookie=" + cookie + System.lineSeparator() + "mp.jwt.verify.audiences=" + audience + System.lineSeparator() + "mp.jwt.verify.publickey.algorithm="
               + algorithm + System.lineSeparator() + "mp.jwt.decrypt.key.location=" + decryptKeyLoc;

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
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getHeader(), mpConfigSettings.getCookie(),
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, mpConfigSettings.getHeader(), mpConfigSettings.getCookie(),
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    public static void deployRSServerMPConfigInAppHeaderApps(LibertyServer server, MP12ConfigSettings mpConfigSettings) throws Exception {

        Log.info(thisClass, "1.2 deployRSServerMPConfigInAppHeaderApps", "1.2");
        String fileLoc = JwtKeyTools.getDefaultKeyFileLoc(server);

        try {
            // Header and cookie test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_HEADER_AUTHORIZATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, MP12ConfigSettings.DefaultCookieName,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, MP12ConfigSettings.DefaultCookieName,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, "OtherCookieName",
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_HEADER_COOKIE_IN_CONFIG_WITH_OTHER_COOKIENAME_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.COOKIE, "OtherCookieName",
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, "badHeader", MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.BAD_HEADER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, "badHeader", mpConfigSettings.getCookie(),
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));

            // audiences test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(),
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        "BadAudience", mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.BAD_AUDIENCES_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          "BadAudience", mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc()));

            // algorithm test setup
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_ES256,
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.BAD_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION, MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_ES256,
                                                                                          mpConfigSettings.getDecryptKeyLoc()));
            // let's create an app with mp config properties that over ride the 1.1 and 1.2 alg settings
            MP12ConfigSettings overrideMpConfigSettings = new MP12ConfigSettings(mpConfigSettings.getPublicKeyLocation(), JwtKeyTools
                            .getComplexPublicKeyForSigAlg(server, MPJwt12FatConstants.SIGALG_ES256), mpConfigSettings.getIssuer(), MPJwt12FatConstants.X509_CERT, mpConfigSettings
                                            .getHeader(), mpConfigSettings
                                                            .getCookie(), mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc());
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(overrideMpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_ES256,
                                                                                        mpConfigSettings.getDecryptKeyLoc()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_KEY_AND_ALGORITHM_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(overrideMpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_ES256,
                                                                                          mpConfigSettings.getDecryptKeyLoc()));

            // encrypt apps
            // the mp.jwt.decrypt.key.location can be set in the mp config props in meta-inf or under web-inf and we'll test each of those
            // the format of the location can be the relative file location, fully qualified file location as well as the url formatted file location
            // we'll test these different forms, but cover that over the different algs and locations
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_URL_DECRYPT_KEY_RS384_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(overrideSignerInMP12ConfigSettings(mpConfigSettings, server,
                                                                                                                           MPJwt12FatConstants.SIGALG_RS384),
                                                                                        MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS384,
                                                                                        "file:///" + fileLoc + JwtKeyTools
                                                                                                        .getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS384)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_RELATIVE_DECRYPT_KEY_RS384_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(overrideSignerInMP12ConfigSettings(mpConfigSettings, server,
                                                                                                                             MPJwt12FatConstants.SIGALG_RS384),
                                                                                          MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS384,
                                                                                          JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS384)));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.GOOD_FILE_DECRYPT_KEY_RS512_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(overrideSignerInMP12ConfigSettings(mpConfigSettings, server,
                                                                                                                           MPJwt12FatConstants.SIGALG_RS512),
                                                                                        MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS512,
                                                                                        fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS512)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.GOOD_URL_DECRYPT_KEY_RS512_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(overrideSignerInMP12ConfigSettings(mpConfigSettings, server,
                                                                                                                             MPJwt12FatConstants.SIGALG_RS512),
                                                                                          MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS512,
                                                                                          "file:///" + fileLoc + JwtKeyTools
                                                                                                          .getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS512)));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_DECRYPT_KEY_ES256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(overrideMpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_ES256)));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.BAD_DECRYPT_KEY_ES256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(overrideMpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                          fileLoc + JwtKeyTools.getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_ES256)));

            // plain text private key
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_PLAINTEXT_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        JwtKeyTools.getKeyFromFile(server, JwtKeyTools
                                                                                                        .getPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256))));

            // jwks uri
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MPJwt12FatConstants.BAD_JWKSURI_DECRYPT_KEY_RS256_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                          MP12ConfigSettings.CookieNotSet,
                                                                                          mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                          resolvedJwksUri(jwtBuilderServer, MP12ConfigSettings.jwksUri)));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_STRING_DECRYPT_KEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        "SomeString"));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_DECRYPT_PUBLIC_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        JwtKeyTools.getPublicKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MPJwt12FatConstants.BAD_SHORT_DECRYPT_KEY_RS256_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings, MPJwt12FatConstants.AUTHORIZATION,
                                                                                        MP12ConfigSettings.CookieNotSet,
                                                                                        mpConfigSettings.getAudience(), MPJwt12FatConstants.SIGALG_RS256,
                                                                                        JwtKeyTools.getShortPrivateKeyFileNameForAlg(MPJwt12FatConstants.SIGALG_RS256)));

//            fileLoc + MP11ConfigSettings.PemFile
            // combinations of file, rel file, url - randonly choose these with meta-inf/web-inf
        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    public static MP12ConfigSettings overrideSignerInMP12ConfigSettings(MP12ConfigSettings mpConfigSettings, LibertyServer server, String sigAlg) throws Exception {

        Log.info(thisClass, "overrideSignerInMP12ConfigSettings", "sigAlg: " + sigAlg);
        return new MP12ConfigSettings(mpConfigSettings.getPublicKeyLocation(), JwtKeyTools
                        .getComplexPublicKeyForSigAlg(server, sigAlg), mpConfigSettings.getIssuer(), MPJwt12FatConstants.X509_CERT, mpConfigSettings
                                        .getHeader(), mpConfigSettings
                                                        .getCookie(), mpConfigSettings.getAudience(), mpConfigSettings.getAlgorithm(), mpConfigSettings.getDecryptKeyLoc());
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

        String builtToken = getToken(builder);
        useToken(builtToken, testUrl, className, location, name, expectations);
    }

    public void useToken(String builtToken, String testUrl, String className, String location, String name, Expectations expectations) throws Exception {
        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }

        Page response = null;
        if (MPJwt12FatConstants.AUTHORIZATION.equals(location)) {
            if (MPJwt12FatConstants.TOKEN_TYPE_BEARER.equals(name)) {
                response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
            } else {
                response = invokeUrlWithOtherHeader(webClient, testUrl, location, name, builtToken);
            }
        } else {
            if (MPJwt12FatConstants.COOKIE.equals(location)) {
                response = actions.invokeUrlWithCookie(_testName, testUrl, name, builtToken);
            } else {
                // if we haven't requested Authorization, or Cookie, we want to test passing both - but, there can
                // be 2 varieties of both - 1) header has good value and cookie is bad and 2) header is bad and cookie is good
                // Headers and cookies are added as additional headers, so create a map of "headers" and pass that on the request of the url.
                if (BothHeaderGood.equals(location)) {
                    Map<String, String> headers = addToMap(null, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER + " " + builtToken);
                    headers = addToMap(headers, MPJwt12FatConstants.COOKIE, name + "=badTokenString");
                    response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, testUrl, null, headers);
                } else {
                    if (BothCookieGood.equals(location)) {
                        Map<String, String> headers = addToMap(null, MPJwt12FatConstants.AUTHORIZATION, MPJwt12FatConstants.TOKEN_TYPE_BEARER + " " + "badTokenString");
                        headers = addToMap(headers, MPJwt12FatConstants.COOKIE, name + "=" + builtToken);
                        response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, testUrl, null, headers);
                    } else {
                        throw new Exception("Test code does not understand request");
                    }
                }
            }
        }

        validationUtils.validateResult(response, expectations);
        actions.destroyWebClient(webClient);

    }

    public String getToken(String builder) throws Exception {
        if (builder == null || builder.equals("")) {
            builder = MPJwt12FatConstants.SIGALG_RS256;
        }
        return actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer, builder);

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

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not set
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptMissingKeyExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, "The key must not be null", "Messagelog did not contain an exception indicating that the keyManagementKeyAlias was missing."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not set
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptPlainTextKeyExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6062E_PLAINTEXT_KEY, "Messagelog did not contain an exception indicating that the mp.jwt.decrypt.key.location contained a plaintext key."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not set
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptMismatchExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, "javax.crypto.AEADBadTagException", "Messagelog did not contain an exception indicating a tag mismatch."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not set
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptMismatchKeyTypeExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, "java.lang.ClassCastException", "Messagelog did not contain an exception indicating a classcast exception due to the key type."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptInvalidKeyTypeExpectations(LibertyServer server, String keyName, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, "alias.*" + keyName
                                                                         + ".*is not present", "Messagelog did not contain an exception indicating that the key could not be found."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptShortKeyTypeExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setAllBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, "InvalidKeyException.*bits or larger MUST be used with the all JOSE RSA algorithms", "Messagelog did not contain an exception indicating that the key could not be found."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setNoEncryptNotJWSTokenExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6063E_JWS_REQUIRED_BUT_TOKEN_NOT_JWS, "Messagelog did not contain an exception indicating that the token is NOT in JWS format."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptNotJWETokenExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6064E_JWE_REQUIRED_BUT_TOKEN_NOT_JWE, "Messagelog did not contain an exception indicating that the token is NOT in JWE format."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptInvalidPayloadExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6065E_JWE_DOES_NOT_CONTAIN_JWS, "Messagelog did not contain an exception indicating that the payload of the JWE was NOT a JWS."));
        return expectations;

    }

    /**
     * Sets expectations to check when the keyManagementKeyAlias is not valid
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setEncryptBadCtyExpectations(LibertyServer server, boolean extraMsgs) throws Exception {
        Expectations expectations = setBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6057E_BAD_CTY_VALUE, "Messagelog did not contain an exception indicating that the cty was not set to [jwt]."));
        return expectations;

    }

    /**
     * Set expectations for tests that have bad Signature Algorithms
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setAllBadEncryptExpectations(LibertyServer server, boolean extraMsgs) throws Exception {

        Expectations expectations = setBadEncryptExpectations(server, extraMsgs);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6056E_CAN_NOT_EXTRACT_JWS, "Messagelog did not contain an exception indicating a problem extracting the JWS from the JWE."));

        return expectations;

    }

    /**
     * Set expectations for tests that have bad Signature Algorithms
     *
     * @param server - server whose logs will be searched
     * @param extraMsgs - the tai drives the code down different paths depending on if it finds config info in server.xml - if it finds config settings, we'll get 2 extra messages.
     * @return Expectations - built expectations
     * @throws Exception
     */
    public Expectations setBadEncryptExpectations(LibertyServer server, boolean extraMsgs) throws Exception {

        Expectations expectations = badAppExpectations(MPJwt12FatConstants.UNAUTHORIZED_MESSAGE);

        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request with the provided token."));
        if (extraMsgs) {
            expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5524E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an exception indicating a problem creating a JWT with the config and token provided."));
            expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6031E_CAN_NOT_PROCESS_TOKEN, "Messagelog did not contain an exception indicating a problem processing the token string."));
        }

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

        Expectations expectations = badAppExpectations(MPJwt12FatConstants.UNAUTHORIZED_MESSAGE);

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

        Expectations expectations = badAppExpectations(MPJwt12FatConstants.UNAUTHORIZED_MESSAGE);
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5522E_MPJWT_TOKEN_NOT_FOUND, "Messagelog did not contain an error indicating that the JWT token was not found."));

        return expectations;

    }

}
