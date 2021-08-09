/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.v21.cdi.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.enterprise.inject.spi.BeanManager;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class IBMHibernateExtendedBeanManager {

    private ClassLoader applicationClassLoader;
    private String earAppId;
    //Used to filter before shutdown events to the correct app. 
    //Before shutdown events have very little data to identify apps, but we can check to see it's the
    //same bean manager as the after bean discovery event, which has more data. 
    //This memory expensive reference is garbage collected along with this entire object after the
    //before shutdown event.
    private BeanManager beanManager; 
    //Actual type is org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener
    private Queue<Object> hibernateLifecycleListeners = new LinkedList<Object>();

    public IBMHibernateExtendedBeanManager(ClassLoader applicationClassLoader, String earAppId){
        this.applicationClassLoader = applicationClassLoader;
        this.earAppId = earAppId;
    }

    //Actual type is org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener
    @FFDCIgnore(ClassNotFoundException.class)
    public void registerLifecycleListener(Object lifecycleListener) {
        Class<?> lifeCycleListenerClass = null;
        try {
            lifeCycleListenerClass = Class.forName("org.hibernate.resource.beans.container.spi.ExtendedBeanManager$LifecycleListener"
                                                   ,true, applicationClassLoader);
        } catch (ClassNotFoundException e) {
            //We're unlikely to reach this as this class is only used if 
            //org.hibernate.resource.beans.container.spi.ExtendedBeanManager is in the classloader 
            throw new IllegalStateException("Failed to find org.hibernate.resource.beans.container.spi.ExtendedBeanManager$LifecycleListener", e);
        }
        
        if (lifeCycleListenerClass.isAssignableFrom(lifecycleListener.getClass())) {
            hibernateLifecycleListeners.add(lifecycleListener);
        } else {
            String errorMsg = "Object " + lifecycleListener + " of class " + lifecycleListener.getClass().getCanonicalName()
                    + " is not instance of org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener";
            throw new IllegalArgumentException(errorMsg);
        }
    }
    
    public void notifyHibernateAfterBeanDiscovery(final String baseClassLoaderId, final BeanManager beanManager) throws SecurityException, IllegalArgumentException {
        if (earAppId.equals(baseClassLoaderId)) {
            this.beanManager = beanManager;
            Iterator<Object> it = hibernateLifecycleListeners.iterator();
            while (it.hasNext()){
                final Object o = it.next();
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            try {
                                Method beanManagerInitialized = o.getClass().getMethod("beanManagerInitialized", BeanManager.class);
                                beanManagerInitialized.setAccessible(true);
                                beanManagerInitialized.invoke(o, beanManager);
                            } catch (NoSuchMethodException e) {
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            }
                            return null;
                        }
                    });    
                } catch (SecurityException e) {
                }
            }
        }
    }

    public boolean notifyHibernateBeforeShutdown(final BeanManager beanManager) throws SecurityException, IllegalArgumentException {
        boolean rightBeanManager = false;
        if (this.beanManager.equals(beanManager)) {
            rightBeanManager = true;
            Iterator<Object> it = hibernateLifecycleListeners.iterator();
            while (it.hasNext()){
                final Object o = it.next();
                try {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            try {
                                Method beforeBeanManagerDestroyed = o.getClass().getMethod("beforeBeanManagerDestroyed", BeanManager.class);
                                beforeBeanManagerDestroyed.setAccessible(true);
                                beforeBeanManagerDestroyed.invoke(o, beanManager);
                            } catch (NoSuchMethodException e) {
                            } catch (IllegalAccessException e) {
                            } catch (InvocationTargetException e) {
                            }
                            return null;
                        }
                    });    
                } catch (SecurityException e) {
                } finally {
                    it.remove();
                }
            }
        }
        return rightBeanManager;
    }
}
