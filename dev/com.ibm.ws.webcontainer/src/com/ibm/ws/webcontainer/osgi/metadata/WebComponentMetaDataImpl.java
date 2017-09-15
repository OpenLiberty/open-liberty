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

import java.util.ArrayList;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.webcontainer.osgi.WebContainerListener;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

/**
 *
 */
public class WebComponentMetaDataImpl extends MetaDataImpl implements WebComponentMetaData, IdentifiableComponentMetaData
 {
    private J2EEName j2eeName;
    WebModuleMetaData webModuleMetaData;
    public ServiceRegistration<?> mBeanServiceReg;
    
    /**
     * @param wccmd 
     * @param wmmd 
     * @param slotCnt
     */
    public WebComponentMetaDataImpl(WebModuleMetaData wmmd) {
        super(0);
        this.webModuleMetaData = wmmd;
    }

    public WebComponentMetaDataImpl(WebModuleMetaData wmmd, J2EEName j2eeName) {
        this(wmmd);
        this.j2eeName = j2eeName;
    }

    /** {@inheritDoc} */
    @Override
    public int getCallbacksId() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getImplementationClass() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList getPageListMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object getSecurityMetaData() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public IServletConfig getServletConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getWebComponentDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Map getWebComponentInitParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getWebComponentType() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getWebComponentVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void handleCallbacks() {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void handleCallbacks(int arg0) {
    // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public boolean isTypeJSP() {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int setCallbacksID() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void setSecurityMetaData(Object arg0) {
    // TODO Auto-generated method stub

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
    public ModuleMetaData getModuleMetaData() {
        return (ModuleMetaData) this.webModuleMetaData;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData#getPersistentIdentifier()
     */
    @Override
    public String getPersistentIdentifier() {
        return WebContainerListener
                        .getPersistentIdentifierImpl(webModuleMetaData.getJ2EEName().getApplication(),
                                                     webModuleMetaData.getJ2EEName().getModule());
    }
}
