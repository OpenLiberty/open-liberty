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
import java.text.Format;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

/**
 * <p>Container filter implementation.</p>
 *
 * <p>This implementation is stateless. A single container filter is used by all applications.</p> *
 */
public class OpentracingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingContainerFilter.class);

    public static final String SERVER_SPAN_PROP_ID = OpentracingContainerFilter.class.getName() + ".Span";

    public static final String SERVER_SPAN_SKIPPED_ID = OpentracingContainerFilter.class.getName() + ".Skipped";

    @Context
    private ResourceInfo resourceInfo;

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext) throws IOException {
        String methodName = "filter(incoming)";

        Tracer tracer = OpentracingTracerManager.getTracer();
        if (tracer == null) {
            Tr.error(tc, "OPENTRACING_NO_TRACER_FOR_INBOUND_REQUEST");
            return;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, OpentracingUtils.getTracerText(tracer));
            }
        }

        URI incomingUri = incomingRequestContext.getUriInfo().getRequestUri();
        String incomingURL = incomingUri.toURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " incomingURL", incomingURL);
        }

        SpanContext priorOutgoingContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                                                          new MultivaluedMapToTextMap(incomingRequestContext.getHeaders()));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " priorContext", priorOutgoingContext);
        }

        /*
         * Removing filter processing until microprofile spec for it is approved. Expect to add this code
         * back in in 1Q18 - smf
         */
        // boolean process = OpentracingService.process(incomingUri, SpanFilterType.INCOMING);
        boolean process = true;

        Annotation[] annotations = resourceInfo.getResourceMethod().getAnnotations();
        for (Annotation anno : annotations) {
            String processing = anno.toString();
            if (processing.contains("Traced(value=")) {
                if (processing.contains("value=true")) {
                    System.out.println("BB - Annotated method Traced found and passed to interceptor - incoming");
                    return;
                }
            }

        }

        if (process) {
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(incomingURL);
            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            spanBuilder.withTag(Tags.HTTP_URL.getKey(), incomingURL);
            spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), incomingRequestContext.getMethod());

            if (priorOutgoingContext != null) {
                spanBuilder.asChildOf(priorOutgoingContext);
            } else {
                spanBuilder.ignoreActiveSpan();
                // TODO: This is a work-around for a mock tracer bug.
                // See io.opentracing.mock.MockTracer.MockSpanBuilder.startManual(),
                // which will set the parent based on the active context.
            }

            Span newIncomingSpan = spanBuilder.startManual();

            tracer.makeActive(newIncomingSpan);

            incomingRequestContext.setProperty(SERVER_SPAN_PROP_ID, newIncomingSpan);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " contextSpan", newIncomingSpan);
            }
        }

        incomingRequestContext.setProperty(SERVER_SPAN_SKIPPED_ID, !process);
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext,
                       ContainerResponseContext outgoingResponseContext) throws IOException {
        String methodName = "filter(outgoing)";

        Annotation[] annotations = resourceInfo.getResourceMethod().getAnnotations();
        for (Annotation anno : annotations) {
            String processing = anno.toString();
            if (processing.contains("Traced(value=")) {
                if (processing.contains("value=true")) {
                    System.out.println("BB - Annotated method Traced found and passed to interceptor - incoming");
                    return;
                }
            }

        }

        if ((Boolean) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_SKIPPED_ID)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " skipped");
            }
            incomingRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_SKIPPED_ID);
            return;
        }

        Span incomingSpan = (Span) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        if (incomingSpan == null) {
            Tr.error(tc, "OPENTRACING_NO_SPAN_FOR_RESPONSE_TO_INBOUND_REQUEST");
            return;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " incomingSpan", incomingSpan);
            }
        }

        try {
            Integer httpStatus = Integer.valueOf(outgoingResponseContext.getStatus());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " httpStatus", httpStatus);
            }
            incomingSpan.setTag(Tags.HTTP_STATUS.getKey(), httpStatus);

            if (outgoingResponseContext.getStatus() >= 400) {
                incomingSpan.setTag(Tags.ERROR.getKey(), true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " error", Boolean.TRUE);
                }
            }

        } finally {
            incomingSpan.finish();
            incomingRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        }
    }

    //

    private static class MultivaluedMapToTextMap implements TextMap {
        private final MultivaluedMap<String, String> mvMap;

        @Trivial
        public MultivaluedMapToTextMap(MultivaluedMap<String, String> mvMap) {
            this.mvMap = mvMap;
        }

        @Trivial
        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return new MultivaluedMapFlatIterator<>(mvMap.entrySet());
        }

        @Trivial
        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException();
        }
    }

    private static class MultivaluedMapFlatIterator<K, V> implements Iterator<Map.Entry<K, V>> {
        private final Iterator<Map.Entry<K, List<V>>> mapIterator;

        private Map.Entry<K, List<V>> mapEntry;
        private Iterator<V> mapEntryIterator;

        @Trivial
        public MultivaluedMapFlatIterator(Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
            this.mapIterator = multiValuesEntrySet.iterator();

            this.mapEntry = null;
            this.mapEntryIterator = null;
        }

        @Override
        @Trivial
        public boolean hasNext() {
            return (((mapEntryIterator != null) && mapEntryIterator.hasNext()) ||
                    mapIterator.hasNext());
        }

        @Override
        @Trivial
        public Map.Entry<K, V> next() {
            if ((mapEntry == null) ||
                (!mapEntryIterator.hasNext() && mapIterator.hasNext())) {
                mapEntry = mapIterator.next();
                mapEntryIterator = mapEntry.getValue().iterator();
            }

            if (mapEntryIterator.hasNext()) {
                // Iterate across the entry's values ...
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), mapEntryIterator.next());
            } else {
                // Generate (key, null) for an entry with empty values
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), null);
            }
        }

        @Override
        @Trivial
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
