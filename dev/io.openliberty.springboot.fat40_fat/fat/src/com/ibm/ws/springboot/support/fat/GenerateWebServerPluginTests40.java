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
import static junit.framework.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.utils.HttpUtils;

@Mode(FULL)
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class GenerateWebServerPluginTests40 extends AbstractSpringTests {

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
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
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
        HttpUtils.findStringInUrl(server, "/myAppContextPath/", "HELLO SPRING BOOT!!");
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
