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
package com.ibm.ws.cdi.extension.test;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * @param <T>
 *
 */
public class CustomInjectionTarget<T> implements InjectionTarget<T> {

    private final InjectionTarget<T> original;

    /**
     * @param original
     */
    public CustomInjectionTarget(InjectionTarget<T> original) {
        this.original = original;
    }

    /** {@inheritDoc} */
    @Override
    public void dispose(T arg0) {
        this.original.dispose(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return this.original.getInjectionPoints();
    }

    /** {@inheritDoc} */
    @Override
    public T produce(CreationalContext<T> arg0) {
        System.out.println("CustomInjectionTarget: produce " + original);
        return this.original.produce(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void inject(T arg0, CreationalContext<T> arg1) {
        System.out.println("CustomInjectionTarget: inject " + original);
        this.original.inject(arg0, arg1);
    }

    /** {@inheritDoc} */
    @Override
    public void postConstruct(T arg0) {
        this.original.postConstruct(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public void preDestroy(T arg0) {
        this.original.preDestroy(arg0);
    }

}
