/*******************************************************************************
 * Copyright (c) 2018,2025 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class MissingSslFeatureTests40 extends AbstractSpringTests {

    @Override
    public boolean expectApplicationSuccess() {
        return false;
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }

    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
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
    public void testMissingSslFeatureFor40() throws Exception {
        testMissingSslFeature();
    }

    public void testMissingSslFeature() throws Exception {
        assertNotNull("No error message was found for missing ssl feature ", server.waitForStringInLog("CWWKC0258E"));
    }

}
