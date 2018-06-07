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
package com.ibm.websphere.security.audit.context;

import java.util.ArrayList;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class AuditManager {

    private static ThreadLocal<AuditThreadContext> threadLocal = new AuditThreadLocal();

    /**
     * Sets the HttpServletRequest on the thread
     */
    public void setHttpServletRequest(Object req) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setHttpServletRequest(req);

    }

    /**
     * Gets the HttpServletRequest from the thread
     */
    public Object getHttpServletRequest() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getHttpServletRequest();
    }

    /**
     * Sets the WebRequest on the thread
     */
    public void setWebRequest(Object webreq) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setWebRequest(webreq);

    }

    /**
     * Gets the WebRequest from the thread
     */
    public Object getWebRequest() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getWebRequest();
    }

    /**
     * Sets the realm on the thread
     */
    public void setRealm(String realm) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRealm(realm);
    }

    /**
     * Gets the realm on the thread
     */
    public String getRealm() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRealm();
    }

    /**
     * Sets the JMS conversation on the thread
     */
    public void setJMSConversationMetaData(Object conversationMetaData) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setJMSConversationMetaData(conversationMetaData);
    }

    /**
     * Gets the REST request on the thread
     */
    public Object getJMSConversationMetaData() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getJMSConversationMetaData();
    }

    /**
     * Sets the JMS bus name on the thread
     */
    public void setJMSBusName(String busName) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setJMSBusName(busName);
    }

    /**
     * Gets the JMS bus name on the thread
     */
    public String getJMSBusName() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getJMSBusName();
    }

    /**
     * Sets whether we are doing a local or remote JMS call
     */
    public void setJMSCallType(String callType) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setJMSCallType(callType);
    }

    /**
     * Sets the JMS messaging engine on the thread
     */
    public void setJMSMessagingEngine(String me) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setJMSMessagingEngine(me);
    }

    /**
     * Gets the JMS messaging engine on the thread
     */
    public String getJMSMessagingEngine() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getJMSMessagingEngine();
    }

    /**
     * Sets the REST request on the thread
     */
    public void setRESTRequest(Object request) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRESTRequest(request);
    }

    /**
     * Gets the REST request on the thread
     */
    public Object getRESTRequest() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRESTRequest();
    }

    /**
     * Sets the repositoryId on the thread
     */
    public void setRepositoryId(String repoId) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRepositoryId(repoId);
    }

    /**
     * Gets the repositoryId on the thread
     */
    public String getRepositoryId() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRepositoryId();
    }

    /**
     * Sets the repository uniqueName on the thread
     */
    public void setRepositoryUniqueName(String uniqueName) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRepositoryUniqueName(uniqueName);
    }

    /**
     * Gets the repository uniqueName on the thread
     */
    public String getRepositoryUniqueName() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRepositoryUniqueName();
    }

    /**
     * Sets the repository realm on the thread
     */
    public void setRepositoryRealm(String realm) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRepositoryRealm(realm);
    }

    /**
     * Sets the CRUD request type on the thread
     */
    public void setRequestType(String requestType) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRequestType(requestType);;
    }

    /**
     * Sets the credential type of the user on the thread
     */
    public void setCredentialType(String cred) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setCredentialType(cred);
    }

    /**
     * Sets the user on the thread
     */
    public void setCredentialUser(String cred) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setCredentialUser(cred);
    }

    /**
     * Sets the remote address on the thread
     */
    public void setRemoteAddr(String addr) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setRemoteAddr(addr);
    }

    /**
     * Sets the agent type on the thread
     */
    public void setAgent(String agent) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setAgent(agent);
    }

    /**
     * Sets the local address requested on the thread
     */
    public void setLocalAddr(String addr) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setLocalAddr(addr);
    }

    /**
     * Sets the local port requested on the thread
     */
    public void setLocalPort(String port) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setLocalPort(port);
    }

    /**
     * Sets the sessionId on the thread
     */
    public void setSessionId(String id) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setSessionId(id);
    }

    /**
     * Sets the http type of the request on the thread
     */
    public void setHttpType(String type) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setHttpType(type);
    }

    /**
     * Gets the repository realm on the thread
     */
    public String getRepositoryRealm() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRepositoryRealm();
    }

    /**
     * Gets the CRUD request type on the thread
     */
    public String getRequestType() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRequestType();
    }

    /**
     * Gets the credential type of the authenticated user on this thread
     */
    public String getCredentialType() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getCredentialType();
    }

    /**
     * Gets the authenticated user on this thread
     */
    public String getCredentialUser() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getCredentialUser();
    }

    /**
     * Gets the remote address from the request on this thread
     */
    public String getRemoteAddr() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getRemoteAddr();
    }

    /**
     * Gets the agent type on this thread
     */
    public String getAgent() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getAgent();
    }

    /**
     * Gets the requested local address on this thread
     */
    public String getLocalAddr() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getLocalAddr();
    }

    /**
     * Gets the requested local port on this thread
     */
    public String getLocalPort() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getLocalPort();
    }

    /**
     * Gets the sessionId on this thread
     */
    public String getSessionId() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getSessionId();
    }

    /**
     * Gets the http type on this thread
     */
    public String getHttpType() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getHttpType();
    }

    /**
     * Sets the list of users from the initial caller through the last caller in a runAs delegation call
     */
    public void setDelegatedUsers(ArrayList<String> delegatedUsers) {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        auditThreadContext.setDelegatedUsers(delegatedUsers);
    }

    /**
     * Gets the list of users from the initial through the last in a runAs delegation call
     */
    public ArrayList<String> getDelegatedUsers() {
        AuditThreadContext auditThreadContext = getAuditThreadContext();
        return auditThreadContext.getDelegatedUsers();
    }

    /**
     * Gets the audit thread context that is unique per thread.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the subject thread context.
     */
    @Trivial
    protected AuditThreadContext getAuditThreadContext() {
        ThreadLocal<AuditThreadContext> currentThreadLocal = getThreadLocal();
        AuditThreadContext auditThreadContext = currentThreadLocal.get();
        if (auditThreadContext == null) {
            auditThreadContext = new AuditThreadContext();
            currentThreadLocal.set(auditThreadContext);
        }
        return auditThreadContext;
    }

    /**
     * Gets the thread local object.
     * If/when a common thread storage framework is supplied, then this method
     * implementation may need to be updated to take it into consideration.
     *
     * @return the thread local object.
     */
    @Trivial
    private ThreadLocal<AuditThreadContext> getThreadLocal() {
        return threadLocal;
    }

    private static final class AuditThreadLocal extends ThreadLocal<AuditThreadContext> {
        @Override
        protected AuditThreadContext initialValue() {
            return new AuditThreadContext();
        }
    }

}
