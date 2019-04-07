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
package com.ibm.ws.microprofile.faulttolerance.cdi.config.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.utils.FTDebug;

/**
 * The annotation config implementation for {@code @Asynchronous} on FT 2.0
 * <p>
 * This implementation allows a method to return a {@link CompletionStage}
 */
public class AsynchronousConfigImpl20 extends AbstractAnnotationConfig<Annotation> implements AsynchronousConfig {

    private static final TraceComponent tc = Tr.register(AsynchronousConfigImpl20.class);

    public AsynchronousConfigImpl20(Class<?> annotatedClass, Annotation annotation) {
        super(annotatedClass, annotation, Annotation.class);
    }

    public AsynchronousConfigImpl20(Method annotatedMethod, Class<?> annotatedClass, Annotation annotation) {
        super(annotatedMethod, annotatedClass, annotation, Annotation.class);
    }

    @Override
    public void validate() {
        Method method = getAnnotatedMethod();
        if (method != null) {
            Class<?> originalMethodReturnType = getAnnotatedMethod().getReturnType();
            if (Future.class != originalMethodReturnType
                && CompletionStage.class != originalMethodReturnType) {
                throw new FaultToleranceDefinitionException(Tr.formatMessage(tc, "asynchronous.method.not.returning.future.completionstage.CWMFT5020E",
                                                                             FTDebug.formatMethod(method)));
            }
        }
    }

}
