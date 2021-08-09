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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.internal.lifecycle.ServiceReg;
import com.ibm.ws.app.manager.internal.monitor.UpdateTrigger;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 *
 */
class ResolveFileAction implements Action, FileMonitor {
    private static final TraceComponent _tc = Tr.register(ResolveFileAction.class);
    private final WsLocationAdmin _locAdmin;
    private final String _configPid;
    private final String _location;
    private final AtomicReference<ResourceCallback> _callback = new AtomicReference<ResourceCallback>();
    private final String _name;
    private final BundleContext _ctx;
    private final ServiceReg<FileMonitor> _mon;
    private final UpdateTrigger _trigger;
    private final AtomicReference<File> _file = new AtomicReference<File>();
    private final AtomicReference<Container> _container = new AtomicReference<Container>();
    private final List<String> _filesToMonitor = new ArrayList<String>();
    private final AtomicReference<ApplicationHandler<?>> _handler;
    private final ReentrantLock lock;

    /**
     * @param _locAdmin
     * @param string
     * @param fm
     * @param callback
     */
    public ResolveFileAction(BundleContext ctx, long pollRate, UpdateTrigger trigger,
                             WsLocationAdmin locAdmin, String name, String configPid, String location,
                             ResourceCallback callback, AtomicReference<ApplicationHandler<?>> handler) {
        _locAdmin = locAdmin;
        _configPid = configPid;
        _location = location;
        _callback.set(callback);
        _name = name;
        _ctx = ctx;
        _trigger = trigger;
        _handler = handler;
        _mon = new ServiceReg<FileMonitor>();
        _mon.setProperties(new Hashtable<String, Object>());
        _mon.setProperty(Constants.SERVICE_VENDOR, "IBM");
        _mon.setProperty(FileMonitor.MONITOR_INTERVAL, pollRate);
        _mon.setProperty(FileMonitor.MONITOR_RECURSE, false);
        _mon.setProperty(FileMonitor.MONITOR_FILTER, ".*"); // find all types of file (including folders)
        _mon.setProperty(FileMonitor.MONITOR_INCLUDE_SELF, true);
        if (trigger != UpdateTrigger.DISABLED) {
            _mon.setProperty(FileMonitor.MONITOR_TYPE, trigger == UpdateTrigger.MBEAN ? FileMonitor.MONITOR_TYPE_EXTERNAL : FileMonitor.MONITOR_TYPE_TIMED);
        }
        _mon.setProperty(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");
        lock = new ReentrantLock();
    }

    private void findFile(boolean complete) {
        File f = null;
        for (String s : _filesToMonitor) {
            f = new File(s);
            if (f.exists())
                break;
        }

        if (f != null && f.exists()) {
            _file.set(f);
            ResourceCallback callback = _callback.get();
            if (callback != null) {
                _container.set(callback.setupContainer(_configPid, f));

                if (_container.get() != null) {
                    if (!complete) {
                        _mon.unregister();
                    }

                    callback = _callback.getAndSet(null);
                    if (callback != null) {
                        callback.successfulCompletion(_container.get(), _locAdmin.asResource(f, f.isFile()));
                    } else {
                        if (_tc.isEventEnabled()) {
                            Tr.event(_tc, "attempted to reuse callback");
                        }
                    }
                } else {
                    AppMessageHelper.get(_handler.get()).error("APPLICATION_AT_LOCATION_NOT_VALID", _location, _name);
                    if (complete) {
                        callback = _callback.getAndSet(null);
                        if (callback != null) {
                            callback.failedCompletion(null);
                        } else {
                            if (_tc.isEventEnabled()) {
                                Tr.event(_tc, "attempted to reuse callback");
                            }
                        }
                    } else {
                        callback = _callback.get();
                        if (callback != null) {
                            callback.pending();
                        }
                    }
                }
            } else {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "attempted to reuse callback");
                }
            }
        } else {
            if (complete) {
                ResourceCallback callback = _callback.getAndSet(null);
                if (callback != null) {
                    AppMessageHelper.get(_handler.get()).warning("APPLICATION_NOT_FOUND", _name, _location);
                    callback.failedCompletion(null);
                } else {
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, "attempted to reuse callback");
                    }
                }
            } else {
                ResourceCallback callback = _callback.get();
                if (callback != null) {
                    callback.pending();
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(ExecutorService executor) {
        File f = new File(_locAdmin.resolveString(_location));
        if (f.isAbsolute()) {
            resolve(_location, _filesToMonitor);
        } else {
            resolve(AppManagerConstants.SERVER_APPS_DIR + _location, _filesToMonitor);
            resolve(AppManagerConstants.SHARED_APPS_DIR + _location, _filesToMonitor);
        }
        _mon.setProperty(FileMonitor.MONITOR_DIRECTORIES, _filesToMonitor);
        _mon.setProperty(FileMonitor.MONITOR_FILES, _filesToMonitor);
        _mon.setProperty(com.ibm.ws.kernel.filemonitor.FileMonitor.MONITOR_IDENTIFICATION_NAME, "com.ibm.ws.kernel.monitor.artifact");

        if (_trigger == UpdateTrigger.DISABLED) {
            findFile(true);
        } else {
            _mon.register(_ctx, FileMonitor.class, this);
            if (_container.get() == null) {
                if (_file.get() == null) {
                    if (!FrameworkState.isStopping()) {
                        // Don't issue this message if the server is stopping
                        AppMessageHelper.get(_handler.get()).warning("APPLICATION_NOT_FOUND", _name, _location);
                    }
                }
            }
        }
    }

    private void resolve(String locationStem, List<String> locations) {
        String resolved = _locAdmin.resolveString(locationStem);
        locations.add(resolved);
        locations.add(resolved + ".xml");

    }

    /** {@inheritDoc} */
    @Override
    public void cancel() {
        lock.lock();
        try {
            _callback.set(null);
            _mon.unregister();
        } finally {
            lock.unlock();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBaseline(Collection<File> baseline) {
        onChange(baseline, Collections.<File> emptyList(), Collections.<File> emptyList());
    }

    /** {@inheritDoc} */
    @Override
    public void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles) {
        lock.lock();
        try {
            // If the callback is null, the action was either completed or cancelled.
            if (_callback.get() == null)
                return;

            _file.set(null);
            findFile(false);
        } finally {
            lock.unlock();
        }
    }
}
