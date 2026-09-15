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

import static org.junit.Assert.assertNotNull;

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

    // Stop the server after each test method so that each test uses its own servlet feature
    // and only the async (servlet-6.1) test allows SRVE8046E.
    @After
    public void stopServerAfterTest() throws Exception {
        if (testName.getMethodName().contains("Servlet61")) {
            // Known bug: When running Spring Boot 3 with servlet-6.1, the web container emits SRVE8046E
            // for the async dispatch NullPointerException in the web async Spring MVC AOP path.
            // SRVE8046E is expected and is tracked at https://github.com/OpenLiberty/open-liberty/issues/35666.
            // TODO: Remove "SRVE8046E" from stopServer once https://github.com/OpenLiberty/open-liberty/issues/35666 is fixed.
            stopServer(DO_CLEANUP_APPS, "SRVE8046E");
        } else {
            // servlet-6.0 test uses the synchronous endpoint; SRVE8046E must not appear.
            stopServer(DO_CLEANUP_APPS);
        }
    }

    @Test
    public void testAopWebApplicationServlet60() throws Exception {
        testAopSync();
    }

    // The SRVE8046E NullPointerException on AsyncContext dispatch is expected when running with servlet-6.1.
    // See https://github.com/OpenLiberty/open-liberty/issues/35666
    @Test
    public void testAopWebApplicationServlet61() throws Exception {
        testAopAsync();
        assertNotNull("No NullPointerException on AsyncContext dispatch found.", server.waitForStringInLog("SRVE8046E"));
    }
}
