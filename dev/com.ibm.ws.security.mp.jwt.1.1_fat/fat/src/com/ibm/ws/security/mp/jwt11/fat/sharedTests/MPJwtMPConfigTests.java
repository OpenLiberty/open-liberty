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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonIOUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.utils.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt11.fat.utils.MPConfigSettings;
import com.ibm.ws.security.mp.jwt11.fat.utils.MpJwtMessageConstants;

import componenttest.annotation.MinimumJavaLevel;
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

@MinimumJavaLevel(javaLevel = 8)
@RunWith(FATRunner.class)
public class MPJwtMPConfigTests extends CommonMpJwtFat {

    public static Class<?> thisClass = MPJwtMPConfigTests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat.builder")
    public static LibertyServer jwtBuilderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    protected final TestValidationUtils validationUtils = new TestValidationUtils();

//    public static class MPConfigSettings {
//
//        String publicKeyLocation = null;
//        String publicKey = ComplexPublicKey;
//        String issuer = null;
//        String certType = MpJwtFatConstants.X509_CERT;
//
//        public MPConfigSettings() {
//        }
//
//        public MPConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType) {
//
//            publicKeyLocation = inPublicKeyLocation;
//            publicKey = inPublicKey;
//            issuer = inIssuer;
//            certType = inCertType;
//        }
//    }

//    protected static String cert_type = MpJwtFatConstants.X509_CERT;

    public static enum MPConfigLocation {
        IN_APP, SYSTEM_VAR, ENV_VAR
    };

//    // if you recreate the rsa_cert.pem file, please update the PublicKey value saved here.
//    protected final static String SimplePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl66sYoc5HXGHnrtGCMZ6G8zLHnAl+xhP7bOQMmqwEqtwI+yJJG3asvLhJQiizP0cMA317ekJE6VAJ2DBT8g2npqJSXK/IuVQokM4CNp0IIbD66qgVLJ4DS1jzf6GFciJAiGOHztl8ICd7/q0EvuYcwd/sUjTrwRpkLcEH2Z/FE2sh4a82UwyxZkX3ghbZ/3MFtsMjzw0cSqKPUrgGCr4ZcAWZeoye81cLybY5Vb/5/eZfkeBIDwSSssqJRmsNBFs23c+RAymtKaP7wsQw5ATEeI7pe0kiWLpqH4wtsDVyN1C/p+vZJSia0OQJ/z89b5OkmpFC6qGBGxC7eOk71wCJwIDAQAB";
//    protected final static String ComplexPublicKey = "-----BEGIN PUBLIC KEY-----" + SimplePublicKey + "-----END PUBLIC KEY-----";
//    protected final static String PemFile = "rsa_key.pem";
//    protected final static String ComplexPemFile = "rsa_key_withCert.pem";
//    protected final static String BadPemFile = "bad_key.pem";
//    protected final static String jwksUri = "\"http://localhost:${bvt.prop.security_2_HTTP_default}/jwt/ibm/api/defaultJWT/jwk\"";
//    protected final static String PublicKeyNotSet = "";
//    protected final static String PublicKeyLocationNotSet = "";
//    protected final static String IssuerNotSet = null;

