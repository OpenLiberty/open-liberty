/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.sar;

import java.util.Collections;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppInfoFailure;
import com.ibm.ws.javaee.ddmodel.DDParser.ParseException;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.DefaultNotification;
import com.ibm.wsspi.adaptable.module.Notifier.Notification;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.application.handler.DefaultApplicationMonitoringInformation;

/**
 * @author SAGIA
 * The application handler for sar applications
 */
@Component(service = { ApplicationHandler.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "type:String=sar" })
public class SARApplicationHandlerImpl implements ApplicationHandler<DeployedAppInfo> {
  //  private static final TraceComponent _tc = Tr.register(SARApplicationHandlerImpl.class);

    private FutureMonitor futureMonitor;
    private DeployedAppInfoFactory deployedAppFactory;


    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setFutureMonitor(FutureMonitor fm) {
        futureMonitor = fm;
    }

    protected void unsetFutureMonitor(FutureMonitor fm) {}

    @Reference(name = "deployedAppFactory", target = "(type=sar)")
    protected void setDeployedAppFactory(DeployedAppInfoFactory factory) {
        deployedAppFactory = factory;
    }

    protected void unsetDeployedAppFactory(DeployedAppInfoFactory factory) {
        deployedAppFactory = null;
    }

    @Override
    public ApplicationMonitoringInformation setUpApplicationMonitoring(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        // Only monitor the /WEB-INF directory in the container
        Notification webInfNotification = new DefaultNotification(applicationInformation.getContainer(), "/WEB-INF");
        return new DefaultApplicationMonitoringInformation(Collections.singleton(webInfNotification), false);
    }

    @Override
    public Future<Boolean> install(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        final Future<Boolean> result = futureMonitor.createFuture(Boolean.class);

        SARDeployedAppInfo deployedApp;
        try {
            deployedApp = (SARDeployedAppInfo) deployedAppFactory.createDeployedAppInfo(applicationInformation);
        } catch (UnableToAdaptException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ParseException) {
                futureMonitor.setResult(result, new DeployedAppInfoFailure(cause.getMessage(), cause));
            } else {
                futureMonitor.setResult(result, ex);
            }
            return result;
        }

        if (!deployedApp.deployApp(result)) {
            futureMonitor.setResult(result, false);
            return result;
        }

        return result;
    }

    @Override
    public Future<Boolean> uninstall(ApplicationInformation<DeployedAppInfo> applicationInformation) {
        SARDeployedAppInfo deployedApp = (SARDeployedAppInfo) applicationInformation.getHandlerInfo();
        if (deployedApp == null) {
            // Somebody asked us to remove an app we don't know about
            return futureMonitor.createFutureWithResult(false);
        }

        boolean success = deployedApp.uninstallApp();
        return futureMonitor.createFutureWithResult(success);
    }
}
