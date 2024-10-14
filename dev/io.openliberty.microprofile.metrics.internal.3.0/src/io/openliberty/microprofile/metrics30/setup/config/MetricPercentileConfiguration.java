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

public class MetricPercentileConfiguration extends PropertyArrayConfiguration<Double> {
    private boolean isDisabled = false;

    private static final TraceComponent tc = Tr.register(MetricPercentileConfiguration.class);

    public MetricPercentileConfiguration(String metricName, Double[] percentileValues) {
        super(metricName, percentileValues);
    }

    public MetricPercentileConfiguration(String metricName, boolean isDisabled) {
        super(metricName, null);
        this.isDisabled = isDisabled;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    /**
     *
     * Parse the `mp.metrics.distribution.percentile` property.
     * syntax of {@code <metric_name>=<value-1>,<value-2>,...,<value-n>}
     * No values supplied to a metric name disables percentile output.
     * Can use wild card `*` at the end of metric name (e.g. demo.app.*)
     *
     * @param input MP Config value
     * @return Collection of {@link MetricPercentileConfiguration} objects
     */

    public static Collection<MetricPercentileConfiguration> parseMetricPercentiles(String input) {

        ArrayDeque<MetricPercentileConfiguration> metricPercentileConfigCollection = new ArrayDeque<MetricPercentileConfiguration>();

        if (input == null || input.length() == 0) {
            // no input is the same as disabling all
            input = "*=";
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            MetricPercentileConfiguration metricPercentileConfiguration;

            // empty value - disabled
            if (keyValueSplit.length == 1) {
                metricPercentileConfiguration = new MetricPercentileConfiguration(metricName, true);
            } else {
                // Parse values of percentile - ensure value is 0 <= x <= 1.0
                Double[] percentileValues = Arrays.asList(keyValueSplit[1].split(",")).stream().map(s -> {

                    if (s.matches("[0][.][0-9]+")) {
                        return Double.parseDouble(s);
                    } else {
                        Tr.warning(tc, "invalidPercentileValueConfigured.warning.CWMMC0017W", new Object[] { s, MetricsConfigurationManager.MP_PERCENTILES_PROP });

                        return null;
                    }

                }).filter(d -> d != null && d >= 0.0 && d <= 1.0).toArray(Double[]::new);

                Arrays.sort(percentileValues);

                metricPercentileConfiguration = new MetricPercentileConfiguration(metricName, percentileValues);
            }

            // LIFO - right most configuration takes precedence
            metricPercentileConfigCollection.addFirst(metricPercentileConfiguration);
        }
        return metricPercentileConfigCollection;

    }

}
