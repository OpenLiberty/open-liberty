/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class CreateJsonStringTest {

    @Test
    public void testCreateWithEmptyObjectInMap() {
        Map<String, String> data = new HashMap<>();

        data.put("", "");

        String jsonData = new CVEServiceClient().buildJsonString(data);

        assertEquals("{\"\": \"\"}", jsonData);
    }

    @Test
    public void testCreateWithEmptyIFixes() {
        Map<String, String> data = new HashMap<>();

        data.put("id", "TEST123test");
        data.put("productEdition", "Open");
        data.put("productVersion", "24.0.0.4");
        data.put("features", String.join(",",
                "componenttest-2.0,jsonb-3.0,mpRestClient-3.0,restfulWS-3.1,timedexit-1.0,jndi-1.0,jsonp-2.1,restfulWSClient-3.1,cdi-4.0,mpConfig-3.0"));
        data.put("javaVendor", "ibm corporation");
        data.put("javaVersion", "17.0.8+7");
        data.put("iFixes", String.join(",", ""));

        String jsonData = new CVEServiceClient().buildJsonString(data);

        assertEquals(
                "{\"productEdition\": \"Open\", \"features\": [\"componenttest-2.0\", \"jsonb-3.0\", \"mpRestClient-3.0\", \"restfulWS-3.1\", \"timedexit-1.0\", \"jndi-1.0\", \"jsonp-2.1\", \"restfulWSClient-3.1\", \"cdi-4.0\", \"mpConfig-3.0\"], \"productVersion\": \"24.0.0.4\", \"iFixes\": [], \"javaVersion\": \"17.0.8+7\", \"id\": \"TEST123test\", \"javaVendor\": \"ibm corporation\"}",
                jsonData);
    }

    @Test
    public void testCreateWithExpectedMap() {
        Map<String, String> data = new HashMap<>();

        data.put("id", "TEST123test");
        data.put("productEdition", "Open");
        data.put("productVersion", "24.0.0.4");
        data.put("features", String.join(",",
                "componenttest-2.0,jsonb-3.0,mpRestClient-3.0,restfulWS-3.1,timedexit-1.0,jndi-1.0,jsonp-2.1,restfulWSClient-3.1,cdi-4.0,mpConfig-3.0"));
        data.put("javaVendor", "ibm corporation");
        data.put("javaVersion", "17.0.8+7");
        data.put("iFixes", String.join(",", "ifix1,ifix2,ifix3"));

        String jsonData = new CVEServiceClient().buildJsonString(data);

        assertEquals(
                "{\"productEdition\": \"Open\", \"features\": [\"componenttest-2.0\", \"jsonb-3.0\", \"mpRestClient-3.0\", \"restfulWS-3.1\", \"timedexit-1.0\", \"jndi-1.0\", \"jsonp-2.1\", \"restfulWSClient-3.1\", \"cdi-4.0\", \"mpConfig-3.0\"], \"productVersion\": \"24.0.0.4\", \"iFixes\": [\"ifix1\", \"ifix2\", \"ifix3\"], \"javaVersion\": \"17.0.8+7\", \"id\": \"TEST123test\", \"javaVendor\": \"ibm corporation\"}",
                jsonData);
    }

}
