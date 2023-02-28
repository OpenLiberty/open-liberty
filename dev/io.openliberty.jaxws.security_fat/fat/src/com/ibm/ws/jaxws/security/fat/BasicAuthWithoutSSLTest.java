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
 * Test case for basic authentication without ssl configuration
 */
@RunWith(FATRunner.class)
public class BasicAuthWithoutSSLTest extends AbstractJaxWsTransportSecurityTest {

    protected static final String SERVER_CONFIG_FILE_NAME = "basicAuthWithoutSSL.xml";

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        buildDefaultApps();

        updateSingleFileInServerRoot("server.xml", "serverConfigs/" + SERVER_CONFIG_FILE_NAME);
        lastServerConfig = "serverConfigs/" + SERVER_CONFIG_FILE_NAME;

        updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "basicAuthWithoutSSL_provider_web.xml");

        server.startServer("JaxWsTransportSecurityServer.log");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // Valid name and valid plain password
    @Test
    public void testValidNameAndValidPlainPassword() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // Valid name but invalid password
    @Test
    public void testValidNameButInvalidPassword() throws Exception {
        updateClientBndFile("bindings/validNameButInvalidPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService", "401"),
                                                                   new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService", "401"),
                                                                   new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService", "401")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*employee");
        runTest(params, serverInfos);
    }

    // Valid name and valid plain password but not authorized
    @Test
    public void testValidNameAndValidPlainPasswordButNOTAuthorized() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/manager/employPojoService", "403"),
                                                                   new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/manager/employStatelessService", "403"),
                                                                   new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/manager/employSingletonService", "403")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS9104A.*employee");
        runTest(params, serverInfos);
    }

    // Valid name and valid encoded password
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidEncodedPassword() throws Exception {
        updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // Invalid name
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testInvalidName() throws Exception {
        updateClientBndFile("bindings/invalidName.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService", "401"),
                                                                   new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService", "401"),
                                                                   new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService", "401")));
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*inexisteduser");
        runTest(params, serverInfos);
    }
}
