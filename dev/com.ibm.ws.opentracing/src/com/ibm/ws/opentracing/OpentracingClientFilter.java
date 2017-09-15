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

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
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
 */
@Component
public class OpentracingClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingClientFilter.class);
    /**
     * <p>The property used to store the client span ID for the outgoing request.</p>
     *
     * <p>See {@link OpentracingContainerFilter#SERVER_SPAN_PROP_ID} for more information.</p>
     */
    public static final String CLIENT_SPAN_PROP_ID = OpentracingClientFilter.class.getName() + ".Span";

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
        // Get the Tracer
        Tracer tracer = OpentracingTracerManager.getTracer();

        // If there is no tracer, then there is nothing the filter can do
        if (tracer == null) {
            Tr.error(tc, "OPENTRACING_NO_TRACER_FOR_OUTBOUND_REQUEST");
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ClientRequestContext.filter request. tracer = " + tracer);
        }

        // Use the URL to add data to the Span
        String outgoingURL = clientRequestContext.getUri().toURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ClientRequestContext.filter request. Outgoing URL = " + outgoingURL);
        }

        // Set the name of the Span to the outgoing URL
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(outgoingURL);

        // Add appropriate Tags to the Span
        spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        spanBuilder.withTag(Tags.HTTP_URL.getKey(), outgoingURL);
        spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), clientRequestContext.getMethod());

        // Make this Span a child of the Active Span if there is an Active Span
        ActiveSpan activeSpan = tracer.activeSpan();
        if (activeSpan != null) {
            spanBuilder.asChildOf(activeSpan.context());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpentracingClientFilter.filter request. Creating child Span");
            }
        }

        // Start the Span
        Span span = spanBuilder.startManual();

        // Inject the Span info into the ougoing header so the info is propagated to the next server
        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new MultivaluedMapToTextMap(clientRequestContext.getHeaders()));

        // Note the use of the span stored under the client property: This is to
        // create the association of the outgoing request to the incoming response.
        clientRequestContext.setProperty(CLIENT_SPAN_PROP_ID, span);
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
        Span span = (Span) clientRequestContext.getProperty(OpentracingClientFilter.CLIENT_SPAN_PROP_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OpentracingClientFilter.filter response. Span =" + span);
        }
        if (span == null) {
            Tr.error(tc, "OPENTRACING_NO_SPAN_FOR_RESPONSE_TO_OUTBOUND_REQUEST");
            return;
        }

        try {
            // Add appropriate Tags to the Span
            span.setTag(Tags.HTTP_STATUS.getKey(), clientResponseContext.getStatus());
            if (clientResponseContext.getStatus() >= 400) {
                span.setTag(Tags.ERROR.getKey(), true);
            }

            // Finish the Span
            span.finish();
        } finally {
            // Clean up
            clientRequestContext.removeProperty(OpentracingClientFilter.CLIENT_SPAN_PROP_ID);
        }
    }

    private class MultivaluedMapToTextMap implements TextMap {
        private final MultivaluedMap<String, Object> mvMap;

        public MultivaluedMapToTextMap(MultivaluedMap<String, Object> mvMap) {
            this.mvMap = mvMap;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(String key, String value) {
            mvMap.add(key, value);
        }

    }
}
