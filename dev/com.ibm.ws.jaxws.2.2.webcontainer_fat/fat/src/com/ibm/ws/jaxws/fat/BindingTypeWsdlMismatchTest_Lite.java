/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the binding type is align with the wsdl file if specified.
 */
@SkipForRepeat("jaxws-2.3")
public class BindingTypeWsdlMismatchTest_Lite {

    @Server("BindingTypeWsdlMismatchTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ExplodedShrinkHelper.explodedDropinApp(server, "testBindingTypeWsdlWeb", "com.ibm.ws.jaxws.test.bindingtypewsdl");
    }

    @After
    public void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer("CWWKW0058E");
        }

        server.deleteFileFromLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF/webservices.xml");
        server.deleteFileFromLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl/HelloService.wsdl");
    }

    /**
     * TestDescription:
     * - Test default binding type against soap11 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Don't config <protocol-binding> in webservices.xml
     * - use soap11 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate can pass, so the war can start successfully
     */
    @Test
    public void test_DefaultHttpBindingType_Soap11Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_DefaultHttpBindingType_Soap11Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_DefaultHttpBindingType_Soap11Wsdl", "HelloService11.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testBindingTypeWsdlWeb"); // start successfully

        String result = TestUtils.getServletResponse(getBaseUrl() + "/testBindingTypeWsdlWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + result, result.contains("<?xml"));

    }

    /**
     * TestDescription:
     * - Test SOAP12HTTP binding type against soap12 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Config <protocol-binding>##SOAP12_HTTP</protocol-binding> in webservices.xml
     * - use soap12 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate can pass, so the war can start successfully
     */
    @Test
    public void test_Soap12HttpBindingType_Soap12Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_Soap12HttpBindingType_Soap12Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_Soap12HttpBindingType_Soap12Wsdl", "HelloService12.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testBindingTypeWsdlWeb"); // start successfully

        String result = TestUtils.getServletResponse(getBaseUrl() + "/testBindingTypeWsdlWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + result, result.contains("<?xml"));
    }

    protected String getBaseUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}
