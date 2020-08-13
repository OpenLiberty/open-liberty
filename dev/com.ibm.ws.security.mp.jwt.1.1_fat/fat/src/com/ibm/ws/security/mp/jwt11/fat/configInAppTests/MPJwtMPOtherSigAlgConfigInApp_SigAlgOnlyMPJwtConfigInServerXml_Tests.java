/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will NOT have any mpJwt config specified, so, the values will have to
 * come from the application.
 * The tests will do one of the following:
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

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // server has an empty mpJwt config
//        MPConfigSettings mpConfigSettings = new MPConfigSettings(MPConfigSettings.PemFile, MPConfigSettings.PublicKeyNotSet, MPConfigSettings.IssuerNotSet, MpJwtFatConstants.X509_CERT);
        MPConfigSettings mpConfigSettings = new MPConfigSettings();

        // create and install apps
        deployRSServerMPConfigInAppApps(resourceServer, mpConfigSettings);

        startRSServerForMPTests(resourceServer, "rs_server_AltConfigInApp_SigAlgOnlyServerXmlConfig.xml");

//        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigInApp_SigAlgOnlyServerXmlConfig.xml", mpConfigSettings, MPConfigLocation.IN_APP);

    }

    public static void deployRSServerMPConfigInAppApps(LibertyServer server, MPConfigSettings mpConfigSettings) throws Exception {

        String thisMethod = "deployRSServerMPConfigInAppApps";
        try {
            String fixedJwksUri = resolvedJwksUri(MPConfigSettings.jwksUri);
            String fileLoc = mpConfigSettings.getDefaultKeyFileLoc(server);

            Log.info(thisClass, thisMethod, "Complex ES384: " + mpConfigSettings.getComplexKey(server, "ES384public-key.pem"));
            Log.info(thisClass, thisMethod, "Simple ES384: " + mpConfigSettings.getSimpleKey(server, "ES384public-key.pem"));

//            // the microprofile-config.properties files will have xxx_<attr>_xxx values that need to be replaced
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent("rsa_key.pem", MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent("rsa_key.pem", MPConfigSettings.PublicKeyLocationNotSet,
//                                                                                          mpConfigSettings.getIssuer()));

            // publicKey (NOT keyLocation)
            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                                                               buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, "ES384public-key.pem"),
                                                                                        MPConfigSettings.PublicKeyLocationNotSet,
                                                                                        mpConfigSettings.getIssuer()));
            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                                                                 buildMPConfigFileContent(mpConfigSettings.getComplexKey(server, "ES384public-key.pem"),
                                                                                          MPConfigSettings.PublicKeyLocationNotSet,
                                                                                          mpConfigSettings.getIssuer()));

//            // publicKeyLocation (NOT publicKey)
//            // not testing all locations (relative, file, url, jwksuri) with all pem loc types (good pem, complex pem, bad pem)
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PemFile, mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.ComplexPemFile,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.PemFile,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MPConfigSettings.PemFile,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, MPConfigSettings.PemFile,
//                                                                                          mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.PemFile,
//                                                                                          mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.ComplexPemFile,
//                                                                                          mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + MPConfigSettings.PemFile,
//                                                                                          mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fixedJwksUri, mpConfigSettings.getIssuer()));
//
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, fileLoc + MPConfigSettings.BadPemFile,
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppInMetaInfApp(server, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
//                                                               buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "file:///" + fileLoc + "someKey.pem",
//                                                                                        mpConfigSettings.getIssuer()));
//            setupUtils.deployRSServerMPConfigInAppUnderWebInfApp(server, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
//                                                                 buildMPConfigFileContent(MPConfigSettings.PublicKeyNotSet, "badPublicKeyLocation", mpConfigSettings.getIssuer()));
//
        } catch (Exception e) {
            Log.info(thisClass, "MPJwtAltConfig", "Hit an exception updating the war file" + e.getMessage());
            throw e;
        }

    }

    /******************************************** tests **************************************/

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid mp-config (in the META-INF directory of the
     * app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid mp-config (under the WEB-INF directory of the
     * app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid issuer in mp-config (in the META-INF directory of the
     * app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodIssuerInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid issuer in mp-config (under the WEB-INF directory of the
     * app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodIssuerInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has an invalid issuer in mp-config (in the META-INF directory of the
     * app).
     * This test shows that the runtime fails to match the issuer in the token with what is specified in the mp-config.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadIssuerInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setBadIssuerExpectations(resourceServer));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has an invalid issuer in mp-config (under the WEB-INF directory of
     * the
     * app).
     * This test shows that the runtime fails to match the issuer in the token with what is specified in the mp-config.
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadIssuerInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadIssuerExpectations(resourceServer));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid simple publicKey in mp-config (in the META-INF directory
     * of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    // TODO - enable once supported - Issue 4783  //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodSimplePublicKeyInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid simple publicKey in mp-config (under the WB-INF
     * directory
     * of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4783  //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodSimplePublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid complex publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodComplexPublicKeyInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow("RS384", resourceServer, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid complex publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodComplexPublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow("RS384", resourceServer, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a bad publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime fails to validate the key
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadPublicKeyInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.JWK_CERT));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a bad publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime fails to validate the key
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadPublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.JWK_CERT));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid relative publicKeyLocation in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid relative publicKeyLocation in mp-config (under the
     * WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFilePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFilePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid URL file based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid URL file based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid jwksuri based publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodJwksUriPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid jwksuri based publicKeyLocation in mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodJwksUriPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid Complex relative publicKeyLocation in mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    // TODO - enable once supported - Issue 4794  //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodRelativeComplexPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has a valid Complex relative publicKeyLocation in mp-config (under
     * the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4794  //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_GoodFileComplexPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has an file publicKeyLocation that points to a bad pem file
     * (contents are bad) in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the mp-config and fails
     *
     * @throws Exception
     */
    //chc@Test
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException", "org.jose4j.jwt.consumer.InvalidJwtSignatureException" })
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadFilePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, "other"));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has an file publicKeyLocation that points to a bad pem file
     * (contents are bad) in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the mp-config and fails
     *
     * @throws Exception
     */
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadUrlPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.JWK_CERT));

    }

    /**
     * The server.xml has NO mp_jwt config specified. The app has an file publicKeyLocation that points to a bad pem file
     * (contents are bad) in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config and fails
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void MPJwtMPConfigInApp_NoMPJwtConfigInServerXml_BadRelativePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.JWK_CERT));

    }

}
