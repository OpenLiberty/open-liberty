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
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MaximumJavaLevel(javaLevel = 8)
public class CommonWebServerTests15Servlet40 extends CommonWebServerTests {

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();

        if (!javaVersion.startsWith("1.")) {
            if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
                super.stopServer(true, "CWWKC0265W", "CWWKT0015W");
            } else {
                super.stopServer(true, "CWWKC0265W");
            }
        } else {
            if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
                super.stopServer(true, "CWWKT0015W");
            } else {
                super.stopServer();
            }
        }
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-1.5", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_15_APP_BASE;
    }

    @Test
    public void testBasicSpringBootApplication15Servlet40() throws Exception {
        testBasicSpringBootApplication();
    }

    @Test
    public void testDefaultHostWithAppPort15() throws Exception {
        testBasicSpringBootApplication();
    }

}
