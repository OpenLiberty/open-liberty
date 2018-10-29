/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import junit.framework.Assert;

@RunWith(FATRunner.class)
@Mode(FULL)
public class EnableSpringBootTraceTests extends CommonWebServerTests {

    private static final String TEST_ENABLE_TRACE_FOR_15 = "testEnableSpringBootTraceFor15";
    private static final String TEST_ENABLE_TRACE_FOR_20 = "testEnableSpringBootTraceFor20";

    @Override
    public Set<String> getFeatures() {
        String methodName = testName.getMethodName();
        Set<String> features = new HashSet<>(Arrays.asList("servlet-3.1"));
        if (TEST_ENABLE_TRACE_FOR_15.equals(methodName)) {
            features.add("springBoot-1.5");
        } else if (TEST_ENABLE_TRACE_FOR_20.equals(methodName)) {
            features.add("springBoot-2.0");
        } else {
            Assert.fail("Unknown test.");
        }
        return features;
    }

    @Override
    public String getApplication() {
        String methodName = testName.getMethodName();
        if (TEST_ENABLE_TRACE_FOR_15.equals(methodName)) {
            return SPRING_BOOT_15_APP_BASE;
        } else if (TEST_ENABLE_TRACE_FOR_20.equals(methodName)) {
            return SPRING_BOOT_20_APP_BASE;
        }
        Assert.fail("Unknown test.");
        return null;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("com.ibm.ws.logging.trace.specification", "*=audit=enabled:springboot=all");
        return properties;
    }

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();
        if (TEST_ENABLE_TRACE_FOR_15.equals(methodName) && !javaVersion.startsWith("1.")) {
            super.stopServer(true, "CWWKC0265W");
        } else {
            super.stopServer(true);
        }
    }

    @Test
    public void testEnableSpringBootTraceFor15() throws Exception {
        testBasicSpringBootApplication();
    }

    @Test
    public void testEnableSpringBootTraceFor20() throws Exception {
        testBasicSpringBootApplication();
    }

}
