/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.microprofile.reactive.messaging.kafka;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

/**
 * An OSGi component that listens to application lifecycle events and tracks
 * each application's started state using a {@link CountDownLatch}.
 * <p>
 * On {@code applicationStarting} a latch with count 1 is placed in the map.
 * On {@code applicationStarted} the latch is counted down to zero, unblocking
 * any thread waiting on it. On {@code applicationStopped} the entry is removed
 * from the map.
 *
 * This class tolerates gets being called before application starting
 */
@Component(service = { KafkaApplicationStateListener.class,
                       ApplicationStateListener.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class KafkaApplicationStateListener implements ApplicationStateListener {

    private final ConcurrentHashMap<String, CountDownLatch> appLatches = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        appLatches.computeIfAbsent(appInfo.getDeploymentName(), key -> new CountDownLatch(1)); //Tolerate gets being called before application starting.
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {

        CountDownLatch latch = appLatches.get(appInfo.getDeploymentName());
        if (latch != null) {
            latch.countDown();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        // Nothing to do on stopping
    }

    /** {@inheritDoc} */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        appLatches.remove(appInfo.getDeploymentName());
    }

    /**
     * Returns the {@link CountDownLatch} associated with the named application
     *
     * @param appName the application name
     * @return the latch
     */
    public CountDownLatch getLatch(String appName) {
        return appLatches.computeIfAbsent(appName, key -> new CountDownLatch(1)); //Tolerate gets being called before application starting.
    }
}
