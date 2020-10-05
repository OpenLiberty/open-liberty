package com.ibm.ws.microprofile.opentracing.jaeger.adapter;

import io.opentracing.ScopeManager;

public interface JaegerTracer extends JaegerAdapter {

    public interface Builder {
        
        public Builder withScopeManager(ScopeManager scopeManager);
        
    }
}
