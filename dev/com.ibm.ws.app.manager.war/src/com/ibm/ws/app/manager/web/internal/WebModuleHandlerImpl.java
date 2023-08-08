/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.app.manager.web.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleHandlerBase;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;

@Component(service = ModuleHandler.class,
           property = { "type:String=web" })
public class WebModuleHandlerImpl extends ModuleHandlerBase {

    @Reference(target = "(type=web)")
    protected void setWebContainer(ModuleRuntimeContainer webContainer) {
        super.setModuleRuntimeContainer(webContainer);
    }
}
