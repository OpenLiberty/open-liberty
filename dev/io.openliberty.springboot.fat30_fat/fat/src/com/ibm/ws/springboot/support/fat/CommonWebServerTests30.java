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

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

@RunWith(FATRunner.class)
@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class CommonWebServerTests30 extends CommonWebServerTests {

    @After
    public void stopTestServer() throws Exception {
        String methodName = testName.getMethodName();
        if ((methodName != null) && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            super.stopServer(true, "CWWKT0015W");
        } else {
            super.stopServer();
        }
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    //

    @Test
    public void testBasicSpringBootApplication30Servlet60() throws Exception {
        testBasicSpringBootApplication();
    }

    @Test
    public void testDefaultHostWithAppPort30() throws Exception {
        // A variation of 'testBasicSpringBootApplication30Servlet60'.
        // The different behavior is triggered by the test name.
        testBasicSpringBootApplication();
    }
}
