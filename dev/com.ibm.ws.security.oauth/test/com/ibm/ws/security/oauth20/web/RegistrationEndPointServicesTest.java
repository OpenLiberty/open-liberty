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
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientSerializer;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

import test.common.SharedOutputManager;

/**
 *
 */
public class RegistrationEndPointServicesTest {
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
    private final String HDR_ETAG = "ETag";
    private final String HDR_IF_MATCH = "If-Match";
    private final String HDR_IF_NONE_MATCH = "If-None-Match";
    private final String HDR_IF_MODIFIED_SINCE = "If-Modified-Since";
    private final String HDR_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    public static final String CT_APPLICATION_JSON = "application/json";
    public static final String CT_APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    public static final String CT_APPLICATION_RDF = "application/rdf+xml";
    public static final String CT_HTML = "text/html";
    public static final String CACHE_CONTROL = "Cache-Control";
    private final String CT = "Content-Type";
    private final String UTF = "UTF-8";

    private final static String clientId = "b0a376ec4b694b67b6baeb0604a312d8";
    private final static String clientId2 = "b01f298b2cd34893bcdec81730946e19";
    private final static String clientSecret = "secret";
    private final static String clientName = "client123";
    private final static String componentId = "TestComponent";
    private final static String redirectUri1 = "https://localhost:8999/resource/redirect1";
    private final static String redirectUri2 = "https://localhost:8999/resource/redirect2";
    private final Enumeration<String> locales = (new Vector()).elements();

    private final String pathInfoBase = "/OIDC/registration/";
    private final String pathInfoClient = pathInfoBase + clientId;
    private final String registrationBase = "https://localhost:8020/oidc/endpoint/OIDC/registration/";

    private static OidcBaseClient client;
    private static List<OidcBaseClient> clients;

