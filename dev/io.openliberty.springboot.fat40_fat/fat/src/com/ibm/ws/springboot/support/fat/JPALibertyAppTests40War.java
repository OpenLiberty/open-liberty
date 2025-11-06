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

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class JPALibertyAppTests40War extends JPAAppAbstractTests {

    @Test
    public void testLibertyJPAAppRunnerWar() throws Exception {
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("COMMAND_LINE_RUNNER: SPRING DATA TEST: PASSED"));
    }

    @Test
    public void testLibertyJPAWebContextWar() throws Exception {
        HttpUtils.findStringInUrl(server, "testName/testPersistence", "TESTED PERSISTENCE");
        assertNotNull("Did not find TESTS PASSED messages", server.waitForStringInLog("WEB_CONTEXT: SPRING DATA TEST: PASSED"));
    }

    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList("servlet-6.1", "jdbc-4.3", "jndi-1.0", "componenttest-2.0", "persistence-3.2"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }

    @AfterClass
    public static void stopServerWithErrors() throws Exception {
        server.stopServer("CWWJP9991W", "WTRN0074E");
    }

    @Override
    public Map<String, String> getBootStrapProperties() {
        return Collections.singletonMap("test.persistence", "liberty");
    }
}
