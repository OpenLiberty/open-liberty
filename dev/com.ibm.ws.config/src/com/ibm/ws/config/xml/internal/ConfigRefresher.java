/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package com.ibm.ws.config.xml.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.config.ConfigUpdateException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.ConfigComparator.ComparatorResult;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
import com.ibm.ws.config.xml.internal.variables.ConfigVariableRegistry;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

import io.openliberty.checkpoint.spi.CheckpointHook;

/**
 *
 */
public class ConfigRefresher {

    static final TraceComponent tc = Tr.register(ConfigRefresher.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    private final ChangeHandler changeHandler;
    private final ServerXMLConfiguration serverXMLConfig;
    private final ConfigurationMonitor configurationMonitor;

    /** service trackers */
    private final ServiceTracker<RuntimeUpdateManager, RuntimeUpdateManager> runtimeUpdateManagerTracker;
    private final ServiceTracker<Executor, Executor> executorTracker;
    private final ServiceTracker<MetaTypeRegistry, MetaTypeRegistry> metatypeTracker;

    private long configStartTime = 0;
    private Collection<Future<?>> futuresForChanges = null;

    private final ChangesEndedHook changesEndedHook;
    private final ServiceRegistration<CheckpointHook> checkpointHookRegistration;

    ConfigRefresher(BundleContext bundleContext,
                    ChangeHandler changeHandler, ServerXMLConfiguration serverXMLConfig, ConfigVariableRegistry variableRegistry) {
        this.changeHandler = changeHandler;
        this.serverXMLConfig = serverXMLConfig;

        this.configurationMonitor = new ConfigurationMonitor(bundleContext, serverXMLConfig, this, variableRegistry);

        runtimeUpdateManagerTracker = new ServiceTracker<RuntimeUpdateManager, RuntimeUpdateManager>(bundleContext, RuntimeUpdateManager.class.getName(), null);
        runtimeUpdateManagerTracker.open();

        executorTracker = new ServiceTracker<Executor, Executor>(bundleContext, java.util.concurrent.ExecutorService.class.getName(), null);
        executorTracker.open();

        metatypeTracker = new ServiceTracker<MetaTypeRegistry, MetaTypeRegistry>(bundleContext, MetaTypeRegistry.class.getName(), null);
        metatypeTracker.open();

        changesEndedHook = new ChangesEndedHook();
        Hashtable<String, Object> hookProps = new Hashtable<>();
        // Lesser ranking ensures changesEnded() executes ASAP after the SystemConfiguration
        // single-threaded restore hook
        hookProps.put(Constants.SERVICE_RANKING, Integer.MIN_VALUE + 10000);
        hookProps.put(CheckpointHook.MULTI_THREADED_HOOK, Boolean.TRUE);
        checkpointHookRegistration = bundleContext.registerService(CheckpointHook.class, changesEndedHook, hookProps);
    }

    void start() {
        configurationMonitor.registerService();
    }

    void stop() {
        configurationMonitor.stopConfigurationMonitoring();
        runtimeUpdateManagerTracker.close();

        if (checkpointHookRegistration != null) {
            checkpointHookRegistration.unregister();
        }
    }

    public void refreshConfiguration() {
        doRefresh(null);
    }

    private synchronized void doRefresh(Map<String, DeltaType> variableDelta) {
        if (FrameworkState.isStopping()) {
            // if the framework is stopping, just ignore incoming events
            return;
        }

        configStartTime = System.nanoTime();

        RuntimeUpdateManager runtimeUpdateManager = runtimeUpdateManagerTracker.getService();
        RuntimeUpdateNotification configUpdatesDelivered = runtimeUpdateManager.createNotification(RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED);
        futuresForChanges = null;

        try {
            Tr.audit(tc, "info.config.refresh.start");

            ServerConfiguration newConfiguration = serverXMLConfig.loadNewConfiguration();
            if (newConfiguration == null) {
                return;
            }

            ComparatorResult result = compareConfigurations(serverXMLConfig.getConfiguration(), newConfiguration, variableDelta);

            // Error condition -- A result with no changes will have result.hasDelta() == false
            if (result == null) {
                return;
            }

            Collection<ConfigurationInfo> configurations = null;

            if (!result.hasDelta()) {
                Tr.audit(tc, "info.config.refresh.nochanges");
            } else {
                // switch to new configuration & process changes
                try {
                    configurations = changeHandler.switchConfiguration(serverXMLConfig, result);
                } catch (ConfigUpdateException e) {
                    Tr.error(tc, "error.config.update.init", new Object[] { e.getMessage() });
                }
            }

            // update the file monitoring service
            configurationMonitor.updateFileMonitor(serverXMLConfig.getFilesToMonitor());
            configurationMonitor.updateDirectoryMonitor(serverXMLConfig.getDirectoriesToMonitor());

            if (configurations != null) {
                futuresForChanges = fireConfigurationChanges(configurations);
            }
        } catch (Exception e) {
            // Let the notification show that we got an error while making the configuration changes
            configUpdatesDelivered.setResult(e);
        } finally {
            if (!changesEndedHook.queueNotification(configUpdatesDelivered)) {
                changesEnded(configUpdatesDelivered);
            }
        }
    }

    private ComparatorResult compareConfigurations(ServerConfiguration serverConfiguration, ServerConfiguration newConfiguration, Map<String, DeltaType> variableDelta) {
        ConfigComparator comparator = new ConfigComparator(serverConfiguration, newConfiguration, metatypeTracker.getService(), variableDelta);

        ComparatorResult result;
        try {
            result = comparator.computeDelta();
        } catch (ConfigUpdateException e1) {
            Tr.error(tc, "error.config.update.init", new Object[] { e1.getMessage() });
            return null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "doRefreshConfiguration(): Configuration changes: " + result);
        }

        result.setNewConfiguration(newConfiguration);
        return result;
    }

