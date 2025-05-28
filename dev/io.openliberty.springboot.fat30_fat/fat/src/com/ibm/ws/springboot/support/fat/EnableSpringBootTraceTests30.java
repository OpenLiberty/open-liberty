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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
//@Mode(FULL)
@MinimumJavaLevel(javaLevel = 17)
public class EnableSpringBootTraceTests30 extends CommonWebServerTests {

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_BASE;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("com.ibm.ws.logging.trace.specification", "*=audit=enabled:springboot=all");
        return properties;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer(true);
    }

    @Test
    public void testEnableSpringBootTraceFor30() throws Exception {
        testBasicSpringBootApplication();
    }
}