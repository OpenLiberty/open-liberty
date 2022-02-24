/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal;

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
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.opentracing.internal.filters.SpanFilterType;
import io.opentracing.Scope;
import io.opentracing.Span;
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
@Provider
public class OpentracingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final TraceComponent tc = Tr.register(OpentracingContainerFilter.class);

    public static final String SERVER_SPAN_PROP_ID = OpentracingContainerFilter.class.getName() + ".Span";

    public static final String SERVER_SPAN_SKIPPED_ID = OpentracingContainerFilter.class.getName() + ".Skipped";

    public static final String EXCEPTION_KEY = OpentracingContainerFilter.class.getName() + ".Exception";

    private static final String TAG_COMPONENT_JAXRS = "jaxrs";

    @Context
    protected ResourceInfo resourceInfo;

    private OpentracingFilterHelper helper;

    private boolean spanErrorLogged = false;

    public OpentracingContainerFilter() {}

    void setFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext) throws IOException {
        String methodName = "filter(incoming)";
        helper = OpentracingFilterHelperProvider.getInstance().getOpentracingFilterHelper();

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
        String incomingPath = incomingRequestContext.getUriInfo().getPath();
        if (!incomingPath.startsWith("/")) {
            incomingPath = "/" + incomingPath;
        }

        String incomingURL = incomingUri.toURL().toString();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " incomingURL", incomingURL);
        }

        SpanContext priorOutgoingContext = tracer.extract(Format.Builtin.HTTP_HEADERS,
                                                          new MultivaluedMapToTextMap(incomingRequestContext.getHeaders()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " priorContext", priorOutgoingContext);
        }

        boolean process = OpentracingService.process(incomingUri, incomingPath, SpanFilterType.INCOMING);

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

            try{
                Span span = spanBuilder.start();
                Scope scope = tracer.scopeManager().activate(span);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " span", span);
                }

                incomingRequestContext.setProperty(SERVER_SPAN_PROP_ID, new ActiveSpan(span, scope));
            } catch (NoSuchMethodError e) {
                if (!spanErrorLogged) {
                    Tr.error(tc, "OPENTRACING_COULD_NOT_START_SPAN", e);
                    spanErrorLogged = true;
                }
            }
        }

        incomingRequestContext.setProperty(SERVER_SPAN_SKIPPED_ID, !process);
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ContainerRequestContext incomingRequestContext,
                       ContainerResponseContext outgoingResponseContext) throws IOException {

        String methodName = "filter(outgoing)";
        helper = OpentracingFilterHelperProvider.getInstance().getOpentracingFilterHelper();
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
        ActiveSpan activeSpan = (ActiveSpan) incomingRequestContext.getProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);
        if (activeSpan == null) {
            // This may occur if there's no Tracer (see other method); otherwise, there's
            // probably some bug sending the right ActiveSpan (e.g. threading?).
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " no ActiveSpan");
            }
            return;
        }

        incomingRequestContext.removeProperty(OpentracingContainerFilter.SERVER_SPAN_PROP_ID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " ActiveSpan", activeSpan);
        }

        Integer httpStatus = Integer.valueOf(outgoingResponseContext.getStatus());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " httpStatus", httpStatus);
        }
        Span span = activeSpan.getSpan();
        span.setTag(Tags.HTTP_STATUS.getKey(), httpStatus);

        if (outgoingResponseContext.getStatus() >= 400) {
            MultivaluedMap<String, Object> headers = outgoingResponseContext.getHeaders();

            Throwable exception = (Throwable) headers.getFirst(EXCEPTION_KEY);
            if (exception != null) {
                headers.remove(EXCEPTION_KEY);
            }

            OpentracingService.addSpanErrorInfo(span, exception);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " finish span", span);
        }
        activeSpan.getScope().close();
        span.finish();
    }

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
