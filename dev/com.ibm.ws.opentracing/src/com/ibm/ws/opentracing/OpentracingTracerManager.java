/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing;

import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * <p>Open tracing context management.</p>
 *
 * <p>Associate incoming requests with a unique open tracing context. This is made
 * available to outgoing requests by storing the context to a thread local variable.
 */
public class OpentracingTracerManager {
    private static final TraceComponent tc = Tr.register(OpentracingTracerManager.class);

    private static final String DEFAULT_SERVICE_NAME = "com.ibm.ws.opentracing.defaultServiceName";

    private static Map<String, Tracer> appNameToTracerMap = new HashMap<String, Tracer>();

    // Open tracing context pass through ...

    /**
     * <p>Answer the tracer of the active open tracing context.</p>
     *
     * @Return The tracer of the active open tracing context.
     */
    public static synchronized Tracer getTracer() {
        Tracer tracer = getOpentracingContext().getTracer();
        if (tracer == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpentracingTracerManager.getTracer() Look up Tracer");
            }
            String serviceName;
            try {
                serviceName = (String) new InitialContext().lookup("java:app/AppName");
            } catch (NamingException e) {
                // Should never happen
                Tr.error(tc, "OPENTRACING_NO_APPNAME_FOUND_IN_JNDI");
                serviceName = DEFAULT_SERVICE_NAME;
            }
            tracer = getTracer(serviceName);
            if (tracer == null) {
                tracer = OpentracingUserFeatureAccessService.getTracerInstance(serviceName);
            }
            setTracer(serviceName, tracer);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OpentracingTracerManager.getTracer() tracer = " + tracer);
        }
        return tracer;
    }

    @Trivial
    private static Tracer getTracer(String serviceName) {
        return appNameToTracerMap.get(serviceName);
    }

    /**
     * <p>Set the tracer of the active open tracing context.</p>
     *
     * @param tracer The tracer to set to the active open tracing context.
     */
    @Trivial
    private static void setTracer(Tracer tracer) {
        getOpentracingContext().setTracer(tracer);
    }

    @Trivial
    private static void setTracer(String serviceName, Tracer tracer) {
        appNameToTracerMap.put(serviceName, tracer);
        setTracer(tracer);
    }

    // Open tracing context storage ...

    /** <p>Storage for the open tracing context variable.</p> */
    private static final InheritableThreadLocal<OpentracingContext> OPEN_TRACING_CONTEXT_VAR = new OpentracingThreadLocal();

    /**
     * <p>Answer the active open tracing context.</p>
     *
     * <p>This is currently stored as a thread local value, with the association
     * to the thread which received an incoming service request. Placement in
     * a thread local variable makes the value available to outgoing requests
     * which are made in the same thread.</p>
     *
     * <p>An inheritable thread local variable (see {@link InheritableThreadLocal}) is
     * used for the storage, which means that new threads will be given a reference
     * to the context, which in turn means that asynchronous outgoing requests are
     * handled.</p>
     *
     * <p>Implementations other than a thread local variable are being considered.
     * The problem which must be solved is the association of outgoing requests
     * to incoming requests. That association is needed to associate the open
     * tracing span which was created for the incoming request with the open tracing
     * span which is created for an outgoing request. Service APIs do not provide
     * a mechanism to convey this association.</p>
     *
     * @return The active open tracing context. This should never be null.
     */
    @Trivial
    private static OpentracingContext getOpentracingContext() {
        return getOpenTracingContextVar().get();
        // Rely on 'initialValue' to supply a non-null open tracing context.
        // There is currently no code which clears the context.
    }

    /**
     * <p>Answer the thread local variable which stores the open tracing context.</p>
     *
     * @return The thread local variable which stores the open tracing context.
     */
    @Trivial
    private static ThreadLocal<OpentracingContext> getOpenTracingContextVar() {
        return OPEN_TRACING_CONTEXT_VAR;
    }

    /**
     * <p>Class for the open tracing context thread local variable.</p>
     *
     * <p>Note the use of type {@link InheritableThreadLocal}. Use of this type
     * means that threads which are spawned from the thread which initially handles
     * a request are be given a reference to the thread local value. That provides
     * the capability of handling outgoing requests in spawned threads.</p>
     *
     * <p>As a consequence, which must be handled then within the {@link Span}
     * implementation, is that the relationship of parent incoming requests to
     * child outgoing requests is not one to zero-or-one. The relationship is
     * one to zero-or-more.</p>
     */
    private static final class OpentracingThreadLocal extends InheritableThreadLocal<OpentracingContext> {
        /**
         * <p>Provide value obtained by the first call to {@link ThreadLocal#get()} for
         * the open tracing context variable. The value which is returned is used unless
         * a prior call is made to {@link ThreadLocal#set(Object)}.</p>
         *
         * TODO: The open tracing context is never cleared. This needs to be reviewed against
         * the probably reuse of threads for handling requests, both from the perspective
         * of a new request seeing a previously set tracing context, and from the
         * perspective of open tracing objects lasting longer than is necessary.
         *
         * @return The initial open tracing context value.
         */
        @Override
        protected OpentracingContext initialValue() {
            return new OpentracingContext();
        }
    }

}
