/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class AsynchronousConfig extends AbstractAnnotationConfig<Asynchronous> implements Asynchronous {

    private static final TraceComponent tc = Tr.register(AsynchronousConfig.class);

    public AsynchronousConfig(Class<?> annotatedClass, Asynchronous annotation) {
        super(annotatedClass, annotation, Asynchronous.class);
    }

    public AsynchronousConfig(Method annotatedMethod, Class<?> annotatedClass, Asynchronous annotation) {
        super(annotatedMethod, annotatedClass, annotation, Asynchronous.class);
    }

    /**
     * Validate Asynchronous annotation to make sure all methods with this annotation specified returns a Future.
     * If placed on class-level, all declared methods in this class will need to return a Future.
     *
     * @param method the method to be validated
     *
     */
    @Override
    public void validate() {
        Method method = getAnnotatedMethod();
        if (method != null) {
            Class<?> originalMethodReturnType = getAnnotatedMethod().getReturnType();
            if (!(Future.class.isAssignableFrom(originalMethodReturnType))) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "asynchronous.method.not.returning.future.CWMFT5001E", method));
            }
        }
    }

}
