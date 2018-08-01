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

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.opentracing.References;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

/**
 * <p>Open tracing client filter implementation. Handles outgoing requests and
 * incoming responses. (Contract with the server filter, which handles incoming
 * requests and outgoing responses.)</p>
 *
 * <p>Implements both {@link ClientRequestFilter} and {@link ClientResponseFilter}.</p>
 *
 * <p>This implementation is stateless. A single client filter is used by all clients.</p>
 */
public class OpentracingClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingClientFilter.class);

    /**
     * <p>The property used to store the continuation for the outgoing request.</p>
     *
     * <p>See {@link OpentracingContainerFilter#SERVER_SPAN_PROP_ID} for more information.</p>
     */
    public static final String CLIENT_CONTINUATION_PROP_ID = OpentracingClientFilter.class.getName() + ".Span";

    public static final String CLIENT_SPAN_SKIPPED_ID = OpentracingClientFilter.class.getName() + ".Skipped";

    private static final String TAG_COMPONENT_JAXRS = "jaxrs";

    private OpentracingFilterHelper helper;

    OpentracingClientFilter(OpentracingFilterHelper helper) {
        setFilterHelper(helper);
    }

    void setFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
    }

    /**
     * <p>Handle an outgoing request.</p>
     *
     * <p>Associate the outgoing request with the incoming request. Create
     * an outgoing span. Inject the span into the outgoing request headers
     * for propagation to the next server.</p>
     *
     * <p>A tracer is expected to be available from the open tracing context
     * manager. Do nothing if a tracer is not available.</p>
     *
     * @param clientRequestContext The outgoing request context.
     *
     * @throws IOException Thrown if handling the request failed.
     */
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        String methodName = "filter(outgoing)";

        Tracer tracer = OpentracingTracerManager.getTracer();
        if (tracer == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " no tracer");
            }
            return;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, OpentracingUtils.getTracerText(tracer));
            }
        }

        URI outgoingUri = clientRequestContext.getUri();
        String outgoingURL = outgoingUri.toURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " outgoing URL", outgoingURL);
        }

        /*
         * Removing filter processing until microprofile spec for it is approved. Expect to add this code
         * back in 1Q18 - smf
         */
//        boolean process = OpentracingService.process(outgoingUri, SpanFilterType.OUTGOING);
        boolean process = true;

        if (process) {
            String buildSpanName = helper != null ? helper.getBuildSpanName(clientRequestContext) : outgoingURL;
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(buildSpanName);

            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
            spanBuilder.withTag(Tags.HTTP_URL.getKey(), outgoingURL);
            spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), clientRequestContext.getMethod());
            spanBuilder.withTag(Tags.COMPONENT.getKey(), TAG_COMPONENT_JAXRS);

            SpanContext parentSpanContext = (SpanContext) clientRequestContext.getProperty(References.CHILD_OF);

            if (parentSpanContext != null) {
                spanBuilder.ignoreActiveSpan().asChildOf(parentSpanContext);
            }

            Span span = spanBuilder.start();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " span", span);
            }
            tracer.inject(
                          span.context(),
                          Format.Builtin.HTTP_HEADERS, new MultivaluedMapToTextMap(clientRequestContext.getHeaders()));

            clientRequestContext.setProperty(CLIENT_CONTINUATION_PROP_ID, span);

        } else {
            Span currentSpan = tracer.activeSpan();
            if (currentSpan != null) {
                tracer.inject(
                              currentSpan.context(),
                              Format.Builtin.HTTP_HEADERS, new MultivaluedMapToTextMap(clientRequestContext.getHeaders()));
            }
        }

        clientRequestContext.setProperty(CLIENT_SPAN_SKIPPED_ID, !process);
    }

    /**
     * <p>Handle an incoming response.</p>
     *
     * <p>A span is expected to be available from the tracing context. Do
     * nothing if a span is not available.</p>
     *
     * @param clientRequestContext The outgoing request context.
     * @param clientResponseContext The incoming response context.
     *
     * @throws IOException Thrown if handling the response failed.
     */
    @Override
    public void filter(ClientRequestContext clientRequestContext,
                       ClientResponseContext clientResponseContext) throws IOException {
        String methodName = "filter(incoming)";

        Boolean skip = (Boolean) clientRequestContext.getProperty(CLIENT_SPAN_SKIPPED_ID);
        if ((skip != null) && skip) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "skipped");
            }
            clientRequestContext.removeProperty(CLIENT_SPAN_SKIPPED_ID);
            return;
        }

        Span contSpan = (Span) clientRequestContext.getProperty(CLIENT_CONTINUATION_PROP_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, contSpan);
        }

        if (contSpan == null) {
            // This may occur if there's no Tracer (see other method); otherwise, there's
            // probably some bug sending the right Continuation (e.g. threading?).
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " no continuation span");
            }
            return;
        }

        clientRequestContext.removeProperty(CLIENT_CONTINUATION_PROP_ID);

        Integer httpStatus = Integer.valueOf(clientResponseContext.getStatus());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " httpStatus", httpStatus);
        }

        contSpan.setTag(Tags.HTTP_STATUS.getKey(), httpStatus);
        if (clientResponseContext.getStatus() >= 400) {
            contSpan.setTag(Tags.ERROR.getKey(), true);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " error", clientResponseContext.getStatus());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " finish span", contSpan);
        }
        contSpan.finish();
    }

    private class MultivaluedMapToTextMap implements TextMap {
        private final MultivaluedMap<String, Object> mvMap;

        @Trivial
        public MultivaluedMapToTextMap(MultivaluedMap<String, Object> mvMap) {
            this.mvMap = mvMap;
        }

        @Trivial
        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            throw new UnsupportedOperationException();
        }

        @Trivial
        @Override
        public void put(String key, String value) {
            mvMap.add(key, value);
        }
    }
}
