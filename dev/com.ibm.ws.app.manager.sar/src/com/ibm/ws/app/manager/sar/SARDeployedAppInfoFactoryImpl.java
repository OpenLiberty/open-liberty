/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.sar;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;

@Component(service = DeployedAppInfoFactory.class,
property = { "service.vendor=IBM", "type:String=sar" })
public class SARDeployedAppInfoFactoryImpl extends DeployedAppInfoFactoryBase {

	protected ModuleHandler webModuleHandler;
    /**
     * setting the webModuleHandler that is used by the deployedAppInfo class.
     * @param handler
     */
    @Reference(target = "(type=web)")
    protected void setWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = handler;
    }

    /**
     * unsetting the WebModuleHandler
     * @param handler
     */
    protected void unsetWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = null;
    }

    /**
     * creating the appInfo
     * @see com.ibm.ws.app.manager.module.DeployedAppInfoFactory#createDeployedAppInfo(com.ibm.wsspi.application.handler.ApplicationInformation)
     */
    @Override
    public SARDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
    	SARDeployedAppInfo deployedApp = new SARDeployedAppInfo(applicationInformation, this);
        applicationInformation.setHandlerInfo(deployedApp);
        return deployedApp;
    }
}
