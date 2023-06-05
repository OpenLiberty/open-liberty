/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.utils.zipkin;

import static java.time.temporal.ChronoUnit.MICROS;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * Represents a Span retrieved from the Zipkin trace server
 */
public class ZipkinSpan {
    private final JsonObject json;

    public ZipkinSpan(JsonObject json) {
        this.json = json;
    }

    /**
     * @return the id
     */
    public String getId() {
        return json.getString("id", null);
    }

    /**
     * @return the traceId
     */
    public String getTraceId() {
        return json.getString("traceId", null);
    }

    /**
     * @return the kind
     */
    public String getKind() {
        return json.getString("kind", null);
    }

    /**
     * @return the parentId
     */
    public String getParentId() {
        return json.getString("parentId", null);
    }

    /**
     * @return the name
     */
    public String getName() {
        return json.getString("name", null);
    }

    /**
     * @return the timestamp
     */
    public Instant getTimestamp() {
        JsonNumber timestampMicros = json.getJsonNumber("timestamp");
        if (timestampMicros == null) {
            return null;
        }
        return microsToInstant(timestampMicros.longValue());
    }

    /**
     * @return the duration
     */
    public Duration getDuration() {
        JsonNumber durationMicros = json.getJsonNumber("duration");
        if (durationMicros == null) {
            return null;
        }
        return Duration.of(durationMicros.longValue(), MICROS);
    }

    /**
     * @return the tags as a map
     */
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        JsonObject tagObject = json.getJsonObject("tags");
        if (tagObject != null) {
            tagObject.forEach((k, v) -> tags.put(k, ((JsonString) v).getString()));
        }
        return tags;
    }

    /**
     * @return the json
     */
    public JsonObject getRawObject() {
        return json;
    }

    /**
     * @return the annotations
     */
    public List<Annotation> getAnnotations() {
        JsonArray jsonAnnotations = json.getJsonArray("annotations");
        if (jsonAnnotations == null) {
            return Collections.emptyList();
        }

        return jsonAnnotations.stream()
                              .map(a -> {
                                  JsonObject jsonAnnotation = a.asJsonObject();
                                  Annotation annotation = new Annotation();
                                  annotation.value = jsonAnnotation.getString("value");
                                  annotation.timestamp = microsToInstant(jsonAnnotation.getJsonNumber("timestamp").longValue());
                                  return annotation;
                              })
                              .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return json.toString();
    }

    public static class Annotation {
        private Instant timestamp;
        private String value;

        /**
         * @return the timestamp
         */
        public Instant getTimestamp() {
            return timestamp;
        }

        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
    }

    private static Instant microsToInstant(long micros) {
        long seconds = micros / 1_000_000;
        long nanos = (micros % 1_000_000) * 1000;
        return Instant.ofEpochSecond(seconds, nanos);
    }
}