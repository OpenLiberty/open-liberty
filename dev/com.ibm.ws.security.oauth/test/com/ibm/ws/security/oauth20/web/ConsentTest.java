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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.oauth.core.api.OAuthResult;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20Client;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.form.FormRenderer;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.BoundedConsentCache;
import com.ibm.ws.security.oauth20.util.ConsentCacheKey;
import com.ibm.ws.security.oauth20.util.Nonce;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.TemplateRetriever;

import test.common.SharedOutputManager;

/**
 *
 */
public class ConsentTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule outputRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final HttpServletRequest request = mock.mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock.mock(HttpServletResponse.class);
    private final OAuth20Provider provider = mock.mock(OAuth20Provider.class);
    private final OAuthResult result = mock.mock(OAuthResult.class);
    private final BoundedConsentCache bcc = mock.mock(BoundedConsentCache.class);
    private final HttpSession hs = mock.mock(HttpSession.class);
    private final AttributeList al = mock.mock(AttributeList.class);
    private final ConsentCacheKey cck = mock.mock(ConsentCacheKey.class);
    private final OidcBaseClient obc = mock.mock(OidcBaseClient.class);
    private final OidcOAuth20ClientProvider oocp = mock.mock(OidcOAuth20ClientProvider.class);
    private final ServletContext sc = mock.mock(ServletContext.class);
    private final RequestDispatcher rd = mock.mock(RequestDispatcher.class);
    private final FormRenderer fr = mock.mock(FormRenderer.class);
    private final Nonce nonce = mock.mock(Nonce.class);

    private static final String ATTR_CONSENT_CACHE = "consentCache";
    private final static String ATTR_PROMPT = "prompt";
    private static final String ATTR_NONCE = "consentNonce";
    private static final String ATTR_OAUTH_CLIENT = "oauthClient";
    public static final String HEADER_ACCEPT_LANGUAGE = TemplateRetriever.HEADER_ACCEPT_LANGUAGE;

    /**
     * Tests handleConsent with following conditions
     * 1.Prompt is set as consent.
     * Expected result: no error.
     */
    @Test
    public void handleConsentNormal() {
        final String client = "client01";
        final String redirect = "http://localhost/redirect";
        final String scope = "openid";
        final long cacheSize = 100;
        final long lifetime = 100;
        mock.checking(new Expectations() {
            {
                one(request).getParameter(ATTR_PROMPT);
                will(returnValue(Constants.PROMPT_LOGIN));
                one(request).getParameter(OAuth20Constants.REDIRECT_URI);
                will(returnValue(redirect));
                allowing(request).getParameter(OAuth20Constants.SCOPE);
                will(returnValue(scope));
                one(request).getParameterValues(OAuth20Constants.RESOURCE);//
                will(returnValue(null)); //
                one(provider).getConsentCacheSize();
                will(returnValue(cacheSize));
                one(provider).isLocalStoreUsed();
                will(returnValue(true));
                allowing(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_CONSENT_CACHE);
                will(returnValue(null));
                one(request).getParameter(OAuth20Constants.RESOURCE);//
                will(returnValue(null));//
                one(provider).getConsentCacheEntryLifetime();
                will(returnValue(lifetime));
                //set
                one(hs).setAttribute(with(equal(ATTR_CONSENT_CACHE)), with(any(BoundedConsentCache.class)));
            }
        });

        Prompt prompt = new Prompt(request);
        Consent consent = new Consent();
        consent.handleConsent(provider, request, prompt, client);
    }

    /**
     * Tests handleConsent with following conditions
     * 1.Prompt is not set.
     * Expected result: do nothing.
     */
    @Test
    public void handleConsentNoPrompt() {
        final String client = "client01";
        mock.checking(new Expectations() {
            {
                one(request).getParameter(ATTR_PROMPT);
                will(returnValue(null));
            }
        });

        Prompt prompt = new Prompt(request);
        Consent consent = new Consent();
        consent.handleConsent(provider, request, prompt, client);
    }

    /**
     * Tests handleConsent with following conditions
     * 1.Prompt is set as "login".
     * Expected result: nothing happens.
     */
    public void handleConsentNoConsent() {
        final String client = "client01";
        mock.checking(new Expectations() {
            {
                one(request).getParameter(ATTR_PROMPT);
                will(returnValue(Constants.PROMPT_LOGIN));
                never(request).getParameter(OAuth20Constants.REDIRECT_URI);
            }
        });

        Prompt prompt = new Prompt(request);
        Consent consent = new Consent();
        consent.handleConsent(provider, request, prompt, client);
    }

    /**
     * Tests isCachedAndValid with following conditions
     * 1.cached object is still valid
     * Expected result: return true, setAttribute.
     */
    @Test
    public void isCachedAndValidCachedObjectValid() {
        final String client = "client01";
        final String redirect = "http://localhost/redirect";
        final String scope = "openid";
        final String scopes[] = { scope };
        final long cacheSize = 100;
        final long newCacheSize = 200;
        final long lifetime = 100;
        mock.checking(new Expectations() {
            {
                allowing(result).getAttributeList();
                will(returnValue(al));
                one(al).getAttributeValuesByName(OAuth20Constants.SCOPE);
                will(returnValue(scopes));
                one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                will(returnValue(client));
                one(al).getAttributeValueByName(OAuth20Constants.REDIRECT_URI);
                will(returnValue(redirect));
                one(al).getAttributeValueByName(OAuth20Constants.RESOURCE);//
                will(returnValue(null));//
                one(provider).getConsentCacheEntryLifetime();
                will(returnValue(lifetime));
                one(provider).getConsentCacheSize();
                will(returnValue(newCacheSize));
                one(provider).isLocalStoreUsed();
                will(returnValue(true));
                allowing(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_CONSENT_CACHE);
                will(returnValue(bcc));
                one(bcc).getCapacity();
                will(returnValue((int) cacheSize));
                // set
                one(bcc).updateCapacity((int) newCacheSize);
                one(bcc).get(with(any(ConsentCacheKey.class)));
                will(returnValue(cck));
                one(cck).isValid();
                will(returnValue(true));
                one(cck).getLifetime();
                will(returnValue((int) lifetime));
                //set
                one(hs).setAttribute(with(equal(ATTR_CONSENT_CACHE)), with(any(BoundedConsentCache.class)));
            }
        });

        Consent consent = new Consent();
        assertTrue(consent.isCachedAndValid(result, provider, request, response));

    }

    /**
     * Tests isCachedAndValid with following conditions
     * 1.cached object is not valid
     * Expected result: return false, setAttribute.
     */
    @Test
    public void isCachedAndValidCachedObjectInvalid() {
        final String client = "client01";
        final String redirect = "http://localhost/redirect";
        final String scope = "openid";
        final String scopes[] = { scope };
        final long cacheSize = 100;
        final long newCacheSize = 200;
        final long lifetime = 100;
        mock.checking(new Expectations() {
            {
                allowing(result).getAttributeList();
                will(returnValue(al));
                one(al).getAttributeValuesByName(OAuth20Constants.SCOPE);
                will(returnValue(scopes));
                one(al).getAttributeValueByName(OAuth20Constants.CLIENT_ID);
                will(returnValue(client));
                one(al).getAttributeValueByName(OAuth20Constants.REDIRECT_URI);
                will(returnValue(redirect));
                one(al).getAttributeValueByName(OAuth20Constants.RESOURCE);
                will(returnValue(null));
                one(provider).getConsentCacheEntryLifetime();
                will(returnValue(lifetime));
                one(provider).getConsentCacheSize();
                will(returnValue(newCacheSize));
                one(provider).isLocalStoreUsed();
                will(returnValue(true));
                allowing(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_CONSENT_CACHE);
                will(returnValue(bcc));
                one(bcc).getCapacity();
                will(returnValue((int) cacheSize));
                // set
                one(bcc).updateCapacity((int) newCacheSize);
                one(bcc).get(with(any(ConsentCacheKey.class)));
                will(returnValue(cck));
                one(cck).isValid();
                will(returnValue(false));
                //set
                one(bcc).remove(with(any(ConsentCacheKey.class)));
                //set
                one(hs).setAttribute(with(equal(ATTR_CONSENT_CACHE)), with(any(BoundedConsentCache.class)));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isCachedAndValid(result, provider, request, response));

    }

    /**
     * Tests isCachedAndValid with following conditions
     * 1.OAuthResult is null
     * Expected result: return false
     */
    @Test
    public void isCachedAndValidNoOAuthResult() {
        Consent consent = new Consent();
        assertFalse(consent.isCachedAndValid(null, provider, request, response));
    }

    /**
     * Tests isCachedAndValid with following conditions
     * 1.scope is null
     * Expected result: return false
     */
    @Test
    public void isCachedAndValidScopeNull() {
        final String scopes[] = null;
        mock.checking(new Expectations() {
            {
                allowing(result).getAttributeList();
                will(returnValue(al));
                one(al).getAttributeValuesByName(OAuth20Constants.SCOPE);
                will(returnValue(scopes));
                never(hs).setAttribute(with(equal(ATTR_CONSENT_CACHE)), with(any(BoundedConsentCache.class)));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isCachedAndValid(result, provider, request, response));
    }

    /**
     * Tests isCacheKeyValid with following conditions
     * 1. consentCacheKey is null
     * Expected result: return false
     */
    @Test
    public void isCacheKeyValidNullConsentCacheKey() {
        Consent consent = new Consent();
        assertFalse(consent.isCacheKeyValid(bcc, null, null, 100));
    }

    /**
     * Tests renderConsentForm with following conditions
     * 1.normal initial invocation
     * Expected result: forwarding a request
     */
    @Test
    public void renderConsentFormNormalForward() {
        final String methodName = "renderConsentFormNormalForward";
        final String client = "client01";
        final String root = "/root";
        final String path = "/path";
        final String template = "{" + root + "}" + path;
        final Nonce nonceLocal = Nonce.getInstance();

        try {
            mock.checking(new Expectations() {
                {
                    one(provider).getDefaultAuthorizationFormTemplateContent();
                    one(provider).getClientProvider();
                    will(returnValue(oocp));
                    one(oocp).get(client);
                    will(returnValue(obc));
                    one(provider).getAuthorizationFormTemplate();
                    will(returnValue(template));
                    //set
                    one(request).setAttribute(ATTR_OAUTH_CLIENT, obc);
                    //set
                    one(request).setAttribute(ATTR_NONCE, nonceLocal);

                    one(sc).getContext(root);
                    will(returnValue(sc));
                    one(sc).getRequestDispatcher(path);
                    will(returnValue(rd));
                    //set
                    one(rd).forward(request, response);
                }
            });

            Consent consent = new Consent();
            consent.renderConsentForm(request, response, provider, client, nonceLocal, al, sc);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests renderConsentForm with following conditions
     * 1.normal initial invocation
     * Expected result: redirecting a request.
     */

    @Test
    public void renderConsentFormNormalRedirect() {
        final String methodName = "renderConsentFormNormalRedirect";
        final String client = "client01";
        final StringBuffer requestURL = new StringBuffer("http://localhost/path");
        final String path = "/path";
        final Nonce nonceLocal = Nonce.getInstance();

        try {
            mock.checking(new Expectations() {
                {
                    one(provider).getDefaultAuthorizationFormTemplateContent();
                    one(provider).getClientProvider();
                    will(returnValue(oocp));
                    one(oocp).get(client);
                    will(returnValue(obc));
                    one(provider).getAuthorizationFormTemplate();
                    will(returnValue(OIDCConstants.DEFAULT_TEMPLATE_HTML));
                    allowing(request).getContextPath();
                    will(returnValue(path));
                    allowing(request).getRequestURL();
                    will(returnValue(requestURL));
                    one(request).getHeader(HEADER_ACCEPT_LANGUAGE);
                    will(returnValue("US-ASCII"));
                    //set
                    one(response).setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
                    //set
                    one(response).setHeader("Pragma", "no-cache");
                    //set
                    one(response).setDateHeader("Expires", 0);
                    //set
                    one(fr).renderForm(with(any(OidcOAuth20Client.class)), with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(Nonce.class)),
                                       with(any(AttributeList.class)), with(any(String.class)), with(any(HttpServletResponse.class)), with(any(byte[].class)));
                }
            });

            Consent consent = new Consent();
            consent.renderConsentForm(request, response, provider, client, nonceLocal, al, sc, fr);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests renderConsentForm with following conditions
     * 1.normal initial invocation
     * 2.dipatcher cannot be found.
     * Expected result: nothing happens, CWOAU0018E error message is logged.
     */
    @Test
    public void renderConsentFormNoDispatcher() {
        final String methodName = "renderConsentFormNoDispatcher";
        final String client = "client01";
        final String root = "/root";
        final String path = "/path";
        final String template = "{" + root + "}" + path;
        final Nonce nonceLocal = Nonce.getInstance();
        final String error = "CWOAU0018E:";
        try {
            mock.checking(new Expectations() {
                {
                    one(provider).getDefaultAuthorizationFormTemplateContent();
                    one(provider).getClientProvider();
                    will(returnValue(oocp));
                    one(oocp).get(client);
                    will(returnValue(obc));
                    one(provider).getAuthorizationFormTemplate();
                    will(returnValue(template));
                    //set
                    one(request).setAttribute(ATTR_OAUTH_CLIENT, obc);
                    //set
                    one(request).setAttribute(ATTR_NONCE, nonceLocal);

                    one(sc).getContext(root);
                    will(returnValue(sc));
                    one(sc).getRequestDispatcher(path);
                    will(returnValue(null));
                    //set
                    never(rd).forward(request, response);
                }
            });

            Consent consent = new Consent();
            consent.renderConsentForm(request, response, provider, client, nonceLocal, al, sc);
            assertTrue(outputMgr.checkForMessages(error));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests handleNonceError with following conditions
     * 1.nonce is expired
     * Expected result: send HttpServletResponse.SC_REQUEST_TIMEOUT
     */
    @Test
    public void handleNonceErrorTimeout() {
        final String methodName = "handleNonceErrorTimeout";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getSession();
                    will(returnValue(hs));
                    one(hs).getAttribute(ATTR_NONCE);
                    will(returnValue(nonce));
                    one(nonce).isExpired();
                    will(returnValue(true));
                    //set
                    one(response).sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                }
            });

            Consent consent = new Consent();
            consent.handleNonceError(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests handleNonceError with following conditions
     * 1.nonce is not expired.
     * Expected result: send HttpServletResponse.SC_INTERNAL_SERVER_ERROR
     */
    @Test
    public void handleNonceErrorNoTimeout() {
        final String methodName = "handleNonceErrorNoTimeout";
        try {
            mock.checking(new Expectations() {
                {
                    one(request).getSession();
                    will(returnValue(hs));
                    one(hs).getAttribute(ATTR_NONCE);
                    will(returnValue(nonce));
                    one(nonce).isExpired();
                    will(returnValue(false));
                    //set
                    one(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            });

            Consent consent = new Consent();
            consent.handleNonceError(request, response);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    /**
     * Tests isNonceValid with following conditions
     * 1.nonce is valid.
     * Expected result: true
     */
    @Test
    public void isNonceValidValid() {
        final String requestNonce = "rn";
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(nonce));
                one(nonce).isValid(requestNonce);
                will(returnValue(true));
            }
        });

        Consent consent = new Consent();
        assertTrue(consent.isNonceValid(request, requestNonce));
    }

    /**
     * Tests isNonceValid with following conditions
     * 1.nonce is invalid.
     * Expected result: false
     */
    @Test
    public void isNonceValidInvalid() {
        final String requestNonce = "rn";
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(nonce));
                one(nonce).isValid(requestNonce);
                will(returnValue(false));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isNonceValid(request, requestNonce));
    }

    /**
     * Tests isNonceValid with following conditions
     * 1.nonce doesn't exist
     * Expected result: false
     */
    @Test
    public void isNonceValidNoNonce() {
        final String requestNonce = "rn";
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(null));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isNonceValid(request, requestNonce));
    }

    /**
     * Tests isNonceExpired with following conditions
     * 1.nonce is not expired.
     * Expected result: true
     */
    @Test
    public void isNonceExpiredNotExpired() {
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(nonce));
                one(nonce).isExpired();
                will(returnValue(false));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isNonceExpired(request));
    }

    /**
     * Tests isNonceExpired with following conditions
     * 1.nonce is expired
     * Expected result: false
     */
    @Test
    public void isNonceExpiredExpired() {
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(nonce));
                one(nonce).isExpired();
                will(returnValue(true));
            }
        });

        Consent consent = new Consent();
        assertTrue(consent.isNonceExpired(request));
    }

    /**
     * Tests isNonceExpired with following conditions
     * 1.nonce doesn't exist
     * Expected result: false
     */
    @Test
    public void isNonceExpiredNoNonce() {
        mock.checking(new Expectations() {
            {
                one(request).getSession();
                will(returnValue(hs));
                one(hs).getAttribute(ATTR_NONCE);
                will(returnValue(null));
            }
        });

        Consent consent = new Consent();
        assertFalse(consent.isNonceExpired(request));
    }

    /**
     * Tests setNonce with following conditions
     * 1.nonce doesn't exist
     * Expected result: false
     */
    @Test
    public void setNonceNormal() {
        mock.checking(new Expectations() {
            {
                one(request).getSession(true);
                will(returnValue(hs));
                //set
                one(hs).setAttribute(with(any(String.class)), with(any(Nonce.class)));
                one(request).setAttribute(with(any(String.class)), with(any(String.class)));
            }
        });

        Consent consent = new Consent();
        assertNotNull(consent.setNonce(request));
    }

}
