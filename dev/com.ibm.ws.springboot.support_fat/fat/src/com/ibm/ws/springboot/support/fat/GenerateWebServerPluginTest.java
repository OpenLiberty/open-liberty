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
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

@Mode(FULL)
@RunWith(FATRunner.class)
public class GenerateWebServerPluginTest extends AbstractSpringTests {

    @After
    public void stop() throws Exception {
        //stop after each test to force server reconfigure.
        stopServer();
    }

    @Test
    public void testPluginCfgGeneratedUsingDefaultVirtualHost() throws Exception {
        exerciseAppOnConfiguredServer();
    }

    @Test
    public void testPluginCfgGeneratedUsingSpringBootVirtualHost() throws Exception {
        exerciseAppOnConfiguredServer();
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        config.getSpringBootApplications().get(0).getApplicationArguments().add("--server.servlet.context-path=/myAppContextPath");
        if (testName.getMethodName().contains("UsingDefaultVirtualHost")) {
            //use port configured in default_host
            server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        } else {
            //use port from application.properties
            server.setHttpDefaultPort(EXPECTED_HTTP_PORT);
        }
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-3.1"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        if (testName.getMethodName().contains("UsingDefaultVirtualHost"))
            return true;
        else
            return false;
    }

    public void exerciseAppOnConfiguredServer() throws Exception {
        HttpUtils.findStringInUrl(server, "/myAppContextPath", "HELLO SPRING BOOT!!");
        //wait a short time for the plugin generation logic to write out the file
        String pluginCfgRelativePath = "logs/state/plugin-cfg.xml";
        long startTime = System.currentTimeMillis();
        while (!server.fileExistsInLibertyServerRoot(pluginCfgRelativePath)) {
            if ((startTime + 1000 * 10) < System.currentTimeMillis()) {
                break;
            }
            Thread.sleep(1000);
        }
        assertTrue("Expected one matching contextroot entry in plugin cfg",
                   server.findStringsInFileInLibertyServerRoot("<Uri .* Name=\"/myAppContextPath/\\*\"/>", pluginCfgRelativePath).size() == 1);
    }
}
