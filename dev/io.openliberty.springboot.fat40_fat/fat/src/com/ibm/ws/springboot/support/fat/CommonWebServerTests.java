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

import java.util.HashMap;
import java.util.Map;

import componenttest.topology.utils.HttpUtils;

/**
 * Extension to common spring FAT test class:
 *
 * Provide implementations for common web module tests.
 */
public abstract class CommonWebServerTests extends AbstractSpringTests {

    /**
     * Override: When {@link #DEFAULT_HOST_WITH_APP_PORT} a part of the
     * test method name, add BVT HTTP default and default security ports set to -1.
     *
     * @return Bootstrap properties for web application tests.
     */
    @Override
    public Map<String, String> getBootStrapProperties() {
        String methodName = testName.getMethodName();

        Map<String, String> properties = new HashMap<>();
        if ((methodName != null) && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            properties.put("bvt.prop.HTTP_default", "-1");
            properties.put("bvt.prop.HTTP_default.secure", "-1");
        }
        return properties;
    }

    /**
     * Override: When {@link #DEFAULT_HOST_WITH_APP_PORT} is a part of the test
     * method name, answer true, indicating the default virtual host should be
     * used. Otherwise, answer false.
     *
     * @return True or false telling if the default virtual host is to be used.
     */
    @Override
    public boolean useDefaultVirtualHost() {
        String methodName = testName.getMethodName();
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            return true;
        }
        return false;
    }

    /**
     * Override: Most often, web server tests use the
     * base spring application.
     *
     * @return The name of the application used by this test
     *         class. This implementation always answers
     *         {@link #SPRING_BOOT_40_APP_BASE}.
     */
    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_BASE;
    }

    //

    /**
     * Common web application test: Verify that a request to the
     * server answers expected response text. An assertion error
     * is thrown if the expected response text is not received.
     *
     * @throws Exception Thrown if the request was not handled by
     *                       the server.
     */
    public void testBasicSpringBootApplication() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }
}