    @SuppressWarnings("serial")
    List<String> reconfigMsgs = new ArrayList<String>() {
        {
            add("CWWKS5603E");
            add("CWWKZ0002E");
        }
    };

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
    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile, MPConfigSettings mpConfigSettings,
                                                        MPConfigLocation mpConfigLocation) throws Exception {

        setupBootstrapPropertiesForMPTests(server, mpConfigSettings);

        setupMPConfig(server, mpConfigSettings, mpConfigLocation);

        startRSServerForMPTests(server, configFile);

    }

    protected static void setupBootstrapPropertiesForMPTests(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        if (mpConfigSettings.getCertType().equals(MpJwtFatConstants.JWK_CERT)) {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", MPConfigSettings.jwksUri);
        } else {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        }

    }

    protected static void startRSServerForMPTests(LibertyServer server, String configFile) throws Exception {
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile, commonStartMsgs);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
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
    public static void setupMPConfig(LibertyServer theServer, MPConfigSettings mpConfigSettings, MPConfigLocation mpConfigLocation) throws Exception {

        // build fully resolved issuer
        mpConfigSettings.setIssuer(buildIssuer(mpConfigSettings.getIssuer()));
        // remove variables/dollar signs/... (need the server defined before we can do this...
        mpConfigSettings.setPublicKeyLocation(resolvedJwksUri(mpConfigSettings.getPublicKeyLocation()));

        Log.info(thisClass, "setupMPConfig", "mpConfigLocation is set to: " + mpConfigLocation.toString());
        switch (mpConfigLocation) {
            case SYSTEM_VAR:
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
    public static void setAlternateMP_ConfigProperties_InJvmOptions(LibertyServer theServer, MPConfigSettings mpConfigSettings) throws Exception {

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
    public static void updateJvmOptionsFile(String jvmOptionsFile, MPConfigSettings mpConfigSettings) throws Exception {

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
    public static void setAlternateMP_ConfigProperties_envVars(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

        setAlternateMP_ConfigProperties_envVars(null, server, mpConfigSettings);

    }

    public static void setAlternateMP_ConfigProperties_envVars(HashMap<String, String> envVars, LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

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
    protected static String buildMPConfigFileContent(String publicKey, String publicKeyLocation, String issuer) {
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
    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

        try {
            String fixedJwksUri = resolvedJwksUri(MPConfigSettings.jwksUri);
            String fileLoc = server.getServerRoot() + "/";

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
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getPublicKey(), mpConfigSettings.getPublicKeyLocation(), "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PublicKeyLocationNotSet, "badIssuer"));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PublicKeyLocationNotSet, "badIssuer"));

            // publicKey (NOT keyLocation)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.ComplexPublicKey, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.SimplePublicKey, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.ComplexPublicKey, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.SimplePublicKey, MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent("badPublicKey", MPConfigSettings.PublicKeyLocationNotSet, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent("badPublicKey", MPConfigSettings.PublicKeyLocationNotSet, mpConfigSettings.getIssuer()));

            // publicKeyLocation (NOT publicKey)
            // not testing all locations (relative, file, url, jwksuri) with all pem loc types (good pem, complex pem, bad pem)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PemFile, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.ComplexPemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.PemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MPConfigSettings.PemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.ComplexPemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MPConfigSettings.PemFile,
                                                                                          mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.BadPemFile,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + "someKey.pem",
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "badPublicKeyLocation", mpConfigSettings.getIssuer()));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    /**
     * Builds the issuer value based on the test machine name and ports. THe issuer is a list of valid hostname:port,
     * hostname:securePort, ipAddr:port, ipAddr:securePort + "/jwt/defaultJWT"
     *
     * @param issuer - for negative issuer tests, we will already have set the issuer to something like "badIssuer", pass in a value
     *            to override the default value being set.
     * @return - return either the passed in override, or the generated valid for your test system value
     * @throws Exception
     */
    public static String buildIssuer(String issuer) throws Exception {
        if (issuer == null) {
            return SecurityFatHttpUtils.getServerUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerIpUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + SecurityFatHttpUtils.getServerIpSecureUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + "https://localhost:" + jwtBuilderServer.getBvtPort() + "/oidc/endpoint/OidcConfigSample, "
                   + "https://localhost:" + jwtBuilderServer.getBvtSecurePort() + "/oidc/endpoint/OidcConfigSample";

        } else {
            return issuer;
        }

    }

    /**
     * The jwksuri value in the server.xml contains variables for the server and port. The server can not resolve these
     * variables
     * in the microprofile.properties file, in system properties or in env variables. We need to use the fully resolved string
     * there. This method generates the resolved string.
     *
     * @param rawJwksUri - the raw value if we need to set a value, "" if we don't want to put a value into the mp-config
     *
     * @return - return the fully expanded jwksuri (no $variables in it)
     * @throws Exception
     */
    public static String resolvedJwksUri(String rawJwksUri) throws Exception {

        if (rawJwksUri != "" && rawJwksUri.contains("bvt.prop")) {
            return SecurityFatHttpUtils.getServerUrlBase(jwtBuilderServer) + "jwt/ibm/api/defaultJWT/jwk";

        } else {
            return rawJwksUri;
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
    public void standardTestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standardTestFlow(builder, server, rootContext, theApp, className, null);

    }

    public void standardTestFlow(LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standardTestFlow(server, rootContext, theApp, className, null);

    }

    public void standardTestFlow(LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {
        standardTestFlow("defaultJWT", server, rootContext, theApp, className, expectations);

    }

    public void standardTestFlow(String builder, LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {

        String builtToken = actions.getJwtFromTokenEndpoint(_testName, builder, SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer), defaultUser, defaultPassword);

        String testUrl = buildAppUrl(server, rootContext, theApp);

        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }
        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Set expectations for tests that have bad keyName/publicKey or jwksuri/keyLocations
     *
     * @param server - the server whose log we'll need to check for failure messages
     * @param failureCause - the cause of the failure (failures that occur validating each type of cert x509/jwk)
     * @return - an expectation object with a variety of errors to check for
     * @throws Exception
     */
    public Expectations setBadCertExpectations(LibertyServer server, String failureCause) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messages.log did not contain an error indicating a problem authenticating the request the provided token."));
        switch (failureCause) {
            case MpJwtFatConstants.X509_CERT:
                String invalidKeyName = "badKeyName";
                expectations.addExpectation(new ServerMessageExpectation(server, invalidKeyName
                                                                                 + ".*is not present in the KeyStore as a certificate entry", "Messages.log did not contain a message stating that the alias was NOT found in the keystore."));
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".*"
                                                                                 + invalidKeyName, "Messages.log did not indicate that the signing key is NOT available."));
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*"
                                                                                 + invalidKeyName, "Message log did not indicate that the signing key is NOT available."));
                break;
            case MpJwtFatConstants.JWK_CERT:
                expectations.addExpectation(new ServerMessageExpectation(server, MpJwtMessageConstants.CWWKS6029E_SIGNING_KEY_CANNOT_BE_FOUND, "Messages.log did not contain an error indicating that a signing key could not be found."));
                break;
            default:
                break;
        }

        return expectations;

    }

    /**
     * Setup expectations for a valid/good path (we get to invoke the app). Expectations set status codes to check and content to
     * validate in the response from the app.
     *
     * @param step - the step to check after
     * @param testUrl - the app that should have benn invoked
     * @param theClass - the class that should be invoked
     * @return - the formatted expectations - for a test with a good result
     * @throws Exception
     */
    public Expectations setGoodAppExpectations(String testUrl, String theClass) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(testUrl));
        expectations.addExpectation(new ResponseFullExpectation(MpJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));

        return expectations;
    }

    /**
     * The test framework randomly chooses between x509 and jwks. We need to set the config based on what the
     * framework has chosen for this spcific run.
     *
     * @param certType - the cert type being tested - indicates which config to load
     * @throws Exception
     */
    public void badTokenVerificationReconfig(LibertyServer server, String certType) throws Exception {

        if (MpJwtFatConstants.X509_CERT.equals(MPConfigSettings.cert_type)) {
            server.reconfigureServer("rs_server_AltConfigInApp_badServerXmlKeyName.xml", _testName);
        } else {
            server.reconfigureServer("rs_server_AltConfigInApp_badServerXmlJwksUri.xml", _testName);
        }

    }

}
