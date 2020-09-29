/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwt11MPConfigTests;
import com.ibm.ws.security.mp.jwt11.fat.utils.MP11ConfigSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This test class that will test the placement of mp jwt config settings inside of an app. They can be found in multiple
 * places within the application. The server.xml will have a bad keyName configured - these tests will show that the
 * configuration specified in the server will override the values in the mp-config in the app.
 * The tests will do one of the following:
 * - request use of app that has placement of config in "resources/META-INF/microprofile-config.properties"
 * - request use of app that has placement of config in "resources/WEB-INF/classes/META-INF/microprofile-config.properties"
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "com.ibm.websphere.security.jwt.KeyException", "java.security.cert.CertificateException" })
public class MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_Tests extends MPJwt11MPConfigTests {

    public static Class<?> thisClass = MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_Tests.class;

    @Server("com.ibm.ws.security.mp.jwt.1.1.fat")
    public static LibertyServer resourceServer;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

        // let's pick a default config - use a pemFile located in the app
        MP11ConfigSettings mpConfigSettings = new MP11ConfigSettings(MP11ConfigSettings.PemFile, MP11ConfigSettings.PublicKeyNotSet, MP11ConfigSettings.IssuerNotSet, MpJwtFatConstants.X509_CERT);
        setUpAndStartRSServerForTests(resourceServer, "rs_server_AltConfigInApp_badServerXmlKeyName.xml", mpConfigSettings, MPConfigLocation.IN_APP);

    }

    /******************************************** tests **************************************/

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid mp-config (in the
     * META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_IN_META_INF_ROOT_CONTEXT, MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP,
                         MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF, setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid mp-config (under the
     * WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has an invalid issuer in mp-config
     * (in
     * the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadIssuerInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has an invalid issuer in mp-config
     * (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadIssuerInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_ISSUER_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid simple publicKey in
     * mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4783  @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodSimplePublicKeyInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid simple publicKey in
     * mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4783  @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodSimplePublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_SIMPLE_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid complex publicKey in
     * mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodComplexPublicKeyInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid complex publicKey in
     * mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodComplexPublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_COMPLEX_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a bad publicKey in mp-config
     * (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadPublicKeyInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a bad publicKey in mp-config
     * (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadPublicKeyInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_PUBLICKEY_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid relative
     * publicKeyLocation in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodRelativePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid relative
     * publicKeyLocation in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodRelativePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid file based
     * publicKeyLocation in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodFilePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid file based
     * publicKeyLocation in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodFilePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid URL file based
     * publicKeyLocation in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid URL file based
     * publicKeyLocation in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodUrlPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_URL_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid jwksuri based
     * publicKeyLocation in mp-config (in the META-INF directory of the* app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodJwksUriPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid jwksuri based
     * publicKeyLocation in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodJwksUriPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_JWKSURI_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid relative Complex
     * publicKeyLocation in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4794  @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodRelativeComplexPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_RELATIVE_COMPLEX_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has a valid relative complex
     * publicKeyLocation in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    // TODO - enable once supported - Issue 4794  @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_GoodFileComplexPublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.GOOD_FILE_COMPLEX_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has an file publicKeyLocation that
     * points to a bad pem file (contents are bad) in mp-config (in the META-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadFilePublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_FILE_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has an url publicKeyLocation that
     * points to a bad pem file (contents are bad) in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadUrlPublicKeyLocationInMPConfigInMetaInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_URL_KEYLOCATION_IN_CONFIG_IN_META_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_IN_META_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_IN_META_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

    /**
     * The server.xml has an mp_jwt config specified that includes an invalid keyName. The app has an relative publicKeyLocation
     * that
     * points to a bad pem file (contents are bad) in mp-config (under the WEB-INF directory of the app).
     * This test shows that the runtime uses the value from the server.xml instead of what is in the mp-config in the app.
     *
     * @throws Exception
     */
    @Test
    public void MPJwtMPConfigInApp_BadKeyNameMPJwtConfigInServerXml_BadRelativePublicKeyLocationInMPConfigUnderWebInf_test() throws Exception {

        standard11TestFlow(resourceServer, MpJwtFatConstants.BAD_RELATIVE_KEYLOCATION_IN_CONFIG_UNDER_WEB_INF_ROOT_CONTEXT,
                         MpJwtFatConstants.MP_CONFIG_UNDER_WEB_INF_TREE_APP, MpJwtFatConstants.MPJWT_APP_CLASS_MP_CONFIG_UNDER_WEB_INF,
                         setBadCertExpectations(resourceServer, MpJwtFatConstants.X509_CERT));

    }

}
