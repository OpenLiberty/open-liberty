/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/package com.ibm.ws.microprofile.opentracing.jaeger.adapter.impl;

import java.util.HashMap;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.util.ThreadLocalScopeManager;

public class LRCScopeManager implements ScopeManager {
    
    private static final TraceComponent tc = Tr.register(LRCScopeManager.class);
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
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "previousTraceId=" + previousTraceId);
                Tr.debug(tc, "previousSpanId=" + previousSpanId);
                Tr.debug(tc, "TraceId=" + span.context().toTraceId());
                Tr.debug(tc, "SpanId=" + span.context().toSpanId());
            }
            LogRecordContext.addExtension(LRC_TRACE_ID_KEY, span.context().toTraceId());
            LogRecordContext.addExtension(LRC_SPAN_ID_KEY, span.context().toSpanId());
        }
        
        @Override
        public void close() {
            this.scope.close();
            if (previousTraceId != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "restore previous traceId=" + previousTraceId);
                }
                LogRecordContext.addExtension(LRC_TRACE_ID_KEY, previousTraceId);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "remove traceId");
                }
                LogRecordContext.removeExtension(LRC_TRACE_ID_KEY);
            }
            if (previousSpanId != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "restore previous spanId=" + previousSpanId);
                }
                LogRecordContext.addExtension(LRC_SPAN_ID_KEY, previousSpanId);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "remove spanId");
                }
                LogRecordContext.removeExtension(LRC_SPAN_ID_KEY);
            }
        }
     
        private String getLRCValue(String key) {
            HashMap<String, String> map = new HashMap<String, String>();
            LogRecordContext.getExtensions(map);
            return map.get(key);
        }
        
    }

}
