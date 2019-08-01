/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal.scan;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.filemonitor.FileNotification;
import com.ibm.ws.kernel.filemonitor.internal.CoreServiceImpl;
import com.ibm.ws.kernel.filemonitor.internal.MonitorHolder;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * The traditional core service, used when we're below Java 1.7
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, configurationPid = "com.ibm.ws.kernel.filemonitor",
           service = { FileNotification.class, ServerQuiesceListener.class },
           property = { "service.vendor=IBM" })
public class ScanningCoreServiceImpl extends CoreServiceImpl {

    @Override
    protected MonitorHolder createMonitorHolder(ServiceReference<FileMonitor> monitorRef) {
        return new ScanningMonitorHolder(this, monitorRef);
    }

    /**
     * We don't do anything with the process, but having it set allows us to only be activated by DS if criteria we set
     * about the Java version are met.
     */
    @Reference(policy = ReferencePolicy.STATIC/* , target = "(&(java.specification.version<=1.7)(! (java.specification.version=1.7)))" */)
    protected void setProcess(LibertyProcess process) {

    }

    @Override
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        super.activate(componentContext, properties);
    }

    @Override
    protected void deactivate(int reason) {
        super.deactivate(reason);
    }

    @Override
    protected void modified(Map<String, Object> properties) {
        super.modified(properties);
    }

    @Override
    protected void setScheduler(ScheduledExecutorService scheduler) {
        super.setScheduler(scheduler);
    }

    @Override
    protected void unsetScheduler(ScheduledExecutorService scheduler) {
        super.unsetScheduler(scheduler);
    }

    @Override
    protected void setLocation(WsLocationAdmin locRef) {
        super.setLocation(locRef);
    }

    @Override
    protected void unsetLocation(WsLocationAdmin locRef) {
        super.unsetLocation(locRef);
    }

    @Override
    protected void setMonitor(ServiceReference<FileMonitor> monitorRef) {
        super.setMonitor(monitorRef);
    }

    @Override
    protected void unsetMonitor(ServiceReference<FileMonitor> monitorRef) {
        super.unsetMonitor(monitorRef);
    }

    @Override
    protected void updatedMonitor(ServiceReference<FileMonitor> monitorRef) {
        super.updatedMonitor(monitorRef);
    }
}
