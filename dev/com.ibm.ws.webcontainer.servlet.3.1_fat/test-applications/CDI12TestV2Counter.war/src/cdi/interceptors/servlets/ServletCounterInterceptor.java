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
package cdi.interceptors.servlets;

import java.io.Serializable;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import cdi.beans.v2.log.ApplicationLog;

/**
 * Part of the CDI interceptor test data.
 * 
 * The metadata-annotation {@link AroundInvoke} makes this type a CDI
 * interceptor. See the method {@link #checkParams(InvocationContext)}, below.
 * 
 * The method
 * 
 * See {@linkplain "http://docs.oracle.com/javaee/6/tutorial/doc/gkhjx.html"}.
 * In particular:
 * 
 * <quote>
 * An interceptor class often contains a method annotated \@AroundInvoke,
 * which specifies the tasks the interceptor will perform when intercepted
 * methods are invoked. It can also contain a method annotated \@PostConstruct,
 * \@PreDestroy, \@PrePassivate, or \@PostActivate, to specify lifecycle callback
 * interceptors, and a method annotated @AroundTimeout, to specify EJB timeout
 * interceptors. An interceptor class can contain more than one interceptor
 * method, but it must have no more than one method of each type.
 * </quote>
 * 
 * Enablement of this interceptor requires a declaration in "beans.xml". In this
 * case:
 * 
 * <pre>
 * &lt;interceptors&gt;
 * &lt;class&gt;cdi.interceptors.servlet.ServletCounterInterceptor&lt;/class&gt;
 * &lt;/interceptors&gt;
 * </pre>
 */
@ServletCounterOperation
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class ServletCounterInterceptor implements Serializable {
    //
    private static final long serialVersionUID = 1L;

    // Logging ...
    public static final String LOG_CLASS_NAME = "ServletServiceInterceptor";

    // Common application log ...
    @Inject
    ApplicationLog applicationLog;

    //

    public ServletCounterInterceptor() {
        // String methodName = "ServletServiceInterceptor";
        // Empty

        // (new Throwable("Dummy: " + LOG_CLASS_NAME + "." + methodName + " [ " + applicationLog + " ]")).printStackTrace(System.out);
    }

    /**
     * Intercepter of {@link ServletCounterOperation}.
     * 
     * @param context Required interceptor parameter. The context of a servlet request
     *            which is being handled.
     * @return Required intercepter return value. For this implementation, the value obtained
     *         from {@link InvocationContext#proceed()}.
     * 
     * @throws Exception Required interceptor thrown exception. Thrown in case of
     *             an error during the interceptor call.
     */
    @AroundInvoke
    public Object logCounter(InvocationContext context) throws Exception {
        String methodName = "logService";

        // (new Throwable("Dummy: " + LOG_CLASS_NAME + "." + methodName + " [ " + applicationLog + " ]")).printStackTrace(System.out);

        applicationLog.log(LOG_CLASS_NAME, methodName, "Counter [ " + context.getMethod().getName() + " ]");

        return context.proceed();
    }
}
