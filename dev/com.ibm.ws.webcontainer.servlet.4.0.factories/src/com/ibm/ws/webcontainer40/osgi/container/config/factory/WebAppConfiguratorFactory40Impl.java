/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.container.config.factory;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelperFactory;
import com.ibm.ws.webcontainer40.osgi.container.config.WebAppConfiguratorHelper40;

/**
 *
 */
@Component(service = WebAppConfiguratorHelperFactory.class, property = { "service.vendor=IBM" })
public class WebAppConfiguratorFactory40Impl implements WebAppConfiguratorHelperFactory {

    @Override
    public WebAppConfiguratorHelper createWebAppConfiguratorHelper(ServletConfigurator configurator,
                                                                   ResourceRefConfigFactory resourceRefConfigFactory, List<Class<?>> listenerInterfaces) {
        return new WebAppConfiguratorHelper40(configurator, resourceRefConfigFactory, listenerInterfaces);
    }
}
