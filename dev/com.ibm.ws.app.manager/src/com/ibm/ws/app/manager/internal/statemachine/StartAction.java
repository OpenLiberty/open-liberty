/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
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
package com.ibm.ws.app.manager.internal.statemachine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.ApplicationStateCoordinator;
import com.ibm.ws.app.manager.NotificationHelper;
import com.ibm.ws.app.manager.internal.ApplicationConfig;
import com.ibm.ws.app.manager.internal.ApplicationConfigurator;
import com.ibm.ws.app.manager.internal.ApplicationInstallInfo;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 *
 */
class StartAction implements Action, CheckpointHook {
    private static final TraceComponent _tc = Tr.register(StartAction.class);
    private final ApplicationConfig _config;
    private final ApplicationInstallInfo _aii;
    private final AtomicReference<StateChangeCallback> _callback = new AtomicReference<StateChangeCallback>();
    private final FutureMonitor _monitor;
    private final AtomicLong _startTime = new AtomicLong();
    private final ApplicationMonitor _appMonitor;
    private final boolean _update;
    private volatile boolean cancelled = false;
    private final AtomicReference<Future<?>> _slowMessageAction = new AtomicReference<Future<?>>();
    private final ApplicationConfigurator _configurator;
    private final CompletionListener<Boolean> _listener = new CompletionListener<Boolean>() {

        @Override
        public void successfulCompletion(Future<Boolean> future, Boolean result) {
            StateChangeCallback callback = _callback.getAndSet(null);
            if (callback != null) {
                stopSlowStartMessage();
                if (result) {
                    String key = _update ? "APPLICATION_UPDATE_SUCCESSFUL" : "APPLICATION_START_SUCCESSFUL";
                    NotificationHelper.broadcastChange(_config.getMBeanNotifier(), _config.getMBeanName(), _update ? "application.update" : "application.start", Boolean.TRUE,
                                                       AppMessageHelper.get(_aii.getHandler()).formatMessage(key, _config.getName(),
                                                                                                             TimestampUtils.getElapsedTimeNanos(_startTime.get())));
                    AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName(), TimestampUtils.getElapsedTimeNanos(_startTime.get()));
                    _configurator.restoreMessage(() -> {
                        AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName(), TimestampUtils.getElapsedTime());
                    });
                    callback.changed();
                } else {
                    if (!cancelled) {
                        String key = _update ? "APPLICATION_NOT_UPDATED" : "APPLICATION_NOT_STARTED";
                        NotificationHelper.broadcastChange(_config.getMBeanNotifier(), _config.getMBeanName(), _update ? "application.update" : "application.start", Boolean.FALSE,
                                                           AppMessageHelper.get(_aii.getHandler()).formatMessage(key, _config.getName()));
                        AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName());
                        _configurator.restoreMessage(() -> {
                            AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName());
                        });
                    }
                    callback.failed(null);
                }
            } else {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "attempted to reuse callback");
                }
            }
        }

        @Override
        public void failedCompletion(Future<Boolean> future, Throwable t) {
            StateChangeCallback callback = _callback.getAndSet(null);
            if (callback != null) {
                stopSlowStartMessage();

                String key = _update ? "APPLICATION_UPDATE_FAILED" : "APPLICATION_START_FAILED";
                NotificationHelper.broadcastChange(_config.getMBeanNotifier(), _config.getMBeanName(), _update ? "application.update" : "application.start", Boolean.FALSE,
                                                   AppMessageHelper.get(_aii.getHandler()).formatMessage(key, _config.getName(), t.toString()));
                AppMessageHelper.get(_aii.getHandler()).error(key, _config.getName(), t.toString());
                _configurator.restoreMessage(() -> {
                    AppMessageHelper.get(_aii.getHandler()).error(key, _config.getName(), t.toString());
                });
                callback.failed(t);
            } else {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "attempted to reuse callback");
                }
            }
        }
    };

    /**
     * @param update
     * @param _appMonitor
     * @param _config
     * @param applicationHandler
     * @param scc
     */
    public StartAction(ApplicationConfig config,
                       boolean update, ApplicationMonitor appMonitor,
                       ApplicationInstallInfo appInstallInfo,
                       StateChangeCallback scc,
                       FutureMonitor fm,
                       ApplicationConfigurator configurator) {
        _config = config;
        _aii = appInstallInfo;
        _callback.set(scc);
        _monitor = fm;
        _appMonitor = appMonitor;
        _update = update;
        _configurator = configurator;
        CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();
        if (checkpointPhase == CheckpointPhase.AFTER_APP_START) {
            // Only for afterAppStart; we need a hook that fails checkpoint if the application has not started yet
            checkpointPhase.addMultiThreadedHook(this);
        }
        if (checkpointPhase == CheckpointPhase.BEFORE_APP_START && !checkpointPhase.restored()) {
            // Only for beforeAppStart; we need to reset the start time to get accurate app start time message
            CheckpointPhase.onRestore(() -> _startTime.set(TimestampUtils.getStartTimeNano()));
        }
    }

    @Override
    public void prepare() {
        if (_callback.get() != null) {
            // application startup timed out, fail checkpoint
            final ApplicationHandler<?> handler = _aii.getHandler();
            if (handler == null) {
                // this should never happen
                throw new IllegalStateException("The application handler is not available");
            }
            throw new IllegalStateException(AppMessageHelper.get(handler).formatMessage("APPLICATION_SLOW_STARTUP", _config.getName(),
                                                                                        TimestampUtils.getElapsedTimeNanos(_startTime.get())));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutorService executor) {
        _startTime.set(System.nanoTime());
        @SuppressWarnings({ "rawtypes" })
        final ApplicationHandler handler = _aii.getHandler();
        if (handler == null) {
            _listener.failedCompletion(null, new IllegalArgumentException("The application handler is not available"));
            return;
        }

        if (_tc.isInfoEnabled()) {
            AppMessageHelper.get(handler).info("STARTING_APPLICATION", _config.getName());
        }
        long maxWait = ApplicationStateCoordinator.getApplicationStartTimeout();

        _slowMessageAction.set(((ScheduledExecutorService) executor).schedule(new Runnable() {

            @Override
            public void run() {
                AppMessageHelper.get(handler).audit("APPLICATION_SLOW_STARTUP", _config.getName(), TimestampUtils.getElapsedTimeNanos(_startTime.get()));
            }
        }, maxWait, TimeUnit.SECONDS));

        try {
            @SuppressWarnings("unchecked")
            ApplicationMonitoringInformation ami = handler.setUpApplicationMonitoring(_aii);
            _aii.setApplicationMonitoringInformation(ami);
            _appMonitor.addApplication(_aii);

            @SuppressWarnings("unchecked")
            Future<Boolean> result = handler.install(_aii);
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Handler install called, result: " + result);
            }
            _monitor.onCompletion(result, _listener);

        } catch (Throwable t) {
            _listener.failedCompletion(null, t);
        }
    }

    /**
     *
     */
    private void stopSlowStartMessage() {
        Future<?> slow = _slowMessageAction.getAndSet(null);
        if (slow != null) {
            slow.cancel(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        this.cancelled = true;
        stopSlowStartMessage();
    }
}
