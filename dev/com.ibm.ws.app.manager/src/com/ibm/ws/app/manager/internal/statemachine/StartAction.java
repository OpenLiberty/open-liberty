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
import com.ibm.ws.app.manager.internal.ApplicationInstallInfo;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

/**
 *
 */
class StartAction implements Action {
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
    private final CompletionListener<Boolean> _listener = new CompletionListener<Boolean>() {
        @SuppressWarnings("deprecation")
        @Override
        public void successfulCompletion(Future<Boolean> future, Boolean result) {
            StateChangeCallback callback = _callback.getAndSet(null);
            if (callback != null) {
                stopSlowStartMessage();
                if (result) {
                    String key = _update ? "APPLICATION_UPDATE_SUCCESSFUL" : "APPLICATION_START_SUCCESSFUL";
                    NotificationHelper.broadcastChange(_config.getMBeanNotifier(), _config.getMBeanName(), _update ? "application.update" : "application.start", Boolean.TRUE,
                                                       AppMessageHelper.get(_aii.getHandler()).formatMessage(key, _config.getName(),
                                                                                                             TimestampUtils.getElapsedTime(_startTime.get())));
                    AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName(), TimestampUtils.getElapsedTime(_startTime.get()));
                    callback.changed();
                } else {
                    if (!cancelled) {
                        String key = _update ? "APPLICATION_NOT_UPDATED" : "APPLICATION_NOT_STARTED";
                        NotificationHelper.broadcastChange(_config.getMBeanNotifier(), _config.getMBeanName(), _update ? "application.update" : "application.start", Boolean.FALSE,
                                                           AppMessageHelper.get(_aii.getHandler()).formatMessage(key, _config.getName()));
                        AppMessageHelper.get(_aii.getHandler()).audit(key, _config.getName());
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
                       FutureMonitor fm) {
        _config = config;
        _aii = appInstallInfo;
        _callback.set(scc);
        _monitor = fm;
        _appMonitor = appMonitor;
        _update = update;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutorService executor) {
        _startTime.set(System.currentTimeMillis());
        if (_tc.isInfoEnabled()) {
            AppMessageHelper.get(_aii.getHandler()).info("STARTING_APPLICATION", _config.getName());
        }
        long maxWait = ApplicationStateCoordinator.getApplicationStartTimeout();

        _slowMessageAction.set(((ScheduledExecutorService) executor).schedule(new Runnable() {

            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                AppMessageHelper.get(_aii.getHandler()).audit("APPLICATION_SLOW_STARTUP", _config.getName(), TimestampUtils.getElapsedTime(_startTime.get()));
            }
        }, maxWait, TimeUnit.SECONDS));

        try {
            @SuppressWarnings("rawtypes")
            ApplicationHandler handler = _aii.getHandler();
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
