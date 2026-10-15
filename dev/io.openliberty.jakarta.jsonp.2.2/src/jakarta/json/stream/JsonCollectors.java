/*
 * Copyright (c) 2015, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package jakarta.json.stream;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collector;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.BiConsumer;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonException;

/**
 * This class contains some implementations of {@code java.util.stream.Collector} for accumulating
 * {@link JsonValue}s into {@link JsonArray} and {@link JsonObject}.
 *
 * @since 1.1
 */

public final class JsonCollectors {

    /**
     * No instantiation.
     */
    private JsonCollectors() {
    }

    /**
     * Constructs a {@code java.util.stream.Collector} that accumulates the input {@code JsonValue}
     * elements into a {@code JsonArray}.
     *
     * @return the constructed Collector
     */
    public static Collector<JsonValue, JsonArrayBuilder, JsonArray> toJsonArray() {
        return Collector.of(
                Json::createArrayBuilder,
                JsonArrayBuilder::add,
                JsonArrayBuilder::addAll,
                JsonArrayBuilder::build);
    }

    /**
     * Constructs a {@code java.util.stream.Collector} that accumulates the input {@code Map.Entry<String,JsonValue>}
     * elements into a {@code JsonObject}.
     *
     * @return the constructed Collector
     */
    public static Collector<Map.Entry<String, JsonValue>, JsonObjectBuilder, JsonObject> toJsonObject() {
        return Collector.of(
                Json::createObjectBuilder,
                (JsonObjectBuilder b, Map.Entry<String, JsonValue> v) -> b.add(v.getKey(), v.getValue()),
                JsonObjectBuilder::addAll,
                JsonObjectBuilder::build);
    }

    /**
     * Constructs a {@code java.util.stream.Collector} that accumulates the input {@code JsonValue}
     * elements into a {@code JsonObject}.  The name/value pairs of the {@code JsonObject} are computed
     * by applying the provided mapping functions.
     *
     * @param keyMapper a mapping function to produce names.
     * @param valueMapper a mapping function to produce values
     * @return the constructed Collector
     */
    public static Collector<JsonValue, JsonObjectBuilder, JsonObject>
                toJsonObject(Function<JsonValue, String> keyMapper,
                             Function<JsonValue, JsonValue> valueMapper) {
        return Collector.of(
                Json::createObjectBuilder,
                (b, v) -> b.add(keyMapper.apply(v), valueMapper.apply(v)),
                JsonObjectBuilder::addAll,
                JsonObjectBuilder::build,
                Collector.Characteristics.UNORDERED);
    }

    /**
     * Constructs a {@code java.util.stream.Collector} that implements a "group by" operation on the
     * input {@code JsonValue} elements. A classifier function maps the input {@code JsonValue}s to keys, and
     * the {@code JsonValue}s are partitioned into groups according to the value of the key.
     * A reduction operation is performed on the {@code JsonValue}s in each group, using the
     * downstream {@code Collector}. For each group, the key and the results of the reduction operation
     * become the name/value pairs of the resultant {@code JsonObject}.
     *
     * @param <T> the intermediate accumulation {@code JsonArrayBuilder} of the downstream collector
     * @param classifier a function mapping the input {@code JsonValue}s to a String, producing keys
     * @param downstream a {@code Collector} that implements a reduction operation on the
     *        {@code JsonValue}s in each group.
     * @return the constructed {@code Collector}
     */
    public static <T extends JsonArrayBuilder> Collector<JsonValue, Map<String, T>, JsonObject>
                groupingBy(Function<JsonValue, String> classifier,
                           Collector<JsonValue, T, JsonArray> downstream) {

        BiConsumer<Map<String, T>, JsonValue> accumulator =
            (map, value) -> {
                String key = classifier.apply(value);
                if (key == null) {
                    throw new JsonException("element cannot be mapped to a null key");
                }
                // Build a map of key to JsonArrayBuilder
                T arrayBuilder =
                    map.computeIfAbsent(key, v->downstream.supplier().get());
                // Add elements from downstream Collector to the arrayBuilder.
                downstream.accumulator().accept(arrayBuilder, value);
            };
        Function<Map<String, T>, JsonObject> finisher =
            map -> {
                // transform the map of name: JsonArrayBuilder to
                //                      name: JsonArray
                // using the downstream collector for reducing the JsonArray
                JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                map.forEach((k, v) -> {
                    JsonArray array = downstream.finisher().apply(v);
                    objectBuilder.add(k, array);
                });
                return objectBuilder.build();
            };
        BinaryOperator<Map<String, T>> combiner =
            (map1, map2) -> {
                map1.putAll(map2);
                return map1;
            };
        return Collector.of(HashMap::new, accumulator, combiner, finisher,
            Collector.Characteristics.UNORDERED);
    }

    /**
     * Constructs a {@code java.util.stream.Collector} that implements a "group by" operation on the
     * input {@code JsonValue} elements. A classifier function maps the input {@code JsonValue}s to keys, and
     * the {@code JsonValue}s are partitioned into groups according to the value of the key.
     * The {@code JsonValue}s in each group are added to a {@code JsonArray}.  The key and the
     * {@code JsonArray} in each group becomes the name/value pair of the resultant {@code JsonObject}.
     *
     * @param classifier a function mapping the input {@code JsonValue}s to a String, producing keys
     * @return the constructed {@code Collector}
     */
    public static Collector<JsonValue, Map<String, JsonArrayBuilder>, JsonObject>
                groupingBy(Function<JsonValue, String> classifier) {
        return groupingBy(classifier, toJsonArray());
    }
}

