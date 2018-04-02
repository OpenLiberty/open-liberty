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
package com.ibm.ws.springboot.support.web.server.internal;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory;
import com.ibm.ws.app.manager.springboot.support.SpringBootApplication;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
@Component
public class WebInstanceFactory implements ContainerInstanceFactory<WebInitializer> {
    @Reference(service = ModuleHandler.class, target = "(type=web)")
    private ModuleHandler moduleHandler;
    private DeployedAppInfoFactoryBase deployedAppFactory;
    private BundleContext context;
    @Reference
    private FutureMonitor futureMonitor;

    @Reference(target = "(type=war)")
    protected void setDeployedAppFactory(DeployedAppInfoFactory deployedAppFactory) {
        this.deployedAppFactory = (DeployedAppInfoFactoryBase) deployedAppFactory;
    }

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Override
    public Class<WebInitializer> getType() {
        return WebInitializer.class;
    }

    @Override
    public Instance intialize(SpringBootApplication app, String id, WebInitializer initializer) throws IOException, UnableToAdaptException, MetaDataException {
        return new WebInstance(this, app, id, initializer);
    }

    ModuleHandler getModuleHandler() {
        return moduleHandler;
    }

    DeployedAppInfoFactoryBase getDeployedAppFactory() {
        return deployedAppFactory;
    }

    FutureMonitor getFutureMonitor() {
        return futureMonitor;
    }

    BundleContext getContext() {
        return context;
    }
}
