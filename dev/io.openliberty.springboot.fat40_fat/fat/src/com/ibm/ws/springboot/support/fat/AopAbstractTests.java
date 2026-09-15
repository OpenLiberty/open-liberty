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

    // Exercises the synchronous /service-sync endpoint so that stopServer does not produce SRVE8046E.
    protected void testAop() throws Exception {
        HttpUtils.findStringInUrl(server, getContextRoot() + "service-sync", "Spring Boot AOP Service");
        HttpUtils.findStringInUrl(server, getContextRoot() + "service-sync?value=test%2Ftest", "Spring Boot AOP Service");
        HttpUtils.findStringInUrl(server, getContextRoot() + "service-sync?value=test%5Ctest", "Spring Boot AOP Service");
        assertNotNull("Did not find message printed during execution of external service", server.waitForStringInLogUsingLastOffset("External Service method execution"));
        //This gets printed when LTW (LoadTimeWeaving) intercepts the call to the internal method. This explicitly requires LTW because Spring AOP does not intercept internal method calls.
        assertNotNull("Did not find message printed by AOP before execution of internal service",
                      server.waitForStringInLogUsingLastOffset("Before internal service method execution"));
        assertNotNull("Did not find message printed during execution of internal service", server.waitForStringInLogUsingLastOffset("Internal Service method execution"));
        assertNotNull("Did not find message printed after execution of external service",
                      server.waitForStringInLogUsingLastOffset("After external service method execution"));
    }

    // Exercises the async /service endpoint (WebAsyncTask). When running with servlet-6.1,
    // SRVE8046E is expected in the server logs due to the known bug tracked at
    // https://github.com/OpenLiberty/open-liberty/issues/35666. Each concrete subclass declares
    // SRVE8046E as expected in its @AfterClass stopServerWithKnownErrors() so the JUnit
    // report treats it correctly.
    protected void testAopAsync() throws Exception {
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
