/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test cases for client certificate fall back on basic-auth
 */
@RunWith(FATRunner.class)
public class ClientCertFailOverToBasicAuthTest extends AbstractJaxWsTransportSecurityTest {

    protected static final String FAIL_OVER_TO_BASIC_AUTH_CONFIG = "clientCertFailOverToBasicAuthConfiguration.xml";

    protected static final String PATCHY_SERVER_TRUST_STORE_CONFIG = "patchyServerTrustStoreConfiguration.xml";

    protected static final List<String> INEXISTENT_ALIAS_SERVER_INFO = new ArrayList<String>();

    protected static final List<String> NOT_AUTHORIZAED_SERVER_INFO = new ArrayList<String>();

    protected static final List<String> AUTHENTICATION_FAILED_SERVER_INFO = new ArrayList<String>();

    protected static final String SCHEMA = "https";

    protected static final int SECURE_PORT = server.getHttpDefaultSecurePort();

    protected static final boolean checkAppUpdate = false;

    static {
        INEXISTENT_ALIAS_SERVER_INFO.add("CWPKI0023E.*");
        INEXISTENT_ALIAS_SERVER_INFO.add("CWWKW0601E.*");

        NOT_AUTHORIZAED_SERVER_INFO.add("CWWKS9104A.*employee");

        AUTHENTICATION_FAILED_SERVER_INFO.add("CWWKS1100A.*manager");
    }

    @BeforeClass
    public static void beforeAllTests() throws Exception {
        buildDefaultApps();
        updateSingleFileInServerRoot("server.xml", "serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG);
        lastServerConfig = "serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG;

        updateSingleFileInServerRoot(WEB_XML_IN_PROVIDER_WAR, "clientCert_provider_web.xml");

        server.startServer("JaxWsTransportSecurityServer.log");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0601E", "CWPKI0023E");
        }
    }

    // 1 Inexistent client cert but valid basic-auth
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
    @Test
    public void testInexistentClientCertButValidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/inexistentCertButValidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "CWPKI0023E")));

        runTest(params, INEXISTENT_ALIAS_SERVER_INFO);
    }

    // 2 existent client cert but invalid basic-auth
    @Test
    public void testExistentClientCertButInvalidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/existentCertButInvalidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // 3 No client cert configured but valid basic auth
    @Test
    public void testNoClientCertConfiguredButValidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/noAliasConfiguredButValidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("manager", "pojo", SCHEMA, SECURE_PORT, "/manager/employPojoService", "Hello, manager from SayHelloPojoService"),
                                                                   new RequestParams("manager", "stateless", SCHEMA, SECURE_PORT, "/manager/employStatelessService", "From other bean: Hello, manager from SayHelloStatelessService"),
                                                                   new RequestParams("manager", "singleton", SCHEMA, SECURE_PORT, "/manager/employSingletonService", "From other bean: Hello, manager from SayHelloSingletonService")));

        runTest(params, null);
    }

    // 1 Inexistent client cert and invalid basic-auth
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.net.ssl.SSLException" })
    @Test
    public void testInexistentClientCertAndInvalidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/inexistentCertAndInvalidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "CWPKI0023E"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "CWPKI0023E")));

        runTest(params, INEXISTENT_ALIAS_SERVER_INFO);
    }

    // 2 existent client cert and valid basic-auth
    @Test
    public void testExistentClientCertAndValidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + FAIL_OVER_TO_BASIC_AUTH_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/existentCertAndValidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("employee", "pojo", SCHEMA, SECURE_PORT, "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", "stateless", SCHEMA, SECURE_PORT, "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", "singleton", SCHEMA, SECURE_PORT, "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
    }

    // 6 No client cert configured and invalid basic auth
    @Test
    public void testNoClientCertConfiguredAndInvalidBasicAuth() throws Exception {
        updateServerConfigFile("serverConfigs/" + PATCHY_SERVER_TRUST_STORE_CONFIG, checkAppUpdate);
        updateClientBndFile("bindings/noAliasConfiguredAndInvalidBA.xml");

        List<RequestParams> params = new ArrayList<>(Arrays.asList(
                                                                   new RequestParams("manager", "pojo", SCHEMA, SECURE_PORT, "/manager/employPojoService", "401"),
                                                                   new RequestParams("manager", "stateless", SCHEMA, SECURE_PORT, "/manager/employStatelessService", "401"),
                                                                   new RequestParams("manager", "singleton", SCHEMA, SECURE_PORT, "/manager/employSingletonService", "401")));

        runTest(params, AUTHENTICATION_FAILED_SERVER_INFO);

    }
}
