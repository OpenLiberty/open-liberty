/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ConfigSpringBootApplicationTagTests30 extends AbstractSpringTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        appConfig.getApplicationArguments().add("--server.servlet.context-parameters.context_parameter_test_key=PASSED");
        appConfig.getApplicationArguments().add("--server.server-header=SpringServerHeaderTest");
    }

    @Test
    public void testSpringBootApplicationTag() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

    @Test
    public void testContextParams() throws IOException {
        HttpUtils.findStringInUrl(server, "/testContextParams", "PASSED");
    }

    @Test
    public void testServerHeader() throws IOException {
        HttpURLConnection conn = HttpUtils.getHttpConnection(server, "");
        conn.connect();
        String serverHeader = conn.getHeaderField("Server");
        conn.disconnect();
        assertEquals("Wrong server header.", "SpringServerHeaderTest", serverHeader);
    }
}
