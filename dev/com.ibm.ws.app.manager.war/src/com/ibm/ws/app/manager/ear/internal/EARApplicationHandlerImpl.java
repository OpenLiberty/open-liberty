/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ear.internal;

import java.util.concurrent.Future;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;

@Component(service = ApplicationHandler.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "type:String=ear" })
public class EARApplicationHandlerImpl implements ApplicationHandler<DeployedAppInfo> {
    private static final TraceComponent _tc = Tr.register(EARApplicationHandlerImpl.class);

    private FutureMonitor _monitor;
    private DeployedAppInfoFactory deployedAppFactory;

    private boolean isClient = false;

    /** {@inheritDoc} */
    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        final EARDeployedAppInfo deployedApp;
        try {
            deployedApp = (EARDeployedAppInfo) deployedAppFactory.createDeployedAppInfo(applicationInformation);

            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Created application [ " + deployedApp.getName() + " ] : Path [ " + deployedApp.getContainer().getPath() + " ]");
            }

        } catch (UnableToAdaptException e) {
            throw new IllegalStateException(e);
        }

        return deployedApp.createApplicationMonitoringInformation();
    }

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> applicationInformation) {

        final Future<Boolean> result = _monitor.createFuture(Boolean.class);

        String name = applicationInformation.getName();
        EARDeployedAppInfo deployedApp = (EARDeployedAppInfo) applicationInformation.getHandlerInfo();

        if (!deployedApp.hasModules()) {
            Tr.error(_tc, "error.no.modules", name);
            _monitor.setResult(result, false);
            return result;
        }

        if (!deployedApp.deployApp(result)) {
            if (isClient) {
                Tr.error(_tc, "error.client.not.installed", name);
            } else {
                Tr.error(_tc, "error.not.installed", name);
            }
            _monitor.setResult(result, false);
            return result;
        }

        return result;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        EARDeployedAppInfo deployedApp = (EARDeployedAppInfo) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return _monitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return _monitor.createFutureWithResult(success);
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setFutureMonitor(FutureMonitor fm) {
        _monitor = fm;
    }

    protected void unsetFutureMonitor(FutureMonitor fm) {
    }

    @Reference(name = "deployedAppFactory", target = "(type=ear)")
    protected void setDeployedAppFactory(DeployedAppInfoFactory deployedAppFactory) {
        this.deployedAppFactory = deployedAppFactory;
    }

    protected void unsetDeployedAppFactory(DeployedAppInfoFactory deployedAppFactory) {
    }

    @Activate
    protected void activate(ComponentContext context) {
        if (context.getBundleContext().getProperty("wlp.process.type").equals("client")) {
            isClient = true;
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        //
    }

    @Reference
    private void setApplicationStartBarrier(ApplicationStartBarrier applicationStartBarrier) {
    }
}
