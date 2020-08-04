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
package com.ibm.ws.security.mp.jwt11.fat;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;
import com.ibm.ws.security.mp.jwt11.fat.sharedTests.MPJwtLoginConfig_VariationTests;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will verify that we get the correct behavior with
 * ignoreApplicationAuthMethod=true in the config and various combinations of
 * login-config existing or not existing in web.xml and/or the application.
 * login-config must exist and be set to MP-JWT in either the web.xml or the application.
 * If set in both, the value in web.xml will take precedence...
 *
 **/

@Mode(TestMode.FULL)
public class MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests extends MPJwtLoginConfig_VariationTests {

    public static Class<?> thisClass = MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests.class;

    @BeforeClass
    public static void setUp() throws Exception {

        loginConfigSetUp("rs_server_loginConfig_ignoreApplicationAuthMethodTrue.xml");

    }

    /******************** Tests *******************/

    /**
     * login-config does NOT exist in web.xml
     * login-config does NOT exist in the app
     * ignoreApplicationAuthMethod is true:
     * We should access the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_notInWebXML_notInApp() throws Exception {

        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_NOT_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_NO_LOGIN_CONFIG,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, and is set to BASIC
     * ignoreApplicationAuthMethod is true:
     * We should access the app
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_notInWebXML_basicInApp() throws Exception {

        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_BASIC,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, and is set to MP-JWT
     * ignoreApplicationAuthMethod is true:
     * We should be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_notInWebXML_mpJwtInApp() throws Exception {

        genericLoginConfigVariationTest(MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MP_JWT,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does exist in web.xml, and is set to FORM_LOGIN
     * login-config does NOT exist in the app
     * ignoreApplicationAuthMethod is true:
     * We should access the app having used the mp-jwt token
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_formLoginInWebXML_notInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_NOT_IN_APP,
                                                 MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_NOTINAPP,
                                                 UseJWTToken.YES);

    }

    /**
     * login-config does exist in web.xml, and is set to FORM_LOGIN
     * login-config does exist in the app, and is set to BASIC
     * ignoreApplicationAuthMethod is true:
     * We should access the app having used the mp-jwt token
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_formLoginInWebXML_basicInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                                                 MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_BASICINAPP,
                                                 UseJWTToken.YES);

    }

    /**
     * login-config does exist in web.xml, and is set to FORM_LOGIN
     * login-config does exist in the app, and is set to MP-JWT
     * ignoreApplicationAuthMethod is true:
     * We should access the app having used the mp-jwt token
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_formLoginInWebXML_mpJwtInApp() throws Exception {

        genericLoginConfigFormLoginVariationTest(
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                                 MpJwtFatConstants.LOGINCONFIG_FORM_LOGIN_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                                                 MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_FORMLOGININWEBXML_MPJWTINAPP,
                                                 UseJWTToken.YES);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does NOT exist in the app
     * ignoreApplicationAuthMethod is true:
     * We should be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_mpJwtInWebXML_notInApp() throws Exception {

        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_NOT_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_NOTINAPP,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does exist in the app, and is set to BASIC
     * ignoreApplicationAuthMethod is true:
     * We should be able to access the protected app
     *
     * @throws Exception
     *
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_mpJwtInWebXML_basicInApp() throws Exception {
        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_BASIC_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_BASICINAPP,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does exist in web.xml, and is set to MP-JWT
     * login-config does exist in the app, and is set to MP-JWT
     * ignoreApplicationAuthMethod is true:
     * We should be able to access the protected app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_mpJwtInWebXML_mpJwtInApp() throws Exception {
        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_MP_JWT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MPJWTINWEBXML_MPJWTINAPP,
                                        ExpectedResult.GOOD);

    }

    /**
     * login-config does NOT exist in web.xml
     * login-config does exist in the app, and is set to MP-JWT
     * ignoreApplicationAuthMethod is true:
     * We should be able to access the protected app
     *
     * @throws Exception
     */
    @Test
    public void MPJwtLoginConfig_ignoreApplicationAuthMethodTrueTests_multiLayer_notInWebXML_mpJwtInApp() throws Exception {
        genericLoginConfigVariationTest(
                                        MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP_ROOT_CONTEXT,
                                        MpJwtFatConstants.LOGINCONFIG_MULTI_LAYER_NOT_IN_WEB_XML_SERVLET_MP_JWT_IN_APP,
                                        MpJwtFatConstants.MPJWT_APP_CLASS_LOGIN_CONFIG_MULTI_LAYER_MPJWTNOTINWEBXML_MPJWTINAPP,
                                        ExpectedResult.GOOD);

    }

}
