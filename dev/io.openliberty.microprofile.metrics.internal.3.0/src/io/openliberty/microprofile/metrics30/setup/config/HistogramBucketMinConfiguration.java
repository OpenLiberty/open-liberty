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

import java.util.ArrayDeque;
import java.util.Collection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HistogramBucketMinConfiguration extends PropertySingleValueConfiguration<Double> {

    private static final TraceComponent tc = Tr.register(HistogramBucketMinConfiguration.class);

    public HistogramBucketMinConfiguration(String metricName, Double value) {
        super(metricName, value);
    }

    public static Collection<HistogramBucketMinConfiguration> parse(String input) {

        ArrayDeque<HistogramBucketMinConfiguration> metricBucketMinMax = new ArrayDeque<HistogramBucketMinConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            HistogramBucketMinConfiguration metricBucketConfiguration = null;

            // metricGroup=<blank> == invalid
            if (keyValueSplit.length == 2) {
                if (keyValueSplit[1].matches(("[0-9]+[.]*[0-9]*"))) {
                    Double value = Double.parseDouble(keyValueSplit[1].trim());
                    metricBucketConfiguration = new HistogramBucketMinConfiguration(metricName, value);
                } else {
                    Tr.warning(tc, "invalidHistogramValueConfigured.warning.CWMMC0015W", new Object[] { keyValueSplit[1], MetricsConfigurationManager.MP_HISTOGRAM_BUCKET_PROP });

                }
            } else {
                //either no value.. or too many values through improper syntax
                continue;
            }

            // LIFO - right most configuration takes precedence
            if (metricBucketConfiguration != null) {
                metricBucketMinMax.addFirst(metricBucketConfiguration);
            }

        }
        return metricBucketMinMax;

    }

}
