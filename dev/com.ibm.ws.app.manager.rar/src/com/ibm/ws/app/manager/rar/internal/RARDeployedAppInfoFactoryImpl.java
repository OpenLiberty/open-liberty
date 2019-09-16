/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.rar.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type:String=rar" })
public class RARDeployedAppInfoFactoryImpl implements DeployedAppInfoFactory {

    @Reference
    protected DeployedAppServices deployedAppServices;
    @Reference(target = "(type=connector)")
    protected ModuleHandler rarModuleHandler;

    @Override
    public RARDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
    	RARDeployedAppInfo deployedApp = new RARDeployedAppInfo(applicationInformation, deployedAppServices, rarModuleHandler);
        applicationInformation.setHandlerInfo(deployedApp);
        return deployedApp;
    }
}
