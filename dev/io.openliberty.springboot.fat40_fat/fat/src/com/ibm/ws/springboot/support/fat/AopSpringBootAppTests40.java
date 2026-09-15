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

import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class AopSpringBootAppTests40 extends AopAbstractTests {
    @Override
    public Set<String> getFeatures() {
        return getWebFeatures();
    }

    @Override
    public AppConfigType getApplicationConfigType() {
        return AppConfigType.SPRING_BOOT_APP_TAG;
    }

    // Stop the server after each test method so that only the async test allows SRVE8046E.
    // The synchronous test uses the /service-sync endpoint and must not produce SRVE8046E,
    // even though both tests run with servlet-6.1.
    @After
    public void stopServerAfterTest() throws Exception {
        if (testName.getMethodName().contains("Servlet61Aysnc")) {
            // Known bug: When running Spring Boot 4 with servlet-6.1, the web container emits SRVE8046E
            // for the async dispatch NullPointerException in the web async Spring MVC AOP path.
            // SRVE8046E is expected and is tracked at https://github.com/OpenLiberty/open-liberty/issues/35666.
            // TODO: Remove "SRVE8046E" from stopServer once https://github.com/OpenLiberty/open-liberty/issues/35666 is fixed.
            stopServer(DO_CLEANUP_APPS, "SRVE8046E");
        } else {
            // Synchronous test uses /service-sync endpoint; SRVE8046E must not appear.
            stopServer(DO_CLEANUP_APPS);
        }
    }

    @Test
    public void testAopSpringBootApplicationServlet61Sync() throws Exception {
        testAopSync();
    }

    // The SRVE8046E NullPointerException on AsyncContext dispatch is expected when running with servlet-6.1.
    // See https://github.com/OpenLiberty/open-liberty/issues/35666
    @Test
    public void testAopSpringBootApplicationServlet61Aysnc() throws Exception {
        testAopAsync();
    }
}
