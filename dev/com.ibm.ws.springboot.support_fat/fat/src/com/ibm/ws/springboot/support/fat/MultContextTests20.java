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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MultContextTests20 extends AbstractSpringTests {
    private static final int CHILD1_MAIN_PORT = 8081;
    private static final int CHILD1_ACTUATOR_PORT = 9991;
    private static final int CHILD2_MAIN_PORT = 8082;
    private static final int CHILD2_ACTUATOR_PORT = 9992;

    private static final String USE_APP_CONFIG = "useAppConfigPorts";

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_MULTI_CONTEXT;
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

    @After
    public void stopOverrideServer() throws Exception {
        super.stopServer();
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
