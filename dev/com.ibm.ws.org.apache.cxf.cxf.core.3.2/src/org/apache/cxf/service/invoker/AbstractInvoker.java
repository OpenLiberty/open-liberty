/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.service.invoker;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Abstract implementation of Invoker.
 * <p>
 */
public abstract class AbstractInvoker implements Invoker {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractInvoker.class);

    public Object invoke(Exchange exchange, Object o) {

        final Object serviceObject = getServiceObject(exchange);
        try {

            BindingOperationInfo bop = exchange.getBindingOperationInfo();
            // Liberty Change: exchange.getService() causes an NPE since it returns null when an EJB service is being invoked. 
            // Changed to exchange.get(Service.class)
            MethodDispatcher md = (MethodDispatcher) exchange.get(Service.class).get(MethodDispatcher.class.getName());
            // End Liberty Change
            Method m = bop == null ? null : md.getMethod(bop);
            if (m == null && bop == null) {
                LOG.severe(new Message("MISSING_BINDING_OPERATION", LOG).toString());
                throw new Fault(new Message("EXCEPTION_INVOKING_OBJECT", LOG,
                                             "No binding operation info", "unknown method", "unknown"));
            }
            List<Object> params = null;
            if (o instanceof List) {
                params = CastUtils.cast((List<?>)o);
            } else if (o != null) {
                params = new MessageContentsList(o);
            }
            
            // Liberty Change: since serviceObject.getClass() throws a NPE changed to get class from the exchange instead
            m = adjustMethodAndParams(m, exchange, params, exchange.get(Service.class).getClass());
            // End Liberty Change

            //Method m = (Method)bop.getOperationInfo().getProperty(Method.class.getName());
            m = matchMethod(m, serviceObject);


            return invoke(exchange, serviceObject, m, params);
        } finally {
            releaseServiceObject(exchange, serviceObject);
        }
    }

    protected Method adjustMethodAndParams(Method m,
                                           Exchange ex,
                                           List<Object> params, Class<?> serviceObjectClass) {
        //nothing to do
        return m;
    }
    
    protected Object invoke(Exchange exchange, final Object serviceObject, Method m, List<Object> params) {
        Object res;
        try {
            Object[] paramArray = new Object[]{};
            if (params != null) {
                paramArray = params.toArray();
            }

            res = performInvocation(exchange, serviceObject, m, paramArray);

            if (exchange.isOneWay()) {
                return null;
            }

            return new MessageContentsList(res);
        } catch (InvocationTargetException e) {

            Throwable t = e.getCause();

            if (t == null) {
                t = e;
            }

            checkSuspendedInvocation(exchange, serviceObject, m, params, t);

            exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);


            for (Class<?> cl : m.getExceptionTypes()) {
                if (cl.isInstance(t)) {
                    exchange.getInMessage().put(FaultMode.class,
                                                FaultMode.CHECKED_APPLICATION_FAULT);
                }
            }

            if (t instanceof Fault) {
                exchange.getInMessage().put(FaultMode.class,
                                            FaultMode.CHECKED_APPLICATION_FAULT);
                throw (Fault)t;
            }
            throw createFault(t, m, params, true);
        } catch (SuspendedInvocationException suspendedEx) {
            // to avoid duplicating the same log statement
            checkSuspendedInvocation(exchange, serviceObject, m, params, suspendedEx);
            // unreachable
            throw suspendedEx;
        } catch (Fault f) {
            exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);
            throw f;
        } catch (Exception e) {
            checkSuspendedInvocation(exchange, serviceObject, m, params, e);
            exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);
            throw createFault(e, m, params, false);
        }
    }

    protected void checkSuspendedInvocation(Exchange exchange,
                                            Object serviceObject,
                                            Method m,
                                            List<Object> params,
                                            Throwable t) {
        if (t instanceof SuspendedInvocationException) {

            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "SUSPENDED_INVOCATION_EXCEPTION",
                        new Object[]{serviceObject, m.toString(), params});
            }
            throw (SuspendedInvocationException)t;
        }
    }

    protected Fault createFault(Throwable ex, Method m, List<Object> params, boolean checked) {

        if (checked) {
            return new Fault(ex);
        }
        String message = (ex == null) ? "" : ex.getMessage();
        String method = (m == null) ? "<null>" : m.toString();
        return new Fault(new Message("EXCEPTION_INVOKING_OBJECT", LOG,
                                     message, method, params),
                                     ex);
    }

    protected Object performInvocation(Exchange exchange, final Object serviceObject, Method m,
                                       Object[] paramArray) throws Exception {
        paramArray = insertExchange(m, paramArray, exchange);
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "INVOKING_METHOD", new Object[] {serviceObject,
                                                                  m,
                                                                  Arrays.asList(paramArray)});
        }
        return m.invoke(serviceObject, paramArray);
    }

    public Object[] insertExchange(Method method, Object[] params, Exchange context) {
        Object[] newParams = params;
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            if (method.getParameterTypes()[i].equals(Exchange.class)) {
                newParams = new Object[params.length + 1];

                for (int j = 0; j < newParams.length; j++) {
                    if (j == i) {
                        newParams[j] = context;
                    } else if (j > i) {
                        newParams[j] = params[j - 1];
                    } else {
                        newParams[j] = params[j];
                    }
                }
            }
        }
        return newParams;
    }

    /**
     * Creates and returns a service object depending on the scope.
     */
    public abstract Object getServiceObject(Exchange context);

    /**
     * Called when the invoker is done with the object.   Default implementation
     * does nothing.
     * @param context
     * @param obj
     */
    public void releaseServiceObject(final Exchange context, Object obj) {
    }

    /**
     * Returns a Method that has the same declaring class as the class of
     * targetObject to avoid the IllegalArgumentException when invoking the
     * method on the target object. The methodToMatch will be returned if the
     * targetObject doesn't have a similar method.
     *
     * @param methodToMatch The method to be used when finding a matching method
     *            in targetObject
     * @param targetObject The object to search in for the method.
     * @return The methodToMatch if no such method exist in the class of
     *         targetObject; otherwise, a method from the class of targetObject
     *         matching the matchToMethod method.
     */
    private static Method matchMethod(Method methodToMatch, Object targetObject) {
        if (isJdkDynamicProxy(targetObject)) {
            for (Class<?> iface : targetObject.getClass().getInterfaces()) {
                Method m = getMostSpecificMethod(methodToMatch, iface);
                if (!methodToMatch.equals(m)) {
	            LOG.fine("matchMethod: Returning method: " + m.getName());
                    return m;
                }
            }
        }
        return methodToMatch;
    }

    /**
     * Return whether the given object is a J2SE dynamic proxy.
     *
     * @param object the object to check
     * @see java.lang.reflect.Proxy#isProxyClass
     */
    public static boolean isJdkDynamicProxy(Object object) {
        return object != null && Proxy.isProxyClass(object.getClass());
    }

    /**
     * Given a method, which may come from an interface, and a targetClass used
     * in the current AOP invocation, find the most specific method if there is
     * one. E.g. the method may be IFoo.bar() and the target class may be
     * DefaultFoo. In this case, the method may be DefaultFoo.bar(). This
     * enables attributes on that method to be found.
     *
     * @param method method to be invoked, which may come from an interface
     * @param targetClass target class for the current invocation. May be
     *            <code>null</code> or may not even implement the method.
     * @return the more specific method, or the original method if the
     *         targetClass doesn't specialize it or implement it or is null
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        if (method != null && targetClass != null) {
            try {
                method = targetClass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ex) {
		LOG.fine("getMostSpecificMethod: This is OK, using original method " + ex); // Liberty Change
                // Perhaps the target class doesn't implement this method:
                // that's fine, just use the original method
            }
        }
        return method;
    }
}
