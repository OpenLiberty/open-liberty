/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.jcache.internal;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.condition.Condition;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Activator to track running condition (io.openliberty.process.running) and listen configuration update events. Re-register the
 * jcache.cachemanager.config condition service when both the running condition is true and the configuration is updated.
 *
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<Condition, Boolean>, ConfigurationListener {
    public static final String CACHE_MANAGER_CONFIG_CONDITION = "jcache.cachemanager.config";
    private final AtomicReference<ServiceRegistration<Condition>> conditionReg = new AtomicReference<>();
    volatile ServiceTracker<Condition, Boolean> runningConditionTracker;
    volatile BundleContext bc;

    @Override
    public void start(BundleContext context) throws Exception {
        bc = context;
        runningConditionTracker = new ServiceTracker<>(bc, Condition.class, this);
        runningConditionTracker.open();
        refreshConfigCondition();
        bc.registerService(ConfigurationListener.class, this, FrameworkUtil.asDictionary(Collections.singletonMap(Constants.SERVICE_RANKING, Integer.MIN_VALUE)));
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        runningConditionTracker.close();
    }

    @Override
    public Boolean addingService(ServiceReference<Condition> ref) {
        if (CheckpointPhase.CONDITION_PROCESS_RUNNING_ID.equals(ref.getProperty(Condition.CONDITION_ID))) {
            return Boolean.TRUE;
        }
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<Condition> ref, Boolean present) {
        // ignore
    }

    @Override
    public void removedService(ServiceReference<Condition> ref, Boolean present) {
        // ignore
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if ("io.openliberty.jcache.cachemanager".equals(event.getFactoryPid())
            && (event.getType() == ConfigurationEvent.CM_UPDATED || event.getType() == ConfigurationEvent.CM_DELETED)) {
            // using == Boolean.TRUE here because getService may return null
            if (runningConditionTracker.getService() == Boolean.TRUE) {
                refreshConfigCondition();
            }
        }
    }

    private void refreshConfigCondition() {
        conditionReg.getAndUpdate((reg) -> {
            if (reg != null) {
                reg.unregister();
            }
            return bc.registerService(Condition.class, Condition.INSTANCE,
                                      FrameworkUtil.asDictionary(Collections.singletonMap(Condition.CONDITION_ID, CACHE_MANAGER_CONFIG_CONDITION)));
        });
    }
}
