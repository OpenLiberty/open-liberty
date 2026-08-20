/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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

    static final String QUIESCE_PHASE_PROPERTY = "quiesce.phase";

    enum QuiescePhase {
        PRE,
        DEFAULT
    }

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
               cardinality = ReferenceCardinality.MANDATORY)
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
                // Find all PRE quiesce listeners and notify them first
                try {
                    quiesceListeners();
                } catch (InvalidSyntaxException e) {
                    // not going to happen with a null filter.
                }
            }
        }
    }

    private boolean callQuiesceListeners(long startTime, int quiesceTimeout, final ConcurrentLinkedQueue<Object> invoking,
                                         Collection<ServiceReference<ServerQuiesceListener>> listenerRefs, ThreadQuiesce tq) {
        FutureCollection quiesceListenerFutures = new FutureCollection();
        // Queue the notification of each hook (unbounded queue)
        for (ServiceReference<ServerQuiesceListener> ref : listenerRefs) {
            final ServerQuiesceListener listener = bundleCtx.getService(ref);
            if (listener != null) {
                quiesceListenerFutures.add(executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            invoking.add(listener);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Invoking serverStopping() on listener: " + ref);
                            }
                            listener.serverStopping();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "serverStopping() method completed on listener: " + ref);
                            }

                        } catch (Throwable t) {
                            // Auto-FFDC here..
                        } finally {
                            invoking.remove(listener);
                        }
                    }
                }));
            }
        }
        // Notify the executor service that we are quiescing, if available
        boolean quiesceListenerSuccess = quiesceListenerFutures.isComplete(startTime, quiesceTimeout);
        return quiesceListenerSuccess && (tq != null ? tq.quiesceThreads(startTime) : true);
    }

    /**
     * Call the {@code ServerQuiesceListener#serverStopping()} method on all discovered
     * references. Calls are queued to a small thread pool, which is then stopped.
     * All invocations are then allowed to complete.
     *
     * @param listenerRefs Collection of {@code ServiceReference}s for {@code ServerQuiesceListener}s
     * @throws InvalidSyntaxException
     */
    private void quiesceListeners() throws InvalidSyntaxException {
        // Make a copy of existing notifications: we can't hold the lock around notifications
        // to iterate while waiting for the existing notifications to complete because that would
        // lock-out cleanupNotifications.
        final Map<String, RuntimeUpdateNotification> existingNotifications = new HashMap<String, RuntimeUpdateNotification>();
        synchronized (notifications) {
            existingNotifications.putAll(notifications);
        }

        Collection<ServiceReference<ServerQuiesceListener>> preListenerRefs = bundleCtx.getServiceReferences(ServerQuiesceListener.class,
                                                                                                             "(" + QUIESCE_PHASE_PROPERTY + "=" + QuiescePhase.PRE + ")");
        // NOTE this filter will need to change if another phase is defined for quiesce
        Collection<ServiceReference<ServerQuiesceListener>> defaultListenerRefs = bundleCtx.getServiceReferences(ServerQuiesceListener.class,
                                                                                                                 "(!(" + QUIESCE_PHASE_PROPERTY + "=" + QuiescePhase.PRE + "))");
        if (preListenerRefs.isEmpty() && defaultListenerRefs.isEmpty() && existingNotifications.isEmpty())
            return;

        ThreadQuiesce tq = (ThreadQuiesce) executorService;
        int quiesceTimeout = tq.getQuiesceTimeout();

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

        if (tc.isDebugEnabled())
            Tr.debug(tc, "About to begin quiesce of executor service threads.");

        final ConcurrentLinkedQueue<Object> invoking = new ConcurrentLinkedQueue<>();
        long startTime = System.currentTimeMillis();
        // now call the listeners
        boolean preListenerSuccess = callQuiesceListeners(startTime, quiesceTimeout, invoking, preListenerRefs, null);
        long currentTime = System.currentTimeMillis();
        long preQuiesceTime = (currentTime - startTime) / 1000;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Done calling pre-quiesce listeners, time taken: " + preQuiesceTime);
        }

        // check if pre listener time took more than half the configured timeout
        if (preQuiesceTime > (quiesceTimeout / 2)) {
            // Pre listeners took more than half the timeout;
            // Resetting the startTime and setting the timeout to give normal listeners
            // half the configured timeout.
            startTime = currentTime;
            quiesceTimeout = quiesceTimeout / 2;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Extending timeout for default listeners after pre listeners timed out: " + quiesceTimeout);
            }
        }
        boolean defaultListenerSuccess = callQuiesceListeners(startTime, quiesceTimeout, invoking, defaultListenerRefs, tq);

        if (preListenerSuccess && defaultListenerSuccess) {
            if (isServer())
                Tr.info(tc, "quiesce.end");
            else
                Tr.info(tc, "client.quiesce.end");

            return;
        } else {
            int count = tq.getActiveThreads();

            // we timed out - we now have to stop waiting for existing notifications to finish...
            // we set the normal stop to false and issue the timeout warning after the diagnostics to
            // ensure that we have valid diagnostics (to avoid situations where the long running threads
            // finish right after we issue the warning - then we wouldn't see them in the diagnostics).
            normalServerStop.set(false);
            Tr.warning(tc, "quiece.warning");

            int notificationCount = 0;

            List<String> incompleteNotifications = new ArrayList<String>(0);
            for (RuntimeUpdateNotification notification : existingNotifications.values()) {
                if (!!!notification.isDone()) {
                    notificationCount++;
                    incompleteNotifications.add(notification.getName());
                }
            }
            if (notificationCount > 0) {
                Tr.warning(tc, "notifications.not.complete", notificationCount, String.join(", ", incompleteNotifications));
            }

            List<String> invokingObjects = Collections.emptyList();
            if (invoking.size() > 0) {
                // snapshot invoking classes to avoid timing issues
                invokingObjects = invoking.stream().map(Object::toString).collect(Collectors.toList());
                if (invokingObjects.size() > 0) {
                    Tr.warning(tc, "quiesce.listeners.not.complete", invokingObjects.size(),
                               String.join(", ", invokingObjects));
                }
            }

            count = count - notificationCount - invokingObjects.size();
            if (count > 0) {
                Tr.warning(tc, "quiesce.waiting.on.threads", count);
            }

            // Moved the core to the end to ensure the above messages are accurate.
            // Java cores can take a long time on slow systems; if this was done first then some
            // of the notifications and listeners may have completed before we could gather them
            // for the messages.
            if (tc.isDebugEnabled()) {
                // If debug is enabled we will dump threads so that we can determine what was still running
                libertyProcess.createJavaDump(Collections.singleton("thread"));
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
                long waitTime = endTime - System.currentTimeMillis();
                if (waitTime < 0)
                    return false;
                try {
                    f.get(waitTime, TimeUnit.MILLISECONDS);
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
