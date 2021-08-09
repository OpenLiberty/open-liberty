/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.statemachine;

import java.io.File;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.ApplicationStateCoordinator;
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.internal.ApplicationConfig;
import com.ibm.ws.app.manager.internal.ApplicationDependency;
import com.ibm.ws.app.manager.internal.ApplicationInstallInfo;
import com.ibm.ws.app.manager.internal.FutureCollectionCompletionListener;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitorConfig;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.application.ApplicationState;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

class ApplicationStateMachineImpl extends ApplicationStateMachine implements ApplicationMonitor.UpdateHandler, Runnable {
    private static final TraceComponent _tc = Tr.register(ApplicationStateMachineImpl.class);

    // this is the set of internal states
    private enum InternalState {
        INITIAL, STOPPED, STARTING, STARTED, STOPPING, FAILED, REMOVED
    }

    // this is the set of actions that we can be given
    private enum StateChangeAction {
        CONFIGURE, START, STOP, RESTART, REMOVE
    };

    private enum CallbackState {
        CALLING, WAITING, RECEIVED
    }

    @Override
    public Future<Boolean> start() {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
            Tr.debug(_tc, asmLabel() + "appService: start for app " + _appConfig.get().getName());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        final ApplicationDependency appDep = createDependency("resolves when app " + getAppName() + " finishes starting");
        _notifyAppStarted.add(appDep);
        completeExplicitStartFuture();
        // We ignore startAfter when starting manually
        completeStartAfterFutures();
        attemptStateChange(StateChangeAction.START);
        return appDep.getFuture();
    }

    @Override
    public Future<Boolean> stop() {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
            Tr.debug(_tc, asmLabel() + "appService: stop for app " + _appConfig.get().getName());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        final ApplicationDependency appDep = createDependency("resolves when app " + getAppName() + " finishes stopping");
        _notifyAppStopped.add(appDep);
        createExplicitStartFuture();
        attemptStateChange(StateChangeAction.STOP);
        return appDep.getFuture();
    }

    @Override
    public void restart() {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
            Tr.debug(_tc, asmLabel() + "appService: restart for app " + _appConfig.get().getName());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        completeExplicitStartFuture();
        attemptStateChange(StateChangeAction.RESTART);
    }

