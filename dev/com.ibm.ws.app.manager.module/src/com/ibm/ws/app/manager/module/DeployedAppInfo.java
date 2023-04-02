/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module;

import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

public interface DeployedAppInfo {
    void moduleMetaDataCreated(ExtendedModuleInfo moduleInfo, ModuleHandler moduleHandler, ModuleMetaData mmd);

    DeployedModuleInfo getDeployedModule(ExtendedModuleInfo moduleInfo);

    boolean uninstallApp();
}
