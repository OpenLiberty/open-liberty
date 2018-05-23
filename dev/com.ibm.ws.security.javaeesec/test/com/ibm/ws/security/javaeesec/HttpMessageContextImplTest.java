/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.CallerPrincipal;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.servlet.RequestDispatcher;
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
import com.ibm.ws.security.jaspi.JaspiMessageInfo;
import com.ibm.ws.security.jaspi.NonMappingCallbackHandler;
import com.ibm.ws.webcontainer.security.JaspiService;
import com.ibm.wsspi.security.token.AttributeNameConstants;

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
    private JaspiService jaspiService;
    private CallbackHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private CallerPrincipal principal;
    private final String principalName = "user1";
    private Set<String> groups;
    private final String path = "/login.jsp";
    private RequestDispatcher requestDispatcher;

    @Before
    public void setUp() throws Exception {
        request = mockery.mock(HttpServletRequest.class);
        response = mockery.mock(HttpServletResponse.class);
        messageInfo = createMessageInfo(true);
        clientSubject = new Subject();
        jaspiService = mockery.mock(JaspiService.class);
        handler = new NonMappingCallbackHandler(jaspiService);
        principal = new CallerPrincipal(principalName);
        groups = new HashSet<String>();
        groups.add("tester");
        httpMessageContext = new HttpMessageContextImpl(messageInfo, clientSubject, handler);
    }

    @SuppressWarnings("unchecked")
    private MessageInfo createMessageInfo(boolean mandatory) {
        MessageInfo messageInfo = new JaspiMessageInfo(request, response);
        messageInfo.getMap().put(IS_MANDATORY_POLICY, Boolean.toString(mandatory));
        return messageInfo;
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

//    @Test
//    public void testCleanClientSubject() {
//
//    }
//
    @Test
    public void testDoNothing() {
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.NOT_DONE, httpMessageContext.doNothing());
    }

    @Test
    public void testResponseNotFound() {
        expectResponseStatus(HttpServletResponse.SC_NOT_FOUND);
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.SEND_FAILURE, httpMessageContext.responseNotFound());
    }

    @Test
    public void testResponseUnauthorized() {
        expectResponseStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertEquals("The AuthenticationStatus must be NOT_DONE.", AuthenticationStatus.SEND_FAILURE, httpMessageContext.responseUnauthorized());
    }

    private void expectResponseStatus(final int status) {
        mockery.checking(new Expectations() {
            {
                one(response).setStatus(status);
            }
        });
    }

    @Test
    public void testRedirect() throws Exception {
        String location = "https://localhost/contextRoot/someplace";
        setResponseExpectations(location);
        AuthenticationStatus status = httpMessageContext.redirect(location);
        assertEquals("The status must be set.", AuthenticationStatus.SEND_CONTINUE, status);
    }

    private void setResponseExpectations(final String location) throws Exception {
        mockery.checking(new Expectations() {
            {
                one(response).encodeURL(location);
                will(returnValue(location));
                one(response).sendRedirect(location);
            }
        });
        expectResponseStatus(HttpServletResponse.SC_FOUND);
    }

    @Test
    public void testForward() throws Exception {
        setRequestExpectations();
        AuthenticationStatus status = httpMessageContext.forward("/login.jsp");
        assertEquals("The status must be set.", AuthenticationStatus.SEND_CONTINUE, status);
    }

    private void setRequestExpectations() throws Exception {
        requestDispatcher = mockery.mock(RequestDispatcher.class);
        mockery.checking(new Expectations() {
            {
                one(request).getRequestDispatcher(path);
                will(returnValue(requestDispatcher));
                one(requestDispatcher).forward(request, response);
            }
        });
    }

    @Test
    public void testGetAuthParameters() {
        AuthenticationParameters authParams = httpMessageContext.getAuthParameters();
        assertNotNull("There must be an AuthenticationParameters default instance.", authParams);
        AuthenticationParameters authenticationParameters = new AuthenticationParameters();
        httpMessageContext = new HttpMessageContextImpl(messageInfo, clientSubject, handler, authenticationParameters);
        assertTrue("The AuthenticationParameters must be set.", authParams != httpMessageContext.getAuthParameters());
    }

    @Test
    public void testGetClientSubject() {
        assertSame("The client subject must be set.", clientSubject, httpMessageContext.getClientSubject());
    }

    @Test
    public void testGetHandler() {
        assertSame("The callback handler must be set.", handler, httpMessageContext.getHandler());
    }

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
    public void testSetRequest() {
        HttpServletRequest newRequest = mockery.mock(HttpServletRequest.class, "newRequest");
        httpMessageContext.setRequest(newRequest);
        assertEquals("The HttpServletRequest must be replaced.", newRequest, httpMessageContext.getRequest());
    }

    @Test
    public void testSetResponse() {
        HttpServletResponse newResponse = mockery.mock(HttpServletResponse.class, "newResponse");
        httpMessageContext.setResponse(newResponse);
        assertEquals("The HttpServletResponse must be replaced.", newResponse, httpMessageContext.getResponse());
    }

    @Test
    public void testWithRequest() {
        HttpServletRequest newRequest = mockery.mock(HttpServletRequest.class, "newRequest");
        assertEquals("The HttpServletRequest must be replaced.", newRequest, httpMessageContext.withRequest(newRequest).getRequest());
    }

    @Test
    public void testIsAuthenticationRequest() {
        assertFalse("The request must not be an authentication request.", httpMessageContext.isAuthenticationRequest());
        AuthenticationParameters authenticationParameters = new AuthenticationParameters();
        httpMessageContext = new HttpMessageContextImpl(messageInfo, clientSubject, handler, authenticationParameters);
        assertTrue("The request must be an authentication request.", httpMessageContext.isAuthenticationRequest());
    }

    @Test
    public void testIsProtected() {
        assertTrue("The resource must be protected when the mandatory property is set in the MessageInfo.", httpMessageContext.isProtected());
        httpMessageContext = new HttpMessageContextImpl(createMessageInfo(false), clientSubject, handler);
        assertFalse("The resource must not be protected when the mandatory property is not set in the MessageInfo.", httpMessageContext.isProtected());
    }

    @Test
    public void testNotifyContainerAboutLoginPrincipalSetOfString() {
        setJaspicExpectations();
        AuthenticationStatus status = httpMessageContext.notifyContainerAboutLogin(principal, groups);
        assertGoodNotification(status);
    }

    @Test
    public void testNotifyContainerAboutLoginCredentialValidationResult() {
        setJaspicExpectations();
        CredentialValidationResult result = new CredentialValidationResult(principal, groups);
        AuthenticationStatus status = httpMessageContext.notifyContainerAboutLogin(result);
        assertGoodNotification(status);
    }

    @Test
    public void testNotifyContainerAboutLoginCredentialValidationResultIdStoreIdAsRealm() {
        setJaspicExpectations();
        String idStoreId = "IdStoreId";
        CredentialValidationResult result = new CredentialValidationResult(idStoreId, principal, null, principalName, groups);
        httpMessageContext.notifyContainerAboutLogin(result);

        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertEquals("The unique id must be set in the subject.", "user:" + idStoreId + "/" + principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertEquals("The realm name must be set in the subject.", idStoreId, customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
    }

    @Test
    public void testNotifyContainerAboutLoginCredentialValidationResultFAILURE() {
        setJaspicExpectations();

        CredentialValidationResult result = CredentialValidationResult.INVALID_RESULT;
        AuthenticationStatus status = httpMessageContext.notifyContainerAboutLogin(result);

        assertSubjectContentsNotAdded();
        assertNull("The principal must not be set in the context.", httpMessageContext.getCallerPrincipal());
        assertTrue("The groups must not be set in the context.", httpMessageContext.getGroups().isEmpty());
        assertEquals("The status must be set.", AuthenticationStatus.SEND_FAILURE, status);
    }

    @Test
    public void testNotifyContainerAboutLoginStringSetOfString() {
        setJaspicExpectations();

        httpMessageContext.notifyContainerAboutLogin(principalName, groups);

        assertCommonSubjectContents();
        Set<String> contextGroups = httpMessageContext.getGroups();
        assertTrue("The groups must be exacty the same as notified.", groups.containsAll(contextGroups) && contextGroups.containsAll(groups));
    }

    @Test
    public void testRegisterSession() {
        assertFalse("The session must not be registered.", httpMessageContext.isRegisterSession());
        httpMessageContext.setRegisterSession(principalName, groups);
        assertTrue("The session must be registered.", httpMessageContext.isRegisterSession());
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

    private void assertGoodNotification(AuthenticationStatus status) {
        assertMessageContextContents();
        assertSubjectContents();
        assertEquals("The status must be set.", AuthenticationStatus.SUCCESS, status);
    }

    private void assertMessageContextContents() {
        assertSame("The principal must be set in the context.", principal, httpMessageContext.getCallerPrincipal());
        Set<String> contextGroups = httpMessageContext.getGroups();
        assertTrue("The groups must be exacty the same as notified.", groups.containsAll(contextGroups) && contextGroups.containsAll(groups));
    }

    private void assertSubjectContents() {
        Hashtable<String, ?> customProperties = assertCommonSubjectContents();
        assertSame("The principal must be set in the subject.", principal, customProperties.get("com.ibm.wsspi.security.cred.jaspi.principal"));
    }

    @SuppressWarnings("unchecked")
    private Hashtable<String, ?> assertCommonSubjectContents() {
        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertEquals("The unique id must be set in the subject.", "user:defaultRealm/" + principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID));
        assertNull("The user id must not be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_USERID));
        assertEquals("The security name must be set in the subject.", principalName, customProperties.get(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME));
        List<String> subjectGroups = (List<String>) customProperties.get(AttributeNameConstants.WSCREDENTIAL_GROUPS);
        assertTrue("The groups must be set in the subject.", groups.containsAll(subjectGroups) && subjectGroups.containsAll(groups));
        assertNotNull("The realm name must be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_REALM));
        assertNull("The cache key must not be set in the subject.", customProperties.get(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY));
        return customProperties;
    }

    private void assertSubjectContentsNotAdded() {
        Hashtable<String, ?> customProperties = getSubjectHashtable();
        assertNull("The hashtable must not be set in the subject.", customProperties);
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

}
