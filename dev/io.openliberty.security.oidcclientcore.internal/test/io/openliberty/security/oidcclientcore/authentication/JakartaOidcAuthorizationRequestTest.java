/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.oidcclientcore.client.OidcClientConfig;
import io.openliberty.security.oidcclientcore.client.OidcProviderMetadata;
import io.openliberty.security.oidcclientcore.exceptions.OidcClientConfigurationException;
import test.common.SharedOutputManager;

public class JakartaOidcAuthorizationRequestTest extends CommonTestClass {

    protected static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    private final OidcClientConfig config = mockery.mock(OidcClientConfig.class);
    private final OidcProviderMetadata providerMetadata = mockery.mock(OidcProviderMetadata.class);

    private final String CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR = "CWWKS2401E";
    private final String CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI = "CWWKS2404E";

    private final String clientId = "myOidcClientId";
    private final String authorizationEndpointUrl = "https://localhost:8020/oidc/op/authorize";

    private JakartaOidcAuthorizationRequest authzRequest;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        mockery.checking(new Expectations() {
            {
                one(config).getClientId();
                will(returnValue(clientId));
                one(config).getProviderMetadata();
                will(returnValue(providerMetadata));
            }
        });
        authzRequest = new JakartaOidcAuthorizationRequest(request, response, config);
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    // TODO - getAuthorizationEndpoint
//    public void test_getAuthorizationEndpoint_noProviderMetadata() {

    @Test
    public void test_getAuthorizationEndpoint_providerMetadataContainsValidAuthzEndpoint() {
        mockery.checking(new Expectations() {
            {
                one(providerMetadata).getAuthorizationEndpoint();
                will(returnValue(authorizationEndpointUrl));
            }
        });
        try {
            String result = authzRequest.getAuthorizationEndpoint();
            assertEquals(authorizationEndpointUrl, result);
        } catch (Exception e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void test_performDiscovery_missingProviderUri() {
        mockery.checking(new Expectations() {
            {
                one(config).getProviderURI();
                will(returnValue(null));
            }
        });
        try {
            authzRequest.performDiscovery();
            fail("Should have thrown an exception but didn't.");
        } catch (OidcClientConfigurationException e) {
            verifyException(e, CWWKS2401E_OIDC_CLIENT_CONFIGURATION_ERROR + ".+" + CWWKS2404E_OIDC_CLIENT_MISSING_PROVIDER_URI);
        }
    }

}
