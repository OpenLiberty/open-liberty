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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class CommonWebFluxTests20Servlet40 extends CommonWebFluxTests {
    @Test
    public void testBasicWebFluxtAppServlet40() throws Exception {
        testBasicSpringBootApplication();
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        // add channel trace for RTC defect 266559
        properties.put("com.ibm.ws.logging.trace.specification", "*=info:HTTPChannel=all:TCPChannel=all:GenericBNF=all:ChannelFramework=all");
        return properties;
    }
    @Test
    public void testBlockingIOServlet40() throws IOException, InterruptedException {
        testBlockingIO();
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_WEBFLUX;
    }

}
