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

import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.injection.metadata.InjectionRuntimeContext;
import com.ibm.ws.kernel.service.util.SecureAction;

/**
 * A proxy that can be injected for {@link HttpServletResponse}s. All method invocations on the wrapped context are handled
 * via {@link java.lang.reflect.Proxy}. This class allows clients to access the original context
 * using {@link ServletResponseWrapper#getResponse()}.
 */
public class HttpServletResponseInjectionProxy extends HttpServletResponseWrapper {

    private static final TraceComponent tc = Tr.register(HttpServletResponseInjectionProxy.class);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());
    private static final Class<?> contextClass = HttpServletResponse.class;

    public HttpServletResponseInjectionProxy() {
        super((HttpServletResponse) Proxy.newProxyInstance(priv.getClassLoader(contextClass),
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
                                                                   result = method.invoke(getHttpServletResponse(), args);
                                                                   if (tc.isEntryEnabled()) {
                                                                       Tr.exit(tc, "invoke", result);
                                                                   }
                                                                   return result;
                                                               }
                                                           }));
    }

    private static HttpServletResponse getHttpServletResponse() {
        final String methodName = "getHttpServletResponse";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        // use runtimeContext from TLS
        InjectionRuntimeContext runtimeContext = InjectionRuntimeContextHelper.getRuntimeContext();
        // get the real context from the RuntimeContext
        Object context = runtimeContext.getRuntimeCtxObject(contextClass.getName());
        return (HttpServletResponse) context;
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
     * @see javax.servlet.ServletResponseWrapper#getResponse()
     */
    @Override
    public ServletResponse getResponse() {
        return getHttpServletResponse();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletResponseWrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor(@SuppressWarnings("rawtypes") Class wrappedType) {
        if (!ServletResponse.class.isAssignableFrom(wrappedType)) {
            throw new IllegalArgumentException("Given class " +
                                               wrappedType.getName() + " not a subinterface of " +
                                               ServletResponse.class.getName());
        }

        final ServletResponse response = getHttpServletResponse();
        @SuppressWarnings("unchecked")
        final Class<? extends ServletResponse> wrappedServletType = wrappedType;

        if (wrappedServletType.isAssignableFrom(response.getClass())) {
            return true;
        } else if (response instanceof ServletResponseWrapper) {
            return ((ServletResponseWrapper) response).isWrapperFor(wrappedType);
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletResponseWrapper#isWrapperFor(javax.servlet.ServletResponse)
     */
    @Override
    public boolean isWrapperFor(ServletResponse wrapped) {
        final ServletResponse response = getHttpServletResponse();

        if (response == wrapped) {
            return true;
        } else if (response instanceof ServletResponseWrapper) {
            return ((ServletResponseWrapper) response).isWrapperFor(wrapped);
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.ServletResponseWrapper#setResponse(javax.servlet.ServletResponse)
     */
    @Override
    public void setResponse(ServletResponse response) {
        throw new UnsupportedOperationException("ServletResponse may not be set on this proxy");
    }
}
