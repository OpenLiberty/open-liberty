/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.metadata;

import java.util.List;

import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.SyncToOSThreadMetaData;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.webcontainer.metadata.BaseJspComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebCollaboratorComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 *  WAS impl also implements ComponentMetaDataFactory for dynamic creation of ComponentMetaData
 */
public class WebModuleMetaDataImpl extends MetaDataImpl implements WebModuleMetaData, SyncToOSThreadMetaData {

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
    // Have 3 states to track: not checked (null), checked and doesn't have HAM (Boolean.FALSE), checked and has HAM (Boolean.TRUE)
    private Boolean hasHAM = null;

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

    public boolean checkForHAM() {
        // Only return false if it has been checked and found to be false.
        return hasHAM != Boolean.FALSE;
    }
    
    public void setHasHAM(boolean hamFound) {
        hasHAM = (hamFound ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public boolean isSyncToOSThreadEnabled() {

        boolean syncToOSThread = false;

        List<EnvEntry> envEntries = webAppConfig.getEnvEntries();

        // Copied from SecurityServletConfiguratorHelper.java.
        final String SYNC_TO_OS_THREAD_ENV_ENTRY_KEY = "com.ibm.websphere.security.SyncToOSThread";

        for (EnvEntry envEntry : envEntries) {
            if (SYNC_TO_OS_THREAD_ENV_ENTRY_KEY.equals(envEntry.getName())) {
                syncToOSThread = Boolean.parseBoolean(envEntry.getValue());
                break;
            }
        }
        return syncToOSThread;
    }
}
