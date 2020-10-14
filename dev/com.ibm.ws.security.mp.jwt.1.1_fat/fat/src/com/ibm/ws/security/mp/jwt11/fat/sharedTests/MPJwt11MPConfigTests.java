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
package com.ibm.ws.security.mp.jwt11.fat.sharedTests;

import java.util.HashMap;

import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is a common test class that will test the alternate placement of mp jwt config settings. They can be found in multiple
 * places within the application, or can be set as system or environment variables.
 * The extending test classes will do one of the following:
 * - request use of an app that has placement of MPConfig in "resources/META-INF/microprofile-config.properties"
 * - request use of an app that has placement of MPConfig in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 * - request use of a server that has placement of MPConfig in jvm.options
 * - setup MPConfig as environment variables
 *
 **/

@RunWith(FATRunner.class)
public class MPJwt11MPConfigTests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwt11MPConfigTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    /******************************************** helper methods **************************************/

    /**
     * Setup apps, System properties or environment variables needed for the tests.
     * Return a list of apps that we need to wait for at startup.
     *
     * @param theServer
     *            - the resource server
     * @param mpConfigSettings
     *            - the settings values to use
     * @param mpConfigLocation
     *            - where to put the settings (system properties, env vars, or in apps)
     * @return
     * @throws Exception
     */

    // Need to pass in the resource server reference as we can be using one of several
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, MP11ConfigSettings mpConfigSettings,
                                                        MPConfigLocation mpConfigLocation) throws Exception {

        setupBootstrapPropertiesForMPTests(server, MP11ConfigSettings.jwksUri, mpConfigSettings.getCertType().equals(MpJwtFatConstants.JWK_CERT));

        setupMPConfig(server, mpConfigSettings, mpConfigLocation);

        startRSServerForMPTests(server, configFile);

    }

    /**
     * Setup the system properties, environment variables or microprofile-config.properties in the test apps as appropriate.
     * When testing with System properties, we will update the jvm.options file and install an app with no microprofile-config.properties file
     * When testing with env variables, we will set those in the server environment and install an app with no microprofile-config.properties file
     * When testing with microprofile-config.properties in the app, we will create multiple apps with a variety of settings within the
     * microprofile-config.properties file within the app. We'll also create apps with the microprofile-config.properties in the META-INF directory
     * and the WEB-INF/classes/META-INF directory.
     *
     * @param theServer - the server to install the apps on and set the system properties or env variables for
     * @param mpConfigSettings - The microprofile settings to put into the various locations
     * @param mpConfigLocation - where this test instance would like the MPConfig settings (system properties, environment variables, or microprofile-config.properties in apps)
     * @throws Exception
     */
    public static void setupMPConfig(LibertyServer theServer, MP11ConfigSettings mpConfigSettings, MPConfigLocation mpConfigLocation) throws Exception {

        // build fully resolved issuer
        mpConfigSettings.setIssuer(buildIssuer(jwtBuilderServer, mpConfigSettings.getIssuer()));
        // remove variables/dollar signs/... (need the server defined before we can do this...
        mpConfigSettings.setPublicKeyLocation(resolvedJwksUri(jwtBuilderServer, mpConfigSettings.getPublicKeyLocation()));

        Log.info(thisClass, "setupMPConfig", "mpConfigLocation is set to: " + mpConfigLocation.toString());
        switch (mpConfigLocation) {
            case SYSTEM_PROP:
                // if we're testing system properties, we'll need to update the values in the jvm.options file (if the file exists, update it)
                setAlternateMP_ConfigProperties_InJvmOptions(theServer, mpConfigSettings);
                setupUtils.deployRSServerNoMPConfigInAppApp(theServer);
                break;
            case ENV_VAR:
                // if we're testing env variables, we'll need to set environment variables
                setAlternateMP_ConfigProperties_envVars(theServer, mpConfigSettings);
                setupUtils.deployRSServerNoMPConfigInAppApp(theServer);
                break;
            case IN_APP:
                deployRSServerMPConfigInAppApps(theServer, mpConfigSettings);
                break;
            default:
                throw new Exception("Invalid MP Config location passed to setupMPConfig - tests do NOT understand " + mpConfigLocation);
        }

    }

    /**
     * Sets system properties before the server is started
     *
     * @param theServer - the resource server
     * @param mpConfigSettings - the mp-config settings values
     * @throws Exception
     */
    public static void setAlternateMP_ConfigProperties_InJvmOptions(LibertyServer theServer, MP11ConfigSettings mpConfigSettings) throws Exception {

        String jvmFile = theServer.getServerRoot() + "/jvm.options";
        updateJvmOptionsFile(jvmFile, mpConfigSettings);

    }

    /**
     * Update the values in the jvm.options file of the server
     *
     * @param jvmOptionsFile - the file to update
     * @param publicKey - the publicKey value to update
     * @param keyLoc - the keyLocation value to update
     * @param issuer - the issuer value to update
     */
    public static void updateJvmOptionsFile(String jvmOptionsFile, MP11ConfigSettings mpConfigSettings) throws Exception {

        HashMap<String, String> optionMap = new HashMap<String, String>();
        optionMap.put("xxx_publicKeyName_xxx", mpConfigSettings.getPublicKey());
        optionMap.put("xxx_publicKeyLoc_xxx", mpConfigSettings.getPublicKeyLocation());
        optionMap.put("xxx_issuer_xxx", mpConfigSettings.getIssuer());

        CommonIOUtils cioTools = new CommonIOUtils();
        if (cioTools.replaceStringsInFile(jvmOptionsFile, optionMap)) {
            return;
        }
        throw new Exception("Failure updating the jvm.options file - tests will NOT behave as expected - exiting");

    }

    /**
     * Sets/creates environment variables
     *
     * @param theServer - the resource server
     * @param mpConfigSettings - the mp-config settings values
     * @throws Exception
     */
    public static void setAlternateMP_ConfigProperties_envVars(LibertyServer server, MP11ConfigSettings mpConfigSettings) throws Exception {

        setAlternateMP_ConfigProperties_envVars(null, server, mpConfigSettings);

    }

    public static void setAlternateMP_ConfigProperties_envVars(HashMap<String, String> envVars, LibertyServer server, MP11ConfigSettings mpConfigSettings) throws Exception {

        // some platforms do NOT support env vars with ".", so, we'll use underscores "_" (our runtime allows either)
        String PublicKeyName = "mp_jwt_verify_publickey";
        String LocationName = "mp_jwt_verify_publickey_location";
        String IssuerName = "mp_jwt_verify_issuer";

        if (envVars == null) {
            envVars = new HashMap<String, String>();
        }
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", PublicKeyName + "=" + mpConfigSettings.getPublicKey());
        envVars.put(PublicKeyName, mpConfigSettings.getPublicKey());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", LocationName + "=" + mpConfigSettings.getPublicKeyLocation());
        envVars.put(LocationName, mpConfigSettings.getPublicKeyLocation());
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", IssuerName + "=" + mpConfigSettings.getIssuer());
        addNonNullToHash(envVars, IssuerName, mpConfigSettings.getIssuer());
//        envVars.put(IssuerName, mpConfigSettings.getIssuer());

        server.setAdditionalSystemProperties(envVars);
    }

    public static void addNonNullToHash(HashMap<String, String> theMap, String name, String value) throws Exception {

        if (name != null && value != null) {
            theMap.put(name, value);
        }
    }

    /**
     * Sets the MPConfig content for the microprofile-config.properties file
     *
     * @param publicKey - public Key value to add to properties file
     * @param publicKeyLocation - public key location value to add to the properties file
     * @param issuer - issuer value to add to the properties file
     * @return - return the microprofile-config.properties file content
     */
    public static String buildMPConfigFileContent(String publicKey, String publicKeyLocation, String issuer) {
        Log.info(thisClass, "", "mp.jwt.verify.publickey=" + publicKey + " mp.jwt.verify.publickey.location=" + publicKeyLocation + " mp.jwt.verify.issuer=" + issuer);
        return "mp.jwt.verify.publickey=" + publicKey + System.lineSeparator() + "mp.jwt.verify.publickey.location=" + publicKeyLocation + System.lineSeparator()
               + "mp.jwt.verify.issuer=" + issuer;

    }

    /**
     * Copy the master wars (one for META-INF and one for WEB-INF testing) and create new wars that contain updated
     * microprofile-config.properties files.
     * This method creates many wars that will be used later to test both good and bad values within the
     * microprofile-config.properties files.
     *
     * @param theServer - the resource server
     * @param mpConfigSettings- a master/default set of mp-config settings (the wars will be created with specific good or bad values)
     * @throws Exception
     */
    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MP11ConfigSettings mpConfigSettings) throws Exception {

        try {
            String fixedJwksUri = resolvedJwksUri(jwtBuilderServer, MP11ConfigSettings.jwksUri);
            String fileLoc = mpConfigSettings.getDefaultKeyFileLoc(server);

            // the microprofile-config.properties files will have xxx_<attr>_xxx values that need to be replaced
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(),
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(),
                                                                                          mpConfigSettings.getIssuer()));
            // apps with some "bad" and some "good" values do need to be updated

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(),
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(),
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          "badIssuer"));

            // publicKey (NOT keyLocation)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.ComplexPublicKey, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.SimplePublicKey, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.ComplexPublicKey, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.SimplePublicKey, MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent("badPublicKey", MP11ConfigSettings.PublicKeyLocationNotSet, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent("badPublicKey", MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            // publicKeyLocation (NOT publicKey)
            // not testing all locations (relative, file, url, jwksuri) with all pem loc types (good pem, complex pem, bad pem)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.ComplexPemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.PemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MP11ConfigSettings.PemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.ComplexPemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MP11ConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.BadPemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + "someKey.pem",
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "badPublicKeyLocation",
                                                                                          mpConfigSettings.getIssuer()));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    /**
     * This method performs all of the steps needed for each test case. Good flow and bad flow tests all follow the same
     * steps. The good tests expect output from the test app. The bad tests expect bad status and error messages. The
     * actual steps are all the same.
     *
     * @param rootContext - root context of the app to invoke
     * @param theApp - the app name to invoke
     * @param className - the className to validate in the test output (how we check that we got where we should have gotten)
     * @param expectations - null when running a good test, the expectations to check in the case of a bad/negative test
     * @throws Exception
     */
    public void standard11TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standard11TestFlow(builder, server, rootContext, theApp, className, null);

    }

    public void standard11TestFlow(LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standard11TestFlow(server, rootContext, theApp, className, null);

    }

    public void standard11TestFlow(LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {
        standard11TestFlow("defaultJWT", server, rootContext, theApp, className, expectations);

    }

    public void standard11TestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {

        Log.info(thisClass, "standardTestFlow", "builderId: " + builder);
        String builtToken = actions.getJwtFromTokenEndpoint(_testName, builder, SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer), defaultUser, defaultPassword);

        String testUrl = buildAppUrl(server, rootContext, theApp);

        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }
        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, expectations);

    }

}
