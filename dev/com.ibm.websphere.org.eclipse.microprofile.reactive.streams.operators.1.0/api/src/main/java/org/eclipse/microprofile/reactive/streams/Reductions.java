/*******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.eclipse.microprofile.reactive.streams;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

/**
 * Reduction utilities that convert arguments supplied to reduce methods on the builders to Collectors.
 */
final class Reductions {

    private Reductions() {
    }

    static <T> Collector<T, ?, Optional<T>> reduce(BinaryOperator<T> reducer) {

        return Collector.of(Reduction<T>::new,
            (r, t) -> {
                if (r.value == null) {
                    r.value = t;
                }
                else {
                    r.value = reducer.apply(r.value, t);
                }
            },
            (r, s) -> {
                if (r.value == null) {
                    return r.replace(s.value);
                }
                else if (s.value != null) {
                    return r.replace(reducer.apply(r.value, s.value));
                }
                else {
                    return r;
                }
            },
            r -> Optional.ofNullable(r.value)
        );
    }

    static <T> Collector<T, ?, T> reduce(T identity, BinaryOperator<T> reducer) {

        return Collector.of(() -> new Reduction<>(identity),
            (r, t) -> r.value = reducer.apply(r.value, t),
            (r, s) -> r.replace(reducer.apply(r.value, s.value)),
            r -> r.value
        );
    }

    static <T, S> Collector<T, ?, S> reduce(S identity,
                                            BiFunction<S, ? super T, S> accumulator,
                                            BinaryOperator<S> combiner) {

        return Collector.of(() -> new Reduction<>(identity),
            (r, t) -> r.value = accumulator.apply(r.value, t),
            (r, s) -> r.replace(combiner.apply(r.value, s.value)),
            r -> r.value
        );
    }

    private static class Reduction<T> {
        private T value;

        Reduction() {
        }

        Reduction(T value) {
            this.value = value;
        }

        Reduction<T> replace(T newValue) {
            this.value = newValue;
            return this;
        }
    }

}
