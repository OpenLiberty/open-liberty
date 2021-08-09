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
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

/**
 * This is the test class that will run basic OpenID Connect RP tests.
 * This test class extends GenericRPTests.
 * GenericRPTests contains common code for all RP tests.
 *
 **/

public class SameSiteTestExpectations {

    public static Class<?> thisClass = SameSiteTestExpectations.class;

    public static enum TestServerExpectations {
        ALL_SERVERS_SUCCEED, OP_GENERIC_FAILURE, RP_GENERIC_FAILURE, RP_REDIRECT_FAILURE, RS_GENERIC_FAILURE, ONLY_BROWSER_SHOWS_FAILURE
    };

    private TestServerExpectations baseTest = TestServerExpectations.ALL_SERVERS_SUCCEED;
    private TestServerExpectations httpRPAppUrlTest = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpRedirectUrlTest = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpAuthEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpTokenEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations rsHttpValidationEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;

    public SameSiteTestExpectations() {
        this(TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED);
    }

    public SameSiteTestExpectations(TestServerExpectations inHttpRPAppUrlTest, TestServerExpectations inHttpRedirectUrlTest, TestServerExpectations inHttpAuthEndpointUrlTestResult, TestServerExpectations inHttpTokenEndpointUrlTestResult, TestServerExpectations inRSHttpValidationEndpointUrlTestResult,
            TestServerExpectations inOPSSORequiresSSLTestResult, TestServerExpectations inRPSSORequiresSSLTestResult, TestServerExpectations inRSSSORequiresSSLTestResult) {
        this(TestServerExpectations.ALL_SERVERS_SUCCEED, inHttpRPAppUrlTest, inHttpRedirectUrlTest, inHttpAuthEndpointUrlTestResult, inHttpTokenEndpointUrlTestResult, inRSHttpValidationEndpointUrlTestResult);
    }

    public SameSiteTestExpectations(TestServerExpectations inBaseTest, TestServerExpectations inHttpRPAppUrlTest, TestServerExpectations inHttpRedirectUrlTest, TestServerExpectations inHttpAuthEndpointUrlTestResult, TestServerExpectations inHttpTokenEndpointUrlTestResult, TestServerExpectations inRSHttpValidationEndpointUrlTestResult) {
        baseTest = inBaseTest;
        httpRPAppUrlTest = inHttpRPAppUrlTest;
        httpRedirectUrlTest = inHttpRedirectUrlTest;
        httpAuthEndpointUrlTestResult = inHttpAuthEndpointUrlTestResult;
        httpTokenEndpointUrlTestResult = inHttpTokenEndpointUrlTestResult;
        rsHttpValidationEndpointUrlTestResult = inRSHttpValidationEndpointUrlTestResult;
    }

    public void setBaseTestResult(TestServerExpectations inBaseTest) {
        baseTest = inBaseTest;
    }

    public TestServerExpectations getBaseTestResult() {
        return baseTest;
    }

    public void setHttpRPAppUrlTestResult(TestServerExpectations inHttpRPAppUrlTest) {
        httpRPAppUrlTest = inHttpRPAppUrlTest;
    }

    public TestServerExpectations getHttpRPAppUrlTestResult() {
        return httpRPAppUrlTest;
    }

    public void setHttpRedirectUrlTestResult(TestServerExpectations inHttpRedirectUrlTest) {
        httpRedirectUrlTest = inHttpRedirectUrlTest;
    }

    public TestServerExpectations getHttpRedirectUrlTestResult() {
        return httpRedirectUrlTest;
    }

    public void setHttpAuthEndpointUrlTestResult(TestServerExpectations inHttpAuthEndpointUrlTestResult) {
        httpAuthEndpointUrlTestResult = inHttpAuthEndpointUrlTestResult;
    }

    public TestServerExpectations getHttpAuthEndpointUrlTestResult() {
        return httpAuthEndpointUrlTestResult;
    }

    public void setHttpTokenEndpointUrlTestResult(TestServerExpectations inHttpTokenEndpointUrlTestResult) {
        httpTokenEndpointUrlTestResult = inHttpTokenEndpointUrlTestResult;
    }

    public TestServerExpectations getHttpTokenEndpointUrlTestResult() {
        return httpTokenEndpointUrlTestResult;
    }

    public void setRSHttpValidationEndpointUrlTestResult(TestServerExpectations inRSHttpValidationEndpointUrlTestResult) {
        rsHttpValidationEndpointUrlTestResult = inRSHttpValidationEndpointUrlTestResult;
    }

    public TestServerExpectations getRSHttpValidationEndpointUrlTestResult() {
        return rsHttpValidationEndpointUrlTestResult;
    }

    public void setOPSSORequiresSSLTestResult(TestServerExpectations inOPSSORequiresSSLTestResult) {
        rsHttpValidationEndpointUrlTestResult = inOPSSORequiresSSLTestResult;
    }

}
