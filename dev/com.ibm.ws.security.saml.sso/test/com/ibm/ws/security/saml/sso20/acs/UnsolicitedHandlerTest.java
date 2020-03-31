/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import static com.ibm.ws.security.saml.Constants.COOKIE_WAS_REQUEST;
import static com.ibm.ws.security.saml.Constants.KEY_SAML_SERVICE;
import static com.ibm.ws.security.saml.Constants.SAMLResponse;
import static com.ibm.ws.security.saml.Constants.SP_INITAL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.SubjectConfirmation;
import org.opensaml.saml2.core.SubjectConfirmationData;

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.HttpRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.UnsolicitedResponseCache;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

import test.common.SharedOutputManager;

public class UnsolicitedHandlerTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final Cache cache = common.getCache();
    private static final HttpServletRequest request = common.getServletRequest();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final ForwardRequestInfo requestInfo = common.getRequestInfo();
    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final SsoSamlService ssoService = common.getSsoService();
    private static final UserData userData = common.getUserData();
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final WebSSOConsumer<?, ?, ?> webSSOConsumer = mockery.mock(WebSSOConsumer.class, "webSSOConsumer");
    private static final UnsolicitedResponseCache resCache = mockery.mock(UnsolicitedResponseCache.class, "resCache");
    private static final Assertion a_assertion = mockery.mock(Assertion.class, "a_assertion");
    private static final Subject a_subject = mockery.mock(Subject.class, "a_subject");
    private static final SubjectConfirmation subject_confirm = mockery.mock(SubjectConfirmation.class, "subject_confirm");
    private static final List<SubjectConfirmation> listsubjectconfirm = new ArrayList<SubjectConfirmation>();
    static {
        listsubjectconfirm.add(subject_confirm);
    }
    private static final SubjectConfirmationData subject_confirm_data = mockery.mock(SubjectConfirmationData.class, "subject_confirm_data");

    private static final String RESPONSE = "HTTP/200";
    private static final String PROVIDER_NAME = "https://sp.example.com/SAML2";

    private static final String a_id = "assertion_id";

    private static UnsolicitedHandler unsolicitedHandler;
    private static Map<String, Object> parameters;
    private static byte[] cookieValueBytes = { 'i', 'd', 'p', '_', 'i', 'n', 'i', 't', 'i', 'a', 'l', '_' };

    @BeforeClass
    public static void setUp() throws SamlException {
        outputMgr.trace("*=all");
        parameters = new HashMap<String, Object>();
        parameters.put(KEY_SAML_SERVICE, ssoService);

        unsolicitedHandler = new UnsolicitedHandler(request, response, ssoRequest, parameters);
        WebSSOConsumer.setInstance(webSSOConsumer);

        mockery.checking(new Expectations() {
            {
                one(webSSOConsumer).handleSAMLResponse(request, response, ssoService, null, ssoRequest);
                will(returnValue(basicMessageContext));

                atMost(2).of(ssoRequest).getProviderName();//
                will(returnValue(PROVIDER_NAME));//

                atMost(2).of(ssoService).getUnsolicitedResponseCache(PROVIDER_NAME);//
                will(returnValue(resCache));//

                atMost(2).of(basicMessageContext).getValidatedAssertion();//
                will(returnValue(a_assertion)); //

                atMost(2).of(a_assertion).getID(); //
                will(returnValue(a_id)); //

                atMost(2).of(resCache).isValid(a_id); //
                will(returnValue(false)); //

                atMost(2).of(a_assertion).getSubject(); //
                will(returnValue(a_subject)); //

                atMost(2).of(a_subject).getSubjectConfirmations();//
                will(returnValue(listsubjectconfirm));//

                atMost(2).of(subject_confirm).getSubjectConfirmationData();//
                will(returnValue(subject_confirm_data));//

                atMost(2).of(subject_confirm_data).getNotOnOrAfter(); //
                will(returnValue((new DateTime()).plus(3600000))); //

                atMost(2).of(resCache).put(with(any(String.class)), with(any(Long.class))); //

                atMost(2).of(ssoRequest).getProviderName();
                will(returnValue(PROVIDER_NAME));

                //restricting this makes adding new custom props a pain -- don't do it
                allowing(ssoRequest).getSsoConfig();
                will(returnValue(ssoConfig));

                one(ssoService).getAcsCookieCache(PROVIDER_NAME);
                will(returnValue(cache));

                one(cache).get("");
                will(returnValue(requestInfo));
                one(cache).remove("");

                one(requestInfo).setWithFragmentUrl(request, response);
                one(requestInfo).redirectCachedHttpRequest(with(any(HttpServletRequest.class)),
                                                           with(any(HttpServletResponse.class)),
                                                           with(any(String.class)),
                                                           with(any(String.class)));

                one(basicMessageContext).getUserDataIfReady();
                will(returnValue(userData));

                one(cache).put(with(any(String.class)), with(any(UserData.class)));

                allowing(ssoConfig).getUseRelayStateForTarget();
                will(returnValue(true));
            }
        });
    }

    @SuppressWarnings("rawtypes")
    @AfterClass
    public static void tearDown() {
        WebSSOConsumer.setInstance(new WebSSOConsumer());
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testHandleRequest() {
        mockery.checking(new Expectations() {
            {
                one((IExtendedRequest) request).getCookieValueAsBytes(COOKIE_WAS_REQUEST);
                will(returnValue(cookieValueBytes));

                one(request).getParameter(SAMLResponse);
                will(returnValue(RESPONSE));
            }
        });
        try {
            unsolicitedHandler.handleRequest(SP_INITAL);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }

    @Test
    public void testHandleRequest_NullRelayState() {
        mockery.checking(new Expectations() {
            {
                one((IExtendedRequest) request).getCookieValueAsBytes(COOKIE_WAS_REQUEST);
                will(returnValue(null));
                one(ssoConfig).getTargetPageUrl();//
                will(returnValue(null));//
            }
        });
        try {
            unsolicitedHandler.handleRequest(null);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleRequest_EmptyRelayState() {
        final byte[] emptyArray = { ' ' };
        mockery.checking(new Expectations() {
            {
                one((IExtendedRequest) request).getCookieValueAsBytes(COOKIE_WAS_REQUEST);
                will(returnValue(emptyArray));
                one(ssoConfig).getTargetPageUrl();//
                will(returnValue(""));//
            }
        });
        try {
            unsolicitedHandler.handleRequest("");
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testHandleRequest_NullRawSamlResponse() {
        mockery.checking(new Expectations() {
            {
                one((IExtendedRequest) request).getCookieValueAsBytes(COOKIE_WAS_REQUEST);
                will(returnValue(cookieValueBytes));

                one(request).getParameter(SAMLResponse);
                will(returnValue(null));
            }
        });
        try {
            unsolicitedHandler.handleRequest(SP_INITAL);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }

    @Test
    public void testGetUnsolicitedRequestInfo() {
        final String relayState = SP_INITAL + " " + PROVIDER_NAME;

        mockery.checking(new Expectations() {
            {
                one(cache).get(PROVIDER_NAME);
                will(returnValue(null));
            }
        });

        try {
            HttpRequestInfo requestInfo = unsolicitedHandler.getUnsolicitedRequestInfo(basicMessageContext, relayState, cache);
            if (requestInfo != null) {
                assertEquals("Expected to receive the message '" + relayState + "' but it was not received.",
                             relayState, requestInfo.getRequestUrl());
                assertEquals("Expected to receive an empty String but it was not received.",
                             "", requestInfo.getQueryString());
            } else {
                fail("requestInfo must not be null.");
            }

        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex.getMessage());
        }
    }
}
