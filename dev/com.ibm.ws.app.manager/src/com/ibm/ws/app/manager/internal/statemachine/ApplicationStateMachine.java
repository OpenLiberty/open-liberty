/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.statemachine;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleContext;

import com.ibm.ws.app.manager.internal.ApplicationConfig;
import com.ibm.ws.app.manager.internal.ApplicationDependency;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.application.ApplicationState;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 *
 */
public abstract class ApplicationStateMachine {

    public interface ASMHelper {
        public void switchApplicationState(ApplicationConfig applicationConfig, ApplicationState newAppState);

        public boolean appTypeSupported();

        public void notifyAppStarted(String configPid);

        public void notifyAppFailed(String configPid);
    }

    public static ApplicationStateMachine newInstance(BundleContext ctx, WsLocationAdmin locAdmin, FutureMonitor futureMonitor,
                                                      ArtifactContainerFactory artifactFactory, AdaptableModuleFactory moduleFactory,
                                                      ExecutorService executorService, ScheduledExecutorService scheduledExecutor,
                                                      ASMHelper asmHelper, ApplicationMonitor appMonitor) {
        return new ApplicationStateMachineImpl(ctx, locAdmin, futureMonitor, artifactFactory, moduleFactory, executorService, scheduledExecutor, asmHelper, appMonitor);
    }

    public abstract Future<Boolean> start();

    public abstract Future<Boolean> stop();

    public abstract void restart();

    public abstract void setAppHandler(ApplicationHandler<?> appHandler);

    public abstract void configure(ApplicationConfig appConfig,
                                   Collection<ApplicationDependency> appStartingFutures,
                                   Collection<ApplicationDependency> startAfterFutures,
                                   ApplicationDependency notifyAppStopped,
                                   ApplicationDependency notifyAppStarting,
                                   ApplicationDependency notifyAppInstalled,
                                   ApplicationDependency notifyAppStarted);

    public abstract void recycle(Collection<ApplicationDependency> appStartingConditions,
                                 ApplicationDependency notifyAppStopped,
                                 ApplicationDependency notifyAppInstalled,
                                 ApplicationDependency notifyAppStarted);

    public abstract void uninstall(ApplicationDependency notifyAppRemoved);

    public abstract void describe(StringBuilder sb);

    public abstract boolean isBlocked();
}
