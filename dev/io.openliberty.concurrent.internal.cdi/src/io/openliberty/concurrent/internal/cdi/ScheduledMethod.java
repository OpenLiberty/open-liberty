/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletionStage;

import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance.Handle;
import jakarta.enterprise.inject.spi.CDI;

/**
 * A task that can be scheduled to run a method at the appropriate time,
 * according to the method's Schedule annotation.
 */
public class ScheduledMethod<T> extends ScheduledMethodAbstract {
    private final Class<T> beanClass;
    private final Annotation[] beanQualifierAnnos;

    /**
     * Constructor for Schedule directly annotating a bean method.
     * This constructor also schedules the first execution.
     *
     * @param method            the bean method annotated with a Schedule
     * @param schedule          the Schedule annotation
     * @param contextDescriptor captured thread context
     * @param managedExecutor   managed executor or managed scheduled executor
     * @param beanAnnos         qualifier annotations on the bean class (if any)
     */
    ScheduledMethod(Method method,
                    Schedule schedule,
                    ThreadContextDescriptor contextDescriptor,
                    WSManagedExecutorService managedExecutor,
                    Class<T> beanClass,
                    Annotation... beanQualifierAnnos) {
        super(method, //
              contextDescriptor, //
              managedExecutor, //
              List.of(ScheduleCronTrigger.create(schedule)), //
              List.of(schedule.skipIfLateBy()));

        this.beanClass = beanClass;
        this.beanQualifierAnnos = beanQualifierAnnos;
    }

    /**
     * Invokes the bean method that is annotated Schedule.
     *
     * @return completion stage that is returned by the bean method, otherwise null
     * @throws InvocationTargetException if the bean method raises a declared
     *                                       exception
     * @throws Exception                 if another error occurs attempting to
     *                                       invoke the bean method
     */
    @Override
    protected CompletionStage<?> invokeMethod() //
                    throws InvocationTargetException, Exception {

        Handle<T> handle = CDI.current() //
                        .select(beanClass, beanQualifierAnnos) //
                        .getHandle();
        boolean isDependent = Dependent.class.equals(handle.getBean().getScope());

        Object result;
        try {
            result = method.invoke(handle.get());
        } finally {
            if (isDependent)
                handle.destroy();
        }

        if (result instanceof CompletionStage<?> cs)
            return cs;
        else if (result == null)
            return null;
        else
            throw new ClassCastException("The " + method.getName() + " method of the " +
                                         beanClass.getName() +
                                         " managed bean class is annotated " +
                                         "Schedule, but has the return type: " +
                                         method.getReturnType().getName() +
                                         ". Managed bean methods annotated Schedule" +
                                         " must have one of the following return types: " +
                                         "void, CompletionStage, CompletableFuture"); // TODO NLS
    }
}
