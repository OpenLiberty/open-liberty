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

import static com.ibm.ws.security.saml.Constants.KEY_SAML_SERVICE;
import static com.ibm.ws.security.saml.Constants.SAMLResponse;
import static com.ibm.ws.security.saml.Constants.SP_INITAL;
import static com.ibm.ws.security.saml.sso20.common.CommonMockObjects.SAML20_AUTHENTICATION_FAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
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

import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;

import test.common.SharedOutputManager;

public class SolicitedHandlerTest {

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
    private static final SsoConfig ssoConfig = common.getSsoConfig();
    private static final UserData userData = common.getUserData();
    private static final BasicMessageContext<?, ?, ?> basicMessageContext = common.getBasicMessageContext();
    private static final WebSSOConsumer<?, ?, ?> webSSOConsumer = mockery.mock(WebSSOConsumer.class, "webSSOConsumer");

    private static final String RESPONSE = "HTTP/200";
    private static final String PROVIDER_NAME = "https://sp.example.com/SAML2";

    private static SolicitedHandler solicitedHandler;
    private static Map<String, Object> parameters;

    @BeforeClass
    public static void setUp() {
        outputMgr.trace("*=all");
        parameters = new HashMap<String, Object>();
        parameters.put(KEY_SAML_SERVICE, ssoService);

        solicitedHandler = new SolicitedHandler(request, response, ssoRequest, parameters);
        WebSSOConsumer.setInstance(webSSOConsumer);
    }

    @SuppressWarnings("rawtypes")
    @AfterClass
    public static void tearDown() {
        WebSSOConsumer.setInstance(new WebSSOConsumer());
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testHandleRequest() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(SAMLResponse);
                will(returnValue(RESPONSE));

                one(webSSOConsumer).handleSAMLResponse(request, response, ssoService, SP_INITAL, ssoRequest);
                will(returnValue(basicMessageContext));

                atMost(2).of(ssoRequest).getProviderName();
                will(returnValue(PROVIDER_NAME));

                one(ssoService).getAcsCookieCache(PROVIDER_NAME);
                will(returnValue(cache));

                one(basicMessageContext).getCachedRequestInfo();
                will(returnValue(requestInfo));

                one(requestInfo).getBirthTime();
                will(returnValue(new DateTime()));

                one(ssoService).getConfig();
                will(returnValue(ssoConfig));

                one(ssoConfig).getAuthnRequestTime();
                will(returnValue(600000L));

                one(requestInfo).setWithFragmentUrl(request, response);
                one(requestInfo).redirectCachedHttpRequest(with(any(HttpServletRequest.class)),
                                                           with(any(HttpServletResponse.class)),
                                                           with(any(String.class)),
                                                           with(any(String.class)));

                one(basicMessageContext).getUserDataIfReady();
                will(returnValue(userData));

                one(cache).put(with(any(String.class)), with(any(UserData.class)));
            }
        });

        try {
            solicitedHandler.handleRequest(SP_INITAL);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testHandleRequest_NullSAMLResponse() {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(SAMLResponse);
                will(returnValue(null));
            }
        });
        try {
            solicitedHandler.handleRequest(SP_INITAL);
            fail("SamlException was not thrown");
        } catch (SamlException ex) {
            assertEquals("Expected to receive the message for '" + SAML20_AUTHENTICATION_FAIL + "' but it was not received.",
                         SAML20_AUTHENTICATION_FAIL, ex.getMsgKey());
        }
    }
}
