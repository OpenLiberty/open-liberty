/*
 **********************************************************************
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
 *               2010-2013 Coda Hale, Yammer.com
 *
 * See the NOTICES file(s) distributed with this work for additional
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
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/
package org.eclipse.microprofile.metrics;

/**
 * A filter used to determine whether or not a metric should be reported, among other things.
 */
public interface MetricFilter {
    /**
     * Matches all metrics, regardless of type or {@link MetricID}.
     */
    MetricFilter ALL = (metricID, metric) -> true;

    /**
     * Returns {@code true} if the metric matches the filter; {@code false} otherwise.
     *
     * @param metricID the metric's {@link MetricID}
     * @param metric the metric
     * @return {@code true} if the metric matches the filter
     */
    boolean matches(MetricID metricID, Metric metric);
}
