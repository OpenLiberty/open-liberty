/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.ejb.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxrs20.ejb.EJBUtils;

/**
 *
 */
public class EjbProviderProxy {
    /**
     * @param ejbName
     */
    public EjbProviderProxy(Boolean hasEJBExceptionMapper, String ejbName, List<String> localInterfaces, String ejbModuleName) {
        super();
        this.ejbName = ejbName;
        this.ejbLocalInterfaces = localInterfaces;
        this.hasEJBExceptionMapper = hasEJBExceptionMapper;
        this.ejbModuleName = ejbModuleName;
    }

    private final Boolean hasEJBExceptionMapper;
    private final String ejbName;
    private final String ejbModuleName;
    private final List<String> ejbLocalInterfaces;
    private static List<Class<?>> PROVIDER_INTERFACES = Arrays.asList(
                    new Class<?>[] {
                                    ContextResolver.class,
                                    ExceptionMapper.class,
                                    ReaderInterceptor.class,
                                    WriterInterceptor.class,
                                    MessageBodyReader.class,
                                    MessageBodyWriter.class,
                                    ContainerRequestFilter.class,
                                    ContainerResponseFilter.class
                    });

    private String getJndiName(Class<?> providerInterface, Class<?> providerClass) {
        StringBuilder jndiName = new StringBuilder();
        jndiName = (ejbModuleName == null) ? jndiName.append("java:module/")
                        : jndiName.append("java:app/").append(ejbModuleName + "/");
        if (this.ejbName != null) {

            jndiName.append(ejbName);
            if (providerInterface != null) {
                jndiName.append("!").append(providerInterface.getName());
            }

        } else {
            jndiName.append(providerClass.getName());
            if (providerInterface != null) {
                jndiName.append("!").append(providerInterface.getName());
            }
        }
        return jndiName.toString();
    }

    <T> T lookUpProviderEjb(Class<T> providerInterface, Object providerObject) {

        String jndiName = getJndiName(providerInterface, providerObject.getClass());
        Object ejbServiceObject;
        try {
            ejbServiceObject = new InitialContext().lookup(jndiName);
            return (T) ejbServiceObject;
        } catch (Throwable e) {
        }

        return providerInterface.cast(providerObject);
    }

    /**
     * 
     * @param providerObject
     * @param ejbObjects
     * @return
     */
    public Object createEjbProviderObject(Object providerObject) {
        Map<Class<?>, Object> ejbProviderMap = new HashMap<Class<?>, Object>();
        //if ejbName == null, it means there is no EJBEndpoint for this providerObject. Thus, it's not EJB, just return the original instance.
        if (this.ejbName == null)
            return providerObject;
        //session bean doesn't implement any interface or have interface but annotated with @LocalBean
        if (this.ejbLocalInterfaces.size() == 0) {
            Object ejbObject = lookUpProviderEjb(null, providerObject);
            for (Class inter : providerObject.getClass().getInterfaces()) {
                ejbProviderMap.put(inter, ejbObject);
            }

            return Proxy.newProxyInstance(providerObject.getClass().getClassLoader(),
                                          ejbProviderMap.keySet().toArray(new Class<?>[ejbProviderMap.size()]),
                                          new EjbProxyInvocationHandler(this.hasEJBExceptionMapper, providerObject, ejbProviderMap));
//            return ejbObject;
        }
        for (Class<?> providerInterface : PROVIDER_INTERFACES) {

            for (String ejbLocalInterface : this.ejbLocalInterfaces) {
                //session bean implements provider directly, just use provider interface for JNDI lookup
                if (ejbLocalInterface.equals(providerInterface.getName())) {
                    Object ejbObject = lookUpProviderEjb(providerInterface, providerObject);
                    if (ejbObject != providerObject) {
                        ejbProviderMap.put(providerInterface, ejbObject);
                    }
                }
                //session bean implements a customized interface, which extend the provider service. The customized interface is an Local interface.
                else {
                    try {
                        Class ejbLocalInterfaceClazz = providerObject.getClass().getClassLoader().loadClass(ejbLocalInterface);
//                        Class providerInterfaceClazz = providerObject.getClass().getClassLoader().loadClass(providerInterface.getName());
//                        Class ejbLocalInterfaceClazz = Thread.currentThread().getContextClassLoader().loadClass(ejbLocalInterface);
                        if (providerInterface.isAssignableFrom(ejbLocalInterfaceClazz)) {
                            @SuppressWarnings("unchecked")
                            Object ejbObject = lookUpProviderEjb(ejbLocalInterfaceClazz, providerObject);
                            if (ejbObject != providerObject) {
                                ejbProviderMap.put(ejbLocalInterfaceClazz, ejbObject);
                            }
                        }

                    } catch (ClassNotFoundException e) {

                    }

                }
            }

        }

        switch (ejbProviderMap.size()) {
            case 0:
                return providerObject;
//            case 1:
//                return ejbProviderMap.values().iterator().next();
            default: {
                return Proxy.newProxyInstance(providerObject.getClass().getClassLoader(),
                                              ejbProviderMap.keySet().toArray(new Class<?>[ejbProviderMap.size()]),
                                              new EjbProxyInvocationHandler(this.hasEJBExceptionMapper, providerObject, ejbProviderMap));
            }
        }
    }

