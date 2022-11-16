/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;

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
public class MissingSslFeatureTests extends AbstractSpringTests {

    private static final String TEST_MISSING_SSL_FOR_20 = "testMissingSslFeatureFor20";

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Override
    public String getApplication() {
        String methodName = testName.getMethodName();
        if (TEST_MISSING_SSL_FOR_20.equals(methodName)) {
            return SPRING_BOOT_20_APP_BASE;
        }
        Assert.fail("Unknown test.");
        return null;
    }

    @Override
    public Set<String> getFeatures() {
        String methodName = testName.getMethodName();
        Set<String> features = new HashSet<>(Arrays.asList("servlet-3.1"));
        if (TEST_MISSING_SSL_FOR_20.equals(methodName)) {
            features.add("springBoot-2.0");
        } else {
            Assert.fail("Unknown test.");
        }
        return features;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("server.ssl.key-store", "classpath:server-keystore.jks");
        properties.put("server.ssl.key-store-password", "secret");
        properties.put("server.ssl.key-password", "secret");
        properties.put("server.ssl.trust-store", "classpath:server-truststore.jks");
        properties.put("server.ssl.trust-store-password", "secret");
        return properties;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true, "CWWKC0258E", "CWWKZ0002E");
    }

    @Test
    public void testMissingSslFeatureFor20() throws Exception {
        testMissingSslFeature();
    }

    public void testMissingSslFeature() throws Exception {
        assertNotNull("No error message was found for missing ssl feature ", server.waitForStringInLog("CWWKC0258E"));
    }

}
