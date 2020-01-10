/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.cors;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.webcontainer.cors.config.ConfigUtil;
import com.ibm.ws.webcontainer.cors.config.CorsConfig;

public class CorsHelperTest {
    private final Mockery mock = new JUnit4Mockery();
    private final HttpServletRequest httpRequest = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse httpResponse = mock.mock(HttpServletResponse.class);

    private final CorsHelper corsHelper = new CorsHelper();

    // --------------------------------------------------------
    // --- Constants from http://www.w3.org/TR/cors/#syntax ---

    // -- Request
    /** Request from preFlight(OPTIONS) or cross-origin */
    private static final String REQUEST_HEADER_ORIGIN = "Origin";

    /** Request from preFlight(OPTIONS) */
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";

    /** Request from preFlight(OPTIONS) */
    private static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";

    // -- Response
    /** Response to preFlight(OPTIONS) or cross-origin. Values can be Origin, * or "null" */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    /** Response to preFlight(OPTIONS) or cross-origin. Value can be true */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    /** Optional. Response to preFlight(OPTIONS) or cross-origin. Whitelist of custom headers that browsers are allowed to access. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

    /** Optional. Response to preFlight(OPTIONS). Indicates how long the results of a preFlight request can be cached. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

    /** Response to preFlight(OPTIONS). Which methods can be used in the actual request. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    /** Response to preFlight(OPTIONS). Which headers can be used in the actual request. */
    private static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static List<String> allResponses = Arrays.asList(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS,
                                                             RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE,
                                                             RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS, RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS);

    private static List<String> allRequests = Arrays.asList(REQUEST_HEADER_ORIGIN, REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD, REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    @Test
    public void simpleHandling_basic() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_basic_nonStandardHTTPMethod() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "PATCH", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_postMethod_contentTypeApplication() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("POST"));

                allowing(httpRequest).getContentType();
                will(returnValue("application/x-www-form-urlencoded; text/html; charset=UTF-8"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_postMethod_contentTypeMultipart() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("POST"));

                allowing(httpRequest).getContentType();
                will(returnValue("multipart/form-data; boundary=---------------------------90519140415448"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_postMethod_contentTypeText() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("POST"));

                allowing(httpRequest).getContentType();
                will(returnValue("text/plain; charset=UTF-8"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_notMatch() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("random.gov"));

                allowing(httpRequest).getMethod();
                will(returnValue("GET"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void simpleHandling_allowCredentialsTrue() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, true, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS)), with(equal("true")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);

        mock.assertIsSatisfied();
    }

    @Test
    public void simpleHandling_allowCredentialsFalse() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, false, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);

        mock.assertIsSatisfied();
    }

    @Test
    public void simpleHandling_allowCredentialsTrue_allowAllOrigins() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "*", "GET", null, null, true, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS)), with(equal("true")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);

        mock.assertIsSatisfied();
    }

    @Test
    public void simpleHandling_exposeHeaders() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "GET", null, null, null, "GET, POST, CUSTOM_HEADER");
        corsHelper.setCorsConfig(config);

        mock.checking(creasteSimpleRequestExpectation());

        mock.checking(new Expectations() {
            {
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS)), with(equal("GET, POST, CUSTOM_HEADER")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_basic() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_notMatch() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowedOrigins_wildcard() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "*", "DELETE, GET, POST", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("*")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowCredentialsTrue() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", null, null, true, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS)), with(equal("true")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowCredentialsTrue_allowAllOrigins() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "*", "DELETE, GET, POST", null, null, true, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS)), with(equal("true")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowCredentialsFalse_allowAllOrigins() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "*", "DELETE, GET, POST", null, null, false, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("*")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowedHeaders() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", "CUSTOM_HEADER1,  CUSTOM_HEADER2", null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue("CUSTOM_HEADER2"));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS)), with(equal("CUSTOM_HEADER1, CUSTOM_HEADER2")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_allowedHeaders_notAllowed() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", "CUSTOM_HEADER1,  CUSTOM_HEADER2", null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue("RANDOM_HEADER"));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_maxAge() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", null, 1800L, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("DELETE"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));

                // Response related expectations
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS)), with(equal("DELETE, GET, POST")));
                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE)), with(equal("1800")));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void preflightHandling_requestMethod_notAllowed() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(creastePreflightRequestExpectation());

        mock.checking(new Expectations() {
            {
                // Request related expectations
                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue("HEAD"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                will(returnValue(null));
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void actualCorsRequest_preflight_noRequestMethod() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST, OPTIONS", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("OPTIONS"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                allowing(httpRequest).getHeader(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                will(returnValue(null));

                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void nonCorsRequest_preflight_noRequestHeaderOrigin() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST, OPTIONS", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue(null));

                allowing(httpRequest).getMethod();
                will(returnValue("OPTIONS"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void nonCorsRequest_noRequestHeaderOrigin() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST, OPTIONS", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue(null));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    @Test
    public void actualCorsRequest_notSimple_post() {
        // Create and add config
        CorsConfig config = ConfigUtil.generateCorsConfig("/ibm/api/", "abc.com, ibm.com, xyz.biz", "DELETE, GET, POST, OPTIONS", null, null, null, null);
        corsHelper.setCorsConfig(config);

        mock.checking(new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("POST"));

                allowing(httpRequest).getContentType();
                will(returnValue("application/javascript;"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                oneOf(httpResponse).setHeader(with(equal(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN)), with(equal("abc.com")));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });

        corsHelper.handleCorsRequest(httpRequest, httpResponse);

        // Clean up
        corsHelper.setCorsConfig(config);
    }

    private Expectations creasteSimpleRequestExpectation() {
        return (new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("GET"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });
    }

    private Expectations creastePreflightRequestExpectation() {
        return (new Expectations() {
            {
                allowing(httpRequest).getHeader(REQUEST_HEADER_ORIGIN);
                will(returnValue("abc.com"));

                allowing(httpRequest).getMethod();
                will(returnValue("OPTIONS"));

                allowing(httpRequest).getRequestURI();
                will(returnValue("/ibm/api/doc"));

                for (String header : allResponses) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }

                for (String header : allRequests) {
                    never(httpResponse).setHeader(with(equal(header)), with(any(String.class)));
                }
            }
        });
    }
}
