/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.injection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;

import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.ws.kernel.service.util.SecureAction;

/**
 * A proxy that can be injected for {@link HttpServletRequest}s. All method invocations on the wrapped context are handled
 * via {@link java.lang.reflect.Proxy}. This class allows clients to access the original context
 * using {@link ServletRequestWrapper#getRequest()}.
 */
public class HttpServletRequestInjectionProxy extends HttpServletRequestWrapper {

    private static final TraceComponent tc = Tr.register(HttpServletRequestInjectionProxy.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    private static final Class<?> contextClass = HttpServletRequest.class;

    public HttpServletRequestInjectionProxy() {
        super((HttpServletRequest) Proxy.newProxyInstance(priv.getClassLoader(contextClass),
                                                          new Class[] { contextClass },
                                                          new InvocationHandler() {
                                                              @Override
                                                              public Object invoke(Object proxy,
                                                                                   Method method,
                                                                                   Object[] args) throws Throwable {
                                                                  if (tc.isEntryEnabled()) {
                                                                      Tr.entry(tc, "invoke");
                                                                  }
                                                                  Object result;
                                                                  if ("toString".equals(method.getName()) && (method.getParameterTypes().length == 0)) {
                                                                      result = "Injection Proxy for " + contextClass.getName();
                                                                      if (tc.isEntryEnabled()) {
                                                                          Tr.exit(tc, "invoke", result);
                                                                      }
                                                                      return result;
                                                                  }
                                                                  result = method.invoke(getHttpServletRequest(), args);
                                                                  if (tc.isEntryEnabled()) {
                                                                      Tr.exit(tc, "invoke", result);
                                                                  }
                                                                  return result;
                                                              }
                                                          }));
    }

    private static HttpServletRequest getHttpServletRequest() {
        final String methodName = "getHttpServletRequest";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        // use runtimeContext from TLS
        InjectionRuntimeContext runtimeContext = InjectionRuntimeContextHelper.getRuntimeContext();
        // get the real context from the RuntimeContext
        Object context = runtimeContext.getRuntimeCtxObject(contextClass.getName());
        return (HttpServletRequest) context;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Injection Proxy for " + contextClass.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletRequestWrapper#getRequest()
     */
    @Override
    public ServletRequest getRequest() {
        return getHttpServletRequest();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletRequestWrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(@SuppressWarnings("rawtypes") Class wrappedType) {
        if (!ServletRequest.class.isAssignableFrom(wrappedType)) {
            throw new IllegalArgumentException("Given class " +
                                               wrappedType.getName() + " not a subinterface of " +
                                               ServletRequest.class.getName());
        }

        final ServletRequest request = getHttpServletRequest();
        @SuppressWarnings("unchecked")
        final Class<? extends ServletRequest> wrappedServletType = wrappedType;

        if (wrappedServletType.isAssignableFrom(request.getClass())) {
            return true;
        } else if (request instanceof ServletRequestWrapper) {
            return ((ServletRequestWrapper) request).isWrapperFor(wrappedType);
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletRequestWrapper#isWrapperFor(javax.servlet.ServletRequest)
     */
    @Override
    public boolean isWrapperFor(ServletRequest wrapped) {
        final ServletRequest request = getHttpServletRequest();

        if (request == wrapped) {
            return true;
        } else if (request instanceof ServletRequestWrapper) {
            return ((ServletRequestWrapper) request).isWrapperFor(wrapped);
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletRequestWrapper#setRequest(javax.servlet.ServletRequest)
     */
    @Override
    public void setRequest(ServletRequest request) {
        throw new UnsupportedOperationException("ServletRequest may not be set on this proxy");
    }
}
