/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.jaspi.JaspiMessageInfo;

public class HttpMessageContextImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private HttpMessageContext httpMessageContext;
    private MessageInfo messageInfo;
    private Subject clientSubject;
    private CallbackHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        messageInfo = createMessageInfo(true);
        clientSubject = new Subject();
        handler = mockery.mock(CallbackHandler.class);
        httpMessageContext = new HttpMessageContextImpl(messageInfo, clientSubject, handler);
    }

    @After
    public void tearDown() throws Exception {}

//    @Test
//    public void testCleanClientSubject() {
//
//    }
//
    @Test
    public void testDoNothing() {
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.NOT_DONE, httpMessageContext.doNothing());
    }

//
//    @Test
//    public void testForward() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetAuthParameters() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetCallerPrincipal() {
//        fail("Not yet implemented");
//    }
//
    @Test
    public void testGetClientSubject() {
        assertSame("The client subject must be set.", clientSubject, httpMessageContext.getClientSubject());
    }

//
//    @Test
//    public void testGetGroups() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testGetHandler() {
//        fail("Not yet implemented");
//    }
//
    @Test
    public void testGetMessageInfo() {
        assertEquals("The MessageInfo must be set.", messageInfo, httpMessageContext.getMessageInfo());
    }

    @Test
    public void testGetRequest() {
        assertEquals("The HttpServletRequest must be set.", request, httpMessageContext.getRequest());
    }

    @Test
    public void testGetResponse() {
        assertEquals("The HttpServletResponse must be set.", response, httpMessageContext.getResponse());
    }

    @Test
    public void testIsAuthenticationRequest() {
        messageInfo.getMap().put("com.ibm.websphere.jaspi.request", "authenticate");
        assertTrue("The request must be an authentication request.", httpMessageContext.isAuthenticationRequest());
    }

    @Test
    public void testIsProtected() {
        assertTrue("The resource must be protected when the mandatory property is set in the MessageInfo.", httpMessageContext.isProtected());
    }
//
//    @Test
//    public void testIsRegisterSession() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testNotifyContainerAboutLoginCredentialValidationResult() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testNotifyContainerAboutLoginStringSetOfString() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testNotifyContainerAboutLoginPrincipalSetOfString() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testRedirect() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testResponseNotFound() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testResponseUnauthorized() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSetRegisterSession() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSetRequest() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testSetResponse() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testWithRequest() {
//        fail("Not yet implemented");
//    }

    private MessageInfo createMessageInfo(boolean mandatory) {
        MessageInfo messageInfo = new JaspiMessageInfo(request, response);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }
}
