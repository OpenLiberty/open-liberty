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
package com.ibm.ws.app.manager.internal;

import java.util.concurrent.atomic.AtomicReference;

import javax.management.NotificationBroadcasterSupport;

import com.ibm.ws.app.manager.internal.monitor.ApplicationMonitor;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoForContainer;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.application.handler.ApplicationMonitoringInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class ApplicationInstallInfo implements ApplicationInformation<Object>, ApplicationInfoForContainer {
    private final ApplicationConfig _config;
    private final AtomicReference<Object> _handlerInfo = new AtomicReference<Object>();
    private final AtomicReference<Container> _container = new AtomicReference<Container>();
    private final AtomicReference<WsResource> _resolvedLocation = new AtomicReference<WsResource>();
    private final ApplicationHandler<?> _handler;
    private final AtomicReference<ApplicationMonitoringInformation> _ami = new AtomicReference<ApplicationMonitoringInformation>();
    private final ApplicationMonitor.UpdateHandler _updateHandler;

    public ApplicationInstallInfo(ApplicationConfig cfg, Container container, WsResource resolvedLocation,
                                  ApplicationHandler<?> handler, ApplicationMonitor.UpdateHandler updateHandler) {
        _config = cfg;
        _container.set(container);
        _resolvedLocation.set(resolvedLocation);
        _handler = handler;
        _updateHandler = updateHandler;
    }

    public ApplicationHandler<?> getHandler() {
        return _handler;
    }

    /** {@inheritDoc} */
    @Override
    public String getPid() {
        return _config.getConfigPid();
    }

    public NotificationBroadcasterSupport getMBeanNotifier() {
        return _config.getMBeanNotifier();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return _config.getName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean getUseJandex() {
        return _config.getUseJandex();
    }

    public String getMBeanName() {
        return _config.getMBeanName();
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        WsResource res = _resolvedLocation.get();
        if (res != null && res.exists()) {
            return res.asFile().getAbsolutePath();
        } else {
            return _config.getLocation();
        }
    }

    /** {@inheritDoc} */
    @Override
    public Container getContainer() {
        return _container.get();
    }

    /** {@inheritDoc} */
    @Override
    public void setContainer(Container container) {
        _container.set(container);
    }

    /** {@inheritDoc} */
    @Override
    public Object getConfigProperty(String propName) {
        return _config.getConfigProperty(propName);
    }

    /** {@inheritDoc} */
    @Override
    public Object getHandlerInfo() {
        return _handlerInfo.get();
    }

    /** {@inheritDoc} */
    @Override
    public void setHandlerInfo(Object handlerInfo) {
        _handlerInfo.set(handlerInfo);
    }

    public void setApplicationMonitoringInformation(ApplicationMonitoringInformation ami) {
        _ami.set(ami);
    }

    public ApplicationMonitoringInformation getApplicationMonitoringInformation() {
        return _ami.get();
    }

    public ApplicationMonitor.UpdateHandler getUpdateHandler() {
        return _updateHandler;
    }
}
