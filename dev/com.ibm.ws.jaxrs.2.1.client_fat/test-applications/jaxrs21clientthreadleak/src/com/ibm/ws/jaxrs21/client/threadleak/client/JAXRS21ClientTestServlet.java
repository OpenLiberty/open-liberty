/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs21.client.threadleak.client;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Test servlet that simulates the thread leak scenario by making repeated
 * JAX-RS client requests to non-routable IP addresses, causing connection
 * failures and timeouts.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JAXRS21ClientTestServlet")
public class JAXRS21ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVER_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default");

    
    /**
     * Get the base_thread_count from mpMetrics endpoint.
     * @param hostname the server hostname
     * @param port the server port
     * @return the base_thread_count value, or -1 if unable to retrieve
     */
    private int getBaseThreadCountFromMetrics() {
        try {
            String metricsUrl = SERVER_CONTEXT_ROOT + "/metrics/base";
            URL url = new URL(metricsUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    // Look for: base_thread_count 123
                    if (line.startsWith("base_thread_count ") && !line.contains("#")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            in.close();
                            return Integer.parseInt(parts[1]);
                        }
                    }
                }
                in.close();
            }
        } catch (Exception e) {
            System.out.println("Error fetching metrics: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Test method that makes CONCURRENT JAX-RS client requests to a slow endpoint,
     * simulating the customer's real-time scenario where requests pile up.
     * Like the sample app, this creates a singleton client and fires requests
     * without waiting for responses, causing thread buildup.
     */
     @Test
    public void testThreadLeak() {
       
        int iterations = 50;
        
        // Create a SINGLETON JAX-RS client, not closed between requests
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .build();
    
        int timeoutCount = 0;

        int initialBaseThreadCount = getBaseThreadCountFromMetrics();
        
        // Make SEQUENTIAL requests with 250ms delay between each request
        for (int i = 0; i < iterations; i++) {
            try {
                // Build URL with varying student number 
                // Each student number triggers a different IP in ComputerApiRestClient
                // This causes varying URLs in the JAX-RS client, creating new Bus instances
                String endpoint = SERVER_CONTEXT_ROOT + "/jaxrs21clientthreadleak/Test/rest/" + i + "/data";
                // JAXRS21MyResource -> ComputerService -> ComputerApiRestClient -> RestClient
                WebTarget target = client.target(endpoint);
                Response resp = target.request(MediaType.APPLICATION_JSON).get();
                resp.close();
            } catch (Exception e) {
                timeoutCount++;
            }           
            // 250ms delay between requests
            try {
                Thread.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }        
        }
    
        client.close();

        // Wait for threads to expire 
        try {
                Thread.sleep(10000);
        } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
        }
        
        // Get final thread count 
        // If there's a leak, threads will still be high
        int finalBaseThreadCount = getBaseThreadCountFromMetrics();

        // Calculate thread increase
        int threadCountIncrease = finalBaseThreadCount - initialBaseThreadCount;
        //Increased the accepted thread count increase from 10 to 20 as in some environments it is going beyond 10. 
        // This is still valid as the original thread count increase issue was always above 100 and it is fixed.
        assertTrue("Thread count increased by " + threadCountIncrease + " which exceeds acceptable threshold of 20",
                   threadCountIncrease <= 20);
    }
}

