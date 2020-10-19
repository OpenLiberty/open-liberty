/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerTracer.Builder;

import io.opentracing.ScopeManager;
import io.opentracing.Tracer;

public class JaegerTracerBuilderImpl extends AbstractJaegerAdapter<io.jaegertracing.internal.JaegerTracer.Builder>
        implements com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerTracer.Builder {

    public JaegerTracerBuilderImpl(io.jaegertracing.internal.JaegerTracer.Builder delegate) {
        super(delegate);
    }

    @Override
    public Builder withScopeManager(ScopeManager scopeManager) {
        getDelegate().withScopeManager(scopeManager);
        return this;
    }

    @Override
    public Tracer build() {
        return getDelegate().build();
    }

}
