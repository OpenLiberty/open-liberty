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

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JTAAppTests30 extends JTAAppAbstractTests {
    @Test
    public void testTransactionsAppRunner() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("AppRunner: TRANSACTION TESTS PASSED"));
    }

    @Test
    public void testTransactionsWebContext() throws Exception {
        HttpUtils.findStringInUrl(server, "testTransactions", "TESTED TRANSACTIONS");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WebContext: TRANSACTION TESTS PASSED"));
    }

    @Test
    public void testJNDIWebContext() throws Exception {
        HttpUtils.findStringInUrl(server, "testJNDI", "TESTED JNDI");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WebContext: JNDI TESTS PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("springBoot-3.0", "servlet-6.0", "connectors-2.1", "jdbc-4.2", "jndi-1.0", "componenttest-2.0", "cdi-4.0", "enterpriseBeansLite-4.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }
}
