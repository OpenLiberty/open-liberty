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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class ProgrammaticTransAppTests20War extends ProgrammaticTransAppAbstractTests {

    @Test
    public void testTransactionsAppRunnerWar() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("AppRunner: TRANSACTION TESTS PASSED"));
    }

    @Test
    public void testTransactionsWebContextWar() throws Exception {
        HttpUtils.findStringInUrl(server, "testName/testTransactions", "TESTED TRANSACTIONS");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WebContext: TRANSACTION TESTS PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-4.0", "jca-1.7", "jdbc-4.2", "jndi-1.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        return Collections.singletonMap("configure.DataSourceTransactionManager", "true");
    }
}
