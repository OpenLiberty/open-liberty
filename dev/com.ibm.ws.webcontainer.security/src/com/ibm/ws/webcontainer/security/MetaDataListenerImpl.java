/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class MetaDataListenerImpl implements ModuleMetaDataListener {
    private static final TraceComponent tc = Tr.register(MetaDataListenerImpl.class);

    protected static final String KEY_JACC_SERVICE = "jaccService";
    protected final AtomicServiceReference<JaccService> jaccService = new AtomicServiceReference<JaccService>(KEY_JACC_SERVICE);

    protected void setJaccService(ServiceReference<JaccService> reference) {
        jaccService.setReference(reference);
    }

    protected void unsetJaccService(ServiceReference<JaccService> reference) {
        jaccService.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        jaccService.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        jaccService.deactivate(cc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        JaccService js = jaccService.getService();
        if (js != null) {
            MetaData metaData = event.getMetaData();
            if (metaData instanceof WebModuleMetaData) {
                WebModuleMetaData wmmd = (WebModuleMetaData) metaData;
                if (wmmd.getSecurityMetaData() != null && ((SecurityMetadata) (wmmd.getSecurityMetaData())).getSecurityConstraintCollection() != null) {
                    // propagate the security constraints when it is available.
                    WebAppConfig webAppConfig = wmmd.getConfiguration();
                    js.propagateWebConstraints(webAppConfig.getApplicationName(), webAppConfig.getModuleName(), webAppConfig);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
        // TODO Auto-generated method stub

    }

}
