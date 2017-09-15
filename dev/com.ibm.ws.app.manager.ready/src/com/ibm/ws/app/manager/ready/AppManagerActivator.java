/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ready;

import java.util.concurrent.ExecutorService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.ApplicationStateCoordinator;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 *
 */
public class AppManagerActivator implements BundleActivator {
    private static final TraceComponent _tc = Tr.register(AppManagerActivator.class);

    @Override
    public void start(BundleContext context) throws Exception {
        ServiceReference<?> serverStarted = context.getServiceReference("com.ibm.ws.kernel.feature.ServerStarted");
        if (serverStarted == null) {
            // this is initial startup we want to block here to report slowly started apps
            reportSlowlyStartingApps();
        } else {
            // this is after initial startup; we cannot block here because that will block any apps from starting
            ServiceReference<ExecutorService> executorRef = context.getServiceReference(ExecutorService.class);
            ExecutorService executor = executorRef == null ? null : context.getService(executorRef);
            if (executor == null) {
                // This is unexpected that the executor service is not available by this point.
                // better continue synchronously
                reportSlowlyStartingApps();
            } else {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        reportSlowlyStartingApps();
                    }
                });
                context.ungetService(executorRef);
            }
        }
    }

    private void reportSlowlyStartingApps() {
        try {
            String[] slowApps = ApplicationStateCoordinator.getSlowlyStartingApps();
            if (slowApps != null) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled()) {
                    Tr.event(_tc, "Stopped waiting for applications to start as the following did not start in time: ", (Object[]) slowApps);
                }
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, getClass().getName(), "start.exception");
            if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled()) {
                Tr.event(_tc, "Exception waiting for applications to start: ", ex);
            }
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        try {
            String[] slowApps = ApplicationStateCoordinator.getSlowlyStoppingApps();
            if (slowApps != null) {
                if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled()) {
                    Tr.event(_tc, "Stopped waiting for applications to stop as the following apps did not stop in time: ", (Object[]) slowApps);
                }
            }
        } catch (Exception ex) {
            FFDCFilter.processException(ex, getClass().getName(), "stop.exception");
            if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled()) {
                Tr.event(_tc, "Exception waiting for applications to stop: ", ex);
            }
        }
    }
}
