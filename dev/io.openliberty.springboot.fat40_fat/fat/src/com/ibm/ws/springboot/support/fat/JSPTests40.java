/*******************************************************************************
 * Copyright (c) 2019,2025 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JSPTests40 extends AbstractSpringTests {

    @Test
    public void testJSP() throws Exception {
        HttpUtils.findStringInUrl(server, "/test", "resources/text.txt");
    }

    @Test
    public void testWelcomePage() throws Exception {
        HttpUtils.findStringInUrl(server, "", "Welcome Page!!");
    }

    @Test
    public void testWelcomePageOnDefaultHost() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        HttpUtils.findStringInUrl(server, "", "Welcome Page!!");
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-4.0", "pages-4.0"));
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_WAR;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        if (testName.getMethodName().contains("DefaultHost")) {
            return true;
        }
        return false;
    }

    @After
    public void stopTestServer() throws Exception {
        super.stopServer();
    }

}
