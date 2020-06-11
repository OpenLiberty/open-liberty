/*
 **********************************************************************
 * Copyright (c) 2017, 2018 Contributors to the Eclipse Foundation
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

/**
 * MicroProfile Metrics
 *
 * <h2>Rational</h2>
 *
 * <p>
 * To ensure reliable operation of software it is necessary to monitor essential
 * system parameters. There is already JMX as standard to expose metrics, but
 * remote-JMX is not easy to deal with and especially does not fit well in a
 * polyglot environment where other services are not running on the JVM. To
 * enable monitoring in an easy fashion, the MicroProfile Metrics specification
 * provides a standard to instrument an application with metrics and provides a
 * simple REST endpoint for integration with monitoring services.
 *
 * <h2>Adding Metrics</h2>
 * <p>
 * MicroProfile Metrics provides 6 different metric types that can be used to
 * instrument an application. Developers can create an accompanying
 * {@link org.eclipse.microprofile.metrics.Metadata Metadata} object to supply
 * the metric's name, description, display name, and units. Once the
 * metric and the metadata are registered against the application
 * {@link org.eclipse.microprofile.metrics.MetricRegistry MetricRegistry}, the
 * metrics will be available in the REST endpoints.
 *
 * <h2>Metric Types</h2>
 *
 * <p>
 * {@link org.eclipse.microprofile.metrics.Counter Counter} is used to measure
 * an increasing value.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     Counter count = metricRegistry.counter(metadata);
 *     count.inc();
 * </code>
 * </pre>
 *
 * <p>
 * {@link org.eclipse.microprofile.metrics.ConcurrentGauge ConcurrentGauge} is used
 * to monitor the number of concurrent invocations of a component.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     ConcurrentGauge cgauge = metricRegistry.concurrentGauge(metadata);
 *     cgauge.inc();
 *     // .. a block of code that can be executed by multiple threads at the same time
 *     cgauge.dec();
 * </code>
 * </pre>
 *
 * {@link org.eclipse.microprofile.metrics.Gauge Gauge} is used to provide the
 * immediate measurement of a value.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     Gauge&lt;Double&gt; temperature = new Gauge&lt;Double&gt;() {
 *         public Double getValue() {
 *             return getTemperature();
 *         }
 *     };
 *     metricRegistry.register(metadata, temperature);
 * </code>
 * </pre>
 *
 *
 * {@link org.eclipse.microprofile.metrics.Meter Meter} is used to measure the
 * frequency of an event.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     Meter meter = metricRegistry.meter(metadata);
 *     meter.mark();
 * </code>
 * </pre>
 *
 *
 * {@link org.eclipse.microprofile.metrics.Histogram Histogram} is used to
 * sample and compute the distribution of values
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     Histogram histogram = metricRegistry.histogram(metadata);
 *     histogram.update(score);
 * </code>
 * </pre>
 *
 * {@link org.eclipse.microprofile.metrics.Timer Timer} is used to measure the
 * duration of an event as well as the frequency of occurrence.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     Timer timer = metricRegistry.timer(metadata);
 *     Timer.Context context = timer.time();
 *
 *     ... // code that will be timed
 *
 *     context.close();
 * </code>
 * </pre>
 *
 * {@link org.eclipse.microprofile.metrics.SimpleTimer SimpleTimer} is used to measure the
 * duration of an event.
 * <p>
 * Example usage:
 *
 * <pre>
 * <code>
 *     SimpleTimer simpleTimer = metricRegistry.simpleTimer(metadata);
 *     SimpleTimer.Context context = simpleTimer.time();
 *
 *     ... // code that will be timed
 *
 *     context.close();
 * </code>
 * </pre>
 */
@org.osgi.annotation.versioning.Version("3.0")
package org.eclipse.microprofile.metrics;
