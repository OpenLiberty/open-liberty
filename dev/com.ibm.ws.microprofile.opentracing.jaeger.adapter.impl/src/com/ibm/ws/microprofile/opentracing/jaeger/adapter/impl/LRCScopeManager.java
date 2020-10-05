package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import java.util.HashMap;

import com.ibm.websphere.logging.hpel.LogRecordContext;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.util.ThreadLocalScopeManager;

public class LRCScopeManager implements ScopeManager {
    
    private ScopeManager scopeManager = new ThreadLocalScopeManager();
    private static final String LRC_TRACE_ID_KEY = "traceId";
    private static final String LRC_SPAN_ID_KEY = "spanId";
    
    @Override
    public Scope activate(Span span) {
        return new LRCScope(this.scopeManager.activate(span), span);
    }

    @Override
    public Span activeSpan() {
        return this.scopeManager.activeSpan();
    }
    
    private class LRCScope implements Scope {
        private final Scope scope;
        private final String previousTraceId;
        private final String previousSpanId;

        LRCScope(Scope scope, Span span) {
            this.scope = scope;
            this.previousTraceId = getLRCValue(LRC_TRACE_ID_KEY);
            this.previousSpanId = getLRCValue(LRC_SPAN_ID_KEY);
            LogRecordContext.addExtension(LRC_TRACE_ID_KEY, span.context().toTraceId());
            LogRecordContext.addExtension(LRC_SPAN_ID_KEY, span.context().toSpanId());
        }
        
        @Override
        public void close() {
            this.scope.close();
            if (previousTraceId != null) {
                LogRecordContext.addExtension(LRC_TRACE_ID_KEY, previousTraceId);
            }
            if (previousSpanId != null) {
                LogRecordContext.addExtension(LRC_SPAN_ID_KEY, previousSpanId);
            }
        }
     
        private String getLRCValue(String key) {
            HashMap<String, String> map = new HashMap<String, String>();
            LogRecordContext.getExtensions(map);
            return map.get(key);
        }
        
    }

}
