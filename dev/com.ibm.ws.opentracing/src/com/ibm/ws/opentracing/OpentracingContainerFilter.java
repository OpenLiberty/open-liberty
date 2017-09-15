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
import javax.ws.rs.core.UriInfo;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.tag.Tags;

/**
 *
 */
public class OpentracingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingContainerFilter.class);

    public static final String SERVER_SPAN_PROP_ID = OpentracingContainerFilter.class.getName() + ".Span";

    @Context
    ResourceInfo resourceInfo;

    /** {@inheritDoc} */
    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        Tracer tracer = OpentracingTracerManager.getTracer();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OpentracingServerFilter.filter request. tracer = " + tracer + " resourceInfo = " + resourceInfo);
        }
        if (tracer == null) {
            Tr.error(tc, "OPENTRACING_NO_TRACER_FOR_INBOUND_REQUEST");
            return;
        }
        SpanContext extractedSpanContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                                                          new MultivaluedMapToTextMap(containerRequestContext.getHeaders()));

        UriInfo uri = containerRequestContext.getUriInfo();
        String urlLocation = uri.getRequestUri().toURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OpentracingServerFilter.filter request. Incoming URL = " + urlLocation);
        }
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(urlLocation);
        spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
        spanBuilder.withTag(Tags.HTTP_URL.getKey(), urlLocation);
        spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), containerRequestContext.getMethod());

        if (extractedSpanContext != null) {
            spanBuilder.asChildOf(extractedSpanContext);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpentracingServerFilter.filter request. Creating child Span");
            }
        }

        Span span = spanBuilder.startManual();
        tracer.makeActive(span);
        containerRequestContext.setProperty(SERVER_SPAN_PROP_ID, span);
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        //Retrieve Span from containerRequestContext and finish the Span.
        Span span = (Span) containerRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "OpentracingServerFilter.filter response. Span =" + span);
        }
        if (span == null) {
            Tr.error(tc, "OPENTRACING_NO_SPAN_FOR_RESPONSE_TO_INBOUND_REQUEST");
            return;
        }
        span.setTag(Tags.HTTP_STATUS.getKey(), containerResponseContext.getStatus());
        if (containerResponseContext.getStatus() >= 400) {
            span.setTag(Tags.ERROR.getKey(), true);
        }
        span.finish();
        containerRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
    }

    private class MultivaluedMapToTextMap implements TextMap {

        private final MultivaluedMap<String, String> mvMap;

        public MultivaluedMapToTextMap(MultivaluedMap<String, String> mvMap) {
            this.mvMap = mvMap;
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return new MultivaluedMapFlatIterator<>(mvMap.entrySet());
        }

        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException();
        }
    }

    private class MultivaluedMapFlatIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<K, List<V>>> mapIterator;
        private Map.Entry<K, List<V>> mapEntry;
        private Iterator<V> listIterator;

        public MultivaluedMapFlatIterator(Set<Map.Entry<K, List<V>>> multiValuesEntrySet) {
            this.mapIterator = multiValuesEntrySet.iterator();
        }

        @Override
        public boolean hasNext() {
            if (listIterator != null && listIterator.hasNext()) {
                return true;
            }

            return mapIterator.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (mapEntry == null || (!listIterator.hasNext() && mapIterator.hasNext())) {
                mapEntry = mapIterator.next();
                listIterator = mapEntry.getValue().iterator();
            }

            if (listIterator.hasNext()) {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), listIterator.next());
            } else {
                return new AbstractMap.SimpleImmutableEntry<>(mapEntry.getKey(), null);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
