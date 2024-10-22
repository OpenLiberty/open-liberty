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
import java.util.Arrays;
import java.util.Collection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class HistogramBucketConfiguration extends PropertyArrayConfiguration<Double> {

    private static final TraceComponent tc = Tr.register(HistogramBucketConfiguration.class);

    public HistogramBucketConfiguration(String metricName, Double[] values) {
        super(metricName, values);

    }

    public static Collection<HistogramBucketConfiguration> parse(String input) {

        ArrayDeque<HistogramBucketConfiguration> metricBucketConfiCollection = new ArrayDeque<HistogramBucketConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            HistogramBucketConfiguration metricBucketConfiguration = null;

            /*
             * <metricName>=<blank> --> invalid
             * Nothing happens and metric name and empty-value is not recorded
             */
            if (keyValueSplit.length == 1) {
                continue;
            } else {
                // Parse values of buckets
                Double[] bucketValues = Arrays.asList(keyValueSplit[1].split(",")).stream().map(s -> {
                    if (s.matches("[0-9]+[.]*[0-9]*")) {
                        return Double.parseDouble(s);
                    } else {
                        Tr.warning(tc, "invalidHistogramValueConfigured.warning.CWMMC0015W", new Object[] { s, MetricsConfigurationManager.MP_HISTOGRAM_BUCKET_PROP });
                        return null;
                    }
                }).filter(x -> x != null).toArray(Double[]::new);

                Arrays.sort(bucketValues);

                metricBucketConfiguration = new HistogramBucketConfiguration(metricName, bucketValues);
            }

            // LIFO - right most configuration takes precedence
            metricBucketConfiCollection.addFirst(metricBucketConfiguration);
        }
        return metricBucketConfiCollection;

    }

}
