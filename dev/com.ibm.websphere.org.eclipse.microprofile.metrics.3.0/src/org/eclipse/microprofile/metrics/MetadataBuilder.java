/*
 **********************************************************************
 * Copyright (c) 2018, 2020 Contributors to the Eclipse Foundation
 *               2018 Red Hat, Inc. and/or its affiliates
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

/**
 * The {@link Metadata} builder.
 *
 * All values are considered "not present" by default.
 * Name must be set before {@link #build()} is called.
 */
public class MetadataBuilder {

    private String name;

    private String displayName;

    private String description;

    private MetricType type;

    private String unit;

    MetadataBuilder(Metadata metadata) {
        this.name = metadata.getName();
        this.type = metadata.getTypeRaw();
        metadata.description().ifPresent(this::withDescription);
        metadata.unit().ifPresent(this::withUnit);
        metadata.displayName().ifPresent(this::withDisplayName);
    }

    public MetadataBuilder() {

    }

    /**
     * Sets the name. Does not accept null.
     *
     * @param name the name
     * @return the builder instance
     * @throws NullPointerException when name is null
     * @throws IllegalArgumentException when name is empty
     */
    public MetadataBuilder withName(String name) {
        this.name = Objects.requireNonNull(name, "name is required");
        if ("".equals(name)) {
            throw new IllegalArgumentException("Name must not be empty");
        }
        return this;
    }

    /**
     * Sets the displayName.
     *
     * @param displayName the displayName, empty string is considered as "not present" (null)
     * @return the builder instance
     */
    public MetadataBuilder withDisplayName(String displayName) {
        this.displayName = "".equals(displayName) ? null : displayName;
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description the name, empty string is considered as "not present" (null)
     * @return the builder instance
     */
    public MetadataBuilder withDescription(String description) {
        this.description = "".equals(description) ? null : description;
        return this;
    }

    /**
     * Sets the type.
     *
     * @param type the type, {@link MetricType#INVALID} is considered as "not present" (null)
     * @return the builder instance
     */
    public MetadataBuilder withType(MetricType type) {
        this.type = MetricType.INVALID == type ? null : type;
        return this;
    }

    /**
     * Sets the unit.
     *
     * @param unit the unit, {@link MetricUnits#NONE} is considered as "not present" (null)
     * @return the builder instance
     */
    public MetadataBuilder withUnit(String unit) {
        this.unit = MetricUnits.NONE.equals(unit) ? null : unit;
        return this;
    }


    /**
     * @return An object implementing {@link Metadata} from the provided properties
     * @throws IllegalStateException when either name is null
     */
    public Metadata build() {
        if (Objects.isNull(name)) {
            throw new IllegalStateException("Name is required");
        }

        return new DefaultMetadata(name, displayName, description, type, unit);
    }
}
