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

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class TimerBucketMaxConfiguration extends PropertySingleValueConfiguration<Duration> {

    private static final TraceComponent tc = Tr.register(TimerBucketMaxConfiguration.class);

    public TimerBucketMaxConfiguration(String metricName, Duration value) {
        super(metricName, value);
    }

    public static Collection<TimerBucketMaxConfiguration> parse(String input) {

        ArrayDeque<TimerBucketMaxConfiguration> sloMinConfigCollection = new ArrayDeque<TimerBucketMaxConfiguration>();

        if (input == null || input.length() == 0) {
            return null;
        }

        // not expecting backslashes?
        String[] metricValuePairs = input.split(";");

        // Individual metric name grouping and values
        for (String kvString : metricValuePairs) {

            String[] keyValueSplit = kvString.split("=");

            String metricName = keyValueSplit[0];

            TimerBucketMaxConfiguration sloMinConfiguration = null;
            Duration dur = null;
            // metricGroup=<blank> == invalid
            if (keyValueSplit.length == 2) {

                String s = keyValueSplit[1];

                if (s.matches("[0-9]+ms")) {
                    String val = s.substring(0, s.length() - 2);
                    dur = Duration.ofMillis(Long.parseLong(val));
                } else if (s.matches("[0-9]+s")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofSeconds(Long.parseLong(val));
                } else if (s.matches("[0-9]+m")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofMinutes(Long.parseLong(val));
                } else if (s.matches("[0-9]+h")) {
                    String val = s.substring(0, s.length() - 1);
                    dur = Duration.ofHours(Long.parseLong(val));
                } else if (s.matches("[0-9]+")) {
                    dur = Duration.ofMillis(Long.parseLong(s));
                } else {
                    Tr.warning(tc, "invalidTimerValueConfigured.warning.CWMMC0016W", new Object[] { s, MetricsConfigurationManager.MP_TIMER_BUCKET_PROP });
                }

            } else {
                //either no value.. or too many values through improper syntax
                continue;
            }

            if (dur != null) {
                sloMinConfiguration = new TimerBucketMaxConfiguration(metricName, dur);

                // LIFO - right most configuration takes precedence
                sloMinConfigCollection.addFirst(sloMinConfiguration);
            }

        }
        return sloMinConfigCollection;

    }

}
