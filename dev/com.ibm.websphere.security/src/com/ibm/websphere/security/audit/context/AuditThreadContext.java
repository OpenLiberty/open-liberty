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
package com.ibm.websphere.security.audit.context;

import java.util.ArrayList;

/**
 * <p>This interface encapsulates the artifacts pertaining to an auditable transaction.</p>
 *
 * <p>Implementations of this interface are not guaranteed to be thread safe</p>
 *
 * @ibm-spi
 */
public class AuditThreadContext {

    private Object auditReq;
    private Object auditWebReq;
    private String auditRealm;
    private ArrayList<String> delegatedUsers;
    private String repositoryId;
    private String repositoryUniqueName;
    private String repositoryRealm;
    private Object restRequest;
    private Object jmsConversationMetaData;
    private String jmsBusName;
    private String jmsMessagingEngine;

    public void setHttpServletRequest(Object req) {
        auditReq = req;
    }

    public Object getHttpServletRequest() {
        return auditReq;
    }

    public void setWebRequest(Object webreq) {
        auditWebReq = webreq;
    }

    public Object getWebRequest() {
        return auditWebReq;
    }

    public void setRealm(String realm) {
        auditRealm = realm;
    }

    public String getRealm() {
        return auditRealm;
    }

    public void setDelegatedUsers(ArrayList<String> delUsers) {
        if (delUsers != null) {
            delegatedUsers = new ArrayList<String>(delUsers);
        }
    }

    public ArrayList<String> getDelegatedUsers() {
        return delegatedUsers;
    }

    public void setRESTRequest(Object request) {
        restRequest = request;
    }

    public Object getRESTRequest() {
        return restRequest;
    }

    public void setJMSConversationMetaData(Object JMSConversationMetaData) {
        jmsConversationMetaData = JMSConversationMetaData;
    }

    public Object getJMSConversationMetaData() {
        return jmsConversationMetaData;
    }

    public void setJMSBusName(String busName) {
        jmsBusName = busName;
    }

    public String getJMSBusName() {
        return jmsBusName;
    }

    public void setJMSMessagingEngine(String me) {
        jmsMessagingEngine = me;
    }

    public String getJMSMessagingEngine() {
        return jmsMessagingEngine;
    }

    public void setRepositoryId(String repoId) {
        repositoryId = repoId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryUniqueName(String uniqueName) {
        repositoryUniqueName = uniqueName;
    }

    public String getRepositoryUniqueName() {
        return repositoryUniqueName;
    }

    public void setRepositoryRealm(String realm) {
        repositoryRealm = realm;
    }

    public String getRepositoryRealm() {
        return repositoryRealm;
    }

}
