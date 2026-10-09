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

import java.net.HttpURLConnection;

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
    public void doConfigureServer() throws Exception {
        // Restore the HTTP port to DEFAULT_HTTP_PORT before every server start,
        // because AbstractSpringTests.tearDownTest() resets it to EXPECTED_HTTP_PORT
        // (8081) after each test, causing subsequent server starts to be contacted
        // on the wrong port (8081 instead of 8010).
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        super.doConfigureServer();
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_30_APP_AOP;
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
    protected void testAopSync() throws Exception {
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
    // SRVE8046E as expected in its @After stopServerAfterTest() so the JUnit report treats it
    // correctly. SRVE8046E does NOT apply to the servlet-6.0 case.
    protected void testAopAsync() throws Exception {
        HttpUtils.findStringInUrl(server, getContextRoot() + "service", "Spring Boot AOP Service");
        // When running with servlet-6.1, the async dispatch NullPointerException (SRVE8046E) causes
        // Liberty to return HTTP 500 for the encoded-character variants. Expect 500 explicitly so the
        // test client does not fail on the error response — the NPE and SRVE8046E are the expected
        // behavior tracked at https://github.com/OpenLiberty/open-liberty/issues/35666.
        HttpUtils.getHttpConnection(HttpUtils.createURL(server, getContextRoot() + "service?value=test%2Ftest"),
                                    HttpURLConnection.HTTP_INTERNAL_ERROR, 5, HttpUtils.HTTPRequestMethod.GET);
        HttpUtils.getHttpConnection(HttpUtils.createURL(server, getContextRoot() + "service?value=test%5Ctest"),
                                    HttpURLConnection.HTTP_INTERNAL_ERROR, 5, HttpUtils.HTTPRequestMethod.GET);
        assertNotNull("Did not find message printed during execution of external service", server.waitForStringInLogUsingLastOffset("External Service method execution"));
        //This gets printed when LTW (LoadTimeWeaving) intercepts the call to the internal method. This explicitly requires LTW because Spring AOP does not intercept internal method calls.
        assertNotNull("Did not find message printed by AOP before execution of internal service",
                      server.waitForStringInLogUsingLastOffset("Before internal service method execution"));
        assertNotNull("Did not find message printed during execution of internal service", server.waitForStringInLogUsingLastOffset("Internal Service method execution"));
        assertNotNull("Did not find message printed after execution of external service",
                      server.waitForStringInLogUsingLastOffset("After external service method execution"));
    }

}
