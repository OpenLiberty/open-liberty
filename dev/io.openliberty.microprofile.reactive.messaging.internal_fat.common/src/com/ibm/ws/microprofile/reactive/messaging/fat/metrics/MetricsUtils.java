/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.reactive.messaging.fat.metrics;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
public class MetricsUtils {

    /**
     *
     * This method is used to get the Reactive Messaging metrics (message counts) from all channels of
     * a particular application.
     *
     * @param The URL of the application
     * @return HashMap of each Reactive Messaging channel and it's corresponding message count
     */
    public static HashMap<String, Integer> getReactiveMessagingMetrics(URL url) throws Exception {
        // Returns the MicroProfile Reactive Messaging metrics from the /metrics endpoint
        HttpURLConnection response = HttpUtils.getHttpConnection(url, 200, null, 30, HTTPRequestMethod.GET, null, null);
        HashMap<String, Integer> metrics = new HashMap<String, Integer>();
        response.setReadTimeout(30 * 1000);
        InputStream result = response.getInputStream();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(result))) {
            String currentLine;

            // filter the /metrics output for reactive messaging metrics
            while ((currentLine = in.readLine()) != null) {
                if (currentLine.contains("mp_messaging_message_count_total{channel=")) {
                    String channelString = currentLine.split("channel=\"")[1].replace(",mp_scope=\"base\",", "");
                    // the metric counter is always a whole number (int)
                    // extract channel name and counter total from response line
                    metrics.put(channelString.split("\"} ")[0], (int) Float.parseFloat(channelString.split("\"} ")[1]));
                }
            }
        }

        return metrics;
    }

}
