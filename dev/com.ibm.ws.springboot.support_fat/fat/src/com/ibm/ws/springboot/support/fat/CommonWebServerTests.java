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

import java.util.HashMap;
import java.util.Map;

import componenttest.topology.utils.HttpUtils;

public abstract class CommonWebServerTests extends AbstractSpringTests {

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
        if (methodName != null && methodName.contains(DEFAULT_HOST_WITH_APP_PORT)) {
            return true;
        }
        return false;
    }

    public void testBasicSpringBootApplication() throws Exception {
        HttpUtils.findStringInUrl(server, "", "HELLO SPRING BOOT!!");
    }

}
