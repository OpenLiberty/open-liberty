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
package com.ibm.ws.security.javaeesec.cdi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Hashtable;

import javax.interceptor.InvocationContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
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

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.utility.SubjectHelper;
import com.ibm.ws.security.jaspi.JaspiCallbackHandler;
import com.ibm.ws.security.jaspi.JaspiMessageInfo;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class AutoApplySessionInterceptorTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private AutoApplySessionInterceptor interceptor;
    private HttpMessageContext httpMessageContext;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private MessageInfo messageInfo;
    private Principal principal;
    private Subject clientSubject;
    private CallbackHandler handler;
    private JaspiService jaspiService;

    @Before
    public void setUp() throws Exception {
        interceptor = new AutoApplySessionInterceptor();
        httpMessageContext = mockery.mock(HttpMessageContext.class);
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        messageInfo = createMessageInfo(true);
        principal = null;
        clientSubject = new Subject();

        jaspiService = mockery.mock(JaspiService.class);
        handler = new JaspiCallbackHandler(jaspiService);

        mockery.checking(new Expectations() {
            {
                allowing(httpMessageContext).getRequest();
                will(returnValue(request));
                allowing(httpMessageContext).getMessageInfo();
                will(returnValue(messageInfo));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    @Test
    public void testInterceptValidateRequestWithNoPrincipalAndSuccess() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest");
        withPrincipal(null).invokesNextInterceptor(ic, AuthenticationStatus.SUCCESS);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.interceptValidateRequest(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
        assertTrue("The javax.servlet.http.registerSession property must be set in the MessageInfo's map.",
                   Boolean.valueOf((String) messageInfo.getMap().get("javax.servlet.http.registerSession")).booleanValue());
    }

    @Test
    public void testInterceptValidateRequestWithNoPrincipalAndFailure() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest");
        AuthenticationStatus nextInterceptorStatus = AuthenticationStatus.SEND_FAILURE;
        withPrincipal(null).invokesNextInterceptor(ic, nextInterceptorStatus);

        AuthenticationStatus status = (AuthenticationStatus) interceptor.interceptValidateRequest(ic);

        assertEquals("The AuthenticationStatus must be as returned from the next interceptor.", nextInterceptorStatus, status);
        assertFalse("The javax.servlet.http.registerSession property must not be set in the MessageInfo's map.",
                    Boolean.valueOf((String) messageInfo.getMap().get("javax.servlet.http.registerSession")).booleanValue());
    }

    @Test
    public void testInterceptValidateRequestWithPrincipal() throws Exception {
        InvocationContext ic = createInvocationContext("validateRequest");
        principal = new CallerPrincipal("user1");
        withPrincipal(principal).handlesCallback();

        AuthenticationStatus status = (AuthenticationStatus) interceptor.interceptValidateRequest(ic);

        assertEquals("The AuthenticationStatus must be AuthenticationStatus.SUCCESS.", AuthenticationStatus.SUCCESS, status);
        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertSame("The principal must be set in the subject.", principal, customProperties.get("com.ibm.wsspi.security.cred.jaspi.principal"));
    }

    @Test
    public void testSecureResponseNotIntercepted() throws Exception {
        doesNotIntercept("secureResponse");
    }

    @Test
    public void testCleanSubjectNotIntercepted() throws Exception {
        doesNotIntercept("cleanSubject");
    }

    private InvocationContext createInvocationContext(final String methodName) throws Exception {
        final InvocationContext ic = mockery.mock(InvocationContext.class);
        final Method method = getMethod(methodName);

        mockery.checking(new Expectations() {
            {
                one(ic).getMethod();
                will(returnValue(method));
            }
        });
        if ("validateRequest".equals(methodName)) {
            final Object[] parameters = new Object[3];
            parameters[0] = request;
            parameters[1] = response;
            parameters[2] = httpMessageContext;
            mockery.checking(new Expectations() {
                {
                    one(ic).getParameters();
                    will(returnValue(parameters));
                }
            });
        }

        return ic;
    }

    private AutoApplySessionInterceptorTest withPrincipal(final Principal principal) {
        mockery.checking(new Expectations() {
            {
                one(request).getUserPrincipal();
                will(returnValue(principal));
            }
        });
        return this;
    }

    private AutoApplySessionInterceptorTest invokesNextInterceptor(final InvocationContext invocationContext, final AuthenticationStatus status) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(invocationContext).proceed();
                will(returnValue(status));
            }
        });
        return this;
    }

    private Method getMethod(String methodName) throws Exception {
        return HttpAuthenticationMechanism.class.getMethod(methodName, HttpServletRequest.class, HttpServletResponse.class,
                                                           HttpMessageContext.class);
    }

    @SuppressWarnings("unchecked")
    private MessageInfo createMessageInfo(boolean mandatory) {
        MessageInfo messageInfo = new JaspiMessageInfo(request, response);
        messageInfo.getMap().put("javax.security.auth.message.MessagePolicy.isMandatory", Boolean.toString(mandatory));
        return messageInfo;
    }

    private AutoApplySessionInterceptorTest handlesCallback() {
        setJaspicExpectations();
        mockery.checking(new Expectations() {
            {
                one(httpMessageContext).getClientSubject();
                will(returnValue(clientSubject));
                one(httpMessageContext).getHandler();
                will(returnValue(handler));
            }
        });
        return this;
    }

    private void setJaspicExpectations() {
        final Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        clientSubject.getPrivateCredentials().add(hashtable);

        mockery.checking(new Expectations() {
            {
                allowing(jaspiService).getCustomCredentials(clientSubject);
                will(returnValue(hashtable));
            }
        });
    }

    private Hashtable<String, ?> getSubjectHashtable() {
        String[] hashtableLoginProperties = { AttributeNameConstants.WSCREDENTIAL_UNIQUEID,
                                              AttributeNameConstants.WSCREDENTIAL_USERID,
                                              AttributeNameConstants.WSCREDENTIAL_SECURITYNAME,
                                              AttributeNameConstants.WSCREDENTIAL_GROUPS,
                                              AttributeNameConstants.WSCREDENTIAL_REALM,
                                              AttributeNameConstants.WSCREDENTIAL_CACHE_KEY,
                                              AuthenticationConstants.INTERNAL_ASSERTION_KEY };
        return new SubjectHelper().getHashtableFromSubject(clientSubject, hashtableLoginProperties);
    }

    private void doesNotIntercept(String methodName) throws Exception {
        InvocationContext ic = createInvocationContext(methodName);
        AuthenticationStatus nextInterceptorStatus = AuthenticationStatus.SUCCESS;
        invokesNextInterceptor(ic, nextInterceptorStatus);
    
        AuthenticationStatus status = (AuthenticationStatus) interceptor.interceptValidateRequest(ic);
    
        assertEquals("The AuthenticationStatus must be as returned from the next interceptor.", nextInterceptorStatus, status);
        assertFalse("The javax.servlet.http.registerSession property must not be set in the MessageInfo's map.",
                    Boolean.valueOf((String) messageInfo.getMap().get("javax.servlet.http.registerSession")).booleanValue());
        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertNull("The hashtable must not be set in the subject.", customProperties);
    }

}
