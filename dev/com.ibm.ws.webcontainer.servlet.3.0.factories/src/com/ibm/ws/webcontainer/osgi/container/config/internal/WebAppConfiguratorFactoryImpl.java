/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi.container.config.internal;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelperFactory;

@Component(service = WebAppConfiguratorHelperFactory.class, property = { "service.vendor=IBM" })
public class WebAppConfiguratorFactoryImpl implements WebAppConfiguratorHelperFactory {

    @Override
    public WebAppConfiguratorHelper createWebAppConfiguratorHelper(ServletConfigurator configurator,
                                                                   ResourceRefConfigFactory resourceRefConfigFactory, List<Class<?>> listenerInterfaces) {
        return new WebAppConfiguratorHelper(configurator, resourceRefConfigFactory, listenerInterfaces);
    }
}
