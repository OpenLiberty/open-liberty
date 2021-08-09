/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.weld.bean.CommonBean;
import org.jboss.weld.construction.api.WeldCreationalContext;
import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.serialization.spi.BeanIdentifier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

/**
 * This is a chain of Interceptors, invoked in turn before finally calling proceed on the delegate InvocationContext
 */
public class InterceptorChain implements InvocationContext {

    private final InvocationContext delegateInvocationContext;
    private final List<Interceptor<?>> interceptors;
    private int nextInterceptor = 0;
    private final BeanManager beanManager;
    private final InterceptionType interceptionType;
    private final WeldCreationalContext<?> creationalContext;
    private final ConcurrentHashMap<BeanIdentifier, Object> activeInterceptors;

    private static final TraceComponent tc = Tr.register(InterceptorChain.class);

    public InterceptorChain(InterceptionType interceptionType,
                            InvocationContext delegateInvocationContext,
                            List<Interceptor<?>> interceptors,
                            BeanManager beanManager,
                            ConcurrentHashMap<BeanIdentifier, Object> activeInterceptors) {
        this.interceptionType = interceptionType;
        this.delegateInvocationContext = delegateInvocationContext;
        this.interceptors = interceptors;
        this.beanManager = beanManager;
        this.activeInterceptors = activeInterceptors;

        ManagedObjectInvocationContext<?> managedObjectInvocationContext = (ManagedObjectInvocationContext<?>) delegateInvocationContext;
        ManagedObjectContext managedObjectContext = managedObjectInvocationContext.getManagedObjectContext();
        this.creationalContext = managedObjectContext.getContextData(WeldCreationalContext.class);

    }

    /** {@inheritDoc} */
    @Override
    public Object getTarget() {
        return delegateInvocationContext.getTarget();
    }

    /** {@inheritDoc} */
    @Override
    public Object getTimer() {
        return delegateInvocationContext.getTimer();
    }

    /** {@inheritDoc} */
    @Override
    public Method getMethod() {
        return delegateInvocationContext.getMethod();
    }

    /** {@inheritDoc} */
    @Override
    public Constructor<?> getConstructor() {
        return delegateInvocationContext.getConstructor();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] getParameters() {
        return delegateInvocationContext.getParameters();
    }

    /** {@inheritDoc} */
    @Override
    public void setParameters(Object[] paramArrayOfObject) {
        delegateInvocationContext.setParameters(paramArrayOfObject);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getContextData() {
        return delegateInvocationContext.getContextData();
    }

    /** {@inheritDoc} */
    @Override
    public Object proceed() throws Exception {
        Object rc = null;

        //if there are more interceptors left in the chain, call the next one
        if (nextInterceptor < interceptors.size()) {
            rc = invokeNextInterceptor();
        }
        else {
            //otherwise call proceed on the delegate InvocationContext
            rc = delegateInvocationContext.proceed();
        }

        return rc;
    }

    private <S> Object invokeNextInterceptor() throws Exception {
        Object rc = null;

        //find the next interceptor in the chain
        Interceptor<S> interceptor = (Interceptor<S>) interceptors.get(nextInterceptor);
        CommonBean<S> commonBean = (CommonBean<S>) interceptor;

        S interceptorInstance = (S) this.activeInterceptors.get(commonBean.getIdentifier());
        if (interceptorInstance == null) {
            //create a contextual instance of the interceptor
            interceptorInstance = (S) beanManager.getReference(interceptor, interceptor.getBeanClass(), creationalContext);
            S previous = (S) this.activeInterceptors.putIfAbsent(commonBean.getIdentifier(), interceptorInstance);
            if (previous != null) {
                interceptorInstance = previous;
            }
        }

        //increment the next counter
        nextInterceptor++;

        //invoke it
        try {
            rc = interceptor.intercept(interceptionType, interceptorInstance, this);
        } catch (WeldException e) {
            Throwable t = e.getCause();
            if (t != null && t instanceof Exception) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unwrapping a WeldException");
                }
                throw (Exception) t;
            } else {
                throw e;
            }
        }

        return rc;
    }
}
