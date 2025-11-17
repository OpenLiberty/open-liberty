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

import java.io.IOException;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class CommonWebServerTests40 extends CommonWebServerTests {

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();
        if ((methodName != null) && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            super.stopServer(true, "CWWKT0015W");
        } else {
            super.stopServer();
        }
    }

    /**
     * Override: Web applications use springboot and servlet.
     *
     * @return The features provisioned in the test server. This
     *         implementation always answers "springBoot-4.0" and
     *         "servlet-6.1`".
     */
    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }

    @Test
    public void testBasicSpringBootApplication40() throws Exception {
        testBasicSpringBootApplication();
    }

    @Test
    public void testDefaultHostWithAppPort40() throws Exception {
        // A variation of 'testBasicSpringBootApplication40'.
        // The different behavior is triggered by the test name.
        testBasicSpringBootApplication();
    }

    @Test
    public void testPackagePrivateBean() throws IOException {
        HttpUtils.findStringInUrl(server, "/testPackagePrivateBean", "PASSED");
    }
}
