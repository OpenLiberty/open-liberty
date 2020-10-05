package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import io.opentracing.ScopeManager;

public class JaegerTracerImplBuilder extends AbstractJaegerAdapter<io.jaegertracing.internal.JaegerTracer.Builder> 
                                      implements com.ibm.ws.microprofile.opentracing.jaeger.adapter.JaegerTracer.Builder {

    public JaegerTracerImplBuilder(String serviceName) {
        super(new io.jaegertracing.internal.JaegerTracer.Builder(serviceName));
    }

    public JaegerTracerImplBuilder withScopeManager(ScopeManager scopeManager) {
        getDelegate().withScopeManager(scopeManager);
        return this;
    }

}
