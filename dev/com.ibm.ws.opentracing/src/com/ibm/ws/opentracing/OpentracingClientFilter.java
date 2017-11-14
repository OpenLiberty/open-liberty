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

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.opentracing.filters.SpanFilterType;

import io.opentracing.ActiveSpan;
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
@Component
public class OpentracingClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingClientFilter.class);

    //

    /**
     * <p>The property used to store the client span ID for the outgoing request.</p>
     *
     * <p>See {@link OpentracingContainerFilter#SERVER_SPAN_PROP_ID} for more information.</p>
     */
    public static final String CLIENT_SPAN_PROP_ID = OpentracingClientFilter.class.getName() + ".Span";

    public static final String CLIENT_SPAN_SKIPPED_ID = OpentracingClientFilter.class.getName() + ".Skipped";

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
            Tr.error(tc, "OPENTRACING_NO_TRACER_FOR_OUTBOUND_REQUEST");
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

        ActiveSpan priorIncomingSpan = tracer.activeSpan();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " parent", priorIncomingSpan);
        }

        boolean process = OpentracingService.process(outgoingUri, SpanFilterType.OUTGOING);

        SpanContext nextContext;

        if (process) {
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(outgoingURL);

            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
            spanBuilder.withTag(Tags.HTTP_URL.getKey(), outgoingURL);
            spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), clientRequestContext.getMethod());

            if (priorIncomingSpan != null) {
                spanBuilder.asChildOf(priorIncomingSpan.context());
            } else {
                // spanBuilder.ignoreActiveSpan();
                //
                // TODO:
                // The work-around, which is a call 'spanBuilder.ignoreActiveSpan()',
                // and which is used in the container filter, is not needed here.
                //
                // The code is a bit muddled, though, as the mock span defaults to
                // look for and use the active span as the parent span, which means
                // the call 'spanBuilder.asChildOf' may not be necessary.
            }

            Span newOutoingSpan = spanBuilder.startManual();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " outgoingSpan", newOutoingSpan);
            }

            nextContext = newOutoingSpan.context();

            // Note the use of the span stored under the client property: This is to
            // create the association of the outgoing request to the incoming response.
            clientRequestContext.setProperty(CLIENT_SPAN_PROP_ID, newOutoingSpan);
        } else {
            nextContext = priorIncomingSpan.context();
        }

        tracer.inject(
                      nextContext,
                      Format.Builtin.HTTP_HEADERS, new MultivaluedMapToTextMap(clientRequestContext.getHeaders()));

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

        if ((Boolean) clientRequestContext.getProperty(CLIENT_SPAN_SKIPPED_ID)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "skipped");
            }
            clientRequestContext.removeProperty(CLIENT_SPAN_SKIPPED_ID);
            return;
        }

        Span priorOutgoingSpan = (Span) clientRequestContext.getProperty(CLIENT_SPAN_PROP_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, priorOutgoingSpan);
        }

        if (priorOutgoingSpan == null) {
            Tr.error(tc, "OPENTRACING_NO_SPAN_FOR_RESPONSE_TO_OUTBOUND_REQUEST");
            return;
        }

        try {
            Integer httpStatus = Integer.valueOf(clientResponseContext.getStatus());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " httpStatus", httpStatus);
            }

            priorOutgoingSpan.setTag(Tags.HTTP_STATUS.getKey(), httpStatus);
            if (clientResponseContext.getStatus() >= 400) {
                priorOutgoingSpan.setTag(Tags.ERROR.getKey(), true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " error", Boolean.TRUE);
                }
            }

            priorOutgoingSpan.finish();

        } finally {
            clientRequestContext.removeProperty(CLIENT_SPAN_PROP_ID);
        }
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
