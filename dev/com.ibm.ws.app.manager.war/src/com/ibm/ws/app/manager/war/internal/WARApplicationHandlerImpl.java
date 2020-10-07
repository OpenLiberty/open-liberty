/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.war.internal;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

@Component(service = { ApplicationHandler.class },
           property = { "service.vendor=IBM", "type:String=war" })
public class WARApplicationHandlerImpl implements ApplicationHandler<DeployedAppInfo> {
    private FutureMonitor futureMonitor;
    private DeployedAppInfoFactory deployedAppFactory;

    @Reference
    protected void setFutureMonitor(FutureMonitor fm) {
        futureMonitor = fm;
    }

    @Reference(target = "(type=war)")
    protected void setDeployedAppFactory(DeployedAppInfoFactory factory) {
        deployedAppFactory = factory;
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        Container oldContainer = applicationInformation.getContainer();

        final WARDeployedAppInfo deployedApp;
        try {
            deployedApp = (WARDeployedAppInfo) deployedAppFactory.createDeployedAppInfo(applicationInformation);
        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        return deployedApp.createApplicationMonitoringInformation(oldContainer);

    }

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        final Future<Boolean> result = futureMonitor.createFuture(Boolean.class);

        WARDeployedAppInfo deployedApp = (WARDeployedAppInfo) applicationInformation.getHandlerInfo();

        if (!deployedApp.deployApp(result)) {
            futureMonitor.setResult(result, false);
            return result;
        }

        return result;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        WARDeployedAppInfo deployedApp = (WARDeployedAppInfo) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return futureMonitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return futureMonitor.createFutureWithResult(success);
    }

    @Reference
    private void setApplicationStartBarrier(ApplicationStartBarrier applicationStartBarrier) {
    }
}
