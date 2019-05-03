/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.NotificationHelper;
import com.ibm.ws.app.manager.internal.ApplicationInstallInfo;
import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.application.handler.ApplicationHandler;

/**
 *
 */
class StopAction implements Action {
    private static final TraceComponent _tc = Tr.register(StopAction.class);
    private final ApplicationInstallInfo _aii;
    private final FutureMonitor _monitor;
    private final AtomicReference<StateChangeCallback> _callback = new AtomicReference<StateChangeCallback>();
    private final ApplicationMonitor _appMonitor;
    private final CompletionListener<Boolean> _listener = new CompletionListener<Boolean>() {
        @Override
        public void successfulCompletion(Future<Boolean> future, Boolean result) {
            StateChangeCallback callback = _callback.getAndSet(null);
            if (callback != null) {
                NotificationHelper.broadcastChange(_aii.getMBeanNotifier(), _aii.getMBeanName(), "application.stop", Boolean.TRUE,
                                                   AppMessageHelper.get(_aii.getHandler()).formatMessage("APPLICATION_STOPPED", _aii.getName()));
                AppMessageHelper.get(_aii.getHandler()).audit("APPLICATION_STOPPED", _aii.getName());
                callback.changed();
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
                NotificationHelper.broadcastChange(_aii.getMBeanNotifier(), _aii.getMBeanName(), "application.stop", Boolean.FALSE,
                                                   AppMessageHelper.get(_aii.getHandler()).formatMessage("APPLICATION_STOP_FAILED", _aii.getName(), t.toString()));
                AppMessageHelper.get(_aii.getHandler()).error("APPLICATION_STOP_FAILED", _aii.getName(), t.toString());
                callback.failed(t);
            } else {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "attempted to reuse callback");
                }
            }
        }
    };

    public StopAction(ApplicationInstallInfo aii, ApplicationMonitor appMonitor, FutureMonitor monitor, StateChangeCallback callback) {
        _aii = aii;
        _monitor = monitor;
        _callback.set(callback);
        _appMonitor = appMonitor;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutorService executor) {
        try {
            _appMonitor.removeApplication(_aii.getPid());
            @SuppressWarnings("rawtypes")
            ApplicationHandler handler = _aii.getHandler();
            @SuppressWarnings("unchecked")
            Future<Boolean> result = handler.uninstall(_aii);
            _monitor.onCompletion(result, _listener);
        } catch (Throwable t) {
            _listener.failedCompletion(null, t);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        _callback.set(null);
    }
}