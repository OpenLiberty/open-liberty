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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProducerFactory;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class IBMHibernateExtendedBeanManager {

    private ClassLoader applicationClassLoader;
    private BeanManager deligateBeanManager = null;
    //Actual type is org.hibernate.resource.beans.container.spi.ExtendedBeanManager.LifecycleListener
    private Set<Object> hibernateLifecycleListeners = new HashSet<Object>(); 

    public IBMHibernateExtendedBeanManager(ClassLoader applicationClassLoader){
        this.applicationClassLoader = applicationClassLoader;
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

    public void setBaseBeanManager(BeanManager deligateBeanManager) {
        this.deligateBeanManager = deligateBeanManager;
    }

    public BeanManager getBaseBeanManager() {
        return deligateBeanManager;
    }
    
    public void notifyHibernateAfterBeanDiscovery(BeanManager deligateBeanManager) throws SecurityException, IllegalArgumentException {
        if (deligateBeanManager != null && deligateBeanManager.equals(this.deligateBeanManager)) {
            for (Object o : hibernateLifecycleListeners) {
                try {
                    Method beanManagerInitialized = o.getClass().getMethod("beanManagerInitialized", BeanManager.class);
                    beanManagerInitialized.invoke(o, deligateBeanManager);
                } catch (NoSuchMethodException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                }
            }
        }
    }

    public boolean notifyHibernateBeforeShutdown(BeanManager deligateBeanManager) throws SecurityException, IllegalArgumentException {
        boolean rightBeanManager = false;
        if (deligateBeanManager != null && deligateBeanManager.equals(this.deligateBeanManager)) {
            rightBeanManager = true;
            Iterator<Object> it = hibernateLifecycleListeners.iterator();
                while (it.hasNext()){
                Object o = it.next();
                try {
                    Method beanManagerInitialized = o.getClass().getMethod("beforeBeanManagerDestroyed", BeanManager.class);
                    beanManagerInitialized.invoke(o, deligateBeanManager);
                } catch (NoSuchMethodException e) {
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                } finally {
                    it.remove();
                }
            }
        }
        return rightBeanManager;
    }
}
