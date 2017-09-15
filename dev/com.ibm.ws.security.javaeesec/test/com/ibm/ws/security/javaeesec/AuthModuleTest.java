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
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.ProtectionPolicy;
import javax.security.auth.message.MessagePolicy.Target;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.jaspi.JaspiMessageInfo;

public class AuthModuleTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private ServerAuthModule authModule;
    private CDI cdi;
    private Instance<HttpAuthenticationMechanism> beanInstance;
    private HttpAuthenticationMechanism httpAuthenticationMechanism;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Subject clientSubject;
    private Subject serviceSubject;

    @Before
    public void setUp() throws Exception {
        cdi = mockery.mock(CDI.class);
        beanInstance = mockery.mock(Instance.class);
        httpAuthenticationMechanism = mockery.mock(HttpAuthenticationMechanism.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        clientSubject = new Subject();
        serviceSubject = null;

        authModule = new AuthModule() {
            @Override
            protected CDI getCDI() {
                return cdi;
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testGetSupportedMessageTypes() {
        Class[] supportedMessageTypes = authModule.getSupportedMessageTypes();
        assertEquals("The supported message types must contain only two entries.", 2, supportedMessageTypes.length);
        assertEquals("The supported message types must contain an HttpServletRequest.class entry.", HttpServletRequest.class, supportedMessageTypes[0]);
        assertEquals("The supported message types must contain an HttpServletResponse.class entry.", HttpServletResponse.class, supportedMessageTypes[1]);
    }

    @Test
    public void testValidateRequest() throws Exception {
        withBeanInstance().withOneMechanism().authMechValidatesRequest(AuthenticationStatus.SUCCESS);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        AuthStatus status = authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals("The AuthStatus must be SUCCESS.", AuthStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestWithSendFailure() throws Exception {
        withBeanInstance().withOneMechanism().authMechValidatesRequest(AuthenticationStatus.SEND_FAILURE);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        AuthStatus status = authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals("The AuthStatus must be SEND_FAILURE.", AuthStatus.SEND_FAILURE, status);
    }

    @Test
    public void testValidateRequestWithSendContinue() throws Exception {
        withBeanInstance().withOneMechanism().authMechValidatesRequest(AuthenticationStatus.SEND_CONTINUE);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        AuthStatus status = authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals("The AuthStatus must be SEND_CONTINUE.", AuthStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testValidateRequestWithAuthenticationException() throws Exception {
        withBeanInstance().withOneMechanism().authMechValidateRequestThrowsException();
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        try {
            authModule.validateRequest(messageInfo, clientSubject, serviceSubject);
            fail("There must be an AuthException thrown from the AuthModule.");
        } catch (AuthException e) {
            assertEquals("The AuthException cause must be set.", AuthenticationException.class, e.getCause().getClass());
        }
    }

    @Test
    public void testSecureResponse() throws Exception {
        withBeanInstance().withOneMechanism().authMechSecuresResponse(AuthenticationStatus.SUCCESS);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        Subject receivedSubject = new Subject();
        AuthStatus status = authModule.secureResponse(messageInfo, receivedSubject);
        assertEquals("The AuthStatus must be SEND_SUCCESS.", AuthStatus.SEND_SUCCESS, status);
    }

    @Test
    public void testSecureResponseWithSendFailure() throws Exception {
        withBeanInstance().withOneMechanism().authMechSecuresResponse(AuthenticationStatus.SEND_FAILURE);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        Subject receivedSubject = new Subject();
        AuthStatus status = authModule.secureResponse(messageInfo, receivedSubject);
        assertEquals("The AuthStatus must be SEND_FAILURE.", AuthStatus.SEND_FAILURE, status);
    }

    @Test
    public void testSecureResponseWithSendContinue() throws Exception {
        withBeanInstance().withOneMechanism().authMechSecuresResponse(AuthenticationStatus.SEND_CONTINUE);
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);

        Subject receivedSubject = new Subject();
        AuthStatus status = authModule.secureResponse(messageInfo, receivedSubject);
        assertEquals("The AuthStatus must be SEND_CONTINUE.", AuthStatus.SEND_CONTINUE, status);
    }

    @Test
    public void testSecureResponseWithAuthenticationException() throws Exception {
        withBeanInstance().withOneMechanism().authMechSecuresResponseThrowsException();
        initializeModule();
        MessageInfo messageInfo = createMessageInfo(true);
        Subject receivedSubject = new Subject();

        try {
            authModule.secureResponse(messageInfo, receivedSubject);
            fail("There must be an AuthException thrown from the AuthModule.");
        } catch (AuthException e) {
            assertEquals("The AuthException cause must be set.", AuthenticationException.class, e.getCause().getClass());
        }
    }

    private AuthModuleTest withBeanInstance() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(cdi).select(HttpAuthenticationMechanism.class);
                will(returnValue(beanInstance));
            }
        });
        return this;
    }

    private AuthModuleTest withOneMechanism() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(beanInstance).get();
                will(returnValue(httpAuthenticationMechanism));
            }
        });
        return this;
    }

    private AuthModuleTest authMechValidatesRequest(final AuthenticationStatus status) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpAuthenticationMechanism).validateRequest(with(request), with(response), with(aNonNull(HttpMessageContext.class)));
                will(returnValue(status));
            }
        });
        return this;
    }

    private AuthModuleTest authMechValidateRequestThrowsException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpAuthenticationMechanism).validateRequest(with(request), with(response), with(aNonNull(HttpMessageContext.class)));
                will(throwException(new AuthenticationException()));
            }
        });
        return this;
    }

    private AuthModuleTest authMechSecuresResponse(final AuthenticationStatus status) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpAuthenticationMechanism).secureResponse(with(request), with(response), with(aNonNull(HttpMessageContext.class)));
                will(returnValue(status));
            }
        });
        return this;
    }

    private AuthModuleTest authMechSecuresResponseThrowsException() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(httpAuthenticationMechanism).secureResponse(with(request), with(response), with(aNonNull(HttpMessageContext.class)));
                will(throwException(new AuthenticationException()));
            }
        });
        return this;
    }

    private void initializeModule() throws Exception {
        MessagePolicy requestPolicy = newMessagePolicy(true, newProtectionPolicy(ProtectionPolicy.AUTHENTICATE_SENDER));
        CallbackHandler handler = mockery.mock(CallbackHandler.class);
        Map options = new HashMap<String, String>();
        authModule.initialize(requestPolicy, null, handler, options);
    }

    private MessagePolicy newMessagePolicy(boolean isMandatory, ProtectionPolicy policy) {
        TargetPolicy[] policies = new TargetPolicy[] { new TargetPolicy((Target[]) null, policy) };
        return new MessagePolicy(policies, isMandatory);
    }

    private ProtectionPolicy newProtectionPolicy(final String id) {
        return new ProtectionPolicy() {

            @Override
            public String getID() {
                return id;
            }
        };
    }

    private MessageInfo createMessageInfo(boolean mandatory) {
        MessageInfo messageInfo = new JaspiMessageInfo(request, response);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }

//
//    @Test
//    public void testCleanSubject() {
//        fail("Not yet implemented");
//    }
//

//

}