    @Override
    public void setAppHandler(ApplicationHandler<?> appHandler) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "setAppHandler: interruptible=" + isInterruptible());
        }
        ApplicationHandler<?> oldHandler = _handler.getAndSet(appHandler);
        if (oldHandler == appHandler) {
            // setAppHandler is called on every configuration update, and in most
            // cases the app will have the same app handler it did before, so just
            // ignore it if nothing is changed
            return;
        }
        if (appHandler == null) {
            // Cancel any pending actions -- setAppHandler(null) is only called from UpdateEpisodeState.unsetAppHandler prior to
            // initiating a recycle, so there's no need to proceed with pending actions here, and doing so will likely result in
            // an NPE because the app handler no longer exists.
            cleanupActions();

            ApplicationDependency appHandlerFuture = createDependency("resolves when the app handler for app " + getAppName() + " arrives");
            appHandlerFuture = waitingForAppHandlerFuture.getAndSet(appHandlerFuture);
            if (appHandlerFuture != null) {
                resolveDependency(appHandlerFuture);
            }
        } else {
            completeAppHandlerFuture();
            if (oldHandler != null) {
                queueRestartChange(StateChangeAction.RESTART);
            }
        }
    }

    @Override
    public void configure(ApplicationConfig appConfig,
                          Collection<ApplicationDependency> appStartingFutures,
                          Collection<ApplicationDependency> startAfterFutures,
                          ApplicationDependency notifyAppStopped,
                          ApplicationDependency notifyAppStarting,
                          ApplicationDependency notifyAppInstallCalled,
                          ApplicationDependency notifyAppStarted) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "configure: interruptible=" + isInterruptible());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        final boolean checkForUnprocessedConfigChange = _nextAppConfig.getAndSet(appConfig) != null;

        addAppStartingFutures(appStartingFutures);
        updateStartAfterFutures(startAfterFutures);
        if (notifyAppStopped != null) {
            _notifyAppStopped.add(notifyAppStopped);
        }
        if (notifyAppStarting != null) {
            _notifyAppStarting.add(notifyAppStarting);
        }
        if (notifyAppInstallCalled != null) {
            _notifyAppInstallCalled.add(notifyAppInstallCalled);
        }
        if (notifyAppStarted != null) {
            _notifyAppStarted.add(notifyAppStarted);
        }
        if (getInternalState() == InternalState.INITIAL && !appConfig.isAutoStarted()) {
            createExplicitStartFuture();
        }
        if (checkForUnprocessedConfigChange) {
            synchronized (_interruptibleLock) {
                for (QueuedStateChangeAction queuedAction : _queuedActions) {
                    if (queuedAction.action == StateChangeAction.CONFIGURE) {
                        return;
                    }
                }
            }
        }
        queueStateChange(StateChangeAction.CONFIGURE);
    }

    @Override
    public void recycle(Collection<ApplicationDependency> appStartingFutures,
                        ApplicationDependency notifyAppStopped,
                        ApplicationDependency notifyAppInstallCalled,
                        ApplicationDependency notifyAppStarted) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "recycle: interruptible=" + isInterruptible());
        }

        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        addAppStartingFutures(appStartingFutures);
        if (notifyAppStopped != null) {
            _notifyAppStopped.add(notifyAppStopped);
        }
        if (notifyAppInstallCalled != null) {
            _notifyAppInstallCalled.add(notifyAppInstallCalled);
        }
        if (notifyAppStarted != null) {
            _notifyAppStarted.add(notifyAppStarted);
        }
        queueRestartChange(StateChangeAction.RESTART);
    }

    @Override
    public void uninstall(final ApplicationDependency notifyAppRemoved) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "uninstall: interruptible=" + isInterruptible());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            cl.cancel();
        }
        if (getInternalState() == InternalState.REMOVED) {
            throw new IllegalStateException("uninstall: removed");
        }
        if (notifyAppRemoved != null) {
            _notifyAppRemoved.add(notifyAppRemoved);
        }
        ApplicationDependency appStoppedFuture = createDependency("resolves when the app " + getAppName() + " finishes stopping, at which point it will be removed");
        appStoppedFuture.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "uninstall: successfulCompletion: future " + future + ", result " + result);
                }
                switchInternalState(InternalState.STOPPED, InternalState.REMOVED);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "uninstall: failedCompletion: future " + future + ", throwable " + t);
                }
                switchInternalState(InternalState.STOPPED, InternalState.FAILED);
            }
        });
        _notifyAppStopped.add(appStoppedFuture);
        queueStateChange(StateChangeAction.REMOVE);
    }

    @Trivial
    @Override
    public void describe(StringBuilder sb) {
        sb.append("\nASM");
        sb.append("\nSequence Number: ");
        sb.append(_asmSeqNo);
        sb.append("\nInternal State: ");
        sb.append(getInternalState());
        sb.append("\nCallback State: ");
        sb.append(_callbackState.get());

        if (!startAfterFutures.isEmpty()) {
            sb.append("\n\nStart After Dependencies: ");
            for (ApplicationDependency ad : startAfterFutures) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (!_notifyAppStopped.isEmpty()) {
            sb.append("\n\nApp Stopped Dependencies: ");
            for (ApplicationDependency ad : _notifyAppStopped) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (!_notifyAppInstallCalled.isEmpty()) {
            sb.append("\n\nApp Install Called Dependencies: ");
            for (ApplicationDependency ad : _notifyAppInstallCalled) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (!_notifyAppStarting.isEmpty()) {
            sb.append("\n\nApp Starting Dependencies: ");
            for (ApplicationDependency ad : _notifyAppStarting) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (!_notifyAppStarted.isEmpty()) {
            sb.append("\n\nApp Started Dependencies: ");
            for (ApplicationDependency ad : _notifyAppStarted) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (!_notifyAppRemoved.isEmpty()) {
            sb.append("\n\nApp Removed Dependencies: ");
            for (ApplicationDependency ad : _notifyAppRemoved) {
                sb.append("\n");
                sb.append(ad.toString());
            }
        }

        if (waitingForAppHandlerFuture.get() != null) {
            sb.append("\n\nWaiting for App Handler: ");
            sb.append(waitingForAppHandlerFuture.get());
        }

        if (waitingForExplicitStartFuture.get() != null) {
            sb.append("\n\nWaiting for Explicit Start: ");
            sb.append(waitingForExplicitStartFuture.get());
        }

        WsResource location = _resolvedLocation.get();
        if (location != null) {
            sb.append("\nResolved Location: ");
            sb.append(location.toString());
        }

    }

    @Override
    public void handleMonitorUpdate(boolean shouldRemove) {
        if (FrameworkState.isStopping()) {
            // we are stopping so ignore any monitor updates
            return;
        }
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "handleMonitorUpdate: interruptible=" + isInterruptible());
        }
        if (shouldRemove) {
            Object val = _appConfig.get().getConfigProperty(AppManagerConstants.AUTO_INSTALL_PROP);
            if (val == null || ((Boolean) val) == false) {
                // if it wasn't installed by dropins error message and stop
                AppMessageHelper.get(_handler.get()).error("INVALID_DELETE_OF_APPLICATION", _appConfig.get().getName(), _appConfig.get().getLocation());
            } else {
                // it's removal of an app installed by dropins and the configuration
                // for that app will be deleted so we need do nothing else here
                return;
            }
        }
        if (getInternalState() != InternalState.REMOVED) {
            queueStateChange(StateChangeAction.CONFIGURE);
        }
    }

    private class QueuedStateChangeAction {
        private final StateChangeAction action;
        private final int actionNum;

        QueuedStateChangeAction(StateChangeAction action, int actionNum) {
            this.action = action;
            this.actionNum = actionNum;
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "created " + this);
            }
        }

        @Override
        public String toString() {
            return "SCA[" + actionNum + "] action=" + action;
        }
    }

    private class CancelableCompletionListenerWrapper<T> implements CompletionListener<T> {
        private volatile CompletionListener<T> listener;

        public CancelableCompletionListenerWrapper(CompletionListener<T> listener) {
            this.listener = listener;
        }

        public void cancel() {
            listener = null;
        }

        @Override
        public void successfulCompletion(Future<T> future, T result) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "CCLW" + (listener == null ? "[cancelled]" : "") + ": successfulCompletion: completed future " + future + ", result " + result);
            }
            CompletionListener<T> l = listener;
            if (l != null) {
                l.successfulCompletion(future, result);
            }
        }

        @Override
        public void failedCompletion(Future<T> future, Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "CCLW" + (listener == null ? "[cancelled]" : "") + ": failedCompletion: future " + future + ", throwable " + t);
            }
            CompletionListener<T> l = listener;
            if (l != null) {
                l.failedCompletion(future, t);
            }
        }
    }

    private final class StartActionCallback implements StateChangeCallback {
        private final AtomicReference<InternalState> immediateCallbackResult = new AtomicReference<InternalState>();

        StartActionCallback() {
            _callbackState.set(CallbackState.CALLING);
        }

        synchronized InternalState resolvedState() {
            if (_callbackState.compareAndSet(CallbackState.CALLING, CallbackState.WAITING)) {
                ApplicationDependency installCalledFuture;
                while ((installCalledFuture = _notifyAppInstallCalled.poll()) != null) {
                    resolveDependency(installCalledFuture);
                }
                return null;
            } else {
                _currentAction.getAndSet(null);
                return immediateCallbackResult.getAndSet(null);
            }
        }

        @Override
        public synchronized void changed() {
            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                ApplicationDependency installCalledFuture;
                while ((installCalledFuture = _notifyAppInstallCalled.poll()) != null) {
                    resolveDependency(installCalledFuture);
                }
                immediateCallbackResult.set(InternalState.STARTED);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (switchInternalState(InternalState.STARTING, InternalState.STARTED)) {
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }

        @Override
        public void failed(Throwable t) {
            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                ApplicationDependency installCalledFuture;
                while ((installCalledFuture = _notifyAppInstallCalled.poll()) != null) {
                    resolveDependency(installCalledFuture);
                }
                _failedThrowable = t;
                immediateCallbackResult.set(InternalState.FAILED);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (switchInternalState(InternalState.STARTING, InternalState.FAILED)) {
                _failedThrowable = t;
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }
    }

    private final class StopActionCallback implements StateChangeCallback {
        private final AtomicReference<InternalState> immediateCallbackResult = new AtomicReference<InternalState>();

        StopActionCallback() {
            _callbackState.set(CallbackState.CALLING);
        }

        InternalState resolvedState() {
            if (_callbackState.compareAndSet(CallbackState.CALLING, CallbackState.WAITING)) {
                return null;
            } else {
                if (_currentAction.getAndSet(null) == null) {
                    return null;
                }
                return immediateCallbackResult.getAndSet(null);
            }
        }

        @Override
        public void changed() {
            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                immediateCallbackResult.set(InternalState.STOPPED);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (switchInternalState(InternalState.STOPPING, InternalState.STOPPED)) {
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }

        @Override
        public void failed(Throwable t) {
            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                _failedThrowable = t;
                immediateCallbackResult.set(InternalState.FAILED);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (switchInternalState(InternalState.STOPPING, InternalState.FAILED)) {
                _failedThrowable = t;
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }
    }

    private final class ResolveFileCallback implements ResourceCallback {
        private final AtomicReference<InternalState> immediateCallbackResult = new AtomicReference<InternalState>();

        ResolveFileCallback() {
            _callbackState.set(CallbackState.CALLING);
        }

        InternalState resolvedState() {
            if (_callbackState.compareAndSet(CallbackState.CALLING, CallbackState.WAITING)) {
                setInterruptible();
                return null;
            } else {
                if (_currentAction.getAndSet(null) == null) {
                    return null;
                }
                return immediateCallbackResult.getAndSet(null);
            }
        }

        @Override
        public void pending() {
            // The application was not immediately available, so the application
            // will not be starting as requested.
            for (ApplicationDependency appDep; (appDep = _notifyAppStarting.poll()) != null;) {
                failedDependency(appDep, null);
            }
            ApplicationStateCoordinator.updateStartingAppStatus(_appConfig.get().getConfigPid(), ApplicationStateCoordinator.AppStatus.FAILED);

        }

        @Override
        public void successfulCompletion(Container container, WsResource resource) {
            _appContainer.set(container);
            _resolvedLocation.set(resource);

            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                immediateCallbackResult.set(InternalState.STARTING);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (_internalState.compareAndSet(InternalState.STOPPED, InternalState.STARTING)) {
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    setNonInterruptible();
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }

        @Override
        public void failedCompletion(Throwable t) {
            if (_callbackState.compareAndSet(CallbackState.CALLING, null)) {
                _failedThrowable = t;
                immediateCallbackResult.set(InternalState.FAILED);
                return;
            }
            if (_currentAction.getAndSet(null) == null) {
                return;
            }
            if (_internalState.compareAndSet(InternalState.STOPPED, InternalState.FAILED)) {
                _failedThrowable = t;
                if (_callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                    _executorService.execute(ApplicationStateMachineImpl.this);
                }
            }
        }

        @Override
        public Container setupContainer(String pid, File locationFile) {
            File cacheDir = new File(getCacheDir(), pid);
            if (!FileUtils.ensureDirExists(cacheDir)) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "Could not create directory at {0}.", cacheDir.getAbsolutePath());
                }
                return null;
            }

            ArtifactContainer artifactContainer = _artifactFactory.getContainer(cacheDir, locationFile);
            if (artifactContainer == null) {
                return null;
            }

            File cacheDirAdapt = new File(getCacheAdaptDir(), pid);
            if (!FileUtils.ensureDirExists(cacheDirAdapt)) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "Could not create directory at {0}.", cacheDirAdapt.getAbsolutePath());
                }
                return null;
            }

            File cacheDirOverlay = new File(getCacheOverlayDir(), pid);
            if (!FileUtils.ensureDirExists(cacheDirOverlay)) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "Could not create directory at {0}.", cacheDirOverlay.getAbsolutePath());
                }
                return null;
            }

            return _moduleFactory.getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
        }

        private File getCacheDir() {
            return _locAdmin.getBundleFile(this, "cache");
        }

        private File getCacheAdaptDir() {
            return _locAdmin.getBundleFile(this, "cacheAdapt");
        }

        private File getCacheOverlayDir() {
            return _locAdmin.getBundleFile(this, "cacheOverlay");
        }
    }

    @SuppressWarnings("unchecked")
    final Future<Boolean>[] EMPTY_FUTURE_ARRAY = (Future<Boolean>[]) Array.newInstance(Future.class, 0);

    private static final AtomicLong asmSequenceNumber = new AtomicLong(0);

    private final AtomicInteger _qscaCounter = new AtomicInteger();

    private volatile Throwable _failedThrowable;

    private final Object _interruptibleLock = new Object() {
    };
    private volatile boolean _interruptible;
    private volatile boolean _performingQueuedActions;
    private final ConcurrentLinkedQueue<QueuedStateChangeAction> _queuedActions = new ConcurrentLinkedQueue<QueuedStateChangeAction>();
    private final AtomicReference<CallbackState> _callbackState = new AtomicReference<CallbackState>();

    private final Set<ApplicationDependency> blockAppStartingFutures = Collections.newSetFromMap(new ConcurrentHashMap<ApplicationDependency, Boolean>());
    private final AtomicReference<ApplicationDependency> waitingForAppHandlerFuture = new AtomicReference<ApplicationDependency>();
    private final AtomicReference<ApplicationDependency> waitingForExplicitStartFuture = new AtomicReference<ApplicationDependency>();
    private final Set<ApplicationDependency> startAfterFutures = Collections.newSetFromMap(new ConcurrentHashMap<ApplicationDependency, Boolean>());
    private final AtomicReference<CancelableCompletionListenerWrapper<Boolean>> completionListener = new AtomicReference<CancelableCompletionListenerWrapper<Boolean>>();

    private final ConcurrentLinkedQueue<ApplicationDependency> _notifyAppStopped = new ConcurrentLinkedQueue<ApplicationDependency>();
    private final ConcurrentLinkedQueue<ApplicationDependency> _notifyAppStarting = new ConcurrentLinkedQueue<ApplicationDependency>();
    private final ConcurrentLinkedQueue<ApplicationDependency> _notifyAppInstallCalled = new ConcurrentLinkedQueue<ApplicationDependency>();
    private final ConcurrentLinkedQueue<ApplicationDependency> _notifyAppStarted = new ConcurrentLinkedQueue<ApplicationDependency>();
    private final ConcurrentLinkedQueue<ApplicationDependency> _notifyAppRemoved = new ConcurrentLinkedQueue<ApplicationDependency>();

    private final Object _stateLock = new Object() {
    };
    private final AtomicReference<InternalState> _internalState = new AtomicReference<InternalState>();
    private final AtomicReference<Action> _currentAction = new AtomicReference<Action>();
    private final AtomicReference<ResolveFileAction> _rfa = new AtomicReference<ResolveFileAction>();
    private final AtomicReference<ApplicationInstallInfo> _appInstallInfo = new AtomicReference<ApplicationInstallInfo>();
    private final AtomicBoolean _update = new AtomicBoolean();

    private final AtomicReference<ApplicationConfig> _appConfig = new AtomicReference<ApplicationConfig>();
    private final AtomicReference<ApplicationConfig> _nextAppConfig = new AtomicReference<ApplicationConfig>();

    private final AtomicReference<Container> _appContainer = new AtomicReference<Container>();
    private final AtomicReference<WsResource> _resolvedLocation = new AtomicReference<WsResource>();
    private final AtomicReference<ApplicationHandler<?>> _handler = new AtomicReference<ApplicationHandler<?>>();

    private final long _asmSeqNo;
    private final BundleContext _ctx;
    private final WsLocationAdmin _locAdmin;
    private final FutureMonitor _futureMonitor;
    private final ArtifactContainerFactory _artifactFactory;
    private final AdaptableModuleFactory _moduleFactory;
    private final ExecutorService _executorService;
    private final ScheduledExecutorService _scheduledExecutorService;
    private final ApplicationStateMachine.ASMHelper _asmHelper;
    private final ApplicationMonitor _appMonitor;

    ApplicationStateMachineImpl(BundleContext ctx, WsLocationAdmin locAdmin, FutureMonitor futureMonitor,
                                ArtifactContainerFactory artifactFactory, AdaptableModuleFactory moduleFactory,
                                ExecutorService executorService, ScheduledExecutorService scheduledExecutorService,
                                ApplicationStateMachine.ASMHelper asmHelper, ApplicationMonitor appMonitor) {
        _asmSeqNo = asmSequenceNumber.getAndIncrement();
        _ctx = ctx;
        _locAdmin = locAdmin;
        _futureMonitor = futureMonitor;
        _artifactFactory = artifactFactory;
        _moduleFactory = moduleFactory;
        _executorService = executorService;
        _scheduledExecutorService = scheduledExecutorService;
        _asmHelper = asmHelper;
        _appMonitor = appMonitor;

        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "ASM[" + _asmSeqNo + "]: created");
        }

        waitingForAppHandlerFuture.set(createDependency("resolves when the app handler for this app arrives"));

        synchronized (_stateLock) {
            _internalState.set(InternalState.INITIAL);
        }
        setInterruptible();
    }

    @Trivial
    private String getAppName() {
        final ApplicationConfig appConfig = _appConfig.get();
        return appConfig != null ? appConfig.getName() : null;
    }

    @Trivial
    private String asmLabel() {
        final ApplicationConfig appConfig = _appConfig.get();
        return "ASM[" + _asmSeqNo + "]: " + (appConfig != null ? appConfig.getName() + ": " : "");
    }

    @Trivial
    private InternalState getInternalState() {
        synchronized (_stateLock) {
            return _internalState.get();
        }
    }

    private boolean switchInternalState(InternalState oldState, InternalState newState) {
        synchronized (_stateLock) {
            return _internalState.compareAndSet(oldState, newState);
        }
    }

    @FFDCIgnore(MalformedURLException.class)
    private boolean isLocationAURL(String location) {
        try {
            new URL(location);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Trivial
    boolean isInterruptible() {
        synchronized (_interruptibleLock) {
            return _interruptible;
        }
    }

    void assertInterruptible() {
        synchronized (_interruptibleLock) {
            if (_interruptible != true) {
                interruptibleFailure();
            }
        }
    }

    void assertNonInterruptible() {
        synchronized (_interruptibleLock) {
            if (_interruptible != false) {
                interruptibleFailure();
            }
        }
    }

    void setInterruptible() {
        executeQueuedActions();
        synchronized (_interruptibleLock) {
            if (_interruptible) {
                interruptibleFailure();
            }
            _interruptible = true;
        }
    }

    void setNonInterruptible() {
        synchronized (_interruptibleLock) {
            if (!_interruptible) {
                interruptibleFailure();
            }
            _interruptible = false;
        }
    }

    void interruptibleFailure() {
        throw new IllegalStateException("interruptibleFailure");
    }

    void flushQueuedActions() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "flushQueuedActions: interruptible=" + isInterruptible());
        }
        synchronized (_interruptibleLock) {
            _queuedActions.clear();
        }
    }

    boolean executeQueuedActions() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "executeQueuedActions: interruptible=" + isInterruptible());
        }
        synchronized (_interruptibleLock) {
            if (_performingQueuedActions) {
                return false;
            }
            _performingQueuedActions = true;
        }
        boolean performedAction = false;
        for (;;) {
            assertNonInterruptible();
            QueuedStateChangeAction queuedAction;
            synchronized (_interruptibleLock) {
                if (_queuedActions.isEmpty()) {
                    if (!_performingQueuedActions) {
                        interruptibleFailure();
                    }
                    _performingQueuedActions = false;
                    return performedAction;
                }
                queuedAction = _queuedActions.poll();
            }
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "executeQueuedActions: executing " + queuedAction);
            }
            performAction(queuedAction.action);
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "executeQueuedActions: executed " + queuedAction);
            }
            performedAction = true;
        }
    }

    @Override
    public void run() {
        try {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "run: interruptible=" + isInterruptible());
            }
            synchronized (_interruptibleLock) {
                if (_performingQueuedActions) {
                    return;
                }
                _performingQueuedActions = true;
            }
            for (;;) {
                InternalState callbackReceivedState = null;
                QueuedStateChangeAction queuedAction = null;
                synchronized (_interruptibleLock) {
                    if (!isInterruptible() && _callbackState.get() == CallbackState.WAITING) {
                        _performingQueuedActions = false;
                        return;
                    }
                    if (_callbackState.compareAndSet(CallbackState.RECEIVED, null)) {
                        callbackReceivedState = getInternalState();
                    } else {
                        if (_queuedActions.isEmpty()) {
                            if (!_performingQueuedActions) {
                                interruptibleFailure();
                            }
                            _performingQueuedActions = false;
                            return;
                        }
                        setNonInterruptible();
                        queuedAction = _queuedActions.poll();
                        if (_tc.isDebugEnabled()) {
                            Tr.debug(_tc, asmLabel() + "run: next queued action: " + queuedAction);
                        }
                    }
                }
                if (callbackReceivedState != null) {
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, asmLabel() + "run: calling enterState " + callbackReceivedState);
                    }
                    enterState(callbackReceivedState);
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, asmLabel() + "run: called enterState " + callbackReceivedState);
                    }
                } else {
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, asmLabel() + "run: executing " + queuedAction);
                    }
                    performAction(queuedAction.action);
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, asmLabel() + "run: executed " + queuedAction);
                    }
                }
            }
        } catch (Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "run: caught throwable " + t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * queueRestartChange is intended to be used only for RESTART actions. It will wait for 30 seconds for the
     * action queue to be empty before it adds the restart action. This means that we may end up doing extra work
     * (eg, finishing a start action before doing a restart), but in practice it has been nearly impossible to
     * handle all of the timing issues resulting from canceling operations in flight.
     */
    void queueRestartChange(StateChangeAction action) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "queueRestartChange: interruptible=" + isInterruptible());
        }
        for (int i = 0; i < 30; i++) {
            synchronized (_interruptibleLock) {
                if (_queuedActions.isEmpty()) {
                    QueuedStateChangeAction qa = new QueuedStateChangeAction(action, _qscaCounter.getAndIncrement());
                    _queuedActions.add(qa);
                    if (_tc.isDebugEnabled()) {
                        Tr.debug(_tc, asmLabel() + "queueRestartChange: added action " + qa);
                    }
                    _executorService.execute(this);
                    return;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //Auto FFDC only
            }
        }

        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "queueRestartChange: Restart action could not be added within 30 seconds.");
        }

    }

    void queueStateChange(StateChangeAction action) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "queueStateChange: interruptible=" + isInterruptible());
        }
        synchronized (_interruptibleLock) {
            QueuedStateChangeAction qa = new QueuedStateChangeAction(action, _qscaCounter.getAndIncrement());
            _queuedActions.add(qa);
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, asmLabel() + "queueStateChange: added action " + qa);
            }
        }

        _executorService.execute(this);
    }

    void attemptStateChange(StateChangeAction action) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "attemptStateChange: interruptible=" + isInterruptible());
        }
        final boolean callPerformAction;
        final boolean callQueueAction;
        synchronized (_interruptibleLock) {
            if (!_performingQueuedActions && _queuedActions.isEmpty()) {
                if (isInterruptible()) {
                    setNonInterruptible();
                    callPerformAction = true;
                    callQueueAction = false;
                } else {
                    callPerformAction = false;
                    final InternalState internalState = getInternalState();
                    callQueueAction = !(internalState == InternalState.STOPPING || (internalState == InternalState.STARTING && action == StateChangeAction.START));
                }
            } else {
                callPerformAction = false;
                callQueueAction = true;
            }
        }
        if (callPerformAction) {
            performAction(action);
        } else if (callQueueAction) {
            queueStateChange(action);
        }
    }

    @Trivial
    ApplicationDependency createDependency(String desc) {
        ApplicationDependency appDep = new ApplicationDependency(_futureMonitor, desc);
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "createDependency: created " + appDep);
        }
        return appDep;
    }

    @Trivial
    void resolveDependency(ApplicationDependency appDep) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "resolveDependency: " + appDep);
        }
        try {
            appDep.setResult(true);
        } catch (Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "resolveDependency: caught throwable " + t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    @Trivial
    void failedDependency(ApplicationDependency appDep, Throwable failure) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "failedDependency: " + appDep);
        }
        try {
            appDep.setResult(failure);
        } catch (Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, asmLabel() + "failedDependency: caught throwable " + t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    private void addAppStartingFutures(Collection<ApplicationDependency> appStartingFutures) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "addAppStartingFutures: interruptible=" + isInterruptible());
        }
        blockAppStartingFutures.addAll(appStartingFutures);
    }

    private void updateStartAfterFutures(Collection<ApplicationDependency> startAfters) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "updateStartAfterFutures: interruptible=" + isInterruptible());
        }
        Iterator<ApplicationDependency> iter = startAfterFutures.iterator();
        while (iter.hasNext()) {
            ApplicationDependency ad = iter.next();
            if (!startAfters.contains(ad)) {
                blockAppStartingFutures.remove(ad);
                iter.remove();
            }
        }

        startAfterFutures.addAll(startAfters);
        blockAppStartingFutures.addAll(startAfters);
    }

    private void addAppHandlerFuture() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "addAppHandlerFuture: interruptible=" + isInterruptible());
        }
        final ApplicationDependency appHandlerFuture = waitingForAppHandlerFuture.get();
        blockAppStartingFutures.add(appHandlerFuture);
    }

    private void completeAppHandlerFuture() {
        final ApplicationDependency appHandlerFuture = waitingForAppHandlerFuture.get();
        resolveDependency(appHandlerFuture);
    }

    private void createExplicitStartFuture() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "createExplicitStartFuture: interruptible=" + isInterruptible());
        }
        final ApplicationDependency explicitStartFuture = createDependency("resolves when the app " + getAppName() + " is explicitly (re)started");
        if (waitingForExplicitStartFuture.compareAndSet(null, explicitStartFuture)) {
            blockAppStartingFutures.add(explicitStartFuture);
        }
    }

    private void completeExplicitStartFuture() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "completeExplicitStartFuture: interruptible=" + isInterruptible());
        }
        final ApplicationDependency explicitStartFuture = waitingForExplicitStartFuture.getAndSet(null);
        if (explicitStartFuture != null) {
            resolveDependency(explicitStartFuture);
        }
    }

    private void completeStartAfterFutures() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "completeStartAfterFutures: interruptible=" + isInterruptible());
        }
        for (ApplicationDependency ad : startAfterFutures) {
            resolveDependency(ad);
        }
    }

    private boolean waitForFutures() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "waitForFutures: interruptible=" + isInterruptible());
        }
        final Collection<ApplicationDependency> futureConditions = Arrays.asList(blockAppStartingFutures.toArray(new ApplicationDependency[] {}));
        CompletionListener<Boolean> listener = new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "waitForFutures: successfulCompletion: future " + future + ", result " + result);
                }
                CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
                if (cl != null) {
                    blockAppStartingFutures.removeAll(futureConditions);
                    final boolean execute;
                    synchronized (_interruptibleLock) {
                        if (!_callbackState.compareAndSet(CallbackState.CALLING, null) && _callbackState.compareAndSet(CallbackState.WAITING, CallbackState.RECEIVED)) {
                            setNonInterruptible();
                            execute = true;
                        } else {
                            execute = false;
                        }
                    }
                    if (execute) {
                        _executorService.execute(ApplicationStateMachineImpl.this);
                    }
                }
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, asmLabel() + "waitForFutures: failedCompletion: future " + future + ", throwable " + t);
                }
            }
        };
        CancelableCompletionListenerWrapper<Boolean> newCL = new CancelableCompletionListenerWrapper<Boolean>(listener);
        CancelableCompletionListenerWrapper<Boolean> oldCL = completionListener.getAndSet(newCL);
        if (oldCL != null) {
            oldCL.cancel();
        }
        _callbackState.set(CallbackState.CALLING);
        FutureCollectionCompletionListener.newFutureCollectionCompletionListener(futureConditions, newCL);

        synchronized (_interruptibleLock) {
            if (_callbackState.compareAndSet(CallbackState.CALLING, CallbackState.WAITING)) {
                setInterruptible();
                return true;
            } else {
                return false;
            }
        }
    }

    private void cancelWaitForFutures() {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "cancelWaitForFutures: interruptible=" + isInterruptible());
        }
        CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
        if (cl != null) {
            _callbackState.set(null);
            cl.cancel();
        }
    }

    private void performAction(StateChangeAction action) {

        assertNonInterruptible();
        final InternalState currentState = getInternalState();

        final InternalState nextState;
        switch (currentState) {
            case INITIAL:
                if (action == StateChangeAction.REMOVE) {
                    switchInternalState(InternalState.INITIAL, InternalState.REMOVED);
                    enterState(InternalState.REMOVED);
                } else {
                    ApplicationConfig nextAppConfig = _nextAppConfig.getAndSet(null);
                    if (nextAppConfig != null) {
                        _appConfig.set(nextAppConfig);
                    }
                    switchInternalState(InternalState.INITIAL, InternalState.STOPPED);
                    enterState(InternalState.STOPPED);
                }
                break;
            case STOPPED:
                cancelWaitForFutures();
                cleanupActions();
                if (action == StateChangeAction.REMOVE) {
                    switchInternalState(InternalState.STOPPED, InternalState.REMOVED);
                    enterState(InternalState.REMOVED);
                } else {
                    enterState(InternalState.STOPPED);
                }
                break;
            case STARTING:
                break; // we leave this state when StartAction completes its StateChangeCallback
            case STARTED:
                if (action == StateChangeAction.START) {
                    enterState(InternalState.STARTED);
                } else {
                    switchInternalState(InternalState.STARTED, InternalState.STOPPING);
                    enterState(InternalState.STOPPING);
                }
                break;
            case STOPPING:
                break; // we leave this state when StopAction completes its StateChangeCallback
            case FAILED:
                if (action == StateChangeAction.REMOVE) {
                    nextState = InternalState.REMOVED;
                } else {
                    nextState = InternalState.STOPPED;
                }
                switchInternalState(InternalState.FAILED, nextState);
                enterState(nextState);
                break;
            case REMOVED:
                break; // this is a terminal state, we never leave it
            default:
                throw new IllegalStateException("currentState");
        }

    }

    private void cleanupActions() {
        synchronized (_stateLock) {
            ResolveFileAction rfa = _rfa.getAndSet(null);
            if (rfa != null) {
                rfa.cancel();
            }
            Action a = _currentAction.getAndSet(null);
            if (a != null) {
                a.cancel();
            }
        }
    }

    private final AtomicBoolean enterStateCalled = new AtomicBoolean();

    private void enterState(InternalState state) {
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, asmLabel() + "enterState: interruptible=" + isInterruptible());
        }
        if (!enterStateCalled.compareAndSet(false, true)) {
            throw new RuntimeException("enterState reentry");
        }
        try {
            for (;;) {
                assertNonInterruptible();
                switch (state) {
                    case INITIAL:
                        // since we start in this state and never return to it
                        // the enterState method is never called
                        throw new IllegalStateException("enterState");
                    case STOPPED:
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.STOPPED);
                        flushQueuedActions();
                        ApplicationDependency stoppedFuture;
                        while ((stoppedFuture = _notifyAppStopped.poll()) != null) {
                            resolveDependency(stoppedFuture);
                        }
                        // if an internal completion listener changes us to a different state
                        // then we take no action here
                        if ((state = getInternalState()) != InternalState.STOPPED) {
                            break;
                        }
                        if (waitForFutures()) {
                            return;
                        }
                        ApplicationConfig nextAppConfig = _nextAppConfig.getAndSet(null);
                        if (nextAppConfig != null) {
                            _appConfig.set(nextAppConfig);
                        }
                        if (!_appConfig.get().isValid()) {
                            try {
                                throw new IllegalArgumentException("ApplicationConfig");
                            } catch (IllegalArgumentException e) {
                                _failedThrowable = e;
                            }
                            switchInternalState(InternalState.STOPPED, InternalState.FAILED);
                            state = InternalState.FAILED;
                            break;
                        }
                        if (_handler.get() == null) {
                            CancelableCompletionListenerWrapper<Boolean> cl = completionListener.getAndSet(null);
                            if (cl != null) {
                                cl.cancel();
                            }
                            // wait for the app handler to arrive, note this is done even if we know the type is not supported.
                            addAppHandlerFuture();
                            if (!_asmHelper.appTypeSupported()) {
                                Tr.error(_tc, "NO_APPLICATION_HANDLER", _appConfig.get().getLocation());
                                // we only fail here if the app type is not supported; otherwise we assume the handler is coming
                                for (ApplicationDependency startingFuture; (startingFuture = _notifyAppStarting.poll()) != null;) {
                                    failedDependency(startingFuture, null);
                                }
                            }
                            break;
                        }
                        final ResolveFileCallback resolveFileCallback;
                        final Action resolveFileAction;
                        synchronized (_stateLock) {
                            resolveFileCallback = new ResolveFileCallback();
                            ApplicationConfig appConfig = _appConfig.get();
                            String configPid = appConfig.getConfigPid();
                            String location = appConfig.getLocation();
                            if (isLocationAURL(location)) {
                                resolveFileAction = new DownloadFileAction(_locAdmin, configPid, location, resolveFileCallback, _handler);
                            } else {
                                ApplicationMonitorConfig appMonitorConfig = _appMonitor.getConfig();
                                resolveFileAction = new ResolveFileAction(_ctx, appMonitorConfig.getPollingRate(), appMonitorConfig.getUpdateTrigger(), _locAdmin, appConfig.getName(), configPid, location, resolveFileCallback, _handler);
                                _rfa.set((ResolveFileAction) resolveFileAction);
                            }
                            _currentAction.set(resolveFileAction);
                        }
                        resolveFileAction.execute(_executorService);
                        state = resolveFileCallback.resolvedState();
                        if (state != null) {
                            switchInternalState(InternalState.STOPPED, state);
                            break;
                        }
                        return;
                    case STARTING:
                        cleanupActions();
                        final StartActionCallback startCallback;
                        final Action startAction;
                        synchronized (_stateLock) {
                            ApplicationInstallInfo aii = new ApplicationInstallInfo(_appConfig.get(), _appContainer.getAndSet(null), _resolvedLocation.getAndSet(null), _handler.get(), ApplicationStateMachineImpl.this);
                            _appInstallInfo.set(aii); // capture the handler so we call the same one for stopping.
                            startCallback = new StartActionCallback();
                            startAction = new StartAction(_appConfig.get(), _update.getAndSet(true), _appMonitor, aii, startCallback, _futureMonitor);
                            _currentAction.set(startAction);
                        }
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.STARTING);
                        for (ApplicationDependency startingFuture; (startingFuture = _notifyAppStarting.poll()) != null;) {
                            resolveDependency(startingFuture);
                        }
                        startAction.execute(_scheduledExecutorService);
                        state = startCallback.resolvedState();
                        if (state != null) {
                            switchInternalState(InternalState.STARTING, state);
                            break;
                        }
                        return;
                    case STARTED:
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.STARTED);
                        ApplicationDependency startedFuture = null;
                        while ((startedFuture = _notifyAppStarted.poll()) != null) {
                            resolveDependency(startedFuture);
                        }
                        _asmHelper.notifyAppStarted(_appConfig.get().getConfigPid());
                        setInterruptible();
                        return;
                    case STOPPING:
                        final StopActionCallback stopCallback;
                        final Action stopAction;
                        synchronized (_stateLock) {
                            stopCallback = new StopActionCallback();
                            stopAction = new StopAction(_appInstallInfo.getAndSet(null), _appMonitor, _futureMonitor, stopCallback);
                            _currentAction.set(stopAction);
                        }
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.STOPPING);
                        stopAction.execute(_executorService);
                        state = stopCallback.resolvedState();
                        if (state != null) {
                            switchInternalState(InternalState.STOPPING, state);
                            break;
                        }
                        return;
                    case FAILED:
                        synchronized (_stateLock) {
                            _appInstallInfo.set(null);
                        }
                        cleanupActions();
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.INSTALLED);
                        ApplicationDependency failedFuture = null;
                        while ((failedFuture = _notifyAppStarting.poll()) != null) {
                            failedDependency(failedFuture, _failedThrowable);
                        }
                        while ((failedFuture = _notifyAppStarted.poll()) != null) {
                            failedDependency(failedFuture, _failedThrowable);
                        }
                        while ((failedFuture = _notifyAppStopped.poll()) != null) {
                            failedDependency(failedFuture, _failedThrowable);
                        }
                        _asmHelper.notifyAppFailed(_appConfig.get().getConfigPid());
                        setInterruptible();
                        return;
                    case REMOVED:
                        _asmHelper.switchApplicationState(_appConfig.get(), ApplicationState.INSTALLED);
                        _appMonitor.removeApplication(_appConfig.get().getConfigPid());
                        ApplicationDependency removedFuture = null;
                        while ((removedFuture = _notifyAppRemoved.poll()) != null) {
                            resolveDependency(removedFuture);
                        }
                        while ((failedFuture = _notifyAppStarting.poll()) != null) {
                            failedDependency(failedFuture, null);
                        }
                        while ((startedFuture = _notifyAppStarted.poll()) != null) {
                            // NOTE: Ideally we should call failedDependency here. We just removed this app, so we shouldn't tell listeners
                            // that it has started. In practice we only use the startedFuture to block apps from starting before RARs.
                            // If the RAR has been removed, we don't need to wait on it.
                            resolveDependency(startedFuture);
                        }
                        flushQueuedActions();
                        setInterruptible();
                        return;
                }
            }
        } finally {
            if (!enterStateCalled.compareAndSet(true, false)) {
                throw new RuntimeException("enterState finally");
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.app.manager.internal.statemachine.ApplicationStateMachine#isBlocked()
     */
    @Override
    public boolean isBlocked() {
        final Collection<ApplicationDependency> blockingConditions = new ArrayList<ApplicationDependency>(blockAppStartingFutures);
        for (ApplicationDependency blockingCondition : blockingConditions) {
            if (!blockingCondition.isDone()) {
                return true;
            }
        }
        return false;
    }

}
