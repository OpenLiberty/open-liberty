/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.io.IOException;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.TabularData;

import org.osgi.jmx.service.cm.ConfigurationAdminMBean;

/**
 * A read only view of ConfigurationAdminMBean
 */
public class ReadOnlyConfigurationAdmin implements ReadOnlyConfigurationAdminMBean, MBeanRegistration, NotificationEmitter {

    final private ConfigurationAdminMBean delegate;

    ReadOnlyConfigurationAdmin(ConfigurationAdminMBean delegate) {
        this.delegate = delegate;
    }

    // Read methods

    public String getBundleLocation(String pid) throws IOException {
        return delegate.getBundleLocation(pid);
    }

    public String[][] getConfigurations(String filter) throws IOException {
        return delegate.getConfigurations(filter);
    }

    public String getFactoryPid(String pid) throws IOException {
        return delegate.getFactoryPid(pid);
    }

    public String getFactoryPidForLocation(String pid, String location) throws IOException {
        return delegate.getFactoryPidForLocation(pid, location);
    }

    public TabularData getProperties(String pid) throws IOException {
        return delegate.getProperties(pid);
    }

    public TabularData getPropertiesForLocation(String pid, String location) throws IOException {
        return delegate.getPropertiesForLocation(pid, location);
    }

    @Override
    public void postDeregister() {
        if (delegate instanceof MBeanRegistration) {
            ((MBeanRegistration) delegate).postDeregister();
        }
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        if (delegate instanceof MBeanRegistration) {
            ((MBeanRegistration) delegate).postRegister(registrationDone);

        }
    }

    @Override
    public void preDeregister() throws Exception {
        if (delegate instanceof MBeanRegistration) {
            ((MBeanRegistration) delegate).preDeregister();
        }

    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        if (delegate instanceof MBeanRegistration) {
            ((MBeanRegistration) delegate).preRegister(server, name);
        }
        return name;
    }

    @Override
    public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
        if (delegate instanceof NotificationEmitter) {
            ((NotificationEmitter) delegate).removeNotificationListener(listener, filter, handback);
        }
    }

    @Override
    public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
        if (delegate instanceof NotificationEmitter) {
            ((NotificationEmitter) delegate).addNotificationListener(listener, filter, handback);
        }
    }

    /** {@inheritDoc} */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        if (delegate instanceof NotificationEmitter) {
            ((NotificationEmitter) delegate).getNotificationInfo();
        }
        return new MBeanNotificationInfo[0];
    }

    @Override
    public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
        if (delegate instanceof NotificationEmitter) {
            ((NotificationEmitter) delegate).removeNotificationListener(listener);
        }
    }
}
