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
package io.openliberty.microprofile.health40.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponse.Status;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.junit.Test;

import io.openliberty.microprofile.health30.impl.HealthCheck30ResponseBuilderImpl;
import io.openliberty.microprofile.health30.impl.HealthCheck30ResponseImpl;
import io.openliberty.microprofile.health40.spi.impl.HealthCheck40ResponseProviderImpl;

public class HealthCheck40ResponseTest {

    /**
     *
     */
    @Test
    public void testResponseNamed() throws Exception {
        final String name = "success-test";

        HealthCheckResponseBuilder builder = HealthCheckResponse.named(name);
        HealthCheckResponse response = builder.build();
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
    *
    */
    @Test
    public void testResponseBuilder() throws Exception {
        final String name = "success-test";

        HealthCheckResponseBuilder builder = HealthCheckResponse.builder();
        builder.name(name);
        HealthCheckResponse response = builder.build();
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
     *
     */
    @Test
    public void testSetProvider() throws Exception {
        final String name = "success-test";

        HealthCheckResponseProvider provider = new HealthCheck40ResponseProviderImpl();
        HealthCheckResponse.setResponseProvider(provider);
        HealthCheckResponseBuilder builder = provider.createResponseBuilder();
        builder.name(name);
        HealthCheckResponse response = builder.build();
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
     *
     */
    @Test
    public void testGetNameWithResponse() throws Exception {
        final String name = "success-test";

        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, null, null);
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
     *
     */
    @Test
    public void testGetNameWithProvider() throws Exception {
        final String name = "success-test";

        HealthCheckResponseProvider provider = new HealthCheck40ResponseProviderImpl();
        HealthCheckResponseBuilder builder = provider.createResponseBuilder();
        builder = builder.name(name);
        HealthCheckResponse response = builder.build();
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
     *
     */
    @Test
    public void testGetNameWithBuilder() throws Exception {
        final String name = "success-test";

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.name(name);
        HealthCheckResponse response = builder.build();
        String testName = response.getName();

        assertEquals(name, testName);
    }

    /**
     *
     */
    @Test
    public void testWithDataWithString() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", "first-value");
        attribute.put("second-key", "second-value");
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.withData("first-key", "first-value");
        builder = builder.withData("second-key", "second-value");
        HealthCheckResponse response = builder.build();
        Optional<Map<String, Object>> testData = response.getData();

        assertEquals(data, testData);

    }

    /**
     *
     */
    @Test
    public void testGetDataWithString2() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", "first-value");
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, null, data);
        Optional<Map<String, Object>> testData = response.getData();
        assertEquals(data, testData);
    }

    /**
     *
     */
    @Test
    public void testWithDataWithLong() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", (long) 15000);
        attribute.put("first-key", (long) 5000);
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.withData("first-key", 15000);
        builder = builder.withData("first-key", 5000);
        HealthCheckResponse response = builder.build();
        Optional<Map<String, Object>> testData = response.getData();

        assertEquals(data, testData);
    }

    /**
     *
     */
    @Test
    public void testGetDataWithLong2() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", 15000);
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, null, data);
        Optional<Map<String, Object>> testData = response.getData();

        assertEquals(data, testData);
    }

    /**
     *
     */
    @Test
    public void testGetDataWithBooleanUp() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", false);
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.withData("first-key", false);
        HealthCheckResponse response = builder.build();
        Optional<Map<String, Object>> testData = response.getData();

        assertEquals(data, testData);
    }

    /**
     *
     */
    @Test
    public void testGetDataWithBooleanDown() throws Exception {
        final String name = "success-test";

        Map<String, Object> attribute = new HashMap<String, Object>();
        attribute.put("first-key", false);
        final Optional<Map<String, Object>> data = Optional.of(attribute);

        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, null, data);
        Optional<Map<String, Object>> testData = response.getData();

        assertEquals(data, testData);
    }

    /**
     *
     */
    @Test
    public void testGetStatusUp() throws Exception {

        final String name = "success-test";
        final Status status = Status.UP;

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.status(true);
        HealthCheckResponse response = builder.build();
        Status testStatus = response.getStatus();

        assertEquals(status, testStatus);
    }

    /**
     *
     */
    @Test
    public void testGetStatusDown() throws Exception {

        final String name = "success-test";
        final Status status = Status.DOWN;

        HealthCheckResponse response = new HealthCheck30ResponseImpl(name, status, null);
        Status testStatus = response.getStatus();
        assertEquals(status, testStatus);
    }

    /**
     *
     */
    @Test
    public void testUp() throws Exception {

        final String name = "success-test";
        final Status status = Status.UP;

        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        builder = builder.up();
        HealthCheckResponse response = builder.build();
        Status testStatus = response.getStatus();

        assertEquals(status, testStatus);
    }

}
