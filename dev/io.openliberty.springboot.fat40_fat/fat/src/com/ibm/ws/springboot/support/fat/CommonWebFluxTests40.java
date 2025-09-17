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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class CommonWebFluxTests40 extends CommonWebFluxTests {

    @Override
    public Map<String, String> getBootStrapProperties() {
        Map<String, String> properties = new HashMap<>();
        // add channel trace for RTC defect 266559
        properties.put("com.ibm.ws.logging.trace.specification",
                       "*=info:HTTPChannel=all:TCPChannel=all:GenericBNF=all:ChannelFramework=all");
        return properties;
    }

    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_WEBFLUX;
    }

    //

    @Test
    public void testBasicWebFluxtAppServlet61() throws Exception {
        testBasicSpringBootApplication();
    }

    @Test
    public void testBlockingIOServlet61() throws IOException, InterruptedException {
        testBlockingIO();
    }
}
