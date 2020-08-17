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
package com.ibm.ws.security.mp.jwt11.fat.configInAppTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwtMPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.utils.MPConfigSettings;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will only specify the signature algorithm (to match each test case)
 * and audience (just so we can re-use the same builders) - everything else will come from the application.
 * * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    /* key file names */
    private static final String rs256PubKey = "RS256public-key.pem";
    private static final String rs384PubKey = "RS384public-key.pem";
    private static final String rs512PubKey = "RS512public-key.pem";
    private static final String es256PubKey = "ES256public-key.pem";
    private static final String es384PubKey = "ES384public-key.pem";
    private static final String es512PubKey = "ES512public-key.pem";
    private static final String ps256PubKey = "PS256public-key.pem";
    private static final String ps384PubKey = "PS384public-key.pem";
    private static final String ps512PubKey = "PS512public-key.pem";

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // server has an empty mpJwt config
        MPConfigSettings mpConfigSettings = new MPConfigSettings(MPConfigSettings.PemFile, MPConfigSettings.PublicKeyNotSet, "testIssuer", MpJwtFatConstants.X509_CERT);

        // create and install apps
        deployRSServerMPConfigInAppApps(resourceServer, mpConfigSettings);

        // each test case will specify its own config, so skip the between test server config restore
        skipRestoreServerTracker.addServer(resourceServer);

        startRSServerForMPTests(resourceServer, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");

    }

    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

        String thisMethod = "deployRSServerMPConfigInAppApps";
        try {
            String fixedJwksUri = resolvedJwksUri(MPConfigSettings.jwksUri);
            String fileLoc = mpConfigSettings.getDefaultKeyFileLoc(server);

            // publicKey (NOT keyLocation)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs256PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs256PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs384PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs384PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs512PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, rs512PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es256PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es256PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es384PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es384PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es512PubKey),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, es512PubKey),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            /*
             * TODO - enable when PS supported is added
             */
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps256PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps256PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));
//
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps384PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps384PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));
//
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps512PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, ps512PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));

            // publicKeyLocation (NOT publicKey)
            // not testing all locations (relative, file, url, jwksuri) with all signature algorithms
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, es512PubKey, mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, rs256PubKey, mpConfigSettings.getIssuer()));
// TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, ps384PubKey, mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + rs256PubKey,
                                                                                        mpConfigSettings.getIssuer()));
            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + ps384PubKey,
//                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + es512PubKey,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + rs384PubKey,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + es256PubKey,
                                                                                          mpConfigSettings.getIssuer()));
            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + ps512PubKey,
//                                                                                          mpConfigSettings.getIssuer()));

            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri.replace("defaultJWT", "PS256"),
//                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri.replace("defaultJWT", "RS512"),
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri.replace("defaultJWT", "ES384"),
                                                                                          mpConfigSettings.getIssuer()));

        } catch (Exception e) {
            Log.info(thisClass, "deployRSServerMPConfigInAppApps", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    /******************************************** tests **************************************/

    /************************************/
    /* public key */
    /************************************/

    // start RS
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standardTestFlow("RS256", resourceServer, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standardTestFlow("RS256", resourceServer, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standardTestFlow("RS384", resourceServer, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standardTestFlow("RS384", resourceServer, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standardTestFlow("RS512", resourceServer, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRS512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standardTestFlow("RS512", resourceServer, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    // end RS
    // start ES
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standardTestFlow("ES256", resourceServer, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standardTestFlow("ES256", resourceServer, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standardTestFlow("ES384", resourceServer, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standardTestFlow("ES384", resourceServer, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standardTestFlow("ES512", resourceServer, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodES512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standardTestFlow("ES512", resourceServer, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    // end ES
    // start PS
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standardTestFlow("PS256", resourceServer, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standardTestFlow("PS256", resourceServer, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standardTestFlow("PS384", resourceServer, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standardTestFlow("PS384", resourceServer, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standardTestFlow("PS512", resourceServer, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
//    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodPS512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standardTestFlow("PS512", resourceServer, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }
    // end PS
    // not including tests here for things like text strings in the mp config and non-existant files - those are tested in
    // the base tests - these tests are focusing on the algorithms and keys themselves.
    /************************************/

    /* key location tests */
    /************************************/
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid relative publicKeyLocation in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativeES512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standardTestFlow("ES512", resourceServer, MpJwtFatConstants.GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid relative publicKeyLocation in mp-config (under the
     * WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativeRS256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standardTestFlow("RS256", resourceServer, MpJwtFatConstants.GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid relative publicKeyLocation in mp-config (under the
     * WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativePS384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standardTestFlow("PS384", resourceServer, MpJwtFatConstants.GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFileRS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standardTestFlow("RS256", resourceServer, MpJwtFatConstants.GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFilePS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standardTestFlow("PS384", resourceServer, MpJwtFatConstants.GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFileES512PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standardTestFlow("ES512", resourceServer, MpJwtFatConstants.GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid URL file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodUrlRS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standardTestFlow("RS384", resourceServer, MpJwtFatConstants.GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid URL file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodUrlES256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standardTestFlow("ES256", resourceServer, MpJwtFatConstants.GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid URL file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standardTestFlow("PS512", resourceServer, MpJwtFatConstants.GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available  @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodJwksUriPS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standardTestFlow("PS256", resourceServer, MpJwtFatConstants.GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodJwksUriRS512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standardTestFlow("RS512", resourceServer, MpJwtFatConstants.GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid jwksuri based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodJwksUriES384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standardTestFlow("ES384", resourceServer, MpJwtFatConstants.GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

}
