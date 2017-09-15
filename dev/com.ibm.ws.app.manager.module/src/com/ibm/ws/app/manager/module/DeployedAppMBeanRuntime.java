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
package com.ibm.ws.app.manager.module;

import java.util.List;

import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
public interface DeployedAppMBeanRuntime {
    ServiceRegistration<?> registerApplicationMBean(String appName, Container container, String ddPath, List<ModuleInfo> modules);
}
