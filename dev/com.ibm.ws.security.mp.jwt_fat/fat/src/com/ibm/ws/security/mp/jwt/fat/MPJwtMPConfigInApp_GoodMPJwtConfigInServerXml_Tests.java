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
package com.ibm.ws.security.mp.jwt.fat;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.sharedTests.MPJwtMPConfigTests;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will have a good MPJwt values configured - these tests will show that the
 * configuration specified in the server will override the values in the mp-config in the app.
 * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_Tests extends MPJwtMPConfigTests {

    public static Class<?> thisClass = MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // server has an empty mpJwt config
        MPConfigSettings mpConfigSettings = new MPConfigSettings(PemFile, PublicKeyNotSet, IssuerNotSet, MpJwtFatConstants.X509_CERT);
        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigInApp_goodServerXmlConfig.xml", mpConfigSettings, MPConfigLocation.IN_APP);

    }

    /******************************************** tests **************************************/

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a valid mp-config (in the META-INF directory of
     * the
     * app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_GoodMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a valid mp-config (under the WEB-INF directory
     * of the app).
     * This test shows that the runtime uses the mp-config
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_GoodMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a invalid issuer in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadIssuerInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a valid issuer in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadIssuerInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a bad publicKey in mp-config (in the META-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadPublicKeyInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has a bad publicKey in mp-config (under the WEB-INF
     * directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadPublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has an file publicKeyLocation that points to a bad
     * pem file (contents are bad) in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadFilePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has an url publicKeyLocation that points to a bad
     * pem file (contents are bad) in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadUrlPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF);

    }

    /**
     * The server.xml has valid values in the mp_jwt config specified. The app has an relative publicKeyLocation that points to a
     * bad
     * pem file (contents are bad) in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the mp-config - access is granted
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_GoodMPJwtConfigInServerXml_BadRelativePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standardTestFlow(resourceServer, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF);

    }

}
