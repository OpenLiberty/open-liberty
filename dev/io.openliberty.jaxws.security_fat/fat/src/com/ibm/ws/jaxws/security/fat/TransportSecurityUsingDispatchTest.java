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

/**
 * Test cases for Dispatch way using the transport security
 */
//@FixMethodOrder(MethodSorters.DEFAULT)
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.FULL)
public class TransportSecurityUsingDispatchTest extends AbstractJaxWsTransportSecurityTest {

    private static final String BASIC_AUTH_WITHOUT_SSL_CONFIG = "basicAuthWithoutSSL.xml";
    private static final String BASIC_AUTH_WITH_SSL_CONFIG = "basicAuthWithSSL.xml";
    private static final String CUSTOMIZE_SSL_CONFIG = "customizeSSLConfiguration.xml";

    private boolean checkAppUpdate = false;

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        buildDefaultApps();
        server.startServer("JaxWsTransportSecurityServer.log");
        server.setMarkToEndOfLog();
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

//     1 Valid name and valid plain password without SSL
    @Test
    public void testValidNameAndValidPlainPasswordWithoutSSLDispatch() throws Exception {
        updateServerConfigFile("serverConfigs/" + BASIC_AUTH_WITHOUT_SSL_CONFIG, checkAppUpdate);
        updateProviderWEBXMLFile("basicAuthWithoutSSL_provider_web.xml");
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");
        Thread.sleep(4000);
        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", "http", server.getHttpDefaultPort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", "http", server.getHttpDefaultPort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", "http", server.getHttpDefaultPort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
        checkAppUpdate = false;
    }

    // 2 Valid name and valid plain password with SSL
    @Test
    public void testValidNameAndValidPlainPasswordWithSSLDispatch() throws Exception {
        updateServerConfigFile("serverConfigs/" + BASIC_AUTH_WITH_SSL_CONFIG, checkAppUpdate);
        updateProviderWEBXMLFile("basicAuthWithSSL_provider_web.xml");
        updateClientBndFile("bindings/validNameAndValidPlainPwd.xml");
        Thread.sleep(2000);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", "https", server.getHttpDefaultSecurePort(), "/employee/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", "https", server.getHttpDefaultSecurePort(), "/employee/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", "https", server.getHttpDefaultSecurePort(), "/employee/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));
        runTest(params, null);
        checkAppUpdate = false;
    }

    // 3 Customize SSL configuration
    @Test
    public void testCustomizeSSLConfigDispatch() throws Exception {
        updateServerConfigFile("serverConfigs/" + CUSTOMIZE_SSL_CONFIG);
        updateProviderWEBXMLFile("basicAuthWithSSL_provider_web.xml");
        updateClientBndFile("bindings/customizeSSLConfig.xml");
        Thread.sleep(2000);

        List<RequestParams> params = new ArrayList<>(Arrays.asList(new RequestParams("employee", TestMode.DISPATCH, "pojo", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employPojoService", "Hello, employee from SayHelloPojoService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "stateless", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employStatelessService", "From other bean: Hello, employee from SayHelloStatelessService"),
                                                                   new RequestParams("employee", TestMode.DISPATCH, "singleton", "https", server.getHttpDefaultSecurePort(), "/unauthorized/employSingletonService", "From other bean: Hello, employee from SayHelloSingletonService")));

        runTest(params, null);
        checkAppUpdate = true;
    }
}
