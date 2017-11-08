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

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Inject;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;

import org.jboss.weld.serialization.spi.BeanIdentifier;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.interfaces.WebSphereEjbServices;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * A wrapper interceptor which actually calls a chain of nested CDI interceptors
 */
public class EJBCDIInterceptorWrapper implements Serializable {

    private static final TraceComponent tc = Tr.register(EJBCDIInterceptorWrapper.class);

    @Inject
    BeanManager beanManager;

    private final ConcurrentHashMap<BeanIdentifier, Object> activeInterceptors = new ConcurrentHashMap<BeanIdentifier, Object>();

    @AroundInvoke
    public Object aroundInvoke(InvocationContext invocation) throws Exception {
        Object rc = invokeInterceptors(invocation, InterceptionType.AROUND_INVOKE);
        return rc;
    }

    @AroundTimeout
    public Object aroundTimeout(InvocationContext invocation) throws Exception {
        Object rc = invokeInterceptors(invocation, InterceptionType.AROUND_TIMEOUT);
        return rc;
    }

    @PostConstruct
    public void postConstruct(InvocationContext invocation) {
        try {
            invokeInterceptors(invocation, InterceptionType.POST_CONSTRUCT);
        } catch (Exception e) {
            Tr.error(tc, "lifecycle.interceptor.exception.CWOWB2001E", InterceptionType.POST_CONSTRUCT, e);
            throw new UndeclaredThrowableException(e);
        }
    }

    @PreDestroy
    public void preDestroy(InvocationContext invocation) {
        try {
            invokeInterceptors(invocation, InterceptionType.PRE_DESTROY);
        } catch (Exception e) {
            Tr.error(tc, "lifecycle.interceptor.exception.CWOWB2001E", InterceptionType.PRE_DESTROY, e);
            throw new UndeclaredThrowableException(e);
        }
    }

    @PostActivate
    public void postActivate(InvocationContext invocation) {
        try {
            invokeInterceptors(invocation, InterceptionType.POST_ACTIVATE);
        } catch (Exception e) {
            Tr.error(tc, "lifecycle.interceptor.exception.CWOWB2001E", InterceptionType.POST_ACTIVATE, e);
            throw new UndeclaredThrowableException(e);
        }
    }

    @PrePassivate
    public void prePassivate(InvocationContext invocation) {
        try {
            invokeInterceptors(invocation, InterceptionType.PRE_PASSIVATE);
        } catch (Exception e) {
            Tr.error(tc, "lifecycle.interceptor.exception.CWOWB2001E", InterceptionType.PRE_PASSIVATE, e);
            throw new UndeclaredThrowableException(e);
        }
    }

    @AroundConstruct
    public Object aroundConstruct(InvocationContext invocation) {
        Object rc = null;
        try {
            rc = invokeInterceptors(invocation, InterceptionType.AROUND_CONSTRUCT);
        } catch (Exception e) {
            Tr.error(tc, "lifecycle.interceptor.exception.CWOWB2001E", InterceptionType.AROUND_CONSTRUCT, e);
            throw new UndeclaredThrowableException(e);
        }
        return rc;
    }

    private Object invokeInterceptors(InvocationContext invocation, InterceptionType interceptionType) throws Exception {
        Object rc = null;

        //find the J2EEName of the current ejb
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        J2EEName j2eeName = cmd.getJ2EEName();
        String applicationID = j2eeName.getApplication();

        //find the nested interceptors to be called
        WebSphereEjbServices webSphereEjbServices = AbstractEjbEndpointService._getWebSphereEjbServices(applicationID);
        List<Interceptor<?>> interceptors = webSphereEjbServices.getInterceptors(j2eeName, invocation.getMethod(), interceptionType);

        //put them into a chain
        InterceptorChain chain = new InterceptorChain(interceptionType, invocation, interceptors, beanManager, activeInterceptors);
        //and invoke them
        rc = chain.proceed();

        return rc;
    }
}
