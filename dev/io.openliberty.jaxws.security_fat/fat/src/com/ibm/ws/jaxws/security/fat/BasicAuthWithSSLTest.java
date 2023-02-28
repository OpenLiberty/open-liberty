/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;

/*
 * Test case for basic authentication with ssl configuration
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class BasicAuthWithSSLTest extends AbstractJaxWsTransportSecuritySSLTest {
    protected static final String SERVER_CONFIG_FILE_NAME = "basicAuthWithSSL.xml";

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        buildDefaultApps();
        updateSingleFileInServerRoot("server.xml", "serverConfigs/" + SERVER_CONFIG_FILE_NAME);
        lastServerConfig = "serverConfigs/" + SERVER_CONFIG_FILE_NAME;

        updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "basicAuthWithSSL_provider_web.xml");

        server.startServer("JaxWsTransportSecurityServer.log");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // Valid name and valid encoded password
    @Test
    public void testValidNameAndValidEncodedPassword() throws Exception {
        updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // Invalid name
    @Mode(componenttest.custom.junit.runner.Mode.TestMode.FULL)
    @Test
    public void testInvalidName() throws Exception {
        updateClientBndFile("bindings/invalidName.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "401"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "401"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "401")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*inexisteduser");
        runTest(params, serverInfos);
    }

    // Valid name and valid plain password but not authorized
    @Test
    public void testValidNameAndValidPlainPasswordButNOTAuthorized() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/manager/employPojoService", "403"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/manager/employStatelessService", "403"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/manager/employSingletonService", "403")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS9104A.*employee");
        runTest(params, serverInfos);
    }

    // Valid name and valid plain password
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidPlainPassword() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");
        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // Valid name but invalid password
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameButInvalidPassword() throws Exception {
        updateClientBndFile("bindings/validNameButInvalidPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "401"),
                                                                   new RequestParams("employee", "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "401"),
                                                                   new RequestParams("employee", "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "401")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*employee");
        runTest(params, serverInfos);
    }
}
