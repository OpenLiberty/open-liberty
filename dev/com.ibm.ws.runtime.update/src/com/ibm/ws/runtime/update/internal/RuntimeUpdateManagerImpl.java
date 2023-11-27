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
package com.ibm.ws.runtime.update.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.ws.kernel.launch.service.ForcedServerStop;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.ThreadQuiesce;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 *
 */
@Component(service = { RuntimeUpdateManager.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class RuntimeUpdateManagerImpl implements RuntimeUpdateManager, SynchronousBundleListener {
    private static final TraceComponent tc = Tr.register(RuntimeUpdateManagerImpl.class);
    public static final ResourceBundle messages = ResourceBundle.getBundle("com.ibm.ws.runtime.update.internal.resources.Messages");

    private volatile FutureMonitor futureMonitor;
    private final AtomicBoolean normalServerStop = new AtomicBoolean(true);

    private final Set<RuntimeUpdateListener> updateListeners = new HashSet<RuntimeUpdateListener>();

    private final Map<String, RuntimeUpdateNotification> notifications = new HashMap<String, RuntimeUpdateNotification>();

    private BundleContext bundleCtx;

    private final CompletionListener<Boolean> cleanupListener = new CompletionListener<Boolean>() {
        @Override
        public void successfulCompletion(Future<Boolean> future, Boolean result) {
            cleanupNotifications();
        }

        @Override
        public void failedCompletion(Future<Boolean> future, Throwable t) {
            cleanupNotifications();
        }
    };

    private WsLocationAdmin locationService;

    private LibertyProcess libertyProcess;

    private ExecutorService executorService;

    @Activate
    protected void activate(BundleContext ctx) {
        bundleCtx = ctx;
        bundleCtx.addBundleListener(this);
    }

    @Reference(service = ExecutorService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               target = "(component.name=com.ibm.ws.threading)")
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Reference(service = FutureMonitor.class)
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = null;
    }

    @Reference(service = RuntimeUpdateListener.class,
               cardinality = ReferenceCardinality.MULTIPLE,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRuntimeUpdateListener(RuntimeUpdateListener updateListener) {
        Collection<RuntimeUpdateNotification> snapshot;
        synchronized (notifications) {
            // get a snapshot of notifications while holding the notifications lock
            // so we can get the existing notifications before we actually add the listener
            snapshot = new ArrayList<RuntimeUpdateNotification>(notifications.values());
            // also add the listener the listener in the sync block
            // so it can be informed of newly created notifications that may be getting blocked
            // while we are in this sync block
            this.updateListeners.add(updateListener);
        }
        // finally send the notification for the snapshot of existing notifications
        for (RuntimeUpdateNotification existing : snapshot) {
            updateListener.notificationCreated(this, existing);
        }
    }

    protected void unsetRuntimeUpdateListener(RuntimeUpdateListener updateListener) {
        synchronized (notifications) {
            this.updateListeners.remove(updateListener);
        }
    }

    @Reference(service = WsLocationAdmin.class)
    protected void setLocationAdmin(WsLocationAdmin admin) {
        this.locationService = admin;
    }

    protected void unsetLocationAdmin(WsLocationAdmin admin) {
        this.locationService = null;
    }

    @Reference(policy = ReferencePolicy.STATIC)
    protected void setProcess(LibertyProcess process) {
        this.libertyProcess = process;
    }

    protected void cleanupNotifications() {
        synchronized (notifications) {
            // Check that all notifications are completed
            for (RuntimeUpdateNotification notification : notifications.values()) {
                if (!notification.isDone()) {
                    // This notification isn't completed, and so we will not clear
                    // the set of notifications yet since someone could still ask
                    // for one of the previously completed notifications in their
                    // completion listener
                    return;
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "cleanupNotifications: notifications cleared");
            }
            notifications.clear();
        }
    }

    @Trivial
    private static class NotificationImpl implements RuntimeUpdateNotification {
        private final String name;
        private final Future<Boolean> future;
        private final FutureMonitor futureMonitor;
        private final AtomicBoolean waitForPendingNotifications;
        private final boolean ignoreOnQuiesce;
        private Map<String, Object> properties;

        NotificationImpl(String name, Future<Boolean> future, FutureMonitor futureMonitor, AtomicBoolean waitForPendingNotifications, boolean ignoreQuiesce) {
            this.name = name;
            this.future = future;
            this.futureMonitor = futureMonitor;
            this.waitForPendingNotifications = waitForPendingNotifications;
            this.ignoreOnQuiesce = ignoreQuiesce;
        }

        @Override
        public String toString() {
            return name + "[" + future + "]";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Future<Boolean> getFuture() {
            return future;
        }

        @Override
        public void setResult(boolean result) {
            futureMonitor.setResult(future, result);
        }

        @Override
        public void setResult(Throwable t) {
            futureMonitor.setResult(future, t);
        }

        @Override
        public void onCompletion(CompletionListener<Boolean> completionListener) {
            futureMonitor.onCompletion(future, completionListener);
        }

        @Override
        public void waitForCompletion() {
            final CountDownLatch latch = new CountDownLatch(1);
            onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    latch.countDown();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    latch.countDown();
                }
            });

            // As long as we're waiting for things to finish, continue to wait!
            // We will stop wanting to wait for things to finish if the --force option is used
            // with the stop command
            while (waitForPendingNotifications.get()) {
                try {
                    if (latch.await(1, TimeUnit.SECONDS)) {
                        return;
                    }
                } catch (InterruptedException e) {
                    e.getCause();
                }
            }
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public boolean ignoreOnQuiesce() {
            return ignoreOnQuiesce;
        }

        @Override
        public Map<String, Object> getProperties() {
            return this.properties;
        }

        @Override
        public void setProperties(Map<String, Object> props) {
            this.properties = Collections.unmodifiableMap(props);
        }
    }

    @Override
    public RuntimeUpdateNotification createNotification(String name) {
        return createNotification(name, false);
    }

    @Override
    public RuntimeUpdateNotification getNotification(String name) {
        if (FrameworkState.isStopping()) {
            // we are in the process of stopping, so just ignore any updates at this point
            return null;
        }

        synchronized (notifications) {
            return notifications.get(name);
        }
    }

    /**
     * Synchronous Bundle Listener:
     * Listens for the system bundle to enter STOPPING state..
     *
     * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        // We *only* react to the STOPPING event for the system bundle (bundle 0).
        if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STOPPING) {
            ServiceReference<ForcedServerStop> forcedStop = bundleCtx.getServiceReference(ForcedServerStop.class);

            if (forcedStop != null) {
                // ABRUPT STOP (no quiesce stage)
                // This will make NotificationImpl#waitForCompletion stop looping
                normalServerStop.set(false);
            } else {
                // NICE / NORMAL STOP
                // Find all ServerQueisceListeners and notify them
                try {
                    quiesceListeners(bundleCtx.getServiceReferences(ServerQuiesceListener.class, null));
                } catch (InvalidSyntaxException e) {
                    // not going to happen with a null filter.
                }
            }
        }
    }

    public static boolean isBeta = Boolean.valueOf(System.getProperty("com.ibm.ws.beta.edition"));

    /**
     * Gets the amount of time to wait for quiesce work to complete before continuing with shutdown.
     *
     * @return
     */
    private int getQuiesceTimeout() {

        final int MINIMUM_QUIESCE_TIMEOUT = 30;
        int quiesceTimeout = MINIMUM_QUIESCE_TIMEOUT;

        if (isBeta) {
            // The quiesceTimeout is configured on the <applicationManager> element in server.xml.  RuntimeUpdateManagerImpl
            // only needs the ApplicationManager to lookup this one value.  The approach we are using to reference the service is
            // rarely appropriate, but is OK here since we only need to lookup the value at a single point in time (i.e. server shutdown).
            ServiceReference<?> appMgr = bundleCtx.getServiceReference("com.ibm.ws.app.manager.ApplicationManager");
            Long valueFromConfig = null;
            if (appMgr != null) {
                valueFromConfig = ((Long) appMgr.getProperty("quiesceTimeout"));
            }

            // The timeout is in seconds.  So conversion to int is fine.
            if (valueFromConfig != null) {
                quiesceTimeout = valueFromConfig.intValue();
            } else {
                Tr.warning(tc, MessageFormat.format(messages.getString("quiesce.timeout.not.available"), quiesceTimeout, MINIMUM_QUIESCE_TIMEOUT));
            }
        }

        if (quiesceTimeout < MINIMUM_QUIESCE_TIMEOUT) {
            Tr.warning(tc, MessageFormat.format(messages.getString("quiesce.timeout.too.low"), quiesceTimeout, MINIMUM_QUIESCE_TIMEOUT));
            quiesceTimeout = MINIMUM_QUIESCE_TIMEOUT;
        }
        return quiesceTimeout;
    }

    /**
     * Call the {@code ServerQuiesceListener#serverStopping()} method on all discovered
     * references. Calls are queued to a small thread pool, which is then stopped.
     * All invocations are then allowed to complete.
     *
     * @param listenerRefs Collection of {@code ServiceReference}s for {@code ServerQuiesceListener}s
     */

    private void quiesceListeners(Collection<ServiceReference<ServerQuiesceListener>> listenerRefs) {
        // Make a copy of existing notifications: we can't hold the lock around notifications
        // to iterate while waiting for the existing notifications to complete because that would
        // lock-out cleanupNotifications.
        final Map<String, RuntimeUpdateNotification> existingNotifications = new HashMap<String, RuntimeUpdateNotification>();
        synchronized (notifications) {
            existingNotifications.putAll(notifications);
        }

        if (listenerRefs.isEmpty() && existingNotifications.isEmpty())
            return;

        int quiesceTimeout = getQuiesceTimeout();

        if (isServer())
            Tr.audit(tc, "quiesce.begin", quiesceTimeout);
        else
            Tr.audit(tc, "client.quiesce.begin", quiesceTimeout);

        // If there are RuntimeUpdateNotifications outstanding, submit a thread to wait on them
        if (!existingNotifications.isEmpty()) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (RuntimeUpdateNotification notification : existingNotifications.values()) {
                            if (!notification.ignoreOnQuiesce())
                                notification.waitForCompletion();
                        }
                    } catch (Throwable t) {
                        // Auto-FFDC here..
                    }
                }
            });
        }

        FutureCollection quiesceListenerFutures = new FutureCollection();

        // Queue the notification of each listener (unbounded queue)
        final ConcurrentLinkedQueue<ServerQuiesceListener> listeners = new ConcurrentLinkedQueue<ServerQuiesceListener>();
        for (ServiceReference<ServerQuiesceListener> ref : listenerRefs) {
            final ServerQuiesceListener listener = bundleCtx.getService(ref);
            if (listener != null) {

                quiesceListenerFutures.add(executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listeners.add(listener);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Invoking serverStopping() on listener: " + listener);
                            }
                            listener.serverStopping();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "serverStopping() method completed on listener: " + listener);
                            }

                        } catch (Throwable t) {
                            // Auto-FFDC here..
                        } finally {
                            listeners.remove(listener);
                        }
                    }
                }));

            }
        }

        if (tc.isDebugEnabled())
            Tr.debug(tc, "About to begin quiesce of executor service threads.");

        // Notify the executor service that we are quiescing

        ThreadQuiesce tq = (ThreadQuiesce) executorService;
        long startTime = System.currentTimeMillis();
        if (tq.quiesceThreads(quiesceTimeout) && quiesceListenerFutures.isComplete(startTime, quiesceTimeout)) {
            if (isServer())
                Tr.info(tc, "quiesce.end");
            else
                Tr.info(tc, "client.quiesce.end");

            return;
        } else {

            if (tc.isDebugEnabled()) {
                // If debug is enabled we will dump threads so that we can determine what was still running
                libertyProcess.createJavaDump(Collections.singleton("thread"));
            }

            int count = tq.getActiveThreads();

            // we timed out - we now have to stop waiting for existing notifications to finish...
            // we set the normal stop to false and issue the timeout warning after the diagnostics to
            // ensure that we have valid diagnostics (to avoid situations where the long running threads
            // finish right after we issue the warning - then we wouldn't see them in the diagnostics).
            normalServerStop.set(false);
            Tr.warning(tc, "quiece.warning");

            int notificationCount = 0;

            for (RuntimeUpdateNotification notification : existingNotifications.values()) {
                if (!!!notification.isDone()) {
                    notificationCount++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Notification did not complete during quiesce: ", notification.getName());
                    }
                }
            }
            if (notificationCount > 0) {
                Tr.warning(tc, "notifications.not.complete", notificationCount);
            }

            if (listeners.size() > 0) {
                Tr.warning(tc, "quiesce.listeners.not.complete", listeners.size());
            }

            if (tc.isDebugEnabled()) {
                for (ServerQuiesceListener sql : listeners) {
                    Tr.debug(tc, "Quiesce listener did not complete during quiesce: ", sql.getClass().getName());
                }
            }

            count = count - notificationCount - listeners.size();
            if (count > 0) {
                Tr.warning(tc, "quiesce.waiting.on.threads", count);
            }
        }

    }

    private class FutureCollection {
        List<Future<?>> quiesceListenerFutures = new ArrayList<Future<?>>();

        FutureCollection() {

        }

        void add(Future<?> f) {
            quiesceListenerFutures.add(f);
        }

        /**
         *
         * @param startTime      - time now in milliseconds
         * @param quiesceTimeout - timeout in seconds
         * @return
         */
        @FFDCIgnore(TimeoutException.class)
        boolean isComplete(long startTime, int quiesceTimeout) {
            // We will wait quiesceTimeout seconds past the start time for tasks to complete
            // Configured in the <executor> element of server.xml.  Default 30 seconds.
            long endTime = startTime + quiesceTimeout * 1000;

            for (Future<?> f : quiesceListenerFutures) {
                long remainingTime = endTime - System.currentTimeMillis();
                if (remainingTime < 0)
                    return false;
                try {
                    f.get(remainingTime, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean isServer() {
        return locationService.resolveString(WsLocationConstants.SYMBOL_PROCESS_TYPE).equals(WsLocationConstants.LOC_PROCESS_TYPE_SERVER);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.runtime.update.RuntimeUpdateManager#createNotification(java.lang.String, boolean)
     */
    @Override
    public RuntimeUpdateNotification createNotification(String name, boolean ignoreOnQuiesce) {

        if (FrameworkState.isStopping()) {
            // we are in the process of stopping, so just ignore any updates at this point
            return null;
        }

        final Future<Boolean> future = futureMonitor.createFuture(Boolean.class);
        RuntimeUpdateNotification notification = new NotificationImpl(name, future, futureMonitor, normalServerStop, ignoreOnQuiesce);

        Collection<RuntimeUpdateListener> listenerSnapshot;
        synchronized (notifications) {
            notifications.put(name, notification);
            // get a snapshot of listeners while holding the lock to avoid sending the same notification twice
            // to the same listener that happens to be getting registered at the same time as the notification is getting created.
            listenerSnapshot = new ArrayList<RuntimeUpdateListener>(updateListeners);
        }

        for (RuntimeUpdateListener updateListener : listenerSnapshot) {
            updateListener.notificationCreated(this, notification);
        }

        notification.onCompletion(cleanupListener);
        return notification;
    }

}
