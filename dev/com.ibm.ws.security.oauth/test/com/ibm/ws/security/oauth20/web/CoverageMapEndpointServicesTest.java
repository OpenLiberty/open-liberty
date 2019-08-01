/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;

import test.common.SharedOutputManager;

/**
 *
 */
public class CoverageMapEndpointServicesTest {

    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);
    private final String HDR_ACCEPT_CHARSET = "Accept-Charset";
    private final String HDR_ACCEPT = "Accept";
    private final String CT = "Content-Type";
    private final String CT_APPLICATION_JSON = "application/json";
    private final String CACHE_CONTROL = "Cache-Control";
    private final String HDR_ETAG = "ETag";
    private final String HDR_IF_MATCH = "If-Match";
    private final String HDR_IF_NONE_MATCH = "If-None-Match";
    private final String HDR_IF_MODIFIED_SINCE = "If-Modified-Since";
    private final String HDR_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    private final JsonArray members = new JsonArray();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUpBefore() throws Exception {
        members.add(new JsonPrimitive(AbstractOidcEndpointServices.addTrailingSlash("https://localhost/app1:5443")));
        members.add(new JsonPrimitive(AbstractOidcEndpointServices.addTrailingSlash("https://localhost/app1:5444")));
        members.add(new JsonPrimitive(AbstractOidcEndpointServices.addTrailingSlash("https://localhost/app2:8020")));
        members.add(new JsonPrimitive(AbstractOidcEndpointServices.addTrailingSlash("https://localhost/app2:8080")));
        // JsonPrimitive registrationEndpoint = new JsonPrimitive("https://localhost:8020/oidc/v10/endpoint/TestProvider/registration");
        // members.add(registrationEndpoint);
    }

    @After
    public void tearDown() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    /**
     * Test that doing a GET on the coverage map end point
     * returns the expected JSON
     */
    @Test
    public void testCoverageMapGet() {
        final String methodName = "testCoverageMapGet";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("UTF-8", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();

        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);

        final List<OidcBaseClient> clientList = new ArrayList<OidcBaseClient>();

        // Add a few test clients to the client list
        OidcBaseClient client1 = new OidcBaseClient("client_id1", "secret1", new JsonArray(), "client_1", "component_1", true);
        JsonArray uriPrefixes = new JsonArray();
        uriPrefixes.add(new JsonPrimitive("https://localhost/app1:5443"));
        uriPrefixes.add(new JsonPrimitive("https://localhost/app1:5444"));
        client1.setTrustedUriPrefixes(uriPrefixes);

        OidcBaseClient client2 = new OidcBaseClient("client_id2", "secret2", new JsonArray(), "client_2", "component_2", true);
        uriPrefixes = new JsonArray();
        uriPrefixes.add(new JsonPrimitive("https://localhost/app2:8020"));
        uriPrefixes.add(new JsonPrimitive("https://localhost/app2:8080"));
        client2.setTrustedUriPrefixes(uriPrefixes);

        clientList.add(client1);
        clientList.add(client2);

        final String cacheCtrHdr = "public, max-age=3600";

        final String expectedJSON = JsonTokenUtil.toJsonFromObj(members);
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getScheme();
                    will(returnValue("https"));
                    allowing(request).getServerName();
                    will(returnValue("localhost"));
                    allowing(request).getServerPort();
                    will(returnValue(8020));
                    allowing(request).getContextPath();
                    will(returnValue("/oidc"));
                    allowing(request).getServletPath();
                    will(returnValue("/endpoint"));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getQueryString();
                    will(returnValue("token_type=Bearer"));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(provider).getID();
                    will(returnValue("TestProvider"));
                    allowing(provider).getCoverageMapSessionMaxAge();
                    will(returnValue(new Long(3600)));
                    allowing(oidcOAuth20ClientProvider).getAll();
                    will(returnValue(clientList));

                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON);
                    one(response).flushBuffer();
                    one(response).addHeader(HDR_ETAG, "\"gsse3HkmZ7mumRNoTN4cGg==\"");
                    one(response).getOutputStream().print(expectedJSON);

                }
            });

            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that a non GET or HEAD
     * request will fail with an OIDCException
     */
    @Test
    public void testRequestNonHeadOrGet() {
        final String methodName = "testRequestNonHeadOrGet";

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_DELETE));
                }
            });

            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc)

        {
            assertEquals("CWWKS1433E: The HTTP method DELETE is not supported for the service CoverageMapEndpointServices.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_SERVER_ERROR, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept types in request other than application/json
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptTypes() {
        final String methodName = "testAcceptTypes";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/rdf+xml", "");
        acceptTable.put("text/html", "");
        final Enumeration<String> headers = acceptTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(headers));
                }
            });

            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals(oidcExc.getErrorDescription(), "The request does not allow for a response of media type \"application/json\"");
            assertEquals(oidcExc.getErrorCode(), OIDCConstants.ERROR_INVALID_REQUEST);
            assertEquals(oidcExc.getHttpStatus(), HttpServletResponse.SC_NOT_ACCEPTABLE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept char-set types in request other than UTF-8
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptCharSet() {
        final String methodName = "testAcceptCharSet";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("ISO-8859-1", "");
        acceptCharsetTable.put("ANSI", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                }
            });

            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals(oidcExc.getErrorDescription(), "The request does not allow for a response that not charset \"UTF-8\"");
            assertEquals(oidcExc.getErrorCode(), OIDCConstants.ERROR_INVALID_REQUEST);
            assertEquals(oidcExc.getHttpStatus(), HttpServletResponse.SC_NOT_ACCEPTABLE);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that if the request misses a query string
     * we will fail with an exception
     */
    @Test
    public void testMissingQueryString() {
        final String methodName = "testMissingQueryString";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("UTF-8", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getQueryString();
                    will(returnValue(null));
                }
            });
            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1434E: Missing required parameters in the request.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that missing a token-type parameter in the query string
     * will fail with an OIDCException
     */
    @Test
    public void testMissingTokenParamInQueryString() {
        final String methodName = "testMissingTokenParamInQueryString";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("UTF-8", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getQueryString();
                    will(returnValue("param1=value1&param2=value2"));
                }
            });
            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1435E: Missing token_type parameter in the request.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that having more than one token-type parameter in the query string
     * will fail with an OIDCException
     */
    @Test
    public void testMultipleTokenParamInQueryString() {
        final String methodName = "testMissingTokenParamInQueryString";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("UTF-8", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getQueryString();
                    will(returnValue("token_type=value1&token_type=value2"));
                }
            });
            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1436E: Request contains multiple token_type parameters.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that having an unrecognized token-type parameter value in the query string
     * will fail with an OIDCException
     */
    @Test
    public void testUnrecognizedTokenValueInQueryString() {
        final String methodName = "testUnrecognizedTokenValueInQueryString";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put("application/json", "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("UTF-8", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final String unrecognizedTokenValue = "Bearer$#";

        try {
            mock.checking(new Expectations() {
                {
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getQueryString();
                    will(returnValue("token_type=" + unrecognizedTokenValue));
                }
            });
            CoverageMapEndpointServices coverageMapServices = new CoverageMapEndpointServices();
            coverageMapServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1437E: Request contains unrecognized token type parameter " + unrecognizedTokenValue + ".", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

}
