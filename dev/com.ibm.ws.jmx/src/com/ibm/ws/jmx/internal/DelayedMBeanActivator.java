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

import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.jmx.service.DelayedMBeanHelper;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerForwarderDelegate;
import com.ibm.ws.kernel.boot.jmx.service.MBeanServerNotificationSupport;

final class DelayedMBeanActivator extends MBeanServerForwarderDelegate implements DelayedMBeanHelper {

    public static final String MBEAN_CLASSES = "com.ibm.ws.jmx.delayed.MBeanClasses";

    private volatile MBeanServerNotificationSupport notificationSupport;
    private final ConcurrentHashMap<ObjectName, DelayedMBeanHolder> delayedMBeanMap;
    private final BundleContext bundleContext;

    DelayedMBeanActivator(BundleContext ctx) {
        delayedMBeanMap = new ConcurrentHashMap<ObjectName, DelayedMBeanHolder>();
        this.bundleContext = ctx;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    //
    // Support for delayed MBean registration.
    //

    @Override
    public boolean isDelayedMBean(ObjectName name) {
        return delayedMBeanMap.containsKey(name);
    }

    @Override
    public void setMBeanServerNotificationSupport(MBeanServerNotificationSupport support) {
        notificationSupport = support;
    }

    boolean registerDelayedMBean(ServiceReference<?> ref,
                                 ObjectName objectName) {
        if (delayedMBeanMap.putIfAbsent(objectName, new DelayedMBeanHolder(ref)) == null) {
            final MBeanServerNotificationSupport _notificationSupport = notificationSupport;
            if (_notificationSupport != null) {
                _notificationSupport.sendRegisterNotification(objectName);
            }
            return true;
        }
        return false;
    }

    //
    // Internal registration / unregistration methods for delayed MBeans.
    //

    private void registerDelayedMBeans() {
        for (ObjectName name : delayedMBeanMap.keySet()) {
            try {
                registerMBeanIfDelayed(name);
            } catch (InstanceNotFoundException e) {
                //TODO log appropriate message??
            } catch (NotCompliantMBeanException e) {
                //TODO log appropriate message??
            } catch (InstanceAlreadyExistsException e) {
                //TODO log appropriate message??
            } catch (MBeanRegistrationException e) {
                //TODO log appropriate message??
            }
        }
    }

    private void registerMBeanIfDelayed(ObjectName name) throws InstanceNotFoundException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        if (name != null) {
            DelayedMBeanHolder mBeanHolder = delayedMBeanMap.get(name);
            if (mBeanHolder != null) {
                registerMBeanIfDelayed(mBeanHolder, name);
            }
        }
    }

