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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.jaspi.JaspiMessageInfo;

/**
 *
 */
public class AuthContextTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String IS_MANDATORY_POLICY = "javax.security.auth.message.MessagePolicy.isMandatory";

    private AuthContext authContext;
    private String authContextID;
    private Subject clientSubject;
    private Subject serviceSubject;
    private Map properties;
    private CallbackHandler handler;
    private ServerAuthModule module;

    @Before
    public void setUp() throws Exception {
        module = mockery.mock(ServerAuthModule.class);
        authContextID = "JASPI_PROTECTED";
        clientSubject = new Subject();
        serviceSubject = null;
        properties = new HashMap<String, String>();
        handler = mockery.mock(CallbackHandler.class);
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testCleanSubject() throws Exception {
        final MessageInfo messageInfo = createMessageInfo(true);
        createAuthContext(messageInfo);

        mockery.checking(new Expectations() {
            {
                one(module).cleanSubject(messageInfo, clientSubject);
            }
        });

        authContext.cleanSubject(messageInfo, clientSubject);
    }

    @Test
    public void testSecureResponse() throws Exception {
        final MessageInfo messageInfo = createMessageInfo(true);
        createAuthContext(messageInfo);

        mockery.checking(new Expectations() {
            {
                one(module).secureResponse(messageInfo, serviceSubject);
                will(returnValue(AuthStatus.SEND_SUCCESS));
            }
        });

        AuthStatus status = authContext.secureResponse(messageInfo, serviceSubject);
        assertEquals("The AuthContext's AuthStatus must be the same as the module's AuthStatus.", AuthStatus.SEND_SUCCESS, status);
    }

    @Test
    public void testValidateRequest() throws Exception {
        final MessageInfo messageInfo = createMessageInfo(true);
        createAuthContext(messageInfo);

        mockery.checking(new Expectations() {
            {
                one(module).validateRequest(messageInfo, clientSubject, serviceSubject);
                will(returnValue(AuthStatus.SUCCESS));
            }
        });

        AuthStatus status = authContext.validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals("The AuthContext's AuthStatus must be the same as the module's AuthStatus.", AuthStatus.SUCCESS, status);
    }

    @Test
    public void testValidateRequestWithNotMandatoryPolicy() throws Exception {
        final MessageInfo messageInfo = createMessageInfo(false);
        createAuthContext(messageInfo);

        mockery.checking(new Expectations() {
            {
                one(module).validateRequest(messageInfo, clientSubject, serviceSubject);
                will(returnValue(AuthStatus.SUCCESS));
            }
        });

        AuthStatus status = authContext.validateRequest(messageInfo, clientSubject, serviceSubject);
        assertEquals("The AuthContext's AuthStatus must be the same as the module's AuthStatus.", AuthStatus.SUCCESS, status);
    }

    private MessageInfo createMessageInfo(boolean mandatory) {
        HttpServletRequest req = mockery.mock(HttpServletRequest.class);
        HttpServletResponse rsp = mockery.mock(HttpServletResponse.class);
        MessageInfo messageInfo = new JaspiMessageInfo(req, rsp);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }

    private void createAuthContext(MessageInfo messageInfo) throws Exception {
        boolean mandatory;
        Object obj = messageInfo.getMap().get("javax.security.auth.message.MessagePolicy.isMandatory");
        if (Boolean.valueOf((String) obj)) {
            authContextID = "JASPI_PROTECTED";
            mandatory = true;
        } else {
            authContextID = "JASPI_UNPROTECTED";
            mandatory = false;
        }

        moduleMustInitialize(mandatory);
        authContext = new AuthContextTestDouble(authContextID, serviceSubject, properties, handler);
    }

    private void moduleMustInitialize(final boolean mandatory) throws AuthException {
        mockery.checking(new Expectations() {
            {
                one(module).initialize(with(new BaseMatcher<MessagePolicy>() {

                    // A MessagePolicy must be created with the correct mandatory value when authContextID is "JASPI_PROTECTED" or "JASPI_UNPROTECTED".
                    @Override
                    public boolean matches(Object obj) {
                        boolean result = false;
                        if (obj instanceof MessagePolicy) {
                            MessagePolicy policy = (MessagePolicy) obj;
                            result = policy.isMandatory() == mandatory;
                        }
                        return result;
                    }

                    @Override
                    public void describeTo(Description description) {}
                }), with(aNull(MessagePolicy.class)), with(handler), with(properties));
            }
        });
    }

    private class AuthContextTestDouble extends AuthContext {

        public AuthContextTestDouble(String authContextID, Subject serviceSubject, Map properties, CallbackHandler handler) {
            super(authContextID, serviceSubject, properties, handler);
        }

        @Override
        protected ServerAuthModule getAuthModule() {
            return module;
        }
    }

}
