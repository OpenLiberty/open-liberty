/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt.fat.sharedTests.MPJwtLoginConfig_VariationTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will verify that we get the correct behavior when we
 * have not mpJwt config.
 *
 **/

@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
public class MPJwtNoMpJwtConfig extends MPJwtLoginConfig_VariationTests {

    public static Class<?> thisClass = MPJwtNoMpJwtConfig.class;

    @BeforeClass
    public static void setUp() throws Exception {

        loginConfigSetUp("rs_server_noMpJwtConfig.xml");

    }

    /******************** Tests *******************/

    /**
     * login-config does NOT exist in web.xml
     * login-config does NOT exist in the app
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_notInWebXML_notInApp() throws Exception {

        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_NO_LOGIN_CONFIG,
                ExpectBadResult);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, but is set to BASIC
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_notInWebXML_basicInApp() throws Exception {

        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_BASIC,
                ExpectBadResult);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, and is set to MP-JWT
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_notInWebXML_mpJwtInApp() throws Exception {

        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MP_JWT,
                ExpectBadResult);

    }

    /**
     * login-config does exist in web.xml, but is set to FORM_LOGIN
     * login-config does NOT exist in the app
     * the mpJwt feature is NOT enabled
     * We should use FORM_LOGIN
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_formLoginInWebXML_notInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_NOTINAPP,
                WillNotUseJwtToken);

    }

    /**
     * login-config does exist in web.xml, but is set to FORM_LOGIN
     * login-config does NOT exist in the app, but is set to BASIC
     * the mpJwt feature is NOT enabled
     * We should use FORM_LOGIN
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_formLoginInWebXML_basicInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_BASICINAPP,
                WillNotUseJwtToken);

    }

    /**
     * login-config does exist in web.xml, but is set to FORM_LOGIN
     * login-config does exist in the app and is set to MP-JWT
     * the mpJwt feature is NOT enabled
     * We should use FORM_LOGIN
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_formLoginInWebXML_mpJwtInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_MPJWTINAPP,
                WillNotUseJwtToken);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does NOT exist in the app
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_mpJwtInWebXML_notInApp() throws Exception {

        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_NOTINAPP,
                ExpectBadResult);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does exist in the app, but is set to BASIC
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     * 
     * @throws Exception
     *
     */

    @Test
    public void MPJwtNoMpJwtConfig_mpJwtInWebXML_basicInApp() throws Exception {
        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_BASICINAPP,
                ExpectBadResult);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does exist in the app, and is set to MP-JWT
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtNoMpJwtConfig_mpJwtInWebXML_mpJwtInApp() throws Exception {
        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_MPJWTINAPP,
                ExpectBadResult);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, and is set to MP-JWT
     * the mpJwt feature is NOT enabled
     * We should receive a 401 status in an exception
     *
     * @throws Exception
     */

    @Test
    public void MPJwtNoMpJwtConfig_multiLayer_notInWebXML_mpJwtInApp() throws Exception {
        genericLoginConfigVariationTest(
                MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MULTI_LAYER_MPJWTNOTINWEBXML_MPJWTINAPP,
                ExpectBadResult);

    }
}
