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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.condition.Condition;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Activator to track running condition (io.openliberty.process.running) and listen configuration update events. Re-register the
 * jcache.cachemanager.config condition service when both the running condition is true and the configuration is updated.
 *
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<Condition, Boolean>, ConfigurationListener, RuntimeUpdateListener {
    public static final String CACHE_MANAGER_CONFIG_CONDITION = "jcache.cachemanager.config";
    private static final Set<String> pids = new HashSet<>(Arrays.asList("io.openliberty.jcache.cachemanager", "io.openliberty.jcache.cachingprovider"));
    private final AtomicReference<ServiceRegistration<Condition>> conditionReg = new AtomicReference<>();
    volatile ServiceTracker<Condition, Boolean> runningConditionTracker;
    volatile BundleContext bc;

    @Override
    public void start(BundleContext context) throws Exception {
        bc = context;
        runningConditionTracker = new ServiceTracker<>(bc, Condition.class, this);
        runningConditionTracker.open();
        refreshConfigCondition();
        bc.registerService(
                           new String[] { ConfigurationListener.class.getName(), RuntimeUpdateListener.class.getName() },
                           this, null);
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
        if (pids.contains(event.getFactoryPid())
            && (event.getType() == ConfigurationEvent.CM_UPDATED || event.getType() == ConfigurationEvent.CM_DELETED)) {
            // using == Boolean.TRUE here because getService may return null
            if (runningConditionTracker.getService() == Boolean.TRUE) {
                doRefreshConfigCondition.set(true);
            }
        }
    }

    AtomicBoolean doRefreshConfigCondition = new AtomicBoolean(false);

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {

                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    checkForRefresh();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    checkForRefresh();
                }

                private void checkForRefresh() {
                    if (doRefreshConfigCondition.compareAndSet(true, false)) {
                        refreshConfigCondition();
                    }
                }

            });
        }
    }

    void refreshConfigCondition() {
        conditionReg.getAndUpdate((reg) -> {
            if (reg != null) {
                reg.unregister();
            }
            return bc.registerService(Condition.class, Condition.INSTANCE,
                                      FrameworkUtil.asDictionary(Collections.singletonMap(Condition.CONDITION_ID, CACHE_MANAGER_CONFIG_CONDITION)));
        });
    }
}
