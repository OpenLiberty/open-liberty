/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import java.lang.reflect.Member;

/**
 * Registered override injection processor can conditionally override some
 * annotations and translate them to their own primary annotations. For
 * example, an processor might override the Resource annotation class based
 * on the target type.
 *
 * <p>Instances of this class must extend {@code InjectionProcessor<A>}.
 *
 * @param <A> the primary annotation class
 * @param <O> the overridden annotation class
 */
public interface OverrideInjectionProcessor<A extends Annotation, O extends Annotation>
{
    /**
     * Returns an injection binding for the primary annotation based on the data
     * in the override annotation, or returns {@code null} if this processor
     * does not want to override the processing of the annotation. This method
     * will only be called if this injection processor does not contain a
     * binding with the specified name.
     *
     * @param annotation the override annotation
     * @param name the name of the injection binding
     * @return the new injection binding to use as an override, or {@code null} to use the normal injection processor
     * @see InjectionProcessor#createInjectionBinding
     */
    InjectionBinding<A> createOverrideInjectionBinding(Class<?> instanceClass,
                                                       Member member,
                                                       O annotation,
                                                       String name)
                    throws InjectionException;

    /**
     * Merges the data from an override annotation into an injection binding for
     * a primary annotation.
     *
     * @param instanceClass the class containing the annotation
     * @param member the member containing the annotation, or null if a
     *            annotation was class-level
     * @param annotation the overridden annotation
     * @param injectionBinding the primary annotation binding
     * @see InjectionBinding#merge
     */
    void mergeOverrideInjectionBinding(Class<?> instanceClass,
                                       Member member,
                                       O annotation,
                                       InjectionBinding<A> injectionBinding)
                    throws InjectionException;
}