    private Collection<Future<?>> fireConfigurationChanges(Collection<ConfigurationInfo> configurations) {
        /*
         * This avoids waiting on futures under BundleProcessor lock.
         * Otherwise, if executed under BundleProcessor lock it might cause
         * a deadlock.
         */
        Collection<Future<?>> futures = new ArrayList<Future<?>>();
        for (ConfigurationInfo info : configurations) {
            // create futures for configuration update events
            info.fireEvents(futures);
        }
        return futures;
    }

    void changesEnded(final RuntimeUpdateNotification configUpdatesDelivered) {
        final long endingConfigStartTime = configStartTime;
        final Collection<Future<?>> endingFuturesForChanges = futuresForChanges;

        configStartTime = 0;
        futuresForChanges = null;

        // Use an executor thread to end the config changes
        Executor executor = executorTracker.getService();
        if (executor != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    endConfigChanges(configUpdatesDelivered, endingConfigStartTime, endingFuturesForChanges);
                }
            });
        } else {
            endConfigChanges(configUpdatesDelivered, endingConfigStartTime, endingFuturesForChanges);
        }
    }

    private void endConfigChanges(RuntimeUpdateNotification configUpdatesDelivered, long endingConfigStartTime, Collection<Future<?>> endingFuturesForChanges) {
        final boolean noTimeout;
        if (endingFuturesForChanges != null) {
            // we made config updates which we have futures for.  wait for those futures to complete
            noTimeout = waitForAll(endingFuturesForChanges, 1, TimeUnit.MINUTES);
        } else {
            noTimeout = true;
        }

        configUpdatesDelivered.setResult(true);
        RuntimeUpdateManager runtimeUpdateManager = runtimeUpdateManagerTracker.getService();

        if (runtimeUpdateManager == null) {
            // If the service isn't available it's probably because the server is stopping. Just return here to avoid an NPE.
            if (FrameworkState.isStopping()) {
                return;
            } else {
                // This should never happen
                throw new IllegalStateException("The RuntimeUpdateManager service could not be obtained");
            }
        }

        RuntimeUpdateNotification featureUpdatesCompleted = runtimeUpdateManager.getNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
        if (featureUpdatesCompleted != null) {
            featureUpdatesCompleted.waitForCompletion();
        }
        RuntimeUpdateNotification appsStarting = runtimeUpdateManager.getNotification(RuntimeUpdateNotification.APPLICATIONS_STARTING);
        if (appsStarting != null) {
            appsStarting.waitForCompletion();
        }

        if (endingFuturesForChanges != null) {
            if (noTimeout) {
                Tr.audit(tc, "info.config.refresh.stop", TimestampUtils.getElapsedTimeNanos(endingConfigStartTime));
            } else {
                Tr.warning(tc, "info.config.refresh.timeout");
            }
        }
    }

    @FFDCIgnore({ InterruptedException.class, ExecutionException.class, TimeoutException.class })
    private boolean waitForAll(Collection<Future<?>> futureList, long timeout, TimeUnit timeUnit) {
        long timeoutNanos = timeUnit.toNanos(timeout);
        for (Future<?> future : futureList) {
            if (future == null || future.isDone()) {
                continue;
            }
            if (timeoutNanos <= 0) {
                return false;
            }
            long startTime = System.nanoTime();
            try {
                future.get(timeoutNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                return false;
            } catch (TimeoutException e) {
                return false;
            }
            long endTime = System.nanoTime();
            timeoutNanos -= (endTime - startTime);
        }
        return true;
    }

    // Entry point for refreshing configuration because of changes in variables
    public void variableRefresh(Map<String, DeltaType> deltaMap) {
        doRefresh(deltaMap);

    }

    // Method changesEnded() performs a blocking operation. Defer the execution
    // of changesEnded() until the JVM enters multi-threaded mode during checkpoint
    // restore.
    private class ChangesEndedHook implements CheckpointHook {

        private final ThreadLocal<Boolean> checkpointThread = new ThreadLocal<Boolean>() {
            @Override
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        // FIFO queue of deferred config update notifications
        final Deque<RuntimeUpdateNotification> configUpdatesToDeliver = new ArrayDeque<RuntimeUpdateNotification>();

        boolean queueNotification(RuntimeUpdateNotification notification) {
            if (!checkpointThread.get()) {
                return false;
            }
            configUpdatesToDeliver.add(notification);
            return true;
        }

        @Override
        public void prepare() {
            checkpointThread.set(true);
        }

        @Override
        public void restore() {
            checkpointThread.set(false);
            RuntimeUpdateNotification notification = null;
            while ((notification = configUpdatesToDeliver.pollFirst()) != null) {
                changesEnded(notification);
            }
        }
    }

}
