/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.web;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.ws.security.openidconnect.client.Cache;
import com.ibm.ws.security.openidconnect.client.internal.OidcClientConfigImpl;
import com.ibm.ws.security.openidconnect.client.internal.OidcClientImpl;
import com.ibm.ws.security.openidconnect.clients.common.ClientConstants;
import com.ibm.ws.security.openidconnect.clients.common.HashUtils;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientUtil;
import com.ibm.ws.security.openidconnect.clients.common.OidcUtil;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.ReferrerURLCookieHandler;
import com.ibm.ws.webcontainer.security.SSOCookieHelperImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

import test.common.SharedOutputManager;

public class OidcRedirectServletTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    @Rule
    public final TestName testName = new TestName();

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    public interface MockInterface {
        void mockCacheRequestParameter();
    }

    private final HttpServletRequest req = mock.mock(HttpServletRequest.class, "req");
    private final HttpServletResponse resp = mock.mock(HttpServletResponse.class, "resp");
    private final WebAppSecurityConfig webAppSecurityConfig = mock.mock(WebAppSecurityConfig.class, "webAppSecurityConfig");
    private final MockInterface mockInterface = mock.mock(MockInterface.class, "mockInterface");
    private final OidcClientImpl oidcClientImpl = mock.mock(OidcClientImpl.class, "oidcClientImpl");
    private final OidcClientConfigImpl oidcClientConfigImpl = mock.mock(OidcClientConfigImpl.class, "oidcClientConfigImpl");
    private final Cache cache = mock.mock(Cache.class, "cache");

    private final String REQUEST_URL = "https://1.2.3.4:9080/snoop";
    private final String OIDC_CODE = "2Zqff6lzUTkEm956JROBcFCdC0T0mI";
    private final String OIDC_ID_TOKEN = "{\"iss\":\"iss_value\",\"aud\":\"client01\"}";
    private final String OIDC_STATE = "RVNnQ0BKKjVxOnlKz7Behttps://1.2.3.4:9080/snoop";
    private final String OIDC_STATE_SHORT = "RVNnQ0BKKjVxO";
    private final String OIDC_SESSION_STATE = "9JVoiH3nocnNDoPH3nfnp1NfpNF3=.NMFainCN0ngoi";

    @Before
    public void setUp() throws Exception {
        OidcClientUtil.setReferrerURLCookieHandler(null);// reset the ReferrerURLCookieHandler which was set in previous tests
        OidcClientUtil.setWebAppSecurityConfig(webAppSecurityConfig);
        mock.checking(new Expectations() {
            {
                allowing(webAppSecurityConfig).getSSORequiresSSL();
                will(returnValue(true));
                allowing(webAppSecurityConfig).getHttpOnlyCookies();
                will(returnValue(true));
                allowing(webAppSecurityConfig).getSSODomainList();//
                will(returnValue(null)); //
                allowing(webAppSecurityConfig).getSSOUseDomainFromURL();//
                will(returnValue(false));
                allowing(webAppSecurityConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecurityConfig)));
                allowing(webAppSecurityConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecurityConfig)));
                allowing(req).getRequestURL();
                will(returnValue(new StringBuffer(REQUEST_URL)));
                atLeast(0).of(resp).addCookie(with(any(Cookie.class)));
                allowing(webAppSecurityConfig).getSameSiteCookie();
                will(returnValue("Disabled"));

            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @Test
    public void testConstructor() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        assertNotNull("There must be an oidc redirect servlet", redirectServlet);
    }

    @Test
    public void testDoGet_error() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        try {
            mockParamValue(Constants.CODE, OIDC_CODE);
            mockParamValue(Constants.STATE, "somestate");
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            });
            redirectServlet.doGet(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_noState() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        try {
            mockParamValue(Constants.STATE, null);
            mock.checking(new Expectations() {
                {
                    allowing(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_error_short_state() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        try {
            mockParamValue(Constants.STATE, OIDC_STATE_SHORT);
            mockParamValue(Constants.CODE, OIDC_CODE);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_error_no_cookie() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        try {
            mockOidcStateAndCode();
            mock.checking(new Expectations() {
                {
                    allowing(req).getCookies();
                    will(returnValue(null));
                    allowing(resp).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_nullCode_accessDenied() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        final String errorMsg = "CWWKS1711E: The request has been denied by the user, or another error occurred that resulted in denial of the request.";
        try {
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, null);
            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.ERROR, OAuth20Exception.ACCESS_DENIED);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(resp).sendError(HttpServletResponse.SC_FORBIDDEN, errorMsg);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_nullCode_invalidScopeError() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        String error = OAuth20Exception.INVALID_SCOPE;
        final String query = Constants.ERROR + "=" + error;
        try {
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, null);
            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.ERROR, error);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(resp).sendError(HttpServletResponse.SC_FORBIDDEN, query);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_nullCode_otherError() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        String error = OAuth20Exception.INVALID_GRANT;
        final String query = Constants.ERROR + "=" + OAuth20Exception.ACCESS_DENIED;
        try {
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, null);
            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.SESSION_STATE, OIDC_SESSION_STATE);
            mockParamValue(Constants.ERROR, error);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(resp).sendError(HttpServletResponse.SC_FORBIDDEN, query);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_nullCode_nullError() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        final String query = Constants.ERROR + "=" + OAuth20Exception.ACCESS_DENIED;

        try {
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, null);
            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.ERROR, null);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(resp).sendError(HttpServletResponse.SC_FORBIDDEN, query);
                }
            });
            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_validCode() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        redirectServlet.activatedOidcClientImpl = oidcClientImpl;

        final String OIDC_CLIENT_ID = "snoop";

        try {
            final Map<String, String[]> map = new HashMap<String, String[]>();
            map.put("requestUrl", new String[] { REQUEST_URL });
            final Hashtable<String, String> table = new Hashtable<String, String>();
            table.put("requestUrl", REQUEST_URL);

            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.SESSION_STATE, OIDC_SESSION_STATE);
            mockParamValue(Constants.CODE, OIDC_CODE);
            mockParamValue(Constants.ID_TOKEN, null);
            mockParamValue(Constants.ACCESS_TOKEN, null);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getScheme();
                    will(returnValue("https"));
                    allowing(req).getParameterMap();
                    will(returnValue(map));
                    allowing(oidcClientImpl).getOidcClientConfig(req, OIDC_CLIENT_ID);
                    will(returnValue(oidcClientConfigImpl));
                    allowing(oidcClientConfigImpl).getClientSecret(); //
                    will(returnValue("clientsecret")); //
                    allowing(oidcClientConfigImpl).isHttpsRequired();
                    will(returnValue(true));
                    allowing(cache).put(OidcUtil.encode(OIDC_STATE), table);
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(mockInterface).mockCacheRequestParameter();
                }
            });
            mockPostToWASReqURL();

            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_validIdToken() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        redirectServlet.activatedOidcClientImpl = oidcClientImpl;

        final String OIDC_CLIENT_ID = "snoop";

        try {
            final Map<String, String[]> map = new HashMap<String, String[]>();
            map.put("requestUrl", new String[] { REQUEST_URL });
            final Hashtable<String, String> table = new Hashtable<String, String>();
            table.put("requestUrl", REQUEST_URL);

            mockParamValue(Constants.STATE, OIDC_STATE);
            mockParamValue(Constants.SESSION_STATE, OIDC_SESSION_STATE);
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, OIDC_ID_TOKEN);
            mockParamValue(Constants.ACCESS_TOKEN, null);
            mockGoodCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getScheme();
                    will(returnValue("https"));
                    allowing(req).getParameterMap();
                    will(returnValue(map));
                    allowing(oidcClientImpl).getOidcClientConfig(req, OIDC_CLIENT_ID);
                    will(returnValue(oidcClientConfigImpl));
                    allowing(oidcClientConfigImpl).getClientSecret(); //
                    will(returnValue("clientsecret")); //
                    allowing(oidcClientConfigImpl).isHttpsRequired();
                    will(returnValue(true));
                    allowing(cache).put(OidcUtil.encode(OIDC_STATE), table);
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(mockInterface).mockCacheRequestParameter();
                }
            });
            mockPostToWASReqURL();

            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testDoPost_validCode_shortState() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        redirectServlet.activatedOidcClientImpl = oidcClientImpl;

        final String OIDC_CLIENT_ID = "snoop";

        try {
            final Map<String, String[]> map = new HashMap<String, String[]>();
            map.put("requestUrl", new String[] { REQUEST_URL });
            final Hashtable<String, String> table = new Hashtable<String, String>();
            table.put("requestUrl", REQUEST_URL);

            mockParamValue(Constants.STATE, OIDC_STATE_SHORT);
            mockParamValue(Constants.SESSION_STATE, OIDC_SESSION_STATE);
            mockParamValue(Constants.CODE, null);
            mockParamValue(Constants.ID_TOKEN, OIDC_ID_TOKEN);
            mockParamValue(Constants.ACCESS_TOKEN, null);
            mock.checking(new Expectations() {
                {
                    allowing(req).getScheme();
                    will(returnValue("https"));
                    allowing(req).getParameterMap();
                    will(returnValue(map));
                    allowing(oidcClientImpl).getOidcClientConfig(req, OIDC_CLIENT_ID);
                    will(returnValue(oidcClientConfigImpl));
                    allowing(oidcClientConfigImpl).getClientSecret(); //
                    will(returnValue("clientsecret")); //
                    allowing(oidcClientConfigImpl).isHttpsRequired();
                    will(returnValue(true));
                    allowing(cache).put(OidcUtil.encode(OIDC_STATE), table);
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//

                    allowing(req).getCookies();
                    will(returnValue(new Cookie[] { new Cookie(ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(OIDC_STATE_SHORT), REQUEST_URL) }));
                    allowing(req).getRequestURI();
                    will(returnValue(REQUEST_URL));
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    allowing(mockInterface).mockCacheRequestParameter();
                    //allowing(resp).sendRedirect(REQUEST_URL);
                }
            });

            mockPostToWASReqURL();

            redirectServlet.doPost(req, resp);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testCacheRequestParameter() {
        OidcRedirectServlet redirectServlet = new OidcRedirectServlet();
        redirectServlet.activatedOidcClientImpl = oidcClientImpl;

        final String OIDC_CLIENT_ID = "myId";

        try {
            final Map<String, String[]> map = new HashMap<String, String[]>();
            map.put("requestUrl", new String[] { REQUEST_URL });
            final Hashtable<String, String> table = new Hashtable<String, String>();
            table.put("requestUrl", REQUEST_URL);

            mockCreateCookie();
            mock.checking(new Expectations() {
                {
                    allowing(req).getScheme();
                    will(returnValue("https"));
                    allowing(req).getParameterMap();
                    will(returnValue(map));
                    allowing(oidcClientImpl).getOidcClientConfig(req, OIDC_CLIENT_ID);
                    will(returnValue(oidcClientConfigImpl));
                    allowing(oidcClientConfigImpl).getClientSecret(); //
                    will(returnValue("clientsecret")); //
                    allowing(oidcClientConfigImpl).isHttpsRequired();
                    will(returnValue(true));
                    allowing(cache).put(OidcUtil.encode(OIDC_STATE), table);
                    allowing(resp).addCookie(with(any(Cookie.class)));
                    will(returnValue(new StringBuffer("https://austin.ibm.com:8020/a/b")));//
                }
            });

            redirectServlet.setCookieForRequestParameter(req, resp, OIDC_CLIENT_ID, OIDC_STATE, false); //, webAppSecurityConfig);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private void mockParamValue(final String name, final String value) {
        mock.checking(new Expectations() {
            {
                allowing(req).getParameter(name);
                will(returnValue(value));
            }
        });
    }

    private void mockGoodCookie() {
        mock.checking(new Expectations() {
            {
                allowing(req).getCookies();
                will(returnValue(new Cookie[] { new Cookie(ClientConstants.WAS_REQ_URL_OIDC + HashUtils.getStrHashCode(OIDC_STATE), REQUEST_URL) }));
            }
        });
    }

    private void mockOidcStateAndCode() {
        mockParamValue(Constants.STATE, OIDC_STATE);
        mockParamValue(Constants.CODE, OIDC_CODE);
        mockParamValue(Constants.SESSION_STATE, OIDC_SESSION_STATE);
    }

    private void mockPostToWASReqURL() throws IOException {
        mock.checking(new Expectations() {
            {
                allowing(resp).setHeader(with(any(String.class)), with(any(String.class)));
                allowing(resp).setDateHeader(with(any(String.class)), with(any(Long.class)));
                allowing(resp).setContentType(with(any(String.class)));
                allowing(resp).getWriter();
            }
        });
    }

    private void mockCreateCookie() throws IOException {
        mock.checking(new Expectations() {
            {
                allowing(webAppSecurityConfig).getHttpOnlyCookies();
                will(returnValue(false));
                allowing(webAppSecurityConfig).getSSORequiresSSL();
                will(returnValue(false));
                allowing(webAppSecurityConfig).createSSOCookieHelper();
                will(returnValue(new SSOCookieHelperImpl(webAppSecurityConfig)));
                allowing(webAppSecurityConfig).createReferrerURLCookieHandler();
                will(returnValue(new ReferrerURLCookieHandler(webAppSecurityConfig)));
            }
        });
    }

}
