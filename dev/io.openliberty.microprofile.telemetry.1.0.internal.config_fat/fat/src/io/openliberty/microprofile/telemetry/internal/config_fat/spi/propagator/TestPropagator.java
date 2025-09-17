/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.config_fat.spi.propagator;

import static java.util.Collections.unmodifiableList;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.baggage.BaggageEntry;
import io.opentelemetry.api.baggage.BaggageEntryMetadata;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * A basic propagator for span context and baggage
 * <p>
 * Span information is passed in the TEST-SPAN key with format {@code traceId;spanId;flags;statekey1=value,statekey2=value}
 * <p>
 * Baggage information is passed in the TEST-BAGGAGE key with format {@code key,value,metadata;key2,value,metadata;key3,value,metadata;...}
 * <p>
 * All individual values are urlencoded to make parsing easy (don't have to worry about values containing separator characters)
 */
public class TestPropagator implements TextMapPropagator {

    public static final String BAGGAGE_KEY = "TEST-BAGGAGE";
    public static final String TRACE_KEY = "TEST-SPAN";

    private static final List<String> FIELDS = unmodifiableList(Arrays.asList(BAGGAGE_KEY, TRACE_KEY));

    /** {@inheritDoc} */
    @Override
    public Collection<String> fields() {
        return FIELDS;
    }

    /** {@inheritDoc} */
    @Override
    public <C> Context extract(Context context, C carrier, TextMapGetter<C> getter) {
        // extract data from carrier using getter and put it into context

        String baggageString = getter.get(carrier, BAGGAGE_KEY);
        if (baggageString != null && !baggageString.isEmpty()) {
            Baggage baggage = deserializeBaggage(baggageString);
            context = context.with(baggage);
        }

        String traceString = getter.get(carrier, TRACE_KEY);
        if (traceString != null && !traceString.isEmpty()) {
            Span span = deserializeSpan(traceString);
            context = context.with(span);
        }

        return context;
    }

    /** {@inheritDoc} */
    @Override
    public <C> void inject(Context context, C carrier, TextMapSetter<C> setter) {
        // take data from context and inject it into carrier using setter
        Baggage baggage = Baggage.fromContextOrNull(context);
        if (baggage != null && !baggage.isEmpty()) {
            setter.set(carrier, BAGGAGE_KEY, serializeBaggage(baggage));
        }

        Span span = Span.fromContextOrNull(context);
        if (span != null && span.getSpanContext().isValid()) {
            setter.set(carrier, TRACE_KEY, serializeSpan(span.getSpanContext()));
        }
    }

    private String serializeBaggage(Baggage baggage) {
        StringBuffer baggageString = new StringBuffer();
        boolean first = true;
        for (Entry<String, BaggageEntry> entry : baggage.asMap().entrySet()) {
            if (!first) {
                baggageString.append(';');
                first = false;
            }
            baggageString.append(encode(entry.getKey()))
                            .append(',')
                            .append(encode(entry.getValue().getValue()))
                            .append(',')
                            .append(encode(entry.getValue().getMetadata().getValue()));
        }
        return baggageString.toString();
    }

    private Baggage deserializeBaggage(String string) {
        BaggageBuilder builder = Baggage.empty().toBuilder();
        for (String entry : string.split(";")) {
            if (entry.isEmpty()) {
                continue;
            }
            String[] parts = entry.split(",", -1); // -1 -> keep trailing empty strings
            builder.put(decode(parts[0]),
                        decode(parts[1]),
                        BaggageEntryMetadata.create(decode(parts[2])));
        }
        return builder.build();
    }

    private String serializeSpan(SpanContext span) {
        StringBuffer spanString = new StringBuffer();
        spanString.append(span.getTraceId())
                        .append(';')
                        .append(span.getSpanId())
                        .append(';')
                        .append(span.getTraceFlags().asHex())
                        .append(';');
        boolean first = true;
        for (Entry<String, String> entry : span.getTraceState().asMap().entrySet()) {
            if (first) {
                spanString.append(',');
                first = false;
            }

            spanString.append(encode(entry.getKey()))
                            .append('=')
                            .append(encode(entry.getValue()));
        }

        return spanString.toString();
    }

    private Span deserializeSpan(String string) {
        String[] parts = string.split(";", -1);
        String traceId = decode(parts[0]);
        String spanId = decode(parts[1]);
        TraceFlags flags = TraceFlags.fromHex(decode(parts[2]), 0);

        TraceStateBuilder stateBuilder = TraceState.builder();
        for (String entry : parts[3].split(",")) {
            if (entry.isEmpty()) {
                continue;
            }
            String[] entryParts = entry.split("=");
            stateBuilder.put(decode(entryParts[0]),
                             decode(entryParts[1]));
        }

        SpanContext spanContext = SpanContext.create(traceId, spanId, flags, stateBuilder.build());
        return Span.wrap(spanContext);
    }

    private String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);//should never happen
        }
    }

    private String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);//should never happen
        }
    }

}
