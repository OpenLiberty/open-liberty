/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest.structures;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;

/**
 * This is the test class that will run basic OpenID Connect RP/Social samesite and partitioned tests.
 *
 **/

public class SameSiteTestExpectations {

    public static Class<?> thisClass = SameSiteTestExpectations.class;

    public static enum TestServerExpectations {
        ALL_SERVERS_SUCCEED, OP_GENERIC_FAILURE, CLIENT_GENERIC_FAILURE, CLIENT_REDIRECT_FAILURE, RS_GENERIC_FAILURE, ONLY_BROWSER_SHOWS_FAILURE
    };

    private TestServerExpectations baseTest = TestServerExpectations.ALL_SERVERS_SUCCEED;
    private TestServerExpectations httpClientAppUrlTest = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpRedirectUrlTest = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpAuthEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations httpTokenEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private TestServerExpectations rsHttpValidationEndpointUrlTestResult = TestServerExpectations.ALL_SERVERS_SUCCEED;;
    private String samesiteSetting = Constants.SAMESITE_NONE;
    private boolean partitionedCookie = false;

    public SameSiteTestExpectations() {
        this(TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, Constants.SAMESITE_DISABLED, false);
    }

    public SameSiteTestExpectations(String samesiteSettings, boolean partitionedCookie) {
        this(TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, TestServerExpectations.ALL_SERVERS_SUCCEED, samesiteSettings, partitionedCookie);
    }

    public SameSiteTestExpectations(TestServerExpectations inHttpClientAppUrlTest, TestServerExpectations inHttpRedirectUrlTest, TestServerExpectations inHttpAuthEndpointUrlTestResult, TestServerExpectations inHttpTokenEndpointUrlTestResult, TestServerExpectations inRSHttpValidationEndpointUrlTestResult,
            TestServerExpectations inOPSSORequiresSSLTestResult, TestServerExpectations inClientSSORequiresSSLTestResult, TestServerExpectations inRSSSORequiresSSLTestResult, String samesiteSetting, boolean isPartitionedCookie) {
        this(TestServerExpectations.ALL_SERVERS_SUCCEED, inHttpClientAppUrlTest, inHttpRedirectUrlTest, inHttpAuthEndpointUrlTestResult, inHttpTokenEndpointUrlTestResult, inRSHttpValidationEndpointUrlTestResult, samesiteSetting, isPartitionedCookie);
    }

    public SameSiteTestExpectations(TestServerExpectations inBaseTest, TestServerExpectations inHttpClientAppUrlTest, TestServerExpectations inHttpRedirectUrlTest, TestServerExpectations inHttpAuthEndpointUrlTestResult, TestServerExpectations inHttpTokenEndpointUrlTestResult, TestServerExpectations inRSHttpValidationEndpointUrlTestResult, String inSamesiteSetting, boolean inPartitionedCookie) {
        baseTest = inBaseTest;
        httpClientAppUrlTest = inHttpClientAppUrlTest;
        httpRedirectUrlTest = inHttpRedirectUrlTest;
        httpAuthEndpointUrlTestResult = inHttpAuthEndpointUrlTestResult;
        httpTokenEndpointUrlTestResult = inHttpTokenEndpointUrlTestResult;
        rsHttpValidationEndpointUrlTestResult = inRSHttpValidationEndpointUrlTestResult;
        samesiteSetting = inSamesiteSetting;
        partitionedCookie = inPartitionedCookie;

    }

    public void setBaseTestResult(TestServerExpectations inBaseTest) {
        baseTest = inBaseTest;
    }

    public TestServerExpectations getBaseTestResult() {
        return baseTest;
    }

    public void setHttpClientAppUrlTestResult(TestServerExpectations inHttpClientAppUrlTest) {
        httpClientAppUrlTest = inHttpClientAppUrlTest;
    }

    public TestServerExpectations getHttpClientAppUrlTestResult() {
        return httpClientAppUrlTest;
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

    public void setSamesiteSetting(String inSamesiteSetting) {
        samesiteSetting = inSamesiteSetting;
    }

    public String getSamesiteSetting() {
        return samesiteSetting;
    }

    public void setPartitionedCookie(boolean inPartitionedCookie) {
        partitionedCookie = inPartitionedCookie;
    }

    public boolean getPartitionedCookie() {
        return partitionedCookie;
    }
}