    private static Gson GSON = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().registerTypeAdapter(OidcBaseClient.class, new OidcBaseClientSerializer()).create();
        client = setUpClient(clientId); // Set up a single client
        clients = setUpClients(clientId, clientId2); // Set up a single client
    }

    @Before
    public void setUp() {
        client.setRegistrationClientUri(null); // Restore it back to original state
        for (OidcBaseClient client : clients) {
            client.setRegistrationClientUri(null); // Restore it back to original state
        }
    }

    @After
    public void tearDown() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void setUpAfterClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.restoreStreams();
    }

    /**
     * Test that HTTP methods other than
     * HEAD/POST/PUT/DELETE/GET in the
     * request will fail with an OIDCException
     */
    @Test
    public void testRequestNonAllowedMethod() {
        final String methodName = "testRequestNonAllowedMethod";

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue("TRACE"));
                }
            });

            RegistrationEndpointServices registrationServices = new RegistrationEndpointServices();
            registrationServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc)

        {
            assertEquals("CWWKS1433E: The HTTP method TRACE is not supported for the service Registration Endpoint Service.", oidcExc.getErrorDescription());
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
    public void testAcceptTypesGET() {
        final String methodName = "testAcceptTypesGET";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_RDF, "");
        acceptTable.put(CT_HTML, "");
        final Enumeration<String> headers = acceptTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(headers));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response of media type \"application/json\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept char-set types in request other than UTF-8
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptCharSetGET() {
        final String methodName = "testAcceptCharSetGET";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("ISO-8859-1", "");
        acceptCharsetTable.put("ANSI", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response that not charset \"UTF-8\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSingleClientInvalidClientIDGET() {
        final String methodName = "testSingleClientInvalidClientIDGET";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(null));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1424E: The client id " + clientId + " was not found.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_FOUND, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    // TODO
    @Test
    public void testMultipleSpacesInClientIDGET() {
        final String methodName = "testMultipleSpacesInClientIDGET";
        final String clientIdSpace = "0  4.ac      \t - bt ";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();
        final String cacheCtrHdr = "private";
        final List<OidcBaseClient> clientSpace = new ArrayList<OidcBaseClient>();
        clientSpace.add(setUpClient(clientIdSpace));

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase + clientIdSpace));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientIdSpace);
                    will(returnValue(setUpClient(clientIdSpace)));
                    allowing(oidcOAuth20ClientProvider).getAll(request);
                    will(returnValue(clientSpace));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    one(response).flushBuffer();
                    one(response).getOutputStream().print(with(any(String.class)));
                    one(response).setStatus(with(HttpServletResponse.SC_OK));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSpecialCharactersGET() {
        final String methodName = "testSpecialCharactersGET";
        final String clientIdSpecialChars = "client~!@#$%^&amp;*()_+{}|:&lt;&gt;?`-=[];',.'&quot;&quot;/b";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();
        final String cacheCtrHdr = "private";
        final List<OidcBaseClient> clientSpace = new ArrayList<OidcBaseClient>();
        clientSpace.add(setUpClient(clientIdSpecialChars));

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase + clientIdSpecialChars));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientIdSpecialChars);
                    will(returnValue(setUpClient(clientIdSpecialChars)));
                    allowing(oidcOAuth20ClientProvider).getAll(request);
                    will(returnValue(clientSpace));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    one(response).flushBuffer();
                    one(response).getOutputStream().print(with(any(String.class)));
                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase + clientIdSpecialChars)));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testSingleClientValidClientIDGET() {
        final String methodName = "testSingleClientValidClientIDGET";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(client);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase + clientId)));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));

                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).flushBuffer();

                    // Exact local ETag computation too fragile (esp across JVM order variance)
                    // and JMockery too limited and won't allow exact / pattern matching combined
                    // FAT test will catch true internal eTag evaluation (so this test is redundant)
                    // one(response).addHeader(HDR_ETAG, computeExpectedETag(client));
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));

                    one(response).getOutputStream().print(with(jsonString));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1424E: The client id b01f298b2cd34893bcdec81730946e19 was not found.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_FOUND, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAllClientGET() {
        final String methodName = "testAllClientGET";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final JsonArray clientsAsJSON = GSON.toJsonTree(clients).getAsJsonArray();
        JsonObject responseBody = new JsonObject();
        responseBody.add("data", GSON.toJsonTree(clientsAsJSON));
        final String jsonString = responseBody.toString();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase)));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_GET));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).getAll(request);
                    will(returnValue(clients));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));

                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).flushBuffer();

                    // Exact local ETag computation too fragile (esp across JVM order variance)
                    // and JMockery too limited and won't allow exact / pattern matching combined
                    // FAT test will catch true internal eTag evaluation (so this test is redundant)
                    // one(response).addHeader(HDR_ETAG, computeExpectedETag(clientsAsJSON));
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));

                    one(response).getOutputStream().print(with(jsonString));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1424E: The client id b01f298b2cd34893bcdec81730946e19 was not found.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_FOUND, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept types in request other than application/json
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptTypesPOST() {
        final String methodName = "testAcceptTypesPOST";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_RDF, "");
        acceptTable.put(CT_HTML, "");
        final Enumeration<String> headers = acceptTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(headers));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response of media type \"application/json\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept char-set types in request other than UTF-8
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptCharSetPOST() {
        final String methodName = "testAcceptCharSetPOST";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("ISO-8859-1", "");
        acceptCharsetTable.put("ANSI", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response that not charset \"UTF-8\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAcceptContentTypePOST() {
        final String methodName = "testAcceptContentTypePOST";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_HTML));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request must contain content-type of \"application/json\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvalidRequestPOST() {
        final String methodName = "testInvalidRequestPOST";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(null));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1425E: The registration request was made to an incorrect URI.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMalformedPOST() {
        final String methodName = "testMalformedPOST";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(RegistrationEndPointServicesTest.client) + "<html>bad</html>";// mal-formed post body
        StringReader sr = new StringReader(jsonString);
        final BufferedReader br = new BufferedReader(sr);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase)));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).exists(clientId);
                    will(returnValue(false));
                    allowing(oidcOAuth20ClientProvider).put(with(any(OidcBaseClient.class)));
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getReader();
                    will(returnValue(br));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
            fail("The method did not throw an exception but it should have");

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1428E: The request body is malformed.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testClientIdExistsPOST() {
        final String methodName = "testClientIdExistsPOST";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(client);
        StringReader sr = new StringReader(jsonString);
        final BufferedReader br = new BufferedReader(sr);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase)));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).exists(clientId);
                    will(returnValue(true));
                    allowing(oidcOAuth20ClientProvider).put(with(any(OidcBaseClient.class)));
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getReader();
                    will(returnValue(br));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
            fail("The method did not throw an exception but it should have");

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1429E: Client id " + clientId + " already exists.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidPOST() {
        final String methodName = "testValidPOST";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(RegistrationEndPointServicesTest.client);
        StringReader sr = new StringReader(jsonString);
        final BufferedReader br = new BufferedReader(sr);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase)));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).exists(clientId);
                    will(returnValue(false));
                    allowing(oidcOAuth20ClientProvider).put(with(any(OidcBaseClient.class)));
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getReader();
                    will(returnValue(br));

                    one(response).setStatus(with(HttpServletResponse.SC_CREATED));
                    one(response).setHeader(CACHE_CONTROL, cacheCtrHdr);
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).flushBuffer();

                    // Exact local ETag computation too fragile (esp across JVM order variance)
                    // and JMockery too limited and won't allow exact / pattern matching combined
                    // FAT test will catch true internal eTag evaluation (so this test is redundant)
                    // one(response).addHeader(HDR_ETAG, computeExpectedETag(client));
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));

                    one(response).getOutputStream().print(with(any(String.class)));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept types in request other than application/json
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptTypesPUT() {
        final String methodName = "testAcceptTypesPUT";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_RDF, "");
        acceptTable.put(CT_HTML, "");
        final Enumeration<String> headers = acceptTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(headers));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response of media type \"application/json\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that accept char-set types in request other than UTF-8
     * will fail with a OIDCException
     */
    @Test
    public void testAcceptCharSetPUT() {
        final String methodName = "testAcceptCharSetPUT";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put("ISO-8859-1", "");
        acceptCharsetTable.put("ANSI", "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request does not allow for a response that not charset \"UTF-8\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_ACCEPTABLE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testAcceptContentTypePUT() {
        final String methodName = "testAcceptContentTypePUT";

        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_HTML));
                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc) {
            assertEquals("The request must contain content-type of \"application/json\"", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvalidRequestPUT() {
        final String methodName = "testInvalidRequestPUT";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();

        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1426E: The PUT operation failed as the request did not contain the client_id parameter.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvalidClientIDPUT() {
        final String methodName = "testInvalidClientIDPUT";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(null));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1424E: The client id " + clientId + " was not found.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_FOUND, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testMalformedPUT() {
        final String methodName = "testMalformedPUT";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(RegistrationEndPointServicesTest.client) + "<html>bad</html>";// mal-formed post body
        StringReader sr = new StringReader(jsonString);
        final BufferedReader br = new BufferedReader(sr);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase + clientId)));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(client));
                    allowing(oidcOAuth20ClientProvider).put(with(any(OidcBaseClient.class)));
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getReader();
                    will(returnValue(br));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);
            fail("The method did not throw an exception but it should have");

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1428E: The request body is malformed.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT_METADATA, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidPUT() {
        final String methodName = "testValidPUT";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        final String cacheCtrHdr = "private";
        final String jsonString = GSON.toJson(RegistrationEndPointServicesTest.client);
        StringReader sr = new StringReader(jsonString);
        final BufferedReader br = new BufferedReader(sr);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_PUT));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(request).getRequestURL();
                    will(returnValue(new StringBuffer(registrationBase + clientId)));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(client));
                    allowing(oidcOAuth20ClientProvider).update(with(any(OidcBaseClient.class)));
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getReader();
                    will(returnValue(br));

                    one(response).setStatus(with(HttpServletResponse.SC_OK));
                    one(response).setHeader(CT, CT_APPLICATION_JSON_UTF8);
                    one(response).flushBuffer();

                    // Exact local ETag computation too fragile (esp across JVM order variance)
                    // and JMockery too limited and won't allow exact / pattern matching combined
                    // FAT test will catch true internal eTag evaluation (so this test is redundant)
                    // one(response).addHeader(HDR_ETAG, computeExpectedETag(client));
                    one(response).addHeader(with(any(String.class)), with(any(String.class)));

                    one(response).getOutputStream().print(with(any(String.class)));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvalidRequestDELETE() {
        final String methodName = "testInvalidRequestDELETE";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_DELETE));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1426E: The DELETE operation failed as the request did not contain the client_id parameter.", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testInvalidClientIDDELETE() {
        final String methodName = "testInvalidClientIDDELETE";
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_DELETE));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(null));

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (OidcServerException oidcExc) {
            assertEquals("CWWKS1427E: The DELETE operation failed as the request contains an invalid client_id parameter " + clientId + ".", oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_CLIENT, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_NOT_FOUND, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testValidDELETE() {
        final String methodName = "testValidDELETE";
        final OidcOAuth20ClientProvider oidcOAuth20ClientProvider = mock.mock(OidcOAuth20ClientProvider.class);
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoClient));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_DELETE));
                    allowing(provider).getClientProvider();
                    will(returnValue(oidcOAuth20ClientProvider));
                    allowing(oidcOAuth20ClientProvider).get(clientId);
                    will(returnValue(client));
                    allowing(request).getHeaders(HDR_IF_NONE_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MODIFIED_SINCE);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_MATCH);
                    will(returnValue(null));
                    allowing(request).getHeaders(HDR_IF_UNMODIFIED_SINCE);
                    will(returnValue(null));

                    one(oidcOAuth20ClientProvider).delete(clientId);
                    one(response).setStatus(with(HttpServletResponse.SC_NO_CONTENT));
                    one(response).flushBuffer();

                }
            });

            RegistrationEndpointServices registrationEndpointServices = new RegistrationEndpointServices();
            registrationEndpointServices.handleEndpointRequest(provider, request, response);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Test that HTTP POST will fail
     * with an OIDCException when
     * provided with an empty client body
     */
    @Test
    public void testRequestEmptyClientBody() {
        final String methodName = "testRequestEmptyClientBody";
        Hashtable<String, String> acceptTable = new Hashtable<String, String>();
        acceptTable.put(CT_APPLICATION_JSON, "");
        final Enumeration<String> acceptHeaders = acceptTable.keys();

        Hashtable<String, String> acceptCharsetTable = new Hashtable<String, String>();
        acceptCharsetTable.put(UTF, "");
        final Enumeration<String> acceptCharsetHeaders = acceptCharsetTable.keys();
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getLocales();
                    will(returnValue(locales));
                    allowing(request).getPathInfo();
                    will(returnValue(pathInfoBase));
                    allowing(request).getMethod();
                    will(returnValue(AbstractOidcEndpointServices.HTTP_METHOD_POST));
                    allowing(request).getHeaders(HDR_ACCEPT);
                    will(returnValue(acceptHeaders));
                    allowing(request).getHeaders(HDR_ACCEPT_CHARSET);
                    will(returnValue(acceptCharsetHeaders));
                    allowing(request).getContentType();
                    will(returnValue(CT_APPLICATION_JSON));
                    allowing(request).getReader();
                    will(returnValue(new BufferedReader(new StringReader(""))));
                }
            });

            RegistrationEndpointServices registrationServices = new RegistrationEndpointServices();
            registrationServices.handleEndpointRequest(provider, request, response);
        } catch (OidcServerException oidcExc)

        {
            assertEquals("CWWKS1463E: The OpenID Connect registration request does not contain a client. Ensure that the request body is not empty and contains a client encoded in JSON format.",
                    oidcExc.getErrorDescription());
            assertEquals(OIDCConstants.ERROR_INVALID_REQUEST, oidcExc.getErrorCode());
            assertEquals(HttpServletResponse.SC_BAD_REQUEST, oidcExc.getHttpStatus());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     *
     */
    private static OidcBaseClient setUpClient(String clientId) {
        JsonArray redirectUris = new JsonArray();
        redirectUris.add(new JsonPrimitive(redirectUri1));
        redirectUris.add(new JsonPrimitive(redirectUri2));
        OidcBaseClient client = new OidcBaseClient(clientId, clientSecret, redirectUris, clientName, componentId, true);
        JsonArray grantTypes = new JsonArray();
        grantTypes.add(new JsonPrimitive("authorization_code"));
        grantTypes.add(new JsonPrimitive("client_credentials"));
        grantTypes.add(new JsonPrimitive("implicit"));
        grantTypes.add(new JsonPrimitive("refresh_token"));
        grantTypes.add(new JsonPrimitive("urn:ietf:params:oauth:grant-type:jwt-bearer"));
        client.setGrantTypes(grantTypes);

        JsonArray responseTypes = new JsonArray();
        responseTypes.add(new JsonPrimitive("code"));
        responseTypes.add(new JsonPrimitive("token"));
        responseTypes.add(new JsonPrimitive("id_token token"));
        client.setResponseTypes(responseTypes);

        JsonArray postLogoutRedirectUris = new JsonArray();
        postLogoutRedirectUris.add(new JsonPrimitive("https://localhost:9000/logout/"));
        postLogoutRedirectUris.add(new JsonPrimitive("https://localhost:9001/exit/"));
        client.setPostLogoutRedirectUris(postLogoutRedirectUris);

        client.setApplicationType("web");
        client.setSubjectType("public");
        client.setPreAuthorizedScope("openid profile email general");
        client.setTokenEndpointAuthMethod("client_secret_basic");
        client.setScope("openid profile email general");
        client.setIntrospectTokens(true);
        return client;
    }

    /**
     * @param clientid3
     * @param clientid22
     * @return
     */
    private static List<OidcBaseClient> setUpClients(String clientid, String clientid2) {

        List<OidcBaseClient> clients = new ArrayList<OidcBaseClient>();
        clients.add(setUpClient(clientid));
        clients.add(setUpClient(clientid2));
        return clients;

    }
}
