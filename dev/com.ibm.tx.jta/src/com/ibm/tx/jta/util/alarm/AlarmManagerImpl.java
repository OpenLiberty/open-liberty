/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
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
package com.ibm.tx.jta.util.alarm;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.tx.util.alarm.Alarm;
import com.ibm.tx.util.alarm.AlarmListener;
import com.ibm.tx.util.alarm.AlarmManager;

public class AlarmManagerImpl implements AlarmManager {
    private static final int POOL_SIZE = 10;
    private final ScheduledThreadPoolExecutor _scheduler;

    public AlarmManagerImpl() {
        _scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(POOL_SIZE, new JTMThreadFactory());
        _scheduler.setRemoveOnCancelPolicy(true);
    }

    @Override
    public Alarm scheduleAlarm(final long millisecondDelay, AlarmListener listener, Object context) {
        final Runnable command = new AlarmListenerWrapper(listener, context);

        ScheduledFuture<?> future = AccessController.doPrivileged(new PrivilegedAction<ScheduledFuture<?>>() {
            @Override
            public ScheduledFuture<?> run() {
                return _scheduler.schedule(command, millisecondDelay, TimeUnit.MILLISECONDS);
            }
        });

        return new AlarmImpl(future);
    }

    @Override
    public Alarm scheduleDeferrableAlarm(long millisecondDelay, AlarmListener listener, Object context) {
        return scheduleAlarm(millisecondDelay, listener, context);
    }

    private static class AlarmListenerWrapper implements Runnable {
        private final ClassLoader _contextClassLoader;
        private final Object _context;
        private final AlarmListener _alarmListener;

        public AlarmListenerWrapper(AlarmListener alarmListener, Object context) {
            _alarmListener = alarmListener;
            _context = context;
            _contextClassLoader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
        }

        @Override
        public void run() {
            final ClassLoader originalLoader = setTCCL(_contextClassLoader);
            try {
                _alarmListener.alarm(_context);
            } finally {
                setTCCL(originalLoader);
            }
        }

        private ClassLoader setTCCL(final ClassLoader classLoader) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

                @Override
                public ClassLoader run() {
                    Thread currentThread = Thread.currentThread();
                    ClassLoader originalLoader = currentThread.getContextClassLoader();
                    currentThread.setContextClassLoader(classLoader);
                    return originalLoader;
                }
            });
        }
    }

    @Override
    public Alarm scheduleAlarm(long millisecondDelay, AlarmListener listener) {
        return scheduleAlarm(millisecondDelay, listener, null);
    }

    @Override
    public Alarm scheduleDeferrableAlarm(long millisecondDelay, AlarmListener listener) {
        return scheduleAlarm(millisecondDelay, listener, null);
    }

    @Override
    public void shutdown() {
        _scheduler.shutdown();
    }

    @Override
    public void shutdownNow() {
        _scheduler.shutdownNow();
    }
}