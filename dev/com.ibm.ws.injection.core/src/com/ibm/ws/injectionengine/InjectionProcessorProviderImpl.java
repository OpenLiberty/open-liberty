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
package com.ibm.ws.injectionengine;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionSimpleProcessor;

public final class InjectionProcessorProviderImpl<A extends Annotation, AS extends Annotation>
                extends InjectionProcessorProvider<A, AS>
{
    private final Class<A> ivAnnotationClass;
    private final Class<? extends InjectionProcessor<A, AS>> ivInjectionProcessorClass;

    /**
     * True if {@link #ivInjectionProcessorClass} is a {@link InjectionSimpleProcessor}.
     */
    private final boolean ivSimple;

    /**
     * True if {@link #ivAnnotationsClass} has been assigned.
     */
    private volatile boolean ivAnnotationsClassChecked;

    /**
     * Lazily initialized return value for {@link #getAnnotationsClass}.
     * Synchronization is managed by {@link #ivAnnotationsClassChecked}.
     */
    private Class<AS> ivAnnotationsClass;

    InjectionProcessorProviderImpl(Class<A> annotationClass,
                                   Class<? extends InjectionProcessor<A, AS>> injectionProcessorClass)
    {
        ivAnnotationClass = annotationClass;
        ivInjectionProcessorClass = injectionProcessorClass;
        ivSimple = InjectionSimpleProcessor.class.isAssignableFrom(injectionProcessorClass);
        ivAnnotationsClassChecked = ivSimple;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + '[' + ivInjectionProcessorClass.getName() + ']';
    }

    @Override
    public Class<A> getAnnotationClass()
    {
        return ivAnnotationClass;
    }

    @Override
    public Class<AS> getAnnotationsClass()
    {
        if (!ivAnnotationsClassChecked)
        {
            // Create a dummy instance to determine the annotations class.
            ivAnnotationsClass = InjectionProcessorContextImpl.getAnnotationsClass(createInjectionProcessor());
            ivAnnotationsClassChecked = true;
        }

        return ivAnnotationsClass;
    }

    @Override
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses()
    {
        return ivSimple ? Collections.<Class<? extends JNDIEnvironmentRef>> emptyList() : null;
    }

    @Override
    public InjectionProcessor<A, AS> createInjectionProcessor()
    {
        try
        {
            return ivInjectionProcessorClass.newInstance();
        } catch (IllegalAccessException ex)
        {
            throw new IllegalStateException(ex);
        } catch (InstantiationException ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
