/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc.monitoring;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import componenttest.topology.impl.LibertyServer;

public class GrpcMetricsTestUtils {
    /**
     * Verifies the given metric by comparing the actual value with the given value
     *
     * @param server        - The server that is under test, this is used to get the port and host name.
     * @param metricName    - the metric to verify
     * @param expectedValue - the expected value
     * @return the actual value received from the Metrics endpoint
     */
    public static String checkMetric(LibertyServer server, String metricName, String expectedValue) {
        String metricValue = getMetric(server, metricName);
        if (metricValue == null || !metricValue.equals(expectedValue)) {
            fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", metricName, expectedValue, metricValue));
        }
        return metricValue;
    }

    /**
     * Verifies the given metric by comparing the actual value with the given value
     *
     * @param metricName    - the metric to verify
     * @param expectedValue - the expected value
     * @param hostname      - server hostname.
     * @param port          - server HTTP port.
     * @return the actual value received from the Metrics endpoint
     */
    public static String checkMetric(String metricName, String expectedValue, String hostname, int port) {
        String metricValue = getMetric(hostname, port, metricName);
        if (metricValue == null || !metricValue.equals(expectedValue)) {
            fail(String.format("Incorrect metric value [%s]. Expected [%s], got [%s]", metricName, expectedValue, metricValue));
        }
        return metricValue;
    }

    /**
     * Retrieves the given metric from the Metrics endpoint.
     *
     * @param server     - The server that is under test, this is used to get the port and host name.
     * @param metricName - the metric to retrieve
     * @return the value of the specified metric
     */
    protected static String getMetric(LibertyServer server, String metricName) {
        return getMetric(server.getHostname(), server.getHttpDefaultPort(), metricName);
    }

    /**
     * Retrieves the given metric from the Metrics endpoint.
     *
     * @param metricName - the metric to retrieve
     * @param hostname   - server hostname.
     * @param port       - server HTTP port.
     * @return the value of the specified metric
     */
    public static String getMetric(String hostname, int port, String metricName) {
        String metricValue = null;
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + hostname + ":" + port + metricName);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            if (retcode != 200) {
                fail("Bad return code from Metrics method call. Expected 200, got " + retcode);

                return null;
            }

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!line.startsWith("#")) {
                    String[] mertricAttr = line.split(" ");
                    if (mertricAttr.length > 0) {
                        metricValue = mertricAttr[mertricAttr.length - 1];
                        break;
                    }
                }
            }
        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        System.out.println(String.format("Metric [%s] value [%s].", metricName, metricValue));
        return metricValue;
    }
}
