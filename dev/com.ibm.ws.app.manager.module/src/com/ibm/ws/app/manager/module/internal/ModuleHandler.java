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
package com.ibm.ws.app.manager.module.internal;

import java.util.concurrent.Future;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public interface ModuleHandler {

    ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo, DeployedAppInfo deployedApp) throws MetaDataException;

    Future<Boolean> deployModule(DeployedModuleInfo deployedModule, DeployedAppInfo deployedApp);

    boolean undeployModule(DeployedModuleInfo deployedModule);
}
