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

import java.util.Arrays;

public abstract class PropertyArrayConfiguration<T> extends PropertyConfiguration {
    protected T[] values = null;

    public PropertyArrayConfiguration(String metricName, T[] values) {
        this.metricName = metricName;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format(this.getClass().getName() + "<Metric name: [%s]>; <values: %s>", metricName,
                             Arrays.toString(values));
    }

    public T[] getValues() {
        return values;
    }

}
