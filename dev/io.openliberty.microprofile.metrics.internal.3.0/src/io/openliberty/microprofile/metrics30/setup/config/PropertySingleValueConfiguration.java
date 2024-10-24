/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.metrics30.setup.config;

public abstract class PropertySingleValueConfiguration<T> extends PropertyConfiguration {
    protected T value = null;

    public PropertySingleValueConfiguration(String metricName, T value) {
        this.metricName = metricName;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getName() + "<Metric name: [%s]>; <value: %s>", metricName,
                             value);
    }

    public T getValue() {
        return value;
    }
}
