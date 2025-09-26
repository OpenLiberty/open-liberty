/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.SpringBootApplication;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class JNDINoEEContextAppTests extends JTAAppAbstractTests {

    @Override
    public void modifyAppConfiguration(SpringBootApplication appConfig) {
        appConfig.setEEContextOnStartup(false);
        super.modifyAppConfiguration(appConfig);
    }

    @Test
    public void testNoStartupContextJNDIAppRunner() throws Exception {
        assertNotNull("Did not find TESTS FAILED messages", server.waitForStringInLog("AppRunner: JNDI TESTS FAILED"));
    }

    @Test
    public void testNoStartupJNDIWebContext() throws Exception {
        HttpUtils.findStringInUrl(server, "testJNDI", "TESTED JNDI");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WebContext: JNDI TESTS PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0", "jca-1.7", "jdbc-4.2", "jndi-1.0", "componenttest-1.0", "cdi-2.0", "ejbLite-3.2"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }
}
