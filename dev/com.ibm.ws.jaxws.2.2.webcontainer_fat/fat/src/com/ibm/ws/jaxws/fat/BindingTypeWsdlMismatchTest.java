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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;

/**
 * Test the binding type is align with the wsdl file if specified.
 */
@RunWith(FATRunner.class)

public class BindingTypeWsdlMismatchTest extends BindingTypeWsdlMismatchTest_Lite {

    /**
     * TestDescription:
     * - Test SOAP11HTTP binding type against soap11 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Config <protocol-binding>##SOAP11_HTTP</protocol-binding> in webservices.xml
     * - use soap11 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate can pass, so the war can start successfully
     */
    @Test
    public void test_Soap11HttpBindingType_Soap11Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_Soap11HttpBindingType_Soap11Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_Soap11HttpBindingType_Soap11Wsdl", "HelloService11.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testBindingTypeWsdlWeb"); // start successfully

        String result = TestUtils.getServletResponse(getBaseUrl() + "/testBindingTypeWsdlWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + result, result.contains("<?xml"));

    }

    /**
     * TestDescription:
     * - Test SOAP12HTTP binding type against soap11 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Config <protocol-binding>##SOAP12_HTTP</protocol-binding> in webservices.xml
     * - use soap11 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate cannot pass, throws javax.xml.ws.WebServiceException*CWWKW0058E
     * Note:
     * - Since we throws a WebServiceException during app deployment, we have to allow the FFDC of
     * "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException"
     */
    @Test
    @AllowedFFDC({ "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException" })
    public void test_Soap12HttpBindingType_Soap11Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_Soap12HttpBindingType_Soap11Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_Soap12HttpBindingType_Soap11Wsdl", "HelloService11.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServerAndValidate(true, true, false);
        List<String> results = server.findStringsInLogs("javax.xml.ws.WebServiceException.*CWWKW0058E"); // start failed
        assertTrue("The validation should be failed with a WebServiceException.", !results.isEmpty());
    }

    /**
     * TestDescription:
     * - Test default binding type against soap12 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Don't config <protocol-binding> in webservices.xml
     * - use soap12 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate cannot pass, throws javax.xml.ws.WebServiceException*CWWKW0058E
     * Note:
     * - Since we throws a WebServiceException during app deployment, we have to allow the FFDC of
     * "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException"
     */
    @Test
    @AllowedFFDC({ "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException" })
    public void test_DefaultHttpBindingType_Soap12Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_DefaultHttpBindingType_Soap12Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_DefaultHttpBindingType_Soap12Wsdl", "HelloService12.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServerAndValidate(true, true, false);
        List<String> results = server.findStringsInLogs("javax.xml.ws.WebServiceException.*CWWKW0058E"); // start failed
        assertTrue("The validation should be failed with a WebServiceException.", !results.isEmpty());
    }

    /**
     * TestDescription:
     * - Test SOAP11HTTP binding type against soap12 in wsdl
     * Condition:
     * - A testBindingTypeWsdlWeb war with a Web service - Hello
     * - Config <protocol-binding>##SOAP11_HTTP</protocol-binding> in webservices.xml
     * - use soap12 in WEB-INF/wsdl/HelloService.wsdl
     * Result:
     * - validate cannot pass, throws javax.xml.ws.WebServiceException*CWWKW0058E
     * Note:
     * - Since we throws a WebServiceException during app deployment, we have to allow the FFDC of
     * "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException"
     */
    @Test
    @AllowedFFDC({ "java.util.concurrent.ExecutionException", "com.ibm.ws.container.service.metadata.MetaDataException", "javax.xml.ws.WebServiceException" })
    public void test_Soap11HttpBindingType_Soap12Wsdl() throws Exception {
        server.copyFileToLibertyServerRoot("dropins/testBindingTypeWsdlWeb.war/WEB-INF", "BindingTypeWsdlMismatchTest/test_Soap11HttpBindingType_Soap12Wsdl/webservices.xml");
        TestUtils.publishFileToServer(server,
                                      "BindingTypeWsdlMismatchTest/test_Soap11HttpBindingType_Soap12Wsdl", "HelloService12.wsdl",
                                      "dropins/testBindingTypeWsdlWeb.war/WEB-INF/wsdl", "HelloService.wsdl");

        server.startServerAndValidate(true, true, false);
        List<String> results = server.findStringsInLogs("javax.xml.ws.WebServiceException.*CWWKW0058E"); // start failed
        assertTrue("The validation should be failed with a WebServiceException.", !results.isEmpty());
    }

}
