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
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

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
public class MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests extends MPJwt11MPConfigTests {

    public static Class<?> thisClass = MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // server has an empty mpJwt config
        MP11ConfigSettings mpConfigSettings = new MP11ConfigSettings(MP11ConfigSettings.PemFile, MP11ConfigSettings.PublicKeyNotSet, "testIssuer", MpJwtFatConstants.X509_CERT);

        // create and install apps
        deployRSServerMPConfigInAppApps(resourceServer, mpConfigSettings);

        // each test case will specify its own config, so skip the between test server config restore
        skipRestoreServerTracker.addServer(resourceServer);

        startRSServerForMPTests(resourceServer, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");

    }

    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MP11ConfigSettings mpConfigSettings) throws Exception {

        String thisMethod = "deployRSServerMPConfigInAppApps";
        try {
            String fixedJwksUri = resolvedJwksUri(jwtBuilderServer, MP11ConfigSettings.jwksUri);
            String fileLoc = MP11ConfigSettings.getDefaultKeyFileLoc(server);

            // publicKey (NOT keyLocation)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs256PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs256PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs384PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs384PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs512PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.rs512PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es256PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es256PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es384PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es384PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es512PubKey),
                                                                                        MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.getComplexKey(server, MP11ConfigSettings.es512PubKey),
                                                                                          MP11ConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

            /*
             * TODO - enable when PS supported is added
             */
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps256PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps256PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));
//
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps384PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps384PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));
//
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps512PubKey),
//                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, MPConfigSettings.ps512PubKey),
//                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));

            // publicKeyLocation (NOT publicKey)
            // not testing all locations (relative, file, url, jwksuri) with all signature algorithms
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.es512PubKey,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.rs256PubKey,
                                                                                          mpConfigSettings.getIssuer()));
// TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.ps384PubKey, mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.rs256PubKey,
                                                                                        mpConfigSettings.getIssuer()));
            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.ps384PubKey,
//                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, fileLoc + MP11ConfigSettings.es512PubKey,
                                                                                          mpConfigSettings.getIssuer()));

            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MP11ConfigSettings.rs384PubKey,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MP11ConfigSettings.es256PubKey,
                                                                                          mpConfigSettings.getIssuer()));
            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MPConfigSettings.ps512PubKey,
//                                                                                          mpConfigSettings.getIssuer()));

            // TODO - enable when PS algs supported
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri.replace("defaultJWT", MpJwtFatConstants.SIGALG_PS256),
//                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet,
                                                                                        fixedJwksUri.replace("defaultJWT", MpJwtFatConstants.SIGALG_RS512),
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(MP11ConfigSettings.PublicKeyNotSet,
                                                                                          fixedJwksUri.replace("defaultJWT", MpJwtFatConstants.SIGALG_ES384),
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS256, resourceServer, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS256, resourceServer, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRS512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES256, resourceServer, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES256, resourceServer, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodES512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS384, resourceServer, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS384, resourceServer, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS512, resourceServer, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
//TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodPS512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS512, resourceServer, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRelativeES512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRelativeRS256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS256, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodRelativePS384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS384, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodFileRS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS256, resourceServer, MpJwtFatConstants.GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodFilePS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS384, resourceServer, MpJwtFatConstants.GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodFileES512PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodUrlRS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodUrlES256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES256, resourceServer, MpJwtFatConstants.GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS512, resourceServer, MpJwtFatConstants.GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodJwksUriPS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodJwksUriRS512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
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
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_GoodJwksUriES384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    //       // test mis-matches between sigAlg and the public key provided
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a relative publicKeyLocation in mp-config (in the META-INF
     * directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with RS384, mpJwt config has RS384, public key is ES512
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgRS384_BadRelativeES512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_ES512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a relative publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES512, mpJwt config has ES512, public key is RS256
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES512_BadRelativeRS256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_RS256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a relative publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES512, mpJwt config has ES512, public key is PS384
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES512_BadRelativePS384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_RELATIVE_PS384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES512, mpJwt config has ES512, public key is RS256
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES512_BadFileRS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_FILE_RS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES512, mpJwt config has ES512, public key is PS384
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES512_BadFilePS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES512, resourceServer, MpJwtFatConstants.GOOD_FILE_PS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with RS384, mpJwt config has RS384, public key is ES512
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgRS384_BadFileES512PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_FILE_ES512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a URL file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES256, mpJwt config has ES256, public key is RS384
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES256_BadUrlRS384PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES256, resourceServer, MpJwtFatConstants.GOOD_URL_RS384_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a URL file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with RS384, mpJwt config has RS384, public key is ES256
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgRS384_BadUrlES256PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_URL_ES256_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a URL file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with RS384, mpJwt config has RS384, public key is PS512
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgRS384_BadUrlPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS384, resourceServer, MpJwtFatConstants.GOOD_URL_PS512_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES384, mpJwt config has ES384, public key is PS256
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available  @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES384_BadJwksUriPS256PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_PS256_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with ES384, mpJwt config has ES384, public key is RS512
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgES384_BadJwksUriRS512PublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_RS512_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a jwksuri based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app) that refers to a public key of a type different than the signature algorithm specified in the config.
     * This test shows that the runtime uses the mp-config and fails appropriately
     * Token created with RS256, mpJwt config has RS256, public key is ES384
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_cfgRS256_BadJwksUriES384PublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS256, resourceServer, MpJwtFatConstants.GOOD_JWKSURI_ES384_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, BadKey));

    }

    // test mis-matches between sigAlg/private key in the token vs the mp config
    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app). The token is built using a different alg. (token RS512, mpConfig RS256)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenRS512_RS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_RS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).The token is built using a different alg. (token ES384, mpConfig RS384)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenES384_RS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_RS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app). The token is built using a different alg. (token PS256, mpConfig RS512)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    //TODO - uncomment when PS is supported @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenPS256_RS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_RS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_RS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app). The token is built using a different alg. (token PS256, mpConfig ES256)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //TODO - uncomment when PS is supported @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenPS256_ES256PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_ES256_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app). The token is built using a different alg. (token RS512, mpConfig ES384)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenRS512_ES384PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_ES384_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app). The token is built using a different alg. (token ES384, mpConfig ES512)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenES384_ES512PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_ES512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_ES512_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app). The token is built using a different alg. (token ES384, mpConfig PS256)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenES384_PS256PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS256InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_ES384, resourceServer, MpJwtFatConstants.GOOD_PS256_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app). The token is built using a different alg. (token PS256, mpConfig PS384)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenPS256_PS384PublicKeyInMPConfigUnderWebInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS384InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_PS256, resourceServer, MpJwtFatConstants.GOOD_PS384_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

    /**
     * The mpJwt config in server.xml only exists to specify the sigAlg. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app). The token is built using a different alg. (token RS512, mpConfig PS512)
     * This test shows that the runtime uses the mp-config and will encounter a failure due to the mismatched private key in the token and
     * public key in the mp config.
     *
     * @throws Exception
     */
    //TODO - enable when ps algs available    @Test
    public void MPJwtMPOtherSigAlgConfigInApp_SigAlgOnlyMPJwtConfigInServerXml_MisMatch_tokenRS512_PS512PublicKeyInMPConfigInMetaInf_test() throws Exception {

        resourceServer.reconfigureServerUsingExpandedConfiguration(_testName, "rs_server_AltConfigInApp_PS512InServerXmlConfig.xml");
        standard11TestFlow(MpJwtFatConstants.SIGALG_RS512, resourceServer, MpJwtFatConstants.GOOD_PS512_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, KeyMismatch));

    }

}
