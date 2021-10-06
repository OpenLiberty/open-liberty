/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.HttpEndpoint;
import com.ibm.websphere.simplicity.config.KeyStore;
import com.ibm.websphere.simplicity.config.SSL;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.SpringBootApplication;
import com.ibm.websphere.simplicity.config.VirtualHost;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class ConfigActuatorXMLOverrideTests20 extends AbstractSpringTests {
    private static final int APP_MAIN_PORT = 8095;
    private static final int APP_ACTUATOR_PORT = 8096;

    private static final int OVERRIDE_MAIN_PORT = 9095;
    private static final int OVERRIDE_ACTUATOR_PORT = 9096;

    private static final String DEFAULT_MAIN_CONFIG_ACTUATOR = "useDefaultHostForMainConfigActuatorPorts";
    private static final String DEFAULT_APP_PORT_MAIN_CONFIG_ACTUATOR = "useDefaultHostWithAppPortForMainConfigActuatorPorts";
    private static final String CONFIG_MAIN_CONFIG_ACTUATOR = "useConfigForMainAndActuatorPorts";
    private static final String OVERRIDE_MAIN_OVERRIDE_ACTUATOR = "useOverrideForMainAndActuatorPorts";

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_ACTUATOR;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
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

        if (methodName.equals(CONFIG_MAIN_CONFIG_ACTUATOR) || //
            methodName.equals(OVERRIDE_MAIN_OVERRIDE_ACTUATOR)) {
            return false;
        }

        return true;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        List<String> appArgs = appConfig.getApplicationArguments();
        appArgs.add("--server.port=" + APP_MAIN_PORT);
        appArgs.add("--endpoints.sensitive=false");

        String methodName = testName.getMethodName();
        if (methodName == null) {
            return;
        }

        if (methodName.equals(DEFAULT_MAIN_CONFIG_ACTUATOR) || //
            methodName.equals(CONFIG_MAIN_CONFIG_ACTUATOR) || //
            methodName.equals(OVERRIDE_MAIN_OVERRIDE_ACTUATOR) ||
            methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            appArgs.add("--management.server.port=" + APP_ACTUATOR_PORT);
        }
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        String methodName = testName.getMethodName();
        if (methodName == null) {
            return;
        }
        List<VirtualHost> virtualHosts = config.getVirtualHosts();
        virtualHosts.clear();
        List<HttpEndpoint> endpoints = config.getHttpEndpoints();
        endpoints.clear();
        List<SSL> ssls = config.getSsls();
        ssls.clear();
        List<KeyStore> keystores = config.getKeyStores();
        keystores.clear();

        if (methodName.equals(OVERRIDE_MAIN_OVERRIDE_ACTUATOR)) {
            VirtualHost mainHost = new VirtualHost();
            virtualHosts.add(mainHost);
            mainHost.setId(ID_VIRTUAL_HOST + APP_MAIN_PORT);
            mainHost.getHostAliases().add("*:" + OVERRIDE_MAIN_PORT);

            HttpEndpoint mainEndpoint = new HttpEndpoint();
            endpoints.add(mainEndpoint);
            mainEndpoint.setHttpPort(Integer.toString(OVERRIDE_MAIN_PORT));
            mainEndpoint.setId("main");

            VirtualHost actuatorHost = new VirtualHost();
            virtualHosts.add(actuatorHost);
            actuatorHost.setId(ID_VIRTUAL_HOST + APP_ACTUATOR_PORT);
            actuatorHost.getHostAliases().add("*:" + OVERRIDE_ACTUATOR_PORT);

            HttpEndpoint actuatorEndpoint = new HttpEndpoint();
            endpoints.add(actuatorEndpoint);
            actuatorEndpoint.setHttpPort(Integer.toString(OVERRIDE_ACTUATOR_PORT));
            actuatorEndpoint.setId("actuator");
        }
    }

    @Override
    public List<String> getExpectedWebApplicationEndpoints() {
        String methodName = testName.getMethodName();
        List<String> expectedEndpoints = new ArrayList<String>(super.getExpectedWebApplicationEndpoints());
        if (methodName != null) {

            if (methodName.equals(DEFAULT_MAIN_CONFIG_ACTUATOR)) {
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_ACTUATOR_PORT);

            } else if (methodName.equals(DEFAULT_APP_PORT_MAIN_CONFIG_ACTUATOR)) {
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_ACTUATOR_PORT);

            } else if (methodName.equals(CONFIG_MAIN_CONFIG_ACTUATOR)) {
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_ACTUATOR_PORT);

            } else if (methodName.equals(OVERRIDE_MAIN_OVERRIDE_ACTUATOR)) {
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_MAIN_PORT);
                expectedEndpoints.add(ID_VIRTUAL_HOST + APP_ACTUATOR_PORT);
            }
        }
        return expectedEndpoints;
    }

    @Override
    public String getLogMethodName() {
        return "-" + testName.getMethodName();
    }

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            super.stopServer(true, "CWWKT0015W");
        } else {
            super.stopServer();
        }
    }

    @Test
    public void useDefaultHostForMainAndActuatorPorts() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useDefaultHostForMainConfigActuatorPorts() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(APP_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useDefaultHostWithAppPortForMainConfigActuatorPorts() throws Exception {
        server.setHttpDefaultPort(APP_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(APP_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useConfigForMainAndActuatorPorts() throws Exception {
        server.setHttpDefaultPort(APP_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(APP_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }

    @Test
    public void useOverrideForMainAndActuatorPorts() throws Exception {
        server.setHttpDefaultPort(OVERRIDE_MAIN_PORT);
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");

        server.setHttpDefaultPort(OVERRIDE_ACTUATOR_PORT);
        HttpUtils.findStringInUrl(server, "actuator/health", "UP");
    }
}
