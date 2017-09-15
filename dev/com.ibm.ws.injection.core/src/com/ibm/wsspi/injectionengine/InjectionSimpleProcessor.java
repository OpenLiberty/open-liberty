/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
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

import javax.annotation.Resources;

/**
 * The InjectionProcessor superclass for annotations that do not have names,
 * which allows for the following simplifying assumptions:
 *
 * <ul>
 * <li>the annotation must be specified on a field or method, not a class,
 * so no support is required for a plural/container annotation
 * <li>bindings cannot be specified in XML
 * </ul>
 *
 * @see InjectionSimpleBinding
 */
public abstract class InjectionSimpleProcessor<A extends Annotation>
                extends InjectionProcessor<A, Resources>
{
    /**
     * Initialize a processor with the annotation class. An instance of each of
     * the registered subclasses will be created for every component that
     * requires processing. <p>
     *
     * After creating an instance, the injection engine will call {@link #initProcessor()} to complete processor initialization. <p>
     *
     * @param annotationClass the annotation
     */
    public InjectionSimpleProcessor(Class<A> annotationClass)
    {
        super(annotationClass, null);
    }

    /**
     * Does nothing. Simple injection processors do not support references
     * defined in XML.
     */
    @Override
    public final void processXML()
                    throws InjectionException
    {
        // Nothing.
    }

    /**
     * Returns null. Simple injection processors do not support binding, so
     * they do not support JNDI names.
     */
    @Override
    public final String getJndiName(A annotation)
    {
        return null;
    }

    /**
     * Unsupported. Simple injection processors do not support binding, so
     * class-level plural annotations are meaningless.
     */
    @Override
    public final A[] getAnnotations(Resources pluralAnnotation)
    {
        // We passed null for the plural annotation, so this method should never
        // be called.
        throw new UnsupportedOperationException();
    }

    /**
     * Calls {@link #createInjectionBinding(Annotation, Member)}. Simple
     * injection processors do not support JNDI names.
     */
    @Override
    public final InjectionBinding<A> createInjectionBinding(A annotation,
                                                            Class<?> instanceClass,
                                                            Member member,
                                                            String jndiName)
                    throws InjectionException
    {
        return createInjectionBinding(annotation, instanceClass, member);
    }

    /**
     * Returns an annotation-specific InjectionSimpleBinding associated with the
     * specified input annotation. The default implementation returns a new
     * InjectionSimpleBinding.
     *
     * @param annotation the annotation to create a binding for
     * @param instanceClass the class containing the annotation
     * @param member the Field or Method associated with the annotation
     * @throws InjectionException
     */
    public InjectionBinding<A> createInjectionBinding(A annotation, Class<?> instanceClass, Member member)
                    throws InjectionException
    {
        return new InjectionSimpleBinding<A>(annotation, ivNameSpaceConfig); // F50309.3
    }

    /**
     * Unsupported. Simple injection processors do not support class-level
     * annotations.
     */
    @Override
    protected final void validateMissingJndiName(Class<?> instanceClass, A annotation) // F50309.5
    throws InjectionException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation of this method ensures that {@link InjectionBinding#isResolved} has been overridden to return true.
     * Otherwise, a subclass can implement this method as usual. If the
     * implementation needs attributes of the injection target, the {@link InjectionSimpleBinding#getInjectionTarget} method can be used.
     */
    @Override
    public void resolve(InjectionBinding<A> binding)
                    throws InjectionException
    {
        if (!binding.isResolved())
        {
            throw new IllegalStateException();
        }
    }
}
