/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

/**
 * The annotation config implementation for {@code @Asynchronous} on FT 1.0
 */
public class AsynchronousConfigImpl extends AbstractAnnotationConfig<Asynchronous> implements AsynchronousConfig {

    private static final TraceComponent tc = Tr.register(AsynchronousConfigImpl.class);

    public AsynchronousConfigImpl(Class<?> annotatedClass, Asynchronous annotation) {
        super(annotatedClass, annotation, Asynchronous.class);
    }

    public AsynchronousConfigImpl(Method annotatedMethod, Class<?> annotatedClass, Asynchronous annotation) {
        super(annotatedMethod, annotatedClass, annotation, Asynchronous.class);
    }

    /** {@inheritDoc} */
    @Override
    public void validate() {
        Method method = getAnnotatedMethod();
        if (method != null) {
            Class<?> originalMethodReturnType = getAnnotatedMethod().getReturnType();
            if (!(Future.class == originalMethodReturnType)) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "asynchronous.method.not.returning.future.CWMFT5001E", FTDebug.formatMethod(method)));
            }
        }
    }

}
