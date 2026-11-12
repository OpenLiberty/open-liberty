/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class AopWebAppTests30 extends AopAbstractTests {
    @Override
    public Set<String> getFeatures() {
        return new HashSet<>(Arrays.asList(testName.getMethodName().contains("Servlet61") ? "servlet-6.1" : "servlet-6.0"));
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.WEB_APP_TAG;
    }

    @Override
    public String getContextRoot() {
        return "/testName/";
    }

    // Stop the server after each test method so that each test uses its own servlet feature.
    @After
    public void stopServerAfterTest() throws Exception {
        stopServer(DO_CLEANUP_APPS);
    }

    @Test
    public void testAopWebApplicationServlet60Sync() throws Exception {
        testAopSync();
    }

    @Test
    public void testAopWebApplicationServlet60Async() throws Exception {
        testAopAsync();
    }

    @Test
    public void testAopWebApplicationServlet61Async() throws Exception {
        testAopAsync();
    }
}
