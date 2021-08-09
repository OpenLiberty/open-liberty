/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.interceptors;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;

import javax.interceptor.InvocationContext;

import com.ibm.ejs.util.dopriv.SetAccessiblePrivilegedAction;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class is used to hold the information needed for invoking
 * either an AroundInvoke interceptor method or a lifecycle callback
 * interceptor method. A separate InterceptorProxy instance is created for
 * each possible interceptor method that can be invoked for a given EJB class.
 * <p>
 * The InterceptorProxy objects are created as part of the EJB initialization
 * that occurs when an application is started or when the EJB is first "touched".
 * Since the data in a InterceptorProxy object does not change, the same
 * InterceptorProxy object can be shared multiple EJB instances to call the
 * interceptor method for that EJB instance. To invoke an interceptor method,
 * the {@link #invokeInterceptor(Object, InvocationContext, Object[])} method
 * of this class is used.
 * <p>
 * AroundInvoke methods may be defined in the bean class itself and/or by an
 * interceptor class. They can also be defined in a superclass of the bean class
 * or interceptor class. However, only one AroundInvoke method may be present on
 * a given class. An AroundInvoke method cannot be a business method of the bean.
 * <p>
 * AroundInvoke methods have the following signature:
 * <p>
 * Object <METHOD>(InvocationContext) throws Exception
 * <p>
 * AroundInvoke methods are invoked in the same transaction context and security
 * context as the business method itself is invoked in.
 * <p>
 * PostConstruct, PostActivate, PrePassivate, and PreDestroy are the possible
 * lifecycle callback events. A single lifecycle callback interceptor method
 * may be used to interpose on multiple callback events (e.g. same method handles
 * both PostConstruct and PostActivate events). Lifecycle callback interceptor
 * methods may be defined in the bean class itself and/or by an interceptor class.
 * They can also be defined in a superclass of either the bean class or interceptor
 * class. However, only one interceptor method per lifecycle event method may be
 * present in a given class.
 * <p>
 * Lifecycle callback interceptor methods defined on an interceptor class have
 * the following signature:
 * <p>
 * void <METHOD> (InvocationContext)
 * <p>
 * Lifecycle callback interceptor methods defined on a bean class have the
 * following signature:
 * <p>
 * void <METHOD>()
 * <p>
 * Lifecycle callback interceptor methods are invoked in an unspecified
 * transaction and security context.
 * <p>
 * Method-level interceptor are used to specify business method interceptor methods.
 * If an interceptor class that is used as a method-level interceptor defines
 * lifecycle callback interceptor methods, those lifecycle callback interceptor methods
 * are not invoked. See section 12.4.1.
 */
public class InterceptorProxy {
    private static final String CLASS_NAME = InterceptorProxy.class.getName();

    private static final TraceComponent tc = Tr.register(InterceptorProxy.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Used when invoke an interceptor method that takes no arguments.
     */
    final private static Object[] NO_ARGS = new Object[0];

    /**
     * Method object to use when invoking an a lifecycle callback
     * interceptor method.
     */
    public final Method ivInterceptorMethod; // d367572.9

    /**
     * The index into array of interceptor instances that is passed
     * to the invokeInterceptor method of this class. A value < 0 must
     * be passed as the index if the interceptor method is defined by
     * the EJB class itself.
     */
    final int ivInterceptorIndex; // d451021

    /**
     * True if the interceptor method is a method of the
     * EJB instance itself.
     */
    final boolean ivBeanInterceptor;

    /**
     * True if and only if interceptor method signature has InvocationContext
     * as an argument.
     */
    final boolean ivRequiresInvocationContext; // d451021

    /**
     * Constructor a InterceptorProxy instance.
     *
     * @param m is the java reflection Method to use when the
     *            invokeInterceptor method of this class is called.
     *
     * @param interceptorIndex is the index into array of interceptor instances
     *            that is passed to the invokeInterceptor method of this class.
     *            Note, a value < 0 must be passed as the index if the interceptor
     *            method is defined by the EJB class itself.
     *
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public InterceptorProxy(Method m, int interceptorIndex) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "InterceptorProxy");
        }

        ivInterceptorIndex = interceptorIndex;
        ivInterceptorMethod = m;
        ivBeanInterceptor = (interceptorIndex < 0);
        ivRequiresInvocationContext = m.getParameterTypes().length > 0;

        // Ensure private interceptor methods can be invoked.
        final SetAccessiblePrivilegedAction priviledgedAction = new SetAccessiblePrivilegedAction(); //d446892
        priviledgedAction.setParameters(m, true); //d446892
        try {
            AccessController.doPrivileged(priviledgedAction); //d446892
        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".<init>", "178", this);
            SecurityException ex = (SecurityException) e.getException();
            throw ex;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, toString());
            }
        }
    }

    /**
     * Invoke the interceptor method associated with the interceptor index
     * that was passed as the "interceptorIndex" parameter of the
     * constructor method of this class.
     *
     * @param bean is the EJB instance that is the target of this invocation.
     *
     * @param inv is the InvocationContext to pass as an argument to the
     *            interceptor method if the interceptor method requires it.
     *
     * @param interceptors is the array of interceptor instances created for a
     *            particular EJB instance. The array must be ordered so that the
     *            "interceptorIndex" parameter passed to the constructor method
     *            of this class can be used as an index into this array to
     *            select the correct interceptor instance to invoke.
     *
     * @return Object returned by interceptor instance.
     *
     * @throws Exception
     */
    public final Object invokeInterceptor(Object bean, InvocationContext inv, Object[] interceptors) throws Exception {
        // Interceptor instance is the bean instance itself if the
        // interceptor index is < 0.
        Object interceptorInstance = (ivBeanInterceptor) ? bean : interceptors[ivInterceptorIndex];

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d367572.7
        {
            Tr.debug(tc, "invoking " + this);
            Tr.debug(tc, "interceptor instance = " + interceptorInstance);
        }

        // Does interceptor method require InvocationContext as an argument?
        if (ivRequiresInvocationContext) {
            try {
                // Yes it does, so pass it as an argument.
                Object[] args = new Object[] { inv }; // d404122
                return ivInterceptorMethod.invoke(interceptorInstance, args); // d404122
            } catch (IllegalArgumentException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ivInterceptorMethod: " + ivInterceptorMethod.toString() + " class: " + ivInterceptorMethod.getClass() + " declaring class: "
                                 + ivInterceptorMethod.getDeclaringClass());
                    Tr.debug(tc, "interceptorInstance: " + interceptorInstance.toString() + " class: " + interceptorInstance.getClass());
                }
                throw ie;
            }
        } else {
            // Nope, interceptor method takes no arguments.
            return ivInterceptorMethod.invoke(interceptorInstance, NO_ARGS);
        }
    }

    /**
     * Override of toString to provide better trace information.
     */
    @Override
    public String toString() {
        return "InterceptorProxy(" + ivInterceptorIndex + "): "
               + ivInterceptorMethod.toGenericString();
    }

    /**
     * Get generic string from Method object that is associated
     * with this object.
     *
     * @return generic string that contains the method signature.
     */
    public String getMethodGenericString() // d367572.7
    {
        return ivInterceptorMethod.toGenericString();
    }
}
