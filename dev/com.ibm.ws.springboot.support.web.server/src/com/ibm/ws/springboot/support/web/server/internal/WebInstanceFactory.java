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

import static org.osgi.framework.Constants.OBJECTCLASS;

import java.io.IOException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory;
import com.ibm.ws.app.manager.springboot.support.SpringBootApplication;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.springboot.support.web.server.initializer.WebInitializer;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.http.VirtualHost;

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
    public Instance intialize(SpringBootApplication app, String id, String virtualHostID,
                              WebInitializer initializer, SpringConfiguration additionalConfig) throws IOException, UnableToAdaptException, MetaDataException {
        String filterString = "(&(" + OBJECTCLASS +
                              "=" + VirtualHost.class.getName() + ")(id=" + virtualHostID + "))";
        Filter filter = null;
        try {
            filter = context.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        ServiceTracker<VirtualHost, VirtualHost> tracker = new ServiceTracker<>(context, filter, null);
        return new WebInstance(this, app, id, virtualHostID, initializer, tracker, additionalConfig);
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
