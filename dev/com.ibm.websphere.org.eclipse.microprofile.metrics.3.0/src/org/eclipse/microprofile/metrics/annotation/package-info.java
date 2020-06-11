/*
 **********************************************************************
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
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
 * <p>
 * This package contains the annotations used for MicroProfile Metrics.
 *
 * <h2>Metric Annotation</h2>
 * <p>
 * The {@link org.eclipse.microprofile.metrics.annotation.Metric Metric}
 * annotation is used to provide metadata for the metric being injected. If a
 * metric with the same name exists in the
 * {@link org.eclipse.microprofile.metrics.MetricRegistry MetricRegistry}, the
 * metric is returned. Otherwise, a new metric is registered into the
 * MetricRegistry along with the metadata provided by the {@literal @}Metric
 * annotation.
 * <p>
 * For example,
 *
 * <pre>
 * <code>
 *     {@literal @}Inject
 *     {@literal @}Metric(name="histogram", description="The description")
 *     public Histogram histogram;
 * </code>
 * </pre>
 *
 * <h2>Interceptor Bindings</h2>
 * <p>
 * MicroProfile Metrics provides interceptor bindings which can be used to
 * instrument an application: {@literal @}Counted, {@literal @}Gauge,
 * {@literal @}Metered, {@literal @}Timed, {@literal @}SimplyTimed and {@literal @}ConcurrentGauge.
 * <p>
 * An example using {@literal @}Counted,
 *
 * <pre>
 * <code>
 *     {@literal @}Counted (name="visitorCount",
 *         description="The number of visitors to the application")
 *     public void visit () {
 *         ...
 *     }
 * </code>
 * </pre>
 * <p>
 * An example using {@literal @}Gauge,
 *
 * <pre>
 * <code>
 *     {@literal @}Gauge(name = "queueSize")
 *     public int getQueueSize() {
 *         return queue.size;
 *     }
 * </code>
 * </pre>
 *
 *
 * <h2>CDI Qualifier</h2>
 * <p>
 * The {@link org.eclipse.microprofile.metrics.annotation.RegistryType
 * RegistryType} is used to identify which <code>MetricRegistry</code> (Application, Base, or
 * Vendor) should be injected. By default, no <code>RegistryType</code> will
 * inject the application <code>MetricRegistry</code>.
 *
 * <pre>
 * <code>
 *      {@literal @}Inject
 *      {@literal @}RegistryType(type=MetricRegistry.Type.BASE)
 *      MetricRegistry baseRegistry;
 * </code>
 * </pre>
 *
 */
@org.osgi.annotation.versioning.Version("3.0")
package org.eclipse.microprofile.metrics.annotation;
