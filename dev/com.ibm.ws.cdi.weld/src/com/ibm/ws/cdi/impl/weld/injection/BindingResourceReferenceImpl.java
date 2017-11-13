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
package com.ibm.ws.cdi.impl.weld.injection;

import java.lang.annotation.Annotation;

import org.jboss.weld.injection.spi.ResourceReference;

import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 *
 */
public class BindingResourceReferenceImpl<T, S extends Annotation> implements ResourceReference<T> {

    private final InjectionBinding<S> binding;

    public BindingResourceReferenceImpl(InjectionBinding<S> binding) {
        this.binding = binding;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public T getInstance() {
        try {
            return (T) binding.getInjectionObject();
        } catch (InjectionException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void release() {
        //no-op
    }

}
