/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.observability.telemetry;

import static java.util.stream.Collectors.reducing;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;

/**
 * Uses a list of expected changes to compare a current metric with the same metric captured previously
 * <p>
 * Reports whether any of the expected changes have not occurred, or if any unexpected changes have occurred.
 *
 * @param <POINT> The metric data point type
 * @param <METRIC> The metric type, which holds many points
 */
public class MetricComparator<POINT, METRIC> {

    private final METRIC newMetric;
    private final Function<METRIC, Stream<POINT>> getPoints;
    private final List<ExpectedChange<POINT>> expectedChanges = new ArrayList<>();
    private final Supplier<POINT> emptyPointSupplier;
    private final BiPredicate<POINT, POINT> isSamePoint;
    private final BiConsumer<POINT, POINT> assertSameData;

    record ExpectedChange<T>(Predicate<T> selector, BiConsumer<T, T> assertion) {};

    public static MetricComparator<HistogramPointData, MetricData> compareOperationDuration(McpMetricReader reader) {
        return compareHistogram(reader.getMetricData("mcp.server.operation.duration").get());
    }

    public static MetricComparator<HistogramPointData, MetricData> compareSessionDuration(McpMetricReader reader) {
        return compareHistogram(reader.getMetricData("mcp.server.session.duration").get());
    }

    public static MetricComparator<HistogramPointData, MetricData> compareHistogram(MetricData data) {
        return new MetricComparator<>(data,
                                      metric -> metric.getHistogramData().getPoints().stream(),
                                      () -> EMPTY_HISTOGRAM_POINT,
                                      (a, b) -> a.getAttributes().equals(b.getAttributes()),
                                      (a, b) -> assertEquals("Metric count is not the same for attributes: " + b.getAttributes(), a.getCount(), b.getCount()));
    }

    /**
     * Create a metric comparator for comparing a current metric with an old metric.
     *
     * @param newMetric the current metric
     * @param getPoints a function to obtain a stream of points from the metric
     * @param emptyPointSupplier a supplier of an "empty" point, to be used for comparison when a point in the current metric has no corresponding point in the old metric
     * @param isSamePoint predicate to check whether a point from the current metric is for the same data as a point from the old metric
     * @param assertSameData consumer to assert that two points, for which {@code isSamePoint} returns true have the same data values. Throws an assertion error if they do not.
     */
    public MetricComparator(METRIC newMetric,
                            Function<METRIC, Stream<POINT>> getPoints,
                            Supplier<POINT> emptyPointSupplier,
                            BiPredicate<POINT, POINT> isSamePoint,
                            BiConsumer<POINT, POINT> assertSameData) {
        this.newMetric = newMetric;
        this.getPoints = m -> m == null ? Stream.empty() : getPoints.apply(m);
        this.emptyPointSupplier = emptyPointSupplier;
        this.isSamePoint = isSamePoint;
        this.assertSameData = assertSameData;
    }

    /**
     * Add an expected change
     *
     * @param selector a predicate which will select a single data point
     * @param assertion a function which asserts the how the metric has changed, receives the old data point and the new data point as arguments
     * @return this comparator
     */
    public MetricComparator<POINT, METRIC> expectChange(Predicate<POINT> selector, BiConsumer<POINT, POINT> assertion) {
        expectedChanges.add(new ExpectedChange<>(selector, assertion));
        return this;
    }

    /**
     * Runs the comparison against an old set of metric data
     * <p>
     * Asserts that all the expected changes have occurred.
     * <p>
     * Asserts that no other changes have occurred.
     *
     * @param oldMetric the old metric data
     */
    public void runCompareAgainst(METRIC oldMetric) {
        // For each expected change, find the data points and assert that the change has occurred
        for (var expectation : expectedChanges) {
            POINT newPoint = getPoints.apply(newMetric)
                                      .filter(expectation.selector())
                                      .collect(toOptional())
                                      .orElseThrow(() -> new AssertionError("No data point found for expected change"));

            POINT oldPoint = getPoints.apply(oldMetric)
                                      .filter(p -> isSamePoint.test(p, newPoint))
                                      .collect(toOptional())
                                      .orElseGet(emptyPointSupplier);

            expectation.assertion().accept(oldPoint, newPoint);
        }

        // Build a predicate which matches any point not part of an expected change
        Predicate<POINT> nonExpectedSelector = expectedChanges.stream()
                                                              .map(ExpectedChange::selector) // Get each selector
                                                              .map(Predicate::not) // Invert it
                                                              .collect(reducing(Predicate::and)) // AND them all together
                                                              .orElse(t -> true); // If there were no expected changes, match everything

        // For each other new data point without an expected change,
        // check that there's a matching old point and that the data has not changed
        getPoints.apply(newMetric)
                 .filter(nonExpectedSelector)
                 .forEach(pNew -> {
                     // Find matching old point
                     POINT pOld = getPoints.apply(oldMetric)
                                           .filter(p -> isSamePoint.test(p, pNew))
                                           .collect(toOptional())
                                           .orElseThrow(() -> new AssertionError("No matching data old point for " + pNew));
                     // Assert data is unchanged
                     assertSameData.accept(pOld, pNew);
                 });
    }

    public static BiConsumer<HistogramPointData, HistogramPointData> countIncreasedBy(int increment) {
        return (pOld, pNew) -> assertEquals("Wrong count for: " + pNew.getAttributes(), pOld.getCount() + increment, pNew.getCount());
    }

    /**
     * A dummy empty histogram data point that we can use to compare newly created histogram data points against
     */
    private static final HistogramPointData EMPTY_HISTOGRAM_POINT = new HistogramPointData() {

        @Override
        public long getStartEpochNanos() {
            return 0;
        }

        @Override
        public long getEpochNanos() {
            return 0;
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.empty();
        }

        @Override
        public boolean hasMin() {
            return false;
        }

        @Override
        public boolean hasMax() {
            return false;
        }

        @Override
        public double getSum() {
            return 0;
        }

        @Override
        public double getMin() {
            return 0;
        }

        @Override
        public double getMax() {
            return 0;
        }

        @Override
        public List<DoubleExemplarData> getExemplars() {
            return List.of();
        }

        @Override
        public List<Long> getCounts() {
            return List.of(0L, 0L);
        }

        @Override
        public long getCount() {
            return 0;
        }

        @Override
        public List<Double> getBoundaries() {
            return List.of(1.0);
        }
    };

    /**
     * Returns a collector that converts a stream into an optional
     * <p>
     * Asserts that the stream contains zero or one elements
     *
     * @param <T> the stream element type
     * @return a collector that converts a stream into an optional
     */
    private static <T> Collector<T, ?, Optional<T>> toOptional() {
        return Collectors.mapping(Optional::ofNullable,
                                  Collectors.reducing(Optional.empty(), (a, b) -> {
                                      if (a.isEmpty()) {
                                          return b;
                                      } else {
                                          if (b.isEmpty()) {
                                              return a;
                                          } else {
                                              throw new AssertionError("Found more than one item");
                                          }
                                      }
                                  }));
    }

}
