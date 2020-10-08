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
