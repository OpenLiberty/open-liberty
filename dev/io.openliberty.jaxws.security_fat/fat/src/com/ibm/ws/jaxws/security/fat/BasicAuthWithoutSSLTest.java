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
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(BasicAuthWithoutSSLTest.class.getName());

    protected static final String SERVER_CONFIG_FILE_NAME = "basicAuthWithoutSSL.xml";

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        buildDefaultApps();

        updateSingleFileInServerRoot("server.xml", "serverConfigs/" + SERVER_CONFIG_FILE_NAME);
        lastServerConfig = "serverConfigs/" + SERVER_CONFIG_FILE_NAME;

        updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "basicAuthWithoutSSL_provider_web.xml");

        ArrayList<String> fileNames = server.listLibertyServerRoot("apps/TransportSecurityProvider.ear/TransportSecurityProvider.war/WEB-INF", null);

        for (String i : fileNames) {
            LOG.info("List of files in app directory: " + i);
        }

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
    public void testValidNameAndValidPlainPasswordPOJO() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService");
        runTest(params, "Hello, employee from SayHelloPojoService", null);
    }

    @Test
    public void testValidNameAndValidPlainPasswordStateless() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService");
        runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
    }

    @Test
    public void testValidNameAndValidPlainPasswordSingleton() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService");
        runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
    }

    // Valid name but invalid password
    @Test
    public void testValidNameButInvalidPasswordPOJO() throws Exception {
        updateClientBndFile("bindings/validNameButInvalidPwd.xml");

        RequestParams params = new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*employee");
        runTest(params, "401", serverInfos);
    }

    @Test
    public void testValidNameButInvalidPasswordStateless() throws Exception {
        updateClientBndFile("bindings/validNameButInvalidPwd.xml");

        RequestParams params = new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*employee");
        runTest(params, "401", serverInfos);
    }

    @Test
    public void testValidNameButInvalidPasswordSingleton() throws Exception {
        updateClientBndFile("bindings/validNameButInvalidPwd.xml");

        RequestParams params = new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*employee");
        runTest(params, "401", serverInfos);
    }

    // Valid name and valid plain password but not authorized
    @Test
    public void testValidNameAndValidPlainPasswordButNOTAuthorizedPOJO() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/manager/employPojoService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS9104A.*employee");
        runTest(params, "403", serverInfos);
    }

    @Test
    public void testValidNameAndValidPlainPasswordButNOTAuthorizedStateless() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/manager/employStatelessService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS9104A.*employee");
        runTest(params, "403", serverInfos);
    }

    @Test
    public void testValidNameAndValidPlainPasswordButNOTAuthorizedSingleton() throws Exception {
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");

        RequestParams params = new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/manager/employSingletonService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS9104A.*employee");
        runTest(params, "403", serverInfos);
    }

    // Valid name and valid encoded password
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidEncodedPasswordPOJO() throws Exception {
        updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

        RequestParams params = new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService");
        runTest(params, "Hello, employee from SayHelloPojoService", null);
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidEncodedPasswordStateless() throws Exception {
        updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

        RequestParams params = new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService");
        runTest(params, "From other bean: Hello, employee from SayHelloSingletonService", null);
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testValidNameAndValidEncodedPasswordSingleton() throws Exception {
        updateClientBndFile("bindings/validNameAndValidEncodedPwd.xml");

        RequestParams params = new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService");
        runTest(params, "From other bean: Hello, employee from SayHelloStatelessService", null);
    }

    // Invalid name
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testInvalidNamePOJO() throws Exception {
        updateClientBndFile("bindings/invalidName.xml");

        RequestParams params = new RequestParams("employee", "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*inexisteduser");
        runTest(params, "401", serverInfos);
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testInvalidNameStateless() throws Exception {
        updateClientBndFile("bindings/invalidName.xml");

        RequestParams params = new RequestParams("employee", "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*inexisteduser");
        runTest(params, "401", serverInfos);
    }

    @Test
    @Mode(Mode.TestMode.FULL)
    public void testInvalidNameSingleton() throws Exception {
        updateClientBndFile("bindings/invalidName.xml");

        RequestParams params = new RequestParams("employee", "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService");
        List<String> serverInfos = new ArrayList<String>(1);
        serverInfos.add("CWWKS1100A.*inexisteduser");
        runTest(params, "401", serverInfos);
    }
}
