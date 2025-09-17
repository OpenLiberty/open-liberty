/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 17)
public class MultiContextTests40 extends AbstractSpringTests {
    private static final int CHILD1_MAIN_PORT = 8081;
    private static final int CHILD1_ACTUATOR_PORT = 9991;
    private static final int CHILD2_MAIN_PORT = 8082;
    private static final int CHILD2_ACTUATOR_PORT = 9992;

    private static final String USE_DEFAULT_HOST = "useDefaultHostPorts";
    private static final String USE_DEFAULT_HOST_WITH_APP = "useDefaultHostWithAppPort";
    private static final String USE_APP_CONFIG = "useAppConfigPorts";

    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_MULTI_CONTEXT;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        String methodName = testName.getMethodName();
        Map<String, String> properties = new HashMap<>();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            properties.put("bvt.prop.HTTP_default", "-1");
            properties.put("bvt.prop.HTTP_default.secure", "-1");
        }
        return properties;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        String methodName = testName.getMethodName();
        if (methodName == null) {
            return true;
        }

        if (methodName.equals(USE_APP_CONFIG)) {
            return false;
        }

        return true;
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @Override
    public List<String> getExpectedWebApplicationEndpoints() {
        String methodName = testName.getMethodName();
        List<String> expectedEndpoints = new ArrayList<String>(super.getExpectedWebApplicationEndpoints());
        if (methodName != null) {

            if (methodName.equals(USE_DEFAULT_HOST)) {
                expectedEndpoints.add(ID_DEFAULT_HOST);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD1_ACTUATOR_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_ACTUATOR_PORT);

            } else if (methodName.equals(USE_DEFAULT_HOST_WITH_APP)) {
                // ID_DEFAULT_HOST was already added via call to super()
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD1_ACTUATOR_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_ACTUATOR_PORT);

            } else if (methodName.equals(USE_APP_CONFIG)) {
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD1_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD1_ACTUATOR_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + CHILD2_ACTUATOR_PORT);
            }
        }
        return expectedEndpoints;
    }

    @After
    public void stopOverrideServer() throws Exception {
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            super.stopServer(true, "CWWKT0015W");
        } else {
            super.stopServer();
        }
    }

    @Test
    public void useDefaultHostPorts() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD1_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");

        server.setHttpDefaultPort(CHILD2_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD2_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useDefaultHostWithAppPort() throws Exception {
        server.setHttpDefaultPort(CHILD1_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD1_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");

        server.setHttpDefaultPort(CHILD2_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD2_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useAppConfigPorts() throws Exception {
        server.setHttpDefaultPort(CHILD1_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD1_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");

        server.setHttpDefaultPort(CHILD2_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(CHILD2_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }
}
