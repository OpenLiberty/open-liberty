/*******************************************************************************
 * Copyright (c) 2016, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * Logstash collector ASL determines how many applications have been started
 * and handles server shutdown coordination
 */

@Component(name = LogstashCollectorASL.COMPONENT_NAME,
           service = { ApplicationStateListener.class, ServerQuiesceListener.class },
           configurationPolicy = ConfigurationPolicy.OPTIONAL,
           immediate = true, property = { "service.vendor=IBM" })
public class LogstashCollectorASL implements ApplicationStateListener, ServerQuiesceListener {

    public static final String COMPONENT_NAME = "com.ibm.ws.logstash.collector.internal.LogstashCollectorASL";
    private final static AtomicInteger runningApplicationCount = new AtomicInteger(0);

    public static int getRunningApplicationCount() {
        return runningApplicationCount.get();
    }

    // ApplicationStateListener methods
    @Override
    public void applicationStarting(ApplicationInfo appInfo) {
        // Not needed
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) {
        runningApplicationCount.incrementAndGet();
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // Not needed
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        runningApplicationCount.decrementAndGet();
    }

    @Override
    public void serverStopping() {
        // Only proceed if applications are running
        if (LogstashCollectorASL.getRunningApplicationCount() == 0) {
            return;
        }

        //Setting shutdown flag in order for TaskImpl to begin parsing for CWWKT0017I
        //LogstashCollectorASL.serverStopping() is called before LogstashColector.java and sets the shutdown flag
        com.ibm.ws.logging.collector.ShutdownSignal.requestShutdown();
    }

}
