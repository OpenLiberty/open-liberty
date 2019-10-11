/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.oauth20.api.OAuth20ProviderConfiguration;
import com.ibm.ws.security.oauth20.util.OAuth20Parameter;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.openidconnect.web.MockServletRequest;

/**
 *
 */
public class OidcDiscoveryProviderConfigTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final OAuth20ProviderConfiguration oAuthProviderConfig = mock.mock(OAuth20ProviderConfiguration.class, "oAuthProviderConfig");

    private static final String CONFIG_PROVIDER_ID = "oAuthProviderId";
    private static final String CONFIG_SCHEME = "http";
    private static final String CONFIG_SERVER_NAME = "localhost";
    private static final int CONFIG_SERVER_PORT = 9080;
    private static final String CONFIG_CONTEXT_PATH = "/myCtxPath";
    private static final String CONFIG_SERVLET_PATH = "/myServletPath";

    private static final String CONFIG_FULL_SERVLET_PATH =
                    CONFIG_SCHEME + "://" + CONFIG_SERVER_NAME + ":" + CONFIG_SERVER_PORT + CONFIG_CONTEXT_PATH + CONFIG_SERVLET_PATH;

    private static final String CONFIG_ISSUER_ID = CONFIG_FULL_SERVLET_PATH + "/" + CONFIG_PROVIDER_ID;

    private static final MockServletRequest request = new MockServletRequest() {
        @Override
        public String getScheme() {
            return CONFIG_SCHEME;
        }

        @Override
        public String getServerName() {
            return CONFIG_SERVER_NAME;
        }

        @Override
        public int getServerPort() {
            return CONFIG_SERVER_PORT;
        }

        @Override
        public String getServletPath() {
            return CONFIG_SERVLET_PATH;
        }

        @Override
        public String getContextPath() {
            return CONFIG_CONTEXT_PATH;
        }

    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testCalculatedIssueId() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getIssuerId();

        String expectedValue = CONFIG_ISSUER_ID;

        assertEquals("did not get expected calculated issuerId", expectedValue, testValue);
    }

    @Test
    public void testCalculatedAuthorizationEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_AUTHORIZATION_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.authorize.name())
                        .toString();

        assertEquals("did not get expected calculated authorize URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedIntrospectionEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_INTROSPECTION_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.introspect.name())
                        .toString();

        assertEquals("did not get expected calculated introspect URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedTokenEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_TOKEN_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.token.name())
                        .toString();

        assertEquals("did not get expected calculated token URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedUserinfoEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_USERINFO_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.userinfo.name())
                        .toString();

        assertEquals("did not get expected calculated userinfo URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedRegistrationEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_REGISTRATION_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.registration.name())
                        .toString();

        assertEquals("did not get expected calculated registration URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedCheckSessionIframeEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_CHECK_SESSION_IFRAME_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.check_session_iframe.name())
                        .toString();

        assertEquals("did not get expected calculated check_session_iframe URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedEndSessionEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_END_SESSION_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.end_session.name())
                        .toString();

        assertEquals("did not get expected calculated end_session URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedCoverageMapEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_COVERAGE_MAP_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.coverage_map.name())
                        .toString();

        assertEquals("did not get expected calculated coverage_map URL", expectedValue, testValue);
    }

    @Test
    public void testCalculatedProxyEndpoint() {
        OidcDiscoveryProviderConfig oidcProviderCfg = constructTestOidcDiscoveryProviderConfig();

        String testValue = oidcProviderCfg.getEndpoint(OIDCConstants.KEY_OIDC_PROXY_EP_QUAL);

        String expectedValue = (new StringBuffer())
                        .append(CONFIG_ISSUER_ID)
                        .append("/")
                        .append(EndpointType.proxy.name())
                        .toString();

        assertEquals("did not get expected calculated proxy URL", expectedValue, testValue);
    }

    private OidcDiscoveryProviderConfig constructTestOidcDiscoveryProviderConfig() {
        initializeEmptyMock();

        return new OidcDiscoveryProviderConfig(CONFIG_PROVIDER_ID, request);
    }

    private void initializeEmptyMock() {
        mock.checking(new Expectations() {
            {
                final List<OAuth20Parameter> oauthParams = new ArrayList<OAuth20Parameter>() {};

                allowing(oAuthProviderConfig).getParameters();
                will(returnValue(oauthParams));
            }
        });
    }

    private static <T> List<T> getInitializedList(final T value) {
        return new ArrayList<T>() {
            {
                add(value);
            }
        };
    }
}
