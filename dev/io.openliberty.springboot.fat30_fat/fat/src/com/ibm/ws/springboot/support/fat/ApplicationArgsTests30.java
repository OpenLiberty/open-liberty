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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Run the basic web application tests on the base application.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ApplicationArgsTests30 extends CommonWebServerTests {
    private static int TEST_PORT = 8082;

    @BeforeClass
    public static void setupTest() {
        server.setHttpDefaultPort(TEST_PORT);

        setExtraArguments();
    }

    public static void setExtraArguments() {
        extraServerArgs.add("--");
        extraServerArgs.add("--server.liberty.useDefaultHost=false");
        extraServerArgs.add("--server.port=" + TEST_PORT);
    }

    //

    /**
     * Test method: Verify that the spring boot application
     * answers the expected message for the default request.
     */
    @Test
    public void testServerAppArgs() throws Exception {
        testBasicSpringBootApplication();
    }
}
