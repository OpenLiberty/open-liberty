/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resources;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;

/**
 * A provider for simple injection processors.
 */
public abstract class InjectionSimpleProcessorProvider<A extends Annotation>
                extends InjectionProcessorProvider<A, Resources>
{
    @Override
    public final Class<Resources> getAnnotationsClass()
    {
        return null;
    }

    @Override
    public final Class<? extends Annotation> getOverrideAnnotationClass()
    {
        return null;
    }

    @Override
    public final List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
    {
        return Collections.emptyList();
    }

    /**
     * Creates a new processor instance. The annotation class passed to the
     * InjectionSimpleProcessor constructor must match the value returned from {@link #getAnnotationClass}.
     */
    @Override
    public abstract InjectionSimpleProcessor<A> createInjectionProcessor();
}
