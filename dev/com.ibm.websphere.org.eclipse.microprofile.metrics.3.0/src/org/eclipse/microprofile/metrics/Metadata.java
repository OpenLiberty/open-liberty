/*
 **********************************************************************
 * Copyright (c) 2017, 2018, 2020 Contributors to the Eclipse Foundation
 *               2017, 2018 Red Hat, Inc. and/or its affiliates
 *               and other contributors as indicated by the @author tags.
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

import java.util.Objects;
import java.util.Optional;

/**
 * Bean holding the metadata of one single metric.
 * <p>
 * The metadata contains:
 * <ul>
 * <li>
 * {@code Name}: (Required) The name of the metric.
 * </li>
 * <li>
 * {@code Display name}: (Optional) The display (friendly) name of the metric.
 * By default, it is set to the {@code Name}.
 * </li>
 * <li>
 * {@code Description}: (Optional) A human readable description of the metric.
 * </li>
 * <li>
 * {@code Type}: (Required) The type of the metric. See {@link MetricType}.
 * </li>
 * <li>
 * {@code Unit}: (Optional) The unit of the metric.
 * The unit may be any unit specified as a String or one specified in {@link MetricUnits}.
 * </li>
 * </ul>
 *
 * @author hrupp, Raymond Lam, Jan Bernitt
 */
public interface Metadata {

    /**
     * Returns the metric name.
     *
     * @return the metric name.
     */
    String getName();

    /**
     * Returns the display name if set, otherwise this method returns the metric name.
     *
     * @return the display name
     */
    String getDisplayName();

    Optional<String> displayName();

    /**
     * Returns the description of the metric if set, otherwise this method returns the empty {@link String}.
     *
     * @return the description
     */
    String getDescription();

    Optional<String> description();

    /**
     * Returns the String representation of the {@link MetricType}.
     *
     * @return the MetricType as a String
     * @see MetricType
     */
    String getType();

    /**
     * Returns the {@link MetricType} of the metric if set, otherwise it returns {@link MetricType#INVALID}
     *
     * @return the {@link MetricType}
     */
    MetricType getTypeRaw();

    /**
     * Returns the unit of this metric if set, otherwise this method returns {@link MetricUnits#NONE}
     *
     * @return the unit
     */
    String getUnit();

    Optional<String> unit();

    /**
     * Returns a new builder
     *
     * @return a new {@link MetadataBuilder} instance
     */
    static MetadataBuilder builder() {
        return new MetadataBuilder();
    }

    /**
     * Returns a new builder with the {@link Metadata} information
     *
     * @param metadata the metadata
     * @return a new {@link MetadataBuilder} instance with the {@link Metadata} values
     * @throws NullPointerException when metadata is null
     */
    static MetadataBuilder builder(Metadata metadata) {
        Objects.requireNonNull(metadata, "metadata is required");
        return new MetadataBuilder(metadata);
    }

}
