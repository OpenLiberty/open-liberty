/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerAdapter;

/**
 *
 */
public abstract class AbstractJaegerAdapter<T> implements JaegerAdapter {

    private final T delegate;

    public AbstractJaegerAdapter(T delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public final T getDelegate() {
        return this.delegate;
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

}
