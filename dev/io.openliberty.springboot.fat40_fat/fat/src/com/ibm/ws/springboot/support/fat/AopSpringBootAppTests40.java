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

import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

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

    // Known bug: When running Spring Boot 4 with servlet-6.1, the web container emits SRVE8046E
    // for the async dispatch NullPointerException in the web async Spring MVC AOP path:
    //   SRVE8046E: An error occurred while invoking a call to AsyncContext dispatch.
    //   java.lang.NullPointerException: Cannot invoke
    //     "com.ibm.ws.webcontainer.webapp.WebAppRequestDispatcher.dispatch(...)" because
    //     "this.requestDispatcher" is null at DispatchRunnable.run(DispatchRunnable.java:92)
    // This SRVE8046E is expected and is tracked at https://github.com/OpenLiberty/open-liberty/issues/35666.
    // It is declared here so that the JUnit test report does not flag it as an unexpected failure.
    @AfterClass
    public static void stopServerWithKnownErrors() throws Exception {
        List<String> srve8046 = server.findStringsInLogs("SRVE8046E");
        if (!srve8046.isEmpty()) {
            Log.info(AopSpringBootAppTests40.class, "stopServerWithKnownErrors",
                     "KNOWN BUG (https://github.com/OpenLiberty/open-liberty/issues/35666): "
                     + "SRVE8046E found in logs as expected -- AsyncContext.dispatch NullPointerException "
                     + "during WebAsync Spring MVC AOP path with servlet-6.1. "
                     + "Occurrences: " + srve8046.size());
        }
        stopServer(DO_CLEANUP_APPS, "SRVE8046E");
    }

    @Test
    public void testAopSpringBootApplication() throws Exception {
        testAop();
    }

    // The SRVE8046E NullPointerException on AsyncContext dispatch is expected when running with servlet-6.1.
    // See https://github.com/OpenLiberty/open-liberty/issues/35666
    @Test
    public void testAopSpringBootApplicationAsync() throws Exception {
        testAopAsync();
    }
}
