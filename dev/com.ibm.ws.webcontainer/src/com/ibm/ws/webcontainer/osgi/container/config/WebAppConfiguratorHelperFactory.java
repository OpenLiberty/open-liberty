/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config;

import java.util.List;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.resource.ResourceRefConfigFactory;


/**
 *
 */
public interface WebAppConfiguratorHelperFactory {

    WebAppConfiguratorHelper createWebAppConfiguratorHelper(ServletConfigurator configurator,
                                                            ResourceRefConfigFactory resourceRefConfigFactory,
                                                            List<Class<?>> listenerInterfaces);
}