    private void registerMBeanIfDelayed(DelayedMBeanHolder mBeanHolder,
                                        ObjectName name) throws InstanceNotFoundException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        Object mBean = bundleContext.getService(mBeanHolder.getRef());
        if (mBean != null) {
            DelayedMBeanRegistrationState state = mBeanHolder.registrationState.get();
            // This ensures that only one thread does the registration. Others will wait until it is done.
            if (state == DelayedMBeanRegistrationState.DELAYED &&
                mBeanHolder.registrationState.compareAndSet(state, DelayedMBeanRegistrationState.PROCESSING)) {
                boolean registered = false;
                try {
                    mBean = MBeanUtil.getRegisterableMBean(mBeanHolder.getRef(), mBean);
                    if (mBean != null) {
                        @SuppressWarnings("unused")
                        ObjectInstance oi = super.registerMBean(mBean, name);
                        registered = true;
                    }
                } finally {
                    // We're done with this delayed MBean holder. Remove it from the map.
                    if (delayedMBeanMap.remove(name) == null) {
                        // Should never happen.
                        // TODO: trace?
                    }
                    mBeanHolder.registrationState.set(registered ? DelayedMBeanRegistrationState.REGISTERED : DelayedMBeanRegistrationState.UNREGISTERED);
                    mBeanHolder.processingCompleteSignal.countDown();
                    if (!registered) {
                        final MBeanServerNotificationSupport _notificationSupport = notificationSupport;
                        if (_notificationSupport != null) {
                            _notificationSupport.sendUnregisterNotification(name);
                        }
                    }
                }
                return;
            }
            //this thread did not win on registering the mbean.  Unget the service
            bundleContext.ungetService(mBeanHolder.getRef());
            // Value changed or is not "delayed". Retrieve it again.
            state = mBeanHolder.registrationState.get();
            if (state == DelayedMBeanRegistrationState.PROCESSING) {
                // Another thread is currently processing this MBean.
                // Wait until it's done so that we can find it in the main MBeanServer if it was registered.
                waitForProcessingToComplete(mBeanHolder);
            }
        } else {
            throw new InstanceNotFoundException(); //TODO appropriate message
        }
    }

    private boolean unregisterMBeanIfDelayed(ObjectName name) {
        if (name != null) {
            DelayedMBeanHolder mBeanHolder = delayedMBeanMap.get(name);
            if (mBeanHolder != null) {
                return unregisterMBeanIfDelayed(mBeanHolder, name);
            }
        }
        return false;
    }

    private boolean unregisterMBeanIfDelayed(DelayedMBeanHolder mBeanHolder, ObjectName name) {
        DelayedMBeanRegistrationState state = mBeanHolder.registrationState.get();

        // This ensures that only one thread does the unregistration.
        if (state == DelayedMBeanRegistrationState.DELAYED &&
            mBeanHolder.registrationState.compareAndSet(state, DelayedMBeanRegistrationState.PROCESSING)) {
            // We're done with this delayed MBean holder. Remove it from the map.
            if (delayedMBeanMap.remove(name) == null) {
                // Should never happen.
                // TODO: trace?
            }
            mBeanHolder.registrationState.set(DelayedMBeanRegistrationState.UNREGISTERED);
            mBeanHolder.processingCompleteSignal.countDown();
            final MBeanServerNotificationSupport _notificationSupport = notificationSupport;
            if (_notificationSupport != null) {
                _notificationSupport.sendUnregisterNotification(name);
            }
            return true;
        }

        // Value changed or is not "delayed". Retrieve it again.
        state = mBeanHolder.registrationState.get();
        if (state == DelayedMBeanRegistrationState.PROCESSING) {
            // Another thread is currently processing this MBean.
            // Wait until it's done so that we can remove it from the MBeanServer if it was registered.
            waitForProcessingToComplete(mBeanHolder);
        }
        return false;
    }

    @FFDCIgnore(InterruptedException.class)
    private void waitForProcessingToComplete(DelayedMBeanHolder mBeanHolder) {
        boolean done = false;
        do {
            try {
                mBeanHolder.processingCompleteSignal.await();
                done = true;
            } catch (InterruptedException e) {
            }
        } while (!done);
    }

    //
    // MBeanServer methods
    //

    @Override
    public void addNotificationListener(ObjectName name,
                                        NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback) throws InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener,
                                        NotificationFilter filter,
                                        Object handback) throws InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(name);
            registerMBeanIfDelayed(listener);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.addNotificationListener(name, listener, filter, handback);
    }

    @Override
    public ObjectInstance createMBean(String className,
                                      ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        try {
            registerMBeanIfDelayed(name);
        } catch (InstanceNotFoundException e) {
            throw new InstanceAlreadyExistsException();//TODOD appropriate message
        }
        return super.createMBean(className, name);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        registerMBeanIfDelayed(name);
        registerMBeanIfDelayed(loaderName);
        return super.createMBean(className, name, loaderName);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name,
                                      Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException {
        try {
            registerMBeanIfDelayed(name);
        } catch (InstanceNotFoundException e) {
            throw new InstanceAlreadyExistsException();//TODOD appropriate message
        }
        return super.createMBean(className, name, params, signature);
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name,
                                      ObjectName loaderName, Object[] params,
                                      String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        registerMBeanIfDelayed(name);
        registerMBeanIfDelayed(loaderName);
        return super.createMBean(className, name, loaderName, params, signature);
    }

    @Override
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws InstanceNotFoundException, OperationsException {
        try {
            registerMBeanIfDelayed(name);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.deserialize(name, data);
    }

    @Override
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        registerDelayedMBeans();
        return super.deserialize(className, data);
    }

    @Override
    public ObjectInputStream deserialize(String className,
                                         ObjectName loaderName, byte[] data) throws InstanceNotFoundException, OperationsException, ReflectionException {
        try {
            registerMBeanIfDelayed(loaderName);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.deserialize(className, loaderName, data);
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        }
        return super.getAttribute(name, attribute);
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.getAttributes(name, attributes);
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(loaderName);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.getClassLoader(loaderName);
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(mbeanName);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.getClassLoaderFor(mbeanName);
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        // NOTE: As long as we do not create "ClassLoader MBeans" and use the delayed registration
        // mechanism for adding them to the MBeanServer we can avoid registering delayed MBeans here.
        return super.getClassLoaderRepository();
    }

    @Override
    public String getDefaultDomain() {
        return super.getDefaultDomain();
    }

    @Override
    public String[] getDomains() {
        // Due to the asynchronous behaviour of this implementation the value
        // returned is a best estimate of the domains of the MBeans currently
        // registered with the server. Even when we're able to return an
        // accurate result the state of the server may change before the
        // caller does anything with the return value.
        final String[] domains = super.getDomains();
        final Iterator<Entry<ObjectName, DelayedMBeanHolder>> entries = delayedMBeanMap.entrySet().iterator();
        if (!entries.hasNext()) {
            return domains;
        }
        Set<String> _domains = domains != null ? new HashSet<String>(Arrays.asList(domains)) : new HashSet<String>();
        do {
            Entry<ObjectName, DelayedMBeanHolder> entry = entries.next();
            if (entry.getValue().registrationState.get() != DelayedMBeanRegistrationState.UNREGISTERED) {
                _domains.add(entry.getKey().getDomain());
            }
        } while (entries.hasNext());
        return _domains.toArray(new String[_domains.size()]);
    }

    @Override
    public Integer getMBeanCount() {
        // Due to the asynchronous behaviour of this implementation the value
        // returned is a best estimate of the number of MBeans currently
        // registered with the server. Even when we're able to return an
        // accurate result the state of the server may change before the
        // caller does anything with the return value.
        final Integer mBeanCount = super.getMBeanCount();
        final Iterator<DelayedMBeanHolder> mBeanHolders = delayedMBeanMap.values().iterator();
        if (!mBeanHolders.hasNext()) {
            return mBeanCount;
        }
        int _mBeanCount = mBeanCount != null ? mBeanCount.intValue() : 0;
        do {
            if (mBeanHolders.next().registrationState.get() != DelayedMBeanRegistrationState.UNREGISTERED) {
                ++_mBeanCount;
            }
        } while (mBeanHolders.hasNext());
        return Integer.valueOf(_mBeanCount);
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.getMBeanInfo(name);
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.getObjectInstance(name);
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        registerDelayedMBeans();
        return super.instantiate(className);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(loaderName);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        }
        return super.instantiate(className, loaderName);
    }

    @Override
    public Object instantiate(String className, Object[] params,
                              String[] signature) throws ReflectionException, MBeanException {
        registerDelayedMBeans();
        return super.instantiate(className, params, signature);
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName,
                              Object[] params, String[] signature) throws ReflectionException, MBeanException, InstanceNotFoundException {
        try {
            registerMBeanIfDelayed(loaderName);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        }
        return super.instantiate(className, loaderName, params, signature);
    }

    @Override
    public Object invoke(ObjectName name, String operationName,
                         Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        }
        return super.invoke(name, operationName, params, signature);
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        DelayedMBeanHolder mBeanHolder = delayedMBeanMap.get(name);
        if (mBeanHolder != null) {
            Object mbeanClassesObj = mBeanHolder.getRef().getProperty(MBEAN_CLASSES);

            if (mbeanClassesObj != null) {
                String[] mbeanClasses = mbeanClassesObj instanceof String ? new String[] { (String) mbeanClassesObj } : (String[]) mbeanClassesObj;
                if (checkTypeMatch(className, (String[]) mBeanHolder.getRef().getProperty(Constants.OBJECTCLASS)) ||
                    checkTypeMatch(className, mbeanClasses)) {
                    return true;
                }
                //mbeanClasses should have the mbean implementation class and the class name from mbeanInfo.  Perform the check
                //specified in javadoc.
                for (String implClassName : mbeanClasses) {
                    if (checkInheritance(className, implClassName, mBeanHolder.getRef().getBundle())) {
                        return true;
                    }
                }
                return false;
            }
            try {
                registerMBeanIfDelayed(name);
            } catch (NotCompliantMBeanException e) {
                throw newInstanceNotFoundException(e);
            } catch (InstanceAlreadyExistsException e) {
                throw newInstanceNotFoundException(e);
            } catch (MBeanRegistrationException e) {
                throw newInstanceNotFoundException(e);
            }
        }
        return super.isInstanceOf(name, className);
    }

    /**
     * @param className
     * @param property
     * @return
     */
    private boolean checkTypeMatch(String className, String[] classNames) {
        for (String test : classNames) {
            if (test.equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param type
     * @param className
     * @return
     */
    @FFDCIgnore(Exception.class)
    private boolean checkInheritance(String className, String test, Bundle b) {
        try {
            Class<?> clazz = b.loadClass(className);
            Class<?> testClazz = b.loadClass(test);
            return clazz.isAssignableFrom(testClazz);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param e
     * @return
     */
    private InstanceNotFoundException newInstanceNotFoundException(Exception e) {
        InstanceNotFoundException e2 = new InstanceNotFoundException();
        e2.initCause(e);
        return e2;
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        DelayedMBeanHolder mBeanHolder = delayedMBeanMap.get(name);
        //TODO shouldn't this wait for the latch if state is processing?
        if (mBeanHolder != null &&
            mBeanHolder.registrationState.get() != DelayedMBeanRegistrationState.UNREGISTERED) {
            return true;
        }
        return super.isRegistered(name);
    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        // REVISIT: It should be possible to answer this query without registering all the delayed MBeans.
        registerDelayedMBeans();
        return super.queryMBeans(name, query);
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Set<ObjectName> names = super.queryNames(name, query);
        final Iterator<Entry<ObjectName, DelayedMBeanHolder>> entries = delayedMBeanMap.entrySet().iterator();
        if (!entries.hasNext()) {
            return names;
        }
        if (query != null) {
            query.setMBeanServer(this);
        }
        // If the the Set returned from super.queryNames() is not a HashSet it might be read-only. Copying it just in case.
        if (!(names instanceof HashSet<?>)) {
            names = (names != null) ? new HashSet<ObjectName>(names) : new HashSet<ObjectName>();
        }
        do {
            Entry<ObjectName, DelayedMBeanHolder> entry = entries.next();
            if (entry.getValue().registrationState.get() != DelayedMBeanRegistrationState.UNREGISTERED) {
                final ObjectName currentName = entry.getKey();
                // Ignore if the name is somehow already in the set.
                if (names.contains(currentName)) {
                    continue;
                }
                // Filter on the ObjectName pattern.
                if (name != null && !name.apply(currentName)) {
                    continue;
                }
                // Filter on the QueryExp.
                if (query != null) {
                    try {
                        if (!query.apply(currentName)) {
                            continue;
                        }
                    } catch (Exception e) {
                        // TODO: Exceptions thrown from a QueryExp are not meant to reach user code,
                        // but need to verify what an MBeanServer is supposed to do when the query
                        // fails in this way. Excluding the ObjectName from the result for now.
                        continue;
                    }
                }
                // This ObjectName got through all the filters. Add it to the set.
                names.add(currentName);
            }
        } while (entries.hasNext());
        return names;
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        try {
            registerMBeanIfDelayed(name);
        } catch (InstanceNotFoundException e) {
            throw new InstanceAlreadyExistsException();//TODOD appropriate message
        }
        return super.registerMBean(object, name);
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            registerMBeanIfDelayed(name);
            registerMBeanIfDelayed(listener);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.removeNotificationListener(name, listener);
    }

    @Override
    public void removeNotificationListener(ObjectName name,
                                           ObjectName listener,
                                           NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            registerMBeanIfDelayed(name);
            registerMBeanIfDelayed(listener);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void removeNotificationListener(ObjectName name,
                                           NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        super.removeNotificationListener(name, listener, filter, handback);
    }

    @Override
    public void setAttribute(ObjectName name,
                             Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        }
        super.setAttribute(name, attribute);
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
        try {
            registerMBeanIfDelayed(name);
        } catch (NotCompliantMBeanException e) {
            newInstanceNotFoundException(e);
        } catch (InstanceAlreadyExistsException e) {
            newInstanceNotFoundException(e);
        } catch (MBeanRegistrationException e) {
            newInstanceNotFoundException(e);
        }
        return super.setAttributes(name, attributes);
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        if (!unregisterMBeanIfDelayed(name)) {
            super.unregisterMBean(name);
        }
    }
}
