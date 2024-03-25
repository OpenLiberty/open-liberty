/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet.internal.osgi.container.config.factory;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelper;
import com.ibm.ws.webcontainer.osgi.container.config.WebAppConfiguratorHelperFactory;

import io.openliberty.webcontainer60.osgi.container.config.WebAppConfiguratorHelper60;

/**
 *
 */
@Component(service = WebAppConfiguratorHelperFactory.class, property = { "service.vendor=IBM" })
public class WebAppConfiguratorFactory61Impl implements WebAppConfiguratorHelperFactory {

    @Override
    public WebAppConfiguratorHelper createWebAppConfiguratorHelper(ServletConfigurator configurator,
                                                                   ResourceRefConfigFactory resourceRefConfigFactory, List<Class<?>> listenerInterfaces) {
        return new WebAppConfiguratorHelper60(configurator, resourceRefConfigFactory, listenerInterfaces);
    }
}
