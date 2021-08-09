/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.internal;

import java.io.ObjectInputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.jmx.service.DelayedMBeanHelper;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerForwarderDelegate;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;

/**
 *
 */
public final class PlatformMBeanServer implements MBeanServer {

    private volatile MBeanServer first;
    private final MBeanServer last;
    private final PlatformMBeanServerDelegate mbServerDelegate;

    PlatformMBeanServer(MBeanServer mbs, PlatformMBeanServerDelegate delegate) {
        first = mbs;
        last = mbs;
        mbServerDelegate = delegate;
    }

    private final MBeanServerPipeline pipeline = new MBeanServerPipeline() {

        @Override
        public synchronized boolean contains(MBeanServerForwarderDelegate filter) {
            if (filter != null) {
                MBeanServerForwarderDelegate next = (first instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) first : null;
                while (next != null) {
                    if (filter == next) {
                        return true;
                    }
                    MBeanServer _next = next.getMBeanServer();
                    next = (_next instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) _next : null;
                }
            }
            return false;
        }

        @Override
        public synchronized boolean insert(MBeanServerForwarderDelegate filter) {
            if (filter != null && !contains(filter)) {
                MBeanServerForwarderDelegate prev = null;
                MBeanServerForwarderDelegate next = (first instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) first : null;
                while (true) {
                    final int nextPriority = (next != null) ? next.getPriority() : 0;
                    if (filter.getPriority() >= nextPriority) {
                        break;
                    }
                    if (next == null) {
                        return false;
                    }
                    prev = next;
                    MBeanServer _next = next.getMBeanServer();
                    next = (_next instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) _next : null;
                }
                if (filter instanceof DelayedMBeanHelper) {
                    DelayedMBeanHelper helper = (DelayedMBeanHelper) filter;
                    helper.setMBeanServerNotificationSupport(mbServerDelegate);
                    mbServerDelegate.addDelayedMBeanHelper(helper);
                }
                filter.setMBeanServer(next != null ? next : last);
                if (prev != null) {
                    prev.setMBeanServer(filter);
                } else {
                    first = filter;
                }
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean remove(MBeanServerForwarderDelegate filter) {
            if (filter != null) {
                MBeanServerForwarderDelegate prev = null;
                MBeanServerForwarderDelegate next = (first instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) first : null;
                while (true) {
                    if (filter == next) {
                        break;
                    }
                    if (next == null) {
                        return false;
                    }
                    prev = next;
                    MBeanServer _next = next.getMBeanServer();
                    next = (_next instanceof MBeanServerForwarderDelegate) ? (MBeanServerForwarderDelegate) _next : null;
                }
                if (filter instanceof DelayedMBeanHelper) {
                    DelayedMBeanHelper helper = (DelayedMBeanHelper) filter;
                    helper.setMBeanServerNotificationSupport(null);
                    mbServerDelegate.removeDelayedMBeanHelper(helper);
                }
                // Do not null the filter's MBeanServer in this method. There might be another thread in
                // the middle of an invocation on the platform MBeanServer and that could result in an NPE
                // if we break this link.
                MBeanServer _next = filter.getMBeanServer();
                if (prev != null) {
                    prev.setMBeanServer(_next);
                } else {
                    first = _next;
                }
                return true;
            }
            return false;
        }
    };

    public void invokePlatformMBeanServerCreated(PlatformMBeanServerBuilderListener listener) {
        listener.platformMBeanServerCreated(pipeline);
    }

    //
    // MBeanServer methods
    //

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        if (System.getSecurityManager() == null) {
            first.addNotificationListener(name, listener, filter, handback);
        } else {
            final ObjectName f_name = name;
            final NotificationListener f_listener = listener;
            final NotificationFilter f_filter = filter;
            final Object f_handback = handback;
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws InstanceNotFoundException {
                        first.addNotificationListener(f_name, f_listener, f_filter, f_handback);
                        return null;

                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) cause;
                }
            }
        }
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException {
        if (System.getSecurityManager() == null) {
            first.addNotificationListener(name, listener, filter, handback);
        } else {
            final ObjectName f_name = name;
            final ObjectName f_listener = listener;
            final NotificationFilter f_filter = filter;
            final Object f_handback = handback;
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws InstanceNotFoundException {
                        first.addNotificationListener(f_name, f_listener, f_filter, f_handback);
                        return null;

                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) cause;
                }
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return first.createMBean(className, name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return first.createMBean(className, name, loaderName);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        return first.createMBean(className, name, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        return first.createMBean(className, name, loaderName, params, signature);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        return first.deserialize(name, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        return first.deserialize(className, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
        return first.deserialize(className, loaderName, data);
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        if (System.getSecurityManager() == null) {
            return first.getAttribute(name, attribute);
        } else {
            final ObjectName f_name = name;
            final String f_attribute = attribute;
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
                        return first.getAttribute(f_name, f_attribute);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) cause;
                } else if (cause instanceof MBeanException) {
                    throw (MBeanException) cause;
                } else if (cause instanceof ReflectionException) {
                    throw (ReflectionException) cause;
                } else if (cause instanceof AttributeNotFoundException) {
                    throw (AttributeNotFoundException) cause;
                }
                return null;
            }

        }
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        return first.getAttributes(name, attributes);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        return first.getClassLoader(loaderName);
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        if (System.getSecurityManager() == null) {
            return first.getClassLoaderFor(mbeanName);
        } else {
            final ObjectName f_mbeanName = mbeanName;
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
                    @Override
                    public ClassLoader run() throws InstanceNotFoundException {

                        return first.getClassLoaderFor(f_mbeanName);

                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) cause;
                }
            }
            return null;
        }
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        return first.getClassLoaderRepository();
    }

    @Override
    public String getDefaultDomain() {
        return first.getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        return first.getDomains();
    }

    @Override
    public Integer getMBeanCount() {
        return first.getMBeanCount();
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return first.getMBeanInfo(name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        return first.getObjectInstance(name);
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        return first.instantiate(className);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return first.instantiate(className, loaderName);
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        return first.instantiate(className, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        return first.instantiate(className, loaderName, params, signature);
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        if (System.getSecurityManager() == null) {
            return first.invoke(name, operationName, params, signature);

        } else {
            final ObjectName f_name = name;
            final String f_operationName = operationName;
            final Object[] f_params = params;
            final String[] f_signature = signature;
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws InstanceNotFoundException, MBeanException, ReflectionException {
                        return first.invoke(f_name, f_operationName, f_params, f_signature);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InstanceNotFoundException) {
                    throw (InstanceNotFoundException) cause;
                } else if (cause instanceof MBeanException) {
                    throw (MBeanException) cause;
                } else if (cause instanceof ReflectionException) {
                    throw (ReflectionException) cause;
                }
                return null;
            }

        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        return first.isInstanceOf(name, className);
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        return first.isRegistered(name);
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        if (System.getSecurityManager() == null) {
            return first.queryMBeans(name, query);
        } else {
            final ObjectName f_name = name;
            final QueryExp f_query = query;
            return AccessController.doPrivileged(new PrivilegedAction<Set<ObjectInstance>>() {
                @Override
                public Set<ObjectInstance> run() {
                    return first.queryMBeans(f_name, f_query);
                }
            });
        }
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        return first.queryNames(name, query);
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        return first.registerMBean(object, name);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        first.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        first.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        first.removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        first.removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        first.setAttribute(name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        return first.setAttributes(name, attributes);
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        first.unregisterMBean(name);
    }
}
