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

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessageInfo;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class HttpMessageContextImpl implements HttpMessageContext {

    private static final TraceComponent tc = Tr.register(HttpMessageContextImpl.class);

    private final MessageInfo messageInfo;
    private final Subject clientSubject;
    private final CallbackHandler handler;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    /**
     * @param messageInfo
     * @param clientSubject
     * @param handler
     */
    public HttpMessageContextImpl(MessageInfo messageInfo, Subject clientSubject, CallbackHandler handler) {
        this.messageInfo = messageInfo;
        this.clientSubject = clientSubject;
        this.handler = handler;
        request = (HttpServletRequest) messageInfo.getRequestMessage();
        response = (HttpServletResponse) messageInfo.getResponseMessage();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#cleanClientSubject()
     */
    @Override
    public void cleanClientSubject() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#doNothing()
     */
    @Override
    public AuthenticationStatus doNothing() {
        return AuthenticationStatus.NOT_DONE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#forward(java.lang.String)
     */
    @Override
    public AuthenticationStatus forward(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getAuthParameters()
     */
    @Override
    public AuthenticationParameters getAuthParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getCallerPrincipal()
     */
    @Override
    public Principal getCallerPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getClientSubject()
     */
    @Override
    public Subject getClientSubject() {
        return clientSubject;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getGroups()
     */
    @Override
    public Set<String> getGroups() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getHandler()
     */
    @Override
    public CallbackHandler getHandler() {
        return handler;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getMessageInfo()
     */
    @Override
    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getRequest()
     */
    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getResponse()
     */
    @Override
    public HttpServletResponse getResponse() {
        return response;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isAuthenticationRequest()
     */
    @Override
    public boolean isAuthenticationRequest() {
        Map<String, String> msgMap = messageInfo.getMap();
        boolean isAuthenticate = "authenticate".equalsIgnoreCase(msgMap.get("com.ibm.websphere.jaspi.request"));
        return isAuthenticate;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isProtected()
     */
    @Override
    public boolean isProtected() {
        Map<String, String> msgMap = messageInfo.getMap();
        boolean isProtected = msgMap.get("javax.security.auth.message.MessagePolicy.isMandatory").equalsIgnoreCase("TRUE");
        return isProtected;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isRegisterSession()
     */
    @Override
    public boolean isRegisterSession() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(javax.security.enterprise.identitystore.CredentialValidationResult)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(CredentialValidationResult arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(java.lang.String, java.util.Set)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(String arg0, Set<String> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(java.security.Principal, java.util.Set)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(Principal arg0, Set<String> arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#redirect(java.lang.String)
     */
    @Override
    public AuthenticationStatus redirect(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#responseNotFound()
     */
    @Override
    public AuthenticationStatus responseNotFound() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#responseUnauthorized()
     */
    @Override
    public AuthenticationStatus responseUnauthorized() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setRegisterSession(java.lang.String, java.util.Set)
     */
    @Override
    public void setRegisterSession(String arg0, Set<String> arg1) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void setRequest(HttpServletRequest arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setResponse(javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void setResponse(HttpServletResponse arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#withRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public HttpMessageContext withRequest(HttpServletRequest arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
