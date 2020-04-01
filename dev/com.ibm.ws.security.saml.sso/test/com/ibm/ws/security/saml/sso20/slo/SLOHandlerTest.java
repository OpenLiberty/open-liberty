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
package com.ibm.ws.security.saml.sso20.slo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.common.random.RandomUtils;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SLOHandlerTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.saml.*=all");

    @Rule
    public TestRule managerRule = outputMgr;

    private final String CWWKS5210E_ERROR_HANDLING_LOGOUT_REQUEST = "CWWKS5210E";
    private final String CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE = "CWWKS5211E";
    private final String CWWKS5212E_LOGOUT_REQUEST_MISSING_SSO_REQUEST = "CWWKS5212E";
    private final String CWWKS5213E_LOGOUT_REQUEST_MISSING_PARAMETERS = "CWWKS5213E";

    final HttpServletRequest request = mockery.mock(HttpServletRequest.class);
    final HttpServletResponse response = mockery.mock(HttpServletResponse.class);
    final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    final SsoRequest ssoRequest = mockery.mock(SsoRequest.class);

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    public interface MockInterface {
        void handleLogoutEndpointRequest() throws SamlException;

        void handleLogoutResponseFromIdp() throws SamlException, IOException;

        void handleLogoutRequestFromIdp() throws SamlException;
    }

    SLOHandler handler = null;

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        handler = new SLOHandler();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    /************************************** handleRequest **************************************/

    /**
     * Tests:
     * - SsoRequest argument: Null
     * - Parameter map argument: Null
     * Expects:
     * - SamlException with CWWKS5212E message saying request is missing SSO request information
     */
    @Test
    public void test_handleRequest_nullSamlRequest_nullParams() throws Exception {
        Map<String, Object> params = null;

        try {
            handler.handleRequest(request, response, null, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5212E_LOGOUT_REQUEST_MISSING_SSO_REQUEST);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SsoRequest argument: Null
     * - Parameter map argument: Empty map
     * Expects:
     * - SamlException with CWWKS5212E message saying request is missing SSO request information
     */
    @Test
    public void test_handleRequest_nullSamlRequest_emptyParams() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();

        try {
            handler.handleRequest(request, response, null, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5212E_LOGOUT_REQUEST_MISSING_SSO_REQUEST);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SsoRequest argument: Null
     * - Parameter map argument: Includes minimum required parameters
     * Expects:
     * - SamlException with CWWKS5212E message saying request is missing SSO request information
     */
    @Test
    public void test_handleRequest_nullSamlRequest() throws Exception {
        Map<String, Object> params = getMinimumParameters();

        try {
            handler.handleRequest(request, response, null, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5212E_LOGOUT_REQUEST_MISSING_SSO_REQUEST);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SsoRequest argument: Valid object
     * - Parameter map argument: Null
     * Expects:
     * - SamlException with CWWKS5211E message saying SSO service info couldn't be found
     */
    @Test
    public void test_handleRequest_nullParameters() throws Exception {
        Map<String, Object> params = null;

        try {
            handler.handleRequest(request, response, ssoRequest, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SsoRequest argument: Valid object
     * - Parameter map argument: Empty map
     * Expects:
     * - SamlException with CWWKS5211E message saying SSO service info couldn't be found
     */
    @Test
    public void test_handleRequest_emptyParams() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();

        try {
            handler.handleRequest(request, response, ssoRequest, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is logout endpoint request
     * Expects:
     * - Request will be handled as a logout endpoint request
     */
    @Test
    public void test_handleRequest_logoutRequest() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutEndpointRequest(HttpServletRequest request, HttpServletResponse response, SsoSamlService ssoService, Map params) throws SamlException {
                mockInterface.handleLogoutEndpointRequest();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.LOGOUT);
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutEndpointRequest();
            }
        });

        handler.handleRequest(request, response, ssoRequest, params);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    @Test
    public void doesNothing() {}

    /**
     * Tests:
     * - Request is logout endpoint request
     * - Exception thrown while handling logout request
     * Expects:
     * - Request will be handled as a logout endpoint request
     * - SamlException with CWWKS5210E message saying there was an error handling the request, with the exception message included
     */
    @Test
    public void test_handleRequest_logoutRequest_throwsException() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutEndpointRequest(HttpServletRequest request, HttpServletResponse response, SsoSamlService ssoService, Map params) throws SamlException {
                mockInterface.handleLogoutEndpointRequest();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.LOGOUT);
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutEndpointRequest();
                will(throwException(new SamlException(defaultExceptionMsg)));
            }
        });

        try {
            handler.handleRequest(request, response, ssoRequest, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5210E_ERROR_HANDLING_LOGOUT_REQUEST + ".+" + Pattern.quote(defaultExceptionMsg));
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is logout response from IdP
     * Expects:
     * - Request will be handled as a logout response from IdP
     */
    @Test
    public void test_handleRequest_logoutResponseFromIdp() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutResponseFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest,
                                             SsoSamlService ssoService) throws SamlException, IOException {
                mockInterface.handleLogoutResponseFromIdp();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.ACS);
        setSamlResponseParameterExpectation("some SAML response");
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutResponseFromIdp();
            }
        });

        handler.handleRequest(request, response, ssoRequest, params);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is logout response from IdP
     * - Exception thrown while handling logout request
     * Expects:
     * - Request will be handled as a logout response from IdP
     * - SamlException with CWWKS5210E message saying there was an error handling the request, with the exception message included
     */
    @Test
    public void test_handleRequest_logoutResponseFromIdp_throwsException() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutResponseFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest,
                                             SsoSamlService ssoService) throws SamlException, IOException {
                mockInterface.handleLogoutResponseFromIdp();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.ACS);
        setSamlResponseParameterExpectation("some SAML response");
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutResponseFromIdp();
                will(throwException(new SamlException(defaultExceptionMsg)));
            }
        });

        try {
            handler.handleRequest(request, response, ssoRequest, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5210E_ERROR_HANDLING_LOGOUT_REQUEST + ".+" + Pattern.quote(defaultExceptionMsg));
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is logout request from IdP
     * Expects:
     * - Request will be handled as a logout request from IdP
     */
    @Test
    public void test_handleRequest_logoutRequestFromIdp() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutRequestFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest,
                                            SsoSamlService ssoService) throws SamlException {
                mockInterface.handleLogoutRequestFromIdp();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.ACS);
        setSamlResponseParameterExpectation(null);
        setSamlRequestParameterExpectation("some SAML request");
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutRequestFromIdp();
            }
        });

        handler.handleRequest(request, response, ssoRequest, params);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is logout request from IdP
     * - Exception thrown while handling logout request
     * Expects:
     * - Request will be handled as a logout request from IdP
     * - SamlException with CWWKS5210E message saying there was an error handling the request, with the exception message included
     */
    @Test
    public void test_handleRequest_logoutRequestFromIdp_throwsException() throws Exception {
        handler = new SLOHandler() {
            @Override
            void handleLogoutRequestFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest,
                                            SsoSamlService ssoService) throws SamlException {
                mockInterface.handleLogoutRequestFromIdp();
            }
        };

        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, Constants.EndpointType.ACS);
        setSamlResponseParameterExpectation(null);
        setSamlRequestParameterExpectation("some SAML request");
        mockery.checking(new Expectations() {
            {
                one(mockInterface).handleLogoutRequestFromIdp();
                will(throwException(new SamlException(defaultExceptionMsg)));
            }
        });

        try {
            handler.handleRequest(request, response, ssoRequest, params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5210E_ERROR_HANDLING_LOGOUT_REQUEST + ".+" + Pattern.quote(defaultExceptionMsg));
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request is of unknown type (not a logout endpoint request, not an IdP logout request or response)
     * Expects:
     * - Nothing will happen
     */
    @Test
    public void test_handleRequest_unknownRequestType() throws Exception {
        Map<String, Object> params = getMinimumParameters();

        setProviderIdAndEndpointTypeExpectations(null, null);
        setSamlResponseParameterExpectation(null);
        setSamlRequestParameterExpectation(null);

        handler.handleRequest(request, response, ssoRequest, params);

        // Effectively nothing should happen
        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** verifySsoRequestNotNull **************************************/

    /**
     * Tests:
     * - SsoRequest argument: Null
     * Expects:
     * - SamlException with CWWKS5212E message saying request is missing SSO request information
     */
    @Test
    public void test_verifySsoRequestNotNull_nullSsoRequest() throws Exception {
        try {
            handler.verifySsoRequestNotNull(null);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5212E_LOGOUT_REQUEST_MISSING_SSO_REQUEST);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SsoRequest argument: Valid object
     * Expects:
     * - No exceptions, no error messages
     */
    @Test
    public void test_verifySsoRequestNotNull_nonEmptyMap() throws Exception {
        handler.verifySsoRequestNotNull(ssoRequest);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** getSsoSamlServiceParameter **************************************/

    /**
     * Tests:
     * - Request parameter map: Null
     * Expects:
     * - SamlException with CWWKS5211E message saying SSO service info couldn't be found
     */
    @Test
    public void test_getSsoSamlServiceParameter_nullMap() throws Exception {
        Map<String, Object> params = null;

        try {
            handler.getSsoSamlServiceParameter(params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request parameter map: Empty
     * Expects:
     * - SamlException with CWWKS5211E message saying SSO service info couldn't be found
     */
    @Test
    public void test_getSsoSamlServiceParameter_emptyMap() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();

        try {
            handler.getSsoSamlServiceParameter(params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request parameter map: Non empty, missing required value
     * Expects:
     * - SamlException with CWWKS5211E message saying SSO service info couldn't be found
     */
    @Test
    public void test_getSsoSamlServiceParameter_nonEmptyMap_missingValue() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("key1", "value1");
        params.put("key2", "value2");

        try {
            handler.getSsoSamlServiceParameter(params);
            fail("Should have thrown SamlException but did not.");
        } catch (SamlException e) {
            verifyException(e, CWWKS5211E_LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE);
        }

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request parameter map: Non empty, contains required value
     * Expects:
     * - No exceptions, no error messages
     */
    @Test
    public void test_getSsoSamlServiceParameter() throws Exception {
        Map<String, Object> params = getMinimumParameters();

        handler.getSsoSamlServiceParameter(params);

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** isLogoutEndpointRequest **************************************/

    /**
     * Tests:
     * - Request type: Null
     * Expects:
     * - Request is NOT a logout endpoint request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutEndpointRequest_nullRequestType() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getType();
                will(returnValue(null));
            }
        });

        assertFalse("Null endpoint type should not be considered a logout endpoint request.", handler.isLogoutEndpointRequest(ssoRequest));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request type: Any endpoint type other than logout
     * Expects:
     * - Request is NOT a logout endpoint request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutEndpointRequest_nonLogoutRequestType() throws Exception {
        final Constants.EndpointType type = RandomUtils.getRandomSelection(Constants.EndpointType.ACS, Constants.EndpointType.REQUEST,
                                                                           Constants.EndpointType.RESPONSE, Constants.EndpointType.SAMLMETADATA,
                                                                           Constants.EndpointType.SLO);
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getType();
                will(returnValue(type));
            }
        });

        assertFalse("Endpoint type [" + type + "] should not be considered a logout endpoint request.", handler.isLogoutEndpointRequest(ssoRequest));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - Request type: Logout
     * Expects:
     * - Request is a logout endpoint request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutEndpointRequest_logoutRequestType() throws Exception {
        final Constants.EndpointType type = Constants.EndpointType.LOGOUT;
        mockery.checking(new Expectations() {
            {
                one(ssoRequest).getType();
                will(returnValue(type));
            }
        });

        assertTrue("Endpoint type [" + type + "] should be considered a logout endpoint request.", handler.isLogoutEndpointRequest(ssoRequest));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** handleLogoutEndpointRequest **************************************/

    // TODO

    /************************************** isLogoutResponseFromIdP **************************************/

    /**
     * Tests:
     * - SAML response parameter: Null
     * Expects:
     * - Request is NOT a logout response
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutResponseFromIdP_nullResponseParam() throws Exception {
        setSamlResponseParameterExpectation(null);

        assertFalse("Request with null SAML response parameter should not be considered a logout response.", handler.isLogoutResponseFromIdP(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SAML response parameter: Empty
     * Expects:
     * - Request is a logout response
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutResponseFromIdP_emptyResponseParam() throws Exception {
        setSamlResponseParameterExpectation("");

        assertTrue("Request with empty, but present, SAML response parameter should be considered a logout response.", handler.isLogoutResponseFromIdP(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SAML response parameter: Non-null
     * Expects:
     * - Request is a logout response
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutResponseFromIdP_nonNullResponseParam() throws Exception {
        setSamlResponseParameterExpectation("some value");

        assertTrue("Request with non-null SAML response parameter should be considered a logout response.", handler.isLogoutResponseFromIdP(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** handleLogoutResponseFromIdp **************************************/

    // TODO

    /************************************** isLogoutRequestFromIdp **************************************/

    /**
     * Tests:
     * - SAML request parameter: Null
     * Expects:
     * - Request is NOT a logout request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutRequestFromIdp_nullRequestParam() throws Exception {
        setSamlRequestParameterExpectation(null);

        assertFalse("Request with null SAML request parameter should not be considered a logout request.", handler.isLogoutRequestFromIdp(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SAML request parameter: Empty
     * Expects:
     * - Request is a logout request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutRequestFromIdp_emptyRequestParam() throws Exception {
        setSamlRequestParameterExpectation("");

        assertTrue("Request with empty, but present, SAML request parameter should be considered a logout request.", handler.isLogoutRequestFromIdp(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /**
     * Tests:
     * - SAML request parameter: Non-null
     * Expects:
     * - Request is a logout request
     * - No exceptions, no error messages
     */
    @Test
    public void test_isLogoutRequestFromIdp_nonNullRequestParam() throws Exception {
        setSamlRequestParameterExpectation("some value");

        assertTrue("Request with non-null SAML request parameter should be considered a logout request.", handler.isLogoutRequestFromIdp(request));

        verifyNoLogMessage(outputMgr, MSG_BASE);
    }

    /************************************** handleLogoutRequestFromIdp **************************************/

    // TODO

    /************************************** Helper methods **************************************/

    Map<String, Object> getMinimumParameters() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.KEY_SAML_SERVICE, ssoService);
        return params;
    }

    void setProviderIdAndEndpointTypeExpectations(final String providerId, final Constants.EndpointType endpointType) {
        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getProviderId();
                will(returnValue(providerId));
                one(ssoRequest).getType();
                will(returnValue(endpointType));
            }
        });
    }

    void setSamlResponseParameterExpectation(final String samlResponse) {
        mockery.checking(new Expectations() {
            {
                allowing(request).getParameter(Constants.SAMLResponse);
                will(returnValue(samlResponse));
            }
        });
    }

    void setSamlRequestParameterExpectation(final String samlRequest) {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(Constants.SAMLRequest);
                will(returnValue(samlRequest));
            }
        });
    }

}