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
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

/**
 * <p>Container filter implementation.</p>
 *
 * <p>This implementation is stateless. A single container filter is used by all applications.</p> *
 */
public class OpentracingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter, ExceptionMapper<Throwable> {
    private static final TraceComponent tc = Tr.register(OpentracingContainerFilter.class);

    public static final String SERVER_SPAN_PROP_ID = OpentracingContainerFilter.class.getName() + ".Span";

    public static final String SERVER_SPAN_SKIPPED_ID = OpentracingContainerFilter.class.getName() + ".Skipped";

    public static final String EXCEPTION_KEY = OpentracingContainerFilter.class.getName() + ".Exception";

    private static final String TAG_COMPONENT_JAXRS = "jaxrs";

    @Context
    protected ResourceInfo resourceInfo;

    private OpentracingFilterHelper helper;

    OpentracingContainerFilter(OpentracingFilterHelper helper) {
        setFilterHelper(helper);
    }

    void setFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext) throws IOException {
        String methodName = "filter(incoming)";

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
         * back in 1Q18 - smf
         */
        // boolean process = OpentracingService.process(incomingUri, SpanFilterType.INCOMING);
        boolean process = true;

        String buildSpanName;
        if (helper != null) {
            buildSpanName = helper.getBuildSpanName(incomingRequestContext, resourceInfo);
            if (buildSpanName == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " skipping not traced method");
                }
                process = false;
            }
        } else {
            buildSpanName = incomingURL;
        }

        if (process) {
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(buildSpanName);
            spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
            spanBuilder.withTag(Tags.HTTP_URL.getKey(), incomingURL);
            spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), incomingRequestContext.getMethod());
            spanBuilder.withTag(Tags.COMPONENT.getKey(), TAG_COMPONENT_JAXRS);
            if (priorOutgoingContext != null) {
                spanBuilder.asChildOf(priorOutgoingContext);
            }

//            Span span = spanBuilder.startActive(false).span();
            Scope scope = spanBuilder.startActive(false);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, methodName + " span", span);
                Tr.debug(tc, methodName + " span", scope.span());
            }

//            incomingRequestContext.setProperty(SERVER_SPAN_PROP_ID, span);
            incomingRequestContext.setProperty(SERVER_SPAN_PROP_ID, scope);
        }

        incomingRequestContext.setProperty(SERVER_SPAN_SKIPPED_ID, !process);
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext,
                       ContainerResponseContext outgoingResponseContext) throws IOException {
        String methodName = "filter(outgoing)";

        Boolean skipped = (Boolean) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_SKIPPED_ID);

        if (skipped != null) {
            // Remove it immediately
            incomingRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_SKIPPED_ID);
        }

        if (skipped != null && skipped) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " skipped");
            }
            return;
        }

        // If processing wasn't skipped, then we should have an ActiveSpan
//        Span span = (Span) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        Scope scope = (Scope) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        if (scope == null) {
//        if (span == null) {
            // This may occur if there's no Tracer (see other method); otherwise, there's
            // probably some bug sending the right ActiveSpan (e.g. threading?).
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " no span");
            }
            return;
        }

        incomingRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);

        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, methodName + " span", span);
                Tr.debug(tc, methodName + " span", scope.span());
            }

            Integer httpStatus = Integer.valueOf(outgoingResponseContext.getStatus());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " httpStatus", httpStatus);
            }
//            span.setTag(Tags.HTTP_STATUS.getKey(), httpStatus);
            scope.span().setTag(Tags.HTTP_STATUS.getKey(), httpStatus);

            if (outgoingResponseContext.getStatus() >= 400) {
                MultivaluedMap<String, Object> headers = outgoingResponseContext.getHeaders();

                Throwable exception = (Throwable) headers.getFirst(EXCEPTION_KEY);
                if (exception != null) {
                    headers.remove(EXCEPTION_KEY);
                }

//                OpentracingService.addSpanErrorInfo(span, exception);
                OpentracingService.addSpanErrorInfo(scope.span(), exception);
            }
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " finish span", scope.span());
            }
            scope.close();
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

    /** {@inheritDoc} */
    @Override
    public Response toResponse(Throwable exception) {
        return Response.serverError().header(EXCEPTION_KEY, exception).build();
    }
}
