/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.acs;

import static com.ibm.ws.security.saml.Constants.RELAY_STATE;
import static com.ibm.ws.security.saml.Constants.SP_INITAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.common.CommonMockObjects;

import test.common.SharedOutputManager;

public class AcsHandlerTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final CommonMockObjects common = new CommonMockObjects();
    private static final Mockery mockery = common.getMockery();

    private static final HttpServletRequest request = common.getServletRequest();
    private static final HttpServletResponse response = common.getServletResponse();
    private static final HttpSession session = common.getSession();
    private static final SsoConfig ssoConfig = common.getSsoConfig();

    private static final SsoRequest ssoRequest = common.getSsoRequest();
    private static final SsoSamlService ssoService = common.getSsoService();

    private static final SolicitedHandler solicitedHandler = mockery.mock(SolicitedHandler.class, "solicitedHandler");
    private static final UnsolicitedHandler unsolicitedHandler = mockery.mock(UnsolicitedHandler.class, "unsolicitedHandler");

    private static Map<String, Object> parameters = new HashMap<String, Object>();

    private static AcsHandler handler;

    @BeforeClass
    public static void setUp() {
        handler = new AcsHandler();
        outputMgr.trace("*=all");
        parameters.put(Constants.KEY_SAML_SERVICE, ssoService);

        mockery.checking(new Expectations() {
            {
                allowing(ssoService).getProviderId();
                will(returnValue(null));

                allowing(ssoService).getConfig();
                will(returnValue(with(any(SsoConfig.class))));

                allowing(ssoConfig).createSession();
                will(returnValue(with(any(Boolean.class))));
                allowing(request).getSession(true);
                will(returnValue(session));
            }
        });
    }

    @AfterClass
    public static void tearDown() {
        outputMgr.trace("*=all=disabled");
    }

    @Test
    public void testGetSamlVersion() {
        assertEquals("Expected to receive the SAML version " + Constants.SamlSsoVersion.SAMLSSO20 + " but it was not received.",
                     Constants.SamlSsoVersion.SAMLSSO20, handler.getSamlVersion());
    }

    @Test
    public void testHandleRequest_RelayState() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(RELAY_STATE);
                will(returnValue(SP_INITAL));
                one(solicitedHandler).handleRequest(SP_INITAL);
            }
        });

        try {
            handler.handleRequest(request, response, ssoRequest, parameters, solicitedHandler, unsolicitedHandler);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }

    @Test
    public void testHandleRequest_NullRelayState() throws SamlException {
        mockery.checking(new Expectations() {
            {
                one(request).getParameter(RELAY_STATE);
                will(returnValue(null));
                one(unsolicitedHandler).handleRequest(null);
            }
        });

        try {
            handler.handleRequest(request, response, ssoRequest, parameters, solicitedHandler, unsolicitedHandler);
        } catch (SamlException ex) {
            ex.printStackTrace();
            fail("Unexpected exception was thrown: " + ex);
        }
    }
}
