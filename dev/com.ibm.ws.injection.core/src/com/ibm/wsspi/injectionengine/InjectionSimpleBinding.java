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
import java.lang.reflect.Method;

/**
 * The InjectionBinding superclass for annotations that do not have names, which
 * allows for the following simplifying assumptions:
 *
 * <ul>
 * <li>the annotation must be on a field or method, which means the binding
 * object is associated with exactly one injection target (see {@link #getInjectionTarget})
 * <li>merging does not need to be supported
 * <li>binding objects are not bound/serialized into any namespace, so
 * Reference and ObjectFactory are not needed; instead, {@link #isResolved} can be overridden to return true, and
 * {@link #getInjectionObjectInstance(Object, InjectionTargetContext)} can
 * be overridden to return the object for injection.
 * </ul>
 *
 * @see InjectionSimpleProcessor
 */
public class InjectionSimpleBinding<A extends Annotation>
                extends InjectionBinding<A>
{
    /**
     * The injection target for the annotated member. This field is only set
     * after metadata processing is complete.
     */
    private InjectionTarget ivInjectionTarget;

    public InjectionSimpleBinding(A annotation, ComponentNameSpaceConfiguration compNSConfig)
    {
        super(annotation, compNSConfig);
    }

    @Override
    public final void merge(A annotation, Class<?> instanceClass, Member member)
    {
        // Simple injection processors do not support JNDI names, so they do not
        // support merging.
    }

    @Override
    public void metadataProcessingComplete()
    {
        // The super method will clear the injection targets list.  Hold on to the
        // one and only injection target for this binding, both for good error
        // messages and for subclass convenience.
        ivInjectionTarget = ivInjectionTargets.get(0); // F87539
        super.metadataProcessingComplete();
    }

    /**
     * Return the one and only injection target for this binding.
     */
    public InjectionTarget getInjectionTarget()
    {
        return ivInjectionTarget != null ? ivInjectionTarget : ivInjectionTargets.get(0); // F87539
    }

    @Override
    public String getDisplayName() // F87539
    {
        StringBuilder builder = new StringBuilder();
        builder.append('@').append(getAnnotationType().getSimpleName());

        if (ivInjectionTarget != null)
        {
            Member member = ivInjectionTarget.getMember();
            builder.append(' ').append(member.getClass().getName())
                            .append('.').append(member.getName());

            if (member instanceof Method)
            {
                builder.append('(');

                Class<?>[] paramTypes = ((Method) member).getParameterTypes();
                if (paramTypes.length > 0)
                {
                    builder.append(paramTypes[0].getSimpleName());
                    for (int i = 1; i < paramTypes.length; i++)
                    {
                        builder.append(", ").append(paramTypes[i].getSimpleName());
                    }
                }

                builder.append(')');
            }
        }

        return builder.toString();
    }
}
