/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
package com.ibm.ws.security.mp.jwt.fat.sharedTests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.CommonMpJwtFat;
import com.ibm.ws.security.mp.jwt.fat.utils.MpJwtMessageConstants;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
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

    @Server("com.ibm.ws.security.mp.jwt.fat.builder")
    public static LibertyServer jwtBuilderServer;

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    public static class MPConfigSettings {

        String publicKeyLocation = null;
        String publicKey = ComplexPublicKey;
        String issuer = null;
        String certType = MpJwtFatConstants.X509_CERT;

        public MPConfigSettings() {}

        public MPConfigSettings(String inPublicKeyLocation, String inPublicKey, String inIssuer, String inCertType) {

            publicKeyLocation = inPublicKeyLocation;
            publicKey = inPublicKey;
            issuer = inIssuer;
            certType = inCertType;
        }
    }

    protected static final String defaultAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

    protected static final boolean ignoreApplicationAuthMethod_true = true;
    protected static final boolean ignoreApplicationAuthMethod_false = false;
    protected static boolean configIsSpecified_true = true;
    protected static boolean configIsSpecified_false = false;

    protected static boolean ignoreApplicationAuthMethod = ignoreApplicationAuthMethod_true;
    protected static boolean configIsSpecified = configIsSpecified_true;

    protected static String cert_type = MpJwtFatConstants.X509_CERT;

    public static enum MPConfigLocation {
        IN_APP, SYSTEM_VAR, ENV_VAR
    };

    // if you recreate the rsa_cert.pem file, please update the PublicKey value saved here.
    protected final static String SimplePublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl66sYoc5HXGHnrtGCMZ6G8zLHnAl+xhP7bOQMmqwEqtwI+yJJG3asvLhJQiizP0cMA317ekJE6VAJ2DBT8g2npqJSXK/IuVQokM4CNp0IIbD66qgVLJ4DS1jzf6GFciJAiGOHztl8ICd7/q0EvuYcwd/sUjTrwRpkLcEH2Z/FE2sh4a82UwyxZkX3ghbZ/3MFtsMjzw0cSqKPUrgGCr4ZcAWZeoye81cLybY5Vb/5/eZfkeBIDwSSssqJRmsNBFs23c+RAymtKaP7wsQw5ATEeI7pe0kiWLpqH4wtsDVyN1C/p+vZJSia0OQJ/z89b5OkmpFC6qGBGxC7eOk71wCJwIDAQAB";
    protected final static String ComplexPublicKey = "-----BEGIN PUBLIC KEY-----" + SimplePublicKey + "-----END PUBLIC KEY-----";
    protected final static String PemFile = "rsa_key.pem";
    protected final static String ComplexPemFile = "rsa_key_withCert.pem";
    protected final static String BadPemFile = "bad_key.pem";
    protected final static String jwksUri = "\"http://localhost:${bvt.prop.security_2_HTTP_default}/jwt/ibm/api/defaultJWT/jwk\"";

    protected static String publicKey = null;
    protected static String keyLoc = null;
    protected static String defKeyLoc = null;
    protected static String absolutePemLoc = null;
    public static String builtToken = null;

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
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, getServerHostIp());
        if (mpConfigSettings.certType.equals(MpJwtFatConstants.JWK_CERT)) {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", jwksUri);
        } else {
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
            bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        }

        setupMPConfig(server, mpConfigSettings, mpConfigLocation);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile);
        saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
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
        mpConfigSettings.issuer = buildIssuer(mpConfigSettings.issuer);
        // remove variables/dollar signs/... (need the server defined before we can do this...
        mpConfigSettings.publicKeyLocation = resolvedJwksUri(mpConfigSettings.publicKeyLocation);

        Log.info(thisClass, "setupMPConfig", "mpConfigLocation is set to: " + mpConfigLocation.toString());
        switch (mpConfigLocation) {
            case SYSTEM_VAR:
                // if we're testing system properties, we'll need to update the values in the jvm.options file (if the file exists, update it)
                setAlternateMP_ConfigProperties_InJvmOptions(theServer, mpConfigSettings);
                deployRSServerNoMPConfigInAppApp(theServer);
                break;
            case ENV_VAR:
                // if we're testing env variables, we'll need to set environment variables
                setAlternateMP_ConfigProperties_envVars(theServer, mpConfigSettings);
                deployRSServerNoMPConfigInAppApp(theServer);
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

        String thisMethod = "setAlternateMP_ConfigProperties_InJvmOptions";

        String jvmFile = theServer.getServerRoot() + "/jvm.options";

        File f = new File(jvmFile);
        if (f.exists() && !f.isDirectory()) {
            updateJvmOptionsFile(f, mpConfigSettings);
        } else {
            Log.info(thisClass, thisMethod, "jvm.options does not exist - nothing to update");
        }
    }

    /**
     * Update the values in the jvm.options file of the server
     *
     * @param f - the file to update
     * @param publicKey - the publicKey value to update
     * @param keyLoc - the keyLocation value to update
     * @param issuer - the issuer value to update
     */
    static void updateJvmOptionsFile(File f, MPConfigSettings mpConfigSettings) {
        String oldContent = "";
        BufferedReader reader = null;
        FileWriter writer = null;

        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();

            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();
            }
            String newContent = oldContent.replaceAll("xxx_publicKeyName_xxx", mpConfigSettings.publicKey)
                            .replaceAll("xxx_publicKeyLoc_xxx", mpConfigSettings.publicKeyLocation)
                            .replaceAll("xxx_issuer_xxx", mpConfigSettings.issuer);
            Log.info(thisClass, "updateJvmOptionsFile", "New jvm.options file content: " + newContent);
            writer = new FileWriter(f);
            writer.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets/creates environment variables
     *
     * @param theServer - the resource server
     * @param mpConfigSettings - the mp-config settings values
     * @throws Exception
     */
    public static void setAlternateMP_ConfigProperties_envVars(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

        // some platforms do NOT support env vars with ".", so, we'll use underscores "_" (our runtime allows either)
        String PublicKeyName = "mp_jwt_verify_publickey";
        String LocationName = "mp_jwt_verify_publickey_location";
        String IssuerName = "mp_jwt_verify_issuer";

        HashMap<String, String> envVars = new HashMap<String, String>();
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", PublicKeyName + "=" + mpConfigSettings.publicKey);
        envVars.put(PublicKeyName, mpConfigSettings.publicKey);
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", LocationName + "=" + mpConfigSettings.publicKeyLocation);
        envVars.put(LocationName, mpConfigSettings.publicKeyLocation);
        Log.info(thisClass, "setAlternateMP_ConfigProperties_envVars", IssuerName + "=" + mpConfigSettings.issuer);
        envVars.put(IssuerName, mpConfigSettings.issuer);

        server.setAdditionalSystemProperties(envVars);
    }

    private static String buildFileContent(String publicKey, String publicKeyLocation, String issuer) {
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

        String notSet = "";

        try {
            String fixedJwksUri = resolvedJwksUri(jwksUri);
            String fileLoc = server.getServerRoot() + "/";

            // the microprofile-config.properties files will have xxx_<attr>_xxx values that need to be replaced
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, mpConfigSettings.issuer));
            // apps with some "bad" and some "good" values do need to be updated

            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, "badIssuer"));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(mpConfigSettings.publicKey, mpConfigSettings.publicKeyLocation, "badIssuer"));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT, buildFileContent(notSet, notSet, "badIssuer"));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_ISSUER_ONLY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, notSet, "badIssuer"));

            // publicKey (NOT keyLocation)
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(ComplexPublicKey, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(SimplePublicKey, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(ComplexPublicKey, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(SimplePublicKey, notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent("badPublicKey", notSet, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent("badPublicKey", notSet, mpConfigSettings.issuer));

            // publicKeyLocation (NOT publicKey)
            // not testing all locations (relative, file, url, jwksuri) with all pem loc types (good pem, complex pem, bad pem)
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, ComplexPemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, fileLoc + PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, "file:///" + fileLoc + PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, fixedJwksUri, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, fileLoc + PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, fileLoc + ComplexPemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, "file:///" + fileLoc + PemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, fixedJwksUri, mpConfigSettings.issuer));

            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, fileLoc + BadPemFile, mpConfigSettings.issuer));
            deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                    buildFileContent(notSet, "file:///" + fileLoc + "someKey.pem", mpConfigSettings.issuer));
            deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                      buildFileContent(notSet, "badPublicKeyLocation", mpConfigSettings.issuer));

        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    private static void deployRSServerNoMPConfigInAppApp(LibertyServer server) throws Exception {
        List<String> classList = new ArrayList<String>();
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.CommonMicroProfileMarker_MPConfigNotInApp");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigNotInApp.MicroProfileApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithPems(MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT, classList));
        server.addInstalledAppForValidation(MpJwtFatConstants.NO_MP_CONFIG_IN_APP_ROOT_CONTEXT);

    }

    private static void deployRSServerMPConfigInAppInMetaInfApp(LibertyServer server, String warName, String configSettings) throws Exception {
        String sourceName = "microProfileMP-ConfigInMETA-INF.war";
        String metaInfFile = "/META-INF/microprofile-config.properties";
        List<String> classList = new ArrayList<String>();
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.CommonMicroProfileMarker_MPConfigInMetaInf");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigInMetaInf.MicroProfileApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithPemsAndMPConfig(sourceName, warName, classList, metaInfFile, configSettings));
        server.addInstalledAppForValidation(warName);

    }

    private static void deployRSServerMPConfigInAppUnderWebInfApp(LibertyServer server, String warName, String configSettings) throws Exception {
        String sourceName = "microProfileMP-ConfigUnderWeb-INF.war";
        String webInfFile = "/WEB-INF/classes/META-INF/microprofile-config.properties";
        List<String> classList = new ArrayList<String>();
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.CommonMicroProfileMarker_MPConfigUnderWebInf");
        classList.add("com.ibm.ws.jaxrs.fat.microProfileApp.microProfileMPConfigUnderWebInf.MicroProfileApp");
        ShrinkHelper.exportAppToServer(server, genericCreateArchiveWithPemsAndMPConfig(sourceName, warName, classList, webInfFile, configSettings));
        server.addInstalledAppForValidation(warName);

    }

    protected static WebArchive genericCreateArchiveWithPems(String baseWarName, List<String> classList) throws Exception {
        try {
            String warName = baseWarName + ".war";
            WebArchive newWar = ShrinkWrap.create(WebArchive.class, warName)
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                            .add(new FileAsset(new File("test-applications/" + warName + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                            .add(new FileAsset(new File("publish/shared/securityKeys/bad_key.pem")), "/WEB-INF/classes/bad_key.pem")
                            .add(new FileAsset(new File("publish/shared/securityKeys/rsa_key_withCert.pem")), "/WEB-INF/classes/rsa_key_withCert.pem")
                            .add(new FileAsset(new File("publish/shared/securityKeys/rsa_key.pem")), "/WEB-INF/classes/rsa_key.pem");
            for (String theClass : classList) {
                newWar.addClass(theClass);
            }
            return newWar;
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
            throw e;
        }
    }

    protected static WebArchive genericCreateArchiveWithPemsAndMPConfig(String sourceName, String baseWarName, List<String> classList, String mpConfig,
                                                                        String fileContent) throws Exception {
        try {
            String warName = baseWarName + ".war";
            WebArchive newWar = ShrinkWrap.create(WebArchive.class, warName)
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/CommonMicroProfileApp.class")
                            .add(new FileAsset(new File("build/classes/com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")),
                                 "com/ibm/ws/security/jwt/fat/mpjwt/MpJwtFatConstants.class")
                            .add(new FileAsset(new File("test-applications/" + sourceName + "/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                            .add(new FileAsset(new File("test-applications/" + sourceName + "/resources/META-INF/permissions.xml")), "/META-INF/permissions.xml")
                            .add(new FileAsset(new File("publish/shared/securityKeys/bad_key.pem")), "/WEB-INF/classes/bad_key.pem")
                            .add(new FileAsset(new File("publish/shared/securityKeys/rsa_key_withCert.pem")), "/WEB-INF/classes/rsa_key_withCert.pem")
                            .add(new FileAsset(new File("publish/shared/securityKeys/rsa_key.pem")), "/WEB-INF/classes/rsa_key.pem")
                            .add(new StringAsset(fileContent), mpConfig);
            for (String theClass : classList) {
                newWar.addClass(theClass);
            }
            return newWar;
        } catch (Exception e) {
            Log.error(thisClass, "genericCreateArchive", e);
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
            return getServerUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + getServerIpUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + getServerSecureUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
                   + getServerIpSecureUrlBase(jwtBuilderServer) + "jwt/defaultJWT, "
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
            return getServerUrlBase(jwtBuilderServer) + "jwt/ibm/api/defaultJWT/jwk";

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
    public void standardTestFlow(LibertyServer server, String rootContext, String theApp, String className) throws Exception {
        standardTestFlow(server, rootContext, theApp, className, null);

    }

    public void standardTestFlow(LibertyServer server, String rootContext, String theApp, String className, Expectations expectations) throws Exception {

        builtToken = actions.getJwtFromTokenEndpoint(_testName, "defaultJWT", getServerSecureUrlBase(jwtBuilderServer), defaultUser, defaultPassword);

        String testUrl = buildAppUrl(server, rootContext, theApp);

        WebClient webClient = actions.createWebClient();

        if (expectations == null) { // implies expecting a good result
            expectations = setGoodAppExpectations(testUrl, className);
        }
        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        validationUtils.validateResult(response, defaultAction, expectations);

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
        expectations.addExpectation(new ResponseStatusExpectation(defaultAction, HttpServletResponse.SC_UNAUTHORIZED));
        // TODO finish this
        switch (failureCause) {
            case MpJwtFatConstants.X509_CERT:
                String invalidKeyName = "badKeyName";
//TODO fix checking
                expectations.addExpectation(new ServerMessageExpectation(defaultAction, server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request the provided token."));

                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations,
                //                    MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_MATCHES, "Messagelog did not contain an error indicating a problem authenticating the request the provided token.",
                //                    MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ);
                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations,
                //                    MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_MATCHES, "Messagelog did not a nessage stating that the alias was NOT found in the keystore.", invalidKeyName + ".*is not present in theKeyStore as a certificate");
                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations,
                //                    MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_MATCHES, "Messagelog did not indicate that the signing key is NOT available.", MpJwtMessageConstants.CWWKS6007E_BAD_KEY_ALIAS + ".*" +
                //                            invalidKeyName);
                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations,
                //                    MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_MATCHES, "Messagelog did not indicate that the signing key is NOT available.",
                //                    MpJwtMessageConstants.CWWKS6033E_JWT_CONSUMER_PUBLIC_KEY_NOT_RETRIEVED + ".*" + invalidKeyName + ".*rsa_trust");
                break;
            case MpJwtFatConstants.JWK_CERT:
                expectations.addExpectation(new ServerMessageExpectation(defaultAction, server, MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ, "Messagelog did not contain an error indicating a problem authenticating the request the provided token."));
                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations, MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_CONTAINS, "Messagelog did not contain an error indicating a problem authenticating the request the provided token.",
                //                    MpJwtMessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ);
                //            expectations = validationTools.addMessageExpectation(resourceTestServer, expectations,
                //                    MpJwtFatConstants.INVOKE_RS_PROTECTED_RESOURCE, MpJwtFatConstants.MESSAGES_LOG, MpJwtFatConstants.STRING_CONTAINS, "Messagelog did not contain an exception indicating that the signature was NOT valid.",
                //                    MpJwtMessageConstants.CWWKS6029E_NO_SIGNING_KEY);
                //            break;
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
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(defaultAction, testUrl));
        expectations.addExpectation(new ResponseFullExpectation(defaultAction, MpJwtFatConstants.STRING_CONTAINS, theClass, "Did not invoke the app " + theClass + "."));

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

        if (cert_type == MpJwtFatConstants.X509_CERT) {
            server.reconfigureServer("rs_server_AltConfigInApp_badServerXmlKeyName.xml", _testName);
        } else {
            server.reconfigureServer("rs_server_AltConfigInApp_badServerXmlJwksUri.xml", _testName);
        }

    }

}
