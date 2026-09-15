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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public abstract class AopAbstractTests extends AbstractSpringTests {

    @BeforeClass
    public static void setup() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
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
        stopServer(DO_CLEANUP_APPS, "SRVE8046E");
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_40_APP_AOP;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }

    public String getContextRoot() {
        return "/";
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        config.getWebContainer().setSkipEncodedCharVerification(true);
    }

    // Note: When running with servlet-6.1, the following SRVE8046E warning is expected in the server logs
    // due to a known bug in web async Spring MVC AOP dispatch — see https://github.com/OpenLiberty/open-liberty/issues/35666.
    // The SRVE8046E error is declared as expected in stopServerWithKnownErrors() above so that the JUnit
    // report correctly identifies it as a known issue rather than an unexpected test failure.
    protected void testAop() throws Exception {
        HttpUtils.findStringInUrl(server, getContextRoot() + "service", "Spring Boot AOP Service");
        HttpUtils.findStringInUrl(server, getContextRoot() + "service?value=test%2Ftest", "Spring Boot AOP Service");
        HttpUtils.findStringInUrl(server, getContextRoot() + "service?value=test%5Ctest", "Spring Boot AOP Service");
        assertNotNull("Did not find message printed during execution of external service", server.waitForStringInLogUsingLastOffset("External Service method execution"));
        //This gets printed when LTW (LoadTimeWeaving) intercepts the call to the internal method. This explicitly requires LTW because Spring AOP does not intercept internal method calls.
        assertNotNull("Did not find message printed by AOP before execution of internal service",
                      server.waitForStringInLogUsingLastOffset("Before internal service method execution"));
        assertNotNull("Did not find message printed during execution of internal service", server.waitForStringInLogUsingLastOffset("Internal Service method execution"));
        assertNotNull("Did not find message printed after execution of external service",
                      server.waitForStringInLogUsingLastOffset("After external service method execution"));
    }

}