    class EjbProxyInvocationHandler implements InvocationHandler {

        private final Map<Class<?>, Object> ejbProviderMap;
        private final Object oriProviderObject;
        private final Boolean hasEJBExceptionMapper;

        EjbProxyInvocationHandler(Boolean hasEJBExceptionMapper, Object oriProviderObject, Map<Class<?>, Object> ejbProviderMap) {
            this.ejbProviderMap = ejbProviderMap;
            this.oriProviderObject = oriProviderObject;
            this.hasEJBExceptionMapper = hasEJBExceptionMapper;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        @Override
        @Trivial
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object invokeResult = null;
            for (Entry<Class<?>, Object> entry : ejbProviderMap.entrySet()) {
                Class<?> providerInterface = entry.getKey();
                Object ejbProviderObject = entry.getValue();

                for (Method iMethod : providerInterface.getMethods()) {
                    if (EJBUtils.matchMethod(method, iMethod)) {
                        try {
                            invokeResult = iMethod.invoke(ejbProviderObject, args);
                        } catch (Exception e) {
                            List<Class<?>> exceptionTypes = Arrays.asList(method.getExceptionTypes());

                            Class<? extends Throwable> exceptionClass = e.getCause().getClass();
                            if (this.hasEJBExceptionMapper) {
                                if (EJBException.class.equals(exceptionClass)) {
                                    Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                                    if (causedByException != null && exceptionTypes.contains(causedByException.getClass()))
                                        throw causedByException;
                                }
                            } else {
                                if (EJBException.class.isAssignableFrom(exceptionClass)) {
                                    Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                                    if (causedByException != null)
                                        throw causedByException;
                                }
                            }
                            if (e instanceof InvocationTargetException)
                                throw (Exception) e.getCause();
                            else
                                throw e;
                        }
                        return invokeResult;
                    }
                }
            }

            try {
                invokeResult = method.invoke(oriProviderObject, args);
            } catch (Exception e) {
                List<Class<?>> exceptionTypes = Arrays.asList(method.getExceptionTypes());

                Class<? extends Throwable> exceptionClass = e.getCause().getClass();
                if (this.hasEJBExceptionMapper) {
                    if (EJBException.class.equals(exceptionClass)) {
                        Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                        if (causedByException != null && exceptionTypes.contains(causedByException.getClass()))
                            throw causedByException;
                    }
                } else {
                    if (EJBException.class.isAssignableFrom(exceptionClass)) {
                        Exception causedByException = ((EJBException) e.getCause()).getCausedByException();;
                        if (causedByException != null)
                            throw causedByException;
                    }
                }
                if (e instanceof InvocationTargetException)
                    throw (Exception) e.getCause();
                else
                    throw e;
            }
            return invokeResult;
        }
    }
}
