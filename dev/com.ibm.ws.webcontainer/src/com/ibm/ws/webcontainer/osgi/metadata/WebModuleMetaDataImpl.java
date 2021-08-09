/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.metadata;

import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.webcontainer.metadata.BaseJspComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebCollaboratorComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *  WAS impl also implements ComponentMetaDataFactory for dynamic creation of ComponentMetaData
 */
public class WebModuleMetaDataImpl extends MetaDataImpl implements WebModuleMetaData {

    private final ApplicationMetaData applicationMetaData;
    private J2EEName j2eeName;
    private WebCollaboratorComponentMetaData wccmd;
    private Object securityMetaData;
    private Object annotatedSecurityMetaData;
    private WebAppConfiguration webAppConfig = null;
    boolean _isServlet23OrHigher = false;
    private BaseJspComponentMetaData jspComponentMetaData = null;
    private String sessionCookieNameInUse = null;
    public ServiceRegistration<?> mBeanServiceReg;

    /**
     * @param slotCnt
     */
    public WebModuleMetaDataImpl(ApplicationMetaData amd) {
        super(0);
        this.applicationMetaData = amd;
    }
    
    public void setWebAppConfiguration(WebAppConfiguration config)
    {
            webAppConfig = config;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized WebCollaboratorComponentMetaData getCollaboratorComponentMetaData() {
        return this.wccmd;
    }

    /** {@inheritDoc} */
    @Override
    public WebAppConfig getConfiguration() {
        return webAppConfig;
    }

    /** {@inheritDoc} */
    @Override
    public BaseJspComponentMetaData getJspComponentMetadata() {
        return jspComponentMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public Object getSecurityMetaData() {
        return securityMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public String getSessionCookieNameInUse() {
        return sessionCookieNameInUse;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isServlet23OrHigher() {
        return _isServlet23OrHigher;
    }
    
    public void setIsServlet23OrHigher(boolean isServlet23OrHigher)
    {
        _isServlet23OrHigher = isServlet23OrHigher;
    }

    /** {@inheritDoc} */
    @Override
    public void setCollaboratorComponentMetaData(WebCollaboratorComponentMetaData webCollabCMD) {
        this.wccmd = webCollabCMD;

    }

    /** {@inheritDoc} */
    @Override
    public void setJspComponentMetadata(BaseJspComponentMetaData jspMetaData) {
        jspComponentMetaData = jspMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public void setSecurityMetaData(Object securityMetaData) {
        this.securityMetaData=securityMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public void setSessionCookieNameInUse(String sessionCookieNameInUse) {
        this.sessionCookieNameInUse = sessionCookieNameInUse;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        String name = null;
        if (webAppConfig!=null) {
            name=webAppConfig.getModuleName();
        }
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public void release() {
        // TODO Auto-generated method stub
        
    }

    /** {@inheritDoc} */
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return applicationMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public ComponentMetaData[] getComponentMetaDatas() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setJ2EEName(J2EEName j2eeName) {
        this.j2eeName = j2eeName;
    }

    @Override
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    /** {@inheritDoc} */
    @Override
    public Object getAnnotatedSecurityMetaData() {
        return annotatedSecurityMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public void setAnnotatedSecurityMetaData(Object annotatedSecurityMetaData) {
        this.annotatedSecurityMetaData=annotatedSecurityMetaData;
        
    }

}
