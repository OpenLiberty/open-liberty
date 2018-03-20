/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleHandlerBase;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;

@Component(service = ModuleHandler.class,
           property = { "service.vendor=IBM", "type=" + SpringConstants.SPRING_APP_TYPE })
public class SpringBootModuleHandler extends ModuleHandlerBase {

    @Reference(target = "(type=spring)")
    protected void setRuntimeContainer(ModuleRuntimeContainer springContainer) {
        super.setModuleRuntimeContainer(springContainer);
    }
}
