/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class MetaDataListenerImpl implements ModuleMetaDataListener {
    private static final TraceComponent tc = Tr.register(MetaDataListenerImpl.class);

    protected static final String KEY_WEB_JACC_SERVICE = "webJaccService";
    protected final AtomicServiceReference<WebJaccService> webJaccService = new AtomicServiceReference<WebJaccService>(KEY_WEB_JACC_SERVICE);

    private WebDeclaredRolesService rolesService;

    protected void setDeclaredRolesService(WebDeclaredRolesService rolesService) {
        this.rolesService = rolesService;
    }

    protected void unsetDeclaredRolesService(WebDeclaredRolesService rolesService) {
        if (rolesService == this.rolesService) {
            this.rolesService = null;
        }
    }

    protected void setWebJaccService(ServiceReference<WebJaccService> reference) {
        webJaccService.setReference(reference);
    }

    protected void unsetWebJaccService(ServiceReference<WebJaccService> reference) {
        webJaccService.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        webJaccService.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        webJaccService.deactivate(cc);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        MetaData metaData = event.getMetaData();
        if (metaData instanceof WebModuleMetaData) {
            WebModuleMetaData wmmd = (WebModuleMetaData) metaData;
            SecurityMetadata securityMetadata = (SecurityMetadata) wmmd.getSecurityMetaData();
            if (securityMetadata != null) {
                WebAppConfig webAppConfig = wmmd.getConfiguration();
                String appName = webAppConfig.getApplicationName();
                String moduleName = webAppConfig.getModuleName();
                WebJaccService js = webJaccService.getService();
                // propagate the security constraints when it is available.
                if (js != null && securityMetadata.getSecurityConstraintCollection() != null) {
                    js.propagateWebConstraints(appName, moduleName, webAppConfig);
                }
                rolesService.addDeclaredRoles(appName, moduleName, securityMetadata.getRoles());
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
        MetaData metaData = event.getMetaData();
        if (metaData instanceof WebModuleMetaData) {
            WebModuleMetaData wmmd = (WebModuleMetaData) metaData;
            WebAppConfig webAppConfig = wmmd.getConfiguration();
            rolesService.removeModule(webAppConfig.getApplicationName(), webAppConfig.getModuleName());
        }
    }

}
